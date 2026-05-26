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
            // Product Mapping check
            CountDownLatch prodLatch = new CountDownLatch(1);
            final String[] productId = {null};
            
            firestore.collection("products")
                    .whereEqualTo("barcode", entity.barcode)
                    .limit(1)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            productId[0] = queryDocumentSnapshots.getDocuments().get(0).getId();
                        }
                        prodLatch.countDown();
                    })
                    .addOnFailureListener(e -> prodLatch.countDown());
                    
            prodLatch.await(10, TimeUnit.SECONDS);

            if (productId[0] == null) {
                // Create new product record
                CountDownLatch createProdLatch = new CountDownLatch(1);
                com.google.firebase.firestore.DocumentReference docRef = firestore.collection("products").document();
                productId[0] = docRef.getId();
                Map<String, Object> newProd = new HashMap<>();
                newProd.put("barcode", entity.barcode);
                newProd.put("product_name", entity.label);
                
                docRef.set(newProd).addOnCompleteListener(task -> createProdLatch.countDown());
                createProdLatch.await(5, TimeUnit.SECONDS);
            }
            
            final boolean[] resultFlag = {false};
            final String[] uploadedUrl = {null};
            CountDownLatch latch = new CountDownLatch(1);

            boolean isTrainingData = "barcode_fallback".equals(entity.source);

            // 1. Upload to Firebase Storage
            if (entity.localImagePath != null) {
                File localFile = new File(entity.localImagePath);
                if (localFile.exists()) {
                    StorageReference storageRef;
                    if (isTrainingData) {
                        storageRef = storage.getReference()
                            .child("training_data")
                            .child(productId[0])
                            .child(localFile.getName());
                    } else {
                        storageRef = storage.getReference()
                            .child("product_images")
                            .child(entity.label)
                            .child(localFile.getName());
                    }

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
            if (entity.localImagePath != null && uploadedUrl[0] == null) {
                 return false;
            }

            // 2. Upload to Firestore
            CountDownLatch dbLatch = new CountDownLatch(1);
            Map<String, Object> metadata = new HashMap<>();
            
            if (isTrainingData) {
                metadata.put("product_id", productId[0]);
                metadata.put("product_name", entity.label);
                metadata.put("barcode", entity.barcode);
                metadata.put("image_url", uploadedUrl[0] != null ? uploadedUrl[0] : "");
                metadata.put("source", "barcode_fallback");
                metadata.put("yolo_confidence", entity.yoloConfidence);
                metadata.put("timestamp", entity.timestamp);
                metadata.put("verified", true);
                
                firestore.collection("training_data").document(entity.uuid)
                        .set(metadata)
                        .addOnSuccessListener(aVoid -> {
                            resultFlag[0] = true;
                            dbLatch.countDown();
                        })
                        .addOnFailureListener(e -> dbLatch.countDown());
            } else {
                metadata.put("label", entity.label);
                metadata.put("barcode", entity.barcode);
                metadata.put("image_url", uploadedUrl[0] != null ? uploadedUrl[0] : "");
                metadata.put("timestamp", entity.timestamp);
                metadata.put("source", entity.source);
                metadata.put("device_id", entity.deviceId);
                metadata.put("upload_status", "success");
                
                firestore.collection("products").document(entity.uuid)
                        .set(metadata)
                        .addOnSuccessListener(aVoid -> {
                            resultFlag[0] = true;
                            dbLatch.countDown();
                        })
                        .addOnFailureListener(e -> dbLatch.countDown());
            }
                    
            dbLatch.await(15, TimeUnit.SECONDS);
            
            return resultFlag[0];
            
        } catch (Exception e) {
            Log.e(TAG, "Critical failure during Firebase handshake", e);
            return false;
        }
    }
}
