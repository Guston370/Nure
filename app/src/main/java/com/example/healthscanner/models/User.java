package com.example.healthscanner.models;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * User model for Firebase Firestore
 * Represents a user in the HealthScanner app
 */
public class User {
    private String uid;
    private String email;
    private String displayName;
    private String photoUrl;
    private String authProvider; // "google.com", "password", etc.
    private Date createdAt;
    private Date lastLoginAt;
    private boolean isEmailVerified;
    
    // Health Statistics
    private int totalScans;
    private int healthyChoices;
    private double averageHealthScore;
    private Date lastScanDate;
    
    // User Preferences
    private boolean notificationsEnabled;
    private boolean darkModeEnabled;
    private String preferredLanguage;
    
    // Profile Information
    private int age;
    private String gender;
    private String dietaryPreferences;
    private String healthGoals;

    // Default constructor required for Firestore
    public User() {}

    // Constructor for new user creation
    public User(String uid, String email, String displayName, String authProvider) {
        this.uid = uid;
        this.email = email;
        this.displayName = displayName;
        this.authProvider = authProvider;
        this.createdAt = new Date();
        this.lastLoginAt = new Date();
        this.isEmailVerified = false;
        
        // Initialize default values
        this.totalScans = 0;
        this.healthyChoices = 0;
        this.averageHealthScore = 0.0;
        this.notificationsEnabled = true;
        this.darkModeEnabled = false;
        this.preferredLanguage = "en";
    }

    // Convert to Map for Firestore
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("uid", uid);
        map.put("email", email);
        map.put("displayName", displayName);
        map.put("photoUrl", photoUrl);
        map.put("authProvider", authProvider);
        map.put("createdAt", createdAt);
        map.put("lastLoginAt", lastLoginAt);
        map.put("isEmailVerified", isEmailVerified);
        
        // Health Statistics
        map.put("totalScans", totalScans);
        map.put("healthyChoices", healthyChoices);
        map.put("averageHealthScore", averageHealthScore);
        map.put("lastScanDate", lastScanDate);
        
        // User Preferences
        map.put("notificationsEnabled", notificationsEnabled);
        map.put("darkModeEnabled", darkModeEnabled);
        map.put("preferredLanguage", preferredLanguage);
        
        // Profile Information
        map.put("age", age);
        map.put("gender", gender);
        map.put("dietaryPreferences", dietaryPreferences);
        map.put("healthGoals", healthGoals);
        
        return map;
    }

    // Getters and Setters
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public String getAuthProvider() { return authProvider; }
    public void setAuthProvider(String authProvider) { this.authProvider = authProvider; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(Date lastLoginAt) { this.lastLoginAt = lastLoginAt; }

    public boolean isEmailVerified() { return isEmailVerified; }
    public void setEmailVerified(boolean emailVerified) { isEmailVerified = emailVerified; }

    public int getTotalScans() { return totalScans; }
    public void setTotalScans(int totalScans) { this.totalScans = totalScans; }

    public int getHealthyChoices() { return healthyChoices; }
    public void setHealthyChoices(int healthyChoices) { this.healthyChoices = healthyChoices; }

    public double getAverageHealthScore() { return averageHealthScore; }
    public void setAverageHealthScore(double averageHealthScore) { this.averageHealthScore = averageHealthScore; }

    public Date getLastScanDate() { return lastScanDate; }
    public void setLastScanDate(Date lastScanDate) { this.lastScanDate = lastScanDate; }

    public boolean isNotificationsEnabled() { return notificationsEnabled; }
    public void setNotificationsEnabled(boolean notificationsEnabled) { this.notificationsEnabled = notificationsEnabled; }

    public boolean isDarkModeEnabled() { return darkModeEnabled; }
    public void setDarkModeEnabled(boolean darkModeEnabled) { this.darkModeEnabled = darkModeEnabled; }

    public String getPreferredLanguage() { return preferredLanguage; }
    public void setPreferredLanguage(String preferredLanguage) { this.preferredLanguage = preferredLanguage; }

    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getDietaryPreferences() { return dietaryPreferences; }
    public void setDietaryPreferences(String dietaryPreferences) { this.dietaryPreferences = dietaryPreferences; }

    public String getHealthGoals() { return healthGoals; }
    public void setHealthGoals(String healthGoals) { this.healthGoals = healthGoals; }
}