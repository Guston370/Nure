package com.example.healthscanner;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.healthscanner.database.FirebaseManager;
import com.example.healthscanner.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Authentication Manager for handling user authentication state.
 * Manages Firebase Phone OTP authentication.
 */
public class AuthManager {
    
    private static final String TAG = "AuthManager";
    private static final String PREFS_NAME = "HealthScannerPrefs";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_LOGIN_TIMESTAMP = "login_timestamp";
    private static final String KEY_AUTH_PROVIDER = "auth_provider";
    
    private static AuthManager instance;
    private final Context context;
    private final SharedPreferences prefs;
    private final FirebaseAuth firebaseAuth;
    private final FirebaseManager firebaseManager;
    
    private AuthManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.firebaseAuth = FirebaseAuth.getInstance();
        this.firebaseManager = FirebaseManager.getInstance();
    }
    
    public static synchronized AuthManager getInstance(Context context) {
        if (instance == null) {
            instance = new AuthManager(context);
        }
        return instance;
    }
    
    /**
     * Check if user is authenticated with a valid session.
     */
    public boolean isUserAuthenticated() {
        boolean isLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false);
        if (!isLoggedIn) {
            return false;
        }
        
        if (!isSessionValid()) {
            Log.d(TAG, "Session expired, clearing login state");
            clearAuthState();
            return false;
        }
        
        // Check Firebase authentication
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            Log.d(TAG, "Firebase user is authenticated: " +
                    (currentUser.getPhoneNumber() != null ? currentUser.getPhoneNumber() : currentUser.getUid()));
            
            // Refresh the token to ensure it's still valid
            currentUser.getIdToken(true)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        updateLoginTimestamp();
                    } else {
                        Log.w(TAG, "Failed to refresh Firebase token: " + task.getException());
                    }
                });
            
            return true;
        } else {
            Log.d(TAG, "No Firebase user found, clearing session");
            clearAuthState();
            return false;
        }
    }
    
    private boolean isSessionValid() {
        long loginTime = prefs.getLong(KEY_LOGIN_TIMESTAMP, 0);
        if (loginTime == 0) return false;
        long thirtyDays = 30L * 24 * 60 * 60 * 1000;
        return (System.currentTimeMillis() - loginTime) < thirtyDays;
    }
    
    private void updateLoginTimestamp() {
        prefs.edit().putLong(KEY_LOGIN_TIMESTAMP, System.currentTimeMillis()).apply();
    }
    
    /**
     * Clear all authentication state from SharedPreferences.
     */
    public void clearAuthState() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_IS_LOGGED_IN, false);
        editor.remove(KEY_LOGIN_TIMESTAMP);
        editor.remove("current_user_email");
        editor.remove("current_user_name");
        editor.remove("current_user_id");
        editor.remove("current_user_phone");
        editor.remove(KEY_AUTH_PROVIDER);
        editor.remove("current_user_photo");
        editor.apply();
        
        Log.d(TAG, "Authentication state cleared");
    }
    
    /**
     * Navigate to login activity, clearing the back stack.
     */
    public void navigateToLogin(Activity activity) {
        Intent loginIntent = new Intent(activity, LoginActivity.class);
        loginIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(loginIntent);
        activity.finish();
    }
    
    public String getCurrentUserEmail() {
        return prefs.getString("current_user_email", null);
    }
    
    public String getCurrentUserName() {
        return prefs.getString("current_user_name", null);
    }
    
    public String getCurrentUserId() {
        return prefs.getString("current_user_id", null);
    }
    
    public String getAuthProvider() {
        return prefs.getString(KEY_AUTH_PROVIDER, "");
    }
    
    /**
     * Save user authentication state and sync with Firestore.
     */
    public void saveUserAuthState(FirebaseUser firebaseUser, AuthCallback callback) {
        if (firebaseUser == null) {
            callback.onFailure("Invalid Firebase user");
            return;
        }
        
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putLong(KEY_LOGIN_TIMESTAMP, System.currentTimeMillis());
        editor.putString("current_user_id", firebaseUser.getUid());
        editor.putString("current_user_phone", firebaseUser.getPhoneNumber());
        editor.putString("auth_provider", "phone");
        
        // Use phone number as display name fallback
        String displayName = firebaseUser.getDisplayName();
        if (displayName == null || displayName.isEmpty()) {
            displayName = firebaseUser.getPhoneNumber();
        }
        editor.putString("current_user_name", displayName);
        // For backward compat — some screens read "current_user_email"
        editor.putString("current_user_email", firebaseUser.getPhoneNumber());
        
        if (firebaseUser.getPhotoUrl() != null) {
            editor.putString("current_user_photo", firebaseUser.getPhotoUrl().toString());
        }
        
        editor.apply();
        
        // Sync with Firestore
        firebaseManager.checkUserExists(firebaseUser.getUid(), userExists -> {
            if (userExists) {
                firebaseManager.updateLastLogin(firebaseUser.getUid(), new FirebaseManager.OperationCallback() {
                    @Override public void onSuccess() {
                        Log.d(TAG, "Last login updated for existing user");
                        callback.onSuccess();
                    }
                    @Override public void onFailure(String error) {
                        Log.w(TAG, "Failed to update last login: " + error);
                        callback.onSuccess(); // Auth still succeeded
                    }
                });
            } else {
                firebaseManager.createUserProfile(firebaseUser, new FirebaseManager.OperationCallback() {
                    @Override public void onSuccess() {
                        Log.d(TAG, "New user profile created");
                        callback.onSuccess();
                    }
                    @Override public void onFailure(String error) {
                        Log.e(TAG, "Failed to create user profile: " + error);
                        callback.onFailure("Failed to create user profile: " + error);
                    }
                });
            }
        });
    }
    
    /**
     * Load user profile from Firestore and sync with local storage.
     */
    public void loadUserProfile(UserProfileCallback callback) {
        String userId = getCurrentUserId();
        if (userId == null || userId.isEmpty()) {
            callback.onFailure("No user ID found");
            return;
        }
        
        firebaseManager.getUserProfile(userId, new FirebaseManager.UserCallback() {
            @Override
            public void onSuccess(User user) {
                syncUserDataToLocal(user);
                callback.onSuccess(user);
            }
            @Override
            public void onFailure(String error) {
                Log.e(TAG, "Failed to load user profile: " + error);
                callback.onFailure(error);
            }
        });
    }
    
    /**
     * Update user profile in Firestore and local storage.
     */
    public void updateUserProfile(String displayName, String email, AuthCallback callback) {
        String userId = getCurrentUserId();
        if (userId == null || userId.isEmpty()) {
            callback.onFailure("No user ID found");
            return;
        }
        
        firebaseManager.updateUserProfile(userId, displayName, email, new FirebaseManager.OperationCallback() {
            @Override
            public void onSuccess() {
                SharedPreferences.Editor editor = prefs.edit();
                if (displayName != null && !displayName.trim().isEmpty()) {
                    editor.putString("current_user_name", displayName.trim());
                }
                if (email != null && !email.trim().isEmpty()) {
                    editor.putString("current_user_email", email.trim());
                }
                editor.apply();
                callback.onSuccess();
            }
            @Override
            public void onFailure(String error) {
                Log.e(TAG, "Failed to update user profile: " + error);
                callback.onFailure(error);
            }
        });
    }
    
    /**
     * Update user preferences in Firestore.
     */
    public void updateUserPreferences(boolean notificationsEnabled, boolean darkModeEnabled, AuthCallback callback) {
        String userId = getCurrentUserId();
        if (userId == null || userId.isEmpty()) {
            callback.onFailure("No user ID found");
            return;
        }
        
        java.util.Map<String, Object> preferences = new java.util.HashMap<>();
        preferences.put("notificationsEnabled", notificationsEnabled);
        preferences.put("darkModeEnabled", darkModeEnabled);
        
        firebaseManager.updateUserPreferences(userId, preferences, new FirebaseManager.OperationCallback() {
            @Override
            public void onSuccess() {
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("notifications_enabled", notificationsEnabled);
                editor.putBoolean("dark_mode_enabled", darkModeEnabled);
                editor.apply();
                callback.onSuccess();
            }
            @Override
            public void onFailure(String error) {
                Log.e(TAG, "Failed to update user preferences: " + error);
                callback.onFailure(error);
            }
        });
    }
    
    private void syncUserDataToLocal(User user) {
        SharedPreferences.Editor editor = prefs.edit();
        
        if (user.getDisplayName() != null) {
            editor.putString("current_user_name", user.getDisplayName());
        }
        if (user.getEmail() != null) {
            editor.putString("current_user_email", user.getEmail());
        }
        
        editor.putBoolean("notifications_enabled", user.isNotificationsEnabled());
        editor.putBoolean("dark_mode_enabled", user.isDarkModeEnabled());
        editor.putInt("total_scans", user.getTotalScans());
        editor.putInt("healthy_choices", user.getHealthyChoices());
        editor.putFloat("average_health_score", (float) user.getAverageHealthScore());
        
        if (user.getCreatedAt() != null) {
            editor.putLong("join_date_timestamp", user.getCreatedAt().getTime());
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy", java.util.Locale.getDefault());
            editor.putString("join_date", sdf.format(user.getCreatedAt()));
        }
        
        editor.apply();
    }
    
    public java.util.Map<String, Object> getUserPreferences() {
        java.util.Map<String, Object> preferences = new java.util.HashMap<>();
        preferences.put("notificationsEnabled", prefs.getBoolean("notifications_enabled", true));
        preferences.put("darkModeEnabled", prefs.getBoolean("dark_mode_enabled", false));
        preferences.put("totalScans", prefs.getInt("total_scans", 0));
        preferences.put("healthyChoices", prefs.getInt("healthy_choices", 0));
        preferences.put("averageHealthScore", prefs.getFloat("average_health_score", 0.0f));
        preferences.put("joinDate", prefs.getString("join_date", "2024"));
        return preferences;
    }
    
    /**
     * Sign out user and navigate to login.
     */
    public void signOut(Activity activity) {
        if (firebaseAuth != null) {
            firebaseAuth.signOut();
        }
        clearAuthState();
        navigateToLogin(activity);
        Log.d(TAG, "User signed out successfully");
    }
    
    // Callback interfaces
    public interface AuthCallback {
        void onSuccess();
        void onFailure(String error);
    }
    
    public interface UserProfileCallback {
        void onSuccess(User user);
        void onFailure(String error);
    }
}
