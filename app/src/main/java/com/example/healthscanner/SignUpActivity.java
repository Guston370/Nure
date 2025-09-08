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

public class SignUpActivity extends AppCompatActivity {

    // UI Elements
    private TextInputLayout tilFullName, tilEmail, tilPhone, tilPassword, tilConfirmPassword;
    private TextInputEditText etFullName, etEmail, etPhone, etPassword, etConfirmPassword;
    private CheckBox cbTerms;
    private Button btnSignUp;
    private TextView tvLogin;

    private SharedPreferences healthScannerPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply theme before calling super.onCreate()
        ThemeHelper.applyTheme(this);
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        healthScannerPrefs = getSharedPreferences("HealthScannerPrefs", MODE_PRIVATE);

        initializeViews();
        setupClickListeners();
    }

    private void initializeViews() {
        // TextInputLayouts
        tilFullName = findViewById(R.id.til_full_name);
        tilEmail = findViewById(R.id.til_email);
        tilPhone = findViewById(R.id.til_phone);
        tilPassword = findViewById(R.id.til_password);
        tilConfirmPassword = findViewById(R.id.til_confirm_password);

        // EditTexts
        etFullName = findViewById(R.id.et_full_name);
        etEmail = findViewById(R.id.et_email);
        etPhone = findViewById(R.id.et_phone);
        etPassword = findViewById(R.id.et_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);

        // Other UI elements
        cbTerms = findViewById(R.id.cb_terms);
        btnSignUp = findViewById(R.id.btn_signup);
        tvLogin = findViewById(R.id.tv_login);
    }

    private void setupClickListeners() {
        btnSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleSignUp();
            }
        });

        tvLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate back to Login Activity
                Intent intent = new Intent(SignUpActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    private void handleSignUp() {
        // Clear previous errors
        clearErrors();

        // Get user input
        String fullName = etFullName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        // Validate input
        if (!validateInput(fullName, email, phone, password, confirmPassword)) {
            return;
        }

        // Create User object with the collected data
        User newUser = new User(fullName, email, phone, password);

        // Process the signup
        processSignUp(newUser);
    }

    private boolean validateInput(String fullName, String email, String phone, String password, String confirmPassword) {
        boolean isValid = true;

        // Validate Full Name
        if (TextUtils.isEmpty(fullName)) {
            tilFullName.setError("Full name is required");
            isValid = false;
        } else if (fullName.length() < 2) {
            tilFullName.setError("Name must be at least 2 characters long");
            isValid = false;
        }

        // Validate Email
        if (TextUtils.isEmpty(email)) {
            tilEmail.setError("Email is required");
            isValid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Please enter a valid email address");
            isValid = false;
        } else if (isEmailAlreadyRegistered(email)) {
            tilEmail.setError("This email is already registered. Please sign in instead.");
            isValid = false;
        }

        // Validate Phone Number
        if (TextUtils.isEmpty(phone)) {
            tilPhone.setError("Phone number is required");
            isValid = false;
        } else if (!Patterns.PHONE.matcher(phone).matches() || phone.length() < 10) {
            tilPhone.setError("Please enter a valid phone number");
            isValid = false;
        }

        // Validate Password
        if (TextUtils.isEmpty(password)) {
            tilPassword.setError("Password is required");
            isValid = false;
        } else if (password.length() < 6) {
            tilPassword.setError("Password must be at least 6 characters long");
            isValid = false;
        } else if (!isPasswordStrong(password)) {
            tilPassword.setError("Password must contain at least one letter and one number");
            isValid = false;
        }

        // Validate Confirm Password
        if (TextUtils.isEmpty(confirmPassword)) {
            tilConfirmPassword.setError("Please confirm your password");
            isValid = false;
        } else if (!password.equals(confirmPassword)) {
            tilConfirmPassword.setError("Passwords do not match");
            isValid = false;
        }

        // Check Terms and Conditions
        if (!cbTerms.isChecked()) {
            isValid = false;
        }

        return isValid;
    }

    private boolean isPasswordStrong(String password) {
        // Check if password contains at least one letter and one number
        return password.matches(".*[A-Za-z].*") && password.matches(".*\\d.*");
    }

    private boolean isEmailAlreadyRegistered(String email) {
        // Check if email already exists in SharedPreferences
        String storedEmail = healthScannerPrefs.getString("user_email", "");
        return email.equals(storedEmail);
    }

    private void clearErrors() {
        tilFullName.setError(null);
        tilEmail.setError(null);
        tilPhone.setError(null);
        tilPassword.setError(null);
        tilConfirmPassword.setError(null);
    }

    private void processSignUp(User user) {
        // Show loading state
        btnSignUp.setText("Creating Account...");
        btnSignUp.setEnabled(false);

        // Simulate account creation process
        btnSignUp.postDelayed(new Runnable() {
            @Override
            public void run() {
                // Reset button state
                btnSignUp.setText("Create Account");
                btnSignUp.setEnabled(true);

                // Save user data to SharedPreferences
                saveUserData(user);


                // Navigate to login page instead of main activity
                navigateToLogin(user);
            }
        }, 2000); // 2 second delay to simulate processing
    }

    private void saveUserData(User user) {
        SharedPreferences.Editor editor = healthScannerPrefs.edit();

        // Save user profile data
        editor.putString("user_name", user.getFullName());
        editor.putString("user_email", user.getEmail());
        editor.putString("user_phone", user.getPhone());
        editor.putString("user_password", user.getPassword()); // In real app, hash this!

        // Don't auto-login after signup - let user sign in manually
        editor.putBoolean("is_logged_in", false);

        // Initialize user stats
        editor.putInt("scan_count", 0);
        editor.putLong("join_date", System.currentTimeMillis());

        editor.apply();
    }

    private void navigateToLogin(User user) {
        Intent intent = new Intent(SignUpActivity.this, LoginActivity.class);
        // Pass the email to pre-fill login form
        intent.putExtra("prefill_email", user.getEmail());
        intent.putExtra("signup_success", true);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        // Navigate back to login screen
        Intent intent = new Intent(SignUpActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    // User data model class for HealthScanner
    public static class User {
        private String fullName;
        private String email;
        private String phone;
        private String password;

        public User(String fullName, String email, String phone, String password) {
            this.fullName = fullName;
            this.email = email;
            this.phone = phone;
            this.password = password;
        }

        // Getters
        public String getFullName() { return fullName; }
        public String getEmail() { return email; }
        public String getPhone() { return phone; }
        public String getPassword() { return password; }

        // Setters
        public void setFullName(String fullName) { this.fullName = fullName; }
        public void setEmail(String email) { this.email = email; }
        public void setPhone(String phone) { this.phone = phone; }
        public void setPassword(String password) { this.password = password; }
    }
}