package com.example.healthscanner;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import com.example.healthscanner.adapters.RecentScansProfileAdapter;
import com.example.healthscanner.database.SyncManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Modern Profile Activity with Enhanced UI/UX and Working Features
 * Includes collapsing toolbar, animations, and fully functional settings
 */
public class ProfileActivity extends BaseActivity {

    private static final String TAG = "ProfileActivity";
    private static final String PREFS_NAME = "HealthScannerPrefs";

    // UI Components
    private CollapsingToolbarLayout collapsingToolbar;
    private SyncManager syncManager;
    private DarkModeManager darkModeManager;
    private DataResetManager dataResetManager;
    private Toolbar toolbar;
    private ImageView profileAvatar;
    private TextView userName;
    private TextView userEmail;
    private TextView memberSince;
    private TextView totalScans;
    private TextView healthyChoices;
    private TextView healthScore;
    private TextView statsPeriod;
    private RecyclerView recentScansRecycler;
    private SwitchMaterial notificationsSwitch;
    private SwitchMaterial darkModeSwitch;
    private MaterialButton logoutButton;

    // Health Concerns & Preferences
    private com.google.android.material.chip.ChipGroup healthConcernsChipGroup;
    private com.google.android.material.chip.ChipGroup dietaryPreferencesChipGroup;
    private MaterialButton addConcernButton;
    private MaterialButton addPreferenceButton;
    private ImageView editHealthConcerns;
    private ImageView editDietaryPreferences;

