package com.example.healthscanner;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.appcompat.app.AppCompatDelegate;

/**
 * Dark Mode Manager for Health Scanner App
 * Handles dark mode preferences and applies them consistently across the app
 */
public class DarkModeManager {
    
    private static final String TAG = "DarkModeManager";
    private static final String PREFS_NAME = "HealthScannerPrefs";
    private static final String KEY_DARK_MODE = "dark_mode_enabled";
    
    private static DarkModeManager instance;
    private final SharedPreferences preferences;
    
    private DarkModeManager(Context context) {
        this.preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    public static synchronized DarkModeManager getInstance(Context context) {
        if (instance == null) {
            instance = new DarkModeManager(context.getApplicationContext());
        }
        return instance;
    }
    
    /**
     * Apply the user's dark mode preference on app startup
     */
    public void applyUserPreference() {
        // Force dark mode for premium smoked-glass dark design system
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        Log.d(TAG, "Forced dark mode (MODE_NIGHT_YES)");
    }
    
    /**
     * Toggle dark mode and save preference
     */
    public void toggleDarkMode(boolean isDarkMode) {
        // Force dark mode regardless of toggle for premium theme design
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        preferences.edit().putBoolean(KEY_DARK_MODE, true).apply();
        Log.d(TAG, "Dark mode toggle ignored, dark mode forced: enabled");
    }
    
    /**
     * Check if dark mode is currently enabled
     */
    public boolean isDarkModeEnabled() {
        return true;
    }
    
    /**
     * Get the current night mode setting
     */
    public int getCurrentNightMode() {
        return AppCompatDelegate.MODE_NIGHT_YES;
    }
    
    /**
     * Apply dark mode based on system setting (follow system)
     */
    public void followSystemSetting() {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        preferences.edit().putBoolean("follow_system_theme", false).apply();
        Log.d(TAG, "Follow system setting ignored, dark mode forced");
    }
    
    /**
     * Check if app is set to follow system theme
     */
    public boolean isFollowingSystemTheme() {
        return false;
    }
}