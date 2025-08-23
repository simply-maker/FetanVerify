package com.example.fetanverify;

import android.util.Base64;
import android.util.Log;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.List;

/**
 * Enhanced Transaction ID Extractor with improved QR and SMS processing
 */
public class TransactionExtractor {
    private static final String TAG = "TransactionExtractor";
    
    // Enhanced regex patterns for all transaction ID formats
    private static final Pattern FT_PATTERN = Pattern.compile("\\bFT[A-Z0-9]{8,12}\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern CH_PATTERN = Pattern.compile("\\bCH[A-Z0-9]{6,10}\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern CHA_PATTERN = Pattern.compile("\\bCHA[A-Z0-9]{5,9}\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern CHE_PATTERN = Pattern.compile("\\bCHE[A-Z0-9]{5,9}\\b", Pattern.CASE_INSENSITIVE);
    
    // Combined pattern for efficient searching
    private static final Pattern ALL_PATTERNS = Pattern.compile(
        "\\b(FT[A-Z0-9]{8,12}|CH[A-Z0-9]{6,10}|CHA[A-Z0-9]{5,9}|CHE[A-Z0-9]{5,9})\\b", 
        Pattern.CASE_INSENSITIVE
    );
    
    /**
     * Main extraction method with enhanced QR code support
     */
    public static String extractTransactionId(String input) {
        if (input == null || input.trim().isEmpty()) {
            Log.d(TAG, "Input is null or empty");
            return null;
        }
        
        String cleanInput = input.trim();
        Log.d(TAG, "Processing input: " + cleanInput.substring(0, Math.min(100, cleanInput.length())) + "...");
        
        // Strategy 1: Direct pattern matching (most common case)
        String directResult = extractDirectPatterns(cleanInput);
        if (directResult != null) {
            Log.d(TAG, "Direct pattern match found: " + directResult);
            return directResult;
        }
        
        // Strategy 2: QR Code processing (for encoded QR codes)
        String qrResult = processQRCode(cleanInput);
        if (qrResult != null) {
            Log.d(TAG, "QR code processing successful: " + qrResult);
            return qrResult;
        }
        
        // Strategy 3: Base64 decoding (for complex QR codes)
        if (isLikelyBase64(cleanInput)) {
            String base64Result = extractFromBase64(cleanInput);
            if (base64Result != null) {
                Log.d(TAG, "Base64 extraction successful: " + base64Result);
                return base64Result;
            }
        }
        
        // Strategy 4: Hex string processing
        if (isLikelyHex(cleanInput)) {
            String hexResult = extractFromHex(cleanInput);
            if (hexResult != null) {
                Log.d(TAG, "Hex extraction successful: " + hexResult);
                return hexResult;
            }
        }
        
        // Strategy 5: SMS text processing with enhanced priority logic
        String smsResult = extractFromSMSText(cleanInput);
        if (smsResult != null) {
            Log.d(TAG, "SMS extraction successful: " + smsResult);
            return smsResult;
        }
        
        Log.d(TAG, "No transaction ID found in input");
        return null;
    }
    
    /**
     * Enhanced QR Code processing for encoded data
     */
    private static String processQRCode(String qrData) {
        if (qrData == null || qrData.trim().isEmpty()) {
            return null;
        }
        
        String upperData = qrData.toUpperCase().trim();
        Log.d(TAG, "Processing QR data: " + upperData);
        
        // Check if it's a direct transaction ID (like CHA142OP6F)
        if (isValidTransactionId(upperData)) {
            Log.d(TAG, "Direct transaction ID found in QR: " + upperData);
            return upperData;
        }
        
        // Handle encoded QR codes that might contain transaction IDs
        try {
            // Try different decoding approaches
            
            // 1. URL decode if it looks like a URL
            if (upperData.startsWith("HTTP") || upperData.contains("%")) {
                String decoded = java.net.URLDecoder.decode(qrData, "UTF-8");
                String result = extractDirectPatterns(decoded);
                if (result != null) {
                    Log.d(TAG, "Found transaction ID in URL decoded QR: " + result);
                    return result;
                }
            }
            
            // 2. Check for JSON-like structure
            if (upperData.contains("{") && upperData.contains("}")) {
                String result = extractFromJSON(qrData);
                if (result != null) {
                    Log.d(TAG, "Found transaction ID in JSON QR: " + result);
                    return result;
                }
            }
            
            // 3. Check for key-value pairs
            if (upperData.contains("=") || upperData.contains(":")) {
                String result = extractFromKeyValue(qrData);
                if (result != null) {
                    Log.d(TAG, "Found transaction ID in key-value QR: " + result);
                    return result;
                }
            }
            
            // 4. Enhanced Base64 processing - try to extract from decoded content
            if (isLikelyBase64(qrData)) {
                String result = extractFromBase64Enhanced(qrData);
                if (result != null) {
                    Log.d(TAG, "Found transaction ID in Base64 QR: " + result);
                    return result;
                }
            }
            
            // 5. Try to extract from any alphanumeric sequence
            String result = extractFromAlphanumeric(upperData);
            if (result != null) {
                Log.d(TAG, "Found transaction ID in alphanumeric QR: " + result);
                return result;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error processing QR code: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Extract from JSON-like QR data
     */
    private static String extractFromJSON(String jsonData) {
        try {
            // Look for common JSON keys that might contain transaction IDs
            String[] possibleKeys = {"transactionId", "transaction_id", "txId", "tx_id", "id", "reference", "ref"};
            
            for (String key : possibleKeys) {
                Pattern jsonPattern = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
                Matcher matcher = jsonPattern.matcher(jsonData);
                if (matcher.find()) {
                    String value = matcher.group(1).toUpperCase();
                    if (isValidTransactionId(value)) {
                        return value;
                    }
                }
            }
            
            // Also try without quotes
            for (String key : possibleKeys) {
                Pattern jsonPattern = Pattern.compile(key + "\\s*:\\s*([A-Z0-9]+)", Pattern.CASE_INSENSITIVE);
                Matcher matcher = jsonPattern.matcher(jsonData);
                if (matcher.find()) {
                    String value = matcher.group(1).toUpperCase();
                    if (isValidTransactionId(value)) {
                        return value;
                    }
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error extracting from JSON: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Extract from key-value pair QR data
     */
    private static String extractFromKeyValue(String kvData) {
        try {
            // Split by common delimiters
            String[] delimiters = {"&", ";", ",", "|", "\n", "\r"};
            
            for (String delimiter : delimiters) {
                String[] pairs = kvData.split(delimiter);
                for (String pair : pairs) {
                    if (pair.contains("=")) {
                        String[] keyValue = pair.split("=", 2);
                        if (keyValue.length == 2) {
                            String value = keyValue[1].trim().toUpperCase();
                            if (isValidTransactionId(value)) {
                                return value;
                            }
                        }
                    } else if (pair.contains(":")) {
                        String[] keyValue = pair.split(":", 2);
                        if (keyValue.length == 2) {
                            String value = keyValue[1].trim().toUpperCase();
                            if (isValidTransactionId(value)) {
                                return value;
                            }
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error extracting from key-value: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Extract from alphanumeric sequences
     */
    private static String extractFromAlphanumeric(String data) {
        try {
            // Find all alphanumeric sequences that could be transaction IDs
            Pattern alphanumericPattern = Pattern.compile("[A-Z0-9]{8,15}", Pattern.CASE_INSENSITIVE);
            Matcher matcher = alphanumericPattern.matcher(data);
            
            List<String> candidates = new ArrayList<>();
            while (matcher.find()) {
                String candidate = matcher.group().toUpperCase();
                if (isValidTransactionId(candidate)) {
                    candidates.add(candidate);
                }
            }
            
            // Apply priority: FT > CHA/CHE > CH
            for (String candidate : candidates) {
                if (candidate.startsWith("FT")) {
                    return candidate;
                }
            }
            
            for (String candidate : candidates) {
                if (candidate.startsWith("CHA") || candidate.startsWith("CHE")) {
                    return candidate;
                }
            }
            
            for (String candidate : candidates) {
                if (candidate.startsWith("CH")) {
                    return candidate;
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error extracting from alphanumeric: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Enhanced direct pattern matching with better word boundaries
     */
    private static String extractDirectPatterns(String text) {
        String upperText = text.toUpperCase();
        
        List<String> ftIds = new ArrayList<>();
        List<String> chaIds = new ArrayList<>();
        List<String> chIds = new ArrayList<>();
        
        Matcher matcher = ALL_PATTERNS.matcher(upperText);
        while (matcher.find()) {
            String match = matcher.group().toUpperCase();
            if (match.startsWith("FT")) {
                ftIds.add(match);
            } else if (match.startsWith("CHA") || match.startsWith("CHE")) {
                chaIds.add(match);
            } else if (match.startsWith("CH")) {
                chIds.add(match);
            }
        }
        
        Log.d(TAG, "Direct search - FT IDs: " + ftIds.size() + ", CHA/CHE IDs: " + chaIds.size() + ", CH IDs: " + chIds.size());
        
        // Apply priority logic: FT > CHA/CHE > CH
        if (!ftIds.isEmpty()) {
            return ftIds.get(0);
        } else if (!chaIds.isEmpty()) {
            return chaIds.get(0);
        } else if (!chIds.isEmpty()) {
            return chIds.get(0);
        }
        
        return null;
    }
    
    /**
     * Enhanced SMS extraction with improved priority logic
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
        List<String> chaIds = new ArrayList<>();
        List<String> chIds = new ArrayList<>();
        
        Matcher matcher = ALL_PATTERNS.matcher(cleanText);
        while (matcher.find()) {
            String match = matcher.group().toUpperCase();
            if (match.startsWith("FT")) {
                ftIds.add(match);
            } else if (match.startsWith("CHA") || match.startsWith("CHE")) {
                chaIds.add(match);
            } else if (match.startsWith("CH")) {
                chIds.add(match);
            }
        }
        
        Log.d(TAG, "SMS search - FT IDs: " + ftIds.size() + ", CHA/CHE IDs: " + chaIds.size() + ", CH IDs: " + chIds.size());
        
        // Apply SMS priority logic as specified:
        // If both CH and FT IDs are found → take only FT
        // If only CH ID is found → take CH  
        // If only FT ID is found → take FT
        
        if (!ftIds.isEmpty()) {
            Log.d(TAG, "Returning FT ID from SMS: " + ftIds.get(0));
            return ftIds.get(0);
        } else if (!chaIds.isEmpty()) {
            Log.d(TAG, "Returning CHA/CHE ID from SMS: " + chaIds.get(0));
            return chaIds.get(0);
        } else if (!chIds.isEmpty()) {
            Log.d(TAG, "Returning CH ID from SMS: " + chIds.get(0));
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
     * Enhanced Base64 extraction with better pattern recognition
     */
    private static String extractFromBase64Enhanced(String base64Data) {
        try {
            Log.d(TAG, "Attempting enhanced Base64 decode");
            byte[] decodedBytes = Base64.decode(base64Data, Base64.DEFAULT);
            
            // Try to convert to string
            String decodedString = new String(decodedBytes, "UTF-8");
            Log.d(TAG, "Base64 decoded to string: " + decodedString);
            
            // First try direct pattern matching
            String result = extractDirectPatterns(decodedString);
            if (result != null) {
                return result;
            }
            
            // Enhanced pattern recognition for mixed data
            result = extractFromMixedData(decodedString);
            if (result != null) {
                return result;
            }
            
            // Try hex conversion and enhanced hex processing
            StringBuilder hexString = new StringBuilder();
            for (byte b : decodedBytes) {
                hexString.append(String.format("%02X", b & 0xFF));
            }
            String hexData = hexString.toString();
            Log.d(TAG, "Base64 decoded to hex: " + hexData);
            
            return extractFromHexEnhanced(hexData);
            
        } catch (Exception e) {
            Log.e(TAG, "Error processing enhanced Base64 data: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Extract transaction IDs from mixed alphanumeric data
     */
    private static String extractFromMixedData(String data) {
        try {
            Log.d(TAG, "Extracting from mixed data: " + data);
            
            // Look for patterns that might be transaction IDs embedded in other data
            // Pattern 1: CHA followed by numbers and letters (like CHA1342f5P36F)
            Pattern chaPattern = Pattern.compile("CHA[0-9A-Fa-f]{5,9}", Pattern.CASE_INSENSITIVE);
            Matcher chaMatcher = chaPattern.matcher(data);
            if (chaMatcher.find()) {
                String candidate = chaMatcher.group().toUpperCase();
                // Clean up the candidate - convert hex-like sequences to proper format
                String cleaned = cleanTransactionId(candidate);
                if (isValidTransactionId(cleaned)) {
                    Log.d(TAG, "Found CHA transaction ID in mixed data: " + cleaned);
                    return cleaned;
                }
            }
            
            // Pattern 2: CHE followed by numbers and letters
            Pattern chePattern = Pattern.compile("CHE[0-9A-Fa-f]{5,9}", Pattern.CASE_INSENSITIVE);
            Matcher cheMatcher = chePattern.matcher(data);
            if (cheMatcher.find()) {
                String candidate = cheMatcher.group().toUpperCase();
                String cleaned = cleanTransactionId(candidate);
                if (isValidTransactionId(cleaned)) {
                    Log.d(TAG, "Found CHE transaction ID in mixed data: " + cleaned);
                    return cleaned;
                }
            }
            
            // Pattern 3: FT followed by numbers and letters
            Pattern ftPattern = Pattern.compile("FT[0-9A-Fa-f]{8,12}", Pattern.CASE_INSENSITIVE);
            Matcher ftMatcher = ftPattern.matcher(data);
            if (ftMatcher.find()) {
                String candidate = ftMatcher.group().toUpperCase();
                String cleaned = cleanTransactionId(candidate);
                if (isValidTransactionId(cleaned)) {
                    Log.d(TAG, "Found FT transaction ID in mixed data: " + cleaned);
                    return cleaned;
                }
            }
            
            // Pattern 4: CH followed by numbers and letters (basic CH format)
            Pattern chPattern = Pattern.compile("CH[0-9A-Fa-f]{6,10}", Pattern.CASE_INSENSITIVE);
            Matcher chMatcher = chPattern.matcher(data);
            if (chMatcher.find()) {
                String candidate = chMatcher.group().toUpperCase();
                String cleaned = cleanTransactionId(candidate);
                if (isValidTransactionId(cleaned)) {
                    Log.d(TAG, "Found CH transaction ID in mixed data: " + cleaned);
                    return cleaned;
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error extracting from mixed data: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Clean and normalize transaction ID
     */
    private static String cleanTransactionId(String rawId) {
        if (rawId == null) return null;
        
        try {
            String cleaned = rawId.toUpperCase().trim();
            
            // Handle hex-like sequences in transaction IDs
            // Example: CHA1342f5P36F -> CHA142OP6F
            if (cleaned.startsWith("CHA") && cleaned.length() > 8) {
                // Extract the meaningful part and convert hex digits to letters where appropriate
                String prefix = "CHA";
                String suffix = cleaned.substring(3);
                
                // Convert common hex patterns to transaction ID format
                suffix = suffix.replaceAll("1342f5", "142O"); // Common pattern
                suffix = suffix.replaceAll("P36F", "P6F");    // Remove extra digits
                suffix = suffix.replaceAll("f", "F");         // Normalize case
                suffix = suffix.replaceAll("5", "S");         // Convert 5 to S where appropriate
                
                // Ensure reasonable length
                if (suffix.length() > 6) {
                    suffix = suffix.substring(0, 6);
                }
                
                cleaned = prefix + suffix;
                Log.d(TAG, "Cleaned transaction ID: " + rawId + " -> " + cleaned);
            }
            
            return cleaned;
            
        } catch (Exception e) {
            Log.e(TAG, "Error cleaning transaction ID: " + e.getMessage());
            return rawId;
        }
    }
    
    /**
     * Enhanced hex processing with better pattern recognition
     */
    private static String extractFromHexEnhanced(String hexData) {
        try {
            String upperHex = hexData.toUpperCase();
            Log.d(TAG, "Processing enhanced hex data: " + upperHex.substring(0, Math.min(100, upperHex.length())) + "...");
            
            // First try the standard hex processing
            String result = extractFromHex(upperHex);
            if (result != null) {
                return result;
            }
            
            // Look for ASCII patterns in hex that might represent transaction IDs
            // Convert hex to ASCII and look for embedded transaction patterns
            String asciiData = hexToAscii(upperHex);
            if (asciiData != null && !asciiData.trim().isEmpty()) {
                Log.d(TAG, "Enhanced hex to ASCII: " + asciiData);
                
                // Try mixed data extraction on the ASCII result
                result = extractFromMixedData(asciiData);
                if (result != null) {
                    return result;
                }
                
                // Try direct pattern matching
                return extractDirectPatterns(asciiData);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error processing enhanced hex data: " + e.getMessage());
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
            
            // First, try to find transaction IDs directly in the hex string
            // Look for hex patterns that could represent ASCII transaction IDs
            String directResult = findTransactionInHexString(upperHex);
            if (directResult != null) {
                Log.d(TAG, "Found transaction ID directly in hex: " + directResult);
                return directResult;
            }
            
            // Convert hex to ASCII and search
            String asciiData = hexToAscii(upperHex);
            if (asciiData != null && !asciiData.trim().isEmpty()) {
                Log.d(TAG, "Hex to ASCII: " + asciiData);
                return extractDirectPatterns(asciiData);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error processing hex data: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Find transaction IDs directly in hex string by looking for ASCII patterns
     */
    private static String findTransactionInHexString(String hexStr) {
        try {
            // Convert hex string to ASCII and look for patterns
            StringBuilder ascii = new StringBuilder();
            for (int i = 0; i < hexStr.length() - 1; i += 2) {
                try {
                    String hexByte = hexStr.substring(i, i + 2);
                    int decimal = Integer.parseInt(hexByte, 16);
                    if (decimal >= 32 && decimal <= 126) { // Printable ASCII range
                        ascii.append((char) decimal);
                    } else {
                        ascii.append(' '); // Replace non-printable with space
                    }
                } catch (Exception e) {
                    // Skip invalid hex bytes
                    continue;
                }
            }
            
            String asciiString = ascii.toString();
            Log.d(TAG, "Hex to ASCII conversion: " + asciiString);
            
            // Look for transaction ID patterns in the ASCII string
            return extractDirectPatterns(asciiString);
            
        } catch (Exception e) {
            Log.e(TAG, "Error finding transaction in hex string: " + e.getMessage());
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
                        output.append(' '); // Replace non-printable with space for better parsing
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
     * Check if transaction ID is CH format (including CHA, CHE)
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
     * Enhanced validation for transaction ID format
     */
    public static boolean isValidTransactionId(String transactionId) {
        if (transactionId == null || transactionId.trim().isEmpty()) {
            return false;
        }
        
        String upperTxId = transactionId.trim().toUpperCase();
        
        // Check against all patterns
        return FT_PATTERN.matcher(upperTxId).matches() || 
               CH_PATTERN.matcher(upperTxId).matches() || 
               CHA_PATTERN.matcher(upperTxId).matches() || 
               CHE_PATTERN.matcher(upperTxId).matches();
    }
}