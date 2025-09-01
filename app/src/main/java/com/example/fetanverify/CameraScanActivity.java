package com.example.fetanverify;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Camera activity with ML Kit OCR for SMS text extraction.
 */
@ExperimentalGetImage
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
                        try {
                            processCapturedImage(image);
                        } catch (Exception e) {
                            Log.e(TAG, "Error in onCaptureSuccess: " + e.getMessage(), e);
                            runOnUiThread(() -> {
                                Toast.makeText(CameraScanActivity.this, "Error capturing image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                resetCaptureButton();
                            });
                        }
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

    @ExperimentalGetImage
    private void processCapturedImage(ImageProxy imageProxy) {
        try {
            Bitmap bitmap = imageProxyToBitmap(imageProxy);

            if (bitmap != null) {
                Log.d(TAG, "Processing captured bitmap: " + bitmap.getWidth() + "x" + bitmap.getHeight());

                // Use Google ML Kit Text Recognition with Latin script options
                TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
                InputImage image = InputImage.fromBitmap(bitmap, 0);

                recognizer.process(image)
                        .addOnSuccessListener(visionText -> {
                            String extractedText = visionText.getText();
                            runOnUiThread(() -> {
                                if (extractedText != null && !extractedText.trim().isEmpty()) {
                                    Log.d(TAG, "Extracted text: " + extractedText);
                                    Intent resultIntent = new Intent();
                                    resultIntent.putExtra("SCAN_RESULT", extractedText);
                                    setResult(RESULT_OK, resultIntent);
                                    finish();
                                } else {
                                    Log.w(TAG, "No text extracted");
                                    Toast.makeText(CameraScanActivity.this,
                                            "No text found. Make sure the SMS is clear.",
                                            Toast.LENGTH_LONG).show();
                                    resetCaptureButton();
                                }
                            });
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "OCR failed", e);
                            runOnUiThread(() -> {
                                Toast.makeText(CameraScanActivity.this,
                                        "Failed to process image: " + e.getMessage(),
                                        Toast.LENGTH_LONG).show();
                                resetCaptureButton();
                            });
                        });

            } else {
                Log.e(TAG, "Failed to convert ImageProxy to Bitmap");
                Toast.makeText(this, "Failed to process image", Toast.LENGTH_SHORT).show();
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

            android.graphics.YuvImage yuvImage = new android.graphics.YuvImage(
                    nv21,
                    android.graphics.ImageFormat.NV21,
                    imageProxy.getWidth(),
                    imageProxy.getHeight(),
                    null
            );

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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, "Permissions not granted.", Toast.LENGTH_SHORT).show();
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