package com.example.healthscanner;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.example.healthscanner.database.FirebaseManager;

import java.util.concurrent.TimeUnit;

/**
 * OTP-based phone login screen.
 *
 * <p>Flow:
 * <ol>
 *   <li>User enters phone number (with country code, e.g. +919876543210)</li>
 *   <li>Firebase sends an SMS with a 6-digit OTP</li>
 *   <li>User enters the OTP (or it auto-verifies on the same device)</li>
 *   <li>On success → save session → navigate to MainActivity</li>
 * </ol>
 */
public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private static final long OTP_TIMEOUT_SECONDS = 60L;
    private static final long RESEND_COOLDOWN_MS = 60_000L;

    // ── Phone section ──
    private View phoneSection;
    private TextInputLayout tilPhone;
    private TextInputEditText etPhone;
    private View btnSendOtp;
    private CircularProgressIndicator progressPhone;

    // ── OTP section ──
    private View otpSection;
    private TextView tvOtpSubtitle;
    private TextInputLayout tilOtp;
    private TextInputEditText etOtp;
    private View btnVerifyOtp;
    private CircularProgressIndicator progressOtp;
    private TextView tvResendTimer;
    private TextView btnResendOtp;
    private TextView btnChangeNumber;

    // ── Firebase ──
    private FirebaseAuth mAuth;
    private FirebaseManager firebaseManager;

    // ── State ──
    private String verificationId;
    private PhoneAuthProvider.ForceResendingToken resendToken;
    private SharedPreferences healthScannerPrefs;
    private CountDownTimer resendCountdown;
    private String pendingPhoneNumber;

    // ── Phone auth callbacks ──
    private final PhoneAuthProvider.OnVerificationStateChangedCallbacks phoneAuthCallbacks =
            new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                @Override
                public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                    // Auto-verification (e.g. instant verification or auto-retrieval).
                    Log.d(TAG, "Phone auto-verified, signing in automatically");
                    signInWithPhoneCredential(credential);
                }

                @Override
                public void onVerificationFailed(@NonNull FirebaseException e) {
                    Log.e(TAG, "Phone verification failed", e);
                    setPhoneLoading(false);
                    showError(getFriendlyPhoneError(e));
                }

                @Override
                public void onCodeSent(@NonNull String id,
                                       @NonNull PhoneAuthProvider.ForceResendingToken token) {
                    Log.d(TAG, "OTP code sent successfully");
                    verificationId = id;
                    resendToken = token;
                    setPhoneLoading(false);
                    showOtpSection();
                    startResendCountdown();
                }
            };

    // ─────────────────────────────────────────────────────────────────────
    //  Lifecycle
    // ─────────────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_otp);
        SystemBarInsets.applyTopInset(this);

        mAuth = FirebaseAuth.getInstance();
        firebaseManager = FirebaseManager.getInstance();
        healthScannerPrefs = getSharedPreferences("HealthScannerPrefs", MODE_PRIVATE);

        // Already logged in?
        if (isUserAlreadyLoggedIn()) {
            Log.d(TAG, "User already logged in, navigating to main");
            navigateToMain();
            return;
        }

        initializeViews();
        setupClickListeners();
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null && isSessionValid()) {
            navigateToMain();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (resendCountdown != null) {
            resendCountdown.cancel();
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  View init
    // ─────────────────────────────────────────────────────────────────────

    private void initializeViews() {
        // Phone section
        phoneSection   = findViewById(R.id.phone_section);
        tilPhone       = findViewById(R.id.til_phone);
        etPhone        = findViewById(R.id.et_phone);
        btnSendOtp     = findViewById(R.id.btn_send_otp);
        progressPhone  = findViewById(R.id.progress_phone);

        // OTP section
        otpSection     = findViewById(R.id.otp_section);
        tvOtpSubtitle  = findViewById(R.id.tv_otp_subtitle);
        tilOtp         = findViewById(R.id.til_otp);
        etOtp          = findViewById(R.id.et_otp);
        btnVerifyOtp   = findViewById(R.id.btn_verify_otp);
        progressOtp    = findViewById(R.id.progress_otp);
        tvResendTimer  = findViewById(R.id.tv_resend_timer);
        btnResendOtp   = findViewById(R.id.btn_resend_otp);
        btnChangeNumber = findViewById(R.id.btn_change_number);
    }

    private void setupClickListeners() {
        btnSendOtp.setOnClickListener(v -> handleSendOtp());
        btnVerifyOtp.setOnClickListener(v -> handleVerifyOtp());
        btnResendOtp.setOnClickListener(v -> handleResendOtp());
        btnChangeNumber.setOnClickListener(v -> showPhoneSection());
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Phone → OTP toggle
    // ─────────────────────────────────────────────────────────────────────

    private void showOtpSection() {
        phoneSection.setVisibility(View.GONE);
        otpSection.setVisibility(View.VISIBLE);
        tvOtpSubtitle.setText("Enter the 6-digit code sent to " + pendingPhoneNumber);
        etOtp.setText("");
        etOtp.requestFocus();
    }

    private void showPhoneSection() {
        otpSection.setVisibility(View.GONE);
        phoneSection.setVisibility(View.VISIBLE);
        if (resendCountdown != null) resendCountdown.cancel();
        etPhone.requestFocus();
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Send OTP
    // ─────────────────────────────────────────────────────────────────────

    private void handleSendOtp() {
        tilPhone.setError(null);
        String phone = etPhone.getText().toString().trim();

        if (TextUtils.isEmpty(phone)) {
            tilPhone.setError("Phone number is required");
            return;
        }

        // Auto-prepend + if missing
        if (!phone.startsWith("+")) {
            phone = "+91" + phone;   // default to India; adjust for your needs
            etPhone.setText(phone);
        }

        if (phone.length() < 10) {
            tilPhone.setError("Enter a valid phone number with country code");
            return;
        }

        pendingPhoneNumber = phone;
        setPhoneLoading(true);
        sendVerificationCode(phone);
    }

    private void sendVerificationCode(String phoneNumber) {
        Log.d(TAG, "Sending OTP to " + phoneNumber);

        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(OTP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(phoneAuthCallbacks)
                .build();

        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Verify OTP
    // ─────────────────────────────────────────────────────────────────────

    private void handleVerifyOtp() {
        tilOtp.setError(null);
        String code = etOtp.getText().toString().trim();

        if (TextUtils.isEmpty(code)) {
            tilOtp.setError("Please enter the OTP code");
            return;
        }
        if (code.length() != 6) {
            tilOtp.setError("OTP must be 6 digits");
            return;
        }
        if (verificationId == null) {
            showError("Verification session expired. Please request a new OTP.");
            showPhoneSection();
            return;
        }

        setOtpLoading(true);
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
        signInWithPhoneCredential(credential);
    }

    private void signInWithPhoneCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    setOtpLoading(false);

                    if (task.isSuccessful()) {
                        Log.d(TAG, "Phone authentication successful");
                        FirebaseUser user = mAuth.getCurrentUser();

                        if (user != null) {
                            saveLoginState(user);
                            createOrUpdateFirebaseProfile(user);
                            showSuccess("Welcome!");
                            navigateToMain();
                        }
                    } else {
                        Log.e(TAG, "Phone sign-in failed", task.getException());
                        String msg = task.getException() != null
                                ? task.getException().getMessage()
                                : "Verification failed";
                        if (msg != null && msg.toLowerCase().contains("invalid")) {
                            tilOtp.setError("Invalid OTP code. Please try again.");
                        } else {
                            showError("Verification failed: " + msg);
                        }
                    }
                });
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Resend OTP
    // ─────────────────────────────────────────────────────────────────────

    private void handleResendOtp() {
        if (pendingPhoneNumber == null || resendToken == null) {
            showError("Please go back and re-enter your phone number.");
            showPhoneSection();
            return;
        }

        btnResendOtp.setVisibility(View.GONE);
        tvResendTimer.setVisibility(View.VISIBLE);

        Log.d(TAG, "Resending OTP to " + pendingPhoneNumber);

        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
                .setPhoneNumber(pendingPhoneNumber)
                .setTimeout(OTP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(phoneAuthCallbacks)
                .setForceResendingToken(resendToken)
                .build();

        PhoneAuthProvider.verifyPhoneNumber(options);
        startResendCountdown();
    }

    private void startResendCountdown() {
        btnResendOtp.setVisibility(View.GONE);
        tvResendTimer.setVisibility(View.VISIBLE);

        if (resendCountdown != null) resendCountdown.cancel();

        resendCountdown = new CountDownTimer(RESEND_COOLDOWN_MS, 1_000) {
            @Override
            public void onTick(long millisUntilFinished) {
                tvResendTimer.setText("Resend in " + (millisUntilFinished / 1000) + "s");
            }

            @Override
            public void onFinish() {
                tvResendTimer.setVisibility(View.GONE);
                btnResendOtp.setVisibility(View.VISIBLE);
            }
        }.start();
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Session persistence
    // ─────────────────────────────────────────────────────────────────────

    private void saveLoginState(FirebaseUser user) {
        SharedPreferences.Editor editor = healthScannerPrefs.edit();

        editor.putBoolean("is_logged_in", true);
        editor.putLong("login_timestamp", System.currentTimeMillis());
        editor.putString("current_user_id", user.getUid());
        editor.putString("current_user_phone", user.getPhoneNumber());
        editor.putString("auth_provider", "phone");

        // Use phone number as display name fallback
        String displayName = user.getDisplayName();
        if (displayName == null || displayName.isEmpty()) {
            displayName = user.getPhoneNumber();
        }
        editor.putString("current_user_name", displayName);

        // Photo URL
        if (user.getPhotoUrl() != null) {
            editor.putString("current_user_photo", user.getPhotoUrl().toString());
        }

        // For backward compat — some screens read "current_user_email"
        editor.putString("current_user_email", user.getPhoneNumber());

        editor.apply();
        Log.d(TAG, "Login state saved for: " + user.getPhoneNumber());
    }

    private void createOrUpdateFirebaseProfile(FirebaseUser user) {
        try {
            firebaseManager.checkUserExists(user.getUid(), exists -> {
                if (exists) {
                    firebaseManager.updateLastLogin(user.getUid(),
                            new FirebaseManager.OperationCallback() {
                                @Override public void onSuccess() {
                                    Log.d(TAG, "Last login updated in Firestore");
                                }
                                @Override public void onFailure(String error) {
                                    Log.w(TAG, "Failed to update last login: " + error);
                                }
                            });
                } else {
                    firebaseManager.createUserProfile(user,
                            new FirebaseManager.OperationCallback() {
                                @Override public void onSuccess() {
                                    Log.d(TAG, "New user profile created in Firestore");
                                }
                                @Override public void onFailure(String error) {
                                    Log.w(TAG, "Failed to create profile: " + error);
                                }
                            });
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error creating/updating Firebase profile", e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Session checks
    // ─────────────────────────────────────────────────────────────────────

    private boolean isUserAlreadyLoggedIn() {
        boolean isLoggedIn = healthScannerPrefs.getBoolean("is_logged_in", false);
        if (!isLoggedIn) return false;

        if (!isSessionValid()) {
            clearLoginState();
            return false;
        }

        String userId = healthScannerPrefs.getString("current_user_id", "");
        return !userId.isEmpty();
    }

    private boolean isSessionValid() {
        long loginTime = healthScannerPrefs.getLong("login_timestamp", 0);
        long thirtyDays = 30L * 24 * 60 * 60 * 1000;
        return (System.currentTimeMillis() - loginTime) < thirtyDays;
    }

    private void clearLoginState() {
        SharedPreferences.Editor editor = healthScannerPrefs.edit();
        editor.putBoolean("is_logged_in", false);
        editor.remove("login_timestamp");
        editor.remove("current_user_email");
        editor.remove("current_user_name");
        editor.remove("current_user_id");
        editor.remove("current_user_phone");
        editor.remove("auth_provider");
        editor.remove("current_user_photo");
        editor.apply();
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Navigation
    // ─────────────────────────────────────────────────────────────────────

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra("login_success", true);
        intent.putExtra("show_welcome", true);
        startActivity(intent);
        finish();
    }

    /**
     * Public sign-out helper — called from ProfileActivity / SettingsActivity.
     */
    public static void performSignOut(AppCompatActivity activity) {
        FirebaseAuth.getInstance().signOut();

        SharedPreferences prefs = activity.getSharedPreferences("HealthScannerPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("is_logged_in", false);
        editor.remove("login_timestamp");
        editor.remove("current_user_email");
        editor.remove("current_user_name");
        editor.remove("current_user_id");
        editor.remove("current_user_phone");
        editor.remove("auth_provider");
        editor.remove("current_user_photo");
        editor.apply();

        Intent loginIntent = new Intent(activity, LoginActivity.class);
        loginIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(loginIntent);
        activity.finish();

        Log.d("LoginActivity", "User signed out successfully");
    }

    // ─────────────────────────────────────────────────────────────────────
    //  UI helpers
    // ─────────────────────────────────────────────────────────────────────

    private void setPhoneLoading(boolean loading) {
        btnSendOtp.setAlpha(loading ? 0.6f : 1.0f);
        btnSendOtp.setClickable(!loading);
        progressPhone.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private void setOtpLoading(boolean loading) {
        btnVerifyOtp.setAlpha(loading ? 0.6f : 1.0f);
        btnVerifyOtp.setClickable(!loading);
        progressOtp.setVisibility(loading ? View.VISIBLE : View.GONE);
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

    private String getFriendlyPhoneError(FirebaseException e) {
        String msg = e.getMessage();
        if (msg == null) return "Phone verification failed. Please try again.";

        String lower = msg.toLowerCase();
        if (lower.contains("invalid format") || lower.contains("invalid phone")) {
            return "Invalid phone number format. Use country code (e.g. +919876543210).";
        } else if (lower.contains("too many requests") || lower.contains("quota")) {
            return "Too many OTP requests. Please try again later.";
        } else if (lower.contains("network")) {
            return "Network error. Please check your internet connection.";
        } else if (lower.contains("app not authorized") || lower.contains("not authorized")) {
            return "This app is not authorized for phone auth. Check Firebase Console settings.";
        } else if (lower.contains("missing client identifier") || lower.contains("recaptcha")) {
            return "Phone verification setup incomplete. Please add SHA-256 to Firebase Console.";
        } else {
            return "Phone verification failed: " + msg;
        }
    }

    @Override
    public void onBackPressed() {
        if (otpSection.getVisibility() == View.VISIBLE) {
            showPhoneSection();
        } else {
            super.onBackPressed();
            finishAffinity();
        }
    }
}
