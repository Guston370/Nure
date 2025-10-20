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
    private FirebaseManager firebaseManager;
    
    private AuthManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.firebaseAuth = FirebaseAuth.getInstance();
        this.firebaseManager = FirebaseManager.getInstance();
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
        
        // Clear Google-specific data
        editor.remove("current_user_first_name");
        editor.remove("current_user_last_name");
        editor.remove("google_account_type");
        editor.remove("join_date_timestamp");
        editor.remove("account_source");
        editor.remove("fresh_google_signin");
        
        editor.apply();
        
        Log.d(TAG, "Authentication state and Google account data cleared");
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
     * Navigate to login activity with preserved navigation stack
     * @param activity Current activity
     */
    public void navigateToLoginPreserveStack(Activity activity) {
        Intent loginIntent = new Intent(activity, LoginActivity.class);
        loginIntent.putExtra("preserve_navigation_stack", true);
        activity.startActivity(loginIntent);
        
        Log.d(TAG, "Navigated to login screen (preserving stack)");
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
     * Save user authentication state and sync with database
     * @param firebaseUser Firebase user object
     * @param callback Callback for operation result
     */
    public void saveUserAuthState(FirebaseUser firebaseUser, AuthCallback callback) {
        if (firebaseUser == null) {
            callback.onFailure("Invalid Firebase user");
            return;
        }
        
        // Save to SharedPreferences first
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putLong(KEY_LOGIN_TIMESTAMP, System.currentTimeMillis());
        editor.putString("current_user_email", firebaseUser.getEmail());
        editor.putString("current_user_name", firebaseUser.getDisplayName());
        editor.putString("current_user_id", firebaseUser.getUid());
        
        // Determine auth provider
        String authProvider = "password";
        if (firebaseUser.getProviderData() != null && !firebaseUser.getProviderData().isEmpty()) {
            authProvider = firebaseUser.getProviderData().get(0).getProviderId();
        }
        editor.putString(KEY_AUTH_PROVIDER, authProvider);
        
        if (firebaseUser.getPhotoUrl() != null) {
            editor.putString("current_user_photo", firebaseUser.getPhotoUrl().toString());
        }
        
        editor.apply();
        
        // Check if user profile exists in Firestore
        firebaseManager.checkUserExists(firebaseUser.getUid(), userExists -> {
            if (userExists) {
                    // Update last login time
                    firebaseManager.updateLastLogin(firebaseUser.getUid(), new FirebaseManager.OperationCallback() {
                        @Override
                        public void onSuccess() {
                            Log.d(TAG, "Last login updated for existing user");
                            callback.onSuccess();
                        }
                        
                        @Override
                        public void onFailure(String error) {
                            Log.w(TAG, "Failed to update last login: " + error);
                            // Still consider auth successful even if last login update fails
                            callback.onSuccess();
                        }
                    });
                } else {
                    // Create new user profile
                    firebaseManager.createUserProfile(firebaseUser, new FirebaseManager.OperationCallback() {
                        @Override
                        public void onSuccess() {
                            Log.d(TAG, "New user profile created successfully");
                            callback.onSuccess();
                        }
                        
                        @Override
                        public void onFailure(String error) {
                            Log.e(TAG, "Failed to create user profile: " + error);
                            callback.onFailure("Failed to create user profile: " + error);
                        }
                    });
                }
            });
    }
    
    /**
     * Load user profile from database and sync with local storage
     * @param callback Callback with user data
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
                // Update local storage with latest data
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
     * Update user profile in database and local storage
     * @param displayName New display name
     * @param email New email
     * @param callback Callback for operation result
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
                // Update local storage
                SharedPreferences.Editor editor = prefs.edit();
                if (displayName != null && !displayName.trim().isEmpty()) {
                    editor.putString("current_user_name", displayName.trim());
                }
                if (email != null && !email.trim().isEmpty()) {
                    editor.putString("current_user_email", email.trim());
                }
                editor.apply();
                
                Log.d(TAG, "User profile updated successfully");
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
     * Update user preferences in database
     * @param notificationsEnabled Notifications preference
     * @param darkModeEnabled Dark mode preference
     * @param callback Callback for operation result
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
                // Update local storage
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("notifications_enabled", notificationsEnabled);
                editor.putBoolean("dark_mode_enabled", darkModeEnabled);
                editor.apply();
                
                Log.d(TAG, "User preferences updated successfully");
                callback.onSuccess();
            }
            
            @Override
            public void onFailure(String error) {
                Log.e(TAG, "Failed to update user preferences: " + error);
                callback.onFailure(error);
            }
        });
    }
    
    /**
     * Sync user data from database to local storage
     * @param user User object from database
     */
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
            // Format join date for display
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy", java.util.Locale.getDefault());
            editor.putString("join_date", sdf.format(user.getCreatedAt()));
        }
        
        editor.apply();
        Log.d(TAG, "User data synced to local storage");
    }
    
    /**
     * Get user preferences from local storage
     * @return Map of user preferences
     */
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
     * Enable test mode authentication for development
     */
    public void enableTestMode() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putLong(KEY_LOGIN_TIMESTAMP, System.currentTimeMillis());
        editor.putString(KEY_AUTH_PROVIDER, "test_mode");
        editor.putString("current_user_email", "test@healthscanner.com");
        editor.putString("current_user_name", "Test User");
        editor.putString("current_user_id", "test_user_123");
        editor.apply();
        
        Log.d(TAG, "Test mode authentication enabled");
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
        
        // Sign out from Google to ensure account selection on next sign-in
        try {
            com.google.android.gms.auth.api.signin.GoogleSignInOptions gso = 
                new com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(
                    com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(activity.getString(R.string.default_web_client_id))
                .requestEmail()
                .requestProfile()
                .build();
                
            com.google.android.gms.auth.api.signin.GoogleSignInClient googleSignInClient = 
                com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(activity, gso);
                
            googleSignInClient.signOut().addOnCompleteListener(task -> {
                Log.d(TAG, "Google Sign-In client signed out - will prompt for account selection on next sign-in");
            });
        } catch (Exception e) {
            Log.e(TAG, "Error signing out from Google: " + e.getMessage());
        }
        
        // Clear authentication state
        clearAuthState();
        
        // Navigate to login
        navigateToLogin(activity);
        
        Log.d(TAG, "User signed out successfully from all services");
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