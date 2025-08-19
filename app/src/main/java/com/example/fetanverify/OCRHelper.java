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
    private static final Pattern CH_PATTERN = Pattern.compile("CH[A-Z0-9]{8,12}");
    
    /**
     * Extract FT transaction ID from SMS text
     */
    public static String extractFTFromSMS(String smsText) {
        if (smsText == null || smsText.isEmpty()) {
            return null;
        }
        
        // Clean the text and convert to uppercase for better matching
        String cleanText = smsText.toUpperCase().replaceAll("\\s+", " ");
        Log.d(TAG, "Searching for FT ID in text: " + cleanText);
        
        Matcher ftMatcher = FT_PATTERN.matcher(cleanText);
        if (ftMatcher.find()) {
            String ftId = ftMatcher.group();
            Log.d(TAG, "Found FT ID: " + ftId);
            return ftId;
        }
        
        Log.d(TAG, "No FT ID found in SMS text");
        return null;
    }
    
    /**
     * Extract CH transaction ID from text
     */
    public static String extractCHFromText(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        
        String cleanText = text.toUpperCase().replaceAll("\\s+", " ");
        
        Matcher chMatcher = CH_PATTERN.matcher(cleanText);
        if (chMatcher.find()) {
            String chId = chMatcher.group();
            Log.d(TAG, "Found CH ID: " + chId);
            return chId;
        }
        
        return null;
    }
    
    /**
     * Convert hex string to ASCII
     */
    private static String hexToAscii(String hexStr) {
        try {
            StringBuilder output = new StringBuilder();
            for (int i = 0; i < hexStr.length(); i += 2) {
                String str = hexStr.substring(i, i + 2);
                int decimal = Integer.parseInt(str, 16);
                if (decimal >= 32 && decimal <= 126) { // Printable ASCII range
                    output.append((char) decimal);
                }
            }
            return output.toString();
        } catch (Exception e) {
            Log.e(TAG, "Error converting hex to ASCII: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Extract CH ID from hex-encoded data
     */
    private static String extractCHFromHex(String hexData) {
        try {
            // Look for the hex pattern that represents "CH" followed by alphanumeric characters
            // "CH" in ASCII hex is "4348"
            Pattern hexChPattern = Pattern.compile("4348[0-9A-F]+");
            Matcher matcher = hexChPattern.matcher(hexData.toUpperCase());
            
            if (matcher.find()) {
                String hexMatch = matcher.group();
                Log.d(TAG, "Found hex CH pattern: " + hexMatch);
                
                // Convert the hex to ASCII
                String asciiResult = hexToAscii(hexMatch);
                if (asciiResult != null && asciiResult.startsWith("CH")) {
                    // Extract just the CH ID part (CH + alphanumeric)
                    Pattern chIdPattern = Pattern.compile("CH[A-Z0-9]+");
                    Matcher chMatcher = chIdPattern.matcher(asciiResult);
                    if (chMatcher.find()) {
                        String chId = chMatcher.group();
                        // Limit to reasonable length
                        if (chId.length() <= 15) {
                            Log.d(TAG, "Extracted CH ID from hex: " + chId);
                            return chId;
                        }
                    }
                }
            }
            
            // Alternative approach: look for ASCII representation directly in hex
            String asciiFromHex = hexToAscii(hexData);
            if (asciiFromHex != null) {
                Log.d(TAG, "ASCII from hex: " + asciiFromHex);
                return extractCHFromText(asciiFromHex);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error extracting CH from hex: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Decode Base64 QR code data and extract transaction ID
     */
    public static String decodeBase64QR(String base64Data) {
        try {
            if (base64Data == null || base64Data.isEmpty()) {
                return null;
            }
            
            // Remove any whitespace
            base64Data = base64Data.replaceAll("\\s+", "");
            Log.d(TAG, "Decoding Base64: " + base64Data);
            
            // Decode Base64
            byte[] decodedBytes = Base64.decode(base64Data, Base64.DEFAULT);
            
            // Convert to hex string for analysis
            StringBuilder hexString = new StringBuilder();
            for (byte b : decodedBytes) {
                hexString.append(String.format("%02X", b & 0xFF));
            }
            String hexData = hexString.toString();
            Log.d(TAG, "Decoded to hex: " + hexData);
            
            // Try to extract CH ID from hex data
            String chId = extractCHFromHex(hexData);
            if (chId != null) {
                Log.d(TAG, "Successfully extracted CH ID: " + chId);
                return chId;
            }
            
            // Also try direct string conversion
            String decodedString = new String(decodedBytes);
            Log.d(TAG, "Decoded to string: " + decodedString);
            
            // Try to extract CH ID from decoded string
            String chFromString = extractCHFromText(decodedString);
            if (chFromString != null) {
                return chFromString;
            }
            
            // Try to extract FT ID from decoded string
            String ftFromString = extractFTFromSMS(decodedString);
            if (ftFromString != null) {
                return ftFromString;
            }
            
            return decodedString; // Return raw decoded string if no pattern matches
            
        } catch (Exception e) {
            Log.e(TAG, "Error decoding Base64: " + e.getMessage());
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
            return str.length() > 20 && str.matches("^[A-Za-z0-9+/]*={0,2}$");
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Process any scanned text and try to extract transaction ID
     */
    public static String processScannedText(String scannedText) {
        if (scannedText == null || scannedText.isEmpty()) {
            return null;
        }
        
        Log.d(TAG, "Processing scanned text: " + scannedText);
        
        // First check if it's Base64 encoded
        if (isBase64(scannedText)) {
            Log.d(TAG, "Detected Base64 encoded data");
            String decoded = decodeBase64QR(scannedText);
            if (decoded != null) {
                return decoded;
            }
        }
        
        // Try to extract FT ID first (priority for SMS)
        String ftId = extractFTFromSMS(scannedText);
        if (ftId != null) {
            return ftId;
        }
        
        // Try to extract CH ID
        String chId = extractCHFromText(scannedText);
        if (chId != null) {
            return chId;
        }
        
        // Return original text if no patterns match
        return scannedText.trim();
    }
}