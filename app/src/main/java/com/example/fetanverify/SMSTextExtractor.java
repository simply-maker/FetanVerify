package com.example.fetanverify;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import java.util.concurrent.CompletableFuture;

/**
 * Enhanced SMS text extraction using ML Kit OCR
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
        }
    }
    
    /**
     * Extract text from bitmap using ML Kit OCR
     */
    public static CompletableFuture<String> extractTextFromBitmap(Bitmap bitmap) {
        CompletableFuture<String> future = new CompletableFuture<>();
        
        if (textRecognizer == null) {
            initialize();
        }
        
        try {
            InputImage image = InputImage.fromBitmap(bitmap, 0);
            
            textRecognizer.process(image)
                .addOnSuccessListener(visionText -> {
                    String extractedText = visionText.getText();
                    Log.d(TAG, "OCR extracted text: " + extractedText);
                    
                    // Process the extracted text to find transaction IDs
                    String transactionId = TransactionExtractor.extractTransactionId(extractedText);
                    future.complete(transactionId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "OCR failed: " + e.getMessage());
                    future.complete(null);
                });
                
        } catch (Exception e) {
            Log.e(TAG, "Error processing bitmap: " + e.getMessage());
            future.complete(null);
        }
        
        return future;
    }
    
    /**
     * Process SMS screenshot and extract transaction ID
     */
    public static CompletableFuture<String> processSMSScreenshot(Bitmap bitmap) {
        return extractTextFromBitmap(bitmap);
    }
}