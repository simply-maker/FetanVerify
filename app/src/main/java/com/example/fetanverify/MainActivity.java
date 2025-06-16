package com.example.fetanverify;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.journeyapps.barcodescanner.CaptureActivity;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;

public class MainActivity extends AppCompatActivity {
    private static final int SCAN_REQUEST_CODE = 0x0000c0de;
    private TextInputEditText transactionIdEditText;
    private TextInputLayout textInputLayout;
    private MaterialButton verifyButton, scanButton, historyButton;
    private DatabaseReference databaseReference;
    private ArrayList<HistoryItem> historyList;
    private Set<String> verifiedTransactionIds;
    private FirebaseAuth mAuth;
    private ActivityResultLauncher<Intent> scanLauncher;
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());
    private SharedPreferences historyPrefs;
    private static final String HISTORY_PREFS = "HistoryPrefs";
    private static final String HISTORY_LIST_KEY = "historyList";
    private static final String VERIFIED_IDS_KEY = "verifiedIds";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
            return;
        }

        textInputLayout = findViewById(R.id.textInputLayout);
        transactionIdEditText = findViewById(R.id.transactionIdEditText);
        verifyButton = findViewById(R.id.verifyButton);
        scanButton = findViewById(R.id.scanButton);
        historyButton = findViewById(R.id.historyButton);

        databaseReference = FirebaseDatabase.getInstance().getReference("users").child(currentUser.getUid()).child("sms_messages");
        
        // Initialize SharedPreferences for persistent history
        historyPrefs = getSharedPreferences(HISTORY_PREFS, MODE_PRIVATE);
        loadHistoryFromPrefs();

        scanLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        String transactionId = result.getData().getStringExtra("SCAN_RESULT");
                        if (transactionId != null) {
                            transactionIdEditText.setText(transactionId.trim());
                            verifyTransaction(transactionId.trim());
                        } else {
                            Toast.makeText(this, "No scan data received", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Scan cancelled", Toast.LENGTH_SHORT).show();
                    }
                });

        verifyButton.setOnClickListener(v -> {
            String transactionId = transactionIdEditText.getText().toString().trim();
            if (TextUtils.isEmpty(transactionId)) {
                textInputLayout.setError("Enter a transaction ID");
                return;
            }
            textInputLayout.setError(null);
            hideKeyboard();
            verifyTransaction(transactionId);
        });

        scanButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CaptureActivity.class);
            intent.putExtra("SCAN_ORIENTATION_LOCKED", true);
            intent.putExtra("PROMPT_MESSAGE", "Scan QR Code");
            intent.putExtra("BEEP_ENABLED", true);
            scanLauncher.launch(intent);
        });

        historyButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, HistoryActivity.class);
            intent.putParcelableArrayListExtra("historyList", historyList);
            startActivity(intent);
        });
    }

    private void loadHistoryFromPrefs() {
        historyList = new ArrayList<>();
        verifiedTransactionIds = new HashSet<>();
        
        // Load history list
        String historyJson = historyPrefs.getString(HISTORY_LIST_KEY, "");
        if (!historyJson.isEmpty()) {
            Gson gson = new Gson();
            Type type = new TypeToken<ArrayList<HistoryItem>>(){}.getType();
            ArrayList<HistoryItem> savedHistory = gson.fromJson(historyJson, type);
            if (savedHistory != null) {
                historyList.addAll(savedHistory);
            }
        }
        
        // Load verified IDs set
        String verifiedIdsJson = historyPrefs.getString(VERIFIED_IDS_KEY, "");
        if (!verifiedIdsJson.isEmpty()) {
            Gson gson = new Gson();
            Type type = new TypeToken<HashSet<String>>(){}.getType();
            HashSet<String> savedIds = gson.fromJson(verifiedIdsJson, type);
            if (savedIds != null) {
                verifiedTransactionIds.addAll(savedIds);
            }
        }
    }

    private void saveHistoryToPrefs() {
        SharedPreferences.Editor editor = historyPrefs.edit();
        Gson gson = new Gson();
        
        // Save history list
        String historyJson = gson.toJson(historyList);
        editor.putString(HISTORY_LIST_KEY, historyJson);
        
        // Save verified IDs set
        String verifiedIdsJson = gson.toJson(verifiedTransactionIds);
        editor.putString(VERIFIED_IDS_KEY, verifiedIdsJson);
        
        editor.apply();
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null && getCurrentFocus() != null) {
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
    }

    private void verifyTransaction(String transactionId) {
        Query query = databaseReference.orderByChild("transactionId").equalTo(transactionId);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                TextView resultTextView = findViewById(R.id.resultTextView);
                boolean isVerified = false;
                String sender = null;
                String timestamp = null;
                
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    sender = snapshot.child("sender").getValue(String.class);
                    Long timestampLong = snapshot.child("timestamp").getValue(Long.class);
                    if (sender != null && timestampLong != null) {
                        timestamp = dateFormat.format(new Date(timestampLong));
                        isVerified = true;
                        break;
                    }
                }
                
                if (isVerified) {
                    // Check if this transaction ID is already in history
                    if (!verifiedTransactionIds.contains(transactionId)) {
                        HistoryItem item = new HistoryItem(transactionId, "Verified", timestamp);
                        historyList.add(0, item);
                        verifiedTransactionIds.add(transactionId);
                        saveHistoryToPrefs(); // Save to persistent storage
                    }
                    
                    resultTextView.setVisibility(View.VISIBLE);
                    resultTextView.setText("✓ Verified\nSender: " + sender + "\nTimestamp: " + timestamp);
                    
                    // Show success popup
                    VerificationPopup.showSuccessPopup(MainActivity.this, transactionId, sender, timestamp);
                } else {
                    resultTextView.setVisibility(View.VISIBLE);
                    resultTextView.setText("✗ Failed\nInvalid Transaction ID");
                    
                    // Show error popup
                    VerificationPopup.showErrorPopup(MainActivity.this, transactionId);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                TextView resultTextView = findViewById(R.id.resultTextView);
                resultTextView.setVisibility(View.VISIBLE);
                resultTextView.setText("Error: " + databaseError.getMessage());
                
                // Show error popup
                VerificationPopup.showErrorPopup(MainActivity.this, "Database Error");
            }
        });
    }
}