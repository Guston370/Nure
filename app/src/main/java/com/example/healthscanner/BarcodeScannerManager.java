package com.example.healthscanner;

import android.content.Context;
import android.util.Log;
import android.util.Size;
import androidx.camera.core.*;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Advanced barcode scanner using CameraX and ML Kit
 */
public class BarcodeScannerManager {
    private static final String TAG = "BarcodeScannerManager";
    
    private Context context;
    private PreviewView previewView;
    private ProcessCameraProvider cameraProvider;
    private Camera camera;
    private ExecutorService cameraExecutor;
    private BarcodeScanner mlKitBarcodeScanner;
    
    private BarcodeCallback barcodeCallback;
    private boolean isScanning = false;
    private boolean isTorchOn = false;
    
    public interface BarcodeCallback {
        void onBarcodeDetected(String barcode);
        void onError(String error);
    }
    
    public BarcodeScannerManager(Context context, PreviewView previewView) {
        this.context = context;
        this.previewView = previewView;
        this.cameraExecutor = Executors.newSingleThreadExecutor();
        
        // Configure ML Kit barcode scanner
        BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                    Barcode.FORMAT_EAN_13,
                    Barcode.FORMAT_EAN_8,
                    Barcode.FORMAT_UPC_A,
                    Barcode.FORMAT_UPC_E,
                    Barcode.FORMAT_CODE_128,
                    Barcode.FORMAT_CODE_39,
                    Barcode.FORMAT_CODE_93,
                    Barcode.FORMAT_CODABAR,
                    Barcode.FORMAT_ITF,
                    Barcode.FORMAT_QR_CODE,
                    Barcode.FORMAT_DATA_MATRIX,
                    Barcode.FORMAT_PDF417,
                    Barcode.FORMAT_AZTEC
                )
                .build();
        
        this.mlKitBarcodeScanner = BarcodeScanning.getClient(options);
    }
    
    public void startScanning(BarcodeCallback callback) {
        this.barcodeCallback = callback;
        this.isScanning = true;
        
        ProcessCameraProvider.getInstance(context)
                .addListener(() -> {
                    try {
                        cameraProvider = ProcessCameraProvider.getInstance(context).get();
                        bindCameraUseCases();
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to initialize camera", e);
                        if (callback != null) {
                            callback.onError("Failed to initialize camera: " + e.getMessage());
                        }
                    }
                }, ContextCompat.getMainExecutor(context));
    }
    
    private void bindCameraUseCases() {
        if (cameraProvider == null) return;
        
        // Preview use case
        Preview preview = new Preview.Builder()
                .setTargetResolution(new Size(1280, 720))
                .build();
        
        preview.setSurfaceProvider(previewView.getSurfaceProvider());
        
        // Image analysis use case for barcode detection
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setTargetResolution(new Size(1280, 720))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();
        
        imageAnalysis.setAnalyzer(cameraExecutor, new BarcodeAnalyzer());
        
        // Camera selector - prefer back camera
        CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
        
        try {
            // Unbind use cases before rebinding
            cameraProvider.unbindAll();
            
            // Bind use cases to camera
            camera = cameraProvider.bindToLifecycle(
                    (LifecycleOwner) context,
                    cameraSelector,
                    preview,
                    imageAnalysis
            );
            
            Log.d(TAG, "Camera bound successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to bind camera use cases", e);
            if (barcodeCallback != null) {
                barcodeCallback.onError("Failed to bind camera: " + e.getMessage());
            }
        }
    }
    
    private class BarcodeAnalyzer implements ImageAnalysis.Analyzer {
        @Override
        public void analyze(ImageProxy image) {
            if (!isScanning) {
                image.close();
                return;
            }
            
            InputImage inputImage = InputImage.fromMediaImage(
                    image.getImage(), 
                    image.getImageInfo().getRotationDegrees()
            );
            
            mlKitBarcodeScanner.process(inputImage)
                    .addOnSuccessListener(barcodes -> {
                        if (isScanning && !barcodes.isEmpty()) {
                            Barcode barcode = barcodes.get(0);
                            String barcodeText = barcode.getRawValue();
                            
                            if (barcodeText != null && !barcodeText.isEmpty()) {
                                Log.d(TAG, "Barcode detected: " + barcodeText);
                                isScanning = false; // Stop scanning after detection
                                
                                if (barcodeCallback != null) {
                                    barcodeCallback.onBarcodeDetected(barcodeText);
                                }
                            }
                        }
                        image.close();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Barcode detection failed", e);
                        image.close();
                    });
        }
    }
    
    public void stopScanning() {
        isScanning = false;
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
    }
    
    public void resumeScanning() {
        isScanning = true;
        if (cameraProvider != null) {
            bindCameraUseCases();
        }
    }
    
    public void toggleTorch() {
        if (camera != null) {
            try {
                isTorchOn = !isTorchOn;
                camera.getCameraControl().enableTorch(isTorchOn);
                Log.d(TAG, "Torch " + (isTorchOn ? "enabled" : "disabled"));
            } catch (Exception e) {
                Log.e(TAG, "Failed to toggle torch", e);
            }
        }
    }
    
    public boolean isTorchOn() {
        return isTorchOn;
    }
    
    public void cleanup() {
        stopScanning();
        if (mlKitBarcodeScanner != null) {
            mlKitBarcodeScanner.close();
        }
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
    }
}
