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

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private static final int RC_SIGN_IN = 9001; // Request code for Google Sign-In

    // UI Elements
    private TextInputLayout tilEmail, tilPassword;
    private TextInputEditText etEmail, etPassword;
    private MaterialCheckBox cbRememberMe;
    private MaterialButton btnLogin, btnGoogleSignIn, btnSignup, btnForgotPassword;
    private CircularProgressIndicator progressLogin;

    // Firebase
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;

    // SharedPreferences
    private SharedPreferences healthScannerPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        
        // Initialize SharedPreferences
        healthScannerPrefs = getSharedPreferences("HealthScannerPrefs", MODE_PRIVATE);

        // Check if user is already signed in and session is valid
        if (isUserAlreadyLoggedIn()) {
            Log.d(TAG, "User already logged in, navigating to main screen");
            navigateToMain();
            return;
        }
        
        // Development helper - ensure test account exists
        // Only enable in debug builds (you can comment this out for production)
        ensureTestAccountExists();

        // Initialize Google Sign-In
        initializeGoogleSignIn();
        
        // Initialize views and listeners
        initializeViews();
        setupClickListeners();
        
        // Load saved email if remember me was checked
        loadSavedCredentials();
        
        // Check for signup success message
        handleSignupSuccess();
    }

    /**
     * Initialize Google Sign-In configuration
     * Sets up GoogleSignInOptions with web client ID from google-services.json
     */
    private void initializeGoogleSignIn() {
        // Configure Google Sign-In to request ID tokens and basic profile info
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id)) // Web client ID from Firebase
                .requestEmail() // Request user's email address
                .build();

        // Create GoogleSignInClient with the configured options
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        
        Log.d(TAG, "Google Sign-In initialized successfully");
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
        
        // Development helper - add long click for quick test login
        // TODO: Comment out or remove this in production
        btnLogin.setOnLongClickListener(v -> {
            quickTestLogin();
            return true;
        });
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

        // Check for test credentials bypass (for development)
        if (isTestCredentials(email, password)) {
            handleTestLogin(email);
            return;
        }

        // Show loading state
        setLoginLoading(true);

        // Sign in with Firebase
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        setLoginLoading(false);
                        
                        if (task.isSuccessful()) {
                            Log.d(TAG, "signInWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            
                            // Save login state and preferences
                            saveLoginState(user);
                            
                            // Navigate to main activity
                            navigateToMain();
                        } else {
                            Log.w(TAG, "signInWithEmail:failure", task.getException());
                            String errorMessage = task.getException() != null ? 
                                task.getException().getMessage() : "Authentication failed. Please try again.";
                            showError(errorMessage);
                        }
                    }
                });
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
        Log.d(TAG, "Starting Google Sign-In process");
        
        // Show loading state
        setGoogleSignInLoading(true);
        
        // Create sign-in intent and start activity for result
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    /**
     * Handle activity results - specifically for Google Sign-In
     * This is the traditional Java approach using onActivityResult()
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Check if the result is from Google Sign-In
        if (requestCode == RC_SIGN_IN) {
            Log.d(TAG, "Google Sign-In result received");
            
            // Get the GoogleSignInAccount from the intent
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            
            try {
                // Google Sign-In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                Log.d(TAG, "Google Sign-In successful for: " + account.getEmail());
                
                // Authenticate with Firebase using the Google account
                firebaseAuthWithGoogle(account.getIdToken());
                
            } catch (ApiException e) {
                // Google Sign-In failed
                Log.w(TAG, "Google Sign-In failed with code: " + e.getStatusCode(), e);
                setGoogleSignInLoading(false);
                
                // Show user-friendly error message based on error code
                String errorMessage = getGoogleSignInErrorMessage(e.getStatusCode());
                showError(errorMessage);
            }
        }
    }

    /**
     * Authenticate with Firebase using Google ID token
     * @param idToken The Google ID token obtained from successful Google Sign-In
     */
    private void firebaseAuthWithGoogle(String idToken) {
        Log.d(TAG, "Authenticating with Firebase using Google credentials");
        
        // Create Firebase credential using Google ID token
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        
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
                                // Save login state to SharedPreferences
                                saveLoginState(user);
                                
                                // Show success message with user's name
                                String displayName = user.getDisplayName();
                                if (displayName != null && !displayName.isEmpty()) {
                                    showSuccess("Welcome, " + displayName + "!");
                                } else {
                                    showSuccess("Welcome! Sign-in successful.");
                                }
                                
                                // Navigate to main activity
                                navigateToMain();
                            }
                            
                        } else {
                            // Firebase authentication failed
                            Log.w(TAG, "Firebase authentication failed", task.getException());
                            
                            // Get user-friendly error message
                            String errorMessage = getFirebaseErrorMessage(task.getException());
                            showError(errorMessage);
                            
                            // Check for specific error cases
                            if (task.getException() != null) {
                                String exceptionMessage = task.getException().getMessage();
                                if (exceptionMessage != null && 
                                    exceptionMessage.contains("account exists with different credential")) {
                                    showError("An account with this email already exists. Please try signing in with email/password.");
                                }
                            }
                        }
                    }
                });
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

    private void navigateToMain() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void loadSavedCredentials() {
        // Check if user is remembered and auto-fill email
        String savedEmail = healthScannerPrefs.getString("saved_email", "");
        boolean rememberMe = healthScannerPrefs.getBoolean("remember_me", false);

        if (rememberMe && !TextUtils.isEmpty(savedEmail)) {
            etEmail.setText(savedEmail);
            cbRememberMe.setChecked(true);
        }
        
        // Development helper - auto-fill test credentials
        // TODO: Remove this in production build
        loadDevelopmentCredentials();
    }
    
    /**
     * Development helper method to auto-fill test credentials
     * This should be removed or disabled in production builds
     */
    private void loadDevelopmentCredentials() {
        // Auto-fill test credentials for development
        // TODO: Comment out or remove this method in production
        etEmail.setText("aditya@test.com");
        etPassword.setText("123456");
        
        Log.d(TAG, "Development credentials auto-filled");
        
        // Show a subtle hint that these are test credentials
        showDevelopmentHint();
    }
    
    /**
     * Show a development hint about test credentials
     */
    private void showDevelopmentHint() {
        // Show a toast to indicate development mode
        Toast.makeText(this, "Development Mode: Test credentials loaded", Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Development helper - ensure test account exists in Firebase
     * This creates the test account if it doesn't exist
     */
    private void ensureTestAccountExists() {
        String testEmail = "aditya@test.com";
        String testPassword = "123456";
        
        // Try to sign in with test credentials first
        mAuth.signInWithEmailAndPassword(testEmail, testPassword)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Test account exists and works
                        Log.d(TAG, "Test account verified: " + testEmail);
                        mAuth.signOut(); // Sign out immediately, just checking if account exists
                    } else {
                        // Test account doesn't exist, create it
                        Log.d(TAG, "Creating test account: " + testEmail);
                        createTestAccount(testEmail, testPassword);
                    }
                });
    }
    
    /**
     * Create the test account in Firebase
     */
    private void createTestAccount(String email, String password) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Test account created successfully: " + email);
                        
                        // Update the user's display name
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName("Aditya (Test User)")
                                    .build();
                            
                            user.updateProfile(profileUpdates)
                                    .addOnCompleteListener(profileTask -> {
                                        if (profileTask.isSuccessful()) {
                                            Log.d(TAG, "Test user profile updated");
                                        }
                                        // Sign out after creating account
                                        mAuth.signOut();
                                    });
                        }
                        
                        Toast.makeText(this, "Test account created: " + email, Toast.LENGTH_SHORT).show();
                    } else {
                        Log.w(TAG, "Failed to create test account", task.getException());
                    }
                });
    }
    
    /**
     * Development helper - quick login with test credentials
     * Long press the login button to use this feature
     */
    private void quickTestLogin() {
        String testEmail = "aditya@test.com";
        String testPassword = "123456";
        
        // Fill the fields
        etEmail.setText(testEmail);
        etPassword.setText(testPassword);
        
        // Show hint
        Toast.makeText(this, "Quick Test Login: Long press activated", Toast.LENGTH_SHORT).show();
        
        // Auto-submit after a short delay
        btnLogin.postDelayed(() -> {
            if (!TextUtils.isEmpty(etEmail.getText()) && !TextUtils.isEmpty(etPassword.getText())) {
                handleEmailPasswordLogin();
            }
        }, 500);
    }
    
    /**
     * Check if the provided credentials are test credentials
     * @param email The email to check
     * @param password The password to check
     * @return true if these are test credentials, false otherwise
     */
    private boolean isTestCredentials(String email, String password) {
        return "aditya@test.com".equals(email) && "123456".equals(password);
    }
    
    /**
     * Handle test login bypass (for development when Firebase isn't configured)
     * @param email The test email
     */
    private void handleTestLogin(String email) {
        Log.d(TAG, "Test login bypass activated for: " + email);
        
        // Show loading state briefly
        setLoginLoading(true);
        
        // Simulate login delay
        btnLogin.postDelayed(() -> {
            setLoginLoading(false);
            
            // Save test login state to SharedPreferences
            saveTestLoginState(email);
            
            // Show success message
            showSuccess("Welcome, Aditya! (Test Mode)");
            
            // Navigate to main activity
            navigateToMain();
            
        }, 1000); // 1 second delay to simulate authentication
    }
    
    /**
     * Save test login state to SharedPreferences (bypasses Firebase)
     * @param email The test email
     */
    private void saveTestLoginState(String email) {
        SharedPreferences.Editor editor = healthScannerPrefs.edit();
        
        // Save authentication state
        editor.putBoolean("is_logged_in", true);
        editor.putLong("login_timestamp", System.currentTimeMillis());
        
        // Save test user profile information
        editor.putString("current_user_email", email);
        editor.putString("current_user_name", "Aditya (Test User)");
        editor.putString("current_user_id", "test_user_123");
        editor.putString("auth_provider", "test_mode");
        
        // Handle remember me preference
        if (cbRememberMe != null && cbRememberMe.isChecked()) {
            editor.putString("saved_email", email);
            editor.putBoolean("remember_me", true);
        }

        // Apply changes
        editor.apply();
        
        Log.d(TAG, "Test login state saved successfully for: " + email);
    }
    
    /**
     * Comprehensive check if user is already logged in (Firebase or test mode)
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
        
        // Check if it's test mode
        String authProvider = healthScannerPrefs.getString("auth_provider", "");
        if ("test_mode".equals(authProvider)) {
            Log.d(TAG, "Test mode session is valid");
            return true;
        }
        
        // Check Firebase authentication state for real users
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            Log.d(TAG, "Firebase user session is valid: " + currentUser.getEmail());
            return true;
        } else {
            Log.d(TAG, "No Firebase user found, clearing session");
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
     * Show/hide loading state for Google Sign-In button
     * @param loading true to show loading, false to hide
     */
    private void setGoogleSignInLoading(boolean loading) {
        if (loading) {
            // Disable button and show loading text
            btnGoogleSignIn.setEnabled(false);
            btnGoogleSignIn.setText("Signing in with Google...");
            
            // Show progress indicator
            if (progressLogin != null) {
                progressLogin.setVisibility(View.VISIBLE);
            }
            
            // Disable other buttons during Google Sign-In
            btnLogin.setEnabled(false);
            btnSignup.setEnabled(false);
            
        } else {
            // Re-enable button and restore original text
            btnGoogleSignIn.setEnabled(true);
            btnGoogleSignIn.setText("Continue with Google");
            
            // Hide progress indicator
            if (progressLogin != null) {
                progressLogin.setVisibility(View.GONE);
            }
            
            // Re-enable other buttons
            btnLogin.setEnabled(true);
            btnSignup.setEnabled(true);
        }
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
        
        // Provide user-friendly error messages based on Firebase error codes
        if (message.contains("ERROR_INVALID_EMAIL") || message.contains("email address is badly formatted")) {
            return "Please enter a valid email address.";
        } else if (message.contains("ERROR_WRONG_PASSWORD") || message.contains("password is invalid")) {
            return "Incorrect password. Please try again.";
        } else if (message.contains("ERROR_USER_NOT_FOUND") || message.contains("no user record")) {
            return "No account found with this email address.";
        } else if (message.contains("ERROR_USER_DISABLED")) {
            return "This account has been disabled. Please contact support.";
        } else if (message.contains("ERROR_TOO_MANY_REQUESTS")) {
            return "Too many failed attempts. Please try again later.";
        } else if (message.contains("ERROR_NETWORK_REQUEST_FAILED") || message.contains("network error")) {
            return "Network error. Please check your internet connection.";
        } else if (message.contains("ERROR_OPERATION_NOT_ALLOWED")) {
            return "Email/password sign-in is not enabled. Please contact support.";
        } else {
            return "Login failed. Please try again.";
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
            navigateToMain();
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

    @Override
    public void onBackPressed() {
        // Exit app when back is pressed on login screen
        super.onBackPressed();
        finishAffinity();
    }
}