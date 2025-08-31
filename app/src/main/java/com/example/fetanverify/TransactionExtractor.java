package com.example.fetanverify;

import android.util.Base64;
import android.util.Log;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.List;

/**
 * Enhanced Transaction ID Extractor for CH and FT formats
 */
public class TransactionExtractor {
    private static final String TAG = "TransactionExtractor";
    
    // Exact format patterns
    private static final Pattern FT_PATTERN = Pattern.compile("\\bFT[A-Z0-9]{12}\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern CH_PATTERN = Pattern.compile("\\bCH[A-Z0-9]{10}\\b", Pattern.CASE_INSENSITIVE);
    
    /**
     * Main extraction method for both CH and FT formats
     */
    public static String extractTransactionId(String input) {
        if (input == null || input.trim().isEmpty()) {
            Log.d(TAG, "Input is null or empty");
            return null;
        }
        
        String cleanInput = input.trim();
        Log.d(TAG, "Processing input: " + cleanInput.substring(0, Math.min(100, cleanInput.length())) + "...");
        
        // Strategy 1: Direct pattern matching (for raw FT codes)
        String directResult = extractDirectPatterns(cleanInput);
        if (directResult != null) {
            Log.d(TAG, "Direct pattern match found: " + directResult);
            return directResult;
        }
        
        // Strategy 2: Base64 decoding (for encoded CH codes)
        if (isLikelyBase64(cleanInput)) {
            String base64Result = extractFromBase64(cleanInput);
            if (base64Result != null) {
                Log.d(TAG, "Base64 extraction successful: " + base64Result);
                return base64Result;
            }
        }
        
        // Strategy 3: Hex string processing (for encoded CH codes)
        if (isLikelyHex(cleanInput)) {
            String hexResult = extractFromHex(cleanInput);
            if (hexResult != null) {
                Log.d(TAG, "Hex extraction successful: " + hexResult);
                return hexResult;
            }
        }
        
        // Strategy 4: SMS text processing
        String smsResult = extractFromSMSText(cleanInput);
        if (smsResult != null) {
            Log.d(TAG, "SMS extraction successful: " + smsResult);
            return smsResult;
        }
        
        Log.d(TAG, "No transaction ID found in input");
        return null;
    }
    
    /**
     * Direct pattern matching for raw FT codes and any visible CH codes
     */
    private static String extractDirectPatterns(String text) {
        Log.d(TAG, "=== DIRECT PATTERN SEARCH ===");
        Log.d(TAG, "Input text: " + text);
        
        String upperText = text.toUpperCase();
        
        List<String> ftIds = new ArrayList<>();
        List<String> chIds = new ArrayList<>();
        
        // Look for FT patterns (12 characters)
        Matcher ftMatcher = FT_PATTERN.matcher(upperText);
        while (ftMatcher.find()) {
            String match = ftMatcher.group().toUpperCase();
            ftIds.add(match);
            Log.d(TAG, "Found FT ID: " + match);
        }
        
        // Look for CH patterns (10 characters)
        Matcher chMatcher = CH_PATTERN.matcher(upperText);
        while (chMatcher.find()) {
            String match = chMatcher.group().toUpperCase();
            chIds.add(match);
            Log.d(TAG, "Found CH ID: " + match);
        }
        
        Log.d(TAG, "Direct search - FT IDs: " + ftIds.size() + ", CH IDs: " + chIds.size());
        
        // Priority: FT > CH
        if (!ftIds.isEmpty()) {
            Log.d(TAG, "Returning FT ID: " + ftIds.get(0));
            return ftIds.get(0);
        } else if (!chIds.isEmpty()) {
            Log.d(TAG, "Returning CH ID: " + chIds.get(0));
            return chIds.get(0);
        }
        
        Log.d(TAG, "No transaction IDs found in direct pattern search");
        return null;
    }
    
    /**
     * Extract from SMS text with priority logic
     */
    private static String extractFromSMSText(String smsText) {
        if (smsText == null || smsText.trim().isEmpty()) {
            return null;
        }
        
        // Clean and normalize the SMS text
        String cleanText = smsText.replaceAll("\\s+", " ")
                                 .replaceAll("[^A-Za-z0-9\\s]", " ")
                                 .toUpperCase();
        
        Log.d(TAG, "Processing SMS text: " + cleanText.substring(0, Math.min(200, cleanText.length())) + "...");
        
        List<String> ftIds = new ArrayList<>();
        List<String> chIds = new ArrayList<>();
        
        Matcher ftMatcher = FT_PATTERN.matcher(cleanText);
        while (ftMatcher.find()) {
            ftIds.add(ftMatcher.group().toUpperCase());
        }
        
        Matcher chMatcher = CH_PATTERN.matcher(cleanText);
        while (chMatcher.find()) {
            chIds.add(chMatcher.group().toUpperCase());
        }
        
        Log.d(TAG, "SMS search - FT IDs: " + ftIds.size() + ", CH IDs: " + chIds.size());
        
        // Priority: FT > CH
        if (!ftIds.isEmpty()) {
            Log.d(TAG, "Returning FT ID from SMS: " + ftIds.get(0));
            return ftIds.get(0);
        } else if (!chIds.isEmpty()) {
            Log.d(TAG, "Returning CH ID from SMS: " + chIds.get(0));
            return chIds.get(0);
        }
        
        return null;
    }
    
    /**
     * Extract from Base64 encoded data (for CH codes)
     */
    private static String extractFromBase64(String base64Data) {
        try {
            Log.d(TAG, "Attempting Base64 decode");
            byte[] decodedBytes = Base64.decode(base64Data, Base64.DEFAULT);
            
            // Try to convert to string
            String decodedString = new String(decodedBytes, "UTF-8");
            Log.d(TAG, "Base64 decoded to string: " + decodedString);
            
            String result = extractDirectPatterns(decodedString);
            if (result != null) {
                return result;
            }
            
            // Try hex conversion
            StringBuilder hexString = new StringBuilder();
            for (byte b : decodedBytes) {
                hexString.append(String.format("%02X", b & 0xFF));
            }
            String hexData = hexString.toString();
            Log.d(TAG, "Base64 decoded to hex: " + hexData);
            
            return extractFromHex(hexData);
            
        } catch (Exception e) {
            Log.e(TAG, "Error processing Base64 data: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Extract from hex data (for CH codes)
     */
    private static String extractFromHex(String hexData) {
        try {
            String upperHex = hexData.toUpperCase();
            Log.d(TAG, "Processing hex data: " + upperHex.substring(0, Math.min(100, upperHex.length())) + "...");
            
            // First try direct pattern matching in hex string
            String directResult = extractDirectPatterns(upperHex);
            if (directResult != null) {
                Log.d(TAG, "Found transaction ID directly in hex: " + directResult);
                return directResult;
            }
            
            // Convert hex to ASCII and search for CH patterns
            String asciiData = hexToAscii(upperHex);
            if (asciiData != null && !asciiData.trim().isEmpty()) {
                Log.d(TAG, "Hex to ASCII: " + asciiData);
                
                // Enhanced CH extraction from ASCII
                String result = extractCHFromAscii(asciiData);
                if (result != null) {
                    return result;
                }
                
                // Try to find CH pattern in the middle of the ASCII string
                String cleanAscii = asciiData.replaceAll("[^A-Z0-9]", "");
                Log.d(TAG, "Clean ASCII for CH search: " + cleanAscii);
                
                // Look for CH followed by 10 alphanumeric characters
                for (int i = 0; i <= cleanAscii.length() - 12; i++) {
                    String candidate = cleanAscii.substring(i, i + 12);
                    if (candidate.startsWith("CH") && candidate.length() == 12) {
                        String chId = candidate.substring(0, 12);
                        if (isValidTransactionId(chId)) {
                            Log.d(TAG, "Found CH ID in clean ASCII: " + chId);
                            return chId;
                        }
                    }
                }
                
                // Fallback to direct patterns
                return extractDirectPatterns(asciiData);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error processing hex data: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Extract CH patterns specifically from ASCII data
     */
    private static String extractCHFromAscii(String asciiData) {
        Log.d(TAG, "=== CH ASCII EXTRACTION ===");
        Log.d(TAG, "Input ASCII: " + asciiData);
        
        try {
            // Enhanced CH pattern matching
            // Look for CH followed by exactly 10 alphanumeric characters
            Pattern chPattern = Pattern.compile("\\bCH[A-Z0-9]{10}\\b", Pattern.CASE_INSENSITIVE);
            Matcher matcher = chPattern.matcher(asciiData);
            
            if (matcher.find()) {
                String candidate = matcher.group().toUpperCase();
                Log.d(TAG, "Found CH candidate: " + candidate);
                
                if (isValidTransactionId(candidate)) {
                    Log.d(TAG, "Valid CH transaction ID: " + candidate);
                    return candidate;
                }
            }
            
            // If no exact match, try to find CH pattern without word boundaries
            Pattern relaxedChPattern = Pattern.compile("CH[A-Z0-9]{10}", Pattern.CASE_INSENSITIVE);
            Matcher relaxedMatcher = relaxedChPattern.matcher(asciiData);
            
            while (relaxedMatcher.find()) {
                String candidate = relaxedMatcher.group().toUpperCase();
                Log.d(TAG, "Found relaxed CH candidate: " + candidate);
                
                if (isValidTransactionId(candidate)) {
                    Log.d(TAG, "Valid relaxed CH transaction ID: " + candidate);
                    return candidate;
                }
            }
            
            Log.d(TAG, "No valid CH patterns found in ASCII");
            return null;
            
        } catch (Exception e) {
            Log.e(TAG, "Error extracting CH from ASCII: " + e.getMessage());
            return null;
        }
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
                    } else {
                        output.append(' '); // Replace non-printable with space
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
            
            if (!cleaned.matches("^[A-Za-z0-9+/]*={0,2}$")) {
                return false;
            }
            
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
        return CH_PATTERN.matcher(transactionId).matches();
    }
    
    /**
     * Check if transaction ID is FT format
     */
    public static boolean isFTFormat(String transactionId) {
        if (transactionId == null) return false;
        return FT_PATTERN.matcher(transactionId).matches();
    }
    
    /**
     * Validate transaction ID format (CH with 10 chars or FT with 12 chars)
     */
    public static boolean isValidTransactionId(String transactionId) {
        Log.d(TAG, "=== VALIDATING TRANSACTION ID ===");
        Log.d(TAG, "Input: " + transactionId);
        
        if (transactionId == null || transactionId.trim().isEmpty()) {
            Log.d(TAG, "Validation failed: null or empty");
            return false;
        }
        
        String upperTxId = transactionId.trim().toUpperCase();
        Log.d(TAG, "Uppercase for validation: " + upperTxId);
        
        // Check exact formats
        boolean ftMatch = FT_PATTERN.matcher(upperTxId).matches();
        boolean chMatch = CH_PATTERN.matcher(upperTxId).matches();
        
        Log.d(TAG, "FT pattern match (12 chars): " + ftMatch);
        Log.d(TAG, "CH pattern match (10 chars): " + chMatch);
        
        boolean isValid = ftMatch || chMatch;
        Log.d(TAG, "Final validation result: " + isValid);
        
        return isValid;
    }
}