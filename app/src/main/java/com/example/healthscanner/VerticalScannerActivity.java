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
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

/**
 * Vertical Scanner Activity with modern camera interface
 * Supports both Barcode Scanning and Product Detection via Food Recognition API
 */
public class VerticalScannerActivity extends BaseActivity {

    private static final String TAG = "VerticalScanner";
    private static final int CAMERA_PERMISSION_REQUEST = 100;
    private static final int GALLERY_REQUEST_CODE = 101;

    private enum ScanMode {
        BARCODE, PRODUCT_DETECT
    }
    private ScanMode currentMode = ScanMode.BARCODE;

    // UI Elements
    private PreviewView cameraPreview;
    private ImageView backButton, flashToggle, detectCrosshair, captureButtonIcon;
    private CardView galleryButton, cameraCaptureButton, scanStatusCard, manualEntryButton;
    private TextView instructionsText, scanStatusText, modeBarcodeText, modeDetectText;
    private ProgressBar scanProgress;
    private View scanningLine, scanningFrame;

    // Camera
    private ProcessCameraProvider cameraProvider;
    private Preview preview;
    private ImageAnalysis imageAnalysis;
    private ImageCapture imageCapture;

    private boolean isFlashOn = false;
    private boolean isScanning = false;
    private boolean isCapturing = false;

