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
        boolean isDarkModeEnabled = preferences.getBoolean(KEY_DARK_MODE, false);
        int nightMode = isDarkModeEnabled ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;
        
        AppCompatDelegate.setDefaultNightMode(nightMode);
        
        Log.d(TAG, "Applied user dark mode preference: " + (isDarkModeEnabled ? "enabled" : "disabled"));
    }
    
    /**
     * Toggle dark mode and save preference
     */
    public void toggleDarkMode(boolean isDarkMode) {
        int nightMode = isDarkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;
        AppCompatDelegate.setDefaultNightMode(nightMode);
        
        // Save preference
        preferences.edit().putBoolean(KEY_DARK_MODE, isDarkMode).apply();
        
        Log.d(TAG, "Dark mode toggled: " + (isDarkMode ? "enabled" : "disabled"));
    }
    
    /**
     * Check if dark mode is currently enabled
     */
    public boolean isDarkModeEnabled() {
        return preferences.getBoolean(KEY_DARK_MODE, false);
    }
    
    /**
     * Get the current night mode setting
     */
    public int getCurrentNightMode() {
        boolean isDarkMode = isDarkModeEnabled();
        return isDarkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;
    }
    
    /**
     * Apply dark mode based on system setting (follow system)
     */
    public void followSystemSetting() {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        preferences.edit().putBoolean("follow_system_theme", true).apply();
        Log.d(TAG, "Set to follow system dark mode setting");
    }
    
    /**
     * Check if app is set to follow system theme
     */
    public boolean isFollowingSystemTheme() {
        return preferences.getBoolean("follow_system_theme", false);
    }
}