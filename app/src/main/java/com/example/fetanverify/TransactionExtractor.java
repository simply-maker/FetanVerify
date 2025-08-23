package com.example.fetanverify;

import android.util.Base64;
import android.util.Log;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.List;

/**
 * Robust transaction ID extractor with improved pattern matching
 */
public class TransactionExtractor {
    private static final String TAG = "TransactionExtractor";
    
    // Enhanced regex patterns with case insensitive matching
    private static final Pattern FT_PATTERN = Pattern.compile("FT[A-Z0-9]{10,12}", Pattern.CASE_INSENSITIVE);
    private static final Pattern CH_PATTERN = Pattern.compile("CH[A-Z0-9]{8,10}", Pattern.CASE_INSENSITIVE);
    private static final Pattern CHA_PATTERN = Pattern.compile("CHA[A-Z0-9]{5,7}", Pattern.CASE_INSENSITIVE);
    private static final Pattern CHE_PATTERN = Pattern.compile("CHE[A-Z0-9]{7,9}", Pattern.CASE_INSENSITIVE);
    
    /**
     * Main extraction method with fallback strategies
     */
    public static String extractTransactionId(String input) {
        if (input == null || input.trim().isEmpty()) {
            Log.d(TAG, "Input is null or empty");
            return null;
        }
        
        String cleanInput = input.trim();
        Log.d(TAG, "Processing input: " + cleanInput.substring(0, Math.min(50, cleanInput.length())) + "...");
        
        // Strategy 1: Direct pattern matching (for plain text QR codes)
        String directMatch = extractDirectPatterns(cleanInput);
        if (directMatch != null) {
            Log.d(TAG, "Direct pattern match found: " + directMatch);
            return directMatch;
        }
        
        // Strategy 2: Base64 decoding (for encoded QR codes)
        if (isBase64(cleanInput)) {
            String base64Result = extractFromBase64(cleanInput);
            if (base64Result != null) {
                Log.d(TAG, "Base64 extraction successful: " + base64Result);
                return base64Result;
            }
        }
        
        // Strategy 3: SMS text processing with priority logic
        String smsResult = extractFromSMSWithPriority(cleanInput);
        if (smsResult != null) {
            Log.d(TAG, "SMS extraction successful: " + smsResult);
            return smsResult;
        }
        
        // Strategy 4: Hex string processing (fallback)
        if (isHexString(cleanInput)) {
            String hexResult = extractFromHexString(cleanInput);
            if (hexResult != null) {
                Log.d(TAG, "Hex extraction successful: " + hexResult);
                return hexResult;
            }
        }
        
        Log.d(TAG, "No transaction ID found in input");
        return null;
    }
    
    /**
     * Extract using direct pattern matching
     */
    private static String extractDirectPatterns(String text) {
        String upperText = text.toUpperCase();
        
        // Try FT pattern first
        Matcher ftMatcher = FT_PATTERN.matcher(upperText);
        if (ftMatcher.find()) {
            return ftMatcher.group();
        }
        
        // Try CH patterns
        Matcher chMatcher = CH_PATTERN.matcher(upperText);
        if (chMatcher.find()) {
            return chMatcher.group();
        }
        
        Matcher chaMatcher = CHA_PATTERN.matcher(upperText);
        if (chaMatcher.find()) {
            return chaMatcher.group();
        }
        
        Matcher cheMatcher = CHE_PATTERN.matcher(upperText);
        if (cheMatcher.find()) {
            return cheMatcher.group();
        }
        
        return null;
    }
    