    private OkHttpClient httpClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner_simple);

        httpClient = new OkHttpClient();

        // Initialize views
        initializeViews();
        setupClickListeners();

        // Default UI to barcode mode
        updateModeUI();

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
        cameraCaptureButton = findViewById(R.id.camera_capture_button);
        captureButtonIcon = findViewById(R.id.capture_button_icon);
        manualEntryButton = findViewById(R.id.manual_entry_button);

        scanStatusCard = findViewById(R.id.scan_status_card);
        instructionsText = findViewById(R.id.instructions_text);
        scanStatusText = findViewById(R.id.scan_status_text);
        scanProgress = findViewById(R.id.scan_progress);

        scanningLine = findViewById(R.id.scanning_line);
        scanningFrame = findViewById(R.id.scanning_frame);
        detectCrosshair = findViewById(R.id.detect_crosshair);

        modeBarcodeText = findViewById(R.id.mode_barcode);
        modeDetectText = findViewById(R.id.mode_detect);
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

        // Camera capture button
        if (cameraCaptureButton != null) {
            cameraCaptureButton.setOnClickListener(v -> {
                animateButtonPress(v);
                capturePhotoForDetection();
            });
        }

        // Manual entry button
        if (manualEntryButton != null) {
            manualEntryButton.setOnClickListener(v -> {
                animateButtonPress(v);
                openManualEntry();
            });
        }

        // Mode switchers
        if (modeBarcodeText != null) {
            modeBarcodeText.setOnClickListener(v -> {
                if (currentMode != ScanMode.BARCODE) {
                    currentMode = ScanMode.BARCODE;
                    updateModeUI();
                    bindCameraUseCases();
                }
            });
        }

        if (modeDetectText != null) {
            modeDetectText.setOnClickListener(v -> {
                if (currentMode != ScanMode.PRODUCT_DETECT) {
                    currentMode = ScanMode.PRODUCT_DETECT;
                    updateModeUI();
                    bindCameraUseCases();
                }
            });
        }
    }

    private void updateModeUI() {
        if (currentMode == ScanMode.BARCODE) {
            modeBarcodeText.setBackgroundResource(R.drawable.mode_toggle_active);
            modeDetectText.setBackgroundResource(R.drawable.mode_toggle_inactive);

            scanningFrame.setVisibility(View.VISIBLE);
            scanningLine.setVisibility(View.VISIBLE);
            detectCrosshair.setVisibility(View.GONE);

            instructionsText.setText("Position barcode in the frame");

            if (captureButtonIcon != null) {
                captureButtonIcon.setImageResource(R.drawable.ic_barcode_scan);
            }
        } else {
            modeBarcodeText.setBackgroundResource(R.drawable.mode_toggle_inactive);
            modeDetectText.setBackgroundResource(R.drawable.mode_toggle_active);

            scanningFrame.setVisibility(View.GONE);
            scanningLine.setVisibility(View.GONE);
            detectCrosshair.setVisibility(View.VISIBLE);

            instructionsText.setText("Tap capture to recognize food item");

            if (captureButtonIcon != null) {
                captureButtonIcon.setImageResource(R.drawable.ic_detect_product);
            }
        }
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
        if (cameraProvider == null) return;

        // Preview
        preview = new Preview.Builder().build();
        preview.setSurfaceProvider(cameraPreview.getSurfaceProvider());

        // Image analysis for barcode scanning
        imageAnalysis = new ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build();

        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this),
                new BarcodeAnalyzer(barcode -> {
                    if (currentMode == ScanMode.BARCODE) {
                        onBarcodeDetected(barcode);
                    }
                }));

        // Image capture for taking photos
        imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .setFlashMode(isFlashOn ? ImageCapture.FLASH_MODE_ON : ImageCapture.FLASH_MODE_OFF)
                .build();

        CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

        try {
            cameraProvider.unbindAll();
            androidx.camera.core.Camera camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalysis, imageCapture);

            if (camera.getCameraInfo().hasFlashUnit()) {
                camera.getCameraControl().enableTorch(isFlashOn);
            }

            showScanStatus("Ready", true);
        } catch (Exception e) {
            Log.e(TAG, "Error binding camera use cases", e);
            showScanStatus("Camera binding failed", false);
        }
    }

    private void onBarcodeDetected(String barcode) {
        if (isScanning || currentMode != ScanMode.BARCODE) return;
        isScanning = true;

        showScanStatus("✅ Barcode detected! Opening product details...", true);
        performHapticFeedback();
        Log.d(TAG, "Navigating to product details with barcode: " + barcode);

        if (scanStatusCard != null) {
            scanStatusCard.postDelayed(() -> {
                Intent intent = new Intent(this, ProductDetailsEnhancedActivity.class);
                intent.putExtra("barcode", barcode);
                startActivity(intent);
            }, 500);
        } else {
            Intent intent = new Intent(this, ProductDetailsEnhancedActivity.class);
            intent.putExtra("barcode", barcode);
            startActivity(intent);
        }
    }

    private void toggleFlash() {
        try {
            if (cameraProvider != null) {
                isFlashOn = !isFlashOn;
                flashToggle.setImageResource(isFlashOn ? R.drawable.ic_flash_on : R.drawable.ic_flash_off);
                flashToggle.setColorFilter(isFlashOn ? ContextCompat.getColor(this, R.color.primary_teal)
                        : ContextCompat.getColor(this, R.color.white));
                bindCameraUseCases();
                showScanStatus(isFlashOn ? "Flash ON" : "Flash OFF", true);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error toggling flash: " + e.getMessage(), e);
        }
    }

    /**
     * Capture a photo and send it to the Food Recognition API.
     */
    private void capturePhotoForDetection() {
        if (imageCapture == null) {
            showScanStatus("Camera not ready", false);
            return;
        }
        if (isCapturing) return;

        isCapturing = true;
        setCaptureButtonEnabled(false);
        showScanStatus("📸 Recognizing food item...", true);
        if (scanProgress != null) scanProgress.setVisibility(View.VISIBLE);
        performHapticFeedback();

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "NURE_FOOD_" + timeStamp + ".jpg";
        File photoFile = new File(getCacheDir(), fileName);

        ImageCapture.OutputFileOptions outputOptions =
                new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        Log.d(TAG, "Photo captured: " + photoFile.getAbsolutePath());
                        detectFoodViaApi(photoFile);
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        isCapturing = false;
                        setCaptureButtonEnabled(true);
                        Log.e(TAG, "Photo capture failed: " + exception.getMessage(), exception);
                        showScanStatus("❌ Photo capture failed", false);
                    }
                });
    }

    /**
     * Send captured image to the Food Recognition API for ML-based identification.
     */
    private void detectFoodViaApi(File photoFile) {
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("image", photoFile.getName(),
                        RequestBody.create(photoFile, MediaType.parse("image/jpeg")))
                .build();

        Request request = new Request.Builder()
                .url(ApiConfig.API_URL_PREDICT)
                .post(requestBody)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "API call failed", e);
                runOnUiThread(() -> {
                    isCapturing = false;
                    setCaptureButtonEnabled(true);
                    showScanStatus("❌ Network error. Check connection.", false);

                    Intent intent = new Intent(VerticalScannerActivity.this, ApiDetectionResultActivity.class);
                    intent.putExtra("image_path", photoFile.getAbsolutePath());
                    intent.putExtra("error", "Network Error: " + e.getMessage());
                    startActivity(intent);
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    String errBody = response.body() != null ? response.body().string() : "Unknown Error";
                    runOnUiThread(() -> {
                        isCapturing = false;
                        setCaptureButtonEnabled(true);
                        showScanStatus("❌ API Error", false);

                        Intent intent = new Intent(VerticalScannerActivity.this, ApiDetectionResultActivity.class);
                        intent.putExtra("image_path", photoFile.getAbsolutePath());
                        intent.putExtra("error", "API Error HTTP " + response.code() + ": " + errBody);
                        startActivity(intent);
                    });
                    return;
                }

                String responseData = response.body().string();
                Log.d(TAG, "API Response: " + responseData);

                try {
                    org.json.JSONObject json = new org.json.JSONObject(responseData);
                    String product = json.optString("product", "Unknown");
                    double confidence = json.optDouble("confidence", 0.0);
                    org.json.JSONObject nutrition = json.optJSONObject("nutrition");
                    org.json.JSONArray ingredientsArr = json.optJSONArray("ingredients");
                    String servingSize = json.optString("serving_size", "1 serving");
                    int healthScore = json.optInt("health_score", 50);
                    org.json.JSONArray healthTags = json.optJSONArray("health_tags");
                    org.json.JSONArray allergens = json.optJSONArray("allergens");
                    boolean isVeg = json.optBoolean("is_vegetarian", false);
                    org.json.JSONArray similarArr = json.optJSONArray("similar_products");

                    runOnUiThread(() -> {
                        isCapturing = false;
                        setCaptureButtonEnabled(true);

                        if (product.equals("fallback_product")) {
                            showScanStatus("🔍 Manual Selection Required", true);
                            Intent intent = new Intent(VerticalScannerActivity.this, ProductSelectionActivity.class);
                            intent.putExtra("image_path", photoFile.getAbsolutePath());
                            startActivity(intent);
                        } else if (confidence > 0.3) {
                            showScanStatus("✅ Food recognized: " + product, true);

                            Intent intent = new Intent(VerticalScannerActivity.this, ApiDetectionResultActivity.class);
                            intent.putExtra("image_path", photoFile.getAbsolutePath());
                            intent.putExtra("product", product);
                            intent.putExtra("confidence", confidence);
                            intent.putExtra("serving_size", servingSize);
                            intent.putExtra("health_score", healthScore);
                            intent.putExtra("is_vegetarian", isVeg);
                            if (nutrition != null) intent.putExtra("nutrition_json", nutrition.toString());
                            if (ingredientsArr != null) intent.putExtra("ingredients_json", ingredientsArr.toString());
                            if (healthTags != null) intent.putExtra("health_tags_json", healthTags.toString());
                            if (allergens != null) intent.putExtra("allergens_json", allergens.toString());
                            if (similarArr != null) intent.putExtra("similar_products", similarArr.toString());
                            startActivity(intent);
                        } else {
                            showScanStatus("🔍 Low confidence, trying OCR...", true);
                            runOcrFallback(photoFile);
                        }
                    });

                } catch (Exception e) {
                    Log.e(TAG, "JSON parsing error", e);
                    runOnUiThread(() -> {
                        isCapturing = false;
                        setCaptureButtonEnabled(true);

                        Intent intent = new Intent(VerticalScannerActivity.this, ApiDetectionResultActivity.class);
                        intent.putExtra("image_path", photoFile.getAbsolutePath());
                        intent.putExtra("error", "Error parsing response: " + e.getMessage());
                        startActivity(intent);
                    });
                }
            }
        });
    }

    private void runOcrFallback(File photoFile) {
        try {
            Bitmap bitmap = BitmapFactory.decodeFile(photoFile.getAbsolutePath());
            if (bitmap == null) throw new Exception("Failed to decode image");

            com.google.mlkit.vision.common.InputImage image =
                    com.google.mlkit.vision.common.InputImage.fromBitmap(bitmap, 0);
            com.google.mlkit.vision.text.TextRecognizer recognizer =
                    TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

            recognizer.process(image)
                    .addOnSuccessListener(visionText -> {
                        String text = visionText.getText();
                        if (text != null && !text.trim().isEmpty()) {
                            sendOcrToApi(text, photoFile);
                        } else {
                            isCapturing = false;
                            setCaptureButtonEnabled(true);
                            showScanStatus("❌ Could not recognize food", false);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "OCR processing failed", e);
                        isCapturing = false;
                        setCaptureButtonEnabled(true);
                        showScanStatus("❌ OCR Failed", false);
                    });
        } catch (Exception e) {
            Log.e(TAG, "OCR initialization failed", e);
            isCapturing = false;
            setCaptureButtonEnabled(true);
            showScanStatus("❌ Error starting OCR", false);
        }
    }

    private void sendOcrToApi(String text, File photoFile) {
        RequestBody requestBody = new okhttp3.FormBody.Builder()
                .add("text", text)
                .build();

        Request request = new Request.Builder()
                .url(ApiConfig.API_URL_OCR_DETECT)
                .post(requestBody)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    isCapturing = false;
                    setCaptureButtonEnabled(true);
                    showScanStatus("❌ OCR Network error", false);
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    runOnUiThread(() -> {
                        isCapturing = false;
                        setCaptureButtonEnabled(true);
                        showScanStatus("❌ OCR API Error", false);
                    });
                    return;
                }

                String responseData = response.body() != null ? response.body().string() : "{}";
                try {
                    org.json.JSONObject json = new org.json.JSONObject(responseData);
                    String product = json.optString("product", "Unknown");
                    double confidence = json.optDouble("confidence", 0.0);
                    org.json.JSONObject nutrition = json.optJSONObject("nutrition");
                    org.json.JSONArray ingredientsArr = json.optJSONArray("ingredients");
                    org.json.JSONArray similarArr = json.optJSONArray("similar_products");

                    runOnUiThread(() -> {
                        isCapturing = false;
                        setCaptureButtonEnabled(true);

                        Intent intent = new Intent(VerticalScannerActivity.this, ApiDetectionResultActivity.class);
                        intent.putExtra("image_path", photoFile.getAbsolutePath());
                        intent.putExtra("product", product);
                        intent.putExtra("confidence", confidence);
                        if (nutrition != null) intent.putExtra("nutrition_json", nutrition.toString());
                        if (ingredientsArr != null) intent.putExtra("ingredients_json", ingredientsArr.toString());
                        if (similarArr != null) intent.putExtra("similar_products", similarArr.toString());
                        startActivity(intent);
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        isCapturing = false;
                        setCaptureButtonEnabled(true);
                        showScanStatus("❌ OCR parsing error", false);
                    });
                }
            }
        });
    }

    private void setCaptureButtonEnabled(boolean enabled) {
        if (cameraCaptureButton != null) {
            cameraCaptureButton.setEnabled(enabled);
            cameraCaptureButton.setAlpha(enabled ? 1.0f : 0.5f);
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, GALLERY_REQUEST_CODE);
    }

    private void openManualEntry() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Enter Barcode Manually");

        final android.widget.EditText input = new android.widget.EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setHint("Enter barcode number");
        builder.setView(input);

        builder.setPositiveButton("Scan", (dialog, which) -> {
            String barcode = input.getText().toString().trim();
            if (!barcode.isEmpty()) {
                currentMode = ScanMode.BARCODE;
                onBarcodeDetected(barcode);
            } else {
                showScanStatus("Please enter a valid barcode", false);
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

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

        // Handle ZXing result
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null && result.getContents() != null) {
            currentMode = ScanMode.BARCODE;
            onBarcodeDetected(result.getContents());
        }
    }

    private void processImageForBarcode(Uri imageUri) {
        try {
            com.google.mlkit.vision.common.InputImage image =
                    com.google.mlkit.vision.common.InputImage.fromFilePath(this, imageUri);

            com.google.mlkit.vision.barcode.BarcodeScanner scanner =
                    com.google.mlkit.vision.barcode.BarcodeScanning.getClient();

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

            if (!message.contains("detected") && !message.contains("Processing")
                    && !message.contains("Ready") && !message.contains("Recognizing")) {
                scanStatusCard.postDelayed(() -> {
                    if (scanStatusCard != null) {
                        scanStatusCard.setVisibility(View.GONE);
                    }
                }, 3000);
            }
        }

        if (scanProgress != null) {
            scanProgress.setVisibility(
                    isSuccess && !message.contains("Ready") ? View.VISIBLE : View.GONE);
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
        isScanning = false;
        isCapturing = false;
        setCaptureButtonEnabled(true);
        showScanStatus("Ready to scan", true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
    }
}