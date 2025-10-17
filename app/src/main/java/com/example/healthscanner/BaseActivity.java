package com.example.healthscanner;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * BaseActivity for unified bottom navigation across the Nure Health Scanner app
 * Provides consistent navigation behavior and animations
 */
public abstract class BaseActivity extends AppCompatActivity {
    
    private static final String TAG = "BaseActivity";
    
    protected BottomNavigationView bottomNavigation;
    private boolean isNavigationInitialized = false;
    private int currentNavItemId = -1;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Content view should be set by child activities
    }
    
    /**
     * Initialize the bottom navigation after setContentView is called
     * Should be called by child activities in their onCreate method
     */
    protected void initializeBottomNavigation() {
        bottomNavigation = findViewById(R.id.bottom_navigation);
        if (bottomNavigation != null) {
            setupBottomNavigation();
        } else {
            Log.w(TAG, "Bottom navigation view not found in layout");
        }
    }
    
    /**
     * Set up the bottom navigation with unified behavior
     */
    private void setupBottomNavigation() {
        // Set up navigation item selected listener
        bottomNavigation.setOnItemSelectedListener(this::onNavigationItemSelected);
        
        // Mark navigation as initialized
        isNavigationInitialized = true;
        
        // Set the correct item as selected based on current activity
        setCurrentNavigationItem();
        
        Log.d(TAG, "Bottom navigation initialized successfully");
    }
    
    /**
     * Handle navigation item selection with animations and crash-safe logic
     */
    private boolean onNavigationItemSelected(@NonNull MenuItem item) {
        if (!isNavigationInitialized) {
            return false;
        }
        
        int itemId = item.getItemId();
        
        // Prevent reselection of current item
        if (itemId == currentNavItemId) {
            return true;
        }
        
        // Add scale animation to selected item
        animateNavigationItem(item);
        
        // Handle navigation based on item ID
        return handleNavigationAction(itemId);
    }
    
    /**
     * Handle the actual navigation action
     */
    private boolean handleNavigationAction(int itemId) {
        Intent intent = null;
        
        if (itemId == R.id.nav_home) {
            if (!(this instanceof MainActivity)) {
                intent = new Intent(this, MainActivity.class);
            }
        } else if (itemId == R.id.nav_scan) {
            if (!(this instanceof MainActivity)) {
                intent = new Intent(this, MainActivity.class);
                intent.putExtra("start_scanner", true);
            }
        } else if (itemId == R.id.nav_stats) {
            // TODO: Create StatsActivity when implemented
            Log.d(TAG, "Stats navigation - not yet implemented");
            return false;
        } else if (itemId == R.id.nav_history) {
            if (!(this instanceof HistoryActivity)) {
                intent = new Intent(this, HistoryActivity.class);
            }
        } else if (itemId == R.id.nav_profile) {
            if (!(this instanceof ProfileActivity)) {
                intent = new Intent(this, ProfileActivity.class);
            }
        }
        
        // Start activity with transition animation if needed
        if (intent != null) {
            startActivityWithTransition(intent);
            return true;
        }
        
        return true;
    }
    
    /**
     * Start activity with smooth transition animation
     */
    private void startActivityWithTransition(Intent intent) {
        startActivity(intent);
        // Apply custom transition animations
        overridePendingTransition(R.anim.nav_slide_in_left, R.anim.nav_fade_out);
    }
    
    /**
     * Animate the selected navigation item
     */
    private void animateNavigationItem(MenuItem item) {
        try {
            // Find the view for the menu item and apply scale animation
            View itemView = bottomNavigation.findViewById(item.getItemId());
            if (itemView != null) {
                Animation scaleAnimation = AnimationUtils.loadAnimation(this, R.anim.nav_scale_up);
                itemView.startAnimation(scaleAnimation);
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not animate navigation item: " + e.getMessage());
        }
    }
    
    /**
     * Set the current navigation item based on the activity type
     * Should be overridden by child activities to set the correct item
     */
    protected abstract int getCurrentNavigationItemId();
    
    /**
     * Set the current navigation item as selected
     */
    private void setCurrentNavigationItem() {
        int itemId = getCurrentNavigationItemId();
        if (itemId != -1 && bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(itemId);
            currentNavItemId = itemId;
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Ensure correct navigation item is selected when returning to activity
        if (isNavigationInitialized) {
            setCurrentNavigationItem();
        }
    }
    
    /**
     * Fragment-based navigation helper for activities that use fragments
     * Note: Activities using this method should have a fragment container with ID 'fragment_container'
     */
    protected void navigateToFragment(Fragment fragment, String tag, int containerId) {
        if (fragment == null) {
            Log.w(TAG, "Cannot navigate to null fragment");
            return;
        }
        
        FragmentManager fragmentManager = getSupportFragmentManager();
        
        // Check if fragment is already visible to prevent stacking
        Fragment currentFragment = fragmentManager.findFragmentByTag(tag);
        if (currentFragment != null && currentFragment.isVisible()) {
            Log.d(TAG, "Fragment " + tag + " is already visible");
            return;
        }
        
        // Perform fragment transaction with animation
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.setCustomAnimations(
            R.anim.nav_fade_in,
            R.anim.nav_fade_out,
            R.anim.nav_fade_in,
            R.anim.nav_fade_out
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
     * Get the bottom navigation view for custom modifications
     */
    protected BottomNavigationView getBottomNavigation() {
        return bottomNavigation;
    }
}