package com.example.healthscanner;

import android.Manifest;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import android.widget.LinearLayout;

import com.example.healthscanner.database.ScanHistoryStore;
import com.example.healthscanner.models.Scan;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Settings Activity for app configuration and user preferences
 * Matches Home Page design with gradient header and animated cards
 */
public class SettingsActivity extends BaseActivity {

    private static final String TAG = "SettingsActivity";
    /**
     * Shared with {@link DarkModeManager} and the rest of the app. This screen previously
     * used its own preferences file, so the dark mode toggle here never affected the theme.
     */
    private static final String PREFS_NAME = "HealthScannerPrefs";
    private static final String KEY_NOTIFICATIONS = "notifications_enabled";
    private static final String KEY_DARK_MODE = "dark_mode_enabled";

    // UI Elements
    private TextView settingsTitle;
    private TextView settingsSubtitle;
    private View helpIcon;

    // Setting Cards
    private View notificationsCard;
    private View darkModeCard;
    private View privacyPolicyCard;
    private View exportHistoryCard;
    private View clearDataCard;
    private View logoutCard;

    // Switches
    private SwitchMaterial notificationSwitch;
    private SwitchMaterial darkModeSwitch;

    // Managers
    private AuthManager authManager;
    private DarkModeManager darkModeManager;
    private DataResetManager dataResetManager;
    private ScanHistoryStore scanHistoryStore;

    /** Set while {@link #loadSettings()} is populating switches, to suppress listeners. */
    private boolean isBindingSettings;

