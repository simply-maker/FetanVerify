package com.example.fetanverify;

import android.graphics.Bitmap;
import android.util.Base64;
import android.util.Log;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OCRHelper {
    private static final String TAG = "OCRHelper";
    
    // Regex patterns for different transaction ID formats
    private static final Pattern FT_PATTERN = Pattern.compile("FT[A-Z0-9]{10,12}");
    private static final Pattern CHA_PATTERN = Pattern.compile("CHA[A-Z0-9]{6}");
    private static final Pattern CH_PATTERN = Pattern.compile("CH[A-Z0-9]{8,12}");
    private static final Pattern CHE_PATTERN = Pattern.compile("CHE[A-Z0-9]{5,10}");
    
    /**
     * Extract FT transaction ID from SMS text
     */
    public static String extractFTFromSMS(String smsText) {
        if (smsText == null || smsText.isEmpty()) {
            Log.d(TAG, "extractFTFromSMS: Input text is null or empty");
            return null;
        }
        
        // Clean the text and convert to uppercase for better matching
        String cleanText = smsText.toUpperCase().replaceAll("\\s+", " ");
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
     * Extract CH transaction ID from text
     */
    public static String extractCHFromText(String text) {
        if (text == null || text.isEmpty()) {
            Log.d(TAG, "extractCHFromText: Input text is null or empty");
            return null;
        }
        
        String cleanText = text.toUpperCase().replaceAll("\\s+", " ");
        Log.d(TAG, "extractCHFromText: Searching for CH ID in text: " + cleanText);
        
        // Try CHA pattern first (most specific)
        Matcher chaMatcher = CHA_PATTERN.matcher(cleanText);
        if (chaMatcher.find()) {
            String chaId = chaMatcher.group();
            Log.d(TAG, "extractCHFromText: Found CHA ID: " + chaId);
            return chaId;
        }
        
        // Try CHE pattern second (more specific than CH)
        Matcher cheMatcher = CHE_PATTERN.matcher(cleanText);
        if (cheMatcher.find()) {
            String cheId = cheMatcher.group();
            Log.d(TAG, "extractCHFromText: Found CHE ID: " + cheId);
            return cheId;
        }
        
        // Try general CH pattern
        Matcher chMatcher = CH_PATTERN.matcher(cleanText);
        if (chMatcher.find()) {
            String chId = chMatcher.group();
            Log.d(TAG, "extractCHFromText: Found CH ID: " + chId);
            return chId;
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
    
    /**
     * Extract transaction ID from hex-encoded data
     */
    private static String extractTransactionFromHex(String hexData) {
        try {
            Log.d(TAG, "extractTransactionFromHex: Processing hex data: " + hexData);
            
            // Look for CHA pattern in hex (CHA = 434841)
            Pattern chaHexPattern = Pattern.compile("434841([0-9A-F]+)");
            Matcher chaMatcher = chaHexPattern.matcher(hexData.toUpperCase());
            
            if (chaMatcher.find()) {
                String chaHexMatch = chaMatcher.group();
                Log.d(TAG, "extractTransactionFromHex: Found CHA hex pattern: " + chaHexMatch);
                
                // Convert the hex to ASCII
                String asciiResult = hexToAscii(chaHexMatch);
                if (asciiResult != null && asciiResult.startsWith("CHA")) {
                    // Extract reasonable length CHA ID
                    if (asciiResult.length() <= 15) {
                        Log.d(TAG, "extractTransactionFromHex: Extracted CHA ID from hex: " + asciiResult);
                        return asciiResult;
                    } else {
                        // Truncate to reasonable length
                        String truncated = asciiResult.substring(0, Math.min(15, asciiResult.length()));
                        Log.d(TAG, "extractTransactionFromHex: Truncated CHA ID: " + truncated);
                        return truncated;
                    }
                }
            }
            
            // Look for CHE pattern in hex (CHE = 434845)
            Pattern cheHexPattern = Pattern.compile("434845([0-9A-F]+)");
            Matcher cheMatcher = cheHexPattern.matcher(hexData.toUpperCase());
            
            if (cheMatcher.find()) {
                String cheHexMatch = cheMatcher.group();
                Log.d(TAG, "extractTransactionFromHex: Found CHE hex pattern: " + cheHexMatch);
                
                // Convert the hex to ASCII
                String asciiResult = hexToAscii(cheHexMatch);
                if (asciiResult != null && asciiResult.startsWith("CHE")) {
                    // Extract reasonable length CHE ID
                    if (asciiResult.length() <= 15) {
                        Log.d(TAG, "extractTransactionFromHex: Extracted CHE ID from hex: " + asciiResult);
                        return asciiResult;
                    } else {
                        // Truncate to reasonable length
                        String truncated = asciiResult.substring(0, Math.min(15, asciiResult.length()));
                        Log.d(TAG, "extractTransactionFromHex: Truncated CHE ID: " + truncated);
                        return truncated;
                    }
                }
            }
            
            // Look for CH pattern in hex (CH = 4348)
            Pattern chHexPattern = Pattern.compile("4348([0-9A-F]+)");
            Matcher chMatcher = chHexPattern.matcher(hexData.toUpperCase());
            
            if (chMatcher.find()) {
                String chHexMatch = chMatcher.group();
                Log.d(TAG, "extractTransactionFromHex: Found CH hex pattern: " + chHexMatch);
                
                // Convert the hex to ASCII
                String asciiResult = hexToAscii(chHexMatch);
                if (asciiResult != null && asciiResult.startsWith("CH")) {
                    // Extract reasonable length CH ID
                    if (asciiResult.length() <= 15) {
                        Log.d(TAG, "extractTransactionFromHex: Extracted CH ID from hex: " + asciiResult);
                        return asciiResult;
                    } else {
                        // Truncate to reasonable length
                        String truncated = asciiResult.substring(0, Math.min(15, asciiResult.length()));
                        Log.d(TAG, "extractTransactionFromHex: Truncated CH ID: " + truncated);
                        return truncated;
                    }
                }
            }
            
            // Try to convert entire hex to ASCII and look for patterns
            String asciiFromHex = hexToAscii(hexData);
            if (asciiFromHex != null) {
                Log.d(TAG, "extractTransactionFromHex: Full ASCII from hex: " + asciiFromHex);
                
                // Look for CHA pattern in ASCII (highest priority)
                String chaId = extractCHFromText(asciiFromHex);
                if (chaId != null && chaId.startsWith("CHA")) {
                    return chaId;
                }
                
                // Look for CHE pattern in ASCII
                String cheId = extractCHFromText(asciiFromHex);
                if (cheId != null && cheId.startsWith("CHE")) {
                    return cheId;
                }
                
                // Look for general CH pattern in ASCII
                if (cheId != null && cheId.startsWith("CH")) {
                    return cheId;
                }
                
                // Look for FT pattern in ASCII
                String ftId = extractFTFromSMS(asciiFromHex);
                if (ftId != null) {
                    return ftId;
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "extractTransactionFromHex: Error extracting from hex: " + e.getMessage());
        }
        
        return null;
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
            
            // Remove any whitespace
            base64Data = base64Data.replaceAll("\\s+", "");
            Log.d(TAG, "decodeBase64QR: Decoding Base64: " + base64Data);
            
            // Decode Base64
            byte[] decodedBytes = Base64.decode(base64Data, Base64.DEFAULT);
            
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
            
            // Try to extract CHE ID from decoded string
            String cheFromString = extractCHFromText(decodedString);
            if (cheFromString != null) {
                Log.d(TAG, "decodeBase64QR: Found CHE ID in decoded string: " + cheFromString);
                return cheFromString;
            }
            
            // Try to extract FT ID from decoded string
            String ftFromString = extractFTFromSMS(decodedString);
            if (ftFromString != null) {
                Log.d(TAG, "decodeBase64QR: Found FT ID in decoded string: " + ftFromString);
                return ftFromString;
            }
            
            Log.d(TAG, "decodeBase64QR: No transaction ID patterns found, returning raw decoded string");
            return decodedString; // Return raw decoded string if no pattern matches
            
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
        
        // Remove whitespace
        str = str.replaceAll("\\s+", "");
        
        // Check if it's a valid Base64 string
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
        
        Log.d(TAG, "processScannedText: Processing scanned text: " + scannedText);
        
        // First check if it's Base64 encoded
        if (isBase64(scannedText)) {
            Log.d(TAG, "processScannedText: Detected Base64 encoded data");
            String decoded = decodeBase64QR(scannedText);
            if (decoded != null) {
                Log.d(TAG, "processScannedText: Successfully decoded Base64 to: " + decoded);
                return decoded;
            }
        }
        
        // Try to extract FT ID first (priority for SMS)
        String ftId = extractFTFromSMS(scannedText);
        if (ftId != null) {
            Log.d(TAG, "processScannedText: Found FT ID: " + ftId);
            return ftId;
        }
        
        // Try to extract CHE ID
        String cheId = extractCHFromText(scannedText);
        if (cheId != null) {
            Log.d(TAG, "processScannedText: Found CHE ID: " + cheId);
            return cheId;
        }
        
        // Return original text if no patterns match
        Log.d(TAG, "processScannedText: No patterns matched, returning original text: " + scannedText.trim());
        return scannedText.trim();
    }
    
    /**
     * Process SMS text specifically for FT ID extraction
     */
    public static String processSMSText(String smsText) {
        if (smsText == null || smsText.isEmpty()) {
            Log.d(TAG, "processSMSText: Input is null or empty");
            return null;
        }
        
        Log.d(TAG, "processSMSText: Processing SMS text: " + smsText);
        
        // For SMS, we only care about FT IDs
        String ftId = extractFTFromSMS(smsText);
        if (ftId != null) {
            Log.d(TAG, "processSMSText: Successfully extracted FT ID: " + ftId);
            return ftId;
        }
        
        Log.d(TAG, "processSMSText: No FT ID found in SMS text");
        return null;
    }
}