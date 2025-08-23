package com.example.fetanverify;

import android.util.Base64;
import android.util.Log;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.List;

/**
 * Professional Transaction ID Extractor with robust pattern matching
 */
public class TransactionExtractor {
    private static final String TAG = "TransactionExtractor";
    
    // Comprehensive regex patterns for all transaction ID formats
    private static final Pattern FT_PATTERN = Pattern.compile("FT[A-Z0-9]{8,12}", Pattern.CASE_INSENSITIVE);
    private static final Pattern CH_PATTERN = Pattern.compile("CH[A-Z0-9]{6,10}", Pattern.CASE_INSENSITIVE);
    private static final Pattern CHA_PATTERN = Pattern.compile("CHA[A-Z0-9]{5,9}", Pattern.CASE_INSENSITIVE);
    private static final Pattern CHE_PATTERN = Pattern.compile("CHE[A-Z0-9]{5,9}", Pattern.CASE_INSENSITIVE);
    
    // Combined pattern for efficient searching
    private static final Pattern ALL_PATTERNS = Pattern.compile(
        "(FT[A-Z0-9]{8,12})|(CH[A-Z0-9]{6,10})|(CHA[A-Z0-9]{5,9})|(CHE[A-Z0-9]{5,9})", 
        Pattern.CASE_INSENSITIVE
    );
    
    /**
     * Main extraction method with multiple strategies
     */
    public static String extractTransactionId(String input) {
        if (input == null || input.trim().isEmpty()) {
            Log.d(TAG, "Input is null or empty");
            return null;
        }
        
        String cleanInput = input.trim();
        Log.d(TAG, "Processing input: " + cleanInput.substring(0, Math.min(50, cleanInput.length())) + "...");
        
        // Strategy 1: Direct pattern matching (most common case)
        String directResult = extractDirectPatterns(cleanInput);
        if (directResult != null) {
            Log.d(TAG, "Direct pattern match found: " + directResult);
            return directResult;
        }
        
        // Strategy 2: Base64 decoding (for QR codes)
        if (isLikelyBase64(cleanInput)) {
            String base64Result = extractFromBase64(cleanInput);
            if (base64Result != null) {
                Log.d(TAG, "Base64 extraction successful: " + base64Result);
                return base64Result;
            }
        }
        
        // Strategy 3: Hex string processing
        if (isLikelyHex(cleanInput)) {
            String hexResult = extractFromHex(cleanInput);
            if (hexResult != null) {
                Log.d(TAG, "Hex extraction successful: " + hexResult);
                return hexResult;
            }
        }
        
        // Strategy 4: SMS text processing with priority logic
        String smsResult = extractFromSMSText(cleanInput);
        if (smsResult != null) {
            Log.d(TAG, "SMS extraction successful: " + smsResult);
            return smsResult;
        }
        
        Log.d(TAG, "No transaction ID found in input");
        return null;
    }
    
    /**
     * Extract using direct pattern matching
     */
    private static String extractDirectPatterns(String text) {
        String upperText = text.toUpperCase();
        
        List<String> ftIds = new ArrayList<>();
        List<String> chIds = new ArrayList<>();
        
        Matcher matcher = ALL_PATTERNS.matcher(upperText);
        while (matcher.find()) {
            String match = matcher.group().toUpperCase();
            if (match.startsWith("FT")) {
                ftIds.add(match);
            } else if (match.startsWith("CH")) {
                chIds.add(match);
            }
        }
        
        Log.d(TAG, "Direct search - FT IDs: " + ftIds.size() + ", CH IDs: " + chIds.size());
        
        // Apply priority logic: FT > CH
        if (!ftIds.isEmpty()) {
            return ftIds.get(0);
        } else if (!chIds.isEmpty()) {
            return chIds.get(0);
        }
        
        return null;
    }
    
