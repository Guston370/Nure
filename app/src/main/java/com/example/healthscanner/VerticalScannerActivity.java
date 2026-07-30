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
import java.io.IOException;

import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.File;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import java.util.ArrayList;
import java.util.List;

import com.example.healthscanner.database.ScanHistoryStore;
import com.example.healthscanner.models.Scan;

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

    /** Names the food in a photo (ML Kit image labelling, with OCR as a second pass). */
    private FoodRecognizer foodRecognizer;
    /** Turns a food name into nutrition facts. Shared with the barcode path. */
    private NutritionRepository nutritionRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner_simple);

        httpClient = new OkHttpClient();
        foodRecognizer = new FoodRecognizer(this);
        nutritionRepository = NutritionRepository.getInstance(this);

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
                        recognizeFood(photoFile);
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
     * Identify the food in a captured photo and resolve its nutrition.
     *
     * <p>Two stages, deliberately decoupled: {@link FoodRecognizer} only names the food,
     * {@link NutritionRepository} only supplies numbers for a name. The photo path and the
     * barcode path therefore share one nutrition layer and one health score model.</p>
     */
    private void recognizeFood(File photoFile) {
        showScanStatus("🔍 Identifying food...", true);

        Bitmap bitmap = BitmapFactory.decodeFile(photoFile.getAbsolutePath());
        if (bitmap == null) {
            onRecognitionFailed("❌ Could not read the photo");
            return;
        }

        foodRecognizer.recognize(bitmap, recognition -> {
            if (!recognition.isIdentified()) {
                // Distinguish "that isn't food" from "I can't name this food", then let the
                // user type it rather than dead-ending the scan.
                String message = recognition.looksLikeFood
                        ? "🤔 Looks like food, but I can't name it"
                        : "❌ No food detected in that photo";
                showScanStatus(message, false);
                promptForFoodName(photoFile, null);
                return;
            }

            String display = FoodLabelMapper.toDisplayCase(recognition.foodName);
            showScanStatus("✅ " + display + " — looking up nutrition...", true);
            resolveNutritionAndLaunch(photoFile, recognition, recognition.foodName);
        });
    }

    /**
     * Look the food's nutrition up and open the result screen.
     *
     * @param recognition may be {@code null} when the name came from manual entry
     */
    private void resolveNutritionAndLaunch(File photoFile,
            FoodRecognizer.Recognition recognition, String foodName) {
        if (scanProgress != null) scanProgress.setVisibility(View.VISIBLE);

        nutritionRepository.resolve(foodName, resolution -> {
            isCapturing = false;
            setCaptureButtonEnabled(true);
            if (scanProgress != null) scanProgress.setVisibility(View.GONE);

            if (!resolution.hasNutrition()) {
                showScanStatus("⚠️ No nutrition data for " + FoodLabelMapper.toDisplayCase(foodName), false);
                promptForFoodName(photoFile, foodName);
                return;
            }

            launchResult(photoFile, resolution, recognition, foodName);
        });
    }

    /**
     * Persist the scan and hand it to the result screen.
     *
     * <p>The scan goes through {@link ScanHistoryStore}, the same path barcode scans use, so
     * photo scans now show up in history, analytics and CSV export. They previously did
     * not.</p>
     */
    private void launchResult(File photoFile, NutritionRepository.Resolution resolution,
            FoodRecognizer.Recognition recognition, String queriedName) {
        double healthScore = resolution.healthScore();
        NutritionRepository.DietInfo diet =
                NutritionRepository.classifyDiet(resolution.foodName, resolution.ingredients);

        Scan scan = new Scan();
        scan.setProductName(resolution.foodName);
        scan.setBrand(resolution.brand);
        scan.setCategory(resolution.category != null ? resolution.category : "Photo scan");
        scan.setImageUrl(resolution.imageUrl);
        scan.setScanDate(new Date());
        scan.setHealthScore(healthScore);
        scan.setHealthGrade(HealthScoreCalculator.gradeFor(healthScore));
        scan.setCalories((int) Math.round(resolution.nutrition.calories));
        scan.setProtein(resolution.nutrition.protein);
        scan.setCarbs(resolution.nutrition.carbs);
        scan.setFat(resolution.nutrition.fat);
        scan.setSugar(resolution.nutrition.sugar);
        scan.setSodium(resolution.nutrition.sodium);
        scan.setFiber(resolution.nutrition.fiber);
        scan.setScanMethod(recognition != null && recognition.method == FoodRecognizer.Method.OCR_TEXT
                ? "photo_ocr"
                : (recognition == null ? "manual_entry" : "photo_label"));

        ScanHistoryStore.getInstance(this).addScan(scan);

        Intent intent = new Intent(this, ApiDetectionResultActivity.class);
        intent.putExtra("image_path", photoFile.getAbsolutePath());
        intent.putExtra("confidence", recognition != null ? (double) recognition.confidence : 1.0);
        intent.putExtra("nutrition_source", resolution.source.displayName);
        intent.putExtra("is_vegetarian", diet.vegetarian);
        intent.putExtra("is_vegan", diet.vegan);
        intent.putExtra("ingredients", resolution.ingredients);
        intent.putExtra("queried_name", queriedName);

        try {
            intent.putExtra("scan_json", scan.toJson().toString());
        } catch (Exception e) {
            Log.e(TAG, "Could not serialise scan: " + e.getMessage(), e);
        }

        if (recognition != null && !recognition.alternatives.isEmpty()) {
            intent.putExtra("alternatives", new org.json.JSONArray(recognition.alternatives).toString());
        }

        startActivity(intent);
    }

    /**
     * Ask the user to name the food when recognition or lookup came up empty.
     *
     * <p>Better than the old behaviour, which showed "Could not recognize food" and stopped.</p>
     */
    private void promptForFoodName(File photoFile, String prefill) {
        isCapturing = false;
        setCaptureButtonEnabled(true);
        if (scanProgress != null) scanProgress.setVisibility(View.GONE);

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("What is this?");
        builder.setMessage("Type the food name and we'll look up its nutrition.");

        final android.widget.EditText input = new android.widget.EditText(this);
        input.setHint("e.g. paneer butter masala");
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        if (prefill != null && !prefill.isEmpty()) {
            input.setText(prefill);
        }
        builder.setView(input);

        builder.setPositiveButton("Look up", (dialog, which) -> {
            String name = input.getText().toString().trim();
            if (name.isEmpty()) {
                showScanStatus("Enter a food name to continue", false);
                return;
            }
            isCapturing = true;
            setCaptureButtonEnabled(false);
            showScanStatus("🔍 Looking up " + name + "...", true);
            resolveNutritionAndLaunch(photoFile, null, name);
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void onRecognitionFailed(String message) {
        isCapturing = false;
        setCaptureButtonEnabled(true);
        if (scanProgress != null) scanProgress.setVisibility(View.GONE);
        showScanStatus(message, false);
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
        if (foodRecognizer != null) {
            foodRecognizer.close();
            foodRecognizer = null;
        }
    }


}
