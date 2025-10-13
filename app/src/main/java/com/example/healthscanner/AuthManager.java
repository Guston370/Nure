package com.example.healthscanner;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Authentication Manager for handling user authentication state
 * Manages both Firebase and test mode authentication
 */
public class AuthManager {
    
    private static final String TAG = "AuthManager";
    private static final String PREFS_NAME = "HealthScannerPrefs";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_LOGIN_TIMESTAMP = "login_timestamp";
    private static final String KEY_AUTH_PROVIDER = "auth_provider";
    
    private static AuthManager instance;
    private Context context;
    private SharedPreferences prefs;
    private FirebaseAuth firebaseAuth;
    
    private AuthManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.firebaseAuth = FirebaseAuth.getInstance();
    }
    
    /**
     * Get singleton instance of AuthManager
     * @param context Application context
     * @return AuthManager instance
     */
    public static synchronized AuthManager getInstance(Context context) {
        if (instance == null) {
            instance = new AuthManager(context);
        }
        return instance;
    }
    
    /**
     * Check if user is authenticated (Firebase or test mode)
     * @return true if user is authenticated with valid session
     */
    public boolean isUserAuthenticated() {
        // Check SharedPreferences login state
        boolean isLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false);
        if (!isLoggedIn) {
            Log.d(TAG, "User not logged in according to SharedPreferences");
            return false;
        }
        
        // Check session validity (30 days)
        if (!isSessionValid()) {
            Log.d(TAG, "Session expired, clearing login state");
            clearAuthState();
            return false;
        }
        
        // Check authentication provider
        String authProvider = prefs.getString(KEY_AUTH_PROVIDER, "");
        
        if ("test_mode".equals(authProvider)) {
            Log.d(TAG, "Test mode authentication is valid");
            return true;
        }
        
        // Check Firebase authentication for real users
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            Log.d(TAG, "Firebase user is authenticated: " + currentUser.getEmail());
            return true;
        } else {
            Log.d(TAG, "No Firebase user found, clearing session");
            clearAuthState();
            return false;
        }
    }
    
    /**
     * Check if the current session is still valid (within 30 days)
     * @return true if session is valid, false otherwise
     */
    private boolean isSessionValid() {
        long loginTime = prefs.getLong(KEY_LOGIN_TIMESTAMP, 0);
        if (loginTime == 0) {
            return false; // No login timestamp found
        }
        
        long currentTime = System.currentTimeMillis();
        long thirtyDaysInMillis = 30L * 24 * 60 * 60 * 1000; // 30 days
        
        return (currentTime - loginTime) < thirtyDaysInMillis;
    }
    
    /**
     * Clear authentication state from SharedPreferences
     */
    public void clearAuthState() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_IS_LOGGED_IN, false);
        editor.remove(KEY_LOGIN_TIMESTAMP);
        editor.remove("current_user_email");
        editor.remove("current_user_name");
        editor.remove("current_user_id");
        editor.remove(KEY_AUTH_PROVIDER);
        editor.remove("current_user_photo");
        editor.apply();
        
        Log.d(TAG, "Authentication state cleared");
    }
    
    /**
     * Navigate to login activity
     * @param activity Current activity
     */
    public void navigateToLogin(Activity activity) {
        Intent loginIntent = new Intent(activity, LoginActivity.class);
        loginIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(loginIntent);
        activity.finish();
        
        Log.d(TAG, "Navigated to login screen");
    }
    
    /**
     * Get current user email from SharedPreferences
     * @return User email or null if not found
     */
    public String getCurrentUserEmail() {
        return prefs.getString("current_user_email", null);
    }
    
    /**
     * Get current user name from SharedPreferences
     * @return User name or null if not found
     */
    public String getCurrentUserName() {
        return prefs.getString("current_user_name", null);
    }
    
    /**
     * Get current user ID from SharedPreferences
     * @return User ID or null if not found
     */
    public String getCurrentUserId() {
        return prefs.getString("current_user_id", null);
    }
    
    /**
     * Get authentication provider (firebase, google.com, test_mode, etc.)
     * @return Authentication provider or empty string if not found
     */
    public String getAuthProvider() {
        return prefs.getString(KEY_AUTH_PROVIDER, "");
    }
    
    /**
     * Check if current user is in test mode
     * @return true if user is in test mode, false otherwise
     */
    public boolean isTestMode() {
        return "test_mode".equals(getAuthProvider());
    }
    
    /**
     * Sign out user from all providers
     * @param activity Current activity
     */
    public void signOut(Activity activity) {
        // Sign out from Firebase
        if (firebaseAuth != null) {
            firebaseAuth.signOut();
        }
        
        // Clear authentication state
        clearAuthState();
        
        // Navigate to login
        navigateToLogin(activity);
        
        Log.d(TAG, "User signed out successfully");
    }
}