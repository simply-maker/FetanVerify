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

    // Patterns: CH + 8 alphanumeric, FT + 10 alphanumeric, C + 9 alphanumeric
    private static final Pattern FT_PATTERN = Pattern.compile("FT[A-Z0-9]{10}", Pattern.CASE_INSENSITIVE);
    private static final Pattern CH_PATTERN = Pattern.compile("CH[A-Z0-9]{8}", Pattern.CASE_INSENSITIVE);
    private static final Pattern C_PATTERN = Pattern.compile("C[A-Z0-9]{9}", Pattern.CASE_INSENSITIVE);
    private static final Pattern URL_ID_PATTERN = Pattern.compile("id=([A-Za-z0-9]+)", Pattern.CASE_INSENSITIVE);

    /**
     * Main extraction method for CH, FT, and C formats
     */
    public static String extractTransactionId(String input) {
        if (input == null || input.trim().isEmpty()) {
            Log.d(TAG, "Input is null or empty");
            return null;
        }

        String cleanInput = input.trim();
        Log.d(TAG, "Processing input: " + cleanInput.substring(0, Math.min(100, cleanInput.length())) + "...");

        // Strategy 1: URL parsing for id parameter
        String urlResult = extractFromUrl(cleanInput);
        if (urlResult != null) {
            Log.d(TAG, "URL extraction successful: " + urlResult);
            return urlResult;
        }

        // Strategy 2: Direct pattern matching (for raw FT/CH/C codes)
        String directResult = extractDirectPatterns(cleanInput);
        if (directResult != null) {
            Log.d(TAG, "Direct pattern match found: " + directResult);
            return directResult;
        }

        // Strategy 3: Base64 decoding (for encoded CH/C codes)
        if (isLikelyBase64(cleanInput)) {
            String base64Result = extractFromBase64(cleanInput);
            if (base64Result != null) {
                Log.d(TAG, "Base64 extraction successful: " + base64Result);
                return base64Result;
            }
        }

        // Strategy 4: Hex string processing (for encoded CH/C codes)
        if (isLikelyHex(cleanInput)) {
            String hexResult = extractFromHex(cleanInput);
            if (hexResult != null) {
                Log.d(TAG, "Hex extraction successful: " + hexResult);
                return hexResult;
            }
        }

        // Strategy 5: SMS text processing
        String smsResult = extractFromSMSText(cleanInput);
        if (smsResult != null) {
            Log.d(TAG, "SMS extraction successful: " + smsResult);
            return smsResult;
        }

        Log.d(TAG, "No transaction ID found in input");
        return null;
    }

    /**
     * Extract transaction ID from URL id parameter
     */
    private static String extractFromUrl(String input) {
        Log.d(TAG, "=== URL ID EXTRACTION ===");
        Log.d(TAG, "Input text: " + input);

        if (!input.toLowerCase().startsWith("http")) {
            Log.d(TAG, "Input is not a URL");
            return null;
        }

        Matcher urlMatcher = URL_ID_PATTERN.matcher(input);
        if (urlMatcher.find()) {
            String id = urlMatcher.group(1).toUpperCase();
            Log.d(TAG, "Found potential ID in URL: " + id);

            // Validate the extracted ID
            Matcher ftMatcher = FT_PATTERN.matcher(id);
            Matcher chMatcher = CH_PATTERN.matcher(id);
            Matcher cMatcher = C_PATTERN.matcher(id);
            if (ftMatcher.matches()) {
                // For FT IDs, take first 13 characters (FT + 11 chars) to get FT25244VRX4G5
                String shortId = id.substring(0, Math.min(id.length(), 13));
                Log.d(TAG, "Valid FT ID from URL, using first 13 chars: " + shortId);
                return shortId;
            } else if (chMatcher.matches()) {
                Log.d(TAG, "Valid CH ID from URL: " + id);
                return id;
            } else if (cMatcher.matches()) {
                Log.d(TAG, "Valid C ID from URL: " + id);
                return id;
            } else {
                Log.d(TAG, "Extracted ID does not match FT, CH, or C pattern: " + id);
            }
        }

        Log.d(TAG, "No valid ID found in URL");
        return null;
    }

    /**
     * Direct pattern matching for raw FT, CH, and C codes
     */
    private static String extractDirectPatterns(String text) {
        Log.d(TAG, "=== DIRECT PATTERN SEARCH ===");
        Log.d(TAG, "Input text: " + text);

        String upperText = text.toUpperCase();

        List<String> ftIds = new ArrayList<>();
        List<String> chIds = new ArrayList<>();
        List<String> cIds = new ArrayList<>();

        // Look for FT patterns (12 characters: FT + 10 chars)
        Matcher ftMatcher = FT_PATTERN.matcher(upperText);
        while (ftMatcher.find()) {
            String match = ftMatcher.group().toUpperCase();
            ftIds.add(match);
            Log.d(TAG, "Found FT ID: " + match);
        }

        // Look for CH patterns (10 characters: CH + 8 chars)
        Matcher chMatcher = CH_PATTERN.matcher(upperText);
        while (chMatcher.find()) {
            String match = chMatcher.group().toUpperCase();
            chIds.add(match);
            Log.d(TAG, "Found CH ID: " + match);
        }

        // Look for C patterns (10 characters: C + 9 chars)
        Matcher cMatcher = C_PATTERN.matcher(upperText);
        while (cMatcher.find()) {
            String match = cMatcher.group().toUpperCase();
            cIds.add(match);
            Log.d(TAG, "Found C ID: " + match);
        }

        Log.d(TAG, "Direct search - FT IDs: " + ftIds.size() + ", CH IDs: " + chIds.size() + ", C IDs: " + cIds.size());

        // Priority: FT > CH > C
        if (!ftIds.isEmpty()) {
            Log.d(TAG, "Returning FT ID: " + ftIds.get(0));
            return ftIds.get(0);
        } else if (!chIds.isEmpty()) {
            Log.d(TAG, "Returning CH ID: " + chIds.get(0));
            return chIds.get(0);
        } else if (!cIds.isEmpty()) {
            Log.d(TAG, "Returning C ID: " + cIds.get(0));
            return cIds.get(0);
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

        Log.d(TAG, "Processing SMS text: " + smsText.substring(0, Math.min(200, smsText.length())) + "...");

        // Try URL parsing first if it looks like a URL
        if (smsText.toLowerCase().startsWith("http")) {
            String urlResult = extractFromUrl(smsText);
            if (urlResult != null) {
                return urlResult;
            }
        }

        // Clean and normalize the SMS text, preserving id= structure
        String cleanText = smsText.replaceAll("[^A-Za-z0-9\\s=]", " ")
                .replaceAll("\\s+", " ")
                .toUpperCase();

        // Try URL ID extraction on cleaned text
        Matcher urlMatcher = URL_ID_PATTERN.matcher(cleanText);
        if (urlMatcher.find()) {
            String id = urlMatcher.group(1).toUpperCase();
            Log.d(TAG, "Found potential ID in SMS URL: " + id);
            Matcher ftMatcher = FT_PATTERN.matcher(id);
            Matcher chMatcher = CH_PATTERN.matcher(id);
            Matcher cMatcher = C_PATTERN.matcher(id);
            if (ftMatcher.matches()) {
                // For FT IDs, take first 13 characters (FT + 11 chars) to get FT25244VRX4G5
                String shortId = id.substring(0, Math.min(id.length(), 13));
                Log.d(TAG, "Valid FT ID from SMS URL, using first 13 chars: " + shortId);
                return shortId;
            } else if (chMatcher.matches()) {
                Log.d(TAG, "Valid CH ID from SMS URL: " + id);
                return id;
            } else if (cMatcher.matches()) {
                Log.d(TAG, "Valid C ID from SMS URL: " + id);
                return id;
            }
        }

        List<String> ftIds = new ArrayList<>();
        List<String> chIds = new ArrayList<>();
        List<String> cIds = new ArrayList<>();

        Matcher ftMatcher = FT_PATTERN.matcher(cleanText);
        while (ftMatcher.find()) {
            ftIds.add(ftMatcher.group().toUpperCase());
        }

        Matcher chMatcher = CH_PATTERN.matcher(cleanText);
        while (chMatcher.find()) {
            chIds.add(chMatcher.group().toUpperCase());
        }

        Matcher cMatcher = C_PATTERN.matcher(cleanText);
        while (cMatcher.find()) {
            cIds.add(cMatcher.group().toUpperCase());
        }

        Log.d(TAG, "SMS search - FT IDs: " + ftIds.size() + ", CH IDs: " + chIds.size() + ", C IDs: " + cIds.size());

        // Priority: FT > CH > C
        if (!ftIds.isEmpty()) {
            Log.d(TAG, "Returning FT ID from SMS: " + ftIds.get(0));
            return ftIds.get(0);
        } else if (!chIds.isEmpty()) {
            Log.d(TAG, "Returning CH ID from SMS: " + chIds.get(0));
            return chIds.get(0);
        } else if (!cIds.isEmpty()) {
            Log.d(TAG, "Returning C ID from SMS: " + cIds.get(0));
            return cIds.get(0);
        }

        return null;
    }

    /**
     * Extract from Base64 encoded data (for CH and C codes)
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
            String hexData = hexString.toString().toUpperCase();
            Log.d(TAG, "Base64 decoded to hex: " + hexData);

            return extractFromHex(hexData);

        } catch (Exception e) {
            Log.e(TAG, "Error processing Base64 data: " + e.getMessage());
            return null;
        }
    }

    /**
     * Extract from hex data (for CH and C codes)
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

            // Convert hex to ASCII and search for CH/C patterns
            String asciiData = hexToAscii(upperHex);
            if (asciiData != null && !asciiData.trim().isEmpty()) {
                Log.d(TAG, "Hex to ASCII result: " + asciiData);

                // Enhanced CH/C extraction from ASCII
                String result = extractCHorCFromAscii(asciiData);
                if (result != null) {
                    return result;
                }

                // Clean ASCII and try to extract CH/C pattern
                String cleanAscii = asciiData.replaceAll("[^A-Z0-9]", "");
                Log.d(TAG, "Clean ASCII for CH/C search: " + cleanAscii);

                // Look for CH followed by 8 alphanumeric or C followed by 9 alphanumeric
                Pattern relaxedChPattern = Pattern.compile("(?:CH[A-Z0-9]{8}|C[A-Z0-9]{9})", Pattern.CASE_INSENSITIVE);
                Matcher matcher = relaxedChPattern.matcher(cleanAscii);
                if (matcher.find()) {
                    String candidate = matcher.group().toUpperCase();
                    if (isValidTransactionId(candidate)) {
                        Log.d(TAG, "Found CH/C ID in clean ASCII: " + candidate);
                        return candidate;
                    }
                }

                // Fallback to direct patterns
                return extractDirectPatterns(asciiData);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error processing hex data: " + e.getMessage());
            return null;
        }

        return null;
    }

    /**
     * Extract CH or C patterns specifically from ASCII data
     */
    private static String extractCHorCFromAscii(String asciiData) {
        Log.d(TAG, "=== CH/C ASCII EXTRACTION ===");
        Log.d(TAG, "Input ASCII: " + asciiData);

        try {
            String upperAscii = asciiData.toUpperCase();

            // Look for 0A marker and extract 20 hex characters (10 ASCII chars) or 22 hex characters (11 ASCII chars) after it
            int index = upperAscii.indexOf("0A");
            if (index != -1) {
                // Try C + 9 chars (11 ASCII chars = 22 hex chars)
                if (index + 22 <= upperAscii.length()) {
                    String potentialIdHex = upperAscii.substring(index + 2, index + 22);
                    Log.d(TAG, "Potential ID hex after 0A (C + 9): " + potentialIdHex);
                    String potentialId = hexToAscii(potentialIdHex);
                    if (potentialId != null) {
                        Matcher cMatcher = C_PATTERN.matcher(potentialId);
                        if (cMatcher.matches()) {
                            Log.d(TAG, "Valid C ID found after 0A: " + potentialId);
                            return potentialId.toUpperCase();
                        } else {
                            Log.d(TAG, "Extracted ID does not match C pattern: " + potentialId);
                        }
                    }
                }
                // Try CH + 8 chars (10 ASCII chars = 20 hex chars)
                if (index + 20 <= upperAscii.length()) {
                    String potentialIdHex = upperAscii.substring(index + 2, index + 20);
                    Log.d(TAG, "Potential ID hex after 0A (CH + 8): " + potentialIdHex);
                    String potentialId = hexToAscii(potentialIdHex);
                    if (potentialId != null) {
                        Matcher chMatcher = CH_PATTERN.matcher(potentialId);
                        if (chMatcher.matches()) {
                            Log.d(TAG, "Valid CH ID found after 0A: " + potentialId);
                            return potentialId.toUpperCase();
                        } else {
                            Log.d(TAG, "Extracted ID does not match CH pattern: " + potentialId);
                        }
                    }
                }
            }

            // Try strict CH or C pattern on the entire string
            Matcher chMatcher = CH_PATTERN.matcher(upperAscii);
            if (chMatcher.find()) {
                String candidate = chMatcher.group().toUpperCase();
                Log.d(TAG, "Found CH candidate: " + candidate);
                if (isValidTransactionId(candidate)) {
                    Log.d(TAG, "Valid CH transaction ID: " + candidate);
                    return candidate;
                }
            }

            Matcher cMatcher = C_PATTERN.matcher(upperAscii);
            if (cMatcher.find()) {
                String candidate = cMatcher.group().toUpperCase();
                Log.d(TAG, "Found C candidate: " + candidate);
                if (isValidTransactionId(candidate)) {
                    Log.d(TAG, "Valid C transaction ID: " + candidate);
                    return candidate;
                }
            }

            // Try relaxed pattern without word boundaries
            Pattern relaxedChOrCPattern = Pattern.compile("(?:CH[A-Z0-9]{8}|C[A-Z0-9]{9})", Pattern.CASE_INSENSITIVE);
            Matcher relaxedMatcher = relaxedChOrCPattern.matcher(upperAscii);
            if (relaxedMatcher.find()) {
                String candidate = relaxedMatcher.group().toUpperCase();
                Log.d(TAG, "Found relaxed CH/C candidate: " + candidate);
                if (isValidTransactionId(candidate)) {
                    Log.d(TAG, "Valid relaxed CH/C transaction ID: " + candidate);
                    return candidate;
                }
            }

            // Handle CHA prefix as a fallback for CH
            if (upperAscii.contains("CHA")) {
                String modifiedAscii = upperAscii.replace("CHA", "CH");
                Log.d(TAG, "Modified ASCII (CHA to CH): " + modifiedAscii);

                chMatcher = CH_PATTERN.matcher(modifiedAscii);
                if (chMatcher.find()) {
                    String candidate = chMatcher.group().toUpperCase();
                    Log.d(TAG, "Found CH candidate after CHA fix: " + candidate);
                    if (isValidTransactionId(candidate)) {
                        return candidate;
                    }
                }

                relaxedMatcher = relaxedChOrCPattern.matcher(modifiedAscii);
                if (relaxedMatcher.find()) {
                    String candidate = relaxedMatcher.group().toUpperCase();
                    Log.d(TAG, "Found relaxed CH/C candidate after CHA fix: " + candidate);
                    if (isValidTransactionId(candidate)) {
                        return candidate;
                    }
                }
            }

            // Additional fallback: Extract CHA + 7 characters for CH
            Pattern chaPattern = Pattern.compile("CHA[A-Z0-9]{7}", Pattern.CASE_INSENSITIVE);
            Matcher chaMatcher = chaPattern.matcher(upperAscii);
            if (chaMatcher.find()) {
                String candidate = chaMatcher.group().toUpperCase();
                Log.d(TAG, "Found CHA candidate as fallback: " + candidate);
                if (candidate.length() == 10) {
                    Log.d(TAG, "Valid CHA transaction ID as fallback: " + candidate);
                    return candidate;
                }
            }

            Log.d(TAG, "No valid CH or C patterns found in ASCII");
            return null;

        } catch (Exception e) {
            Log.e(TAG, "Error extracting CH/C from ASCII: " + e.getMessage());
            return null;
        }
    }

    /**
     * Convert hex string to ASCII
     */
    private static String hexToAscii(String hexStr) {
        try {
            // Ensure hex string is valid and has even length
            if (hexStr == null || hexStr.length() % 2 != 0) {
                Log.e(TAG, "Invalid hex string: " + hexStr);
                return null;
            }

            StringBuilder output = new StringBuilder();
            for (int i = 0; i < hexStr.length(); i += 2) {
                String str = hexStr.substring(i, i + 2);
                try {
                    int decimal = Integer.parseInt(str, 16);
                    // Only include printable ASCII characters (32-126)
                    if (decimal >= 32 && decimal <= 126) {
                        output.append((char) decimal);
                    }
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Error parsing hex pair: " + str + ", at position: " + i);
                    return null;
                }
            }
            String result = output.toString();
            Log.d(TAG, "Hex to ASCII conversion result: " + result);
            return result;
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

            // Skip Base64 check for strings starting with FT, CH, C, or URLs
            if (cleaned.toUpperCase().startsWith("FT") ||
                    cleaned.toUpperCase().startsWith("CH") ||
                    cleaned.toUpperCase().startsWith("C") ||
                    cleaned.toLowerCase().startsWith("http")) {
                Log.d(TAG, "Skipping Base64 decode for FT/CH/C/URL prefixed string: " + cleaned);
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

        // Skip hex check for strings starting with FT, CH, C, or URLs
        if (str.toUpperCase().startsWith("FT") ||
                str.toUpperCase().startsWith("CH") ||
                str.toUpperCase().startsWith("C") ||
                str.toLowerCase().startsWith("http")) {
            Log.d(TAG, "Skipping hex decode for FT/CH/C/URL prefixed string: " + str);
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
     * Check if transaction ID is C format
     */
    public static boolean isCFormat(String transactionId) {
        if (transactionId == null) return false;
        return C_PATTERN.matcher(transactionId).matches();
    }

    /**
     * Validate transaction ID format (CH with 10 chars, FT with 12 chars, C with 10 chars)
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
        boolean cMatch = C_PATTERN.matcher(upperTxId).matches();

        Log.d(TAG, "FT pattern match (12 chars): " + ftMatch);
        Log.d(TAG, "CH pattern match (10 chars): " + chMatch);
        Log.d(TAG, "C pattern match (10 chars): " + cMatch);

        boolean isValid = ftMatch || chMatch || cMatch;
        Log.d(TAG, "Final validation result: " + isValid);

        return isValid;
    }
}