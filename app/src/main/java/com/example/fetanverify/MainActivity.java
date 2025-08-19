package com.example.fetanverify;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
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

public class MainActivity extends AppCompatActivity {
    private static final int SCAN_REQUEST_CODE = 0x0000c0de;
    private TextInputEditText transactionIdEditText;
    private TextInputLayout textInputLayout;
    private MaterialButton verifyButton, scanButton, historyButton, importImageButton, scanSmsButton, languageButton;
    private ProgressBar progressBar;
    private CardView resultCard;
    private DatabaseReference databaseReference;
    private ArrayList<HistoryItem> historyList;
    private Set<String> verifiedTransactionIds;
    private FirebaseAuth mAuth;
    private ActivityResultLauncher<Intent> scanLauncher;
    private ActivityResultLauncher<Intent> imageLauncher;
    private ActivityResultLauncher<Intent> smsLauncher;
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());
    private SharedPreferences historyPrefs;
    private static final String HISTORY_PREFS = "HistoryPrefs";
    private static final String HISTORY_LIST_KEY = "historyList";
    private static final String VERIFIED_IDS_KEY = "verifiedIds";

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
        scanSmsButton = findViewById(R.id.scanSmsButton);
        languageButton = findViewById(R.id.languageButton);
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
                            String processedId = OCRHelper.processScannedText(transactionId.trim());
                            transactionIdEditText.setText(processedId);
                            verifyTransaction(processedId);
                        } else {
                            Toast.makeText(this, getString(R.string.no_scan_data), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, getString(R.string.scan_cancelled), Toast.LENGTH_SHORT).show();
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
        
        smsLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        String scannedText = result.getData().getStringExtra("SCAN_RESULT");
                        if (scannedText != null) {
                            // For SMS scanning, we're looking for FT ID in the text content
                            String ftId = OCRHelper.extractFTFromSMS(scannedText);
                            if (ftId != null) {
                                transactionIdEditText.setText(ftId);
                                verifyTransaction(ftId);
                            } else {
                                Toast.makeText(this, getString(R.string.no_ft_found_in_sms), Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(this, getString(R.string.no_scan_data), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, getString(R.string.scan_cancelled), Toast.LENGTH_SHORT).show();
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

        scanSmsButton.setOnClickListener(v -> {
            // For SMS scanning, we use the same QR scanner but with different instructions
            // The user will scan a screenshot or photo of the SMS text
            Intent intent = new Intent(MainActivity.this, CaptureActivity.class);
            intent.putExtra("SCAN_ORIENTATION_LOCKED", true);
            intent.putExtra("PROMPT_MESSAGE", getString(R.string.scan_sms_screenshot));
            intent.putExtra("BEEP_ENABLED", true);
            smsLauncher.launch(intent);
        });
        
        languageButton.setOnClickListener(v -> showLanguageMenu());
        
        historyButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, HistoryActivity.class);
            intent.putParcelableArrayListExtra("historyList", historyList);
            startActivity(intent);
        });
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
            recreate(); // Restart activity to apply language changes
            return true;
        });
        
        popup.show();
    }
    
    private void processQRImageFromUri(Uri imageUri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
            String qrContent = decodeQRCode(bitmap);
            if (qrContent != null) {
                String processedId = OCRHelper.processScannedText(qrContent.trim());
                transactionIdEditText.setText(processedId);
                verifyTransaction(processedId);
            } else {
                Toast.makeText(this, getString(R.string.no_qr_found), Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Toast.makeText(this, getString(R.string.error_processing_image), Toast.LENGTH_SHORT).show();
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
        scanSmsButton.setEnabled(!show);
        importImageButton.setEnabled(!show);
        
        if (show) {
            verifyButton.setText(getString(R.string.verifying));
            resultCard.setVisibility(View.GONE);
        } else {
            verifyButton.setText(getString(R.string.verify));
        }
    }

    private void verifyTransaction(String transactionId) {
        showLoading(true);
        
        // First try with the original transaction ID
        Query query = databaseReference.orderByChild("transactionId").equalTo(transactionId);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Found match with original ID
                    handleVerificationResult(dataSnapshot, transactionId);
                } else {
                    // If no match found and it looks like a CH ID, show FT dialog
                    if (transactionId.startsWith("CH")) {
                        showFTIdDialog();
                    } else {
                        // No match found
                        handleVerificationFailure(transactionId);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
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
                transactionIdEditText.setText(ftId);
                verifyTransaction(ftId);
            }

            @Override
            public void onScanSMS() {
                // For SMS scanning from dialog, use the same approach
                Intent intent = new Intent(MainActivity.this, CaptureActivity.class);
                intent.putExtra("SCAN_ORIENTATION_LOCKED", true);
                intent.putExtra("PROMPT_MESSAGE", getString(R.string.scan_sms_screenshot));
                intent.putExtra("BEEP_ENABLED", true);
                smsLauncher.launch(intent);
            }
        });
    }
    
    private void handleVerificationResult(DataSnapshot dataSnapshot, String transactionId) {
        showLoading(false);
        
        TextView resultTextView = findViewById(R.id.resultTextView);
        String sender = null;
        String timestamp = null;
        String amount = null;
        
        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
            sender = snapshot.child("sender").getValue(String.class);
            amount = snapshot.child("amount").getValue(String.class);
            Long timestampLong = snapshot.child("timestamp").getValue(Long.class);
            
            if (sender != null && timestampLong != null) {
                timestamp = dateFormat.format(new Date(timestampLong));
                break;
            }
        }
        
        if (!verifiedTransactionIds.contains(transactionId)) {
            HistoryItem item = new HistoryItem(transactionId, getString(R.string.verified), timestamp, amount != null ? amount : "N/A");
            historyList.add(0, item);
            verifiedTransactionIds.add(transactionId);
            saveHistoryToPrefs();
        }
        
        resultCard.setVisibility(View.VISIBLE);
        String resultText = getString(R.string.verified) + "\n" +
                getString(R.string.transaction_id, transactionId) + "\n" +
                getString(R.string.sender, sender) + "\n" +
                getString(R.string.amount, amount != null ? amount : "N/A") + "\n" +
                getString(R.string.timestamp, timestamp);
        resultTextView.setText(resultText);
        
        VerificationPopup.showSuccessPopup(MainActivity.this, transactionId, sender, timestamp, amount);
    }
    
    private void handleVerificationFailure(String transactionId) {
        showLoading(false);
        TextView resultTextView = findViewById(R.id.resultTextView);
        resultCard.setVisibility(View.VISIBLE);
        resultTextView.setText(getString(R.string.failed) + "\n" + getString(R.string.invalid_transaction_id, transactionId));
        
        VerificationPopup.showErrorPopup(MainActivity.this, transactionId);
    }
}