package com.example.healthscanner;

import android.annotation.SuppressLint;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

/**
 * Barcode analyzer for camera-based scanning using ML Kit
 */
public class BarcodeAnalyzer implements ImageAnalysis.Analyzer {
    
    private static final String TAG = "BarcodeAnalyzer";
    
    private final BarcodeScanner scanner;
    private final OnBarcodeDetectedListener listener;
    private boolean isProcessing = false;
    
    public interface OnBarcodeDetectedListener {
        void onBarcodeDetected(String barcode);
    }
    
    public BarcodeAnalyzer(OnBarcodeDetectedListener listener) {
        this.listener = listener;
        
        // Configure barcode scanner for common formats
        BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E,
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_CODE_128,
                Barcode.FORMAT_CODE_39,
                Barcode.FORMAT_CODE_93,
                Barcode.FORMAT_CODABAR,
                Barcode.FORMAT_ITF,
                Barcode.FORMAT_QR_CODE
            )
            .build();
        
        scanner = BarcodeScanning.getClient(options);
    }
    
    @Override
    @SuppressLint("UnsafeOptInUsageError")
    public void analyze(@NonNull ImageProxy imageProxy) {
        if (isProcessing) {
            imageProxy.close();
            return;
        }
        
        if (imageProxy.getImage() == null) {
            imageProxy.close();
            return;
        }
        
        isProcessing = true;
        
        InputImage image = InputImage.fromMediaImage(
            imageProxy.getImage(),
            imageProxy.getImageInfo().getRotationDegrees()
        );
        
        scanner.process(image)
            .addOnSuccessListener(barcodes -> {
                for (Barcode barcode : barcodes) {
                    String rawValue = barcode.getRawValue();
                    if (rawValue != null && !rawValue.isEmpty()) {
                        Log.d(TAG, "Barcode detected: " + rawValue);
                        if (listener != null) {
                            listener.onBarcodeDetected(rawValue);
                        }
                        break; // Only process the first barcode found
                    }
                }
                isProcessing = false;
                imageProxy.close();
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Barcode scanning failed", e);
                isProcessing = false;
                imageProxy.close();
            });
    }
}