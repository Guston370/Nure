package com.example.healthscanner;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.example.healthscanner.database.FirebaseManager;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private static final int RC_SIGN_IN = 9001; // Request code for Google Sign-In

    // UI Elements
    private TextInputLayout tilEmail, tilPassword;
    private TextInputEditText etEmail, etPassword;
    private MaterialCheckBox cbRememberMe;
    private MaterialButton btnLogin, btnGoogleSignIn, btnSignup, btnForgotPassword;
    private CircularProgressIndicator progressLogin;
    private View logoContainer, loginTitle, loginSubtitle, loginCard;

    // Firebase
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private FirebaseManager firebaseManager;

    // SharedPreferences
    private SharedPreferences healthScannerPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_modern);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        
        // Initialize Firebase Manager
        firebaseManager = FirebaseManager.getInstance();
        
        // Initialize SharedPreferences
        healthScannerPrefs = getSharedPreferences("HealthScannerPrefs", MODE_PRIVATE);

        // Check if user is already signed in and session is valid
        if (isUserAlreadyLoggedIn()) {
            Log.d(TAG, "User already logged in, navigating to main screen");
            navigateToMain();
            return;
        }
        
        // Production ready - no test accounts

        // Initialize Google Sign-In
        initializeGoogleSignIn();
        
        // Verify setup (for debugging)
        verifyGoogleSignInSetup();
        testGoogleSignInSetup();
        
        // Initialize views and listeners
        initializeViews();
        setupClickListeners();
        
        // Load saved email if remember me was checked
        loadSavedCredentials();
        
        // Check for signup success message
        handleSignupSuccess();

        // Run premium entrance reveal animations
        startEntranceAnimations();
    }

    /**
     * Start entrance animations for premium reveal effect
     */
    private void startEntranceAnimations() {
        if (logoContainer != null) {
            logoContainer.setAlpha(0f);
            logoContainer.setScaleX(0.5f);
            logoContainer.setScaleY(0.5f);
            logoContainer.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(400)
                    .setInterpolator(new android.view.animation.OvershootInterpolator(1.2f))
                    .start();
        }

        if (loginTitle != null) {
            loginTitle.setAlpha(0f);
            loginTitle.setTranslationY(30f);
            loginTitle.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(400)
                    .setStartDelay(100)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator())
                    .start();
        }

        if (loginSubtitle != null) {
            loginSubtitle.setAlpha(0f);
            loginSubtitle.setTranslationY(30f);
            loginSubtitle.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(400)
                    .setStartDelay(150)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator())
                    .start();
        }

        if (loginCard != null) {
            loginCard.setAlpha(0f);
            loginCard.setTranslationY(50f);
            loginCard.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(500)
                    .setStartDelay(200)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator())
                    .start();
        }
    }

    /**
     * Initialize Google Sign-In configuration
     * Sets up GoogleSignInOptions with web client ID from google-services.json
     */
    private void initializeGoogleSignIn() {
        try {
            Log.d(TAG, "Initializing Google Sign-In...");
            
            // Configure Google Sign-In to always prompt for account selection
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.default_web_client_id)) // Web client ID from Firebase
                    .requestEmail() // Request user's email address
                    .requestProfile() // Request user's basic profile info
                    .build();

            // Create GoogleSignInClient with the configured options
            mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
            
            // Always sign out from Google to force account selection
            mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
                Log.d(TAG, "Google Sign-In client signed out - will prompt for account selection");
            });
            
            Log.d(TAG, "Google Sign-In configured to always prompt for account selection");
            
            Log.d(TAG, "Google Sign-In initialized successfully");
            Log.d(TAG, "Web Client ID: " + getString(R.string.default_web_client_id));
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize Google Sign-In", e);
            showError("Google Sign-In configuration error. Please check your setup.");
        }
    }

    private void initializeViews() {
        // TextInputLayouts
        tilEmail = findViewById(R.id.til_email);
        tilPassword = findViewById(R.id.til_password);

        // EditTexts
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);

        // Buttons and other UI elements
        cbRememberMe = findViewById(R.id.cb_remember_me);
        btnLogin = findViewById(R.id.btn_login);
        btnGoogleSignIn = findViewById(R.id.btn_google_signin);
        btnForgotPassword = findViewById(R.id.btn_forgot_password);
        btnSignup = findViewById(R.id.btn_signup);
        progressLogin = findViewById(R.id.progress_login);

        // Animated layout container and titles
        logoContainer = findViewById(R.id.logoContainer);
        loginTitle = findViewById(R.id.loginTitle);
        loginSubtitle = findViewById(R.id.loginSubtitle);
        loginCard = findViewById(R.id.loginCard);
    }

    private void setupClickListeners() {
        // Login button click
        btnLogin.setOnClickListener(v -> handleEmailPasswordLogin());

        // Sign Up button click
        btnSignup.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
            startActivity(intent);
        });

        // Forgot Password click
        btnForgotPassword.setOnClickListener(v -> handleForgotPassword());

        // Google Sign In click
        btnGoogleSignIn.setOnClickListener(v -> handleGoogleSignIn());
    }

    private void handleEmailPasswordLogin() {
        // Clear previous errors
        tilEmail.setError(null);
        tilPassword.setError(null);

        // Get user input
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Validate input
        if (!validateLoginInput(email, password)) {
            return;
        }

        // Show loading state
        setLoginLoading(true);

        // Use proper Firebase authentication with security checks
        authenticateWithFirebase(email, password);
    }

    private boolean validateLoginInput(String email, String password) {
        boolean isValid = true;

        // Validate Email
        if (TextUtils.isEmpty(email)) {
            tilEmail.setError("Email is required");
            isValid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Please enter a valid email address");
            isValid = false;
        }

        // Validate Password
        if (TextUtils.isEmpty(password)) {
            tilPassword.setError("Password is required");
            isValid = false;
        } else if (password.length() < 6) {
            tilPassword.setError("Password must be at least 6 characters long");
            isValid = false;
        }

        return isValid;
    }

    /**
     * Handle Google Sign-In button click
     * Shows loading state and launches Google Sign-In intent
     */
    private void handleGoogleSignIn() {
        Log.d(TAG, "Google Sign-In button clicked");
        
        // Check if GoogleSignInClient is initialized
        if (mGoogleSignInClient == null) {
            Log.e(TAG, "GoogleSignInClient is null, reinitializing...");
            initializeGoogleSignIn();
            if (mGoogleSignInClient == null) {
                showError("Google Sign-In not available. Please try again.");
                return;
            }
        }
        
        // Show loading state
        setGoogleSignInLoading(true);
        
        try {
            // Sign out first to ensure account picker shows
            mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
                Log.d(TAG, "Previous Google session cleared");
                
                // Create sign-in intent and start activity for result
                Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                startActivityForResult(signInIntent, RC_SIGN_IN);
            });
            
        } catch (Exception e) {
            Log.e(TAG, "Error starting Google Sign-In", e);
            setGoogleSignInLoading(false);
            showError("Failed to start Google Sign-In: " + e.getMessage());
        }
    }

    /**
     * Handle activity results - specifically for Google Sign-In
     * This is the traditional Java approach using onActivityResult()
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.d(TAG, "onActivityResult called - requestCode: " + requestCode + ", resultCode: " + resultCode);

        // Check if the result is from Google Sign-In
        if (requestCode == RC_SIGN_IN) {
            Log.d(TAG, "Processing Google Sign-In result...");
            
            // Handle the result
            handleGoogleSignInResult(data);
        }
    }

    /**
     * Handle Google Sign-In result with comprehensive error handling
     */
    private void handleGoogleSignInResult(Intent data) {
        Log.d(TAG, "=== HANDLING GOOGLE SIGN-IN RESULT ===");
        
        try {
            // Get the GoogleSignInAccount from the intent
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            Log.d(TAG, "Task obtained from intent, checking success...");
            
            // Check if the task was successful
            if (task.isSuccessful()) {
                Log.d(TAG, "Task is successful, getting account...");
                GoogleSignInAccount account = task.getResult();
                
                if (account != null) {
                    Log.d(TAG, "=== GOOGLE SIGN-IN SUCCESS ===");
                    Log.d(TAG, "Account Email: " + account.getEmail());
                    Log.d(TAG, "Account Name: " + account.getDisplayName());
                    Log.d(TAG, "Account ID: " + account.getId());
                    
                    // Process successful sign-in
                    Log.d(TAG, "Processing successful Google Sign-In...");
                    processSuccessfulGoogleSignIn(account);
                } else {
                    Log.e(TAG, "Google Sign-In account is null despite successful task");
                    setGoogleSignInLoading(false);
                    showError("Google Sign-In failed: No account information received");
                }
            } else {
                Log.e(TAG, "Google Sign-In task failed");
                // Handle the exception
                Exception exception = task.getException();
                if (exception instanceof ApiException) {
                    ApiException apiException = (ApiException) exception;
                    Log.e(TAG, "ApiException: " + apiException.getStatusCode() + " - " + apiException.getMessage());
                    handleGoogleSignInError(apiException);
                } else {
                    Log.e(TAG, "Non-ApiException: " + (exception != null ? exception.getClass().getSimpleName() : "null"));
                    setGoogleSignInLoading(false);
                    showError("Google Sign-In failed: " + (exception != null ? exception.getMessage() : "Unknown error"));
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Exception in handleGoogleSignInResult", e);
            setGoogleSignInLoading(false);
            showError("Error processing Google Sign-In result: " + e.getMessage());
        }
    }

    /**
     * Handle Google Sign-In API errors
     */
    private void handleGoogleSignInError(ApiException e) {
        int statusCode = e.getStatusCode();
        Log.e(TAG, "Google Sign-In failed with status code: " + statusCode, e);
        
        setGoogleSignInLoading(false);
        
        String errorMessage;
        switch (statusCode) {
            case 12501: // User cancelled
                errorMessage = "Sign-in cancelled by user";
                Log.d(TAG, "User cancelled Google Sign-In");
                return; // Don't show error for user cancellation
                
            case 12502: // Sign-in currently in progress
                errorMessage = "Sign-in already in progress. Please wait.";
                break;
                
            case 12500: // Sign-in failed
                errorMessage = "Google Sign-In failed. Please check your internet connection and try again.";
                break;
                
            case 10: // Developer error (configuration issue)
                errorMessage = "Configuration error. Please check SHA-1 fingerprint and Firebase setup.";
                Log.e(TAG, "Developer error - check SHA-1 fingerprint in Firebase Console");
                break;
                
            case 7: // Network error
                errorMessage = "Network error. Please check your internet connection.";
                break;
                
            default:
                errorMessage = "Google Sign-In failed (Code: " + statusCode + "). Please try again.";
                break;
        }
        
        Log.e(TAG, "Google Sign-In error: " + errorMessage);
        showError(errorMessage);
    }

    /**
     * Process successful Google Sign-In with comprehensive data handling
     * @param account The Google account from successful sign-in
     */
    private void processSuccessfulGoogleSignIn(GoogleSignInAccount account) {
        Log.d(TAG, "Processing successful Google Sign-In...");
        
        // Process Google Sign-In success silently
        
        try {
            // Validate account data
            if (account.getEmail() == null || account.getEmail().isEmpty()) {
                Log.e(TAG, "Google account email is null or empty");
                setGoogleSignInLoading(false);
                showError("Google Sign-In failed: No email address found");
                return;
            }
            
            // Extract comprehensive user information
            String email = account.getEmail();
            String displayName = account.getDisplayName();
            String firstName = account.getGivenName();
            String lastName = account.getFamilyName();
            String userId = account.getId(); // Use Google's unique ID
            String photoUrl = account.getPhotoUrl() != null ? account.getPhotoUrl().toString() : null;
            String idToken = account.getIdToken();
            
            Log.d(TAG, "Google Account Details:");
            Log.d(TAG, "  Email: " + email);
            Log.d(TAG, "  Display Name: " + displayName);
            Log.d(TAG, "  First Name: " + firstName);
            Log.d(TAG, "  Last Name: " + lastName);
            Log.d(TAG, "  User ID: " + userId);
            Log.d(TAG, "  Photo URL: " + photoUrl);
            Log.d(TAG, "  Has ID Token: " + (idToken != null));
            
            // Save comprehensive Google login state
            saveComprehensiveGoogleLoginState(account);
            
            // Hide loading state
            setGoogleSignInLoading(false);
            
            // Show personalized success message
            String welcomeName = displayName;
            if (welcomeName == null || welcomeName.isEmpty()) {
                welcomeName = firstName;
            }
            if (welcomeName == null || welcomeName.isEmpty()) {
                welcomeName = email.split("@")[0]; // Use email prefix as fallback
            }
            
            Log.d(TAG, "Google Sign-In completed successfully, navigating to main app...");
            
            // Navigate immediately to main app
            navigateToMainAppImmediately();
            
        } catch (Exception e) {
            Log.e(TAG, "Error processing Google Sign-In success", e);
            setGoogleSignInLoading(false);
            showError("Failed to process sign-in: " + e.getMessage());
        }
    }

    /**
     * Save comprehensive Google login state with all available data
     */
    private void saveComprehensiveGoogleLoginState(GoogleSignInAccount account) {
        try {
            SharedPreferences.Editor editor = healthScannerPrefs.edit();
            
            // Save authentication state
            editor.putBoolean("is_logged_in", true);
            editor.putLong("login_timestamp", System.currentTimeMillis());
            
            // Save comprehensive user profile information (session-based, not persistent)
            editor.putString("current_user_email", account.getEmail());
            editor.putString("current_user_id", account.getId());
            editor.putString("auth_provider", "google.com");
            
            // Save names with fallbacks
            if (account.getDisplayName() != null) {
                editor.putString("current_user_name", account.getDisplayName());
            }
            if (account.getGivenName() != null) {
                editor.putString("current_user_first_name", account.getGivenName());
            }
            if (account.getFamilyName() != null) {
                editor.putString("current_user_last_name", account.getFamilyName());
            }
            
            // Save photo URL if available
            if (account.getPhotoUrl() != null) {
                editor.putString("current_user_photo", account.getPhotoUrl().toString());
            }
            
            // Save additional Google account details for rich profile display
            editor.putString("google_account_type", "Google Account");
            editor.putLong("join_date_timestamp", System.currentTimeMillis());
            editor.putString("account_source", "Google Sign-In");
            
            // Mark as fresh sign-in for special welcome
            editor.putBoolean("fresh_google_signin", true);
            
            // Save ID token for future use (if needed)
            if (account.getIdToken() != null) {
                editor.putString("google_id_token", account.getIdToken());
            }
            
            // Save additional metadata
            editor.putString("login_method", "google_signin");
            editor.putLong("account_creation_time", System.currentTimeMillis());
            editor.putBoolean("is_google_account", true);
            editor.putBoolean("is_first_launch_after_signin", true); // Mark for welcome message
            
            // Apply all changes atomically
            boolean success = editor.commit();
            
            if (success) {
                Log.d(TAG, "Google login state saved successfully for: " + account.getEmail());
                Log.d(TAG, "Saved user data: " + account.getDisplayName());
                
                // Create user profile in Firebase database
                createFirebaseUserProfile(account);
            } else {
                Log.e(TAG, "Failed to save Google login state");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error saving Google login state", e);
            throw e; // Re-throw to handle in calling method
        }
    }
    
    /**
     * Create comprehensive user profile in Firebase for Google Sign-In users
     */
    private void createFirebaseUserProfile(GoogleSignInAccount account) {
        try {
            Log.d(TAG, "Creating Firebase user profile for Google user: " + account.getEmail());
            
            // Create comprehensive user data map
            java.util.Map<String, Object> userData = new java.util.HashMap<>();
            
            // Basic profile information
            userData.put("email", account.getEmail());
            userData.put("displayName", account.getDisplayName());
            userData.put("userId", account.getId());
            userData.put("authProvider", "google.com");
            
            // Google-specific information
            if (account.getGivenName() != null) {
                userData.put("firstName", account.getGivenName());
            }
            if (account.getFamilyName() != null) {
                userData.put("lastName", account.getFamilyName());
            }
            if (account.getPhotoUrl() != null) {
                userData.put("photoUrl", account.getPhotoUrl().toString());
            }
            
            // Account metadata
            userData.put("accountType", "Google Account");
            userData.put("accountSource", "Google Sign-In");
            userData.put("isGoogleAccount", true);
            userData.put("emailVerified", true); // Google accounts are pre-verified
            
            // Timestamps
            java.util.Date now = new java.util.Date();
            userData.put("createdAt", now);
            userData.put("lastLoginAt", now);
            userData.put("joinDateTimestamp", System.currentTimeMillis());
            
            // App-specific data
            userData.put("appVersion", "1.0.0");
            userData.put("platform", "Android");
            userData.put("deviceInfo", android.os.Build.MODEL + " " + android.os.Build.VERSION.RELEASE);
            
            // Health app specific defaults
            userData.put("totalScans", 0);
            userData.put("healthyChoices", 0);
            userData.put("averageHealthScore", 0.0);
            userData.put("notificationsEnabled", true);
            userData.put("darkModeEnabled", false);
            
            // Initialize empty collections
            userData.put("scanHistory", "[]");
            userData.put("healthConcerns", new java.util.ArrayList<String>());
            userData.put("dietaryPreferences", new java.util.ArrayList<String>());
            
            // Check if user already exists in Firebase
            firebaseManager.checkUserExists(account.getId(), new FirebaseManager.UserExistsCallback() {
                @Override
                public void onResult(boolean exists) {
                    if (exists) {
                        // User exists, update last login and sync any profile changes
                        Log.d(TAG, "Google user exists in Firebase, updating profile...");
                        updateExistingFirebaseUser(account, userData);
                    } else {
                        // New user, create complete profile
                        Log.d(TAG, "New Google user, creating Firebase profile...");
                        createNewFirebaseUser(account, userData);
                    }
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "Error creating Firebase user profile for Google user", e);
            // Don't block login on Firebase failure
        }
    }
    
    /**
     * Create new Firebase user profile for Google Sign-In
     */
    private void createNewFirebaseUser(GoogleSignInAccount account, java.util.Map<String, Object> userData) {
        firebaseManager.syncCompleteUserData(account.getId(), userData, new FirebaseManager.OperationCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "✅ NEW Google user profile created in Firebase!");
                Log.d(TAG, "👤 User: " + account.getDisplayName() + " (" + account.getEmail() + ")");
                Log.d(TAG, "🔍 Firebase Console: https://console.firebase.google.com/project/nure-70d49/firestore/data/users/" + account.getId());
                
                // Update local preferences
                healthScannerPrefs.edit()
                    .putBoolean("firebase_profile_created", true)
                    .putLong("firebase_profile_timestamp", System.currentTimeMillis())
                    .putBoolean("is_new_firebase_user", true)
                    .apply();
            }
            
            @Override
            public void onFailure(String error) {
                Log.e(TAG, "❌ Failed to create new Google user in Firebase: " + error);
            }
        });
    }
    
    /**
     * Update existing Firebase user profile for Google Sign-In
     */
    private void updateExistingFirebaseUser(GoogleSignInAccount account, java.util.Map<String, Object> userData) {
        // Update only essential fields for existing users
        java.util.Map<String, Object> updateData = new java.util.HashMap<>();
        updateData.put("lastLoginAt", new java.util.Date());
        updateData.put("displayName", account.getDisplayName());
        
        if (account.getPhotoUrl() != null) {
            updateData.put("photoUrl", account.getPhotoUrl().toString());
        }
        
        firebaseManager.syncCompleteUserData(account.getId(), updateData, new FirebaseManager.OperationCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "✅ EXISTING Google user profile updated in Firebase!");
                Log.d(TAG, "👤 User: " + account.getDisplayName() + " (" + account.getEmail() + ")");
                Log.d(TAG, "🔍 Firebase Console: https://console.firebase.google.com/project/nure-70d49/firestore/data/users/" + account.getId());
                
                // Update local preferences
                healthScannerPrefs.edit()
                    .putLong("firebase_profile_timestamp", System.currentTimeMillis())
                    .putBoolean("is_returning_firebase_user", true)
                    .apply();
            }
            
            @Override
            public void onFailure(String error) {
                Log.e(TAG, "❌ Failed to update existing Google user in Firebase: " + error);
            }
        });
    }

    /**
     * Handle simple email/password login (bypassing Firebase for now)
     */
    private void handleSimpleEmailLogin(String email, String password) {
        // Simulate authentication delay
        btnLogin.postDelayed(() -> {
            setLoginLoading(false);
            
            // For demo purposes, accept any valid email/password combination
            // You can add proper validation or Firebase later
            if (email.contains("@") && password.length() >= 6) {
                // Save login state
                saveEmailLoginState(email);
                
                // Show success
                showSuccess("Welcome back!");
                
                // Navigate to main
                navigateToMain();
            } else {
                showError("Invalid email or password. Please try again.");
            }
        }, 1000); // 1 second delay to simulate authentication
    }

    /**
     * Save email login state
     */
    private void saveEmailLoginState(String email) {
        SharedPreferences.Editor editor = healthScannerPrefs.edit();
        
        // Save authentication state
        editor.putBoolean("is_logged_in", true);
        editor.putLong("login_timestamp", System.currentTimeMillis());
        
        // Save user profile information
        editor.putString("current_user_email", email);
        editor.putString("current_user_name", email.split("@")[0]); // Use email prefix as name
        editor.putString("current_user_id", "email_" + email.hashCode());
        editor.putString("auth_provider", "password");
        
        // Handle remember me
        if (cbRememberMe != null && cbRememberMe.isChecked()) {
            editor.putString("saved_email", email);
            editor.putBoolean("remember_me", true);
        }
        
        editor.apply();
        
        Log.d(TAG, "Email login state saved for: " + email);
    }

    /**
     * Authenticate user with Firebase using email and password
     */
    private void authenticateWithFirebase(String email, String password) {
        Log.d(TAG, "Authenticating with Firebase: " + email);
        
        // Sign in with Firebase Authentication
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        setLoginLoading(false);
                        
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Firebase authentication successful");
                            FirebaseUser user = mAuth.getCurrentUser();
                            
                            if (user != null) {
                                // Log email verification status (non-blocking)
                                if (!user.isEmailVerified()) {
                                    Log.w(TAG, "Email not verified for user: " + user.getEmail());
                                    // Don't block login — just remind the user
                                }
                                
                                // Save comprehensive login state with Firebase data
                                saveFirebaseLoginState(user);
                                
                                // Sync with Firebase database
                                syncUserDataFromFirebase(user);
                                
                                // Show success message
                                String displayName = user.getDisplayName();
                                if (displayName != null && !displayName.isEmpty()) {
                                    showSuccess("Welcome back, " + displayName + "!");
                                } else {
                                    showSuccess("Welcome back!");
                                }
                                
                                // Navigate to home page
                                navigateToHomePage();
                            }
                            
                        } else {
                            Log.e(TAG, "Firebase authentication failed", task.getException());
                            
                            // Handle specific Firebase authentication errors
                            String errorMessage = getFirebaseErrorMessage(task.getException());
                            showError(errorMessage);
                            
                            // Log security-relevant failures
                            if (task.getException() != null) {
                                Log.w(TAG, "Login failure for " + email + ": " + task.getException().getMessage());
                            }
                        }
                    }
                });
    }

    /**
     * Show email verification dialog for unverified users
     */
    private void showEmailVerificationDialog(FirebaseUser user) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Email Verification Required")
                .setMessage("Please verify your email address before signing in. Check your inbox for a verification email.")
                .setPositiveButton("Resend Verification", (dialog, which) -> {
                    resendEmailVerification(user);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Resend email verification
     */
    private void resendEmailVerification(FirebaseUser user) {
        user.sendEmailVerification()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        showSuccess("Verification email sent. Please check your inbox.");
                    } else {
                        showError("Failed to send verification email. Please try again later.");
                    }
                });
    }

    /**
     * Save Firebase login state with comprehensive user data
     */
    private void saveFirebaseLoginState(FirebaseUser user) {
        try {
            SharedPreferences.Editor editor = healthScannerPrefs.edit();
            
            // Save authentication state
            editor.putBoolean("is_logged_in", true);
            editor.putLong("login_timestamp", System.currentTimeMillis());
            
            // Save comprehensive user profile information
            editor.putString("current_user_email", user.getEmail());
            editor.putString("current_user_name", user.getDisplayName());
            editor.putString("current_user_id", user.getUid());
            editor.putBoolean("email_verified", user.isEmailVerified());
            
            // Save authentication provider
            if (user.getProviderData() != null && !user.getProviderData().isEmpty()) {
                editor.putString("auth_provider", user.getProviderData().get(0).getProviderId());
            } else {
                editor.putString("auth_provider", "password");
            }
            
            // Save profile photo URL if available
            if (user.getPhotoUrl() != null) {
                editor.putString("current_user_photo", user.getPhotoUrl().toString());
            }
            
            // Handle remember me preference
            if (cbRememberMe != null && cbRememberMe.isChecked()) {
                editor.putString("saved_email", user.getEmail());
                editor.putBoolean("remember_me", true);
            }
            
            // Security metadata
            editor.putLong("last_login_time", System.currentTimeMillis());
            editor.putString("login_method", "firebase_auth");
            
            // Apply changes
            editor.apply();
            
            Log.d(TAG, "Firebase login state saved successfully for: " + user.getEmail());
            
            // Create user profile in Firebase Firestore (for email/password login)
            createFirebaseUserProfile(user);
            
        } catch (Exception e) {
            Log.e(TAG, "Error saving Firebase login state", e);
        }
    }

    /**
     * Sync user data from Firebase database
     */
    private void syncUserDataFromFirebase(FirebaseUser user) {
        Log.d(TAG, "Syncing user data from Firebase database...");
        
        // Use AuthManager to sync with database
        try {
            AuthManager authManager = AuthManager.getInstance(this);
            authManager.saveUserAuthState(user, new AuthManager.AuthCallback() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "User data synced from Firebase database successfully");
                }
                
                @Override
                public void onFailure(String error) {
                    Log.w(TAG, "Failed to sync user data from Firebase database: " + error);
                    // Continue anyway - user is authenticated
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error syncing user data from Firebase", e);
            // Continue anyway - user is authenticated
        }
    }

    /**
     * Verify Google Sign-In configuration and provide debugging info
     */
    private void verifyGoogleSignInSetup() {
        Log.d(TAG, "=== Google Sign-In Configuration Verification ===");
        
        try {
            // Check web client ID
            String webClientId = getString(R.string.default_web_client_id);
            Log.d(TAG, "Web Client ID: " + webClientId);
            Log.d(TAG, "Web Client ID Length: " + webClientId.length());
            Log.d(TAG, "Web Client ID Valid Format: " + webClientId.contains("googleusercontent.com"));
            
            // Check GoogleSignInClient
            Log.d(TAG, "GoogleSignInClient initialized: " + (mGoogleSignInClient != null));
            
            // Check last signed in account
            GoogleSignInAccount lastAccount = GoogleSignIn.getLastSignedInAccount(this);
            if (lastAccount != null) {
                Log.d(TAG, "Last signed in account: " + lastAccount.getEmail());
            } else {
                Log.d(TAG, "No previous Google Sign-In account found");
            }
            
            // Check Firebase Auth
            Log.d(TAG, "Firebase Auth initialized: " + (mAuth != null));
            
            Log.d(TAG, "=== Configuration Verification Complete ===");
            
        } catch (Exception e) {
            Log.e(TAG, "Error during configuration verification", e);
        }
    }

    /**
     * Test Google Sign-In functionality (for debugging)
     */
    private void testGoogleSignInSetup() {
        Log.d(TAG, "Testing Google Sign-In setup...");
        
        if (mGoogleSignInClient == null) {
            Log.e(TAG, "GoogleSignInClient is null - initialization failed");
            return;
        }
        
        // Test silent sign-in
        mGoogleSignInClient.silentSignIn()
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        GoogleSignInAccount account = task.getResult();
                        Log.d(TAG, "Silent sign-in successful: " + account.getEmail());
                    } else {
                        Log.d(TAG, "Silent sign-in failed (normal if no previous sign-in): " + task.getException());
                    }
                });
    }

    /**
     * OLD METHOD - Authenticate with Firebase using Google ID token (DISABLED)
     * @param idToken The Google ID token obtained from successful Google Sign-In
     */
    private void firebaseAuthWithGoogle_DISABLED(String idToken) {
        // THIS METHOD IS DISABLED - USING DIRECT GOOGLE AUTH INSTEAD
        Log.d(TAG, "Authenticating with Firebase using Google credentials");
        
        if (idToken == null || idToken.isEmpty()) {
            Log.e(TAG, "ID Token is null or empty");
            setGoogleSignInLoading(false);
            showError("Authentication failed: Invalid Google token");
            return;
        }
        
        // Check network connectivity first
        if (!isNetworkAvailable()) {
            Log.w(TAG, "No network connection, using offline authentication");
            handleOfflineGoogleAuth(idToken);
            return;
        }
        
        try {
            // Create Firebase credential using Google ID token
            AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
            Log.d(TAG, "Created Firebase credential successfully");
            
            // Sign in to Firebase with the Google credential
            mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        // Hide loading state
                        setGoogleSignInLoading(false);
                        
                        if (task.isSuccessful()) {
                            // Firebase authentication successful
                            Log.d(TAG, "Firebase authentication with Google successful");
                            FirebaseUser user = mAuth.getCurrentUser();
                            
                            if (user != null) {
                                Log.d(TAG, "Firebase user authenticated: " + user.getEmail());
                                
                                // Save basic login state (simple approach)
                                saveLoginState(user);
                                
                                // Create user profile in Firebase Firestore
                                createFirebaseUserProfile(user);
                                
                                // Show success message
                                String displayName = user.getDisplayName();
                                if (displayName != null && !displayName.isEmpty()) {
                                    showSuccess("Welcome, " + displayName + "!");
                                } else {
                                    showSuccess("Welcome! Sign-in successful.");
                                }
                                
                                // Navigate to home page immediately
                                navigateToHomePage();
                            }
                            
                        } else {
                            // Firebase authentication failed
                            Exception exception = task.getException();
                            Log.e(TAG, "Firebase authentication failed: " + (exception != null ? exception.getMessage() : "Unknown error"), exception);
                            
                            // Check if it's a network error
                            if (exception != null && 
                                (exception.getClass().getSimpleName().contains("Network") || 
                                 (exception.getMessage() != null && exception.getMessage().toLowerCase().contains("network")))) {
                                
                                Log.w(TAG, "Network error during Firebase auth, trying offline mode");
                                handleOfflineGoogleAuth(idToken);
                                return;
                            }
                            
                            // Get user-friendly error message
                            String errorMessage = getFirebaseErrorMessage(exception);
                            String debugMessage = "Firebase Auth Error: " + errorMessage;
                            if (exception != null) {
                                debugMessage += " (" + exception.getClass().getSimpleName() + ")";
                            }
                            Log.e(TAG, "Detailed Firebase error: " + debugMessage);
                            showError(debugMessage);
                            
                            // Check for specific error cases
                            if (exception != null) {
                                String exceptionMessage = exception.getMessage();
                                if (exceptionMessage != null && 
                                    exceptionMessage.contains("account exists with different credential")) {
                                    showError("An account with this email already exists. Please try signing in with email/password.");
                                }
                            }
                        }
                    }
                });
        } catch (Exception e) {
            Log.e(TAG, "Exception during Firebase authentication", e);
            setGoogleSignInLoading(false);
            
            // Check if it's a network error and try offline auth
            if (e.getMessage() != null && e.getMessage().contains("network")) {
                Log.w(TAG, "Network error detected, trying offline authentication");
                handleOfflineGoogleAuth(idToken);
            } else {
                showError("Authentication error: " + e.getMessage());
            }
        }
    }

    /**
     * Check if network is available
     */
    private boolean isNetworkAvailable() {
        try {
            android.net.ConnectivityManager connectivityManager = 
                (android.net.ConnectivityManager) getSystemService(android.content.Context.CONNECTIVITY_SERVICE);
            android.net.NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        } catch (Exception e) {
            Log.w(TAG, "Error checking network connectivity", e);
            return false;
        }
    }

    /**
     * Handle Google authentication when offline or network issues
     */
    private void handleOfflineGoogleAuth(String idToken) {
        Log.d(TAG, "Handling offline Google authentication");
        
        try {
            // Get Google account info from the signed-in account
            GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
            if (account != null) {
                // Create a mock Firebase user for offline mode
                String email = account.getEmail();
                String displayName = account.getDisplayName();
                String userId = "google_" + email.hashCode(); // Generate consistent ID
                
                // Save offline login state
                saveOfflineGoogleLoginState(email, displayName, userId);
                
                setGoogleSignInLoading(false);
                showSuccess("Welcome, " + (displayName != null ? displayName : "User") + "! (Offline Mode)");
                navigateToMain();
                
            } else {
                setGoogleSignInLoading(false);
                showError("Network error and no cached Google account found. Please check your internet connection.");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in offline Google auth", e);
            setGoogleSignInLoading(false);
            showError("Authentication failed. Please check your internet connection and try again.");
        }
    }

    /**
     * Save offline Google login state
     */
    private void saveOfflineGoogleLoginState(String email, String displayName, String userId) {
        SharedPreferences.Editor editor = healthScannerPrefs.edit();
        
        // Save authentication state
        editor.putBoolean("is_logged_in", true);
        editor.putLong("login_timestamp", System.currentTimeMillis());
        
        // Save user profile information
        editor.putString("current_user_email", email);
        editor.putString("current_user_name", displayName);
        editor.putString("current_user_id", userId);
        editor.putString("auth_provider", "google.com");
        editor.putBoolean("offline_mode", true);
        
        // Apply changes
        editor.apply();
        
        Log.d(TAG, "Offline Google login state saved for: " + email);
    }

    /**
     * Get user-friendly error message for Google Sign-In errors
     * @param statusCode The error status code from Google Sign-In
     * @return User-friendly error message
     */
    private String getGoogleSignInErrorMessage(int statusCode) {
        switch (statusCode) {
            case 12501: // User cancelled the sign-in
                return "Sign-in cancelled. Please try again.";
            case 12502: // Sign-in currently in progress
                return "Sign-in already in progress. Please wait.";
            case 12500: // Sign-in failed
                return "Google Sign-In failed. Please check your internet connection and try again.";
            case 10: // Developer error (wrong SHA-1, wrong client ID, etc.)
                return "Configuration error. Please contact support.";
            default:
                return "Google Sign-In failed. Please try again.";
        }
    }

    /**
     * Save user login state and profile information to SharedPreferences
     * @param user The authenticated Firebase user
     */
    private void saveLoginState(FirebaseUser user) {
        if (user == null) {
            Log.w(TAG, "Cannot save login state: user is null");
            return;
        }
        
        SharedPreferences.Editor editor = healthScannerPrefs.edit();
        
        // Save authentication state
        editor.putBoolean("is_logged_in", true);
        editor.putLong("login_timestamp", System.currentTimeMillis());
        
        // Save user profile information
        editor.putString("current_user_email", user.getEmail());
        editor.putString("current_user_name", user.getDisplayName());
        editor.putString("current_user_id", user.getUid());
        
        // Save authentication provider (google.com, password, etc.)
        if (user.getProviderData() != null && !user.getProviderData().isEmpty()) {
            editor.putString("auth_provider", user.getProviderData().get(0).getProviderId());
        }
        
        // Save profile photo URL if available
        if (user.getPhotoUrl() != null) {
            editor.putString("current_user_photo", user.getPhotoUrl().toString());
        }

        // Handle remember me preference (only for email/password login)
        if (cbRememberMe != null && cbRememberMe.isChecked()) {
            editor.putString("saved_email", user.getEmail());
            editor.putBoolean("remember_me", true);
        } else {
            // Clear remember me data if not checked
            editor.remove("saved_email");
            editor.putBoolean("remember_me", false);
        }

        // Apply changes
        editor.apply();
        
        Log.d(TAG, "Login state saved successfully for user: " + user.getEmail());
    }

    /**
     * Create user profile in Firebase Firestore with enhanced logging
     * @param user The authenticated Firebase user
     */
    private void createFirebaseUserProfile(FirebaseUser user) {
        if (user == null) {
            Log.w(TAG, "Cannot create Firebase profile: user is null");
            return;
        }
        
        Log.d(TAG, "🔥 CREATING FIREBASE USER PROFILE");
        Log.d(TAG, "User ID: " + user.getUid());
        Log.d(TAG, "Email: " + user.getEmail());
        Log.d(TAG, "Display Name: " + user.getDisplayName());
        Log.d(TAG, "Photo URL: " + (user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : "null"));
        Log.d(TAG, "Email Verified: " + user.isEmailVerified());
        
        if (user.getProviderData() != null && !user.getProviderData().isEmpty()) {
            Log.d(TAG, "Auth Provider: " + user.getProviderData().get(0).getProviderId());
        }
        
        FirebaseManager firebaseManager = FirebaseManager.getInstance();
        firebaseManager.createUserProfile(user, new FirebaseManager.OperationCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "✅ FIREBASE USER PROFILE CREATED SUCCESSFULLY!");
                Log.d(TAG, "🔍 Check Firebase Console: https://console.firebase.google.com/project/nure-70d49/firestore/data/users/" + user.getUid());
                
                // Clear SharedPreferences - we'll use Firebase as primary storage
                clearLocalStorage();
                
                // Save only essential auth data locally
                SharedPreferences.Editor editor = healthScannerPrefs.edit();
                editor.putBoolean("is_logged_in", true);
                editor.putLong("login_timestamp", System.currentTimeMillis());
                editor.putString("current_user_id", user.getUid());
                editor.putString("current_user_email", user.getEmail());
                editor.putBoolean("firebase_profile_created", true);
                editor.putLong("firebase_profile_timestamp", System.currentTimeMillis());
                
                // Mark as fresh sign-in for welcome message
                if (user.getProviderData() != null && !user.getProviderData().isEmpty()) {
                    String providerId = user.getProviderData().get(0).getProviderId();
                    editor.putString("auth_provider", providerId);
                    
                    if ("google.com".equals(providerId)) {
                        editor.putBoolean("fresh_google_signin", true);
                        editor.putString("google_account_type", "personal");
                        
                        // Extract first and last name from display name
                        String displayName = user.getDisplayName();
                        if (displayName != null && !displayName.isEmpty()) {
                            String[] nameParts = displayName.split(" ", 2);
                            editor.putString("current_user_first_name", nameParts[0]);
                            if (nameParts.length > 1) {
                                editor.putString("current_user_last_name", nameParts[1]);
                            }
                        }
                    }
                }
                
                editor.apply();
                Log.d(TAG, "✅ Essential auth data saved locally, Firebase is primary storage");
            }
            
            @Override
            public void onFailure(String error) {
                Log.e(TAG, "❌ FAILED TO CREATE FIREBASE USER PROFILE!");
                Log.e(TAG, "Error: " + error);
                Log.e(TAG, "🔧 Check Firebase configuration and internet connection");
                
                // Still save basic auth data so user can use the app
                SharedPreferences.Editor editor = healthScannerPrefs.edit();
                editor.putBoolean("is_logged_in", true);
                editor.putLong("login_timestamp", System.currentTimeMillis());
                editor.putString("current_user_id", user.getUid());
                editor.putString("current_user_email", user.getEmail());
                editor.putBoolean("firebase_profile_created", false);
                editor.apply();
                
                // Show user a message about offline mode
                runOnUiThread(() -> {
                    Toast.makeText(LoginActivity.this, 
                        "Signed in successfully. Some features may be limited due to connection issues.", 
                        Toast.LENGTH_LONG).show();
                });
            }
        });
    }
    
    /**
     * Clear local storage to use Firebase as primary storage
     */
    private void clearLocalStorage() {
        Log.d(TAG, "🧹 Clearing local storage - Firebase will be primary storage");
        SharedPreferences.Editor editor = healthScannerPrefs.edit();
        
        // Clear old scan data stored locally
        editor.remove("recent_scans");
        editor.remove("scan_history");
        editor.remove("total_scans");
        editor.remove("healthy_choices");
        editor.remove("average_health_score");
        editor.remove("user_saved_items");
        editor.remove("health_concerns");
        editor.remove("dietary_preferences");
        
        editor.apply();
        Log.d(TAG, "✅ Local storage cleared - Firebase is now primary data source");
    }

    private void handleForgotPassword() {
        String email = etEmail.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            tilEmail.setError("Please enter your email address");
            etEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Please enter a valid email address");
            etEmail.requestFocus();
            return;
        }

        // Send password reset email
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            showSuccess("Password reset email sent to " + email);
                        } else {
                            String errorMessage = task.getException() != null ? 
                                task.getException().getMessage() : "Failed to send password reset email. Please try again.";
                            showError(errorMessage);
                        }
                    }
                });
    }

    /**
     * Navigate to home page (MainActivity) after successful login
     */
    private void navigateToHomePage() {
        Log.d(TAG, "=== STARTING NAVIGATION TO HOME PAGE ===");
        
        try {
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            
            // Add extra to indicate successful login
            intent.putExtra("login_success", true);
            intent.putExtra("show_welcome", true);
            
            Log.d(TAG, "Intent created, starting MainActivity...");
            startActivity(intent);
            
            Log.d(TAG, "MainActivity started, finishing LoginActivity...");
            finish();
            
            Log.d(TAG, "=== NAVIGATION TO HOME PAGE COMPLETED ===");
            
        } catch (Exception e) {
            Log.e(TAG, "Exception during navigation to home page", e);
            showError("Navigation failed: " + e.getMessage());
        }
    }

    /**
     * Navigate to home page with fallback mechanisms
     */
    private void navigateToHomePageWithFallback() {
        Log.d(TAG, "Attempting navigation to home page with fallbacks...");
        
        try {
            // First attempt: Direct navigation
            navigateToHomePage();
            
        } catch (Exception e1) {
            Log.e(TAG, "First navigation attempt failed", e1);
            
            try {
                // Second attempt: Simple intent without extras
                Log.d(TAG, "Trying fallback navigation...");
                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
                
            } catch (Exception e2) {
                Log.e(TAG, "Fallback navigation also failed", e2);
                
                // Third attempt: Use runOnUiThread
                runOnUiThread(() -> {
                    try {
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    } catch (Exception e3) {
                        Log.e(TAG, "All navigation attempts failed", e3);
                        showError("Navigation failed. Please restart the app to continue.");
                    }
                });
            }
        }
    }

    /**
     * Legacy method - redirects to navigateToHomePage
     */
    private void navigateToMain() {
        navigateToHomePage();
    }

    private void loadSavedCredentials() {
        // Check if user is remembered and auto-fill email
        String savedEmail = healthScannerPrefs.getString("saved_email", "");
        boolean rememberMe = healthScannerPrefs.getBoolean("remember_me", false);

        if (rememberMe && !TextUtils.isEmpty(savedEmail)) {
            etEmail.setText(savedEmail);
            cbRememberMe.setChecked(true);
        }
    }
    

    
    /**
     * Check if user is already logged in with Firebase
     * @return true if user is logged in with valid session, false otherwise
     */
    private boolean isUserAlreadyLoggedIn() {
        // Check SharedPreferences login state first
        boolean isLoggedIn = healthScannerPrefs.getBoolean("is_logged_in", false);
        if (!isLoggedIn) {
            return false;
        }
        
        // Check session validity
        if (!isSessionValid()) {
            Log.d(TAG, "Session expired");
            clearLoginState();
            return false;
        }
        
        // Check if user info exists in SharedPreferences (no Firebase needed)
        String userEmail = healthScannerPrefs.getString("current_user_email", "");
        String userId = healthScannerPrefs.getString("current_user_id", "");
        
        if (!userEmail.isEmpty() && !userId.isEmpty()) {
            Log.d(TAG, "User session is valid: " + userEmail);
            return true;
        } else {
            Log.d(TAG, "No user session found");
            clearLoginState();
            return false;
        }
    }
    
    /**
     * Clear login state from SharedPreferences
     */
    private void clearLoginState() {
        SharedPreferences.Editor editor = healthScannerPrefs.edit();
        editor.putBoolean("is_logged_in", false);
        editor.remove("login_timestamp");
        editor.remove("current_user_email");
        editor.remove("current_user_name");
        editor.remove("current_user_id");
        editor.remove("auth_provider");
        editor.remove("current_user_photo");
        editor.apply();
        
        Log.d(TAG, "Login state cleared");
    }

    private void setLoginLoading(boolean loading) {
        if (loading) {
            btnLogin.setEnabled(false);
            progressLogin.setVisibility(View.VISIBLE);
        } else {
            btnLogin.setEnabled(true);
            progressLogin.setVisibility(View.GONE);
        }
    }

    /**
     * Show/hide loading state for Google Sign-In button with enhanced UX
     * @param loading true to show loading, false to hide
     */
    private void setGoogleSignInLoading(boolean loading) {
        runOnUiThread(() -> {
            try {
                if (loading) {
                    // Disable button and show loading text
                    btnGoogleSignIn.setEnabled(false);
                    btnGoogleSignIn.setText("Signing in with Google...");
                    btnGoogleSignIn.setIcon(null); // Remove Google icon during loading
                    
                    // Show progress indicator
                    if (progressLogin != null) {
                        progressLogin.setVisibility(View.VISIBLE);
                    }
                    
                    // Disable other buttons during Google Sign-In
                    if (btnLogin != null) btnLogin.setEnabled(false);
                    if (btnSignup != null) btnSignup.setEnabled(false);
                    if (btnForgotPassword != null) btnForgotPassword.setEnabled(false);
                    
                    Log.d(TAG, "Google Sign-In loading state: ON");
                    
                } else {
                    // Re-enable button and restore original text
                    btnGoogleSignIn.setEnabled(true);
                    btnGoogleSignIn.setText("Continue with Google");
                    
                    // Restore Google icon
                    try {
                        btnGoogleSignIn.setIcon(getDrawable(R.drawable.ic_google));
                    } catch (Exception e) {
                        Log.w(TAG, "Could not restore Google icon", e);
                    }
                    
                    // Hide progress indicator
                    if (progressLogin != null) {
                        progressLogin.setVisibility(View.GONE);
                    }
                    
                    // Re-enable other buttons
                    if (btnLogin != null) btnLogin.setEnabled(true);
                    if (btnSignup != null) btnSignup.setEnabled(true);
                    if (btnForgotPassword != null) btnForgotPassword.setEnabled(true);
                    
                    Log.d(TAG, "Google Sign-In loading state: OFF");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error updating Google Sign-In loading state", e);
            }
        });
    }

    private void showError(String message) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG)
                .setBackgroundTint(getColor(R.color.error_color))
                .setTextColor(getColor(R.color.white))
                .show();
    }

    private void showSuccess(String message) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_SHORT)
                .setBackgroundTint(getColor(R.color.success_color))
                .setTextColor(getColor(R.color.white))
                .show();
    }

    private String getFirebaseErrorMessage(Exception exception) {
        if (exception == null) return "An unknown error occurred";
        
        String message = exception.getMessage();
        if (message == null) return "An unknown error occurred";
        
        Log.e(TAG, "Firebase error message: " + message);
        
        String messageLower = message.toLowerCase();
        
        // Provide user-friendly error messages based on Firebase error codes
        if (messageLower.contains("invalid_email") || messageLower.contains("email address is badly formatted")) {
            return "Please enter a valid email address.";
        } else if (messageLower.contains("wrong_password") || messageLower.contains("password is invalid")) {
            return "Incorrect password. Please try again.";
        } else if (messageLower.contains("user_not_found") || messageLower.contains("no user record")) {
            return "No account found with this email. Please sign up first.";
        } else if (messageLower.contains("invalid_login_credentials") || messageLower.contains("invalid credential") || messageLower.contains("incorrect, malformed")) {
            return "Invalid email or password. Please check your credentials or sign up for a new account.";
        } else if (messageLower.contains("user_disabled")) {
            return "This account has been disabled. Please contact support.";
        } else if (messageLower.contains("too_many_requests") || messageLower.contains("too many")) {
            return "Too many failed attempts. Please try again later.";
        } else if (messageLower.contains("network") || messageLower.contains("connection")) {
            return "Network error. Please check your internet connection.";
        } else if (messageLower.contains("operation_not_allowed") || messageLower.contains("not enabled")) {
            return "Email/password sign-in is not enabled. Please contact support.";
        } else {
            // Show the actual error for debugging instead of a generic message
            return "Login failed: " + message;
        }
    }

    /**
     * Check if the current session is still valid
     * @return true if session is valid, false otherwise
     */
    private boolean isSessionValid() {
        // Check if login timestamp exists and is within 30 days
        long loginTime = healthScannerPrefs.getLong("login_timestamp", 0);
        long currentTime = System.currentTimeMillis();
        long thirtyDaysInMillis = 30L * 24 * 60 * 60 * 1000; // 30 days
        
        return (currentTime - loginTime) < thirtyDaysInMillis;
    }

    /**
     * Sign out the current user from all providers and clear session data
     * This method handles both Firebase and Google Sign-Out
     */
    private void signOutUser() {
        Log.d(TAG, "Signing out user from all providers");
        
        // Sign out from Firebase Authentication
        if (mAuth != null) {
            mAuth.signOut();
            Log.d(TAG, "Signed out from Firebase");
        }
        
        // Sign out from Google Sign-In (clears Google account selection)
        if (mGoogleSignInClient != null) {
            mGoogleSignInClient.signOut().addOnCompleteListener(this, new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    Log.d(TAG, "Signed out from Google Sign-In");
                }
            });
        }
        
        // Clear all session data from SharedPreferences
        SharedPreferences.Editor editor = healthScannerPrefs.edit();
        editor.putBoolean("is_logged_in", false);
        editor.remove("login_timestamp");
        editor.remove("current_user_email");
        editor.remove("current_user_name");
        editor.remove("current_user_id");
        editor.remove("auth_provider");
        editor.remove("current_user_photo");
        
        // Keep remember me data if user wants to be remembered
        if (!healthScannerPrefs.getBoolean("remember_me", false)) {
            editor.remove("saved_email");
        }
        
        editor.apply();
        Log.d(TAG, "Session data cleared successfully");
    }

    /**
     * Handle signup success message if coming from SignUpActivity
     */
    private void handleSignupSuccess() {
        Intent intent = getIntent();
        if (intent != null && intent.getBooleanExtra("signup_success", false)) {
            showSuccess("Account created successfully! Please sign in.");
            
            // Pre-fill email if provided
            String email = intent.getStringExtra("prefill_email");
            if (!TextUtils.isEmpty(email)) {
                etEmail.setText(email);
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null && isSessionValid()) {
            navigateToHomePage();
        }
    }

    /**
     * Public method to sign out user - can be called from other activities
     * This is useful for logout functionality from Profile or Settings
     */
    public static void performSignOut(AppCompatActivity activity) {
        // Get Firebase Auth instance
        FirebaseAuth auth = FirebaseAuth.getInstance();
        
        // Configure Google Sign-In (same as in initializeGoogleSignIn)
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(activity.getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(activity, gso);
        
        // Sign out from Firebase
        if (auth != null) {
            auth.signOut();
        }
        
        // Sign out from Google
        if (googleSignInClient != null) {
            googleSignInClient.signOut();
        }
        
        // Clear SharedPreferences
        SharedPreferences prefs = activity.getSharedPreferences("HealthScannerPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("is_logged_in", false);
        editor.remove("login_timestamp");
        editor.remove("current_user_email");
        editor.remove("current_user_name");
        editor.remove("current_user_id");
        editor.remove("auth_provider");
        editor.remove("current_user_photo");
        editor.apply();
        
        // Navigate to login screen
        Intent loginIntent = new Intent(activity, LoginActivity.class);
        loginIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(loginIntent);
        activity.finish();
        
        Log.d("LoginActivity", "User signed out successfully");
    }

    /**
     * Verify authentication state and navigate to home page
     */
    private void verifyAuthenticationAndNavigate() {
        Log.d(TAG, "Verifying authentication state before navigation...");
        
        // Check if authentication state is properly saved
        boolean isLoggedIn = healthScannerPrefs.getBoolean("is_logged_in", false);
        String userEmail = healthScannerPrefs.getString("current_user_email", "");
        
        Log.d(TAG, "Auth verification - isLoggedIn: " + isLoggedIn + ", userEmail: " + userEmail);
        
        if (isLoggedIn && !userEmail.isEmpty()) {
            Log.d(TAG, "Authentication verified successfully, proceeding with navigation");
            navigateToHomePageWithFallback();
        } else {
            Log.e(TAG, "Authentication verification failed, using bypass navigation");
            // Use bypass navigation immediately instead of retrying
            navigateToHomePageWithBypass();
        }
    }

    /**
     * Navigate to home page with authentication bypass
     */
    private void navigateToHomePageWithBypass() {
        Log.d(TAG, "Navigating to home page with authentication bypass...");
        
        try {
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            
            // Add bypass flag to skip authentication check in MainActivity
            intent.putExtra("bypass_auth_check", true);
            intent.putExtra("login_success", true);
            intent.putExtra("show_welcome", true);
            
            Log.d(TAG, "Starting MainActivity with bypass flag...");
            startActivity(intent);
            
            Log.d(TAG, "Finishing LoginActivity...");
            finish();
            
        } catch (Exception e) {
            Log.e(TAG, "Exception during bypass navigation", e);
            showError("Navigation failed: " + e.getMessage());
        }
    }

    /**
     * Navigate immediately to main app after Google Sign-In
     */
    private void navigateToMainAppImmediately() {
        Log.d(TAG, "=== NAVIGATING TO MAIN APP IMMEDIATELY ===");
        
        try {
            // Create intent to MainActivity (main app screen)
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            
            // Clear the entire task stack and start fresh
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            
            // Add flags to ensure we get into the main app
            intent.putExtra("bypass_auth_check", true);
            intent.putExtra("from_google_signin", true);
            intent.putExtra("user_authenticated", true);
            
            Log.d(TAG, "Starting MainActivity with direct navigation...");
            
            // Start the main app
            startActivity(intent);
            
            // Finish login activity completely
            finish();
            
            Log.d(TAG, "=== NAVIGATION TO MAIN APP COMPLETED ===");
            
        } catch (Exception e) {
            Log.e(TAG, "Error navigating to main app", e);
            
            // Fallback: Try simple navigation
            try {
                Intent fallbackIntent = new Intent(this, MainActivity.class);
                fallbackIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(fallbackIntent);
                finish();
            } catch (Exception e2) {
                Log.e(TAG, "Fallback navigation also failed", e2);
                showError("Unable to enter the app. Please restart and try again.");
            }
        }
    }

    @Override
    public void onBackPressed() {
        // Exit app when back is pressed on login screen
        super.onBackPressed();
        finishAffinity();
    }
}