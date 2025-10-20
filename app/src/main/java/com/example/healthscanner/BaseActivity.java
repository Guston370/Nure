package com.example.healthscanner;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * BaseActivity for unified bottom navigation across the Nure Health Scanner app
 * Provides consistent navigation behavior and parallax depth transitions
 */
public abstract class BaseActivity extends AppCompatActivity {
    
    private static final String TAG = "BaseActivity";
    
    protected BottomNavigationView bottomNavigation;
    protected FrameLayout animationOverlayContainer;
    private boolean isNavigationInitialized = false;
    private int currentNavItemId = -1;
    private int previousNavItemId = -1;
    private ParallaxDepthTransitionHelper transitionHelper;
    private boolean isAnimating = false;
    private boolean isBackNavigation = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Content view should be set by child activities
    }
    
    /**
     * Initialize the bottom navigation after setContentView is called
     */
    protected void initializeBottomNavigation() {
        bottomNavigation = findViewById(R.id.bottom_navigation);
        animationOverlayContainer = findViewById(R.id.animation_overlay_container);
        
        if (bottomNavigation != null) {
            setupBottomNavigation();
            setupParallaxDepthTransitions();
        } else {
            Log.w(TAG, "Bottom navigation view not found in layout");
        }
    }
    
    /**
     * Setup parallax depth transition helper
     */
    private void setupParallaxDepthTransitions() {
        try {
            transitionHelper = new ParallaxDepthTransitionHelper(this);
            
            // Enable hardware acceleration for smooth 60fps performance
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB && !isFinishing() && !isDestroyed()) {
                if (getWindow() != null) {
                    getWindow().setFlags(
                        android.view.WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                        android.view.WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
                }
            }
            
            Log.d(TAG, "Parallax depth transition helper initialized");
        } catch (Exception e) {
            Log.e(TAG, "Error setting up parallax depth transitions: " + e.getMessage(), e);
            // Continue without transitions if setup fails
        }
    }
    
    /**
     * Set up the bottom navigation with unified behavior
     */
    private void setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener(this::onNavigationItemSelected);
        isNavigationInitialized = true;
        setCurrentNavigationItem();
        Log.d(TAG, "Bottom navigation initialized successfully");
    }
    
    /**
     * Handle navigation item selection with animations
     */
    private boolean onNavigationItemSelected(@NonNull MenuItem item) {
        if (!isNavigationInitialized || isAnimating) {
            return false;
        }
        
        int itemId = item.getItemId();
        
        if (itemId == currentNavItemId) {
            return true;
        }
        
        animateNavigationItem(item);
        return handleNavigationActionWithAnimation(itemId);
    }
    
    /**
     * Handle navigation action with parallax depth animation
     */
    private boolean handleNavigationActionWithAnimation(int itemId) {
        Intent intent = null;
        
        if (itemId == R.id.nav_home) {
            if (!(this instanceof MainActivity)) {
                intent = new Intent(this, MainActivity.class);
                intent.putExtra("from_navigation", true);
            }
        } else if (itemId == R.id.nav_scan) {
            // Always launch vertical scanner directly
            intent = new Intent(this, VerticalScannerActivity.class);
        } else if (itemId == R.id.nav_stats) {
            if (!(this instanceof AnalyticsActivity)) {
                intent = new Intent(this, AnalyticsActivity.class);
                intent.putExtra("from_navigation", true);
            }
        } else if (itemId == R.id.nav_history) {
            if (!(this instanceof HistoryActivity)) {
                intent = new Intent(this, HistoryActivity.class);
                intent.putExtra("from_navigation", true);
            }
        } else if (itemId == R.id.nav_profile) {
            if (!(this instanceof ProfileActivity)) {
                intent = new Intent(this, ProfileActivity.class);
                intent.putExtra("from_navigation", true);
            }
        }
        
        updateNavigationTracking(itemId);
        
        if (intent != null) {
            startActivityWithParallaxDepthTransition(intent, itemId);
            return true;
        }
        
        return true;
    }
    
    /**
     * Start activity with parallax depth transition
     */
    private void startActivityWithParallaxDepthTransition(Intent intent, int itemId) {
        if (transitionHelper == null || isFinishing() || isDestroyed()) {
            startActivityWithTransition(intent);
            return;
        }
        
        if (transitionHelper.isAnimating()) {
            Log.w(TAG, "Transition already in progress, skipping");
            return;
        }
        
        try {
            isAnimating = true;
            
            View currentRootView = findViewById(android.R.id.content);
            
            transitionHelper.createForwardDepthTransition(currentRootView, null, () -> {
                if (isFinishing() || isDestroyed()) {
                    isAnimating = false;
                    return;
                }
                
                try {
                    isAnimating = false;
                    intent.putExtra("animate_depth_transition", true);
                    intent.putExtra("is_forward_navigation", true);
                    startActivity(intent);
                    overridePendingTransition(R.anim.depth_slide_in, R.anim.depth_fade_out);
                } catch (Exception e) {
                    Log.e(TAG, "Error starting activity with transition: " + e.getMessage(), e);
                    isAnimating = false;
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error in parallax depth transition: " + e.getMessage(), e);
            isAnimating = false;
            startActivityWithTransition(intent);
        }
    }
    
    /**
     * Start activity with smooth transition animation (fallback)
     */
    private void startActivityWithTransition(Intent intent) {
        startActivity(intent);
        overridePendingTransition(R.anim.nav_slide_in_left, R.anim.nav_fade_out);
    }
    
    /**
     * Animate the selected navigation item
     */
    private void animateNavigationItem(MenuItem item) {
        try {
            View itemView = bottomNavigation.findViewById(item.getItemId());
            if (itemView != null) {
                Animation scaleAnimation = AnimationUtils.loadAnimation(this, R.anim.scale_bounce);
                itemView.startAnimation(scaleAnimation);
                animateNavigationBackground();
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not animate navigation item: " + e.getMessage());
        }
    }
    
    /**
     * Animate the bottom navigation background
     */
    private void animateNavigationBackground() {
        if (bottomNavigation != null) {
            bottomNavigation.animate()
                .scaleY(1.02f)
                .setDuration(150)
                .withEndAction(() -> {
                    bottomNavigation.animate()
                        .scaleY(1.0f)
                        .setDuration(150)
                        .start();
                })
                .start();
        }
    }
    
    /**
     * Get the current navigation item ID
     */
    protected abstract int getCurrentNavigationItemId();
    
    /**
     * Set the current navigation item as selected
     */
    private void setCurrentNavigationItem() {
        int itemId = getCurrentNavigationItemId();
        if (itemId != -1 && bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(itemId);
            updateNavigationTracking(itemId);
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        if (isNavigationInitialized) {
            setCurrentNavigationItem();
        }
        triggerIncomingDepthTransition();
    }
    
    /**
     * Trigger parallax depth transition when activity becomes visible
     */
    private void triggerIncomingDepthTransition() {
        if (transitionHelper == null || isFinishing() || isDestroyed()) {
            return;
        }
        
        try {
            Intent intent = getIntent();
            if (intent != null && intent.getBooleanExtra("animate_depth_transition", false)) {
                
                View rootView = findViewById(android.R.id.content);
                if (rootView == null) return;
                
                boolean isBackNav = intent.getBooleanExtra("is_back_navigation", false);
                boolean isForwardNav = intent.getBooleanExtra("is_forward_navigation", false);
                
                rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }
                        
                        rootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        
                        if (isForwardNav) {
                            rootView.setAlpha(0f);
                            rootView.setScaleX(0.95f);
                            rootView.setScaleY(0.95f);
                            rootView.setTranslationZ(-50f);
                        } else if (isBackNav) {
                            rootView.setAlpha(0f);
                            rootView.setScaleX(1.05f);
                            rootView.setScaleY(1.05f);
                            rootView.setTranslationZ(50f);
                        }
                        
                        rootView.post(() -> {
                            if (isFinishing() || isDestroyed()) {
                                return;
                            }
                            
                            try {
                                if (isBackNav) {
                                    transitionHelper.createBackwardDepthTransition(null, rootView, null);
                                } else {
                                    transitionHelper.createForwardDepthTransition(null, rootView, null);
                                }
                                animateUIElements();
                            } catch (Exception e) {
                                Log.e(TAG, "Error during depth transition: " + e.getMessage(), e);
                            }
                        });
                        
                        intent.removeExtra("animate_depth_transition");
                        intent.removeExtra("is_back_navigation");
                        intent.removeExtra("is_forward_navigation");
                    }
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Error triggering incoming depth transition: " + e.getMessage(), e);
        }
    }
    
    /**
     * Animate UI elements with staggered entrance
     */
    private void animateUIElements() {
        if (transitionHelper == null) return;
        
        if (bottomNavigation != null) {
            transitionHelper.createElementEntranceAnimation(bottomNavigation, 100);
        }
        
        View headerSection = findViewById(R.id.headerSection);
        if (headerSection != null) {
            transitionHelper.createElementEntranceAnimation(headerSection, 150);
        }
        
        View searchCard = findViewById(R.id.searchCard);
        if (searchCard != null) {
            transitionHelper.createElementEntranceAnimation(searchCard, 200);
        }
        
        View statsContainer = findViewById(R.id.statsContainer);
        if (statsContainer != null) {
            transitionHelper.createElementEntranceAnimation(statsContainer, 250);
        }
    }
    
    /**
     * Handle back button press with parallax depth animation
     */
    @Override
    @SuppressWarnings("MissingSuperCall")
    public void onBackPressed() {
        if (isAnimating) {
            return;
        }
        
        int targetNavItemId = determineBackNavigationTarget();
        
        if (targetNavItemId != -1) {
            performBackNavigationWithAnimation(targetNavItemId);
        } else {
            performDefaultBackNavigation();
        }
    }
    
    /**
     * Determine the target navigation item for back navigation
     */
    private int determineBackNavigationTarget() {
        int currentItemId = getCurrentNavigationItemId();
        
        if (currentItemId == R.id.nav_scan || 
            currentItemId == R.id.nav_stats || 
            currentItemId == R.id.nav_history || 
            currentItemId == R.id.nav_profile) {
            return R.id.nav_home;
        } else if (currentItemId == R.id.nav_home) {
            return -1;
        } else {
            return -1;
        }
    }
    
    /**
     * Perform back navigation with parallax depth animation
     */
    private void performBackNavigationWithAnimation(int targetNavItemId) {
        if (transitionHelper == null) {
            navigateToTargetActivity(targetNavItemId);
            return;
        }
        
        if (transitionHelper.isAnimating()) {
            Log.w(TAG, "Transition already in progress, skipping");
            return;
        }
        
        isAnimating = true;
        isBackNavigation = true;
        
        View currentRootView = findViewById(android.R.id.content);
        
        transitionHelper.createBackwardDepthTransition(currentRootView, null, () -> {
            isAnimating = false;
            isBackNavigation = false;
            navigateToTargetActivity(targetNavItemId);
        });
    }
    
    /**
     * Navigate to the target activity for back navigation
     */
    private void navigateToTargetActivity(int targetNavItemId) {
        Intent intent = null;
        
        if (targetNavItemId == R.id.nav_home) {
            if (!(this instanceof MainActivity)) {
                intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                intent.putExtra("from_navigation", true);
            }
        }
        
        if (intent != null) {
            intent.putExtra("animate_depth_transition", true);
            intent.putExtra("is_back_navigation", true);
            startActivity(intent);
            overridePendingTransition(R.anim.depth_slide_in_back, R.anim.depth_fade_out_back);
        }
    }
    
    /**
     * Perform default back navigation
     */
    private void performDefaultBackNavigation() {
        if (transitionHelper != null) {
            View rootView = findViewById(android.R.id.content);
            transitionHelper.createBackwardDepthTransition(rootView, null, () -> {
                BaseActivity.super.onBackPressed();
            });
        } else {
            super.onBackPressed();
        }
    }
    
    /**
     * Update navigation tracking
     */
    private void updateNavigationTracking(int newItemId) {
        previousNavItemId = currentNavItemId;
        currentNavItemId = newItemId;
    }
    
    /**
     * Fragment-based navigation helper
     */
    protected void navigateToFragment(Fragment fragment, String tag, int containerId) {
        if (fragment == null) {
            Log.w(TAG, "Cannot navigate to null fragment");
            return;
        }
        
        FragmentManager fragmentManager = getSupportFragmentManager();
        
        Fragment currentFragment = fragmentManager.findFragmentByTag(tag);
        if (currentFragment != null && currentFragment.isVisible()) {
            Log.d(TAG, "Fragment " + tag + " is already visible");
            return;
        }
        
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.setCustomAnimations(
            R.anim.depth_slide_in,
            R.anim.depth_fade_out,
            R.anim.depth_slide_in_back,
            R.anim.depth_fade_out_back
        );
        
        transaction.replace(containerId, fragment, tag);
        transaction.addToBackStack(tag);
        transaction.commitAllowingStateLoss();
        
        Log.d(TAG, "Navigated to fragment: " + tag);
    }
    
    /**
     * Update the bottom navigation selection programmatically
     */
    protected void updateNavigationSelection(int itemId) {
        if (bottomNavigation != null && isNavigationInitialized) {
            bottomNavigation.setSelectedItemId(itemId);
            currentNavItemId = itemId;
        }
    }
    
    /**
     * Show or hide the bottom navigation
     */
    protected void setBottomNavigationVisibility(boolean visible) {
        if (bottomNavigation != null) {
            bottomNavigation.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }
    
    /**
     * Get the bottom navigation view
     */
    protected BottomNavigationView getBottomNavigation() {
        return bottomNavigation;
    }
}