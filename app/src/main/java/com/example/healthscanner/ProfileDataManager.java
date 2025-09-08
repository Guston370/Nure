package com.example.healthscanner;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.HashSet;
import java.util.Set;

/**
 * Comprehensive Profile Data Model
 * Handles lifestyle, goals, sustainability, and health preferences
 */
public class ProfileDataManager {
    
    private static final String PREFS_NAME = "HealthScannerPrefs";
    
    // Lifestyle & Goals Keys
    private static final String KEY_MEAL_PREFERENCES = "meal_preferences";
    private static final String KEY_WORKOUT_INTEGRATION = "workout_integration";
    private static final String KEY_WATER_TRACKER_ENABLED = "water_tracker_enabled";
    private static final String KEY_DAILY_WATER_GOAL = "daily_water_goal";
    private static final String KEY_DAILY_WATER_CONSUMED = "daily_water_consumed";
    private static final String KEY_PERSONAL_SCORE = "personal_score";
    
    // Sustainability & Ethics Keys
    private static final String KEY_ECO_SCORE = "eco_score";
    private static final String KEY_CARBON_FOOTPRINT = "carbon_footprint";
    private static final String KEY_ECO_CONSCIOUS_SCANS = "eco_conscious_scans";
    private static final String KEY_TOTAL_SCANS = "total_scans";
    
    // Health & Preferences Keys
    private static final String KEY_DIETARY_PREFERENCES = "dietary_preferences";
    private static final String KEY_ALLERGIES = "allergies";
    private static final String KEY_ALLERGIES_TEXT = "allergies_text";
    private static final String KEY_HEALTH_GOALS = "health_goals";
    private static final String KEY_MEDICAL_CONDITIONS = "medical_conditions";
    private static final String KEY_MEDICAL_CONDITIONS_TEXT = "medical_conditions_text";
    
    // Water tracking
    private static final String KEY_LAST_WATER_RESET = "last_water_reset";
    
    private Context context;
    private SharedPreferences prefs;
    
    public ProfileDataManager(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        checkAndResetWaterIfNeeded();
    }
    
    // ===== LIFESTYLE & GOALS =====
    
    public void setMealPreferences(String preferences) {
        prefs.edit().putString(KEY_MEAL_PREFERENCES, preferences).apply();
    }
    
    public String getMealPreferences() {
        return prefs.getString(KEY_MEAL_PREFERENCES, "Balanced");
    }
    
    public void setWorkoutIntegration(boolean enabled) {
        prefs.edit().putBoolean(KEY_WORKOUT_INTEGRATION, enabled).apply();
    }
    
    public boolean isWorkoutIntegrationEnabled() {
        return prefs.getBoolean(KEY_WORKOUT_INTEGRATION, false);
    }
    
