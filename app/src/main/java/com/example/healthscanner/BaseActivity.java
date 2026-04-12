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

    // Color for the active (selected) icon – amber / orange
    private static final int COLOR_ACTIVE = 0xFFF5A623;
    // Color for inactive icons – medium gray
    private static final int COLOR_INACTIVE = 0xFF9E9E9E;

    // Nav item views
    private LinearLayout navItemHome;
    private LinearLayout navItemHistory;
    private FrameLayout navItemScan;
    private LinearLayout navItemStats;
    private LinearLayout navItemProfile;

    // Icon views
    private ImageView navIconHome;
    private ImageView navIconHistory;
    private ImageView navIconScan;
    private ImageView navIconStats;
    private ImageView navIconProfile;

    // Dot indicators
    private View navDotHome;
    private View navDotHistory;
    private View navDotStats;
    private View navDotProfile;

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
            // ---- Find views ----
            navItemHome    = findViewById(R.id.nav_item_home);
            navItemHistory = findViewById(R.id.nav_item_history);
            navItemScan    = findViewById(R.id.nav_item_scan);
            navItemStats   = findViewById(R.id.nav_item_stats);
            navItemProfile = findViewById(R.id.nav_item_profile);

            navIconHome    = findViewById(R.id.nav_icon_home);
            navIconHistory = findViewById(R.id.nav_icon_history);
            navIconScan    = findViewById(R.id.nav_icon_scan);
            navIconStats   = findViewById(R.id.nav_icon_stats);
            navIconProfile = findViewById(R.id.nav_icon_profile);

            navDotHome    = findViewById(R.id.nav_dot_home);
            navDotHistory = findViewById(R.id.nav_dot_history);
            navDotStats   = findViewById(R.id.nav_dot_stats);
            navDotProfile = findViewById(R.id.nav_dot_profile);

            // ---- Highlight the current tab ----
            highlightCurrentTab();

            // ---- Wire up click listeners ----
            if (navItemHome != null) {
                navItemHome.setOnClickListener(v -> onNavItemClicked(R.id.nav_home, v));
            }
            if (navItemHistory != null) {
                navItemHistory.setOnClickListener(v -> onNavItemClicked(R.id.nav_history, v));
            }
            if (navItemScan != null) {
                navItemScan.setOnClickListener(v -> onNavItemClicked(R.id.nav_scan, v));
            }
            if (navItemStats != null) {
                navItemStats.setOnClickListener(v -> onNavItemClicked(R.id.nav_stats, v));
            }
            if (navItemProfile != null) {
                navItemProfile.setOnClickListener(v -> onNavItemClicked(R.id.nav_profile, v));
            }

            // Subtle entrance animation for the navbar
            View navbarContainer = findViewById(R.id.classy_navbar_container);
            if (navbarContainer != null) {
                navbarContainer.setTranslationY(100f);
                navbarContainer.setAlpha(0f);
                navbarContainer.animate()
                        .translationY(0f)
                        .alpha(1f)
                        .setDuration(450)
                        .setInterpolator(new AccelerateDecelerateInterpolator())
                        .start();
            }

            Log.d(TAG, "Classy bottom navigation initialized");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing classy bottom navigation: " + e.getMessage(), e);
        }
    }

    // ---------------------------------------------------------------
    //  Tab highlight
    // ---------------------------------------------------------------

    private void highlightCurrentTab() {
        int currentId = getCurrentNavigationItemId();

        // Reset all icons to inactive colour
        tintIcon(navIconHome, COLOR_INACTIVE);
        tintIcon(navIconHistory, COLOR_INACTIVE);
        tintIcon(navIconStats, COLOR_INACTIVE);
        tintIcon(navIconProfile, COLOR_INACTIVE);

        // Hide all dots
        setDotVisible(navDotHome, false);
        setDotVisible(navDotHistory, false);
        setDotVisible(navDotStats, false);
        setDotVisible(navDotProfile, false);

        // Activate the correct one
        if (currentId == R.id.nav_home) {
            tintIcon(navIconHome, COLOR_ACTIVE);
            setDotVisible(navDotHome, true);
        } else if (currentId == R.id.nav_history) {
            tintIcon(navIconHistory, COLOR_ACTIVE);
            setDotVisible(navDotHistory, true);
        } else if (currentId == R.id.nav_stats) {
            tintIcon(navIconStats, COLOR_ACTIVE);
            setDotVisible(navDotStats, true);
        } else if (currentId == R.id.nav_profile) {
            tintIcon(navIconProfile, COLOR_ACTIVE);
            setDotVisible(navDotProfile, true);
        }
        // nav_scan (center) has no dot – it's always prominent
    }

    private void tintIcon(ImageView icon, int color) {
        if (icon != null) {
            icon.setColorFilter(color, android.graphics.PorterDuff.Mode.SRC_IN);
        }
    }

    private void setDotVisible(View dot, boolean visible) {
        if (dot != null) {
            dot.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
        }
    }

    // ---------------------------------------------------------------
    //  Navigation handling
    // ---------------------------------------------------------------

    private void onNavItemClicked(int itemId, View clickedView) {
        int currentId = getCurrentNavigationItemId();
        if (itemId == currentId) {
            // Already on this tab – tiny bounce animation
            try {
                clickedView.startAnimation(
                        AnimationUtils.loadAnimation(this, R.anim.scale_bounce));
            } catch (Exception ignored) {}
            return;
        }

        // Animate the clicked item
        try {
            clickedView.startAnimation(
                    AnimationUtils.loadAnimation(this, R.anim.scale_bounce));
        } catch (Exception ignored) {}

        // Determine destination activity
        Intent intent = null;

        if (itemId == R.id.nav_home) {
            intent = new Intent(this, MainActivity.class);
        } else if (itemId == R.id.nav_history) {
            intent = new Intent(this, HistoryActivity.class);
        } else if (itemId == R.id.nav_scan) {
            intent = new Intent(this, VerticalScannerActivity.class);
        } else if (itemId == R.id.nav_stats) {
            intent = new Intent(this, AnalyticsActivity.class);
        } else if (itemId == R.id.nav_profile) {
            intent = new Intent(this, ProfileActivity.class);
        }

        if (intent != null) {
            intent.putExtra("from_navigation", true);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            overridePendingTransition(R.anim.nav_fade_in, R.anim.nav_fade_out);
        }
    }
}
