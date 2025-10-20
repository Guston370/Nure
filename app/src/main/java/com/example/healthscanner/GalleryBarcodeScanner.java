package com.example.healthscanner;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Gallery image barcode scanner using ML Kit
 */
public class GalleryBarcodeScanner {
    private static final String TAG = "GalleryBarcodeScanner";
    
    private Context context;
    private ExecutorService executorService;
    private BarcodeScanner mlKitBarcodeScanner;
    
    public interface GalleryScanCallback {
        void onBarcodeDetected(String barcode);
        void onError(String error);
    }
    
    public GalleryBarcodeScanner(Context context) {
        this.context = context;
        this.executorService = Executors.newSingleThreadExecutor();
        
        // Configure ML Kit barcode scanner for gallery images
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
    
    public void scanImageFromUri(Uri imageUri, GalleryScanCallback callback) {
        executorService.execute(() -> {
            try {
                // Load bitmap from URI
                Bitmap bitmap = loadBitmapFromUri(imageUri);
                if (bitmap == null) {
                    callback.onError("Failed to load image");
                    return;
                }
                
                // Create InputImage from bitmap
                InputImage inputImage = InputImage.fromBitmap(bitmap, 0);
                
                // Process the image
                mlKitBarcodeScanner.process(inputImage)
                        .addOnSuccessListener(barcodes -> {
                            if (!barcodes.isEmpty()) {
                                Barcode barcode = barcodes.get(0);
                                String barcodeText = barcode.getRawValue();
                                
                                if (barcodeText != null && !barcodeText.isEmpty()) {
                                    Log.d(TAG, "Barcode detected in gallery image: " + barcodeText);
                                    callback.onBarcodeDetected(barcodeText);
                                } else {
                                    callback.onError("No valid barcode found in image");
                                }
                            } else {
                                callback.onError("No barcode found in image");
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Gallery barcode detection failed", e);
                            callback.onError("Failed to detect barcode: " + e.getMessage());
                        });
                        
            } catch (Exception e) {
                Log.e(TAG, "Error processing gallery image", e);
                callback.onError("Error processing image: " + e.getMessage());
            }
        });
    }
    
    public void scanImageFromBitmap(Bitmap bitmap, GalleryScanCallback callback) {
        executorService.execute(() -> {
            try {
                // Create InputImage from bitmap
                InputImage inputImage = InputImage.fromBitmap(bitmap, 0);
                
                // Process the image
                mlKitBarcodeScanner.process(inputImage)
                        .addOnSuccessListener(barcodes -> {
                            if (!barcodes.isEmpty()) {
                                Barcode barcode = barcodes.get(0);
                                String barcodeText = barcode.getRawValue();
                                
                                if (barcodeText != null && !barcodeText.isEmpty()) {
                                    Log.d(TAG, "Barcode detected in bitmap: " + barcodeText);
                                    callback.onBarcodeDetected(barcodeText);
                                } else {
                                    callback.onError("No valid barcode found in image");
                                }
                            } else {
                                callback.onError("No barcode found in image");
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Bitmap barcode detection failed", e);
                            callback.onError("Failed to detect barcode: " + e.getMessage());
                        });
                        
            } catch (Exception e) {
                Log.e(TAG, "Error processing bitmap", e);
                callback.onError("Error processing image: " + e.getMessage());
            }
        });
    }
    
    private Bitmap loadBitmapFromUri(Uri uri) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            if (inputStream != null) {
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                inputStream.close();
                return bitmap;
            }
        } catch (IOException e) {
            Log.e(TAG, "Error loading bitmap from URI", e);
        }
        return null;
    }
    
    public void cleanup() {
        if (mlKitBarcodeScanner != null) {
            mlKitBarcodeScanner.close();
        }
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}
