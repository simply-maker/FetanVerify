package com.example.fetanverify;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
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
import android.util.Log;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
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
                    Log.d(TAG, "QR Scan result received with code: " + result.getResultCode());
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        String transactionId = result.getData().getStringExtra("SCAN_RESULT");
                        Log.d(TAG, "Raw QR scan result: " + transactionId);
                        if (transactionId != null) {
                            try {
                                String extractedId = extractTransactionId(transactionId.trim());
                                Log.d(TAG, "Extracted transaction ID: " + extractedId);
                                if (extractedId != null) {
                                    transactionIdEditText.setText(extractedId);
                                    verifyTransaction(extractedId);
                                } else {
                                    Log.w(TAG, "No transaction ID found in QR scan result");
                                    Toast.makeText(this, getString(R.string.no_transaction_id_found), Toast.LENGTH_SHORT).show();
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error processing QR scan result: " + e.getMessage(), e);
                                Toast.makeText(this, "Error processing QR code: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        } else {
                            Log.w(TAG, "QR scan result is null");
                            Toast.makeText(this, getString(R.string.no_scan_data), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.w(TAG, "QR scan cancelled or failed");
                        Toast.makeText(this, getString(R.string.scan_cancelled), Toast.LENGTH_SHORT).show();
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

        smsLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Log.d(TAG, "SMS scan result received with code: " + result.getResultCode());
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        String scannedText = result.getData().getStringExtra("SCAN_RESULT");
                        Log.d(TAG, "Raw SMS scan result: " + scannedText);
                        if (scannedText != null) {
                            try {
                                String extractedId = extractFTFromSMS(scannedText);
                                Log.d(TAG, "Extracted FT ID from SMS: " + extractedId);
                                if (extractedId != null) {
                                    transactionIdEditText.setText(extractedId);
                                    verifyTransaction(extractedId);
                                } else {
                                    Log.w(TAG, "No FT ID found in SMS text: " + scannedText);
                                    Toast.makeText(this, getString(R.string.no_ft_found_in_sms), Toast.LENGTH_LONG).show();
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error processing SMS scan result: " + e.getMessage(), e);
                                Toast.makeText(this, "Error processing SMS: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        } else {
                            Log.w(TAG, "SMS scan result is null");
                            Toast.makeText(this, getString(R.string.no_scan_data), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.w(TAG, "SMS scan cancelled or failed");
                        Toast.makeText(this, getString(R.string.scan_cancelled), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private String extractTransactionId(String rawData) {
        if (rawData == null || rawData.isEmpty()) {
            Log.d(TAG, "extractTransactionId: Input is null or empty");
            return null;
        }

        Log.d(TAG, "extractTransactionId: Processing raw data: " + rawData);

        // Check if it's Base64 encoded
        if (isBase64(rawData)) {
            Log.d(TAG, "extractTransactionId: Detected Base64 encoded data");
            String decoded = decodeBase64QR(rawData);
            if (decoded != null) {
                Log.d(TAG, "extractTransactionId: Successfully decoded Base64 to: " + decoded);
                return decoded;
            }
        }

        // Try to extract FT ID
        String ftId = extractFTFromText(rawData);
        if (ftId != null) {
            Log.d(TAG, "extractTransactionId: Found FT ID: " + ftId);
            return ftId;
        }

        // Try to extract CH ID (10 characters exactly)
        String chId = extractCHFromText(rawData);
        if (chId != null) {
            Log.d(TAG, "extractTransactionId: Found CH ID: " + chId);
            return chId;
        }

        // Return original text if no patterns match
        Log.d(TAG, "extractTransactionId: No patterns matched, returning original text: " + rawData.trim());
        return rawData.trim();
    }

    private boolean isBase64(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }

        // Remove whitespace
        str = str.replaceAll("\\s+", "");

        // Check if it's a valid Base64 string
        try {
            android.util.Base64.decode(str, android.util.Base64.DEFAULT);
            boolean isBase64 = str.length() > 20 && str.matches("^[A-Za-z0-9+/]*={0,2}$");
            Log.d(TAG, "isBase64: String '" + str.substring(0, Math.min(20, str.length())) + "...' is Base64: " + isBase64);
            return isBase64;
        } catch (Exception e) {
            Log.d(TAG, "isBase64: String is not valid Base64: " + e.getMessage());
            return false;
        }
    }

    private String decodeBase64QR(String base64Data) {
        try {
            if (base64Data == null || base64Data.isEmpty()) {
                Log.d(TAG, "decodeBase64QR: Input is null or empty");
                return null;
            }

            // Remove any whitespace
            base64Data = base64Data.replaceAll("\\s+", "");
            Log.d(TAG, "decodeBase64QR: Decoding Base64: " + base64Data);

            // Decode Base64
            byte[] decodedBytes = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT);

            // Convert to hex string for analysis
            StringBuilder hexString = new StringBuilder();
            for (byte b : decodedBytes) {
                hexString.append(String.format("%02X", b & 0xFF));
            }
            String hexData = hexString.toString();
            Log.d(TAG, "decodeBase64QR: Decoded to hex: " + hexData);

            // Try to extract transaction ID from hex data
            String transactionId = extractTransactionFromHex(hexData);
            if (transactionId != null) {
                Log.d(TAG, "decodeBase64QR: Successfully extracted transaction ID: " + transactionId);
                return transactionId;
            }

            // Also try direct string conversion
            String decodedString = new String(decodedBytes);
            Log.d(TAG, "decodeBase64QR: Decoded to string: " + decodedString);

            // Try to extract FT ID from decoded string
            String ftFromString = extractFTFromText(decodedString);
            if (ftFromString != null) {
                Log.d(TAG, "decodeBase64QR: Found FT ID in decoded string: " + ftFromString);
                return ftFromString;
            }

            // Try to extract CH ID from decoded string
            String chFromString = extractCHFromText(decodedString);
            if (chFromString != null) {
                Log.d(TAG, "decodeBase64QR: Found CH ID in decoded string: " + chFromString);
                return chFromString;
            }

            Log.d(TAG, "decodeBase64QR: No transaction ID patterns found, returning raw decoded string");
            return decodedString; // Return raw decoded string if no pattern matches

        } catch (Exception e) {
            Log.e(TAG, "decodeBase64QR: Error decoding Base64: " + e.getMessage());
            return null;
        }
    }

    private String extractTransactionFromHex(String hexData) {
        try {
            Log.d(TAG, "extractTransactionFromHex: Processing hex data: " + hexData);

            // Look for CHE pattern in hex (CHE = 434845)
            java.util.regex.Pattern cheHexPattern = java.util.regex.Pattern.compile("434845([0-9A-F]{14})"); // CHE + 7 hex chars = 10 total chars
            java.util.regex.Matcher cheMatcher = cheHexPattern.matcher(hexData.toUpperCase());

            if (cheMatcher.find()) {
                String cheHexMatch = cheMatcher.group();
                Log.d(TAG, "extractTransactionFromHex: Found CHE hex pattern: " + cheHexMatch);

                // Convert the hex to ASCII
                String asciiResult = hexToAscii(cheHexMatch);
                if (asciiResult != null && asciiResult.startsWith("CHE") && asciiResult.length() == 10) {
                    Log.d(TAG, "extractTransactionFromHex: Extracted CHE ID from hex: " + asciiResult);
                    return asciiResult;
                }
            }

            // Look for CH pattern in hex (CH = 4348)
            java.util.regex.Pattern chHexPattern = java.util.regex.Pattern.compile("4348([0-9A-F]{16})"); // CH + 8 hex chars = 10 total chars
            java.util.regex.Matcher chMatcher = chHexPattern.matcher(hexData.toUpperCase());

            if (chMatcher.find()) {
                String chHexMatch = chMatcher.group();
                Log.d(TAG, "extractTransactionFromHex: Found CH hex pattern: " + chHexMatch);

                // Convert the hex to ASCII
                String asciiResult = hexToAscii(chHexMatch);
                if (asciiResult != null && asciiResult.startsWith("CH") && asciiResult.length() == 10) {
                    Log.d(TAG, "extractTransactionFromHex: Extracted CH ID from hex: " + asciiResult);
                    return asciiResult;
                }
            }

            // Try to convert entire hex to ASCII and look for patterns
            String asciiFromHex = hexToAscii(hexData);
            if (asciiFromHex != null) {
                Log.d(TAG, "extractTransactionFromHex: Full ASCII from hex: " + asciiFromHex);

                // Look for CH pattern in ASCII (exactly 10 characters)
                String chId = extractCHFromText(asciiFromHex);
                if (chId != null) {
                    return chId;
                }

                // Look for FT pattern in ASCII
                String ftId = extractFTFromText(asciiFromHex);
                if (ftId != null) {
                    return ftId;
                }
            }

            return null;
        } catch (Exception e) {
            Log.e(TAG, "extractTransactionFromHex: Error extracting from hex: " + e.getMessage());
            return null;
        }
    }

    private String hexToAscii(String hexStr) {
        try {
            Log.d(TAG, "hexToAscii: Converting hex: " + hexStr);
            StringBuilder output = new StringBuilder();
            for (int i = 0; i < hexStr.length(); i += 2) {
                if (i + 1 < hexStr.length()) {
                    String str = hexStr.substring(i, i + 2);
                    int decimal = Integer.parseInt(str, 16);
                    if (decimal >= 32 && decimal <= 126) { // Printable ASCII range
                        output.append((char) decimal);
                    }
                }
            }
            String result = output.toString();
            Log.d(TAG, "hexToAscii: Result: " + result);
            return result;
        } catch (Exception e) {
            Log.e(TAG, "hexToAscii: Error converting hex to ASCII: " + e.getMessage());
            return null;
        }
    }

    private String extractCHFromText(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }

        String cleanText = text.toUpperCase().replaceAll("\\s+", " ");
        Log.d(TAG, "extractCHFromText: Searching for CH ID in text: " + cleanText);

        // Look for CHE followed by exactly 7 alphanumeric characters (total 10 characters) - priority
        java.util.regex.Pattern chePattern = java.util.regex.Pattern.compile("CHE[A-Z0-9]{7}");
        java.util.regex.Matcher cheMatcher = chePattern.matcher(cleanText);
        if (cheMatcher.find()) {
            String cheId = cheMatcher.group();
            Log.d(TAG, "extractCHFromText: Found CHE ID: " + cheId);
            return cheId;
        }

        // Look for CH followed by exactly 8 alphanumeric characters (total 10 characters)
        java.util.regex.Pattern chPattern = java.util.regex.Pattern.compile("CH[A-Z0-9]{8}");
        java.util.regex.Matcher chMatcher = chPattern.matcher(cleanText);
        if (chMatcher.find()) {
            String chId = chMatcher.group();
            Log.d(TAG, "extractCHFromText: Found CH ID: " + chId);
            return chId;
        }

        return null;
    }

    private String extractFTFromText(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }

        String cleanText = text.toUpperCase().replaceAll("\\s+", " ");
        Log.d(TAG, "extractFTFromText: Searching for FT ID in text: " + cleanText);

        java.util.regex.Pattern ftPattern = java.util.regex.Pattern.compile("FT[A-Z0-9]{10,12}");
        java.util.regex.Matcher ftMatcher = ftPattern.matcher(cleanText);
        if (ftMatcher.find()) {
            String ftId = ftMatcher.group();
            Log.d(TAG, "extractFTFromText: Found FT ID: " + ftId);
            return ftId;
        }

        return null;
    }

    private String extractFTFromSMS(String smsText) {
        if (smsText == null || smsText.isEmpty()) {
            Log.d(TAG, "extractFTFromSMS: Input text is null or empty");
            return null;
        }

        // Clean the text and convert to uppercase for better matching
        String cleanText = smsText.toUpperCase().replaceAll("\\s+", " ");
        Log.d(TAG, "extractFTFromSMS: Searching for FT ID in text: " + cleanText);

        // Look for FT ID first (priority)
        java.util.regex.Pattern ftPattern = java.util.regex.Pattern.compile("FT[A-Z0-9]{10,12}");
        java.util.regex.Matcher ftMatcher = ftPattern.matcher(cleanText);
        if (ftMatcher.find()) {
            String ftId = ftMatcher.group();
            Log.d(TAG, "extractFTFromSMS: Found FT ID: " + ftId);
            return ftId;
        }

        // If no FT ID found, look for CH ID (exactly 10 characters)
        String chId = extractCHFromText(cleanText);
        if (chId != null) {
            Log.d(TAG, "extractFTFromSMS: Found CH ID in SMS: " + chId);
            return chId;
        }

        Log.d(TAG, "extractFTFromSMS: No FT ID found in SMS text");
        return null;
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
            Log.d(TAG, "Processing QR image from URI: " + imageUri);
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
            Log.d(TAG, "Bitmap loaded successfully, size: " + bitmap.getWidth() + "x" + bitmap.getHeight());
            String qrContent = decodeQRCode(bitmap);
            Log.d(TAG, "QR content decoded: " + qrContent);
            if (qrContent != null) {
                String extractedId = extractTransactionId(qrContent.trim());
                Log.d(TAG, "Extracted transaction ID: " + extractedId);
                if (extractedId != null) {
                    transactionIdEditText.setText(extractedId);
                    verifyTransaction(extractedId);
                } else {
                    Log.w(TAG, "No transaction ID found in QR image");
                    Toast.makeText(this, getString(R.string.no_transaction_id_found), Toast.LENGTH_SHORT).show();
                }
            } else {
                Log.w(TAG, "No QR code found in image");
                Toast.makeText(this, getString(R.string.no_qr_found), Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Log.e(TAG, "Error processing image: " + e.getMessage(), e);
            Toast.makeText(this, getString(R.string.error_processing_image), Toast.LENGTH_SHORT).show();
        }
    }

    private String decodeQRCode(Bitmap bitmap) {
        try {
            Log.d(TAG, "Attempting to decode QR code from bitmap");
            int[] pixels = new int[bitmap.getWidth() * bitmap.getHeight()];
            bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

            RGBLuminanceSource source = new RGBLuminanceSource(bitmap.getWidth(), bitmap.getHeight(), pixels);
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
        Log.d(TAG, "Starting verification for transaction ID: " + transactionId);
        showLoading(true);

        // First try with the original transaction ID
        Query query = databaseReference.orderByChild("transactionId").equalTo(transactionId);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d(TAG, "Database query completed. Data exists: " + dataSnapshot.exists());
                if (dataSnapshot.exists()) {
                    // Found match with original ID
                    if (isEncodedCHId(transactionId)) {
                        handleVerificationResult(dataSnapshot, transactionId);
                    } else {
                        // If no match found and it looks like a CH ID, show FT dialog
                        Log.d(TAG, "No match found for transaction ID: " + transactionId);
                        showFTIdDialog();
                    }
                } else {
                    // No match found, handle failure
                    Log.d(TAG, "No match found for transaction ID: " + transactionId);
                    handleVerificationFailure(transactionId);
                }
                showLoading(false); // Ensure loading is stopped after processing
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Database error: " + databaseError.getMessage());
                showLoading(false);
                TextView resultTextView = findViewById(R.id.resultTextView);
                resultCard.setVisibility(View.VISIBLE);
                resultTextView.setText(getString(R.string.error) + ": " + databaseError.getMessage());

                VerificationPopup.showErrorPopup(MainActivity.this, getString(R.string.database_error));
            }
        });
    }

    private boolean isEncodedCHId(String transactionId) {
        if (transactionId == null || transactionId.length() != 10) {
            return false;
        }

        String upperCaseId = transactionId.toUpperCase();
        return upperCaseId.startsWith("CHE") || upperCaseId.startsWith("CH");
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
        Log.d(TAG, "Handling verification result for: " + transactionId);
        showLoading(false);

        TextView resultTextView = findViewById(R.id.resultTextView);
        String sender = null;
        String timestamp = null;
        String amount = null;

        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
            sender = snapshot.child("sender").getValue(String.class);
            // Handle amount as either String or Double
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

        if (!verifiedTransactionIds.contains(transactionId)) {
            HistoryItem item = new HistoryItem(transactionId, getString(R.string.verified), timestamp, amount != null ? amount : "N/A");
            historyList.add(0, item);
            verifiedTransactionIds.add(transactionId);
            saveHistoryToPrefs();
            Log.d(TAG, "Added transaction to history: " + transactionId);
        }

        resultCard.setVisibility(View.VISIBLE);
        String resultText = getString(R.string.verified) + "\n" +
                getString(R.string.transaction_id, transactionId) + "\n" +
                getString(R.string.sender, sender) + "\n" +
                getString(R.string.amount, amount != null ? amount : "N/A") + "\n" +
                getString(R.string.timestamp, timestamp);
        resultTextView.setText(resultText);

        VerificationPopup.showSuccessPopup(MainActivity.this, transactionId, sender, timestamp, amount);
        Log.d(TAG, "Verification successful for: " + transactionId);
    }

    private void handleVerificationFailure(String transactionId) {
        Log.d(TAG, "Handling verification failure for: " + transactionId);
        showLoading(false);
        TextView resultTextView = findViewById(R.id.resultTextView);
        resultCard.setVisibility(View.VISIBLE);
        resultTextView.setText(getString(R.string.failed) + "\n" + getString(R.string.invalid_transaction_id, transactionId));

        VerificationPopup.showErrorPopup(MainActivity.this, transactionId);
    }
}