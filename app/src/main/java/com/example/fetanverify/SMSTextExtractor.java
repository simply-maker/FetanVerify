package com.example.fetanverify;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.util.Log;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import java.util.concurrent.CompletableFuture;

/**
 * Enhanced SMS text extraction using ML Kit OCR with improved processing
 */
public class SMSTextExtractor {
    private static final String TAG = "SMSTextExtractor";
    private static TextRecognizer textRecognizer;
    
    /**
     * Initialize the text recognizer
     */
    public static void initialize() {
        if (textRecognizer == null) {
            textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
            Log.d(TAG, "ML Kit Text Recognizer initialized");
        }
    }
    
    /**
     * Extract text from bitmap using ML Kit OCR with enhanced processing
     */
    public static CompletableFuture<String> extractTextFromBitmap(Bitmap bitmap) {
        CompletableFuture<String> future = new CompletableFuture<>();
        
        if (textRecognizer == null) {
            initialize();
        }
        
        if (bitmap == null) {
            Log.e(TAG, "Bitmap is null");
            future.complete(null);
            return future;
        }
        
        try {
            Log.d(TAG, "Processing bitmap: " + bitmap.getWidth() + "x" + bitmap.getHeight());
            
            // Enhance bitmap for better OCR results
            Bitmap enhancedBitmap = enhanceBitmapForOCR(bitmap);
            
            InputImage image = InputImage.fromBitmap(enhancedBitmap, 0);
            
            textRecognizer.process(image)
                .addOnSuccessListener(visionText -> {
                    String extractedText = visionText.getText();
                    Log.d(TAG, "OCR extracted raw text: " + extractedText);
                    
                    if (extractedText == null || extractedText.trim().isEmpty()) {
                        Log.w(TAG, "No text extracted from image");
                        future.complete(null);
                        return;
                    }
                    
                    // Enhanced text processing for better transaction ID extraction
                    String processedText = preprocessSMSText(extractedText);
                    Log.d(TAG, "Preprocessed SMS text: " + processedText);
                    
                    // Use enhanced transaction extractor
                    String transactionId = TransactionExtractor.extractTransactionId(processedText);
                    
                    if (transactionId != null) {
                        Log.d(TAG, "Successfully extracted transaction ID: " + transactionId);
                    } else {
                        Log.w(TAG, "No transaction ID found in extracted text");
                        // Try with original text as fallback
                        transactionId = TransactionExtractor.extractTransactionId(extractedText);
                        if (transactionId != null) {
                            Log.d(TAG, "Found transaction ID in original text: " + transactionId);
                        }
                    }
                    
                    future.complete(transactionId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "OCR failed: " + e.getMessage(), e);
                    future.complete(null);
                });
                
        } catch (Exception e) {
            Log.e(TAG, "Error processing bitmap: " + e.getMessage(), e);
            future.complete(null);
        }
        
        return future;
    }
    
    /**
     * Preprocess SMS text for better transaction ID extraction
     */
    private static String preprocessSMSText(String rawText) {
        if (rawText == null || rawText.trim().isEmpty()) {
            return rawText;
        }
        
        try {
            // Remove common OCR artifacts and normalize text
            String processed = rawText
                // Fix common OCR mistakes
                .replaceAll("(?i)[oO](?=[0-9])", "0")  // Replace O with 0 when followed by numbers
                .replaceAll("(?i)[Il](?=[0-9])", "1")  // Replace I, l with 1 when followed by numbers
                .replaceAll("(?i)[S](?=[0-9])", "5")   // Replace S with 5 when followed by numbers
                // Remove extra whitespace and normalize
                .replaceAll("\\s+", " ")
                .trim();
            
            // Look for common SMS patterns and extract relevant parts
            String[] lines = processed.split("\n");
            StringBuilder relevantText = new StringBuilder();
            
            for (String line : lines) {
                String upperLine = line.toUpperCase();
                // Include lines that might contain transaction IDs
                if (upperLine.contains("FT") || 
                    upperLine.contains("CH") || 
                    upperLine.contains("TRANSACTION") ||
                    upperLine.contains("REFERENCE") ||
                    upperLine.contains("ID") ||
                    upperLine.contains("TELEBIRR") ||
                    upperLine.contains("PAYMENT") ||
                    upperLine.matches(".*[A-Z]{2,3}[0-9A-Z]{6,12}.*")) {
                    relevantText.append(line).append(" ");
                }
            }
            
            String result = relevantText.toString().trim();
            return result.isEmpty() ? processed : result;
            
        } catch (Exception e) {
            Log.e(TAG, "Error preprocessing SMS text: " + e.getMessage());
            return rawText;
        }
    }
    
    /**
     * Enhanced bitmap processing for better OCR results
     */
    private static Bitmap enhanceBitmapForOCR(Bitmap originalBitmap) {
        try {
            // Create a mutable copy
            Bitmap enhancedBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
            
            // Apply multiple enhancement techniques
            enhancedBitmap = adjustContrast(enhancedBitmap, 1.5f, 30);
            enhancedBitmap = sharpenImage(enhancedBitmap);
            
            Log.d(TAG, "Bitmap enhanced for OCR");
            return enhancedBitmap;
            
        } catch (Exception e) {
            Log.w(TAG, "Failed to enhance bitmap, using original: " + e.getMessage());
            return originalBitmap;
        }
    }
    
    /**
     * Adjust contrast and brightness
     */
    private static Bitmap adjustContrast(Bitmap bitmap, float contrast, float brightness) {
        try {
            Bitmap adjustedBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
            
            ColorMatrix colorMatrix = new ColorMatrix();
            colorMatrix.set(new float[] {
                contrast, 0, 0, 0, brightness,     // Red
                0, contrast, 0, 0, brightness,     // Green  
                0, 0, contrast, 0, brightness,     // Blue
                0, 0, 0, 1, 0                      // Alpha
            });
            
            ColorMatrixColorFilter filter = new ColorMatrixColorFilter(colorMatrix);
            Canvas canvas = new Canvas(adjustedBitmap);
            Paint paint = new Paint();
            paint.setColorFilter(filter);
            canvas.drawBitmap(bitmap, 0, 0, paint);
            
            return adjustedBitmap;
            
        } catch (Exception e) {
            Log.w(TAG, "Failed to adjust contrast: " + e.getMessage());
            return bitmap;
        }
    }
    
    /**
     * Apply sharpening filter
     */
    private static Bitmap sharpenImage(Bitmap bitmap) {
        try {
            Bitmap sharpenedBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
            
            // Simple sharpening using ColorMatrix
            ColorMatrix sharpenMatrix = new ColorMatrix(new float[] {
                0, -1, 0,
                -1, 5, -1,
                0, -1, 0
            });
            
            ColorMatrixColorFilter filter = new ColorMatrixColorFilter(sharpenMatrix);
            Canvas canvas = new Canvas(sharpenedBitmap);
            Paint paint = new Paint();
            paint.setColorFilter(filter);
            canvas.drawBitmap(bitmap, 0, 0, paint);
            
            return sharpenedBitmap;
            
        } catch (Exception e) {
            Log.w(TAG, "Failed to sharpen image: " + e.getMessage());
            return bitmap;
        }
    }
    
    /**
     * Process SMS screenshot and extract transaction ID
     */
    public static CompletableFuture<String> processSMSScreenshot(Bitmap bitmap) {
        return extractTextFromBitmap(bitmap);
    }
    
    /**
     * Clean up resources
     */
    public static void cleanup() {
        if (textRecognizer != null) {
            textRecognizer.close();
            textRecognizer = null;
            Log.d(TAG, "Text recognizer cleaned up");
        }
    }
}