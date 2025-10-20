package com.example.healthscanner;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.healthscanner.database.FirebaseManager;

/**
 * Data Reset Manager for Health Scanner App
 * Handles complete reset of all local and cloud data
 */
public class DataResetManager {
    
    private static final String TAG = "DataResetManager";
    private static final String PREFS_NAME = "HealthScannerPrefs";
    
    private static DataResetManager instance;
    private final Context context;
    private final SharedPreferences preferences;
    private final FirebaseManager firebaseManager;
    
    private DataResetManager(Context context) {
        this.context = context.getApplicationContext();
        this.preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.firebaseManager = FirebaseManager.getInstance();
    }
    
    public static synchronized DataResetManager getInstance(Context context) {
        if (instance == null) {
            instance = new DataResetManager(context.getApplicationContext());
        }
        return instance;
    }
    
    /**
     * Complete app reset - clears all local and cloud data
     */
    public void performCompleteReset(ResetCallback callback) {
        Log.d(TAG, "🔄 Starting complete app reset...");
        
        try {
            // Step 1: Clear all local data
            clearAllLocalData();
            
            // Step 2: Clear Firebase data (if user is authenticated)
            clearFirebaseData(new FirebaseManager.OperationCallback() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "✅ Complete reset successful - all data cleared");
                    if (callback != null) callback.onSuccess();
                }
                
                @Override
                public void onFailure(String error) {
                    Log.w(TAG, "⚠️ Firebase reset failed but local data cleared: " + error);
                    if (callback != null) callback.onSuccess(); // Still consider success if local cleared
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Error during complete reset: " + e.getMessage(), e);
            if (callback != null) callback.onFailure(e.getMessage());
        }
    }
    
    /**
     * Clear all local SharedPreferences data
     */
    public void clearAllLocalData() {
        try {
            Log.d(TAG, "🧹 Clearing all local data...");
            
            SharedPreferences.Editor editor = preferences.edit();
            
            // Clear authentication data
            editor.remove("is_logged_in");
            editor.remove("login_timestamp");
            editor.remove("current_user_email");
            editor.remove("current_user_name");
            editor.remove("current_user_id");
            editor.remove("auth_provider");
            editor.remove("current_user_photo");
            editor.remove("current_user_first_name");
            editor.remove("current_user_last_name");
            
            // Clear Google account data
            editor.remove("google_account_type");
            editor.remove("join_date_timestamp");
            editor.remove("account_source");
            editor.remove("fresh_google_signin");
            editor.remove("google_id_token");
            editor.remove("is_google_account");
            editor.remove("firebase_profile_created");
            editor.remove("firebase_profile_timestamp");
            
            // Clear app data
            editor.remove("recent_scans");
            editor.remove("scan_history");
            editor.remove("health_concerns");
            editor.remove("dietary_preferences");
            editor.remove("user_saved_items");
            
            // Clear statistics
            editor.remove("total_scans");
            editor.remove("healthy_choices");
            editor.remove("average_health_score");
            
            // Clear settings
            editor.remove("notifications_enabled");
            editor.remove("dark_mode_enabled");
            editor.remove("follow_system_theme");
            
            // Clear sync data
            editor.remove("last_sync_timestamp");
            editor.remove("is_first_launch_after_signin");
            
            // Clear any other app-specific data
            editor.remove("join_date");
            editor.remove("login_method");
            editor.remove("account_creation_time");
            editor.remove("is_new_firebase_user");
            editor.remove("is_returning_firebase_user");
            
            // Apply all changes
            boolean success = editor.commit();
            
            if (success) {
                Log.d(TAG, "✅ All local data cleared successfully");
            } else {
                Log.e(TAG, "❌ Failed to clear local data");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error clearing local data: " + e.getMessage(), e);
        }
    }
    
    /**
     * Clear Firebase cloud data for current user
     */
    private void clearFirebaseData(FirebaseManager.OperationCallback callback) {
        try {
            // Get current user ID before clearing local data
            String userId = preferences.getString("current_user_id", "");
            
            if (userId.isEmpty()) {
                Log.d(TAG, "No user ID found, skipping Firebase data clear");
                callback.onSuccess();
                return;
            }
            
            Log.d(TAG, "🔥 Clearing Firebase data for user: " + userId);
            
            // Delete all user data from Firebase
            firebaseManager.deleteAllUserData(userId, new FirebaseManager.OperationCallback() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "✅ Firebase data cleared successfully");
                    callback.onSuccess();
                }
                
                @Override
                public void onFailure(String error) {
                    Log.e(TAG, "❌ Failed to clear Firebase data: " + error);
                    callback.onFailure(error);
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "Error clearing Firebase data: " + e.getMessage(), e);
            callback.onFailure(e.getMessage());
        }
    }
    
