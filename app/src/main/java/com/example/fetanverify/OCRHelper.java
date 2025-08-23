package com.example.fetanverify;

import android.util.Base64;
import android.util.Log;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OCRHelper {
    private static final String TAG = "OCRHelper";

    // Updated regex patterns for different transaction ID formats
    private static final Pattern FT_PATTERN = Pattern.compile("FT[A-Za-z0-9]{10,12}", Pattern.CASE_INSENSITIVE);
    // Updated CH pattern to match CH + 8 variable characters
    private static final Pattern CH_PATTERN = Pattern.compile("CH[A-Za-z0-9]{8}", Pattern.CASE_INSENSITIVE);
    // Keep legacy patterns for backward compatibility
    private static final Pattern CHA_PATTERN = Pattern.compile("CHA[A-Za-z0-9]{5}", Pattern.CASE_INSENSITIVE);
    private static final Pattern CHE_PATTERN = Pattern.compile("CHE[A-Za-z0-9]{7}", Pattern.CASE_INSENSITIVE);

    /**
     * Extract transaction IDs from SMS text with priority logic:
     * 1. If both CH and FT IDs are found → take only FT
     * 2. If only CH ID is found → take CH
     * 3. If only FT ID is found → take FT
     */
    public static String extractFromSMSWithPriority(String smsText) {
        if (smsText == null || smsText.isEmpty()) {
            Log.d(TAG, "extractFromSMSWithPriority: Input text is null or empty");
            return null;
        }

        String cleanText = smsText.replaceAll("\\s+", " ").toUpperCase();
        Log.d(TAG, "extractFromSMSWithPriority: Processing SMS text: " + cleanText);

        // Look for both FT and CH IDs
        String ftId = extractFTFromSMS(cleanText);
        String chId = extractCHFromText(cleanText);

        Log.d(TAG, "extractFromSMSWithPriority: Found FT ID: " + ftId + ", CH ID: " + chId);

        // Apply priority logic
        if (ftId != null && chId != null) {
            Log.d(TAG, "extractFromSMSWithPriority: Both found, returning FT ID: " + ftId);
            return ftId;
        } else if (ftId != null) {
            Log.d(TAG, "extractFromSMSWithPriority: Only FT found, returning: " + ftId);
            return ftId;
        } else if (chId != null) {
            Log.d(TAG, "extractFromSMSWithPriority: Only CH found, returning: " + chId);
            return chId;
        }

        Log.d(TAG, "extractFromSMSWithPriority: No transaction IDs found");
        return null;
    }

    /**
     * Extract FT transaction ID from SMS text
     */
    public static String extractFTFromSMS(String smsText) {
        if (smsText == null || smsText.isEmpty()) {
            Log.d(TAG, "extractFTFromSMS: Input text is null or empty");
            return null;
        }

        String cleanText = smsText.replaceAll("\\s+", " ").toUpperCase();
        Log.d(TAG, "extractFTFromSMS: Searching for FT ID in text: " + cleanText);

        Matcher ftMatcher = FT_PATTERN.matcher(cleanText);
        if (ftMatcher.find()) {
            String ftId = ftMatcher.group();
            Log.d(TAG, "extractFTFromSMS: Found FT ID: " + ftId);
            return ftId;
        }

        Log.d(TAG, "extractFTFromSMS: No FT ID found in SMS text");
        return null;
    }

    /**
     * Extract CH transaction ID from text (updated to handle variable suffixes)
     */
    public static String extractCHFromText(String text) {
        if (text == null || text.isEmpty()) {
            Log.d(TAG, "extractCHFromText: Input text is null or empty");
            return null;
        }

        String cleanText = text.toUpperCase();
        Log.d(TAG, "extractCHFromText: Searching for CH ID in text: " + cleanText);

        // Primary pattern: CH + 8 variable characters
        Matcher chMatcher = CH_PATTERN.matcher(cleanText);
        if (chMatcher.find()) {
            String chId = chMatcher.group();
            Log.d(TAG, "extractCHFromText: Found CH ID: " + chId);
            return chId;
        }

        // Legacy patterns for backward compatibility
        Matcher chaMatcher = CHA_PATTERN.matcher(cleanText);
        if (chaMatcher.find()) {
            String chaId = chaMatcher.group();
            Log.d(TAG, "extractCHFromText: Found legacy CHA ID: " + chaId);
            return chaId;
        }

        Matcher cheMatcher = CHE_PATTERN.matcher(cleanText);
        if (cheMatcher.find()) {
            String cheId = cheMatcher.group();
            Log.d(TAG, "extractCHFromText: Found legacy CHE ID: " + cheId);
            return cheId;
        }

        Log.d(TAG, "extractCHFromText: No CH ID found in text");
        return null;
    }

    /**
     * Convert hex string to ASCII
     */
    private static String hexToAscii(String hexStr) {
        try {
            Log.d(TAG, "hexToAscii: Converting hex: " + hexStr);
            StringBuilder output = new StringBuilder();
            for (int i = 0; i < hexStr.length(); i += 2) {
                if (i + 1 < hexStr.length()) {
                    String str = hexStr.substring(i, i + 2);
                    int decimal = Integer.parseInt(str, 16);
                    output.append((char) decimal);
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

    /**
     * Extract transaction ID from hex-encoded data (updated for variable CH suffixes)
     */
    private static String extractTransactionFromHex(String hexData) {
        try {
            hexData = hexData.toUpperCase();
            Log.d(TAG, "extractTransactionFromHex: Processing hex data: " + hexData);

            // Try CH prefix (4348), next 16 hex chars (8 ASCII chars) - UPDATED
            int chIndex = hexData.indexOf("4348"); // CH
            if (chIndex != -1) {
                int start = chIndex;
                int length = 20; // 4 (CH) + 16 (8 chars)
                if (start + length <= hexData.length()) {
                    String idHex = hexData.substring(start, start + length);
                    String idAscii = hexToAscii(idHex);
                    if (idAscii != null && CH_PATTERN.matcher(idAscii).matches()) {
                        Log.d(TAG, "extractTransactionFromHex: Found CH ID: " + idAscii);
                        return idAscii;
                    }
                }
            }

            // Try legacy CHA prefix (434841), next 10 hex chars (5 ASCII chars)
            int chaIndex = hexData.indexOf("434841"); // CHA
            if (chaIndex != -1) {
                int start = chaIndex;
                int length = 16; // 6 (CHA) + 10 (5 chars)
                if (start + length <= hexData.length()) {
                    String idHex = hexData.substring(start, start + length);
                    String idAscii = hexToAscii(idHex);
                    if (idAscii != null && CHA_PATTERN.matcher(idAscii).matches()) {
                        Log.d(TAG, "extractTransactionFromHex: Found legacy CHA ID: " + idAscii);
                        return idAscii;
                    }
                }
            }

            // Try legacy CHE prefix (434845), next 14 hex chars (7 ASCII chars)
            int cheIndex = hexData.indexOf("434845"); // CHE
            if (cheIndex != -1) {
                int start = cheIndex;
                int length = 20; // 6 (CHE) + 14 (7 chars)
                if (start + length <= hexData.length()) {
                    String idHex = hexData.substring(start, start + length);
                    String idAscii = hexToAscii(idHex);
                    if (idAscii != null && CHE_PATTERN.matcher(idAscii).matches()) {
                        Log.d(TAG, "extractTransactionFromHex: Found legacy CHE ID: " + idAscii);
                        return idAscii;
                    }
                }
            }

            // Try FT prefix (4654), next 20-24 hex chars (10-12 ASCII chars)
            int ftIndex = hexData.indexOf("4654"); // FT
            if (ftIndex != -1) {
                int start = ftIndex;
                for (int charLen = 12; charLen >= 10; charLen--) {
                    int hexLen = charLen * 2 + 4; // 4 (FT) + charLen * 2
                    if (start + hexLen <= hexData.length()) {
                        String idHex = hexData.substring(start, start + hexLen);
                        String idAscii = hexToAscii(idHex);
                        if (idAscii != null && FT_PATTERN.matcher(idAscii).matches()) {
                            Log.d(TAG, "extractTransactionFromHex: Found FT ID: " + idAscii);
                            return idAscii;
                        }
                    }
                }
            }

            // Fallback to full ASCII and regex match
            String fullAscii = hexToAscii(hexData);
            if (fullAscii != null) {
                String chFromAscii = extractCHFromText(fullAscii);
                if (chFromAscii != null) {
                    Log.d(TAG, "extractTransactionFromHex: Found CH ID from full ASCII: " + chFromAscii);
                    return chFromAscii;
                }
                String ftFromAscii = extractFTFromSMS(fullAscii);
                if (ftFromAscii != null) {
                    Log.d(TAG, "extractTransactionFromHex: Found FT ID from full ASCII: " + ftFromAscii);
                    return ftFromAscii;
                }
            }

            Log.d(TAG, "extractTransactionFromHex: No transaction ID patterns found in hex data");
            return null;
        } catch (Exception e) {
            Log.e(TAG, "extractTransactionFromHex: Error extracting from hex: " + e.getMessage());
            return null;
        }
    }

    /**
     * Decode Base64 QR code data and extract transaction ID
     */
    public static String decodeBase64QR(String base64Data) {
        try {
            if (base64Data == null || base64Data.isEmpty()) {
                Log.d(TAG, "decodeBase64QR: Input is null or empty");
                return null;
            }

            base64Data = base64Data.replaceAll("\\s+", "");
            Log.d(TAG, "decodeBase64QR: Decoding Base64: " + base64Data);

            byte[] decodedBytes = Base64.decode(base64Data, Base64.DEFAULT);
            StringBuilder hexString = new StringBuilder();
            for (byte b : decodedBytes) {
                hexString.append(String.format("%02X", b & 0xFF));
            }
            String hexData = hexString.toString();
            Log.d(TAG, "decodeBase64QR: Decoded to hex: " + hexData);

            String transactionId = extractTransactionFromHex(hexData);
            if (transactionId != null) {
                Log.d(TAG, "decodeBase64QR: Successfully extracted transaction ID: " + transactionId);
                return transactionId;
            }

            String decodedString = new String(decodedBytes);
            Log.d(TAG, "decodeBase64QR: Decoded to string: " + decodedString);

            String chFromString = extractCHFromText(decodedString);
            if (chFromString != null) {
                Log.d(TAG, "decodeBase64QR: Found CH ID in decoded string: " + chFromString);
                return chFromString;
            }

            String ftFromString = extractFTFromSMS(decodedString);
            if (ftFromString != null) {
                Log.d(TAG, "decodeBase64QR: Found FT ID in decoded string: " + ftFromString);
                return ftFromString;
            }

            Log.d(TAG, "decodeBase64QR: No transaction ID patterns found, returning null");
            return null;
        } catch (Exception e) {
            Log.e(TAG, "decodeBase64QR: Error decoding Base64: " + e.getMessage());
            return null;
        }
    }

    /**
     * Check if string looks like Base64 encoded data
     */
    public static boolean isBase64(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }

        str = str.replaceAll("\\s+", "");
        try {
            Base64.decode(str, Base64.DEFAULT);
            boolean isBase64 = str.length() > 20 && str.matches("^[A-Za-z0-9+/]*={0,2}$");
            Log.d(TAG, "isBase64: String '" + str.substring(0, Math.min(20, str.length())) + "...' is Base64: " + isBase64);
            return isBase64;
        } catch (Exception e) {
            Log.d(TAG, "isBase64: String is not valid Base64: " + e.getMessage());
            return false;
        }
    }

    /**
     * Process any scanned text and try to extract transaction ID
     */
    public static String processScannedText(String scannedText) {
        if (scannedText == null || scannedText.isEmpty()) {
            Log.d(TAG, "processScannedText: Input is null or empty");
            return null;
        }

        Log.d(TAG, "processScannedText: Processing scanned text: " + scannedText.substring(0, Math.min(50, scannedText.length())) + "...");

        if (isBase64(scannedText)) {
            Log.d(TAG, "processScannedText: Detected Base64 encoded data");
            String decoded = decodeBase64QR(scannedText);
            if (decoded != null) {
                Log.d(TAG, "processScannedText: Successfully decoded Base64 to: " + decoded);
                return decoded;
            }
        }

        String ftId = extractFTFromSMS(scannedText);
        if (ftId != null) {
            Log.d(TAG, "processScannedText: Found FT ID: " + ftId);
            return ftId;
        }

        String chId = extractCHFromText(scannedText);
        if (chId != null) {
            Log.d(TAG, "processScannedText: Found CH ID: " + chId);
            return chId;
        }

        Log.d(TAG, "processScannedText: No patterns matched, returning null");
        return null;
    }

    /**
     * Process SMS text with priority logic for FT and CH IDs
     */
    public static String processSMSText(String smsText) {
        if (smsText == null || smsText.isEmpty()) {
            Log.d(TAG, "processSMSText: Input is null or empty");
            return null;
        }

        Log.d(TAG, "processSMSText: Processing SMS text: " + smsText);
        return extractFromSMSWithPriority(smsText);
    }

    /**
     * Check if a transaction ID is a CH format (including legacy formats)
     */
    public static boolean isCHFormat(String transactionId) {
        if (transactionId == null) return false;
        return CH_PATTERN.matcher(transactionId).matches() || 
               CHA_PATTERN.matcher(transactionId).matches() || 
               CHE_PATTERN.matcher(transactionId).matches();
    }

    /**
     * Check if a transaction ID is an FT format
     */
    public static boolean isFTFormat(String transactionId) {
        if (transactionId == null) return false;
        return FT_PATTERN.matcher(transactionId).matches();
    }
}