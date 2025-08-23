package com.example.fetanverify;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.*;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.material.button.MaterialButton;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Custom camera activity for SMS text scanning using ML Kit OCR
 */
public class CameraScanActivity extends AppCompatActivity {
    private static final String TAG = "CameraScanActivity";
    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final String[] REQUIRED_PERMISSIONS = {Manifest.permission.CAMERA};
    
    private PreviewView previewView;
    private MaterialButton captureButton;
    private MaterialButton cancelButton;
    private ExecutorService cameraExecutor;
    private ImageCapture imageCapture;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Apply language
        LanguageHelper.applyLanguage(this);
        
        setContentView(R.layout.activity_camera_scan);
        
        previewView = findViewById(R.id.previewView);
        captureButton = findViewById(R.id.captureButton);
        cancelButton = findViewById(R.id.cancelButton);
        
        cameraExecutor = Executors.newSingleThreadExecutor();
        
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
        
        captureButton.setOnClickListener(v -> captureImage());
        cancelButton.setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });
    }
    
    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());
                
                imageCapture = new ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build();
                
                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
                
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
                
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Use case binding failed", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }
    
    private void captureImage() {
        if (imageCapture == null) return;
        
        captureButton.setEnabled(false);
        captureButton.setText("Processing...");
        
        imageCapture.takePicture(
            ContextCompat.getMainExecutor(this),
            new ImageCapture.OnImageCapturedCallback() {
                @Override
                public void onCaptureSuccess(@NonNull ImageProxy image) {
                    processCapturedImage(image);
                }
                
                @Override
                public void onError(@NonNull ImageCaptureException exception) {
                    Log.e(TAG, "Photo capture failed: " + exception.getMessage(), exception);
                    Toast.makeText(CameraScanActivity.this, "Capture failed: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                    resetCaptureButton();
                }
            }
        );
    }
    
    private void processCapturedImage(ImageProxy imageProxy) {
        try {
            // Convert ImageProxy to Bitmap
            Bitmap bitmap = imageProxyToBitmap(imageProxy);
            
            if (bitmap != null) {
                Log.d(TAG, "Processing captured bitmap: " + bitmap.getWidth() + "x" + bitmap.getHeight());
                // Use ML Kit OCR to extract text
                SMSTextExtractor.extractTextFromBitmap(bitmap)
                    .thenAccept(extractedText -> {
                        runOnUiThread(() -> {
                            if (extractedText != null && !extractedText.trim().isEmpty() && 
                                TransactionExtractor.isValidTransactionId(extractedText)) {
                                Intent resultIntent = new Intent();
                                resultIntent.putExtra("SCAN_RESULT", extractedText);
                                setResult(RESULT_OK, resultIntent);
                                finish();
                            } else {
                                Log.w(TAG, "No valid transaction ID found in captured image");
                                Toast.makeText(this, getString(R.string.no_transaction_id_found), Toast.LENGTH_LONG).show();
                                resetCaptureButton();
                            }
                        });
                    })
                    .exceptionally(throwable -> {
                        runOnUiThread(() -> {
                            Log.e(TAG, "OCR processing failed", throwable);
                            Toast.makeText(this, getString(R.string.error_processing_image), Toast.LENGTH_SHORT).show();
                            resetCaptureButton();
                        });
                        return null;
                    });
            } else {
                Log.e(TAG, "Failed to convert ImageProxy to Bitmap");
                Toast.makeText(this, getString(R.string.error_processing_image), Toast.LENGTH_SHORT).show();
                resetCaptureButton();
            }
        } finally {
            imageProxy.close();
        }
    }
    
    private Bitmap imageProxyToBitmap(ImageProxy imageProxy) {
        try {
            ImageProxy.PlaneProxy[] planes = imageProxy.getPlanes();
            ImageProxy.PlaneProxy yPlane = planes[0];
            ImageProxy.PlaneProxy uPlane = planes[1];
            ImageProxy.PlaneProxy vPlane = planes[2];
            
            int ySize = yPlane.getBuffer().remaining();
            int uSize = uPlane.getBuffer().remaining();
            int vSize = vPlane.getBuffer().remaining();
            
            byte[] nv21 = new byte[ySize + uSize + vSize];
            
            yPlane.getBuffer().get(nv21, 0, ySize);
            vPlane.getBuffer().get(nv21, ySize, vSize);
            uPlane.getBuffer().get(nv21, ySize + vSize, uSize);
            
            android.graphics.YuvImage yuvImage = new android.graphics.YuvImage(nv21, android.graphics.ImageFormat.NV21, 
                imageProxy.getWidth(), imageProxy.getHeight(), null);
            
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            yuvImage.compressToJpeg(new android.graphics.Rect(0, 0, imageProxy.getWidth(), imageProxy.getHeight()), 100, out);
            byte[] imageBytes = out.toByteArray();
            
            return android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
        } catch (Exception e) {
            Log.e(TAG, "Error converting ImageProxy to Bitmap", e);
            return null;
        }
    }
    
    private void resetCaptureButton() {
        captureButton.setEnabled(true);
        captureButton.setText("Capture");
    }
    
    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
    }
}