package com.example.healthscanner;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.util.concurrent.ExecutionException;

/**
 * Vertical Scanner Activity with modern camera interface
 * Provides an intuitive vertical scanning experience
 */
public class VerticalScannerActivity extends BaseActivity {

    private static final String TAG = "VerticalScanner";
    private static final int CAMERA_PERMISSION_REQUEST = 100;
    private static final int GALLERY_REQUEST_CODE = 101;

    // UI Elements
    private PreviewView cameraPreview;
    private ImageView backButton, flashToggle, closeOverlayButton;
    private CardView galleryButton, scanStatusCard, viewDetailsButton, scanAnotherButton;
    private TextView instructionsText, scanStatusText;
    private ProgressBar scanProgress;
    private View scanningLine;

    // Overlay elements removed for simplified scanner

    // Camera
    private ProcessCameraProvider cameraProvider;
    private Preview preview;
    private ImageAnalysis imageAnalysis;
    private boolean isFlashOn = false;
    private boolean isScanning = false;
    private String currentBarcode = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner_simple);

        // Initialize views
        initializeViews();
        setupClickListeners();

        // Check camera permission
        if (checkCameraPermission()) {
            startCamera();
        } else {
            requestCameraPermission();
        }

        // Start scanning line animation
        startScanningLineAnimation();
    }

    private void initializeViews() {
        cameraPreview = findViewById(R.id.camera_preview);
        backButton = findViewById(R.id.back_button);
        flashToggle = findViewById(R.id.flash_toggle);
        galleryButton = findViewById(R.id.gallery_button);
        scanStatusCard = findViewById(R.id.scan_status_card);
        instructionsText = findViewById(R.id.instructions_text);
        scanStatusText = findViewById(R.id.scan_status_text);
        scanProgress = findViewById(R.id.scan_progress);
        scanningLine = findViewById(R.id.scanning_line);

        // Overlay elements removed - scanner now navigates directly to product details
    }

    private void setupClickListeners() {
        // Back button
        backButton.setOnClickListener(v -> {
            animateButtonPress(v);
            finish();
        });

        // Flash toggle
        flashToggle.setOnClickListener(v -> {
            animateButtonPress(v);
            toggleFlash();
        });

        // Gallery button
        galleryButton.setOnClickListener(v -> {
            animateButtonPress(v);
            openGallery();
        });

        // Manual entry button
        CardView manualEntryButton = findViewById(R.id.manual_entry_button);
        if (manualEntryButton != null) {
            manualEntryButton.setOnClickListener(v -> {
                animateButtonPress(v);
                openManualEntry();
            });
        }

        // Add long press on instructions for testing
        if (instructionsText != null) {
            instructionsText.setOnLongClickListener(v -> {
                Log.d(TAG, "Long press detected - testing product details");
                onBarcodeDetected("1234567890123");
                return true;
            });
        }

        // Overlay buttons removed - scanner now navigates directly to product details
    }

    private void animateButtonPress(View view) {
        Animation scaleAnimation = AnimationUtils.loadAnimation(this, R.anim.scale_bounce);
        view.startAnimation(scaleAnimation);
    }

    private void startScanningLineAnimation() {
        if (scanningLine != null) {
            Animation animation = AnimationUtils.loadAnimation(this, R.anim.scanning_line_animation);
            scanningLine.startAnimation(animation);
        }
    }

    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this,
                new String[] { Manifest.permission.CAMERA }, CAMERA_PERMISSION_REQUEST);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                showScanStatus("Camera permission required", false);
                finish();
            }
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases();
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error starting camera", e);
                showScanStatus("Camera initialization failed", false);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraUseCases() {
        if (cameraProvider == null)
            return;

        // Preview use case
        preview = new Preview.Builder().build();
        preview.setSurfaceProvider(cameraPreview.getSurfaceProvider());

        // Image analysis for barcode scanning
        imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        // Set up barcode analyzer
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this),
                new BarcodeAnalyzer(this::onBarcodeDetected));

        // Camera selector (back camera)
        CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

        try {
            // Unbind all use cases before rebinding
            cameraProvider.unbindAll();

            // Bind use cases to camera and get camera control
            androidx.camera.core.Camera camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalysis);

            // Enable flash if needed
            if (camera.getCameraInfo().hasFlashUnit()) {
                camera.getCameraControl().enableTorch(isFlashOn);
            }

            showScanStatus("Ready to scan", true);

        } catch (Exception e) {
            Log.e(TAG, "Error binding camera use cases", e);
            showScanStatus("Camera binding failed", false);
        }
    }

    private void onBarcodeDetected(String barcode) {
        if (isScanning)
            return; // Prevent multiple scans

        isScanning = true;
        currentBarcode = barcode;
        showScanStatus("✅ Barcode detected! Opening product details...", true);

        // Vibrate for feedback
        performHapticFeedback();

        // Navigate directly to product details
        navigateToProductDetails(barcode);
    }

    private void toggleFlash() {
        try {
            if (cameraProvider != null) {
                isFlashOn = !isFlashOn;

                // Update flash icon
                flashToggle.setImageResource(isFlashOn ? R.drawable.ic_flash_on : R.drawable.ic_flash_off);

                // Update flash icon color
                flashToggle.setColorFilter(isFlashOn ? ContextCompat.getColor(this, R.color.primary_teal)
                        : ContextCompat.getColor(this, R.color.white));

                // Rebind camera with flash setting
                bindCameraUseCases();

                showScanStatus(isFlashOn ? "Flash ON" : "Flash OFF", true);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error toggling flash: " + e.getMessage(), e);
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, GALLERY_REQUEST_CODE);
    }

    private void openManualEntry() {
        // Create a simple dialog for manual barcode entry
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Enter Barcode Manually");

        final android.widget.EditText input = new android.widget.EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setHint("Enter barcode number");
        builder.setView(input);

        builder.setPositiveButton("Scan", (dialog, which) -> {
            String barcode = input.getText().toString().trim();
            if (!barcode.isEmpty()) {
                onBarcodeDetected(barcode);
            } else {
                showScanStatus("Please enter a valid barcode", false);
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void navigateToProductDetails(String barcode) {
        Log.d(TAG, "Navigating to product details with barcode: " + barcode);

        // Add a small delay for better UX
        if (scanStatusCard != null) {
            scanStatusCard.postDelayed(() -> {
                Intent intent = new Intent(this, ProductDetailsEnhancedActivity.class);
                intent.putExtra("barcode", barcode);
                Log.d(TAG, "Starting ProductDetailsEnhancedActivity with barcode: " + barcode);

                startActivity(intent);
                // Don't finish immediately to allow user to see the transition
            }, 500); // 0.5 second delay to show success message
        } else {
            // Fallback if scanStatusCard is null
            Intent intent = new Intent(this, ProductDetailsEnhancedActivity.class);
            intent.putExtra("barcode", barcode);
            Log.d(TAG, "Starting ProductDetailsEnhancedActivity immediately with barcode: " + barcode);

            startActivity(intent);
        }
    }

    // Removed complex overlay methods - now navigating directly to product details

    // Health scoring and overlay methods moved to ProductDetailsActivitySimple

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == GALLERY_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            if (imageUri != null) {
                showScanStatus("Processing image...", true);
                processImageForBarcode(imageUri);
            }
        }

        // Handle ZXing result from manual entry
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null && result.getContents() != null) {
            onBarcodeDetected(result.getContents());
        }
    }

    private void processImageForBarcode(Uri imageUri) {
        try {
            com.google.mlkit.vision.common.InputImage image = com.google.mlkit.vision.common.InputImage
                    .fromFilePath(this, imageUri);

            com.google.mlkit.vision.barcode.BarcodeScanner scanner = com.google.mlkit.vision.barcode.BarcodeScanning
                    .getClient();

            scanner.process(image)
                    .addOnSuccessListener(barcodes -> {
                        if (!barcodes.isEmpty()) {
                            String barcode = barcodes.get(0).getRawValue();
                            if (barcode != null && !barcode.isEmpty()) {
                                onBarcodeDetected(barcode);
                            } else {
                                showScanStatus("No barcode found in image", false);
                            }
                        } else {
                            showScanStatus("No barcode found in image", false);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error processing image", e);
                        showScanStatus("Error processing image", false);
                    });

        } catch (Exception e) {
            Log.e(TAG, "Error loading image", e);
            showScanStatus("Error loading image", false);
        }
    }

    private void showScanStatus(String message, boolean isSuccess) {
        if (scanStatusText != null) {
            scanStatusText.setText(message);
        }

        if (scanStatusCard != null) {
            scanStatusCard.setVisibility(View.VISIBLE);

            // Auto-hide after 3 seconds if not scanning
            if (!message.contains("detected") && !message.contains("Processing")) {
                scanStatusCard.postDelayed(() -> {
                    if (scanStatusCard != null) {
                        scanStatusCard.setVisibility(View.GONE);
                    }
                }, 3000);
            }
        }

        if (scanProgress != null) {
            scanProgress.setVisibility(isSuccess && !message.contains("Ready") ? View.VISIBLE : View.GONE);
        }
    }

    private void performHapticFeedback() {
        try {
            android.os.Vibrator vibrator = (android.os.Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    vibrator.vibrate(android.os.VibrationEffect.createOneShot(100,
                            android.os.VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(100);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error performing haptic feedback", e);
        }
    }

    @Override
    protected int getCurrentNavigationItemId() {
        return R.id.nav_scan;
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reset scanning state when returning to scanner
        isScanning = false;
        Log.d(TAG, "Scanner resumed - ready for new scans");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
    }
}