    /**
     * Extract from Base64 encoded data
     */
    private static String extractFromBase64(String base64Data) {
        try {
            Log.d(TAG, "Attempting Base64 decode");
            byte[] decodedBytes = Base64.decode(base64Data, Base64.DEFAULT);
            
            // Convert to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : decodedBytes) {
                hexString.append(String.format("%02X", b & 0xFF));
            }
            String hexData = hexString.toString();
            Log.d(TAG, "Base64 decoded to hex: " + hexData);
            
            // Try to extract from hex
            String hexResult = extractFromHex(hexData);
            if (hexResult != null) {
                return hexResult;
            }
            
            // Try to convert to ASCII and extract
            String asciiData = hexToAscii(hexData);
            if (asciiData != null && !asciiData.trim().isEmpty()) {
                Log.d(TAG, "Base64 converted to ASCII: " + asciiData);
                return extractDirectPatterns(asciiData);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error processing Base64 data: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Extract from hex data with comprehensive pattern recognition
     */
    private static String extractFromHex(String hexData) {
        try {
            String upperHex = hexData.toUpperCase();
            Log.d(TAG, "Processing hex data: " + upperHex.substring(0, Math.min(100, upperHex.length())) + "...");
            
            // Look for specific hex patterns that represent transaction IDs
            
            // FT pattern in hex: 4654 + hex chars
            Pattern ftHexPattern = Pattern.compile("4654[A-F0-9]{16,24}");
            Matcher ftHexMatcher = ftHexPattern.matcher(upperHex);
            if (ftHexMatcher.find()) {
                String ftHex = ftHexMatcher.group();
                String ftAscii = hexToAscii(ftHex);
                if (ftAscii != null && FT_PATTERN.matcher(ftAscii).matches()) {
                    Log.d(TAG, "Found FT in hex: " + ftAscii);
                    return ftAscii;
                }
            }
            
            // CH pattern in hex: 4348 + hex chars
            Pattern chHexPattern = Pattern.compile("4348[A-F0-9]{12,20}");
            Matcher chHexMatcher = chHexPattern.matcher(upperHex);
            if (chHexMatcher.find()) {
                String chHex = chHexMatcher.group();
                String chAscii = hexToAscii(chHex);
                if (chAscii != null && CH_PATTERN.matcher(chAscii).matches()) {
                    Log.d(TAG, "Found CH in hex: " + chAscii);
                    return chAscii;
                }
            }
            
            // CHA pattern in hex: 434841 + hex chars
            Pattern chaHexPattern = Pattern.compile("434841[A-F0-9]{10,18}");
            Matcher chaHexMatcher = chaHexPattern.matcher(upperHex);
            if (chaHexMatcher.find()) {
                String chaHex = chaHexMatcher.group();
                String chaAscii = hexToAscii(chaHex);
                if (chaAscii != null && CHA_PATTERN.matcher(chaAscii).matches()) {
                    Log.d(TAG, "Found CHA in hex: " + chaAscii);
                    return chaAscii;
                }
            }
            
            // CHE pattern in hex: 434845 + hex chars
            Pattern cheHexPattern = Pattern.compile("434845[A-F0-9]{10,18}");
            Matcher cheHexMatcher = cheHexPattern.matcher(upperHex);
            if (cheHexMatcher.find()) {
                String cheHex = cheHexMatcher.group();
                String cheAscii = hexToAscii(cheHex);
                if (cheAscii != null && CHE_PATTERN.matcher(cheAscii).matches()) {
                    Log.d(TAG, "Found CHE in hex: " + cheAscii);
                    return cheAscii;
                }
            }
            
            // Fallback: convert entire hex to ASCII and search
            String fullAscii = hexToAscii(upperHex);
            if (fullAscii != null && !fullAscii.trim().isEmpty()) {
                Log.d(TAG, "Hex to ASCII fallback: " + fullAscii);
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
    private static String extractFromSMSText(String smsText) {
        if (smsText == null || smsText.trim().isEmpty()) {
            return null;
        }
        
        String cleanText = smsText.replaceAll("\\s+", " ").toUpperCase();
        Log.d(TAG, "Processing SMS text: " + cleanText.substring(0, Math.min(100, cleanText.length())) + "...");
        
        List<String> ftIds = new ArrayList<>();
        List<String> chIds = new ArrayList<>();
        
        Matcher matcher = ALL_PATTERNS.matcher(cleanText);
        while (matcher.find()) {
            String match = matcher.group().toUpperCase();
            if (match.startsWith("FT")) {
                ftIds.add(match);
            } else if (match.startsWith("CH")) {
                chIds.add(match);
            }
        }
        
        Log.d(TAG, "SMS search - FT IDs: " + ftIds.size() + ", CH IDs: " + chIds.size());
        
        // Apply SMS priority logic:
        // If both CH and FT IDs are found → take only FT
        // If only CH ID is found → take CH
        // If only FT ID is found → take FT
        if (!ftIds.isEmpty() && !chIds.isEmpty()) {
            Log.d(TAG, "Both FT and CH found, returning FT: " + ftIds.get(0));
            return ftIds.get(0);
        } else if (!ftIds.isEmpty()) {
            Log.d(TAG, "Only FT found: " + ftIds.get(0));
            return ftIds.get(0);
        } else if (!chIds.isEmpty()) {
            Log.d(TAG, "Only CH found: " + chIds.get(0));
            return chIds.get(0);
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
     * Check if string is likely Base64
     */
    private static boolean isLikelyBase64(String str) {
        if (str == null || str.length() < 4) {
            return false;
        }
        
        try {
            String cleaned = str.replaceAll("\\s+", "");
            if (cleaned.length() % 4 != 0) {
                return false;
            }
            
            // Check if it matches Base64 pattern
            if (!cleaned.matches("^[A-Za-z0-9+/]*={0,2}$")) {
                return false;
            }
            
            // Try to decode to verify
            Base64.decode(cleaned, Base64.DEFAULT);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Check if string is likely hex
     */
    private static boolean isLikelyHex(String str) {
        if (str == null || str.length() < 2) {
            return false;
        }
        
        return str.matches("^[0-9A-Fa-f]+$") && str.length() % 2 == 0 && str.length() > 10;
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
    
    /**
     * Validate transaction ID format
     */
    public static boolean isValidTransactionId(String transactionId) {
        if (transactionId == null || transactionId.trim().isEmpty()) {
            return false;
        }
        
        String upperTxId = transactionId.trim().toUpperCase();
        return FT_PATTERN.matcher(upperTxId).matches() || 
               CH_PATTERN.matcher(upperTxId).matches() || 
               CHA_PATTERN.matcher(upperTxId).matches() || 
               CHE_PATTERN.matcher(upperTxId).matches();
    }
}