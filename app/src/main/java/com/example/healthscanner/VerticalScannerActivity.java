package com.example.healthscanner;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
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
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

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
 * Supports both Barcode Scanning and Product Detection via YOLO API
 */
public class VerticalScannerActivity extends BaseActivity {

    private static final String TAG = "VerticalScanner";
    private static final int CAMERA_PERMISSION_REQUEST = 100;
    private static final int GALLERY_REQUEST_CODE = 101;
    
    private enum ScanMode {
        BARCODE, PRODUCT_DETECT
    }
    private ScanMode currentMode = ScanMode.BARCODE;
    

    private enum ScanState {
        IDLE, DETECTED, SUCCESS, ERROR
    }

    // UI Elements
    private PreviewView cameraPreview;
    private ImageView backButton, flashToggle, detectCrosshair, captureButtonIcon;
    private ImageView cornerTl, cornerTr, cornerBl, cornerBr;
    private CardView galleryButton, scanStatusCard, manualEntryButton, scanningFrameCard;
    private View cameraCaptureButton;
    private TextView instructionsText, scanStatusText, modeBarcodeText, modeDetectText;
    private ProgressBar scanProgress;
    private View scanningLine, scanningFrame;
    private ScannerOverlayView scannerOverlay;

    // Preview Card Views
    private View productPreviewCard;
    private ImageView previewProductImage;
    private TextView previewProductName, previewProductBrand, previewProductScore;
    private CardView previewScoreContainer;
    private TextView scanStatusSubtext;
    // Camera
    private ProcessCameraProvider cameraProvider;
    private Preview preview;
    private ImageAnalysis imageAnalysis;
    private ImageCapture imageCapture;
    
    private boolean isFlashOn = false;
    private boolean isScanning = false;
    private boolean isCapturing = false;
    private boolean isNavigating = false;

    private File fallbackPhotoFile = null;
    private double fallbackYoloConfidence = 0.0;
    
    private OkHttpClient httpClient;
    
