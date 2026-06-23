package com.example.healthscanner;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

/**
 * Base Activity providing the classy bottom navigation bar
 * with elevated center scan button and amber selection indicators.
 *
 * All activities that need the bottom navbar extend this class and
 * override {@link #getCurrentNavigationItemId()} to indicate which
 * tab should be active.
 */
public abstract class BaseActivity extends AppCompatActivity {

    private static final String TAG = "BaseActivity";

    /**
     * Subclasses return the ID that identifies the current screen in the
     * bottom navigation.  Use the R.id values defined in
     * bottom_navigation_menu.xml: nav_home, nav_scan, nav_stats,
     * nav_history, nav_profile.
     */
    protected abstract int getCurrentNavigationItemId();

    // ---------------------------------------------------------------
    //  Bottom‐nav initialisation – call after setContentView()
    // ---------------------------------------------------------------

    protected void initializeBottomNavigation() {
        try {
            // Find custom navigation items
            View navHome = findViewById(R.id.navHome);
            View navSearch = findViewById(R.id.navSearch);
            View navScan = findViewById(R.id.navScan);
            View navInsights = findViewById(R.id.navInsights);
            View navProfile = findViewById(R.id.navProfile);

            if (navHome != null) {
                navHome.setOnClickListener(v -> navigateTo(R.id.nav_home, MainActivity.class));
            }
            if (navSearch != null) {
                navSearch.setOnClickListener(v -> navigateTo(R.id.nav_search, SearchActivity.class));
            }
            if (navScan != null) {
                navScan.setOnClickListener(v -> navigateTo(R.id.nav_scan, VerticalScannerActivity.class));
            }
            if (navInsights != null) {
                navInsights.setOnClickListener(v -> navigateTo(R.id.nav_insights, AnalyticsActivity.class));
            }
            if (navProfile != null) {
                navProfile.setOnClickListener(v -> navigateTo(R.id.nav_profile, ProfileActivity.class));
            }

            // Also find the FAB if they click exactly on it
            View navScanButton = findViewById(R.id.navScanButton);
            if (navScanButton != null) {
                navScanButton.setOnClickListener(v -> navigateTo(R.id.nav_scan, VerticalScannerActivity.class));
            }

            // Update UI state for active tab
            updateNavigationState();

            // Removed translation animation to prevent visual jumping during tab switches
            View navContainer = findViewById(R.id.navContainer);
            if (navContainer != null) {
                navContainer.setAlpha(1f);
                navContainer.setTranslationY(0f);
            }

            Log.d(TAG, "Glassmorphism bottom navigation initialized");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing bottom navigation: " + e.getMessage(), e);
        }
    }

    private void navigateTo(int targetItemId, Class<?> targetClass) {
        if (getCurrentNavigationItemId() == targetItemId) {
            return; // Already on this screen
        }

        Intent intent = new Intent(this, targetClass);
        intent.putExtra("from_navigation", true);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        overridePendingTransition(R.anim.nav_fade_in, R.anim.nav_fade_out);
    }

    private void updateNavigationState() {
        int currentId = getCurrentNavigationItemId();
        
        // Reset all tabs to inactive state
        setTabState(R.id.navHome, R.id.navHomeIcon, R.id.navHomeLabel, false);
        setTabState(R.id.navSearch, R.id.navSearchIcon, R.id.navSearchLabel, false);
        setTabState(R.id.navInsights, R.id.navInsightsIcon, R.id.navInsightsLabel, false);
        setTabState(R.id.navProfile, R.id.navProfileIcon, R.id.navProfileLabel, false);

        // Highlight active tab
        if (currentId == R.id.nav_home) {
            setTabState(R.id.navHome, R.id.navHomeIcon, R.id.navHomeLabel, true);
        } else if (currentId == R.id.nav_search) {
            setTabState(R.id.navSearch, R.id.navSearchIcon, R.id.navSearchLabel, true);
        } else if (currentId == R.id.nav_insights) {
            setTabState(R.id.navInsights, R.id.navInsightsIcon, R.id.navInsightsLabel, true);
        } else if (currentId == R.id.nav_profile) {
            setTabState(R.id.navProfile, R.id.navProfileIcon, R.id.navProfileLabel, true);
        }
    }

    private void setTabState(int containerId, int iconId, int labelId, boolean isActive) {
        View container = findViewById(containerId);
        ImageView icon = findViewById(iconId);
        android.widget.TextView label = findViewById(labelId);
        
        if (icon == null || label == null) return;

        int colorRes = isActive ? R.color.health_teal : R.color.text_secondary_light;
        float alpha = isActive ? 1.0f : 0.65f;

        icon.setColorFilter(ContextCompat.getColor(this, colorRes), android.graphics.PorterDuff.Mode.SRC_IN);
        label.setTextColor(ContextCompat.getColor(this, colorRes));
        label.setAlpha(alpha);

        if (container != null) {
            if (isActive) {
                container.setBackgroundResource(R.drawable.bg_nav_active_capsule);
                // Smooth scale transition
                container.animate().scaleX(1.05f).scaleY(1.05f).setDuration(200).start();
            } else {
                container.setBackground(null);
                container.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).start();
            }
        }
    }
}
