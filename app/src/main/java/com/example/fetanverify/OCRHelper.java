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
     * Extract FT transaction ID from SMS text using OCR
     */
    public static String extractFTFromSMS(String smsText) {
        if (smsText == null || smsText.isEmpty()) {
            return null;
        }
        
        // Clean the text and convert to uppercase for better matching
        String cleanText = smsText.toUpperCase().replaceAll("\\s+", " ");
        
        Matcher ftMatcher = FT_PATTERN.matcher(cleanText);
        if (ftMatcher.find()) {
            String ftId = ftMatcher.group();
            Log.d(TAG, "Found FT ID: " + ftId);
            return ftId;
        }
        
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
     * Decode Base64 QR code data and extract transaction ID
     */
    public static String decodeBase64QR(String base64Data) {
        try {
            if (base64Data == null || base64Data.isEmpty()) {
                return null;
            }
            
            // Remove any whitespace
            base64Data = base64Data.replaceAll("\\s+", "");
            
            // Decode Base64
            byte[] decodedBytes = Base64.decode(base64Data, Base64.DEFAULT);
            String decodedString = new String(decodedBytes);
            
            Log.d(TAG, "Decoded Base64: " + decodedString);
            
            // Try to extract CH ID from decoded string
            String chId = extractCHFromText(decodedString);
            if (chId != null) {
                return chId;
            }
            
            // If no CH ID found, try FT ID
            String ftId = extractFTFromSMS(decodedString);
            if (ftId != null) {
                return ftId;
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
        
        // First check if it's Base64 encoded
        if (isBase64(scannedText)) {
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