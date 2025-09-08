package com.example.healthscanner;

import android.content.Context;
import android.content.SharedPreferences;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Daily Nutrition Tracker Utility
 * Handles daily nutrition tracking with automatic reset at 12am
 */
public class DailyNutritionTracker {
    
    private static final String PREFS_NAME = "HealthScannerPrefs";
    private static final String KEY_DAILY_CALORIES = "daily_calories";
    private static final String KEY_DAILY_SUGAR = "daily_sugar";
    private static final String KEY_DAILY_PROTEIN = "daily_protein";
    private static final String KEY_DAILY_FAT = "daily_fat";
    private static final String KEY_DAILY_CARBS = "daily_carbs";
    private static final String KEY_DAILY_SALT = "daily_salt";
    private static final String KEY_LAST_RESET_DATE = "last_reset_date";
    
    // Daily targets
    private static final int TARGET_CALORIES = 2000;
    private static final int TARGET_SUGAR = 50;
    private static final int TARGET_PROTEIN = 100;
    private static final int TARGET_FAT = 65;
    private static final int TARGET_CARBS = 300;
    private static final int TARGET_SALT = 2300;
    
    private Context context;
    private SharedPreferences prefs;
    
    public DailyNutritionTracker(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        checkAndResetIfNeeded();
    }
    
    /**
     * Check if it's a new day and reset values if needed
     */
    private void checkAndResetIfNeeded() {
        String today = getCurrentDateString();
        String lastResetDate = prefs.getString(KEY_LAST_RESET_DATE, "");
        
        if (!today.equals(lastResetDate)) {
            resetDailyValues();
            prefs.edit().putString(KEY_LAST_RESET_DATE, today).apply();
        }
    }
    
    /**
     * Get current date string in YYYY-MM-DD format
     */
    private String getCurrentDateString() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(new Date());
    }
    
    /**
     * Reset all daily nutrition values to zero
     */
    private void resetDailyValues() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_DAILY_CALORIES, 0);
        editor.putInt(KEY_DAILY_SUGAR, 0);
        editor.putInt(KEY_DAILY_PROTEIN, 0);
        editor.putInt(KEY_DAILY_FAT, 0);
        editor.putInt(KEY_DAILY_CARBS, 0);
        editor.putInt(KEY_DAILY_SALT, 0);
        editor.apply();
    }
    
    /**
     * Add nutrition values to today's totals
     */
    public void addNutrition(int calories, int sugar, int protein, int fat, int carbs, int salt) {
        checkAndResetIfNeeded(); // Ensure we're tracking for today
        
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_DAILY_CALORIES, getDailyCalories() + calories);
        editor.putInt(KEY_DAILY_SUGAR, getDailySugar() + sugar);
        editor.putInt(KEY_DAILY_PROTEIN, getDailyProtein() + protein);
        editor.putInt(KEY_DAILY_FAT, getDailyFat() + fat);
        editor.putInt(KEY_DAILY_CARBS, getDailyCarbs() + carbs);
        editor.putInt(KEY_DAILY_SALT, getDailySalt() + salt);
        editor.apply();
    }
    
    /**
     * Get today's nutrition totals
     */
    public int getDailyCalories() {
        checkAndResetIfNeeded();
        return prefs.getInt(KEY_DAILY_CALORIES, 0);
    }
    
    public int getDailySugar() {
        checkAndResetIfNeeded();
        return prefs.getInt(KEY_DAILY_SUGAR, 0);
    }
    
    public int getDailyProtein() {
        checkAndResetIfNeeded();
        return prefs.getInt(KEY_DAILY_PROTEIN, 0);
    }
    
    public int getDailyFat() {
        checkAndResetIfNeeded();
        return prefs.getInt(KEY_DAILY_FAT, 0);
    }
    
    public int getDailyCarbs() {
        checkAndResetIfNeeded();
        return prefs.getInt(KEY_DAILY_CARBS, 0);
    }
    
    public int getDailySalt() {
        checkAndResetIfNeeded();
        return prefs.getInt(KEY_DAILY_SALT, 0);
    }
    
    /**
     * Get nutrition progress as percentage (0-100)
     */
    public int getCaloriesProgress() {
        return Math.min(100, (getDailyCalories() * 100) / TARGET_CALORIES);
    }
    
    public int getSugarProgress() {
        return Math.min(100, (getDailySugar() * 100) / TARGET_SUGAR);
    }
    
    public int getProteinProgress() {
        return Math.min(100, (getDailyProtein() * 100) / TARGET_PROTEIN);
    }
    
    public int getFatProgress() {
        return Math.min(100, (getDailyFat() * 100) / TARGET_FAT);
    }
    
    public int getCarbsProgress() {
        return Math.min(100, (getDailyCarbs() * 100) / TARGET_CARBS);
    }
    
    public int getSaltProgress() {
        return Math.min(100, (getDailySalt() * 100) / TARGET_SALT);
    }
    
    /**
     * Get formatted progress text
     */
    public String getCaloriesProgressText() {
        return getDailyCalories() + " / " + TARGET_CALORIES;
    }
    
    public String getSugarProgressText() {
        return getDailySugar() + "g / " + TARGET_SUGAR + "g";
    }
    
    public String getProteinProgressText() {
        return getDailyProtein() + "g / " + TARGET_PROTEIN + "g";
    }
    
    public String getFatProgressText() {
        return getDailyFat() + "g / " + TARGET_FAT + "g";
    }
    
    public String getCarbsProgressText() {
        return getDailyCarbs() + "g / " + TARGET_CARBS + "g";
    }
    
    public String getSaltProgressText() {
        return getDailySalt() + "mg / " + TARGET_SALT + "mg";
    }
    
    /**
     * Get today's date string for display
     */
    public String getTodayDateString() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault());
        return sdf.format(new Date());
    }
    
    /**
     * Get detailed nutrition summary for today
     */
    public String getTodayNutritionSummary() {
        return String.format(Locale.getDefault(),
            "📊 Today's Nutrition Summary (%s)\n\n" +
            "🔥 Calories: %s (%d%%)\n" +
            "🍯 Sugar: %s (%d%%)\n" +
            "🥩 Protein: %s (%d%%)\n" +
            "🧈 Fat: %s (%d%%)\n" +
            "🍞 Carbs: %s (%d%%)\n" +
            "🧂 Salt: %s (%d%%)",
            getTodayDateString(),
            getCaloriesProgressText(), getCaloriesProgress(),
            getSugarProgressText(), getSugarProgress(),
            getProteinProgressText(), getProteinProgress(),
            getFatProgressText(), getFatProgress(),
            getCarbsProgressText(), getCarbsProgress(),
            getSaltProgressText(), getSaltProgress()
        );
    }
}

