package com.example.fetanverify;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.CircularProgressIndicator;
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
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.journeyapps.barcodescanner.CaptureActivity;
import android.widget.TextView;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private TextInputEditText transactionIdEditText;
    private TextInputLayout textInputLayout;
    private MaterialButton verifyButton, scanButton, historyButton, privacyButton, importImageButton;
    private MaterialCardView resultCard;
    private TextView resultTextView;
    private CircularProgressIndicator loadingIndicator;
    private DatabaseReference databaseReference;
    private ArrayList<HistoryItem> historyList;
    private FirebaseAuth mAuth;
    private ActivityResultLauncher<Intent> scanLauncher;
    private ActivityResultLauncher<Intent> imageLauncher;
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());

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
        setupActivityLaunchers();
        setupClickListeners();
    }

    private void initializeViews() {
        textInputLayout = findViewById(R.id.textInputLayout);
        transactionIdEditText = findViewById(R.id.transactionIdEditText);
        verifyButton = findViewById(R.id.verifyButton);
        scanButton = findViewById(R.id.scanButton);
        historyButton = findViewById(R.id.historyButton);
        privacyButton = findViewById(R.id.privacyButton);
        importImageButton = findViewById(R.id.importImageButton);
        resultCard = findViewById(R.id.resultCard);
        resultTextView = findViewById(R.id.resultTextView);
        loadingIndicator = findViewById(R.id.loadingIndicator);
        historyList = new ArrayList<>();
    }

    private void setupDatabase() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            databaseReference = FirebaseDatabase.getInstance().getReference("users")
                    .child(currentUser.getUid()).child("sms_messages");
        }
    }

    private void setupActivityLaunchers() {
        scanLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        String transactionId = result.getData().getStringExtra("SCAN_RESULT");
                        if (transactionId != null) {
                            transactionIdEditText.setText(transactionId.trim());
                            verifyTransaction(transactionId.trim());
                        } else {
                            showToast("No scan data received");
                        }
                    } else {
                        showToast("Scan cancelled");
                    }
                });

        imageLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            processQRCodeFromImage(imageUri);
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

        privacyButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, PrivacyActivity.class);
            startActivity(intent);
        });
    }

    private void processQRCodeFromImage(Uri imageUri) {
        try {
            showLoading(true);
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
            
            int[] intArray = new int[bitmap.getWidth() * bitmap.getHeight()];
            bitmap.getPixels(intArray, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
            
            LuminanceSource source = new RGBLuminanceSource(bitmap.getWidth(), bitmap.getHeight(), intArray);
            BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(source));
            
            MultiFormatReader reader = new MultiFormatReader();
            Result result = reader.decode(binaryBitmap);
            
            String transactionId = result.getText();
            transactionIdEditText.setText(transactionId);
            verifyTransaction(transactionId);
            
        } catch (Exception e) {
            showLoading(false);
            showToast("Could not read QR code from image");
        }
    }

    private void verifyTransaction(String transactionId) {
        showLoading(true);
        hideResult();
        
        Query query = databaseReference.orderByChild("transactionId").equalTo(transactionId);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                showLoading(false);
                
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String sender = snapshot.child("sender").getValue(String.class);
                    String amount = snapshot.child("amount").getValue(String.class);
                    Long timestampLong = snapshot.child("timestamp").getValue(Long.class);
                    
                    if (sender != null && timestampLong != null) {
                        String timestamp = dateFormat.format(new Date(timestampLong));
                        String amountText = amount != null ? amount : "N/A";
                        
                        HistoryItem item = new HistoryItem(transactionId, "Verified", timestamp, amountText);
                        historyList.add(0, item);
                        
                        showToast("✓ Verification Successful");
                        showResult("✓ Transaction Verified\n\n" +
                                "Transaction ID: " + transactionId + "\n" +
                                "Sender: " + sender + "\n" +
                                "Amount: " + amountText + "\n" +
                                "Timestamp: " + timestamp, true);
                        return;
                    }
                }
                
                String timestamp = dateFormat.format(new Date());
                HistoryItem item = new HistoryItem(transactionId, "Failed", timestamp, "N/A");
                historyList.add(0, item);
                
                showToast("✗ Verification Failed");
                showResult("✗ Transaction Not Found\n\n" +
                        "Transaction ID: " + transactionId + "\n" +
                        "Status: Invalid or not found in database", false);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                showLoading(false);
                showToast("Database error occurred");
                showResult("Error: " + databaseError.getMessage(), false);
            }
        });
    }

    private void showLoading(boolean show) {
        loadingIndicator.setVisibility(show ? View.VISIBLE : View.GONE);
        verifyButton.setEnabled(!show);
        scanButton.setEnabled(!show);
        importImageButton.setEnabled(!show);
    }

    private void showResult(String message, boolean isSuccess) {
        resultTextView.setText(message);
        resultCard.setVisibility(View.VISIBLE);
        
        // Update card appearance based on result
        if (isSuccess) {
            resultCard.setStrokeColor(getColor(R.color.accent_green));
        } else {
            resultCard.setStrokeColor(getColor(R.color.accent_red));
        }
    }

    private void hideResult() {
        resultCard.setVisibility(View.GONE);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}