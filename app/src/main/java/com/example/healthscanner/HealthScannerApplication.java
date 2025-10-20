package com.example.healthscanner;

import android.app.Application;
import android.util.Log;

/**
 * Application class for Health Scanner
 * Handles app-wide initialization including dark mode preferences
 */
public class HealthScannerApplication extends Application {
    
    private static final String TAG = "HealthScannerApp";
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        Log.d(TAG, "Health Scanner Application starting...");
        
        // Apply dark mode preference as early as possible
        DarkModeManager darkModeManager = DarkModeManager.getInstance(this);
        darkModeManager.applyUserPreference();
        
        Log.d(TAG, "Dark mode preference applied on app startup");
    }
}