    public void setWaterTrackerEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_WATER_TRACKER_ENABLED, enabled).apply();
    }
    
    public boolean isWaterTrackerEnabled() {
        return prefs.getBoolean(KEY_WATER_TRACKER_ENABLED, true);
    }
    
    public void setDailyWaterGoal(int goal) {
        prefs.edit().putInt(KEY_DAILY_WATER_GOAL, goal).apply();
    }
    
    public int getDailyWaterGoal() {
        return prefs.getInt(KEY_DAILY_WATER_GOAL, 2000); // Default 2L
    }
    
    public void addWaterConsumed(int amount) {
        int current = prefs.getInt(KEY_DAILY_WATER_CONSUMED, 0);
        prefs.edit().putInt(KEY_DAILY_WATER_CONSUMED, current + amount).apply();
    }
    
    public int getWaterConsumed() {
        return prefs.getInt(KEY_DAILY_WATER_CONSUMED, 0);
    }
    
    public int getWaterProgress() {
        int consumed = getWaterConsumed();
        int goal = getDailyWaterGoal();
        return goal > 0 ? Math.min(100, (consumed * 100) / goal) : 0;
    }
    
    public void updatePersonalScore(int points) {
        int current = prefs.getInt(KEY_PERSONAL_SCORE, 0);
        prefs.edit().putInt(KEY_PERSONAL_SCORE, current + points).apply();
    }
    
    public int getPersonalScore() {
        return prefs.getInt(KEY_PERSONAL_SCORE, 0);
    }
    
    // ===== SUSTAINABILITY & ETHICS =====
    
    public void updateEcoScore(int points) {
        int current = prefs.getInt(KEY_ECO_SCORE, 0);
        prefs.edit().putInt(KEY_ECO_SCORE, current + points).apply();
    }
    
    public int getEcoScore() {
        return prefs.getInt(KEY_ECO_SCORE, 0);
    }
    
    public void updateCarbonFootprint(double footprint) {
        float current = prefs.getFloat(KEY_CARBON_FOOTPRINT, 0f);
        prefs.edit().putFloat(KEY_CARBON_FOOTPRINT, current + (float)footprint).apply();
    }
    
    public double getCarbonFootprint() {
        return prefs.getFloat(KEY_CARBON_FOOTPRINT, 0f);
    }
    
    public void incrementEcoConsciousScans() {
        int current = prefs.getInt(KEY_ECO_CONSCIOUS_SCANS, 0);
        prefs.edit().putInt(KEY_ECO_CONSCIOUS_SCANS, current + 1).apply();
    }
    
    public int getEcoConsciousScans() {
        return prefs.getInt(KEY_ECO_CONSCIOUS_SCANS, 0);
    }
    
    public int getTotalScans() {
        return prefs.getInt(KEY_TOTAL_SCANS, 0);
    }
    
    public double getEcoConsciousPercentage() {
        int ecoScans = getEcoConsciousScans();
        int totalScans = getTotalScans();
        return totalScans > 0 ? (ecoScans * 100.0) / totalScans : 0;
    }
    
    // ===== HEALTH & PREFERENCES =====
    
    public void setDietaryPreferences(Set<String> preferences) {
        prefs.edit().putStringSet(KEY_DIETARY_PREFERENCES, preferences).apply();
    }
    
    public Set<String> getDietaryPreferences() {
        return prefs.getStringSet(KEY_DIETARY_PREFERENCES, new HashSet<String>());
    }
    
    public void setAllergies(Set<String> allergies) {
        prefs.edit().putStringSet(KEY_ALLERGIES, allergies).apply();
    }
    
    public Set<String> getAllergies() {
        return prefs.getStringSet(KEY_ALLERGIES, new HashSet<String>());
    }
    
    public String getAllergiesText() {
        return prefs.getString(KEY_ALLERGIES_TEXT, "");
    }
    
    public void setAllergiesText(String allergiesText) {
        prefs.edit().putString(KEY_ALLERGIES_TEXT, allergiesText).apply();
    }
    
    public void setHealthGoals(Set<String> goals) {
        prefs.edit().putStringSet(KEY_HEALTH_GOALS, goals).apply();
    }
    
    public Set<String> getHealthGoals() {
        return prefs.getStringSet(KEY_HEALTH_GOALS, new HashSet<String>());
    }
    
    public void setMedicalConditions(Set<String> conditions) {
        prefs.edit().putStringSet(KEY_MEDICAL_CONDITIONS, conditions).apply();
    }
    
    public Set<String> getMedicalConditions() {
        return prefs.getStringSet(KEY_MEDICAL_CONDITIONS, new HashSet<String>());
    }
    
    public String getMedicalConditionsText() {
        return prefs.getString(KEY_MEDICAL_CONDITIONS_TEXT, "");
    }
    
    public void setMedicalConditionsText(String medicalConditionsText) {
        prefs.edit().putString(KEY_MEDICAL_CONDITIONS_TEXT, medicalConditionsText).apply();
    }
    
    // ===== UTILITY METHODS =====
    
    private void checkAndResetWaterIfNeeded() {
        String today = java.text.SimpleDateFormat.getDateInstance().format(new java.util.Date());
        String lastReset = prefs.getString(KEY_LAST_WATER_RESET, "");
        
        if (!today.equals(lastReset)) {
            prefs.edit()
                .putInt(KEY_DAILY_WATER_CONSUMED, 0)
                .putString(KEY_LAST_WATER_RESET, today)
                .apply();
        }
    }
    
    public void resetDailyWater() {
        prefs.edit().putInt(KEY_DAILY_WATER_CONSUMED, 0).apply();
    }
    
    // ===== SUMMARY METHODS =====
    
    public String getLifestyleSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("🍽️ Meal Preferences: ").append(getMealPreferences()).append("\n");
        summary.append("💪 Workout Integration: ").append(isWorkoutIntegrationEnabled() ? "Enabled" : "Disabled").append("\n");
        summary.append("💧 Water Tracker: ").append(getWaterConsumed()).append("ml / ").append(getDailyWaterGoal()).append("ml (").append(getWaterProgress()).append("%)\n");
        summary.append("⭐ Personal Score: ").append(getPersonalScore()).append(" points");
        return summary.toString();
    }
    
    public String getSustainabilitySummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("🌱 Eco Score: ").append(getEcoScore()).append(" points\n");
        summary.append("🌍 Carbon Footprint: ").append(String.format("%.2f", getCarbonFootprint())).append(" kg CO₂\n");
        summary.append("♻️ Eco-Conscious Scans: ").append(String.format("%.1f", getEcoConsciousPercentage())).append("%");
        return summary.toString();
    }
    
    public String getHealthSummary() {
        StringBuilder summary = new StringBuilder();
        Set<String> dietary = getDietaryPreferences();
        Set<String> allergies = getAllergies();
        Set<String> goals = getHealthGoals();
        Set<String> conditions = getMedicalConditions();
        
        summary.append("🥗 Dietary: ").append(dietary.isEmpty() ? "None set" : String.join(", ", dietary)).append("\n");
        summary.append("⚠️ Allergies: ").append(allergies.isEmpty() ? "None" : String.join(", ", allergies)).append("\n");
        summary.append("🎯 Goals: ").append(goals.isEmpty() ? "None set" : String.join(", ", goals)).append("\n");
        summary.append("🏥 Conditions: ").append(conditions.isEmpty() ? "None" : String.join(", ", conditions));
        return summary.toString();
    }
}

