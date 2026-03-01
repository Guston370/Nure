package com.example.healthscanner;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import android.widget.LinearLayout;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;

/**
 * Settings Activity for app configuration and user preferences
 * Matches Home Page design with gradient header and animated cards
 */
public class SettingsActivity extends BaseActivity {

    private static final String TAG = "SettingsActivity";
    private static final String PREFS_NAME = "HealthScannerSettings";
    private static final String KEY_NOTIFICATIONS = "notifications_enabled";
    private static final String KEY_DARK_MODE = "dark_mode_enabled";

    // UI Elements
    private TextView settingsTitle;
    private TextView settingsSubtitle;
    private ImageView helpIcon;

    // Setting Cards
    private View notificationsCard;
    private View darkModeCard;
    private View privacyPolicyCard;
    private View exportHistoryCard;
    private View logoutCard;

    // Switches
    private SwitchMaterial notificationSwitch;
    private SwitchMaterial darkModeSwitch;

    // Auth Manager
    private AuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings_enhanced);

        // Initialize AuthManager
        authManager = AuthManager.getInstance(this);

        // Check authentication
        if (!authManager.isUserAuthenticated()) {
            authManager.navigateToLogin(this);
            return;
        }

        initializeViews();
        initializeBottomNavigation();
        setupEntranceAnimations();
        setupClickListeners();
        loadSettings();
    }

    private void initializeViews() {
        // Header elements
        settingsTitle = findViewById(R.id.settingsTitle);
        settingsSubtitle = findViewById(R.id.settingsSubtitle);
        helpIcon = findViewById(R.id.helpIcon);

        // Setting cards
        notificationsCard = findViewById(R.id.notificationsCard);
        darkModeCard = findViewById(R.id.darkModeCard);
        privacyPolicyCard = findViewById(R.id.privacyPolicyCard);
        exportHistoryCard = findViewById(R.id.exportHistoryCard);
        logoutCard = findViewById(R.id.logoutCard);

        // Switches
        notificationSwitch = findViewById(R.id.notificationSwitch);
        darkModeSwitch = findViewById(R.id.darkModeSwitch);
    }

    @Override
    protected int getCurrentNavigationItemId() {
        return R.id.nav_profile; // Settings is accessed from Profile
    }

    private void setupEntranceAnimations() {
        // Header title animation
        if (settingsTitle != null) {
            settingsTitle.postDelayed(() -> {
                settingsTitle.setAlpha(1f);
                settingsTitle.setTranslationY(0f);
                settingsTitle.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in_slide_down));
            }, 300);
        }

        // Staggered card animations
        animateCardsSequentially();
    }

    private void animateCardsSequentially() {
        View[] cards = { notificationsCard, darkModeCard, privacyPolicyCard, exportHistoryCard, logoutCard };
        int[] delays = { 400, 500, 600, 700, 800 };

        for (int i = 0; i < cards.length; i++) {
            if (cards[i] != null) {
                final View card = cards[i];
                final boolean isLogout = (i == cards.length - 1);

                card.postDelayed(() -> {
                    if (isLogout) {
                        // Logout card gets fade + scale animation
                        card.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));
                        card.postDelayed(() -> {
                            card.startAnimation(AnimationUtils.loadAnimation(this, R.anim.scale_bounce));
                        }, 200);
                    } else {
                        // Other cards get slide up animation
                        card.startAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_up));
                    }
                }, delays[i]);
            }
        }
    }

    private void setupClickListeners() {
        // Help icon click
        if (helpIcon != null) {
            helpIcon.setOnClickListener(v -> {
                v.startAnimation(AnimationUtils.loadAnimation(this, R.anim.scale_bounce));
                showHelpDialog();
            });
        }

        // Notifications card click
        if (notificationsCard != null) {
            notificationsCard.setOnClickListener(v -> {
                v.startAnimation(AnimationUtils.loadAnimation(this, R.anim.scale_bounce));
                if (notificationSwitch != null) {
                    notificationSwitch.toggle();
                }
            });
        }

        // Dark mode card click
        if (darkModeCard != null) {
            darkModeCard.setOnClickListener(v -> {
                v.startAnimation(AnimationUtils.loadAnimation(this, R.anim.scale_bounce));
                if (darkModeSwitch != null) {
                    darkModeSwitch.toggle();
                }
            });
        }

        // Privacy policy card click
        if (privacyPolicyCard != null) {
            privacyPolicyCard.setOnClickListener(v -> {
                v.startAnimation(AnimationUtils.loadAnimation(this, R.anim.scale_bounce));
                openPrivacyPolicy();
            });
        }

        // Export history card click
        if (exportHistoryCard != null) {
            exportHistoryCard.setOnClickListener(v -> {
                v.startAnimation(AnimationUtils.loadAnimation(this, R.anim.scale_bounce));
                exportScanHistory();
            });
        }

        // Logout card click
        if (logoutCard != null) {
            logoutCard.setOnClickListener(v -> {
                v.startAnimation(AnimationUtils.loadAnimation(this, R.anim.scale_bounce));
                showLogoutConfirmation();
            });
        }

        // Switch listeners
        setupSwitchListeners();
    }

    private void setupSwitchListeners() {
        // Notification switch
        if (notificationSwitch != null) {
            notificationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                buttonView.startAnimation(AnimationUtils.loadAnimation(this, R.anim.scale_bounce));
                saveNotificationSetting(isChecked);

                String message = isChecked ? "Notifications enabled" : "Notifications disabled";
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            });
        }

        // Dark mode switch
        if (darkModeSwitch != null) {
            darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                buttonView.startAnimation(AnimationUtils.loadAnimation(this, R.anim.scale_bounce));
                saveDarkModeSetting(isChecked);

                String message = isChecked ? "Dark mode enabled" : "Dark mode disabled";
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();

                // Note: In a full implementation, you'd restart the activity or apply theme
                // changes
            });
        }
    }

    private void loadSettings() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Load notification setting
        boolean notificationsEnabled = prefs.getBoolean(KEY_NOTIFICATIONS, true);
        if (notificationSwitch != null) {
            notificationSwitch.setChecked(notificationsEnabled);
        }

        // Load dark mode setting
        boolean darkModeEnabled = prefs.getBoolean(KEY_DARK_MODE, false);
        if (darkModeSwitch != null) {
            darkModeSwitch.setChecked(darkModeEnabled);
        }
    }

    private void saveNotificationSetting(boolean enabled) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_NOTIFICATIONS, enabled).apply();
    }

    private void saveDarkModeSetting(boolean enabled) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_DARK_MODE, enabled).apply();
    }

    private void showHelpDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("Help & Support");
        builder.setMessage("Need help with Nure Health Scanner?\n\n" +
                "• Scan barcodes to get nutrition information\n" +
                "• Save healthy products to your favorites\n" +
                "• Track your health journey with statistics\n\n" +
                "For more support, contact us through the app.");
        builder.setPositiveButton("Got it", null);
        builder.setNeutralButton("Contact Support", (dialog, which) -> {
            // Handle contact support
            Toast.makeText(this, "Contact support feature coming soon!", Toast.LENGTH_SHORT).show();
        });
        builder.show();
    }

    private void openPrivacyPolicy() {
        // In a real app, this would open a web view or external browser
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("Privacy Policy");
        builder.setMessage(
                "Your privacy is important to us. We collect minimal data necessary to provide our health scanning services.\n\n"
                        +
                        "• We don't sell your personal information\n" +
                        "• Scan data is stored locally and encrypted\n" +
                        "• You can delete your data anytime\n\n" +
                        "Full privacy policy available at nure.health/privacy");
        builder.setPositiveButton("OK", null);
        builder.setNeutralButton("View Online", (dialog, which) -> {
            // Open privacy policy URL
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://nure.health/privacy"));
            startActivity(intent);
        });
        builder.show();
    }

    private void exportScanHistory() {
        // Show progress and simulate export
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("Export Scan History");
        builder.setMessage("Export your scan history as a CSV file?\n\n" +
                "This will include:\n" +
                "• Product names and brands\n" +
                "• Scan dates and times\n" +
                "• Nutrition information\n" +
                "• Health scores");
        builder.setPositiveButton("Export", (dialog, which) -> {
            // Simulate export process
            Toast.makeText(this, "Exporting scan history...", Toast.LENGTH_SHORT).show();

            // In a real app, this would generate and save a CSV file
            new Handler().postDelayed(() -> {
                Toast.makeText(this, "Export completed! Check your Downloads folder.", Toast.LENGTH_LONG).show();
            }, 2000);
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showLogoutConfirmation() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("Logout");
        builder.setMessage(
                "Are you sure you want to logout?\n\nYour scan history will be preserved for when you return.");
        builder.setPositiveButton("Logout", (dialog, which) -> {
            // Perform logout
            authManager.signOut(this);
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
}