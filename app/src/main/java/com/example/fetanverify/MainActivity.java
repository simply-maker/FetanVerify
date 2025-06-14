package com.example.fetanverify;

import android.content.Intent;
import android.content.pm.PackageManager;
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

public class MainActivity extends AppCompatActivity {
    private static final int SCAN_REQUEST_CODE = 0x0000c0de;
    private TextInputEditText transactionIdEditText;
    private TextInputLayout textInputLayout;
    private MaterialButton verifyButton, scanButton, historyButton;
    private DatabaseReference databaseReference;
    private ArrayList<HistoryItem> historyList;
    private Set<String> verifiedTransactionIds; // Track verified transaction IDs
    private FirebaseAuth mAuth;
    private ActivityResultLauncher<Intent> scanLauncher;
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());
    private boolean isVerifying = false;

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
        historyList = new ArrayList<>();
        verifiedTransactionIds = new HashSet<>();

        scanLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        String transactionId = result.getData().getStringExtra("SCAN_RESULT");
                        if (transactionId != null) {
                            transactionIdEditText.setText(transactionId.trim());
                            verifyTransaction(transactionId.trim()); // Auto-verify on scan
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

        // Try to trigger Gebi app if available
        tryTriggerGebiApp();
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null && getCurrentFocus() != null) {
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
    }

    private void setVerifyingState(boolean verifying) {
        isVerifying = verifying;
        verifyButton.setEnabled(!verifying);
        scanButton.setEnabled(!verifying);
        
        if (verifying) {
            verifyButton.setText("Verifying...");
        } else {
            verifyButton.setText("Verify");
        }
    }

    private void verifyTransaction(String transactionId) {
        if (isVerifying) return;
        
        setVerifyingState(true);
        
        Query query = databaseReference.orderByChild("transactionId").equalTo(transactionId);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                setVerifyingState(false);
                
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
                setVerifyingState(false);
                TextView resultTextView = findViewById(R.id.resultTextView);
                resultTextView.setVisibility(View.VISIBLE);
                resultTextView.setText("Error: " + databaseError.getMessage());
                
                // Show error popup
                VerificationPopup.showErrorPopup(MainActivity.this, "Database Error");
            }
        });
    }

    private void tryTriggerGebiApp() {
        try {
            // Try to launch Gebi app to keep it active
            PackageManager pm = getPackageManager();
            Intent launchIntent = pm.getLaunchIntentForPackage("com.example.gebi"); // Replace with actual Gebi package name
            
            if (launchIntent != null) {
                // Add flags to bring app to background without showing UI
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                launchIntent.putExtra("trigger_background_service", true);
                startActivity(launchIntent);
            }
        } catch (Exception e) {
            // Gebi app not found or can't be launched
            // This is expected if the app is not installed
        }
    }

    private void triggerGebiAppForTransaction(String transactionId) {
        try {
            Intent intent = new Intent();
            intent.setAction("com.example.gebi.VERIFY_TRANSACTION");
            intent.putExtra("transaction_id", transactionId);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            sendBroadcast(intent);
        } catch (Exception e) {
            // Handle silently
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Trigger Gebi app when this app becomes active
        tryTriggerGebiApp();
    }
}