    /**
     * Extract from Base64 encoded data
     */
    private static String extractFromBase64(String base64Data) {
        try {
            Log.d(TAG, "Decoding Base64 data");
            byte[] decodedBytes = Base64.decode(base64Data, Base64.DEFAULT);
            
            // Convert to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : decodedBytes) {
                hexString.append(String.format("%02X", b & 0xFF));
            }
            String hexData = hexString.toString();
            Log.d(TAG, "Decoded hex: " + hexData);
            
            // Try to extract from hex
            String hexResult = extractFromHexData(hexData);
            if (hexResult != null) {
                return hexResult;
            }
            
            // Try to convert to ASCII and extract
            String asciiData = hexToAscii(hexData);
            if (asciiData != null) {
                Log.d(TAG, "Converted to ASCII: " + asciiData);
                return extractDirectPatterns(asciiData);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error processing Base64 data: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Extract from hex data with improved pattern recognition
     */
    private static String extractFromHexData(String hexData) {
        try {
            // Look for hex patterns that represent our transaction IDs
            
            // FT pattern in hex: 4654 + 20-24 hex chars
            Pattern ftHexPattern = Pattern.compile("4654[A-F0-9]{20,24}");
            Matcher ftHexMatcher = ftHexPattern.matcher(hexData);
            if (ftHexMatcher.find()) {
                String ftHex = ftHexMatcher.group();
                String ftAscii = hexToAscii(ftHex);
                if (ftAscii != null && FT_PATTERN.matcher(ftAscii).matches()) {
                    return ftAscii;
                }
            }
            
            // CH pattern in hex: 4348 + 16-20 hex chars
            Pattern chHexPattern = Pattern.compile("4348[A-F0-9]{16,20}");
            Matcher chHexMatcher = chHexPattern.matcher(hexData);
            if (chHexMatcher.find()) {
                String chHex = chHexMatcher.group();
                String chAscii = hexToAscii(chHex);
                if (chAscii != null && CH_PATTERN.matcher(chAscii).matches()) {
                    return chAscii;
                }
            }
            
            // CHA pattern in hex: 434841 + 10-14 hex chars
            Pattern chaHexPattern = Pattern.compile("434841[A-F0-9]{10,14}");
            Matcher chaHexMatcher = chaHexPattern.matcher(hexData);
            if (chaHexMatcher.find()) {
                String chaHex = chaHexMatcher.group();
                String chaAscii = hexToAscii(chaHex);
                if (chaAscii != null && CHA_PATTERN.matcher(chaAscii).matches()) {
                    return chaAscii;
                }
            }
            
            // CHE pattern in hex: 434845 + 14-18 hex chars
            Pattern cheHexPattern = Pattern.compile("434845[A-F0-9]{14,18}");
            Matcher cheHexMatcher = cheHexPattern.matcher(hexData);
            if (cheHexMatcher.find()) {
                String cheHex = cheHexMatcher.group();
                String cheAscii = hexToAscii(cheHex);
                if (cheAscii != null && CHE_PATTERN.matcher(cheAscii).matches()) {
                    return cheAscii;
                }
            }
            
            // Fallback: convert entire hex to ASCII and search
            String fullAscii = hexToAscii(hexData);
            if (fullAscii != null) {
                return extractDirectPatterns(fullAscii);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error processing hex data: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * SMS extraction with priority logic
     */
    private static String extractFromSMSWithPriority(String smsText) {
        if (smsText == null || smsText.trim().isEmpty()) {
            return null;
        }
        
        String cleanText = smsText.replaceAll("\\s+", " ").toUpperCase();
        Log.d(TAG, "Processing SMS text with priority logic");
        
        // Find all FT IDs
        List<String> ftIds = new ArrayList<>();
        Matcher ftMatcher = FT_PATTERN.matcher(cleanText);
        while (ftMatcher.find()) {
            ftIds.add(ftMatcher.group());
        }
        
        // Find all CH IDs (including CHA and CHE)
        List<String> chIds = new ArrayList<>();
        
        Matcher chMatcher = CH_PATTERN.matcher(cleanText);
        while (chMatcher.find()) {
            chIds.add(chMatcher.group());
        }
        
        Matcher chaMatcher = CHA_PATTERN.matcher(cleanText);
        while (chaMatcher.find()) {
            chIds.add(chaMatcher.group());
        }
        
        Matcher cheMatcher = CHE_PATTERN.matcher(cleanText);
        while (cheMatcher.find()) {
            chIds.add(cheMatcher.group());
        }
        
        Log.d(TAG, "Found FT IDs: " + ftIds.size() + ", CH IDs: " + chIds.size());
        
        // Apply priority logic
        if (!ftIds.isEmpty() && !chIds.isEmpty()) {
            // Both found, return FT (higher priority)
            return ftIds.get(0);
        } else if (!ftIds.isEmpty()) {
            // Only FT found
            return ftIds.get(0);
        } else if (!chIds.isEmpty()) {
            // Only CH found
            return chIds.get(0);
        }
        
        return null;
    }
    
    /**
     * Extract from hex string
     */
    private static String extractFromHexString(String hexString) {
        return extractFromHexData(hexString.toUpperCase());
    }
    
    /**
     * Convert hex string to ASCII
     */
    private static String hexToAscii(String hexStr) {
        try {
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
            return output.toString();
        } catch (Exception e) {
            Log.e(TAG, "Error converting hex to ASCII: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Check if string is Base64
     */
    private static boolean isBase64(String str) {
        if (str == null || str.length() < 4) {
            return false;
        }
        
        try {
            String cleaned = str.replaceAll("\\s+", "");
            if (cleaned.length() % 4 != 0) {
                return false;
            }
            
            Base64.decode(cleaned, Base64.DEFAULT);
            return cleaned.matches("^[A-Za-z0-9+/]*={0,2}$");
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Check if string is hex
     */
    private static boolean isHexString(String str) {
        if (str == null || str.length() < 2) {
            return false;
        }
        
        return str.matches("^[0-9A-Fa-f]+$") && str.length() % 2 == 0;
    }
    
    /**
     * Check if transaction ID is CH format
     */
    public static boolean isCHFormat(String transactionId) {
        if (transactionId == null) return false;
        return CH_PATTERN.matcher(transactionId).matches() || 
               CHA_PATTERN.matcher(transactionId).matches() || 
               CHE_PATTERN.matcher(transactionId).matches();
    }
    
    /**
     * Check if transaction ID is FT format
     */
    public static boolean isFTFormat(String transactionId) {
        if (transactionId == null) return false;
        return FT_PATTERN.matcher(transactionId).matches();
    }
}