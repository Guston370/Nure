package com.example.healthscanner.network;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.util.Log;

import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.example.healthscanner.database.queue.AppDatabase;
import com.example.healthscanner.database.queue.DataQueueEntity;
import com.example.healthscanner.database.queue.DataSyncWorker;

import java.io.File;
import java.io.FileOutputStream;
import java.util.UUID;

public class PipelineManager {
    private static final String TAG = "PipelineManager";
    private static final String PREF_NAME = "pipeline_prefs";
    private static final String PREF_DEVICE_ID = "device_id";

    public interface VerificationCallback {
        void onVerified(String label, String source);
        void onFailure();
    }

    // Phase 1: Verify barcode strictly
    public static void verifyBarcodeStrictly(String barcode, VerificationCallback callback) {
        if (!isValidBarcode(barcode)) {
            callback.onFailure();
            return;
        }

        BarcodeApiClient.fetchProductNameWithRetry(barcode, new BarcodeApiClient.BarcodeCallback() {
            @Override
            public void onSuccess(String sanitizedProductName) {
                callback.onVerified(sanitizedProductName, "barcode_api");
            }

            @Override
            public void onFailure(String errorMsg) {
                Log.e(TAG, "Barcode API hard failure: " + errorMsg);
                callback.onFailure(); // Will branch UX logic to ask for Fallback
            }
        });
    }

    // Phase 2: Capture Image & Commit Offline Queue
    public static void commitToOfflineQueue(Context context, String label, String barcode, Bitmap bitmap, String source) {
        new Thread(() -> {
            try {
                // 1. Label Validation & Sanitization
                String sanitizedLabel = sanitizeLabel(label);
                if (sanitizedLabel.isEmpty()) {
                    Log.e(TAG, "invalid label: " + label);
                    return;
                }

                // 2. Image Quality Check
                ImageQualityHelper.QualityResult quality = ImageQualityHelper.assessImageQuality(bitmap);
                if (quality != ImageQualityHelper.QualityResult.VALID) {
                    Log.w(TAG, "bad image rejected: " + quality.name());
                    return;
                }

                long timestampMs = System.currentTimeMillis();
                long timestampSec = timestampMs / 1000;
                String uuidStr = UUID.randomUUID().toString();
                
                // 3. File Naming Rule: label_timestamp_uuid.jpg
                // Replace spaces with underscores for the filename
                String fileNameLabel = sanitizedLabel.replace(" ", "_");
                String fileName = String.format("%s_%d_%s.jpg", fileNameLabel, timestampSec, uuidStr);
                File cacheDir = context.getCacheDir();
                File imageFile = new File(cacheDir, fileName);

                FileOutputStream out = new FileOutputStream(imageFile);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out); 
                out.flush();
                out.close();

                String deviceId = getOrCreateDeviceId(context);

                DataQueueEntity entity = new DataQueueEntity(
                        uuidStr,
                        sanitizedLabel,
                        barcode,
                        imageFile.getAbsolutePath(),
                        timestampMs,
                        source,
                        deviceId,
                        "pending"
                );

                AppDatabase.getDatabase(context).queueDao().insert(entity);
                Log.d(TAG, "Commited to SQLite Queue: " + fileName);
                
                triggerSyncWorker(context);

            } catch (Exception e) {
                Log.e(TAG, "Failed compressing mapping to offline queue", e);
            }
        }).start();
    }

    private static String sanitizeLabel(String label) {
        if (label == null) return "";
        // Remove special characters, lowercase + trim
        String sanitized = label.toLowerCase().trim();
        sanitized = sanitized.replaceAll("[^a-z0-9\\s]", " "); // keep alphanumeric and spaces
        sanitized = sanitized.replaceAll("\\s+", " ").trim(); // collapse spaces
        return sanitized;
    }

    private static boolean isValidBarcode(String barcode) {
        if (barcode == null || barcode.isEmpty()) return false;
        // Basic EAN/UPC verification
        return barcode.matches("^[0-9]{8,14}$");
    }

    public static String getOrCreateDeviceId(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String deviceId = prefs.getString(PREF_DEVICE_ID, null);
        if (deviceId == null) {
            deviceId = UUID.randomUUID().toString();
            prefs.edit().putString(PREF_DEVICE_ID, deviceId).apply();
        }
        return deviceId;
    }

    public static void triggerSyncWorker(Context context) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest syncRequest = new OneTimeWorkRequest.Builder(DataSyncWorker.class)
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(context).enqueue(syncRequest);
    }
}
