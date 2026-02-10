package com.example.healthscanner;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;

public class SignUpActivity extends AppCompatActivity {

    private static final String TAG = "SignUpActivity";
    private static final int RC_SIGN_IN = 9001;

    // UI Elements
    private TextInputLayout tilFullName, tilEmail, tilPassword, tilConfirmPassword;
    private TextInputEditText etFullName, etEmail, etPassword, etConfirmPassword;
    private MaterialCheckBox cbTerms;
    private MaterialButton btnSignUp, btnSignIn, btnGoogleSignUp;
    private CircularProgressIndicator progressSignup;
    private ImageView btnBack;

    // Firebase
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;

    // SharedPreferences
    private SharedPreferences healthScannerPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply theme before calling super.onCreate()

        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup_modern);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        
        // Initialize SharedPreferences
        healthScannerPrefs = getSharedPreferences("HealthScannerPrefs", MODE_PRIVATE);

        // Initialize Google Sign-In
        initializeGoogleSignIn();

        initializeViews();
        setupClickListeners();
    }

    private void initializeViews() {
        // TextInputLayouts
        tilFullName = findViewById(R.id.til_full_name);
        tilEmail = findViewById(R.id.til_email);
        tilPassword = findViewById(R.id.til_password);
        tilConfirmPassword = findViewById(R.id.til_confirm_password);

        // EditTexts
        etFullName = findViewById(R.id.et_full_name);
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);

        // Other UI elements
        cbTerms = findViewById(R.id.cb_terms);
        btnSignUp = findViewById(R.id.btn_signup);
        btnSignIn = findViewById(R.id.btn_signin);
        btnGoogleSignUp = findViewById(R.id.btn_google_signup);
        progressSignup = findViewById(R.id.progress_signup);
        btnBack = findViewById(R.id.btn_back);
    }

    private void setupClickListeners() {
        btnSignUp.setOnClickListener(v -> handleSignUp());

        btnSignIn.setOnClickListener(v -> {
            Intent intent = new Intent(SignUpActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
        
        btnGoogleSignUp.setOnClickListener(v -> handleGoogleSignUp());
        
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                Intent intent = new Intent(SignUpActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            });
        }
    }
    
    private void initializeGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        Log.d(TAG, "Google Sign-In initialized for signup");
    }
    
    private void handleGoogleSignUp() {
        Log.d(TAG, "Starting Google Sign-Up process");
        setSignUpLoading(true);
        
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                Log.d(TAG, "Google Sign-Up successful for: " + account.getEmail());
                firebaseAuthWithGoogle(account.getIdToken());
                
            } catch (ApiException e) {
                Log.w(TAG, "Google Sign-Up failed with code: " + e.getStatusCode(), e);
                setSignUpLoading(false);
                showError("Google Sign-Up failed. Please try again.");
            }
        }
    }
    
    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        setSignUpLoading(false);
                        
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Firebase authentication with Google successful");
                            FirebaseUser user = mAuth.getCurrentUser();
                            
                            if (user != null) {
                                // Create user profile in database
                                createUserInDatabase(user, user.getDisplayName());
                            }
                            
                        } else {
                            Log.w(TAG, "Firebase authentication failed", task.getException());
                            showError("Sign-up failed. Please try again.");
                        }
                    }
                });
    }

    private void handleSignUp() {
        Log.d(TAG, "Starting secure signup process...");
        
        // Clear previous errors
        clearErrors();

        // Get user input with normalization
        String fullName = etFullName.getText().toString().trim();
        String email = etEmail.getText().toString().trim().toLowerCase(); // Normalize email
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        // Comprehensive validation with security checks
        if (!validateInputWithSecurityChecks(fullName, email, password, confirmPassword)) {
            return;
        }

        // Additional security checks
        if (!performSecurityChecks(email, password)) {
            return;
        }

        // Show loading state
        setSignUpLoading(true);

        Log.d(TAG, "Creating Firebase account for: " + email);

        // Create account with Firebase Authentication
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Firebase account created successfully");
                            FirebaseUser user = mAuth.getCurrentUser();
                            
                            if (user != null) {
                                // Send email verification for security
                                sendEmailVerification(user, fullName, email);
                            } else {
                                setSignUpLoading(false);
                                showError("Account creation failed. Please try again.");
                            }
                            
                        } else {
                            setSignUpLoading(false);
                            Log.e(TAG, "Firebase account creation failed", task.getException());
                            
                            // Handle specific Firebase errors with security logging
                            String errorMessage = getFirebaseErrorMessage(task.getException());
                            showError(errorMessage);
                            
                            // Log security-relevant failures
                            if (task.getException() != null) {
                                Log.w(TAG, "Signup failure reason: " + task.getException().getMessage());
                            }
                        }
                    }
                });
    }

    /**
     * Send email verification to new user for security
     */
    private void sendEmailVerification(FirebaseUser user, String fullName, String email) {
        Log.d(TAG, "Sending email verification to: " + email);
        
        user.sendEmailVerification()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Email verification sent successfully");
                            
                            // Update user profile and create database entry
                            updateUserProfileAndCreateAccount(user, fullName);
                            
                        } else {
                            Log.w(TAG, "Failed to send email verification", task.getException());
                            
                            // Still proceed with account creation but warn user
                            updateUserProfileAndCreateAccount(user, fullName);
                            showError("Account created but email verification failed. Please verify your email manually.");
                        }
                    }
                });
    }

    /**
     * Comprehensive input validation with security checks
     */
    private boolean validateInputWithSecurityChecks(String fullName, String email, String password, String confirmPassword) {
        boolean isValid = true;

        // Validate Full Name
        if (TextUtils.isEmpty(fullName)) {
            tilFullName.setError(getString(R.string.error_name_required));
            etFullName.requestFocus();
            isValid = false;
        } else if (fullName.length() < 2) {
            tilFullName.setError("Name must be at least 2 characters long");
            etFullName.requestFocus();
            isValid = false;
        } else if (!fullName.matches("^[a-zA-Z\\s]+$")) {
            tilFullName.setError("Name can only contain letters and spaces");
            etFullName.requestFocus();
            isValid = false;
        }

        // Validate Email
        if (TextUtils.isEmpty(email)) {
            tilEmail.setError(getString(R.string.error_email_required));
            if (isValid) etEmail.requestFocus();
            isValid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError(getString(R.string.error_email_invalid));
            if (isValid) etEmail.requestFocus();
            isValid = false;
        }

        // Validate Password with strength requirements
        if (TextUtils.isEmpty(password)) {
            tilPassword.setError(getString(R.string.error_password_required));
            if (isValid) etPassword.requestFocus();
            isValid = false;
        } else if (password.length() < 8) {
            tilPassword.setError(getString(R.string.error_password_short));
            if (isValid) etPassword.requestFocus();
            isValid = false;
        } else if (!isPasswordStrong(password)) {
            tilPassword.setError("Password must contain at least one uppercase letter, one lowercase letter, and one number");
            if (isValid) etPassword.requestFocus();
            isValid = false;
        }

        // Validate Confirm Password
        if (TextUtils.isEmpty(confirmPassword)) {
            tilConfirmPassword.setError("Please confirm your password");
            if (isValid) etConfirmPassword.requestFocus();
            isValid = false;
        } else if (!password.equals(confirmPassword)) {
            tilConfirmPassword.setError(getString(R.string.error_password_mismatch));
            if (isValid) etConfirmPassword.requestFocus();
            isValid = false;
        }

        // Check Terms and Conditions
        if (!cbTerms.isChecked()) {
            showError(getString(R.string.error_terms_required));
            cbTerms.requestFocus();
            isValid = false;
        }

        return isValid;
    }

    /**
     * Enhanced password strength validation with comprehensive security checks
     * @param password The password to check
     * @return true if password meets all security requirements, false otherwise
     */
    private boolean isPasswordStrong(String password) {
        // Check minimum length
        if (password.length() < 8) {
            return false;
        }
        
        // Check for at least one uppercase letter
        if (!password.matches(".*[A-Z].*")) {
            return false;
        }
        
        // Check for at least one lowercase letter
        if (!password.matches(".*[a-z].*")) {
            return false;
        }
        
        // Check for at least one digit
        if (!password.matches(".*\\d.*")) {
            return false;
        }
        
        // Check for at least one special character (optional but recommended)
        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) {
            Log.d(TAG, "Password lacks special character (recommended but not required)");
        }
        
        // Check against common weak passwords
        if (isCommonPassword(password)) {
            return false;
        }
        
        return true;
    }

    /**
     * Check if password is in common weak passwords list
     */
    private boolean isCommonPassword(String password) {
        String[] commonPasswords = {
            "password", "123456", "123456789", "12345678", "12345", "1234567",
            "password123", "admin", "qwerty", "abc123", "letmein", "welcome",
            "monkey", "1234567890", "dragon", "123123", "football", "iloveyou"
        };
        
        String lowerPassword = password.toLowerCase();
        for (String common : commonPasswords) {
            if (lowerPassword.equals(common)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Perform additional security checks
     */
    private boolean performSecurityChecks(String email, String password) {
        // Check if password contains email parts
        if (password.toLowerCase().contains(email.split("@")[0].toLowerCase())) {
            showError("Password cannot contain parts of your email address");
            return false;
        }
        
        // Check for sequential characters
        if (hasSequentialCharacters(password)) {
            showError("Password cannot contain sequential characters (e.g., 1234, abcd)");
            return false;
        }
        
        // Check for repeated characters
        if (hasRepeatedCharacters(password)) {
            showError("Password cannot have more than 2 repeated characters in a row");
            return false;
        }
        
        // Validate email domain (basic check)
        if (!isValidEmailDomain(email)) {
            showError("Please use a valid email domain");
            return false;
        }
        
        return true;
    }

    /**
     * Check for sequential characters in password
     */
    private boolean hasSequentialCharacters(String password) {
        String sequences = "0123456789abcdefghijklmnopqrstuvwxyz";
        String lowerPassword = password.toLowerCase();
        
        // Only block sequences of 4+ consecutive characters (e.g. "abcd", "1234")
        for (int i = 0; i <= sequences.length() - 4; i++) {
            String sequence = sequences.substring(i, i + 4);
            if (lowerPassword.contains(sequence)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check for repeated characters in password
     */
    private boolean hasRepeatedCharacters(String password) {
        for (int i = 0; i < password.length() - 2; i++) {
            if (password.charAt(i) == password.charAt(i + 1) && 
                password.charAt(i + 1) == password.charAt(i + 2)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Basic email domain validation
     */
    private boolean isValidEmailDomain(String email) {
        String[] suspiciousDomains = {"tempmail", "10minutemail", "guerrillamail", "mailinator"};
        String domain = email.substring(email.indexOf("@") + 1).toLowerCase();
        
        for (String suspicious : suspiciousDomains) {
            if (domain.contains(suspicious)) {
                return false;
            }
        }
        return true;
    }

    private void clearErrors() {
        tilFullName.setError(null);
        tilEmail.setError(null);
        tilPassword.setError(null);
        tilConfirmPassword.setError(null);
    }

    private void updateUserProfileAndCreateAccount(FirebaseUser user, String displayName) {
        if (user == null) return;

        // Update Firebase Auth profile
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(displayName)
                .build();

        user.updateProfile(profileUpdates)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Firebase Auth profile updated successfully");
                            
                            // Create user profile in Firestore database
                            createUserInDatabase(user, displayName);
                            
                        } else {
                            Log.w(TAG, "Failed to update Firebase Auth profile", task.getException());
                            // Still try to create database entry
                            createUserInDatabase(user, displayName);
                        }
                    }
                });
    }
    
    /**
     * Create comprehensive user profile in Firebase database
     */
    private void createUserInDatabase(FirebaseUser user, String displayName) {
        Log.d(TAG, "Creating comprehensive user profile in Firebase database...");
        
        // Create User object with complete profile data
        com.example.healthscanner.models.User userProfile = new com.example.healthscanner.models.User();
        userProfile.setUid(user.getUid());
        userProfile.setEmail(user.getEmail());
        userProfile.setDisplayName(displayName);
        userProfile.setEmailVerified(user.isEmailVerified());
        userProfile.setCreatedAt(new java.util.Date());
        userProfile.setLastLoginAt(new java.util.Date());
        userProfile.setAuthProvider(user.getProviderData().isEmpty() ? "password" : user.getProviderData().get(0).getProviderId());
        
        // Initialize user statistics
        userProfile.setTotalScans(0);
        userProfile.setHealthyChoices(0);
        userProfile.setAverageHealthScore(0.0);
        
        // Initialize user preferences
        userProfile.setNotificationsEnabled(true);
        userProfile.setDarkModeEnabled(false);
        
        // Save to Firebase database
        com.example.healthscanner.database.FirebaseManager firebaseManager = 
            com.example.healthscanner.database.FirebaseManager.getInstance();
        
        firebaseManager.saveUserProfile(userProfile, new com.example.healthscanner.database.FirebaseManager.OperationCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "User profile created in Firebase database successfully");
                
                // Save comprehensive local user data
                saveComprehensiveUserData(user, displayName, userProfile);
                
                // Show success message with email verification reminder
                if (user.isEmailVerified()) {
                    showSuccess("Account created successfully! Please sign in.");
                } else {
                    showSuccess("Account created! Please check your email to verify your account, then sign in.");
                }
                
                // Navigate to login with success flag
                navigateToLoginWithSuccess(user.getEmail());
            }
            
            @Override
            public void onFailure(String error) {
                Log.e(TAG, "Failed to create user profile in Firebase database: " + error);
                
                // Retry once before giving up
                retryDatabaseCreation(userProfile, user, displayName, 1);
            }
        });
    }

    /**
     * Retry database creation with exponential backoff
     */
    private void retryDatabaseCreation(com.example.healthscanner.models.User userProfile, 
                                     FirebaseUser user, String displayName, int attempt) {
        if (attempt > 3) {
            Log.e(TAG, "Max retry attempts reached for database creation");
            
            // Still save local data and proceed (offline mode)
            saveComprehensiveUserData(user, displayName, userProfile);
            showSuccess("Account created! Database sync will retry automatically. Please sign in.");
            navigateToLoginWithSuccess(user.getEmail());
            return;
        }
        
        Log.d(TAG, "Retrying database creation, attempt: " + attempt);
        
        // Wait before retry (exponential backoff)
        new android.os.Handler().postDelayed(() -> {
            com.example.healthscanner.database.FirebaseManager firebaseManager = 
                com.example.healthscanner.database.FirebaseManager.getInstance();
            
            firebaseManager.saveUserProfile(userProfile, new com.example.healthscanner.database.FirebaseManager.OperationCallback() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "User profile created in Firebase database on retry " + attempt);
                    saveComprehensiveUserData(user, displayName, userProfile);
                    showSuccess("Account created successfully! Please sign in.");
                    navigateToLoginWithSuccess(user.getEmail());
                }
                
                @Override
                public void onFailure(String error) {
                    Log.e(TAG, "Retry " + attempt + " failed: " + error);
                    retryDatabaseCreation(userProfile, user, displayName, attempt + 1);
                }
            });
        }, attempt * 1000); // 1s, 2s, 3s delays
    }

    /**
     * Save comprehensive user data locally with security considerations
     */
    private void saveComprehensiveUserData(FirebaseUser user, String displayName, 
                                         com.example.healthscanner.models.User userProfile) {
        try {
            SharedPreferences.Editor editor = healthScannerPrefs.edit();

            // Save core user profile data
            editor.putString("current_user_name", displayName);
            editor.putString("current_user_email", user.getEmail());
            editor.putString("current_user_id", user.getUid());
            editor.putString("auth_provider", userProfile.getAuthProvider());
            
            // Save account status
            editor.putBoolean("email_verified", user.isEmailVerified());
            editor.putBoolean("is_logged_in", false); // Will be set to true on successful login
            editor.putLong("account_created_at", System.currentTimeMillis());
            editor.putLong("login_timestamp", 0); // Will be set on login
            
            // Initialize user statistics
            editor.putInt("total_scans", 0);
            editor.putInt("healthy_choices", 0);
            editor.putFloat("average_health_score", 0.0f);
            
            // Initialize user preferences
            editor.putBoolean("notifications_enabled", true);
            editor.putBoolean("dark_mode_enabled", false);
            
            // Initialize empty data arrays
            editor.putString("user_scan_history", "[]");
            editor.putString("user_saved_items", "[]");
            
            // Security metadata
            editor.putString("signup_method", "email_password");
            editor.putLong("last_security_check", System.currentTimeMillis());
            
            // Apply all changes atomically
            boolean success = editor.commit();
            
            if (success) {
                Log.d(TAG, "Comprehensive user data saved locally for: " + user.getEmail());
            } else {
                Log.e(TAG, "Failed to save user data locally");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error saving comprehensive user data", e);
        }
    }

    /**
     * Navigate to login with success message and email prefill
     */
    private void navigateToLoginWithSuccess(String email) {
        Intent intent = new Intent(SignUpActivity.this, LoginActivity.class);
        intent.putExtra("signup_success", true);
        intent.putExtra("prefill_email", email);
        intent.putExtra("show_verification_reminder", true);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * Legacy navigation method (kept for compatibility)
     */
    private void navigateToLogin() {
        navigateToLoginWithSuccess(etEmail.getText().toString().trim());
    }

    private void setSignUpLoading(boolean loading) {
        if (loading) {
            btnSignUp.setEnabled(false);
            progressSignup.setVisibility(View.VISIBLE);
        } else {
            btnSignUp.setEnabled(true);
            progressSignup.setVisibility(View.GONE);
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
        if (message.contains("ERROR_EMAIL_ALREADY_IN_USE") || message.contains("email address is already in use")) {
            return "This email is already registered. Please sign in instead.";
        } else if (message.contains("ERROR_WEAK_PASSWORD") || message.contains("password is invalid")) {
            return "Password should be at least 8 characters with uppercase, lowercase, and numbers.";
        } else if (message.contains("ERROR_INVALID_EMAIL") || message.contains("email address is badly formatted")) {
            return "Please enter a valid email address.";
        } else if (message.contains("ERROR_OPERATION_NOT_ALLOWED")) {
            return "Email/password accounts are not enabled. Please contact support.";
        } else if (message.contains("ERROR_NETWORK_REQUEST_FAILED") || message.contains("network error")) {
            return "Network error. Please check your internet connection.";
        } else if (message.contains("ERROR_TOO_MANY_REQUESTS")) {
            return "Too many requests. Please try again later.";
        } else {
            return "Account creation failed. Please try again.";
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        // Navigate back to login screen
        Intent intent = new Intent(SignUpActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}