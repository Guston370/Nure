package com.example.healthscanner;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class LoginActivity extends AppCompatActivity {

    // UI Elements
    private TextInputLayout tilEmail, tilPassword;
    private TextInputEditText etEmail, etPassword;
    private CheckBox cbRememberMe;
    private Button btnLogin, btnGoogleSignIn;
    private TextView tvForgotPassword, tvSignUp;

    private SharedPreferences healthScannerPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply theme before calling super.onCreate()
        ThemeHelper.applyTheme(this);
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        healthScannerPrefs = getSharedPreferences("HealthScannerPrefs", MODE_PRIVATE);

        // Check if already logged in
        if (healthScannerPrefs.getBoolean("is_logged_in", false)) {
            navigateToMain();
            return;
        }

        initializeViews();
        setupClickListeners();
    }

    private void initializeViews() {
        // TextInputLayouts
        tilEmail = findViewById(R.id.til_email);
        tilPassword = findViewById(R.id.til_password);

        // EditTexts
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);

        // Other UI elements
        cbRememberMe = findViewById(R.id.cb_remember_me);
        btnLogin = findViewById(R.id.btn_login);
        btnGoogleSignIn = findViewById(R.id.btn_google_signin);
        tvForgotPassword = findViewById(R.id.tv_forgot_password);
        tvSignUp = findViewById(R.id.tv_signup);
    }

    private void setupClickListeners() {
        // Login button click
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleLogin();
            }
        });

        // Sign Up button click - Navigate to SignUpActivity
        tvSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
                startActivity(intent);
            }
        });

        // Forgot Password click
        tvForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleForgotPassword();
            }
        });

        // Google Sign In click
        btnGoogleSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleGoogleSignIn();
            }
        });
    }

    private void handleLogin() {
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

        // Process login
        processLogin(email, password);
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

    private void processLogin(String email, String password) {
        // Show loading state
        btnLogin.setText("Signing In...");
        btnLogin.setEnabled(false);

        // Simulate login process
        btnLogin.postDelayed(new Runnable() {
            @Override
            public void run() {
                // Reset button state
                btnLogin.setText("Sign In");
                btnLogin.setEnabled(true);

                // Check credentials (in real app, verify with server)
                if (isValidCredentials(email, password)) {
                    // Save login state
                    saveLoginState(email);


                    // Navigate to main activity
                    navigateToMain();
                } else {
                    tilPassword.setError("Invalid credentials");
                }
            }
        }, 1500);
    }

    private boolean isValidCredentials(String email, String password) {
        // Check against stored user data or allow demo login
        String storedEmail = healthScannerPrefs.getString("user_email", "");
        String storedPassword = healthScannerPrefs.getString("user_password", "");

        // If user exists in SharedPreferences, verify credentials
        if (!storedEmail.isEmpty() && !storedPassword.isEmpty()) {
            return email.equals(storedEmail) && password.equals(storedPassword);
        }

        // Demo login - accept any valid email with password >= 6 characters
        return Patterns.EMAIL_ADDRESS.matcher(email).matches() && password.length() >= 6;
    }

    private void saveLoginState(String email) {
        SharedPreferences.Editor editor = healthScannerPrefs.edit();
        editor.putBoolean("is_logged_in", true);
        editor.putString("current_user_email", email);

        // Save remember me preference
        if (cbRememberMe.isChecked()) {
            editor.putString("saved_email", email);
            editor.putBoolean("remember_me", true);
        }

        editor.apply();
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

    }

    private void handleGoogleSignIn() {
        // Show loading state
        btnGoogleSignIn.setText("Signing in with Google...");
        btnGoogleSignIn.setEnabled(false);

        // Here you would integrate with Google Sign-In API
        // For demo purposes, we'll simulate the process
        btnGoogleSignIn.postDelayed(new Runnable() {
            @Override
            public void run() {
                btnGoogleSignIn.setText("Continue with Google");
                btnGoogleSignIn.setEnabled(true);

                // Simulate successful Google login
                saveLoginState("user@gmail.com");
                navigateToMain();
            }
        }, 1500);
    }

    private void navigateToMain() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Check if user is remembered and auto-fill email
        String savedEmail = healthScannerPrefs.getString("saved_email", "");
        boolean rememberMe = healthScannerPrefs.getBoolean("remember_me", false);

        if (rememberMe && !TextUtils.isEmpty(savedEmail)) {
            etEmail.setText(savedEmail);
            cbRememberMe.setChecked(true);
        }
    }

    @Override
    public void onBackPressed() {
        // Exit app when back is pressed on login screen
        super.onBackPressed();
        finishAffinity();
    }
}