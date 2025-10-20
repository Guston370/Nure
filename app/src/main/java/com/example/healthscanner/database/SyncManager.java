package com.example.healthscanner.database;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.healthscanner.AuthManager;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Comprehensive Sync Manager for Firebase Database
 * Handles all data synchronization between local storage and Firebase
 */
public class SyncManager {
    
    private static final String TAG = "SyncManager";
    private static final String PREFS_NAME = "HealthScannerPrefs";
    
    private static SyncManager instance;
    private final Context context;
    private final SharedPreferences prefs;
    private final FirebaseManager firebaseManager;
    private final AuthManager authManager;
    
    private SyncManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.firebaseManager = FirebaseManager.getInstance();
        this.authManager = AuthManager.getInstance(context);
    }
    
    public static synchronized SyncManager getInstance(Context context) {
        if (instance == null) {
            instance = new SyncManager(context);
        }
        return instance;
    }
    
    /**
     * Sync all user data to Firebase (complete backup)
     */
    public void syncAllDataToFirebase(SyncCallback callback) {
        String userId = authManager.getCurrentUserId();
        if (userId == null || userId.isEmpty()) {
            Log.e(TAG, "❌ SYNC FAILED: No user ID available");
            callback.onFailure("No user ID available");
            return;
        }
        
        Log.d(TAG, "🔄 STARTING SYNC TO FIREBASE for user: " + userId);
        Log.d(TAG, "🔍 Firebase Console: https://console.firebase.google.com/project/nure-70d49/firestore");
        
        try {
            // Collect all user data
            Map<String, Object> completeUserData = collectAllUserData();
            
            // Backup to Firebase
            firebaseManager.backupAllUserData(
                userId,
                prefs.getString("recent_scans", "[]"),
                prefs.getStringSet("health_concerns", new HashSet<>()),
                prefs.getStringSet("dietary_preferences", new HashSet<>()),
                completeUserData,
                new FirebaseManager.OperationCallback() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "✅ ALL USER DATA SYNCED TO FIREBASE SUCCESSFULLY!");
                        Log.d(TAG, "📍 Data location: users/" + userId);
                        Log.d(TAG, "🔍 Check: https://console.firebase.google.com/project/nure-70d49/firestore/data/users/" + userId);
                        updateLastSyncTimestamp();
                        callback.onSuccess();
                    }
                    
                    @Override
                    public void onFailure(String error) {
                        Log.e(TAG, "❌ FAILED TO SYNC DATA TO FIREBASE: " + error);
                        Log.e(TAG, "🔧 Check Firebase setup and internet connection");
                        callback.onFailure(error);
                    }
                }
            );
            
        } catch (Exception e) {
            Log.e(TAG, "Error collecting user data for sync: " + e.getMessage(), e);
            callback.onFailure("Error preparing data for sync: " + e.getMessage());
        }
    }
    
    /**
     * Restore all user data from Firebase
     */
    public void restoreAllDataFromFirebase(SyncCallback callback) {
        String userId = authManager.getCurrentUserId();
        if (userId == null || userId.isEmpty()) {
            callback.onFailure("No user ID available");
            return;
        }
        
        firebaseManager.restoreAllUserData(userId, new FirebaseManager.CompleteDataCallback() {
            @Override
            public void onSuccess(Map<String, Object> userData) {
                try {
                    // Restore all data to local storage
                    restoreDataToLocal(userData);
                    Log.d(TAG, "All user data restored from Firebase successfully");
                    callback.onSuccess();
                } catch (Exception e) {
                    Log.e(TAG, "Error restoring data to local storage: " + e.getMessage(), e);
                    callback.onFailure("Error restoring data: " + e.getMessage());
                }
            }
            
            @Override
            public void onFailure(String error) {
                Log.e(TAG, "Failed to restore data from Firebase: " + error);
                callback.onFailure(error);
            }
        });
    }
    
    /**
     * Sync only scan history to Firebase
     */
    public void syncScanHistoryToFirebase(SyncCallback callback) {
        String userId = authManager.getCurrentUserId();
        if (userId == null || userId.isEmpty()) {
            callback.onFailure("No user ID available");
            return;
        }
        
        String scanHistory = prefs.getString("recent_scans", "[]");
        
        firebaseManager.syncScanHistory(userId, scanHistory, new FirebaseManager.OperationCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Scan history synced to Firebase");
                callback.onSuccess();
            }
            
            @Override
            public void onFailure(String error) {
                Log.e(TAG, "Failed to sync scan history: " + error);
                callback.onFailure(error);
            }
        });
    }
    
    /**
     * Sync health preferences to Firebase
     */
    public void syncHealthPreferencesToFirebase(SyncCallback callback) {
        String userId = authManager.getCurrentUserId();
        if (userId == null || userId.isEmpty()) {
            callback.onFailure("No user ID available");
            return;
        }
        
        Set<String> healthConcerns = prefs.getStringSet("health_concerns", new HashSet<>());
        Set<String> dietaryPreferences = prefs.getStringSet("dietary_preferences", new HashSet<>());
        
        firebaseManager.syncHealthPreferences(userId, healthConcerns, dietaryPreferences, 
            new FirebaseManager.OperationCallback() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "Health preferences synced to Firebase");
                    callback.onSuccess();
                }
                
                @Override
                public void onFailure(String error) {
                    Log.e(TAG, "Failed to sync health preferences: " + error);
                    callback.onFailure(error);
                }
            });
    }
    
    /**
     * Collect all user data from SharedPreferences
     */
    private Map<String, Object> collectAllUserData() {
        Map<String, Object> userData = new HashMap<>();
        
        // User profile data
        userData.put("email", prefs.getString("current_user_email", ""));
        userData.put("displayName", prefs.getString("current_user_name", ""));
        userData.put("userId", prefs.getString("current_user_id", ""));
        userData.put("authProvider", prefs.getString("auth_provider", ""));
        userData.put("photoUrl", prefs.getString("current_user_photo", ""));
        
        // Scan history
        userData.put("scanHistory", prefs.getString("recent_scans", "[]"));
        
        // Health preferences
        userData.put("healthConcerns", new java.util.ArrayList<>(prefs.getStringSet("health_concerns", new HashSet<>())));
        userData.put("dietaryPreferences", new java.util.ArrayList<>(prefs.getStringSet("dietary_preferences", new HashSet<>())));
        
        // User statistics
        userData.put("totalScans", prefs.getInt("total_scans", 0));
        userData.put("healthyChoices", prefs.getInt("healthy_choices", 0));
        userData.put("averageHealthScore", prefs.getFloat("average_health_score", 0.0f));
        
        // App preferences
        userData.put("notificationsEnabled", prefs.getBoolean("notifications_enabled", true));
        userData.put("darkModeEnabled", prefs.getBoolean("dark_mode_enabled", false));
        
        // Timestamps
        userData.put("loginTimestamp", prefs.getLong("login_timestamp", 0));
        userData.put("joinDateTimestamp", prefs.getLong("join_date_timestamp", 0));
        userData.put("isFirstLaunchAfterSignin", prefs.getBoolean("is_first_launch_after_signin", true));
        
        return userData;
    }
    
    /**
     * Restore data to local SharedPreferences
     */
    private void restoreDataToLocal(Map<String, Object> userData) {
        SharedPreferences.Editor editor = prefs.edit();
        
        // User profile data
        if (userData.containsKey("email")) {
            editor.putString("current_user_email", (String) userData.get("email"));
        }
        if (userData.containsKey("displayName")) {
            editor.putString("current_user_name", (String) userData.get("displayName"));
        }
        if (userData.containsKey("photoUrl")) {
            editor.putString("current_user_photo", (String) userData.get("photoUrl"));
        }
        
        // Scan history
        if (userData.containsKey("scanHistory")) {
            editor.putString("recent_scans", (String) userData.get("scanHistory"));
        }
        
        // Health preferences
        if (userData.containsKey("healthConcerns")) {
            @SuppressWarnings("unchecked")
            java.util.List<String> concerns = (java.util.List<String>) userData.get("healthConcerns");
            if (concerns != null) {
                editor.putStringSet("health_concerns", new HashSet<>(concerns));
            }
        }
        
        if (userData.containsKey("dietaryPreferences")) {
            @SuppressWarnings("unchecked")
            java.util.List<String> preferences = (java.util.List<String>) userData.get("dietaryPreferences");
            if (preferences != null) {
                editor.putStringSet("dietary_preferences", new HashSet<>(preferences));
            }
        }
        
        // User statistics
        if (userData.containsKey("totalScans")) {
            editor.putInt("total_scans", ((Number) userData.get("totalScans")).intValue());
        }
        if (userData.containsKey("healthyChoices")) {
            editor.putInt("healthy_choices", ((Number) userData.get("healthyChoices")).intValue());
        }
        if (userData.containsKey("averageHealthScore")) {
            editor.putFloat("average_health_score", ((Number) userData.get("averageHealthScore")).floatValue());
        }
        
        // App preferences
        if (userData.containsKey("notificationsEnabled")) {
            editor.putBoolean("notifications_enabled", (Boolean) userData.get("notificationsEnabled"));
        }
        if (userData.containsKey("darkModeEnabled")) {
            editor.putBoolean("dark_mode_enabled", (Boolean) userData.get("darkModeEnabled"));
        }
        
        editor.apply();
        Log.d(TAG, "All user data restored to local storage");
    }
    
    /**
     * Update last sync timestamp
     */
    private void updateLastSyncTimestamp() {
        prefs.edit().putLong("last_sync_timestamp", System.currentTimeMillis()).apply();
    }
    
    /**
     * Get last sync timestamp
     */
    public long getLastSyncTimestamp() {
        return prefs.getLong("last_sync_timestamp", 0);
    }
    
    /**
     * Check if sync is needed (based on time or data changes)
     */
    public boolean isSyncNeeded() {
        long lastSync = getLastSyncTimestamp();
        long currentTime = System.currentTimeMillis();
        long oneMinute = 60 * 1000; // 1 minute - more aggressive sync
        
        return (currentTime - lastSync) > oneMinute;
    }
    
    /**
     * Auto-sync data if needed
     */
    public void autoSyncIfNeeded(SyncCallback callback) {
        if (isSyncNeeded()) {
            Log.d(TAG, "Auto-sync triggered");
            syncAllDataToFirebase(callback);
        } else {
            Log.d(TAG, "Auto-sync not needed");
            if (callback != null) callback.onSuccess();
        }
    }
    
    /**
     * Immediate sync when data changes (no time check)
     */
    public void syncImmediately(SyncCallback callback) {
        Log.d(TAG, "Immediate sync triggered due to data change");
        syncAllDataToFirebase(callback);
    }
    
    /**
     * Sync immediately when scan history changes
     */
    public void syncOnScanHistoryChange(SyncCallback callback) {
        String userId = authManager.getCurrentUserId();
        if (userId == null || userId.isEmpty()) {
            if (callback != null) callback.onFailure("No user ID available");
            return;
        }
        
        Log.d(TAG, "Syncing due to scan history change");
        
        // Sync scan history immediately
        syncScanHistoryToFirebase(new SyncCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Scan history synced successfully");
                // Also sync complete data to ensure everything is up to date
                syncAllDataToFirebase(callback);
            }
            
            @Override
            public void onFailure(String error) {
                Log.e(TAG, "Failed to sync scan history: " + error);
                if (callback != null) callback.onFailure(error);
            }
        });
    }
    
    /**
     * Sync immediately when health preferences change
     */
    public void syncOnHealthPreferencesChange(SyncCallback callback) {
        String userId = authManager.getCurrentUserId();
        if (userId == null || userId.isEmpty()) {
            if (callback != null) callback.onFailure("No user ID available");
            return;
        }
        
        Log.d(TAG, "Syncing due to health preferences change");
        
        // Sync health preferences immediately
        syncHealthPreferencesToFirebase(new SyncCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Health preferences synced successfully");
                // Also sync complete data to ensure everything is up to date
                syncAllDataToFirebase(callback);
            }
            
            @Override
            public void onFailure(String error) {
                Log.e(TAG, "Failed to sync health preferences: " + error);
                if (callback != null) callback.onFailure(error);
            }
        });
    }
    
    /**
     * Sync immediately when user profile changes
     */
    public void syncOnProfileChange(SyncCallback callback) {
        Log.d(TAG, "Syncing due to profile change");
        syncImmediately(callback);
    }
    
    /**
     * Sync immediately when app settings change
     */
    public void syncOnSettingsChange(SyncCallback callback) {
        Log.d(TAG, "Syncing due to settings change");
        syncImmediately(callback);
    }
    
    // Callback interface for sync operations
    public interface SyncCallback {
        void onSuccess();
        void onFailure(String error);
    }
}