    private String currentBarcode = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner_vertical);
        httpClient = new OkHttpClient();
        
        // Initialize views
        initializeViews();
        setupClickListeners();
        
        // Default UI to barcode mode
        updateModeUI();
        

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
        cameraCaptureButton = findViewById(R.id.camera_capture_button);
        captureButtonIcon = findViewById(R.id.capture_button_icon);
        manualEntryButton = findViewById(R.id.manual_entry_button);
        
        scanStatusCard = findViewById(R.id.scan_status_card);
        instructionsText = findViewById(R.id.instructions_text);
        scanStatusText = findViewById(R.id.scan_status_text);
        scanProgress = findViewById(R.id.scan_progress);
        scanStatusSubtext = findViewById(R.id.scan_status_subtext);
        scanningLine = findViewById(R.id.scanning_line);
        scanningFrame = findViewById(R.id.scanning_frame);
        detectCrosshair = findViewById(R.id.detect_crosshair);
        
        modeBarcodeText = findViewById(R.id.mode_barcode);
        modeDetectText = findViewById(R.id.mode_detect);

        scanningFrameCard = findViewById(R.id.scanning_frame_card);
        scannerOverlay = findViewById(R.id.scanner_overlay);
        
        if (scannerOverlay != null && scanningFrameCard != null) {
            scannerOverlay.setTargetFrameView(scanningFrameCard, 32f);
        }

        cornerTl = findViewById(R.id.corner_tl);
        cornerTr = findViewById(R.id.corner_tr);
        cornerBl = findViewById(R.id.corner_bl);
        cornerBr = findViewById(R.id.corner_br);

        // Preview Card Binding
        productPreviewCard = findViewById(R.id.product_preview_card);
        previewProductImage = findViewById(R.id.preview_product_image);
        previewProductName = findViewById(R.id.preview_product_name);
        previewProductBrand = findViewById(R.id.preview_product_brand);
        previewProductScore = findViewById(R.id.preview_product_score);
        previewScoreContainer = findViewById(R.id.preview_score_container);    }

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

        // Camera capture button (used for both taking photos and manual barcode capture)
        if (cameraCaptureButton != null) {
            cameraCaptureButton.setOnClickListener(v -> {
                animateButtonPress(v);
                if (currentMode == ScanMode.PRODUCT_DETECT) {
                    capturePhotoForDetection();
                } else {
                    // Fallback to taking photo if someone presses it in barcode mode
                    capturePhotoForDetection();
                }
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
                    bindCameraUseCases(); // Rebind to enable barcode analyzer
                }
            });
        }
        
        if (modeDetectText != null) {
            modeDetectText.setOnClickListener(v -> {
                if (currentMode != ScanMode.PRODUCT_DETECT) {
                    currentMode = ScanMode.PRODUCT_DETECT;
                    updateModeUI();
                    bindCameraUseCases(); // Rebind to disable barcode analyzer if needed
                }
            });
        }
    }
    
    private void updateModeUI() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT && modeBarcodeText != null) {
            android.transition.TransitionManager.beginDelayedTransition((android.view.ViewGroup) modeBarcodeText.getParent());
        }

        if (currentMode == ScanMode.BARCODE) {
            if (modeBarcodeText != null) {
                modeBarcodeText.setBackgroundResource(R.drawable.mode_toggle_active);
                modeBarcodeText.setTextColor(ContextCompat.getColor(this, R.color.bg_deep_navy));
            }
            if (modeDetectText != null) {
                modeDetectText.setBackgroundResource(R.drawable.mode_toggle_inactive);
                modeDetectText.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            }

            if (scanningFrameCard != null) {
                scanningFrameCard.setVisibility(View.VISIBLE);
            }
            if (scanningFrame != null) {
                scanningFrame.setVisibility(View.VISIBLE);
            }
            if (scanningLine != null) {
                scanningLine.setVisibility(View.VISIBLE);
            }
            if (detectCrosshair != null) {
                detectCrosshair.setVisibility(View.GONE);
            }
            if (scannerOverlay != null) {
                scannerOverlay.setVisibility(View.VISIBLE);
                scannerOverlay.setTargetFrameView(scanningFrameCard, 32f);
            }
            if (cameraCaptureButton != null) {
                cameraCaptureButton.setVisibility(View.INVISIBLE);
            }

            if (instructionsText != null) {
                instructionsText.setText("Align the barcode within the frame to scan automatically");
            }

            if (productPreviewCard != null) {
                productPreviewCard.setVisibility(View.GONE);
            }

            setScannerState(ScanState.IDLE, null);
            startScanningLineAnimation();

        } else {
            if (modeBarcodeText != null) {
                modeBarcodeText.setBackgroundResource(R.drawable.mode_toggle_inactive);
                modeBarcodeText.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            }
            if (modeDetectText != null) {
                modeDetectText.setBackgroundResource(R.drawable.mode_toggle_active);
                modeDetectText.setTextColor(ContextCompat.getColor(this, R.color.bg_deep_navy));
            }

            if (scanningFrameCard != null) {
                scanningFrameCard.setVisibility(View.GONE);
            }
            if (scanningFrame != null) {
                scanningFrame.setVisibility(View.GONE);
            }
            if (scanningLine != null) {
                scanningLine.setVisibility(View.GONE);
            }
            if (detectCrosshair != null) {
                detectCrosshair.setVisibility(View.VISIBLE);
            }
            if (scannerOverlay != null) {
                scannerOverlay.setVisibility(View.GONE);
            }
            if (cameraCaptureButton != null) {
                cameraCaptureButton.setVisibility(View.VISIBLE);
            }

            if (instructionsText != null) {
                instructionsText.setText("Tap capture to recognize food item");
            }

            if (scanStatusCard != null) {
                scanStatusCard.setVisibility(View.GONE);
            }
            if (productPreviewCard != null) {
                productPreviewCard.setVisibility(View.GONE);            }
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
        
        // Also animate the corner indicators
        if (cornerTl != null && cornerTr != null && cornerBl != null && cornerBr != null) {
            Animation pulseAnim = AnimationUtils.loadAnimation(this, R.anim.corner_pulse_animation);
            cornerTl.startAnimation(pulseAnim);
            cornerTr.startAnimation(pulseAnim);
            cornerBl.startAnimation(pulseAnim);
            cornerBr.startAnimation(pulseAnim);
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
                showScanStatus("❌ Camera permission required to scan", false);
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(this::finish, 3000);
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
        
        // Set up barcode analyzer (only process if in barcode mode)
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this),
                new BarcodeAnalyzer(barcode -> {
                    if (currentMode == ScanMode.BARCODE) {
                        onBarcodeDetected(barcode);
                    }
                }));

        // Image capture use case for taking photos
        imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .setFlashMode(isFlashOn ? ImageCapture.FLASH_MODE_ON : ImageCapture.FLASH_MODE_OFF)
                .build();

        // Camera selector (back camera)
        CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

        try {
            // Unbind all use cases before rebinding
            cameraProvider.unbindAll();
            
            // Bind use cases to camera
            androidx.camera.core.Camera camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalysis, imageCapture);

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
        if (isScanning || currentMode != ScanMode.BARCODE) return; // Prevent multiple scans
        
        if (isScanning)
            return; // Prevent multiple scans

        isScanning = true;

        performHapticFeedback();
        
        // Set state to detected immediately
        setScannerState(ScanState.DETECTED, "Analyzing product information...");
        
        // Success animation on scanning frame card
        if (scanningFrameCard != null) {
            Animation successAnim = AnimationUtils.loadAnimation(this, R.anim.success_indicator_animation);
            scanningFrameCard.startAnimation(successAnim);        }

        // Fetch product details for preview
        fetchProductForPreview(barcode);
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

    /**
     * Capture a photo and send it to the YOLO API.
     */
    private void capturePhotoForDetection() {
        if (imageCapture == null) {
            isCapturing = false;
            showScanStatus("❌ Camera not ready", false);
            return;
        }

        isCapturing = true;
        setCaptureButtonEnabled(false);
        showScanStatus("📸 Capturing image for detection...", true);

        File photoFile = new File(getCacheDir(), "DETECT_" + System.currentTimeMillis() + ".jpg");
        androidx.camera.core.ImageCapture.OutputFileOptions options = 
                new androidx.camera.core.ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(options, androidx.core.content.ContextCompat.getMainExecutor(this),
                new androidx.camera.core.ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull androidx.camera.core.ImageCapture.OutputFileResults outputFileResults) {
                        runOnUiThread(() -> {
                            showScanStatus("🔍 Analyzing image with YOLO...", true);
                            detectProductViaApi(photoFile);
                        });
                    }

                    @Override
                    public void onError(@NonNull androidx.camera.core.ImageCaptureException exception) {
                        Log.e(TAG, "Photo capture failed: " + exception.getMessage(), exception);
                        runOnUiThread(() -> {
                            isCapturing = false;
                            setCaptureButtonEnabled(true);
                            showScanStatus("❌ Capture failed", false);
                        });
                    }
                });
    }

    private void detectProductViaApi(File photoFile) {
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
                    
                    // Show error in result activity anyway
                    Intent intent = new Intent(VerticalScannerActivity.this, ApiDetectionResultActivity.class);
                    intent.putExtra("image_path", photoFile.getAbsolutePath());
                    intent.putExtra("error", "Network Error: " + e.getMessage());
                    startActivity(intent);
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "API Error: " + response.code());
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

                    runOnUiThread(() -> {
                        isCapturing = false;
                        setCaptureButtonEnabled(true);
                        
                        if (product.equals("fallback_product")) {
                            showScanStatus("🔍 Manual Selection Required", true);
                            Intent intent = new Intent(VerticalScannerActivity.this, ProductSelectionActivity.class);
                            intent.putExtra("image_path", photoFile.getAbsolutePath());
                            startActivity(intent);
                        } else if (confidence >= 0.6) {
                            showScanStatus("✅ Product detected!", true);
                            
                            Intent intent = new Intent(VerticalScannerActivity.this, ApiDetectionResultActivity.class);
                            intent.putExtra("image_path", photoFile.getAbsolutePath());
                            intent.putExtra("product", product);
                            intent.putExtra("confidence", confidence);
                            if (nutrition != null) {
                                intent.putExtra("nutrition_json", nutrition.toString());
                            }
                            startActivity(intent);
                        } else {
                            showScanStatus("🔍 Low confidence. Falling back to Barcode Scanner...", true);
                            fallbackPhotoFile = photoFile;
                            fallbackYoloConfidence = confidence;
                            
                            // Immediately force barcode mode
                            if (currentMode != ScanMode.BARCODE) {
                                currentMode = ScanMode.BARCODE;
                                updateModeUI();
                                bindCameraUseCases();
                            }
                        }
                    });

                } catch (Exception e) {
                    Log.e(TAG, "JSON parsing error / Invalid Detection", e);
                    runOnUiThread(() -> {
                        isCapturing = false;
                        setCaptureButtonEnabled(true);
                        showScanStatus("🔍 Detection failed. Falling back to Barcode Scanner...", true);
                        fallbackPhotoFile = photoFile;
                        fallbackYoloConfidence = 0.0;
                        
                        if (currentMode != ScanMode.BARCODE) {
                            currentMode = ScanMode.BARCODE;
                            updateModeUI();
                            bindCameraUseCases();
                        }
                    });
                }
            }
        });
    }
    
    private void runOcrFallback(File photoFile) {
        try {
            Bitmap bitmap = BitmapFactory.decodeFile(photoFile.getAbsolutePath());
            if (bitmap == null) throw new Exception("Failed to decode image");
            
            com.google.mlkit.vision.common.InputImage image = com.google.mlkit.vision.common.InputImage.fromBitmap(bitmap, 0);
            com.google.mlkit.vision.text.TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
            
            recognizer.process(image)
                    .addOnSuccessListener(visionText -> {
                        String text = visionText.getText();
                        if (text != null && !text.trim().isEmpty()) {
                            sendOcrToApi(text, photoFile);
                        } else {
                            // OCR found no text, give up
                            isCapturing = false;
                            setCaptureButtonEnabled(true);
                            showScanStatus("❌ Detection Failed (No Text)", false);
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
        okhttp3.RequestBody requestBody = new okhttp3.FormBody.Builder()
                .add("text", text)
                .build();
        
        Request request = new Request.Builder()
                .url(ApiConfig.API_URL_OCR_DETECT)
                .post(requestBody)
                .build();
                
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("OCR_ERROR", e.toString());
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
                Log.d("OCR_RESPONSE", responseData);
                try {
                    org.json.JSONObject json = new org.json.JSONObject(responseData);
                    String product = json.has("product") && !json.isNull("product") ? json.optString("product") : "Unknown";
                    double confidence = json.has("confidence") ? json.optDouble("confidence") : 0.0;
                    org.json.JSONObject nutrition = json.optJSONObject("nutrition");
                    org.json.JSONArray similarProductsArray = json.optJSONArray("similar_products");
                    String similarProductsStr = similarProductsArray != null ? similarProductsArray.toString() : "[]";
                    
                    runOnUiThread(() -> {
                        isCapturing = false;
                        setCaptureButtonEnabled(true);
                        
                        if (product.equals("fallback_product")) {
                            showScanStatus("🔍 Manual Selection Required", true);
                            Intent intent = new Intent(VerticalScannerActivity.this, ProductSelectionActivity.class);
                            intent.putExtra("image_path", photoFile.getAbsolutePath());
                            startActivity(intent);
                        } else if (product != null && !product.equals("null") && !product.equals("Unknown")) {
                            showScanStatus("✅ Product matched via OCR!", true);
                            Intent intent = new Intent(VerticalScannerActivity.this, ApiDetectionResultActivity.class);
                            intent.putExtra("image_path", photoFile.getAbsolutePath());
                            intent.putExtra("product", product);
                            intent.putExtra("confidence", confidence);
                            intent.putExtra("similar_products", similarProductsStr);
                            if (nutrition != null) {
                                intent.putExtra("nutrition_json", nutrition.toString());
                            }
                            startActivity(intent);
                        } else {
                            showScanStatus("🔍 Manual Selection Required", true);
                            Intent intent = new Intent(VerticalScannerActivity.this, ApiDetectionResultActivity.class);
                            intent.putExtra("image_path", photoFile.getAbsolutePath());
                            intent.putExtra("product", "Unknown");
                            intent.putExtra("confidence", 0.0);
                            intent.putExtra("similar_products", similarProductsStr);
                            startActivity(intent);
                        }
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        isCapturing = false;
                        setCaptureButtonEnabled(true);
                        showScanStatus("❌ OCR Error parsing response", false);
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
        
        // Handle ZXing result

        // Handle ZXing result from manual entry
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null && result.getContents() != null) {
            currentMode = ScanMode.BARCODE;
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
        if (!isSuccess) {
            setScannerState(ScanState.ERROR, message);
            if (scanStatusCard != null) {                scanStatusCard.postDelayed(() -> {
                    if (scanStatusCard != null && currentMode == ScanMode.BARCODE) {
                        setScannerState(ScanState.IDLE, null);
                    } else if (scanStatusCard != null) {
                        scanStatusCard.setVisibility(View.GONE);
                    }
                }, 3000);
            }
        } else if (message.contains("detected") || message.contains("Recognizing") || message.contains("Loading")) {
            setScannerState(ScanState.DETECTED, message);
        } else if (message.contains("Ready")) {
            setScannerState(ScanState.IDLE, null);
        } else if (message.contains("recognized") || message.contains("Matched") || message.contains("Found")) {
            setScannerState(ScanState.SUCCESS, message);
        } else {
            if (scanStatusText != null) scanStatusText.setText(message);
            if (scanStatusSubtext != null) scanStatusSubtext.setText("");
            if (scanProgress != null) scanProgress.setVisibility(View.GONE);
        }
    }

    private void setScannerState(ScanState state, String customMessage) {
        if (scanStatusCard == null || scanStatusText == null) return;
        
        int borderColor = R.color.glass_border;
        int bgColor = 0xBF141419; // Default dark graphite glass
        int tintColor = R.color.white;
        
        String title = "";
        String subtitle = "";
        
        switch (state) {
            case IDLE:
                title = "Ready to Scan";
                subtitle = "Align a barcode within the frame.";
                borderColor = R.color.glass_border;
                tintColor = R.color.white;
                if (scanProgress != null) scanProgress.setVisibility(View.GONE);
                break;
            case DETECTED:
                title = "Barcode Detected";
                subtitle = customMessage != null ? customMessage : "Analyzing product information...";
                borderColor = R.color.primary_teal;
                tintColor = R.color.primary_teal;
                if (scanProgress != null) scanProgress.setVisibility(View.VISIBLE);
                break;
            case SUCCESS:
                title = "Product Found";
                subtitle = "Preparing results...";
                borderColor = R.color.success_color;
                tintColor = R.color.success_color;
                bgColor = 0xD90F1F15; // Tinted green-dark background
                if (scanProgress != null) scanProgress.setVisibility(View.GONE);
                break;
            case ERROR:
                title = "Product Not Found";
                subtitle = customMessage != null ? customMessage : "Try another barcode.";
                borderColor = R.color.error_color;
                tintColor = R.color.error_color;
                bgColor = 0xD92A1414; // Tinted red-dark background
                if (scanProgress != null) scanProgress.setVisibility(View.GONE);
                break;
        }
        
        scanStatusText.setText(title);
        if (scanStatusSubtext != null) {
            scanStatusSubtext.setText(subtitle);
        }
        
        if (scanStatusCard instanceof com.google.android.material.card.MaterialCardView) {
            com.google.android.material.card.MaterialCardView mCard = 
                    (com.google.android.material.card.MaterialCardView) scanStatusCard;
            mCard.setStrokeColor(ContextCompat.getColor(this, borderColor));
            mCard.setCardBackgroundColor(android.content.res.ColorStateList.valueOf(bgColor));
        }
        
        int solvedTint = ContextCompat.getColor(this, tintColor);
        if (cornerTl != null) cornerTl.setColorFilter(solvedTint);
        if (cornerTr != null) cornerTr.setColorFilter(solvedTint);
        if (cornerBl != null) cornerBl.setColorFilter(solvedTint);
        if (cornerBr != null) cornerBr.setColorFilter(solvedTint);
        
        scanStatusCard.setVisibility(View.VISIBLE);
    }

    private void fetchProductForPreview(String barcode) {
        String url = "https://world.openfoodfacts.org/api/v0/product/" + barcode + ".json";
        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(url)
                .build();

        httpClient.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, java.io.IOException e) {
                Log.e(TAG, "Failed to fetch product preview", e);
                runOnUiThread(() -> {
                    populatePreviewCard(barcode, "Product Detected", "Tap to view details", null, 70);
                });
            }

            @Override
            public void onResponse(okhttp3.Call call, okhttp3.Response response) throws java.io.IOException {
                if (!response.isSuccessful()) {
                    runOnUiThread(() -> {
                        populatePreviewCard(barcode, "Product Detected", "Tap to view details", null, 70);
                    });
                    return;
                }

                try {
                    String responseBody = response.body().string();
                    org.json.JSONObject json = new org.json.JSONObject(responseBody);
                    if (json.has("status") && json.getInt("status") == 1) {
                        org.json.JSONObject product = json.getJSONObject("product");
                        String name = product.optString("product_name", "Unknown Product");
                        String brand = product.optString("brands", "Unknown Brand");
                        String rawImageUrl = product.optString("image_front_url", "");
                        if (rawImageUrl.isEmpty()) {
                            rawImageUrl = product.optString("image_url", "");
                        }
                        final String imageUrl = rawImageUrl;

                        double score = 65;
                        org.json.JSONObject nutriments = product.optJSONObject("nutriments");
                        if (nutriments != null) {
                            double calories = nutriments.optDouble("energy-kcal_100g", 0);
                            double sugar = nutriments.optDouble("sugars_100g", 0);
                            double fat = nutriments.optDouble("fat_100g", 0);
                            double protein = nutriments.optDouble("proteins_100g", 0);
                            double fiber = nutriments.optDouble("fiber_100g", 0);
                            double sodium = nutriments.optDouble("sodium_100g", 0) * 1000;

                            double calcScore = 0;
                            if (calories <= 100) calcScore += 20;
                            else if (calories <= 200) calcScore += 16;
                            else if (calories <= 300) calcScore += 12;
                            else if (calories <= 400) calcScore += 8;

                            if (sugar <= 2) calcScore += 20;
                            else if (sugar <= 5) calcScore += 16;
                            else if (sugar <= 10) calcScore += 12;

                            if (fat <= 3) calcScore += 15;
                            else if (fat <= 10) calcScore += 12;

                            if (protein >= 20) calcScore += 15;
                            else if (protein >= 10) calcScore += 9;

                            if (fiber >= 5) calcScore += 15;
                            else if (fiber >= 2) calcScore += 9;

                            if (sodium <= 100) calcScore += 15;
                            else if (sodium <= 300) calcScore += 12;

                            score = Math.min(calcScore, 100);
                        }

                        double finalScore = score;
                        runOnUiThread(() -> {
                            populatePreviewCard(barcode, name, brand, imageUrl, (int) finalScore);
                        });
                    } else {
                        runOnUiThread(() -> {
                            int type = Math.abs(barcode.hashCode()) % 4;
                            String name = "Organic Whole Grain Cereal";
                            String brand = "HealthyChoice";
                            int score = 85;
                            if (type == 1) {
                                name = "Greek Yogurt Natural";
                                brand = "FreshDairy";
                                score = 90;
                            } else if (type == 2) {
                                name = "Dark Chocolate Bar 70%";
                                brand = "SweetTreats";
                                score = 45;
                            } else if (type == 3) {
                                name = "Fresh Red Apple";
                                brand = "Nature's Best";
                                score = 98;
                            }
                            populatePreviewCard(barcode, name, brand, null, score);
                        });
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing preview response", e);
                    runOnUiThread(() -> {
                        populatePreviewCard(barcode, "Product Detected", "Tap to view details", null, 70);
                    });
                }
            }
        });
    }

    private void populatePreviewCard(String barcode, String name, String brand, String imageUrl, int score) {
        if (productPreviewCard == null) return;

        if (previewProductName != null) previewProductName.setText(name);
        if (previewProductBrand != null) previewProductBrand.setText(brand);
        if (previewProductScore != null) previewProductScore.setText(String.valueOf(score));

        if (previewScoreContainer != null) {
            int scoreColor;
            if (score >= 75) {
                scoreColor = ContextCompat.getColor(this, R.color.health_score_high);
            } else if (score >= 50) {
                scoreColor = ContextCompat.getColor(this, R.color.health_score_mid);
            } else {
                scoreColor = ContextCompat.getColor(this, R.color.health_score_low);
            }
            previewScoreContainer.setCardBackgroundColor(scoreColor);
        }

        loadPreviewImage(imageUrl);

        setScannerState(ScanState.SUCCESS, "Preparing results...");

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            android.transition.TransitionManager.beginDelayedTransition((android.view.ViewGroup) productPreviewCard.getParent());        }
        productPreviewCard.setVisibility(View.VISIBLE);

        if (scanStatusCard != null) {
            scanStatusCard.setVisibility(View.GONE);
        }

        productPreviewCard.setOnClickListener(v -> {
            animateButtonPress(v);
            navigateToProductDetails(barcode);
        });

        productPreviewCard.postDelayed(() -> {
            if (isFinishing() || isDestroyed()) return;
            navigateToProductDetails(barcode);
        }, 2000);
    }

    private void loadPreviewImage(String imageUrl) {
        if (previewProductImage == null) return;
        
        if (imageUrl != null && !imageUrl.isEmpty()) {
            new Thread(() -> {
                try {
                    java.net.URL url = new java.net.URL(imageUrl);
                    java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
                    connection.setDoInput(true);
                    connection.connect();
                    
                    java.io.InputStream input = connection.getInputStream();
                    android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(input);
                    
                    runOnUiThread(() -> {
                        if (bitmap != null) {
                            previewProductImage.setImageBitmap(bitmap);
                        } else {
                            previewProductImage.setImageResource(R.drawable.ic_product_placeholder);
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Error loading preview image", e);
                    runOnUiThread(() -> {
                        previewProductImage.setImageResource(R.drawable.ic_product_placeholder);
                    });
                }
            }).start();
        } else {
            previewProductImage.setImageResource(R.drawable.ic_product_placeholder);
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
        
        // Return capture button to enabled state just in case it got stuck disabled
        isCapturing = false;
        isNavigating = false;
        setCaptureButtonEnabled(true);
        if (productPreviewCard != null) {
            productPreviewCard.setVisibility(View.GONE);
        }
        updateModeUI();    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
    }
}