package com.example.fetanverify;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
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
import android.util.Log;
import java.util.concurrent.CompletableFuture;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int SCAN_REQUEST_CODE = 0x0000c0de;
    private TextInputEditText transactionIdEditText;
    private TextInputLayout textInputLayout;
    private MaterialButton verifyButton, scanButton, historyButton, importImageButton, languageButton;
    private ProgressBar progressBar;
    private CardView resultCard;
    private DatabaseReference databaseReference;
    private ArrayList<HistoryItem> historyList;
    private Set<String> verifiedTransactionIds;
    private FirebaseAuth mAuth;
    private DatabaseManager databaseManager;
    private ActivityResultLauncher<Intent> scanLauncher;
    private ActivityResultLauncher<Intent> imageLauncher;
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());
    private SharedPreferences historyPrefs;
    private static final String HISTORY_PREFS = "HistoryPrefs";
    private static final String HISTORY_LIST_KEY = "historyList";
    private static final String VERIFIED_IDS_KEY = "verifiedIds";
    private static final String GLOBAL_VERIFIED_IDS_KEY = "globalVerifiedIds";
    private SharedPreferences globalPrefs;
    
    @Override
    protected void onResume() {
        super.onResume();
        // Reload global verified IDs when activity resumes
        loadGlobalVerifiedIds();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Apply language before setting content view
        LanguageHelper.applyLanguage(this);

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
        loadGlobalVerifiedIds();
        databaseManager = new DatabaseManager();
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
        languageButton = findViewById(R.id.languageButton);
        progressBar = findViewById(R.id.progressBar);
        resultCard = findViewById(R.id.resultCard);
        
        // Set input to always uppercase
        transactionIdEditText.setFilters(new android.text.InputFilter[] {
            new android.text.InputFilter.AllCaps()
        });
    }

    private void setupDatabase() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        databaseReference = FirebaseDatabase.getInstance().getReference("users")
                .child(currentUser.getUid()).child("sms_messages");
    }

    private void setupLaunchers() {
        scanLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Log.d(TAG, "QR Scan result received with code: " + result.getResultCode());
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        String transactionId = result.getData().getStringExtra("SCAN_RESULT");
                        Log.d(TAG, "Raw QR scan result: " + transactionId);
                        if (transactionId != null) {
                            try {
                                String extractedId = TransactionExtractor.extractTransactionId(transactionId);
                                Log.d(TAG, "Extracted transaction ID: " + extractedId);
                                if (extractedId != null) {
                                    transactionIdEditText.setText(extractedId);
                                    verifyTransaction(extractedId);
                                } else {
                                    Log.w(TAG, "No transaction ID found in QR scan result");
                                    showAlert("QR code scanned but no valid transaction ID found. Please enter FT ID manually.", "error");
                                    showFTIdDialog();
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error processing QR scan result: " + e.getMessage(), e);
                                showAlert("Error processing QR code: " + e.getMessage(), "error");
                                showFTIdDialog();
                            }
                        } else {
                            Log.w(TAG, "QR scan result is null");
                            showAlert("QR scan failed to capture data", "error");
                            showFTIdDialog();
                        }
                    } else {
                        Log.w(TAG, "QR scan cancelled or failed");
                        if (result.getResultCode() != RESULT_CANCELED) {
                            Toast.makeText(this, getString(R.string.scan_cancelled), Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        imageLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Log.d(TAG, "Image import result received with code: " + result.getResultCode());
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        Log.d(TAG, "Image URI: " + imageUri);
                        if (imageUri != null) {
                            try {
                                processQRImageFromUri(imageUri);
                            } catch (Exception e) {
                                Log.e(TAG, "Error processing image: " + e.getMessage(), e);
                                Toast.makeText(this, "Error processing image: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        }
                    }
                });
    }

    private void setupClickListeners() {
        verifyButton.setOnClickListener(v -> {
            String transactionId = transactionIdEditText.getText().toString().trim();
            if (TextUtils.isEmpty(transactionId)) {
                textInputLayout.setError(getString(R.string.enter_transaction_id));
                return;
            }
            textInputLayout.setError(null);
            hideKeyboard();
            verifyTransaction(transactionId);
        });

        scanButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CaptureActivity.class);
            intent.putExtra("SCAN_ORIENTATION_LOCKED", true);
            intent.putExtra("PROMPT_MESSAGE", getString(R.string.scan_qr_code));
            intent.putExtra("BEEP_ENABLED", true);
            scanLauncher.launch(intent);
        });

        importImageButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*");
            imageLauncher.launch(intent);
        });

        languageButton.setOnClickListener(v -> showLanguageMenu());

        historyButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, HistoryActivity.class);
            intent.putParcelableArrayListExtra("historyList", historyList);
            startActivity(intent);
        });

        MaterialButton logoutButton = findViewById(R.id.logoutButton);
        if (logoutButton != null) {
            logoutButton.setOnClickListener(v -> {
                SharedPreferences prefs = getSharedPreferences("RememberPrefs", MODE_PRIVATE);
                prefs.edit().putBoolean("stay_logged_in", false).apply();
                mAuth.signOut();
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }
    }

    private void showLanguageMenu() {
        PopupMenu popup = new PopupMenu(this, languageButton);
        popup.getMenuInflater().inflate(R.menu.language_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            String languageCode = LanguageHelper.ENGLISH;
            if (item.getItemId() == R.id.language_amharic) {
                languageCode = LanguageHelper.AMHARIC;
            } else if (item.getItemId() == R.id.language_oromo) {
                languageCode = LanguageHelper.OROMO;
            }
            LanguageHelper.setLanguage(this, languageCode);
            recreate();
            return true;
        });
        popup.show();
    }

    private void processQRImageFromUri(Uri imageUri) {
        try {
            Log.d(TAG, "Processing QR image from URI: " + imageUri);
            showLoading(true);
            
            Bitmap bitmap;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.Source source = ImageDecoder.createSource(getContentResolver(), imageUri);
                bitmap = ImageDecoder.decodeBitmap(source);
            } else {
                // Fallback for older Android versions
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
            }
            
            Log.d(TAG, "Bitmap loaded successfully, size: " + bitmap.getWidth() + "x" + bitmap.getHeight());
            
            // Only try QR code detection - removed OCR for performance
            String qrContent = decodeQRCode(bitmap);
            Log.d(TAG, "QR content decoded: " + qrContent);
            
            if (qrContent != null) {
                String extractedId = TransactionExtractor.extractTransactionId(qrContent.trim());
                Log.d(TAG, "Extracted transaction ID: " + extractedId);
                if (extractedId != null && TransactionExtractor.isValidTransactionId(extractedId)) {
                    transactionIdEditText.setText(extractedId);
                    verifyTransaction(extractedId);
                    return;
                }
            }
            
            // If QR detection fails, show FT dialog
            Log.w(TAG, "No QR code found in image");
            showLoading(false);
            showAlert("No QR code found in the image. Please enter FT ID manually.", "error");
            showFTIdDialog();
                
        } catch (IOException e) {
            Log.e(TAG, "Error loading image: " + e.getMessage(), e);
            showLoading(false);
            showAlert("Error loading image: " + e.getMessage(), "error");
            showFTIdDialog();
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error processing image: " + e.getMessage(), e);
            showLoading(false);
            showAlert("Unexpected error: " + e.getMessage(), "error");
            showFTIdDialog();
        }
    }


    private String decodeQRCode(Bitmap bitmap) {
        try {
            Log.d(TAG, "Attempting to decode QR code from bitmap");
            
            // Optimize bitmap for QR scanning
            Bitmap optimizedBitmap = optimizeBitmapForQR(bitmap);
            
            int[] pixels = new int[bitmap.getWidth() * bitmap.getHeight()];
            optimizedBitmap.getPixels(pixels, 0, optimizedBitmap.getWidth(), 0, 0, optimizedBitmap.getWidth(), optimizedBitmap.getHeight());
            RGBLuminanceSource source = new RGBLuminanceSource(optimizedBitmap.getWidth(), optimizedBitmap.getHeight(), pixels);
            BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(source));
            Result result = new MultiFormatReader().decode(binaryBitmap);
            String qrText = result.getText();
            Log.d(TAG, "QR code decoded successfully: " + qrText);
            return qrText;
        } catch (Exception e) {
            Log.e(TAG, "Error decoding QR code: " + e.getMessage());
            return null;
        }
    }

    private Bitmap optimizeBitmapForQR(Bitmap originalBitmap) {
        try {
            // Scale bitmap if too large for better performance
            int maxSize = 1024;
            int width = originalBitmap.getWidth();
            int height = originalBitmap.getHeight();
            
            if (width > maxSize || height > maxSize) {
                float scale = Math.min((float) maxSize / width, (float) maxSize / height);
                int newWidth = Math.round(width * scale);
                int newHeight = Math.round(height * scale);
                return Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true);
            }
            
            return originalBitmap;
        } catch (Exception e) {
            Log.w(TAG, "Failed to optimize bitmap: " + e.getMessage());
            return originalBitmap;
        }
    }

    private void loadHistoryFromPrefs() {
        historyList = new ArrayList<>();
        verifiedTransactionIds = new HashSet<>();
        historyPrefs = getSharedPreferences(HISTORY_PREFS, MODE_PRIVATE);
        globalPrefs = getSharedPreferences("GlobalVerifiedIds", MODE_PRIVATE);
        
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

    private void loadGlobalVerifiedIds() {
        String globalVerifiedIdsJson = globalPrefs.getString(GLOBAL_VERIFIED_IDS_KEY, "");
        if (!globalVerifiedIdsJson.isEmpty()) {
            Gson gson = new Gson();
            Type type = new TypeToken<HashSet<String>>(){}.getType();
            HashSet<String> globalVerifiedIds = gson.fromJson(globalVerifiedIdsJson, type);
            if (globalVerifiedIds != null) {
                verifiedTransactionIds.addAll(globalVerifiedIds);
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
        
        // Also save to global preferences to persist across app reinstalls
        SharedPreferences.Editor globalEditor = globalPrefs.edit();
        globalEditor.putString(GLOBAL_VERIFIED_IDS_KEY, verifiedIdsJson);
        globalEditor.apply();
        
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
            verifyButton.setText(getString(R.string.verifying));
            resultCard.setVisibility(View.GONE);
        } else {
            verifyButton.setText(getString(R.string.verify));
        }
    }

    private void verifyTransaction(String transactionId) {
        if (TextUtils.isEmpty(transactionId)) {
            Log.w(TAG, "verifyTransaction: Transaction ID is empty");
            showLoading(false);
            Toast.makeText(this, getString(R.string.enter_transaction_id), Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if already verified locally
        String upperTransactionId = transactionId.toUpperCase();
        if (verifiedTransactionIds.contains(upperTransactionId)) {
            Log.d(TAG, "Transaction already verified: " + transactionId);
            showLoading(false);
            
            // Show different result text for already verified
            TextView resultTextView = findViewById(R.id.resultTextView);
            resultCard.setVisibility(View.VISIBLE);
            String resultText = "⚠️ " + getString(R.string.already_verified) + "\n" +
                    getString(R.string.transaction_id, transactionId) + "\n" +
                    getString(R.string.status, getString(R.string.previously_verified));
            resultTextView.setText(resultText);
            
            VerificationPopup.showAlreadyVerifiedPopup(this, transactionId);
            return;
        }

        Log.d(TAG, "Starting verification for transaction ID: " + transactionId);
        showLoading(true);

        // First check if transaction exists in database (for already verified transactions)
        checkTransactionInDatabase(transactionId);
    }

    private void checkTransactionInDatabase(String transactionId) {
        // First check if already verified in database
        databaseManager.checkVerifiedTransaction(transactionId, new DatabaseManager.VerificationCallback() {
            @Override
            public void onVerificationResult(boolean isVerified, String sender, String timestamp, String amount, boolean isNewVerification) {
                if (isVerified && !isNewVerification) {
                    // Transaction already verified in database
                    showLoading(false);
                    
                    // Add to local cache if not already there
                    if (!verifiedTransactionIds.contains(transactionId.toUpperCase())) {
                        HistoryItem item = new HistoryItem(transactionId, getString(R.string.verified), timestamp, 
                            amount != null ? amount : "N/A", sender);
                        historyList.add(0, item);
                        verifiedTransactionIds.add(transactionId.toUpperCase());
                        saveHistoryToPrefs();
                    }
                    
                    TextView resultTextView = findViewById(R.id.resultTextView);
                    resultCard.setVisibility(View.VISIBLE);
                    String resultText = "⚠️ " + getString(R.string.already_verified) + "\n" +
                            getString(R.string.transaction_id, transactionId) + "\n" +
                            getString(R.string.status, getString(R.string.previously_verified));
                    resultTextView.setText(resultText);
                    
                    VerificationPopup.showAlreadyVerifiedPopup(MainActivity.this, transactionId, sender, timestamp, amount);
                } else {
                    // Not verified yet, check SMS database
                    checkSMSDatabase(transactionId);
                }
            }
            
            @Override
            public void onError(String error) {
                showLoading(false);
                TextView resultTextView = findViewById(R.id.resultTextView);
                resultCard.setVisibility(View.VISIBLE);
                resultTextView.setText(getString(R.string.error) + ": " + error);
                VerificationPopup.showErrorPopup(MainActivity.this, getString(R.string.database_error));
            }
        });
    }
    
    private void checkSMSDatabase(String transactionId) {
        Query query = databaseReference.orderByChild("transactionId").equalTo(transactionId);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d(TAG, "SMS Database query completed for transaction ID: " + transactionId);
                showLoading(false);
                if (dataSnapshot.exists()) {
                    Log.d(TAG, "Found matching transaction in SMS database for ID: " + transactionId);
                    handleNewVerificationResult(dataSnapshot, transactionId);
                } else {
                    Log.w(TAG, "No match found in SMS database for transaction ID: " + transactionId);
                    handleVerificationFailure(transactionId);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "SMS Database query failed for transaction ID: " + transactionId + ", Error: " + databaseError.getMessage());
                showLoading(false);
                TextView resultTextView = findViewById(R.id.resultTextView);
                resultCard.setVisibility(View.VISIBLE);
                resultTextView.setText(getString(R.string.error) + ": " + databaseError.getMessage());
                VerificationPopup.showErrorPopup(MainActivity.this, getString(R.string.database_error));
            }
        });
    }

    private void showFTIdDialog() {
        showLoading(false);
        FTIdDialog.showFTIdDialog(this, new FTIdDialog.FTIdCallback() {
            @Override
            public void onFTIdEntered(String ftId) {
                if (ftId != null && !ftId.trim().isEmpty()) {
                    transactionIdEditText.setText(ftId.trim().toUpperCase());
                    verifyTransaction(ftId.trim().toUpperCase());
                } else {
                    showAlert("Please enter a valid FT ID", "error");
                }
            }
        });
    }

    private void handleNewVerificationResult(DataSnapshot dataSnapshot, String transactionId) {
        Log.d(TAG, "Handling verification result for: " + transactionId);
        showLoading(false);

        TextView resultTextView = findViewById(R.id.resultTextView);
        String sender = null;
        String timestamp = null;
        String amount = null;

        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
            sender = snapshot.child("sender").getValue(String.class);
            Object amountObj = snapshot.child("amount").getValue();
            if (amountObj instanceof String) {
                amount = (String) amountObj;
            } else if (amountObj instanceof Double) {
                amount = String.valueOf(((Double) amountObj).intValue());
            } else if (amountObj instanceof Long) {
                amount = String.valueOf(amountObj);
            } else if (amountObj != null) {
                amount = amountObj.toString();
            }
            Long timestampLong = snapshot.child("timestamp").getValue(Long.class);
            Log.d(TAG, "Found data - Sender: " + sender + ", Amount: " + amount + ", Timestamp: " + timestampLong + ", Amount type: " + (amountObj != null ? amountObj.getClass().getSimpleName() : "null"));

            if (sender != null && timestampLong != null) {
                timestamp = dateFormat.format(new Date(timestampLong));
                break;
            }
        }

        // This is a new verification from SMS database
        FirebaseUser currentUser = mAuth.getCurrentUser();
        HistoryItem item = new HistoryItem(transactionId, getString(R.string.verified), timestamp, 
            amount != null ? amount : "N/A", sender);
        historyList.add(0, item);
        verifiedTransactionIds.add(transactionId.toUpperCase());
        saveHistoryToPrefs();
        
        // Save to verified transactions database
        Long timestampLong = null;
        try {
            timestampLong = dateFormat.parse(timestamp).getTime();
        } catch (Exception e) {
            timestampLong = System.currentTimeMillis();
        }
        databaseManager.saveVerifiedTransaction(transactionId, sender, amount, timestampLong);
        
        Log.d(TAG, "Added new transaction to history: " + transactionId);
        
        resultCard.setVisibility(View.VISIBLE);
        String resultText = "✅ " + getString(R.string.new_verification) + "\n" +
                getString(R.string.transaction_id, transactionId) + "\n" +
                getString(R.string.sender, sender != null ? sender : "N/A") + "\n" +
                getString(R.string.amount, amount != null ? amount : "N/A") + "\n" +
                getString(R.string.payment_date, timestamp != null ? timestamp : "N/A");
        resultTextView.setText(resultText);

        VerificationPopup.showSuccessPopup(MainActivity.this, transactionId, sender, timestamp, amount);
        Log.d(TAG, "New verification successful for: " + transactionId);
    }

    private void handleVerificationFailure(String transactionId) {
        Log.d(TAG, "Handling verification failure for: " + transactionId);
        showLoading(false);

        TextView resultTextView = findViewById(R.id.resultTextView);
        resultCard.setVisibility(View.VISIBLE);
        String resultText = getString(R.string.failed) + "\n" + getString(R.string.invalid_transaction_id, transactionId != null ? transactionId : "N/A");
        resultTextView.setText(resultText);

        // Show FT dialog for any transaction that fails verification
        if (transactionId != null && (TransactionExtractor.isCHFormat(transactionId) || TransactionExtractor.isFTFormat(transactionId))) {
            Log.d(TAG, "CH or FT format transaction failed verification, showing FT dialog");
            showFTIdDialog();
        } else {
            // For unknown format or null transaction ID, also show FT dialog
            Log.d(TAG, "Unknown format or null transaction, showing FT dialog");
            showFTIdDialog();
        }
    }

    /**
     * Show alert message to user
     */
    private void showAlert(String message, String type) {
        runOnUiThread(() -> {
            if ("error".equals(type)) {
                Toast.makeText(this, "⚠️ " + message, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}