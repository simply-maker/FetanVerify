package com.example.fetanverify;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
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
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.journeyapps.barcodescanner.CaptureActivity;
import java.io.IOException;
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
    private MaterialButton verifyButton, scanButton, historyButton, importImageButton;
    private ProgressBar progressBar;
    private CardView resultCard;
    private DatabaseReference databaseReference;
    private ArrayList<HistoryItem> historyList;
    private Set<String> verifiedTransactionIds;
    private FirebaseAuth mAuth;
    private ActivityResultLauncher<Intent> scanLauncher;
    private ActivityResultLauncher<Intent> imageLauncher;
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

        initializeViews();
        setupDatabase();
        loadHistoryFromPrefs();
        setupLaunchers();
        setupClickListeners();
    }

    private void initializeViews() {
        textInputLayout = findViewById(R.id.textInputLayout);
        transactionIdEditText = findViewById(R.id.transactionIdEditText);
        verifyButton = findViewById(R.id.verifyButton);
        scanButton = findViewById(R.id.scanButton);
        historyButton = findViewById(R.id.historyButton);
        importImageButton = findViewById(R.id.importImageButton);
        progressBar = findViewById(R.id.progressBar);
        resultCard = findViewById(R.id.resultCard);
    }

    private void setupDatabase() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        databaseReference = FirebaseDatabase.getInstance().getReference("users")
                .child(currentUser.getUid()).child("sms_messages");
    }

    private void setupLaunchers() {
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

        imageLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            processQRImageFromUri(imageUri);
                        }
                    }
                });
    }

    private void setupClickListeners() {
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

        importImageButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*");
            imageLauncher.launch(intent);
        });

        historyButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, HistoryActivity.class);
            intent.putParcelableArrayListExtra("historyList", historyList);
            startActivity(intent);
        });
    }

    private void processQRImageFromUri(Uri imageUri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
            String qrContent = decodeQRCode(bitmap);
            if (qrContent != null) {
                transactionIdEditText.setText(qrContent.trim());
                verifyTransaction(qrContent.trim());
            } else {
                Toast.makeText(this, "No QR code found in the image", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Toast.makeText(this, "Error processing image", Toast.LENGTH_SHORT).show();
        }
    }

    private String decodeQRCode(Bitmap bitmap) {
        try {
            int[] pixels = new int[bitmap.getWidth() * bitmap.getHeight()];
            bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
            
            RGBLuminanceSource source = new RGBLuminanceSource(bitmap.getWidth(), bitmap.getHeight(), pixels);
            BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(source));
            
            Result result = new MultiFormatReader().decode(binaryBitmap);
            return result.getText();
        } catch (Exception e) {
            return null;
        }
    }

    private void loadHistoryFromPrefs() {
        historyList = new ArrayList<>();
        verifiedTransactionIds = new HashSet<>();
        historyPrefs = getSharedPreferences(HISTORY_PREFS, MODE_PRIVATE);
        
        String historyJson = historyPrefs.getString(HISTORY_LIST_KEY, "");
        if (!historyJson.isEmpty()) {
            Gson gson = new Gson();
            Type type = new TypeToken<ArrayList<HistoryItem>>(){}.getType();
            ArrayList<HistoryItem> savedHistory = gson.fromJson(historyJson, type);
            if (savedHistory != null) {
                historyList.addAll(savedHistory);
            }
        }
        
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
        
        String historyJson = gson.toJson(historyList);
        editor.putString(HISTORY_LIST_KEY, historyJson);
        
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

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        verifyButton.setEnabled(!show);
        scanButton.setEnabled(!show);
        importImageButton.setEnabled(!show);
        
        if (show) {
            verifyButton.setText("Verifying...");
            resultCard.setVisibility(View.GONE);
        } else {
            verifyButton.setText("Verify");
        }
    }

    private void verifyTransaction(String transactionId) {
        showLoading(true);
        
        Query query = databaseReference.orderByChild("transactionId").equalTo(transactionId);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                showLoading(false);
                
                TextView resultTextView = findViewById(R.id.resultTextView);
                boolean isVerified = false;
                String sender = null;
                String timestamp = null;
                String amount = null;
                
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    sender = snapshot.child("sender").getValue(String.class);
                    amount = snapshot.child("amount").getValue(String.class);
                    Long timestampLong = snapshot.child("timestamp").getValue(Long.class);
                    
                    if (sender != null && timestampLong != null) {
                        timestamp = dateFormat.format(new Date(timestampLong));
                        isVerified = true;
                        break;
                    }
                }
                
                if (isVerified) {
                    if (!verifiedTransactionIds.contains(transactionId)) {
                        HistoryItem item = new HistoryItem(transactionId, "Verified", timestamp, amount != null ? amount : "N/A");
                        historyList.add(0, item);
                        verifiedTransactionIds.add(transactionId);
                        saveHistoryToPrefs();
                    }
                    
                    resultCard.setVisibility(View.VISIBLE);
                    String resultText = "✓ Verified\n" +
                            "Transaction ID: " + transactionId + "\n" +
                            "Sender: " + sender + "\n" +
                            "Amount: " + (amount != null ? amount : "N/A") + "\n" +
                            "Timestamp: " + timestamp;
                    resultTextView.setText(resultText);
                    
                    VerificationPopup.showSuccessPopup(MainActivity.this, transactionId, sender, timestamp, amount);
                } else {
                    resultCard.setVisibility(View.VISIBLE);
                    resultTextView.setText("✗ Failed\nInvalid Transaction ID: " + transactionId);
                    
                    VerificationPopup.showErrorPopup(MainActivity.this, transactionId);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                showLoading(false);
                TextView resultTextView = findViewById(R.id.resultTextView);
                resultCard.setVisibility(View.VISIBLE);
                resultTextView.setText("Error: " + databaseError.getMessage());
                
                VerificationPopup.showErrorPopup(MainActivity.this, "Database Error");
            }
        });
    }
}