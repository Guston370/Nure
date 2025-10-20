package com.example.healthscanner.database;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.healthscanner.models.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Firebase Database Manager
 * Handles all Firestore database operations for user data
 */
public class FirebaseManager {
    private static final String TAG = "FirebaseManager";
    private static final String USERS_COLLECTION = "users";
    private static final String SCANS_COLLECTION = "scans";
    
    private static FirebaseManager instance;
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;
    
    // Callback interfaces
    public interface UserCallback {
        void onSuccess(User user);
        void onFailure(String error);
    }
    
    public interface OperationCallback {
        void onSuccess();
        void onFailure(String error);
    }
    
    private FirebaseManager() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }
    
    public static synchronized FirebaseManager getInstance() {
        if (instance == null) {
            instance = new FirebaseManager();
        }
        return instance;
    }
    
    /**
     * Create or update user profile in Firestore
     * @param user The user object to save
     * @param callback Callback for operation result
     */
    public void saveUserProfile(User user, OperationCallback callback) {
        if (user == null || user.getUid() == null) {
            callback.onFailure("Invalid user data");
            return;
        }
        
        DocumentReference userRef = db.collection(USERS_COLLECTION).document(user.getUid());
        
        userRef.set(user.toMap(), SetOptions.merge())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "User profile saved successfully: " + user.getEmail());
                        callback.onSuccess();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Error saving user profile", e);
                        callback.onFailure("Failed to save user profile: " + e.getMessage());
                    }
                });
    }
    
    /**
     * Get user profile from Firestore
     * @param uid User ID
     * @param callback Callback with user data or error
     */
    public void getUserProfile(String uid, UserCallback callback) {
        if (uid == null || uid.isEmpty()) {
            callback.onFailure("Invalid user ID");
            return;
        }
        
        DocumentReference userRef = db.collection(USERS_COLLECTION).document(uid);
        
        userRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        try {
                            User user = document.toObject(User.class);
                            if (user != null) {
                                Log.d(TAG, "User profile retrieved: " + user.getEmail());
                                callback.onSuccess(user);
                            } else {
                                callback.onFailure("Failed to parse user data");
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing user data", e);
                            callback.onFailure("Error parsing user data: " + e.getMessage());
                        }
                    } else {
                        Log.d(TAG, "User profile not found: " + uid);
                        callback.onFailure("User profile not found");
                    }
                } else {
                    Log.e(TAG, "Error getting user profile", task.getException());
                    callback.onFailure("Failed to retrieve user profile: " + 
                        (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
                }
            }
        });
    }
    
    /**
     * Create user profile for new user
     * @param firebaseUser Firebase user from authentication
     * @param callback Callback for operation result
     */
    public void createUserProfile(FirebaseUser firebaseUser, OperationCallback callback) {
        if (firebaseUser == null) {
            callback.onFailure("Invalid Firebase user");
            return;
        }
        
        // Determine auth provider
        String authProvider = "password"; // default
        if (firebaseUser.getProviderData() != null && !firebaseUser.getProviderData().isEmpty()) {
            authProvider = firebaseUser.getProviderData().get(0).getProviderId();
        }
        
        // Create user object
        User user = new User(
            firebaseUser.getUid(),
            firebaseUser.getEmail(),
            firebaseUser.getDisplayName(),
            authProvider
        );
        
        // Set additional properties
        user.setEmailVerified(firebaseUser.isEmailVerified());
        if (firebaseUser.getPhotoUrl() != null) {
            user.setPhotoUrl(firebaseUser.getPhotoUrl().toString());
        }
        
        // Save to Firestore
        saveUserProfile(user, callback);
    }
    
    /**
     * Update user's last login time
     * @param uid User ID
     * @param callback Callback for operation result
     */
    public void updateLastLogin(String uid, OperationCallback callback) {
        if (uid == null || uid.isEmpty()) {
            callback.onFailure("Invalid user ID");
            return;
        }
        
        DocumentReference userRef = db.collection(USERS_COLLECTION).document(uid);
        
        Map<String, Object> updates = new HashMap<>();
        updates.put("lastLoginAt", new Date());
        
        userRef.update(updates)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "Last login updated for user: " + uid);
                        callback.onSuccess();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Error updating last login", e);
                        callback.onFailure("Failed to update last login: " + e.getMessage());
                    }
                });
    }
    
    /**
     * Update user preferences
     * @param uid User ID
     * @param preferences Map of preferences to update
     * @param callback Callback for operation result
     */
    public void updateUserPreferences(String uid, Map<String, Object> preferences, OperationCallback callback) {
        if (uid == null || uid.isEmpty()) {
            callback.onFailure("Invalid user ID");
            return;
        }
        
        DocumentReference userRef = db.collection(USERS_COLLECTION).document(uid);
        
        userRef.update(preferences)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "User preferences updated for: " + uid);
                        callback.onSuccess();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Error updating user preferences", e);
                        callback.onFailure("Failed to update preferences: " + e.getMessage());
                    }
                });
    }
    
    /**
     * Update user's health statistics
     * @param uid User ID
     * @param totalScans Total number of scans
     * @param healthyChoices Number of healthy choices
     * @param averageScore Average health score
     * @param callback Callback for operation result
     */
    public void updateHealthStatistics(String uid, int totalScans, int healthyChoices, 
                                     double averageScore, OperationCallback callback) {
        if (uid == null || uid.isEmpty()) {
            callback.onFailure("Invalid user ID");
            return;
        }
        
        DocumentReference userRef = db.collection(USERS_COLLECTION).document(uid);
        
        Map<String, Object> updates = new HashMap<>();
        updates.put("totalScans", totalScans);
        updates.put("healthyChoices", healthyChoices);
        updates.put("averageHealthScore", averageScore);
        updates.put("lastScanDate", new Date());
        
        userRef.update(updates)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "Health statistics updated for user: " + uid);
                        callback.onSuccess();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Error updating health statistics", e);
                        callback.onFailure("Failed to update health statistics: " + e.getMessage());
                    }
                });
    }
    
    /**
     * Save a product scan to the user's scan history
     * @param uid User ID
     * @param scanData Scan data to save
     * @param callback Callback for operation result
     */
    public void saveScanHistory(String uid, Map<String, Object> scanData, OperationCallback callback) {
        if (uid == null || uid.isEmpty()) {
            callback.onFailure("Invalid user ID");
            return;
        }
        
        // Add timestamp and user ID to scan data
        scanData.put("userId", uid);
        scanData.put("timestamp", new Date());
        
        db.collection(USERS_COLLECTION)
                .document(uid)
                .collection(SCANS_COLLECTION)
                .add(scanData)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Log.d(TAG, "Scan history saved with ID: " + documentReference.getId());
                        callback.onSuccess();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Error saving scan history", e);
                        callback.onFailure("Failed to save scan history: " + e.getMessage());
                    }
                });
    }
    
    /**
     * Update user profile information
     * @param uid User ID
     * @param displayName New display name
     * @param email New email (optional)
     * @param callback Callback for operation result
     */
    public void updateUserProfile(String uid, String displayName, String email, OperationCallback callback) {
        if (uid == null || uid.isEmpty()) {
            callback.onFailure("Invalid user ID");
            return;
        }
        
        DocumentReference userRef = db.collection(USERS_COLLECTION).document(uid);
        
        Map<String, Object> updates = new HashMap<>();
        if (displayName != null && !displayName.trim().isEmpty()) {
            updates.put("displayName", displayName.trim());
        }
        if (email != null && !email.trim().isEmpty()) {
            updates.put("email", email.trim());
        }
        
        if (updates.isEmpty()) {
            callback.onFailure("No valid updates provided");
            return;
        }
        
        userRef.update(updates)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "User profile updated for: " + uid);
                        callback.onSuccess();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Error updating user profile", e);
                        callback.onFailure("Failed to update profile: " + e.getMessage());
                    }
                });
    }
    
    /**
     * Check if user profile exists in Firestore
     * @param uid User ID
     * @param callback Callback with boolean result
     */
    public void checkUserExists(String uid, UserExistsCallback callback) {
        if (uid == null || uid.isEmpty()) {
            callback.onResult(false);
            return;
        }
        
        DocumentReference userRef = db.collection(USERS_COLLECTION).document(uid);
        
        userRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    boolean exists = document.exists();
                    Log.d(TAG, "User exists check for " + uid + ": " + exists);
                    callback.onResult(exists);
                } else {
                    Log.e(TAG, "Error checking user existence", task.getException());
                    callback.onResult(false);
                }
            }
        });
    }
    
    /**
     * Sync complete user scan history to Firebase
     * @param uid User ID
     * @param scanHistoryJson JSON string of scan history
     * @param callback Callback for operation result
     */
    public void syncScanHistory(String uid, String scanHistoryJson, OperationCallback callback) {
        if (uid == null || uid.isEmpty()) {
            callback.onFailure("Invalid user ID");
            return;
        }
        
        DocumentReference userRef = db.collection(USERS_COLLECTION).document(uid);
        
        Map<String, Object> updates = new HashMap<>();
        updates.put("scanHistory", scanHistoryJson);
        updates.put("lastSyncTimestamp", new Date());
        
        userRef.update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Scan history synced for user: " + uid);
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error syncing scan history", e);
                    callback.onFailure("Failed to sync scan history: " + e.getMessage());
                });
    }
    
    /**
     * Sync health concerns and dietary preferences
     * @param uid User ID
     * @param healthConcerns Set of health concerns
     * @param dietaryPreferences Set of dietary preferences
     * @param callback Callback for operation result
     */
    public void syncHealthPreferences(String uid, java.util.Set<String> healthConcerns, 
                                    java.util.Set<String> dietaryPreferences, OperationCallback callback) {
        if (uid == null || uid.isEmpty()) {
            callback.onFailure("Invalid user ID");
            return;
        }
        
        DocumentReference userRef = db.collection(USERS_COLLECTION).document(uid);
        
        Map<String, Object> updates = new HashMap<>();
        updates.put("healthConcerns", new java.util.ArrayList<>(healthConcerns));
        updates.put("dietaryPreferences", new java.util.ArrayList<>(dietaryPreferences));
        updates.put("preferencesUpdatedAt", new Date());
        
        userRef.update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Health preferences synced for user: " + uid);
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error syncing health preferences", e);
                    callback.onFailure("Failed to sync health preferences: " + e.getMessage());
                });
    }
    
    /**
     * Sync complete user profile data including all app data
     * @param uid User ID
     * @param userData Complete user data map
     * @param callback Callback for operation result
     */
    public void syncCompleteUserData(String uid, Map<String, Object> userData, OperationCallback callback) {
        if (uid == null || uid.isEmpty()) {
            callback.onFailure("Invalid user ID");
            return;
        }
        
        DocumentReference userRef = db.collection(USERS_COLLECTION).document(uid);
        
        // Add sync metadata
        userData.put("lastFullSyncTimestamp", new Date());
        userData.put("appVersion", "1.0.0");
        
        userRef.set(userData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Complete user data synced for: " + uid);
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error syncing complete user data", e);
                    callback.onFailure("Failed to sync user data: " + e.getMessage());
                });
    }
    
    /**
     * Get complete user data from Firebase
     * @param uid User ID
     * @param callback Callback with complete user data
     */
    public void getCompleteUserData(String uid, CompleteDataCallback callback) {
        if (uid == null || uid.isEmpty()) {
            callback.onFailure("Invalid user ID");
            return;
        }
        
        DocumentReference userRef = db.collection(USERS_COLLECTION).document(uid);
        
        userRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    Map<String, Object> userData = document.getData();
                    Log.d(TAG, "Complete user data retrieved for: " + uid);
                    callback.onSuccess(userData);
                } else {
                    callback.onFailure("User data not found");
                }
            } else {
                Log.e(TAG, "Error getting complete user data", task.getException());
                callback.onFailure("Failed to retrieve user data: " + 
                    (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
            }
        });
    }
    
    /**
     * Backup all user data to Firebase (comprehensive backup)
     * @param uid User ID
     * @param scanHistory JSON string of scan history
     * @param healthConcerns Set of health concerns
     * @param dietaryPreferences Set of dietary preferences
     * @param userStats Map of user statistics
     * @param callback Callback for operation result
     */
    public void backupAllUserData(String uid, String scanHistory, java.util.Set<String> healthConcerns,
                                 java.util.Set<String> dietaryPreferences, Map<String, Object> userStats,
                                 OperationCallback callback) {
        if (uid == null || uid.isEmpty()) {
            callback.onFailure("Invalid user ID");
            return;
        }
        
        DocumentReference userRef = db.collection(USERS_COLLECTION).document(uid);
        
        Map<String, Object> completeBackup = new HashMap<>();
        
        // Add all user data
        completeBackup.put("scanHistory", scanHistory);
        completeBackup.put("healthConcerns", new java.util.ArrayList<>(healthConcerns));
        completeBackup.put("dietaryPreferences", new java.util.ArrayList<>(dietaryPreferences));
        completeBackup.putAll(userStats);
        
        // Add backup metadata
        completeBackup.put("backupTimestamp", new Date());
        completeBackup.put("backupVersion", "1.0");
        completeBackup.put("deviceInfo", android.os.Build.MODEL + " " + android.os.Build.VERSION.RELEASE);
        
        userRef.set(completeBackup, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Complete user data backup successful for: " + uid);
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error backing up user data", e);
                    callback.onFailure("Failed to backup user data: " + e.getMessage());
                });
    }
    
    /**
     * Restore all user data from Firebase
     * @param uid User ID
     * @param callback Callback with restored data
     */
    public void restoreAllUserData(String uid, CompleteDataCallback callback) {
        getCompleteUserData(uid, callback);
    }
    
    /**
     * Delete all user data from Firebase (for account deletion)
     * @param uid User ID
     * @param callback Callback for operation result
     */
    public void deleteAllUserData(String uid, OperationCallback callback) {
        if (uid == null || uid.isEmpty()) {
            callback.onFailure("Invalid user ID");
            return;
        }
        
        DocumentReference userRef = db.collection(USERS_COLLECTION).document(uid);
        
        userRef.delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "All user data deleted for: " + uid);
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting user data", e);
                    callback.onFailure("Failed to delete user data: " + e.getMessage());
                });
    }
    
    /**
     * Get Firebase database reference for direct access
     * @return FirebaseFirestore instance
     */
    public FirebaseFirestore getDatabase() {
        return db;
    }
    
    /**
     * Get users collection reference
     * @return Users collection reference
     */
    public com.google.firebase.firestore.CollectionReference getUsersCollection() {
        return db.collection(USERS_COLLECTION);
    }
    
    /**
     * Get specific user document reference
     * @param uid User ID
     * @return User document reference
     */
    public DocumentReference getUserDocument(String uid) {
        return db.collection(USERS_COLLECTION).document(uid);
    }
    
    // Callback interface for complete data operations
    public interface CompleteDataCallback {
        void onSuccess(Map<String, Object> userData);
        void onFailure(String error);
    }
    
    // Callback interface for user existence check
    public interface UserExistsCallback {
        void onResult(boolean exists);
    }
}