    /**
     * Clear only scan history and statistics (keep user profile)
     */
    public void clearScanDataOnly(ResetCallback callback) {
        try {
            Log.d(TAG, "🧹 Clearing scan data only...");
            
            SharedPreferences.Editor editor = preferences.edit();
            
            // Clear scan-related data only
            editor.remove("recent_scans");
            editor.remove("scan_history");
            editor.remove("total_scans");
            editor.remove("healthy_choices");
            editor.remove("average_health_score");
            editor.remove("user_saved_items");
            
            // Reset to zero values
            editor.putString("recent_scans", "[]");
            editor.putInt("total_scans", 0);
            editor.putInt("healthy_choices", 0);
            editor.putFloat("average_health_score", 0.0f);
            editor.putString("user_saved_items", "[]");
            
            boolean success = editor.commit();
            
            if (success) {
                Log.d(TAG, "✅ Scan data cleared successfully");
                if (callback != null) callback.onSuccess();
            } else {
                Log.e(TAG, "❌ Failed to clear scan data");
                if (callback != null) callback.onFailure("Failed to clear scan data");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error clearing scan data: " + e.getMessage(), e);
            if (callback != null) callback.onFailure(e.getMessage());
        }
    }
    
    /**
     * Clear only health preferences (keep profile and scan data)
     */
    public void clearHealthPreferencesOnly(ResetCallback callback) {
        try {
            Log.d(TAG, "🧹 Clearing health preferences only...");
            
            SharedPreferences.Editor editor = preferences.edit();
            
            // Clear health-related preferences only
            editor.remove("health_concerns");
            editor.remove("dietary_preferences");
            
            // Reset to empty arrays
            editor.putStringSet("health_concerns", new java.util.HashSet<>());
            editor.putStringSet("dietary_preferences", new java.util.HashSet<>());
            
            boolean success = editor.commit();
            
            if (success) {
                Log.d(TAG, "✅ Health preferences cleared successfully");
                if (callback != null) callback.onSuccess();
            } else {
                Log.e(TAG, "❌ Failed to clear health preferences");
                if (callback != null) callback.onFailure("Failed to clear health preferences");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error clearing health preferences: " + e.getMessage(), e);
            if (callback != null) callback.onFailure(e.getMessage());
        }
    }
    
    /**
     * Get reset statistics
     */
    public ResetStats getResetStats() {
        try {
            int totalScans = preferences.getInt("total_scans", 0);
            int savedItems = 0;
            int healthConcerns = preferences.getStringSet("health_concerns", new java.util.HashSet<>()).size();
            int dietaryPrefs = preferences.getStringSet("dietary_preferences", new java.util.HashSet<>()).size();
            
            try {
                org.json.JSONArray savedArray = new org.json.JSONArray(preferences.getString("user_saved_items", "[]"));
                savedItems = savedArray.length();
            } catch (Exception e) {
                savedItems = 0;
            }
            
            return new ResetStats(totalScans, savedItems, healthConcerns, dietaryPrefs);
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting reset stats: " + e.getMessage(), e);
            return new ResetStats(0, 0, 0, 0);
        }
    }
    
    // Callback interfaces
    public interface ResetCallback {
        void onSuccess();
        void onFailure(String error);
    }
    
    // Reset statistics class
    public static class ResetStats {
        public final int totalScans;
        public final int savedItems;
        public final int healthConcerns;
        public final int dietaryPreferences;
        
        public ResetStats(int totalScans, int savedItems, int healthConcerns, int dietaryPreferences) {
            this.totalScans = totalScans;
            this.savedItems = savedItems;
            this.healthConcerns = healthConcerns;
            this.dietaryPreferences = dietaryPreferences;
        }
    }
}