package com.example.healthscanner.database.queue;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class DataSyncWorker extends Worker {

    private static final String TAG = "DataSyncWorker";
    
    private final FirebaseStorage storage;
    private final FirebaseFirestore firestore;
    private final QueueDao queueDao;

    public DataSyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        storage = FirebaseStorage.getInstance();
        firestore = FirebaseFirestore.getInstance();
        queueDao = AppDatabase.getDatabase(context).queueDao();
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "SyncWorker initiated. Attempt: " + getRunAttemptCount());
        
        if (getRunAttemptCount() > 5) {
            Log.e(TAG, "Max retry limit reached. Aborting this work request.");
            return Result.failure();
        }
        
        List<DataQueueEntity> pendingUploads = queueDao.getPendingUploads();
        
        if (pendingUploads.isEmpty()) {
            return Result.success();
        }

        boolean hasFailures = false;

        for (DataQueueEntity entity : pendingUploads) {
            
            // Mark uploading state
            entity.status = "uploading";
            queueDao.update(entity);
            
            boolean success = processUpload(entity);
            
            if (success) {
                Log.d(TAG, "Removing successfully synced entity: " + entity.uuid);
                // Data Safety requirement: delete local image until upload success
                if (entity.localImagePath != null) {
                    File localImg = new File(entity.localImagePath);
                    if (localImg.exists()) {
                        boolean deleted = localImg.delete();
                        Log.d(TAG, "Local image deleted post-sync: " + deleted);
                    }
                }
                // Purge safely
                queueDao.delete(entity);
            } else {
                Log.w(TAG, "Entity upload failed, marking pending for next cycle: " + entity.uuid);
                entity.status = "failed";
                queueDao.update(entity);
                hasFailures = true;
            }
        }

        if (hasFailures) {
            return Result.retry();
        }
        
        // Show Synced Successfully using Handler for UI Thread
        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
            android.widget.Toast.makeText(getApplicationContext(), "Synced successfully", android.widget.Toast.LENGTH_SHORT).show();
        });
        
        return Result.success();
    }

    private boolean processUpload(DataQueueEntity entity) {
        try {
            // 0. Duplicate Prevention Check
            // Check if same barcode + similar timestamp (within 5 mins) exists in Firestore
            long threshold = 5 * 60 * 1000; // 5 minutes
            CountDownLatch dupLatch = new CountDownLatch(1);
            final boolean[] isDuplicate = {false};

            firestore.collection("products")
                    .whereEqualTo("barcode", entity.barcode)
                    .whereGreaterThan("timestamp", entity.timestamp - threshold)
                    .limit(1)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            isDuplicate[0] = true;
                        }
                        dupLatch.countDown();
                    })
                    .addOnFailureListener(e -> {
                        // If query fails, assume not a duplicate or handle error
                        dupLatch.countDown();
                    });

            dupLatch.await(10, TimeUnit.SECONDS);

            if (isDuplicate[0]) {
                Log.w(TAG, "duplicate skipped: " + entity.barcode + " (similar entry found)");
                // Return true to remove from local queue as it's considered "processed" (skipped)
                return true;
            }

            final boolean[] resultFlag = {false};
            final String[] uploadedUrl = {null};
            CountDownLatch latch = new CountDownLatch(1);

            // 1. Upload to Firebase Storage
            if (entity.localImagePath != null) {
                File localFile = new File(entity.localImagePath);
                if (localFile.exists()) {
                    StorageReference storageRef = storage.getReference()
                            .child("product_images")
                            .child(entity.label)
                            .child(new File(entity.localImagePath).getName()); // Preserve filename generated in PipelineManager

                    storageRef.putFile(Uri.fromFile(localFile))
                            .addOnSuccessListener(taskSnapshot -> {
                                storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                                    uploadedUrl[0] = uri.toString();
                                    latch.countDown();
                                }).addOnFailureListener(e -> latch.countDown());
                            })
                            .addOnFailureListener(e -> latch.countDown());
                            
                    // Wait for async storage task
                    latch.await(30, TimeUnit.SECONDS);
                } else {
                    Log.e(TAG, "Local file missing: " + entity.localImagePath);
                }
            }
            
            // If upload failed, we must not proceed to write partial DB doc
            // Unless localFile didn't exist at all, but the prompt says images exist for offline
            if (entity.localImagePath != null && uploadedUrl[0] == null) {
                 return false;
            }

            // 2. Upload to Firestore
            CountDownLatch dbLatch = new CountDownLatch(1);
            Map<String, Object> productMetadata = new HashMap<>();
            productMetadata.put("label", entity.label);
            productMetadata.put("barcode", entity.barcode);
            productMetadata.put("image_url", uploadedUrl[0] != null ? uploadedUrl[0] : "");
            productMetadata.put("timestamp", entity.timestamp);
            productMetadata.put("source", entity.source);
            productMetadata.put("device_id", entity.deviceId);
            productMetadata.put("upload_status", "success");

            firestore.collection("products").document(entity.uuid)
                    .set(productMetadata)
                    .addOnSuccessListener(aVoid -> {
                        resultFlag[0] = true;
                        dbLatch.countDown();
                    })
                    .addOnFailureListener(e -> dbLatch.countDown());
                    
            dbLatch.await(15, TimeUnit.SECONDS);
            
            return resultFlag[0];
            
        } catch (Exception e) {
            Log.e(TAG, "Critical failure during Firebase handshake", e);
            return false;
        }
    }
}
