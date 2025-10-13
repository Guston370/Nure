package com.example.healthscanner;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;

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
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

public class SignUpActivity extends AppCompatActivity {

    private static final String TAG = "SignUpActivity";

    // UI Elements
    private TextInputLayout tilFullName, tilEmail, tilPassword, tilConfirmPassword;
    private TextInputEditText etFullName, etEmail, etPassword, etConfirmPassword;
    private MaterialCheckBox cbTerms;
    private MaterialButton btnSignUp, btnSignIn;
    private CircularProgressIndicator progressSignup;

    // Firebase
    private FirebaseAuth mAuth;

    // SharedPreferences
    private SharedPreferences healthScannerPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply theme before calling super.onCreate()

        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        
        // Initialize SharedPreferences
        healthScannerPrefs = getSharedPreferences("HealthScannerPrefs", MODE_PRIVATE);

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
        progressSignup = findViewById(R.id.progress_signup);
    }

    private void setupClickListeners() {
        btnSignUp.setOnClickListener(v -> handleSignUp());

        btnSignIn.setOnClickListener(v -> {
            Intent intent = new Intent(SignUpActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void handleSignUp() {
        // Clear previous errors
        clearErrors();

        // Get user input
        String fullName = etFullName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        // Validate input
        if (!validateInput(fullName, email, password, confirmPassword)) {
            return;
        }

        // Show loading state
        setSignUpLoading(true);

        // Create account with Firebase
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        setSignUpLoading(false);
                        
                        if (task.isSuccessful()) {
                            Log.d(TAG, "createUserWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            
                            // Update user profile with display name
                            updateUserProfile(user, fullName);
                            
                        } else {
                            Log.w(TAG, "createUserWithEmail:failure", task.getException());
                            String errorMessage = task.getException() != null ? 
                                task.getException().getMessage() : "Account creation failed. Please try again.";
                            showError(errorMessage);
                            
                            // If email already exists, suggest signing in instead
                            if (task.getException() != null && 
                                task.getException().getMessage() != null &&
                                task.getException().getMessage().contains("email address is already in use")) {
                                showError("This email is already registered. Please sign in instead.");
                            }
                        }
                    }
                });
    }

    private boolean validateInput(String fullName, String email, String password, String confirmPassword) {
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
     * Check if password meets strength requirements
     * @param password The password to check
     * @return true if password is strong, false otherwise
     */
    private boolean isPasswordStrong(String password) {
        // Check for at least one uppercase letter, one lowercase letter, and one digit
        return password.matches(".*[A-Z].*") && 
               password.matches(".*[a-z].*") && 
               password.matches(".*\\d.*");
    }

    private void clearErrors() {
        tilFullName.setError(null);
        tilEmail.setError(null);
        tilPassword.setError(null);
        tilConfirmPassword.setError(null);
    }

    private void updateUserProfile(FirebaseUser user, String displayName) {
        if (user == null) return;

        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(displayName)
                .build();

        user.updateProfile(profileUpdates)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "User profile updated.");
                            
                            // Save additional user data
                            saveUserData(user, displayName);
                            
                            // Show success message
                            showSuccess(getString(R.string.success_account_created));
                            
                            // Navigate to login
                            navigateToLogin();
                        } else {
                            Log.w(TAG, "Failed to update user profile", task.getException());
                            // Still navigate to login even if profile update fails
                            navigateToLogin();
                        }
                    }
                });
    }

    private void saveUserData(FirebaseUser user, String displayName) {
        SharedPreferences.Editor editor = healthScannerPrefs.edit();

        // Save user profile data
        editor.putString("user_name", displayName);
        editor.putString("user_email", user.getEmail());
        editor.putString("user_id", user.getUid());

        // Initialize user stats
        editor.putInt("scan_count", 0);
        editor.putLong("join_date", System.currentTimeMillis());

        editor.apply();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(SignUpActivity.this, LoginActivity.class);
        intent.putExtra("signup_success", true);
        intent.putExtra("prefill_email", etEmail.getText().toString().trim());
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
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