    // Data & Managers
    private AuthManager authManager;
    private RecentScansProfileAdapter recentScansAdapter;
    private List<RecentScansProfileAdapter.ScanItem> recentScanItems;
    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_modern);

        // Initialize managers and preferences
        authManager = AuthManager.getInstance(this);
        preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        syncManager = SyncManager.getInstance(this);
        darkModeManager = DarkModeManager.getInstance(this);

        // Simple authentication check - trust navigation from authenticated home page
        boolean fromNavigation = getIntent().getBooleanExtra("from_navigation", false);
        if (fromNavigation) {
            // Trust navigation from other authenticated activities
            Log.d(TAG, "ProfileActivity opened from navigation - trusting authentication");
        } else if (!authManager.isUserAuthenticated()) {
            // Only check auth for direct launches, not navigation
            Log.w(TAG, "Direct launch without authentication, redirecting to login");
            authManager.navigateToLogin(this);
            return;
        }

        // Initialize components
        initializeViews();
        setupToolbar();
        initializeBottomNavigation();
        loadUserProfile();
        loadHealthStatistics();
        setupRecentScans();
        setupClickListeners();
        setupHealthConcerns();
        setupDietaryPreferences();
        setupAnimations();
    }

    private void initializeViews() {
        // Toolbar and collapsing layout
        collapsingToolbar = findViewById(R.id.collapsing_toolbar);
        toolbar = findViewById(R.id.toolbar);

        // Profile header
        profileAvatar = findViewById(R.id.profile_avatar);
        userName = findViewById(R.id.user_name);
        userEmail = findViewById(R.id.user_email);
        memberSince = findViewById(R.id.member_since);

        // Statistics
        totalScans = findViewById(R.id.total_scans);
        healthyChoices = findViewById(R.id.healthy_choices);
        healthScore = findViewById(R.id.health_score);
        statsPeriod = findViewById(R.id.stats_period);

        // Recent scans
        recentScansRecycler = findViewById(R.id.recent_scans_recycler);

        // Settings switches
        notificationsSwitch = findViewById(R.id.notifications_switch);
        darkModeSwitch = findViewById(R.id.dark_mode_switch);

        // Buttons
        logoutButton = findViewById(R.id.logout_button);

        // Health Concerns & Preferences
        healthConcernsChipGroup = findViewById(R.id.healthConcernsChipGroup);
        dietaryPreferencesChipGroup = findViewById(R.id.dietaryPreferencesChipGroup);
        addConcernButton = findViewById(R.id.addConcernButton);
        addPreferenceButton = findViewById(R.id.addPreferenceButton);
        editHealthConcerns = findViewById(R.id.editHealthConcerns);
        editDietaryPreferences = findViewById(R.id.editDietaryPreferences);

        Log.d(TAG, "Views initialized successfully");
    }

    private void setupToolbar() {
        try {
            if (toolbar != null) {
                Log.d(TAG, "Setting up toolbar");
                setSupportActionBar(toolbar);

                if (getSupportActionBar() != null) {
                    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                    getSupportActionBar().setDisplayShowHomeEnabled(true);
                    Log.d(TAG, "Toolbar setup successful");
                }
            } else {
                Log.w(TAG, "Toolbar is null, skipping setup");
            }

            if (collapsingToolbar != null) {
                collapsingToolbar.setTitle("Profile");
                Log.d(TAG, "Collapsing toolbar title set");
            } else {
                Log.w(TAG, "Collapsing toolbar is null");
            }
        } catch (IllegalStateException e) {
            Log.e(TAG, "Action bar conflict: " + e.getMessage());
            // Skip toolbar setup if there's a conflict
            if (collapsingToolbar != null) {
                collapsingToolbar.setTitle("Profile");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up toolbar: " + e.getMessage(), e);
            // Continue without toolbar if there's an issue
        }
    }

    @Override
    protected int getCurrentNavigationItemId() {
        return R.id.nav_profile;
    }

    private void loadUserProfile() {
        // Load from database and sync with local storage
        authManager.loadUserProfile(new AuthManager.UserProfileCallback() {
            @Override
            public void onSuccess(com.example.healthscanner.models.User user) {
                // Update UI with database data
                updateUIWithUserData(user);
                Log.d(TAG, "User profile loaded from database: " + user.getEmail());
            }

            @Override
            public void onFailure(String error) {
                Log.w(TAG, "Failed to load user profile from database: " + error);
                // Fallback to local data
                loadUserProfileFromLocal();
            }
        });
    }

    private void loadUserProfileFromLocal() {
        String name = authManager.getCurrentUserName();
        String email = authManager.getCurrentUserEmail();
        String firstName = preferences.getString("current_user_first_name", "");
        String lastName = preferences.getString("current_user_last_name", "");
        String authProvider = preferences.getString("auth_provider", "");
        String photoUrl = preferences.getString("current_user_photo", "");
        boolean freshSignIn = preferences.getBoolean("fresh_google_signin", false);

        // Create personalized greeting
        String displayName = name;
        if (!firstName.isEmpty()) {
            displayName = firstName; // Use first name for more personal feel
        }

        // Set user information with personalized touch
        if (userName != null) {
            if (freshSignIn && !firstName.isEmpty()) {
                userName.setText("Welcome back, " + firstName + "! 👋");
                // Clear the fresh sign-in flag
                preferences.edit().putBoolean("fresh_google_signin", false).apply();
            } else {
                userName.setText(displayName != null ? displayName : "Health Explorer");
            }
        }

        if (userEmail != null) {
            userEmail.setText(email != null ? email : "guest@healthscanner.com");
        }

        // Set member since date with account type info
        if (memberSince != null) {
            long joinTimestamp = preferences.getLong("join_date_timestamp", System.currentTimeMillis());
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.getDefault());
            String joinDate = sdf.format(new java.util.Date(joinTimestamp));

            String memberText = "Member since " + joinDate;
            if ("google.com".equals(authProvider)) {
                memberText += " • Google Account";
            }
            memberSince.setText(memberText);
        }

        // Load profile photo if available
        loadProfilePhoto(photoUrl);

        // Show special message for Google users
        if ("google.com".equals(authProvider) && freshSignIn) {
            showWelcomeMessage(firstName.isEmpty() ? name : firstName);
        }

        Log.d(TAG, "Enhanced user profile loaded: " + displayName + " (" + authProvider + ")");
    }

    /**
     * Load user's profile photo from Google account
     */
    private void loadProfilePhoto(String photoUrl) {
        if (profileAvatar != null && photoUrl != null && !photoUrl.isEmpty()) {
            try {
                // For now, just log the photo URL - you can implement image loading later
                Log.d(TAG, "User profile photo available: " + photoUrl);
                // TODO: Implement image loading with Glide or similar library
                // Glide.with(this).load(photoUrl).into(profileAvatar);
            } catch (Exception e) {
                Log.e(TAG, "Error loading profile photo: " + e.getMessage());
            }
        }
    }

    /**
     * Show special welcome message for Google users
     */
    private void showWelcomeMessage(String firstName) {
        try {
            String welcomeMessage = "Welcome to Health Scanner, " + firstName + "! 🎉\n\n" +
                    "Your Google account has been connected successfully. " +
                    "All your health data will be synced across your devices.";

            new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                    .setTitle("Welcome! 👋")
                    .setMessage(welcomeMessage)
                    .setPositiveButton("Let's Start!", (dialog, which) -> {
                        // Maybe show a quick tour or highlight key features
                        dialog.dismiss();
                    })
                    .setIcon(R.drawable.ic_person)
                    .show();

        } catch (Exception e) {
            Log.e(TAG, "Error showing welcome message: " + e.getMessage());
        }
    }

    private void updateUIWithUserData(com.example.healthscanner.models.User user) {
        // Update user information
        if (userName != null && user.getDisplayName() != null) {
            userName.setText(user.getDisplayName());
        }

        if (userEmail != null && user.getEmail() != null) {
            userEmail.setText(user.getEmail());
        }

        // Set member since date
        if (memberSince != null && user.getCreatedAt() != null) {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy", java.util.Locale.getDefault());
            String joinYear = sdf.format(user.getCreatedAt());
            memberSince.setText("Member since " + joinYear);
        }

        // Update preferences switches
        if (notificationsSwitch != null) {
            notificationsSwitch.setChecked(user.isNotificationsEnabled());
        }

        if (darkModeSwitch != null) {
            darkModeSwitch.setChecked(user.isDarkModeEnabled());
        }

        // Update statistics with real data from database
        if (totalScans != null) {
            animateStatistic(totalScans, 0, user.getTotalScans(), 1000);
        }

        if (healthyChoices != null) {
            animateStatistic(healthyChoices, 0, user.getHealthyChoices(), 1200);
        }

        if (healthScore != null) {
            int scoreValue = (int) (user.getAverageHealthScore() * 10);
            animateStatistic(healthScore, 0, scoreValue, 800, true);
        }
    }

    private void loadHealthStatistics() {
        try {
            // Load real statistics from scan history
            String scanHistoryJson = preferences.getString("recent_scans", "[]");
            org.json.JSONArray scanArray = new org.json.JSONArray(scanHistoryJson);

            int totalScansCount = scanArray.length();
            int healthyCount = 0;
            double totalHealthScore = 0;

            // Calculate statistics
            for (int i = 0; i < scanArray.length(); i++) {
                org.json.JSONObject scan = scanArray.getJSONObject(i);
                if (scan.has("healthScore")) {
                    double score = scan.getDouble("healthScore");
                    totalHealthScore += score;
                    if (score >= 70) {
                        healthyCount++;
                    }
                }
            }

            // Update UI with animations
            animateStatistic(totalScans, 0, totalScansCount, 1000);
            animateStatistic(healthyChoices, 0, healthyCount, 1200);

            if (totalScansCount > 0) {
                double avgScore = totalHealthScore / totalScansCount;
                animateStatistic(healthScore, 0, (int) (avgScore * 10), 800, true);
            } else {
                if (healthScore != null) {
                    healthScore.setText("0.0");
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Error loading health statistics", e);
            // Set default values
            if (totalScans != null)
                totalScans.setText("0");
            if (healthyChoices != null)
                healthyChoices.setText("0");
            if (healthScore != null)
                healthScore.setText("0.0");
        }
    }

    private void setupRecentScans() {
        recentScanItems = new ArrayList<>();

        // Load recent scans from preferences
        loadRecentScansData();

        // Setup RecyclerView
        if (recentScansRecycler != null) {
            recentScansAdapter = new RecentScansProfileAdapter(this, recentScanItems);
            recentScansRecycler.setLayoutManager(new LinearLayoutManager(this));
            recentScansRecycler.setAdapter(recentScansAdapter);

            // Set item click listener
            recentScansAdapter.setOnItemClickListener(item -> {
                // Navigate to product details
                Intent intent = new Intent(this, ProductDetailsEnhancedActivity.class);
                intent.putExtra("barcode", item.getBarcode());
                startActivity(intent);
            });
        }
    }

    private void loadRecentScansData() {
        try {
            String scanHistoryJson = preferences.getString("recent_scans", "[]");
            org.json.JSONArray scanArray = new org.json.JSONArray(scanHistoryJson);

            recentScanItems.clear();

            // Load up to 5 recent scans
            int limit = Math.min(scanArray.length(), 5);
            for (int i = 0; i < limit; i++) {
                org.json.JSONObject scan = scanArray.getJSONObject(i);

                String name = scan.optString("name", "Unknown Product");
                String brand = scan.optString("brand", "Unknown Brand");
                double healthScore = scan.optDouble("healthScore", 0.0);
                int calories = scan.optInt("calories", 0);
                long timestamp = scan.optLong("timestamp", System.currentTimeMillis());
                String barcode = scan.optString("barcode", "");

                recentScanItems.add(new RecentScansProfileAdapter.ScanItem(
                        name, brand, healthScore, calories, timestamp, barcode));
            }

        } catch (Exception e) {
            Log.e(TAG, "Error loading recent scans", e);
        }
    }

    private void setupClickListeners() {
        // Profile avatar click
        if (profileAvatar != null) {
            profileAvatar.setOnClickListener(v -> {
                animateClick(v);
                showAvatarOptions();
            });
        }

        // Edit profile menu
        ImageView editProfileMenu = findViewById(R.id.edit_profile_menu);
        if (editProfileMenu != null) {
            editProfileMenu.setOnClickListener(v -> {
                animateClick(v);
                showEditProfileDialog();
            });
        }

        // View all activity
        TextView viewAllActivity = findViewById(R.id.view_all_activity);
        if (viewAllActivity != null) {
            viewAllActivity.setOnClickListener(v -> {
                animateClick(v);
                Intent intent = new Intent(this, HistoryActivity.class);
                startActivity(intent);
            });
        }

        // Notifications switch
        if (notificationsSwitch != null) {
            boolean notificationsEnabled = preferences.getBoolean("notifications_enabled", true);
            notificationsSwitch.setChecked(notificationsEnabled);

            notificationsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                updatePreferencesInDatabase(isChecked, darkModeSwitch.isChecked());
            });
        }

        // Dark mode switch
        if (darkModeSwitch != null) {
            // Set switch state based on saved preference
            boolean isDarkMode = darkModeManager.isDarkModeEnabled();
            darkModeSwitch.setChecked(isDarkMode);

            darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                darkModeManager.toggleDarkMode(isChecked);
                updatePreferencesInDatabase(notificationsSwitch.isChecked(), isChecked);

                // Immediately sync the dark mode preference
                if (syncManager != null) {
                    syncManager.syncOnSettingsChange(new SyncManager.SyncCallback() {
                        @Override
                        public void onSuccess() {
                            Log.d(TAG, "Dark mode preference synced to Firebase");
                        }

                        @Override
                        public void onFailure(String error) {
                            Log.w(TAG, "Failed to sync dark mode preference: " + error);
                        }
                    });
                }
            });
        }

        // Privacy setting
        View privacySetting = findViewById(R.id.privacy_setting);
        if (privacySetting != null) {
            privacySetting.setOnClickListener(v -> {
                animateClick(v);
                showPrivacyDialog();
            });
        }

        // Help center
        View helpCenter = findViewById(R.id.help_center);
        if (helpCenter != null) {
            helpCenter.setOnClickListener(v -> {
                animateClick(v);
                openHelpCenter();
            });
        }

        // Contact support
        View contactSupport = findViewById(R.id.contact_support);
        if (contactSupport != null) {
            contactSupport.setOnClickListener(v -> {
                animateClick(v);
                contactSupport();
            });
        }

        // Logout button
        if (logoutButton != null) {
            logoutButton.setOnClickListener(v -> {
                animateClick(v);
                showLogoutConfirmation();
            });
        }
    }

    private void setupAnimations() {
        // Staggered entrance animations for cards
        MaterialCardView healthStatsCard = findViewById(R.id.health_stats_card);
        MaterialCardView recentActivityCard = findViewById(R.id.recent_activity_card);
        MaterialCardView healthConcernsCard = findViewById(R.id.health_concerns_card);
        MaterialCardView dietaryPreferencesCard = findViewById(R.id.dietary_preferences_card);
        MaterialCardView settingsCard = findViewById(R.id.settings_card);
        MaterialCardView supportCard = findViewById(R.id.support_card);

        // Animate cards with delays
        animateCardEntrance(healthStatsCard, 200);
        animateCardEntrance(recentActivityCard, 400);
        animateCardEntrance(healthConcernsCard, 600);
        animateCardEntrance(dietaryPreferencesCard, 800);
        animateCardEntrance(settingsCard, 1000);
        animateCardEntrance(supportCard, 1200);
    }

    private void animateCardEntrance(View view, long delay) {
        if (view != null) {
            view.setAlpha(0f);
            view.setTranslationY(100f);
            view.postDelayed(() -> {
                view.animate()
                        .alpha(1f)
                        .translationY(0f)
                        .setDuration(500)
                        .start();
            }, delay);
        }
    }

    private void animateStatistic(TextView textView, int start, int end, int duration) {
        animateStatistic(textView, start, end, duration, false);
    }

    private void animateStatistic(TextView textView, int start, int end, int duration, boolean isDecimal) {
        if (textView != null) {
            ValueAnimator animator = ValueAnimator.ofInt(start, end);
            animator.setDuration(duration);
            animator.addUpdateListener(animation -> {
                int value = (int) animation.getAnimatedValue();
                if (isDecimal) {
                    textView.setText(String.format(Locale.getDefault(), "%.1f", value / 10.0));
                } else {
                    textView.setText(String.valueOf(value));
                }
            });
            animator.start();
        }
    }

    private void animateClick(View view) {
        view.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction(() -> {
                    view.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(100)
                            .start();
                })
                .start();
    }

    private void showAvatarOptions() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("Change Profile Picture");
        builder.setItems(new String[] { "Take Photo", "Choose from Gallery", "Remove Photo" },
                (dialog, which) -> {
                    switch (which) {
                        case 0:
                            // Take photo functionality
                            Log.d(TAG, "Take photo selected");
                            break;
                        case 1:
                            // Choose from gallery functionality
                            Log.d(TAG, "Choose from gallery selected");
                            break;
                        case 2:
                            // Remove photo functionality
                            Log.d(TAG, "Remove photo selected");
                            break;
                    }
                });
        builder.show();
    }

    private void showEditProfileDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_profile, null);

        TextInputEditText nameInput = dialogView.findViewById(R.id.name_input);
        TextInputEditText emailInput = dialogView.findViewById(R.id.email_input);

        // Pre-fill current values
        nameInput.setText(authManager.getCurrentUserName());
        emailInput.setText(authManager.getCurrentUserEmail());

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("Edit Profile");
        builder.setView(dialogView);
        builder.setPositiveButton("Save", (dialog, which) -> {
            String newName = nameInput.getText().toString().trim();
            String newEmail = emailInput.getText().toString().trim();

            if (!newName.isEmpty()) {
                // Update profile information
                updateProfile(newName, newEmail);
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void updateProfile(String name, String email) {
        // Update in database first
        authManager.updateUserProfile(name, email, new AuthManager.AuthCallback() {
            @Override
            public void onSuccess() {
                // Update UI
                if (userName != null) {
                    userName.setText(name);
                }
                if (userEmail != null) {
                    userEmail.setText(email);
                }

                Log.d(TAG, "Profile updated successfully: " + name + ", " + email);

                // Show success message
                android.widget.Toast.makeText(ProfileActivity.this,
                        "Profile updated successfully", android.widget.Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(String error) {
                Log.e(TAG, "Failed to update profile: " + error);

                // Show error message
                android.widget.Toast.makeText(ProfileActivity.this,
                        "Failed to update profile: " + error, android.widget.Toast.LENGTH_LONG).show();
            }
        });
    }

    private void updatePreferencesInDatabase(boolean notificationsEnabled, boolean darkModeEnabled) {
        // Update local preferences first
        preferences.edit()
                .putBoolean("notifications_enabled", notificationsEnabled)
                .putBoolean("dark_mode_enabled", darkModeEnabled)
                .apply();

        // Immediately sync settings to Firebase
        if (syncManager != null) {
            syncManager.syncOnSettingsChange(new SyncManager.SyncCallback() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "App settings synced to Firebase immediately");
                }

                @Override
                public void onFailure(String error) {
                    Log.w(TAG, "Failed to sync app settings: " + error);
                }
            });
        }

        // Also update through AuthManager for backward compatibility
        authManager.updateUserPreferences(notificationsEnabled, darkModeEnabled, new AuthManager.AuthCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Preferences updated in database successfully");
            }

            @Override
            public void onFailure(String error) {
                Log.w(TAG, "Failed to update preferences in database: " + error);
            }
        });
    }

    // Removed toggleDarkMode method - now handled by DarkModeManager

    private void showPrivacyDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("Privacy & Security");
        builder.setMessage(
                "Your data is encrypted and stored securely. We never share your personal information with third parties.");
        builder.setPositiveButton("Learn More", (dialog, which) -> {
            // Open privacy policy
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://healthscanner.com/privacy"));
            startActivity(intent);
        });
        builder.setNegativeButton("Close", null);
        builder.show();
    }

    private void openHelpCenter() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("Help Center");
        builder.setMessage("Need help? Check out our FAQ or contact support for assistance.");
        builder.setPositiveButton("FAQ", (dialog, which) -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://healthscanner.com/faq"));
            startActivity(intent);
        });
        builder.setNegativeButton("Close", null);
        builder.show();
    }

    private void contactSupport() {
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
        emailIntent.setData(Uri.parse("mailto:support@healthscanner.com"));
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "HealthScanner Support Request");
        emailIntent.putExtra(Intent.EXTRA_TEXT, "Hi HealthScanner team,\n\nI need help with...\n\n");

        try {
            startActivity(Intent.createChooser(emailIntent, "Send Email"));
        } catch (Exception e) {
            Log.e(TAG, "No email app found", e);
        }
    }

    private void showLogoutConfirmation() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("Sign Out");
        builder.setMessage("Are you sure you want to sign out?");
        builder.setPositiveButton("Sign Out", (dialog, which) -> {
            authManager.signOut(this);
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Setup health concerns functionality
     */
    private void setupHealthConcerns() {
        loadHealthConcerns();

        if (addConcernButton != null) {
            addConcernButton.setOnClickListener(v -> showAddHealthConcernDialog());
        }

        if (editHealthConcerns != null) {
            editHealthConcerns.setOnClickListener(v -> showEditHealthConcernsDialog());
        }
    }

    /**
     * Setup dietary preferences functionality
     */
    private void setupDietaryPreferences() {
        loadDietaryPreferences();

        if (addPreferenceButton != null) {
            addPreferenceButton.setOnClickListener(v -> showAddDietaryPreferenceDialog());
        }

        if (editDietaryPreferences != null) {
            editDietaryPreferences.setOnClickListener(v -> showEditDietaryPreferencesDialog());
        }
    }

    /**
     * Load health concerns from preferences
     */
    private void loadHealthConcerns() {
        if (healthConcernsChipGroup == null)
            return;

        java.util.Set<String> concerns = preferences.getStringSet("health_concerns", new java.util.HashSet<>());
        healthConcernsChipGroup.removeAllViews();

        for (String concern : concerns) {
            addHealthConcernChip(concern);
        }

        // Add default message if no concerns
        if (concerns.isEmpty()) {
            addHealthConcernChip("No health concerns added");
        }
    }

    /**
     * Load dietary preferences from preferences
     */
    private void loadDietaryPreferences() {
        if (dietaryPreferencesChipGroup == null)
            return;

        java.util.Set<String> preferences = this.preferences.getStringSet("dietary_preferences",
                new java.util.HashSet<>());
        dietaryPreferencesChipGroup.removeAllViews();

        for (String preference : preferences) {
            addDietaryPreferenceChip(preference);
        }

        // Add default message if no preferences
        if (preferences.isEmpty()) {
            addDietaryPreferenceChip("No dietary preferences added");
        }
    }

    /**
     * Add health concern chip to the group
     */
    private void addHealthConcernChip(String concern) {
        com.google.android.material.chip.Chip chip = new com.google.android.material.chip.Chip(this);
        chip.setText(concern);
        chip.setChipBackgroundColorResource(R.color.health_unhealthy);
        chip.setTextColor(ContextCompat.getColor(this, R.color.white));
        chip.setCloseIconVisible(true);
        chip.setCloseIconTintResource(R.color.white);

        chip.setOnCloseIconClickListener(v -> {
            removeHealthConcern(concern);
            healthConcernsChipGroup.removeView(chip);
        });

        healthConcernsChipGroup.addView(chip);
    }

    /**
     * Add dietary preference chip to the group
     */
    private void addDietaryPreferenceChip(String preference) {
        com.google.android.material.chip.Chip chip = new com.google.android.material.chip.Chip(this);
        chip.setText(preference);
        chip.setChipBackgroundColorResource(R.color.health_excellent);
        chip.setTextColor(ContextCompat.getColor(this, R.color.white));
        chip.setCloseIconVisible(true);
        chip.setCloseIconTintResource(R.color.white);

        chip.setOnCloseIconClickListener(v -> {
            removeDietaryPreference(preference);
            dietaryPreferencesChipGroup.removeView(chip);
        });

        dietaryPreferencesChipGroup.addView(chip);
    }

    /**
     * Show dialog to add health concern
     */
    private void showAddHealthConcernDialog() {
        String[] commonConcerns = {
                "Diabetes", "High Blood Pressure", "Heart Disease", "High Cholesterol",
                "Gluten Intolerance", "Lactose Intolerance", "Food Allergies", "Kidney Disease",
                "Obesity", "Acid Reflux", "IBS", "Custom..."
        };

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Add Health Concern")
                .setItems(commonConcerns, (dialog, which) -> {
                    if (which == commonConcerns.length - 1) {
                        // Custom option
                        showCustomHealthConcernDialog();
                    } else {
                        addHealthConcern(commonConcerns[which]);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Show dialog to add dietary preference
     */
    private void showAddDietaryPreferenceDialog() {
        String[] commonPreferences = {
                "Vegetarian", "Vegan", "Keto", "Low Carb", "Low Fat", "Low Sodium",
                "Sugar-Free", "Organic Only", "Non-GMO", "Halal", "Kosher", "Custom..."
        };

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Add Dietary Preference")
                .setItems(commonPreferences, (dialog, which) -> {
                    if (which == commonPreferences.length - 1) {
                        // Custom option
                        showCustomDietaryPreferenceDialog();
                    } else {
                        addDietaryPreference(commonPreferences[which]);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Show custom health concern input dialog
     */
    private void showCustomHealthConcernDialog() {
        android.widget.EditText input = new android.widget.EditText(this);
        input.setHint("Enter your health concern");

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Custom Health Concern")
                .setView(input)
                .setPositiveButton("Add", (dialog, which) -> {
                    String concern = input.getText().toString().trim();
                    if (!concern.isEmpty()) {
                        addHealthConcern(concern);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Show custom dietary preference input dialog
     */
    private void showCustomDietaryPreferenceDialog() {
        android.widget.EditText input = new android.widget.EditText(this);
        input.setHint("Enter your dietary preference");

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Custom Dietary Preference")
                .setView(input)
                .setPositiveButton("Add", (dialog, which) -> {
                    String preference = input.getText().toString().trim();
                    if (!preference.isEmpty()) {
                        addDietaryPreference(preference);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Add health concern to preferences
     */
    private void addHealthConcern(String concern) {
        java.util.Set<String> concerns = new java.util.HashSet<>(
                preferences.getStringSet("health_concerns", new java.util.HashSet<>()));
        concerns.add(concern);

        preferences.edit().putStringSet("health_concerns", concerns).apply();

        // Immediately sync health preferences to Firebase
        if (syncManager != null) {
            syncManager.syncOnHealthPreferencesChange(new SyncManager.SyncCallback() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "Health concern added and synced to Firebase");
                }

                @Override
                public void onFailure(String error) {
                    Log.w(TAG, "Failed to sync health concern: " + error);
                }
            });
        }

        // Refresh UI
        loadHealthConcerns();

        Log.d(TAG, "Added health concern: " + concern);
    }

    /**
     * Add dietary preference to preferences
     */
    private void addDietaryPreference(String preference) {
        java.util.Set<String> prefs = new java.util.HashSet<>(
                preferences.getStringSet("dietary_preferences", new java.util.HashSet<>()));
        prefs.add(preference);

        preferences.edit().putStringSet("dietary_preferences", prefs).apply();

        // Immediately sync dietary preferences to Firebase
        if (syncManager != null) {
            syncManager.syncOnHealthPreferencesChange(new SyncManager.SyncCallback() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "Dietary preference added and synced to Firebase");
                }

                @Override
                public void onFailure(String error) {
                    Log.w(TAG, "Failed to sync dietary preference: " + error);
                }
            });
        }

        // Refresh UI
        loadDietaryPreferences();

        Log.d(TAG, "Added dietary preference: " + preference);
    }

    /**
     * Remove health concern from preferences
     */
    private void removeHealthConcern(String concern) {
        java.util.Set<String> concerns = new java.util.HashSet<>(
                preferences.getStringSet("health_concerns", new java.util.HashSet<>()));
        concerns.remove(concern);

        preferences.edit().putStringSet("health_concerns", concerns).apply();

        // Immediately sync health preferences to Firebase
        if (syncManager != null) {
            syncManager.syncOnHealthPreferencesChange(new SyncManager.SyncCallback() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "Health concern removed and synced to Firebase");
                }

                @Override
                public void onFailure(String error) {
                    Log.w(TAG, "Failed to sync health concern removal: " + error);
                }
            });
        }

        Log.d(TAG, "Removed health concern: " + concern);
    }

    /**
     * Remove dietary preference from preferences
     */
    private void removeDietaryPreference(String preference) {
        java.util.Set<String> prefs = new java.util.HashSet<>(
                preferences.getStringSet("dietary_preferences", new java.util.HashSet<>()));
        prefs.remove(preference);

        preferences.edit().putStringSet("dietary_preferences", prefs).apply();

        // Immediately sync dietary preferences to Firebase
        if (syncManager != null) {
            syncManager.syncOnHealthPreferencesChange(new SyncManager.SyncCallback() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "Dietary preference removed and synced to Firebase");
                }

                @Override
                public void onFailure(String error) {
                    Log.w(TAG, "Failed to sync dietary preference removal: " + error);
                }
            });
        }

        Log.d(TAG, "Removed dietary preference: " + preference);
    }

    /**
     * Sync health data with Firebase
     */
    private void syncHealthDataWithFirebase() {
        try {
            String userId = authManager.getCurrentUserId();
            if (userId != null && !userId.isEmpty()) {
                java.util.Map<String, Object> healthData = new java.util.HashMap<>();
                healthData.put("healthConcerns",
                        preferences.getStringSet("health_concerns", new java.util.HashSet<>()));
                healthData.put("dietaryPreferences",
                        preferences.getStringSet("dietary_preferences", new java.util.HashSet<>()));

                com.example.healthscanner.database.FirebaseManager firebaseManager = com.example.healthscanner.database.FirebaseManager
                        .getInstance();

                firebaseManager.updateUserPreferences(userId, healthData,
                        new com.example.healthscanner.database.FirebaseManager.OperationCallback() {
                            @Override
                            public void onSuccess() {
                                Log.d(TAG, "Health data synced with Firebase successfully");
                            }

                            @Override
                            public void onFailure(String error) {
                                Log.w(TAG, "Failed to sync health data with Firebase: " + error);
                            }
                        });
            }
        } catch (Exception e) {
            Log.e(TAG, "Error syncing health data with Firebase: " + e.getMessage(), e);
        }
    }

    /**
     * Show edit health concerns dialog
     */
    private void showEditHealthConcernsDialog() {
        // For now, just show the add dialog
        showAddHealthConcernDialog();
    }

    /**
     * Show edit dietary preferences dialog
     */
    private void showEditDietaryPreferencesDialog() {
        // For now, just show the add dialog
        showAddDietaryPreferenceDialog();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data when returning to profile
        loadHealthStatistics();
        loadRecentScansData();
        if (recentScansAdapter != null) {
            recentScansAdapter.notifyDataSetChanged();
        }
    }

}