    private ActivityResultLauncher<String> notificationPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings_enhanced);

        // Initialize managers
        authManager = AuthManager.getInstance(this);
        darkModeManager = DarkModeManager.getInstance(this);
        dataResetManager = DataResetManager.getInstance(this);
        scanHistoryStore = ScanHistoryStore.getInstance(this);
        registerNotificationPermissionLauncher();

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
        // Header elements - two-line title: "App" / "Settings"
        settingsTitle = findViewById(R.id.settingsTitle);
        settingsSubtitle = findViewById(R.id.settingsSubtitle);
        helpIcon = findViewById(R.id.helpIcon); // FrameLayout in XML, use View

        // Setting cards
        notificationsCard = findViewById(R.id.notificationsCard);
        darkModeCard = findViewById(R.id.darkModeCard);
        privacyPolicyCard = findViewById(R.id.privacyPolicyCard);
        exportHistoryCard = findViewById(R.id.exportHistoryCard);
        clearDataCard = findViewById(R.id.clearDataCard);
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
        View[] cards = { notificationsCard, darkModeCard, privacyPolicyCard, exportHistoryCard,
                clearDataCard, logoutCard };
        int[] delays = { 400, 500, 600, 700, 800, 900 };

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

        // Clear scan history card click
        if (clearDataCard != null) {
            clearDataCard.setOnClickListener(v -> {
                v.startAnimation(AnimationUtils.loadAnimation(this, R.anim.scale_bounce));
                showClearDataConfirmation();
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
                if (isBindingSettings) {
                    return;
                }
                buttonView.startAnimation(AnimationUtils.loadAnimation(this, R.anim.scale_bounce));

                if (isChecked && !hasNotificationPermission()) {
                    // Android 13+ needs runtime consent before we can post health tips.
                    requestNotificationPermission();
                    return;
                }

                saveNotificationSetting(isChecked);
                Toast.makeText(this, isChecked ? "Notifications enabled" : "Notifications disabled",
                        Toast.LENGTH_SHORT).show();
            });
        }

        // Dark mode switch
        if (darkModeSwitch != null) {
            darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isBindingSettings) {
                    return;
                }
                buttonView.startAnimation(AnimationUtils.loadAnimation(this, R.anim.scale_bounce));

                // DarkModeManager persists the flag and applies the night mode immediately,
                // which recreates this activity with the new theme.
                darkModeManager.toggleDarkMode(isChecked);
            });
        }
    }

    /**
     * Register the POST_NOTIFICATIONS permission request. Must happen in {@code onCreate}.
     */
    private void registerNotificationPermissionLauncher() {
        notificationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    saveNotificationSetting(granted);
                    if (granted) {
                        Toast.makeText(this, "Notifications enabled", Toast.LENGTH_SHORT).show();
                    } else {
                        // Reflect the denial in the UI rather than leaving the switch on.
                        isBindingSettings = true;
                        if (notificationSwitch != null) {
                            notificationSwitch.setChecked(false);
                        }
                        isBindingSettings = false;
                        Toast.makeText(this, R.string.settings_notifications_denied,
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    /** Below Android 13 notifications need no runtime permission. */
    private boolean hasNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true;
        }
        return ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && notificationPermissionLauncher != null) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    private void loadSettings() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Suppress the listeners while binding so restoring state isn't treated as a toggle.
        isBindingSettings = true;

        if (notificationSwitch != null) {
            boolean enabled = prefs.getBoolean(KEY_NOTIFICATIONS, true) && hasNotificationPermission();
            notificationSwitch.setChecked(enabled);
        }

        if (darkModeSwitch != null) {
            darkModeSwitch.setChecked(darkModeManager.isDarkModeEnabled());
        }

        isBindingSettings = false;
    }

    private void saveNotificationSetting(boolean enabled) {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_NOTIFICATIONS, enabled)
                .apply();
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
        builder.setNeutralButton("Contact Support", (dialog, which) -> contactSupport());
        builder.show();
    }

    /**
     * Open the user's mail app pre-filled with a support request.
     *
     * <p>Uses the {@code mailto:} scheme so only email apps can handle the intent.</p>
     */
    private void contactSupport() {
        String body = "\n\n---\n"
                + "App version: 1.0.0\n"
                + "Android: " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")\n"
                + "Device: " + Build.MANUFACTURER + " " + Build.MODEL;

        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:" + getString(R.string.settings_support_email)));
        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.settings_support_subject));
        intent.putExtra(Intent.EXTRA_TEXT, body);

        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Toast.makeText(this, R.string.settings_no_email_app, Toast.LENGTH_LONG).show();
        }
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
        List<Scan> scans = scanHistoryStore.getScans();

        if (scans.isEmpty()) {
            Toast.makeText(this, R.string.settings_export_empty, Toast.LENGTH_LONG).show();
            return;
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("Export Scan History");
        builder.setMessage("Export " + scans.size() + " scan" + (scans.size() == 1 ? "" : "s")
                + " as a CSV file?\n\n" +
                "This will include:\n" +
                "• Product names and brands\n" +
                "• Scan dates and times\n" +
                "• Nutrition information\n" +
                "• Health scores");
        builder.setPositiveButton("Export", (dialog, which) -> writeAndShareCsv(scans));
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    /**
     * Write the CSV to app-private cache storage and hand it to the share sheet.
     *
     * <p>Cache + {@link FileProvider} avoids needing storage permissions on any API level,
     * and lets the user pick where the file ends up (Drive, email, Files, ...).</p>
     */
    private void writeAndShareCsv(List<Scan> scans) {
        try {
            File exportDir = new File(getCacheDir(), "exports");
            if (!exportDir.exists() && !exportDir.mkdirs()) {
                throw new IOException("Could not create export directory");
            }

            File csvFile = new File(exportDir, ScanCsvExporter.suggestedFileName(System.currentTimeMillis()));
            try (FileOutputStream out = new FileOutputStream(csvFile)) {
                out.write(ScanCsvExporter.toCsv(scans).getBytes(StandardCharsets.UTF_8));
            }

            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", csvFile);

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/csv");
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Nure scan history");
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(shareIntent, "Export scan history"));
            Log.d(TAG, "Exported " + scans.size() + " scans to " + csvFile.getAbsolutePath());

        } catch (Exception e) {
            Log.e(TAG, "CSV export failed: " + e.getMessage(), e);
            Toast.makeText(this, R.string.settings_export_failed, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Confirm before clearing scan history. This is destructive and not recoverable, so the
     * dialog spells out exactly what will be removed.
     */
    private void showClearDataConfirmation() {
        DataResetManager.ResetStats stats = dataResetManager.getResetStats();

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle(R.string.settings_clear_data);
        builder.setMessage("This permanently deletes:\n\n" +
                "• " + stats.totalScans + " scan" + (stats.totalScans == 1 ? "" : "s") + "\n" +
                "• " + stats.savedItems + " saved product" + (stats.savedItems == 1 ? "" : "s") + "\n" +
                "• Your statistics and averages\n\n" +
                "Your profile, health concerns and dietary preferences are kept. " +
                "This cannot be undone.");
        builder.setPositiveButton("Delete", (dialog, which) -> clearScanData());
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void clearScanData() {
        dataResetManager.clearScanDataOnly(new DataResetManager.ResetCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> Toast.makeText(SettingsActivity.this,
                        "Scan history cleared", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onFailure(String error) {
                Log.e(TAG, "Failed to clear scan data: " + error);
                runOnUiThread(() -> Toast.makeText(SettingsActivity.this,
                        "Could not clear scan history", Toast.LENGTH_LONG).show());
            }
        });
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