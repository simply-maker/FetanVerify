package com.example.fetanverify;

import android.graphics.Bitmap;
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
                    
                    // Process the extracted text to find transaction IDs
                    String transactionId = TransactionExtractor.extractTransactionId(extractedText);
                    
                    if (transactionId != null) {
                        Log.d(TAG, "Successfully extracted transaction ID: " + transactionId);
                    } else {
                        Log.w(TAG, "No transaction ID found in extracted text");
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
     * Enhance bitmap for better OCR results
     */
    private static Bitmap enhanceBitmapForOCR(Bitmap originalBitmap) {
        try {
            // Create a mutable copy
            Bitmap enhancedBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
            
            // Apply contrast and brightness adjustments
            android.graphics.ColorMatrix colorMatrix = new android.graphics.ColorMatrix();
            colorMatrix.set(new float[] {
                1.2f, 0, 0, 0, 20,     // Red
                0, 1.2f, 0, 0, 20,     // Green  
                0, 0, 1.2f, 0, 20,     // Blue
                0, 0, 0, 1, 0          // Alpha
            });
            
            android.graphics.ColorMatrixColorFilter filter = new android.graphics.ColorMatrixColorFilter(colorMatrix);
            android.graphics.Canvas canvas = new android.graphics.Canvas(enhancedBitmap);
            android.graphics.Paint paint = new android.graphics.Paint();
            paint.setColorFilter(filter);
            canvas.drawBitmap(originalBitmap, 0, 0, paint);
            
            Log.d(TAG, "Bitmap enhanced for OCR");
            return enhancedBitmap;
            
        } catch (Exception e) {
            Log.w(TAG, "Failed to enhance bitmap, using original: " + e.getMessage());
            return originalBitmap;
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