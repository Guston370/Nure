package com.example.healthscanner;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class ProfileActivity extends AppCompatActivity {

    // Profile display views
    private TextView profileName, profileEmail, memberSince;
    private ImageView profileImage, cameraIcon;

    // Statistics views
    private TextView totalScansCount, thisMonthScans, healthScore;

    // Health metrics views
    private TextView heightValue, weightValue, ageValue, bmiValue, bloodTypeValue;

    // Activity views
    private TextView lastScanText, frequentProductsText;

    // Buttons and clickable elements
    private Button editProfileBtn, logoutBtn, exportDataBtn, privacyBtn, helpBtn, aboutBtn, addWaterBtn;
    private TextView updateMetricsBtn, viewAllActivityBtn;

    // Switches
    private SwitchMaterial darkModeSwitch;
    
    // Lifestyle & Goals Views
    private TextView mealPreferencesText, waterProgressText, personalScoreText;
    private SwitchMaterial workoutIntegrationSwitch, waterTrackerSwitch;
    private ProgressBar waterProgressBar;
    
    // Sustainability Views
    private TextView ecoScoreText, carbonFootprintText, ecoConsciousText;
    
    // Health & Preferences Views
    private CheckBox vegetarianCheckbox, veganCheckbox, ketoCheckbox, glutenFreeCheckbox;
    private TextInputEditText allergiesEditText, medicalConditionsEditText;
    private CheckBox weightLossCheckbox, muscleGainCheckbox, balancedDietCheckbox;

    // Bottom Navigation
    private BottomNavigationView bottomNavigation;
    
    // Action Buttons
    private Button saveProfileBtn;
    
    // Haptic feedback
    private Vibrator vibrator;

    // Add this field for navigation initialization tracking - SAME PATTERN AS MAINACTIVITY
    private boolean isNavigationInitialized = false;

    // Profile Data Manager
    private ProfileDataManager profileDataManager;
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "HealthScannerPrefs";
    private static final String KEY_NAME = "user_name";
    private static final String KEY_EMAIL = "user_email";
    private static final String KEY_PHONE = "user_phone";
    private static final String KEY_AGE = "user_age";
    private static final String KEY_WEIGHT = "user_weight";
    private static final String KEY_HEIGHT = "user_height";
    private static final String KEY_BLOOD_TYPE = "user_blood_type";
    private static final String KEY_SCAN_COUNT = "scan_count";
    private static final String KEY_MONTH_SCANS = "month_scans";
    private static final String KEY_HEALTH_SCORE = "health_score";
    private static final String KEY_NOTIFICATIONS = "notifications_enabled";
    private static final String KEY_DARK_MODE = "dark_mode_enabled";
    private static final String KEY_ANALYTICS = "analytics_enabled";
    private static final String KEY_JOIN_DATE = "join_date";
    private static final String KEY_LAST_SCAN = "last_scan_product";
    private static final String KEY_LAST_SCAN_TIME = "last_scan_time";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply theme before calling super.onCreate()
        ThemeHelper.applyTheme(this);
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        try {
            initializeViews();
            initializeHapticFeedback();
            initializeProfileDataManager();
            setupSharedPreferences();
            loadUserData();
            calculateHealthMetrics();
            setupClickListeners();
            setupBottomNavigation(); // FIXED: Following MainActivity pattern
        } catch (Exception e) {
            Log.e("ProfileActivity", "Error in onCreate: " + e.getMessage(), e);
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // FIXED: Set the correct navigation item when returning to this activity - SAME PATTERN
        if (bottomNavigation != null && isNavigationInitialized) {
            bottomNavigation.setSelectedItemId(R.id.nav_profile);
        }

        // Refresh data when returning to profile
        loadUserData();
        calculateHealthMetrics();
    }

    private void initializeViews() {
        // Profile display views
        profileName = findViewById(R.id.profileName);
        profileEmail = findViewById(R.id.profileEmail);
        memberSince = findViewById(R.id.memberSince);
        profileImage = findViewById(R.id.profileImage);
        cameraIcon = findViewById(R.id.cameraIcon);

        // Statistics views
        totalScansCount = findViewById(R.id.totalScansCount);
        thisMonthScans = findViewById(R.id.thisMonthScans);
        healthScore = findViewById(R.id.healthScore);

        // Health metrics views
        heightValue = findViewById(R.id.heightValue);
        weightValue = findViewById(R.id.weightValue);
        ageValue = findViewById(R.id.ageValue);
        bmiValue = findViewById(R.id.bmiValue);
        bloodTypeValue = findViewById(R.id.bloodTypeValue);

        // Activity views
        lastScanText = findViewById(R.id.lastScanText);
        frequentProductsText = findViewById(R.id.frequentProductsText);

        // Buttons and clickable elements
        editProfileBtn = findViewById(R.id.editProfileBtn);
        updateMetricsBtn = findViewById(R.id.updateMetricsBtn);
        viewAllActivityBtn = findViewById(R.id.viewAllActivityBtn);
        exportDataBtn = findViewById(R.id.exportDataBtn);
        privacyBtn = findViewById(R.id.privacyBtn);
        helpBtn = findViewById(R.id.helpBtn);
        aboutBtn = findViewById(R.id.aboutBtn);
        logoutBtn = findViewById(R.id.logoutBtn);
        addWaterBtn = findViewById(R.id.addWaterBtn);

        // Switches
        darkModeSwitch = findViewById(R.id.darkModeSwitch);
        
        // Lifestyle & Goals Views
        mealPreferencesText = findViewById(R.id.mealPreferencesText);
        waterProgressText = findViewById(R.id.waterProgressText);
        personalScoreText = findViewById(R.id.personalScoreText);
        workoutIntegrationSwitch = findViewById(R.id.workoutIntegrationSwitch);
        waterTrackerSwitch = findViewById(R.id.waterTrackerSwitch);
        waterProgressBar = findViewById(R.id.waterProgressBar);
        
        // Sustainability Views
        ecoScoreText = findViewById(R.id.ecoScoreText);
        carbonFootprintText = findViewById(R.id.carbonFootprintText);
        ecoConsciousText = findViewById(R.id.ecoConsciousText);
        
        // Health & Preferences Views
        vegetarianCheckbox = findViewById(R.id.vegetarianCheckbox);
        veganCheckbox = findViewById(R.id.veganCheckbox);
        ketoCheckbox = findViewById(R.id.ketoCheckbox);
        glutenFreeCheckbox = findViewById(R.id.glutenFreeCheckbox);
        allergiesEditText = findViewById(R.id.allergiesEditText);
        medicalConditionsEditText = findViewById(R.id.medicalConditionsEditText);
        weightLossCheckbox = findViewById(R.id.weightLossCheckbox);
        muscleGainCheckbox = findViewById(R.id.muscleGainCheckbox);
        balancedDietCheckbox = findViewById(R.id.balancedDietCheckbox);

        // Bottom Navigation
        bottomNavigation = findViewById(R.id.bottom_navigation);
        
        // Action Buttons
        saveProfileBtn = findViewById(R.id.saveProfileBtn);

        // Check if critical views exist - SAME PATTERN AS MAINACTIVITY
        if (profileName == null || profileEmail == null || totalScansCount == null) {
            Log.e("ProfileActivity", "Critical views not found in layout");
            return;
        }

        if (bottomNavigation == null) {
            Log.e("ProfileActivity", "Bottom navigation not found in layout");
        }
    }
    
    private void initializeProfileDataManager() {
        profileDataManager = new ProfileDataManager(this);
    }
    
    private void initializeHapticFeedback() {
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
    }
    
    private void performHapticFeedback() {
        try {
            if (vibrator != null && vibrator.hasVibrator()) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(50);
                }
            }
        } catch (SecurityException e) {
            Log.w("ProfileActivity", "Vibration permission not granted: " + e.getMessage());
            // Silently fail - haptic feedback is optional
        } catch (Exception e) {
            Log.w("ProfileActivity", "Haptic feedback error: " + e.getMessage());
            // Silently fail - haptic feedback is optional
        }
    }

    private void setupSharedPreferences() {
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
    }

    private void loadUserData() {
        // Load user data from SharedPreferences
        String name = sharedPreferences.getString(KEY_NAME, "Health Scanner User");
        String email = sharedPreferences.getString(KEY_EMAIL, "user@healthscanner.com");
        String height = sharedPreferences.getString(KEY_HEIGHT, "175");
        String weight = sharedPreferences.getString(KEY_WEIGHT, "70");
        String age = sharedPreferences.getString(KEY_AGE, "25");
        String bloodType = sharedPreferences.getString(KEY_BLOOD_TYPE, "O+");

        int totalScans = sharedPreferences.getInt(KEY_SCAN_COUNT, 0);
        int monthScans = sharedPreferences.getInt(KEY_MONTH_SCANS, 0);
        int healthScoreValue = sharedPreferences.getInt(KEY_HEALTH_SCORE, 85);

        long joinDate = sharedPreferences.getLong(KEY_JOIN_DATE, System.currentTimeMillis());
        String lastScan = sharedPreferences.getString(KEY_LAST_SCAN, "No scans yet");
        long lastScanTime = sharedPreferences.getLong(KEY_LAST_SCAN_TIME, 0);

        boolean notificationsEnabled = sharedPreferences.getBoolean(KEY_NOTIFICATIONS, true);
        boolean darkModeEnabled = sharedPreferences.getBoolean(KEY_DARK_MODE, false);
        boolean analyticsEnabled = sharedPreferences.getBoolean(KEY_ANALYTICS, false);

        // Set profile data with null checks
        if (profileName != null) profileName.setText(name);
        if (profileEmail != null) profileEmail.setText(email);

        // Format join date
        if (memberSince != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM yyyy", Locale.getDefault());
            memberSince.setText("Member since " + dateFormat.format(new Date(joinDate)));
        }

        // Set statistics
        if (totalScansCount != null) totalScansCount.setText(String.valueOf(totalScans));
        if (thisMonthScans != null) thisMonthScans.setText(String.valueOf(monthScans));
        if (healthScore != null) healthScore.setText(String.valueOf(healthScoreValue));

        // Set health metrics
        if (heightValue != null) heightValue.setText(height + " cm");
        if (weightValue != null) weightValue.setText(weight + " kg");
        if (ageValue != null) ageValue.setText(age);
        if (bloodTypeValue != null) bloodTypeValue.setText(bloodType);

        // Set activity data
        if (lastScanText != null) {
            if (lastScanTime > 0 && !lastScan.equals("No scans yet")) {
                long timeDiff = System.currentTimeMillis() - lastScanTime;
                String timeAgo = getTimeAgo(timeDiff);
                lastScanText.setText("Last scan: " + lastScan + " - " + timeAgo);
            } else {
                lastScanText.setText("No scans yet - Start scanning to track your health!");
            }
        }

        // Calculate most frequent category (simplified)
        if (frequentProductsText != null) {
            if (totalScans > 0) {
                frequentProductsText.setText("Most scanned category: Health Supplements (45%)");
            } else {
                frequentProductsText.setText("Start scanning to see your preferences");
            }
        }

        // Set switches with null checks
        if (darkModeSwitch != null) darkModeSwitch.setChecked(darkModeEnabled);
        
        // Load lifestyle & goals data
        loadLifestyleData();
        
        // Load sustainability data
        loadSustainabilityData();
        
        // Load health & preferences data
        loadHealthPreferencesData();
    }

    private void loadLifestyleData() {
        if (profileDataManager == null) return;
        
        // Load meal preferences
        if (mealPreferencesText != null) {
            mealPreferencesText.setText(profileDataManager.getMealPreferences());
        }
        
        // Load workout integration
        if (workoutIntegrationSwitch != null) {
            workoutIntegrationSwitch.setChecked(profileDataManager.isWorkoutIntegrationEnabled());
        }
        
        // Load water tracker
        if (waterTrackerSwitch != null) {
            waterTrackerSwitch.setChecked(profileDataManager.isWaterTrackerEnabled());
        }
        
        // Update water progress
        updateWaterProgress();
        
        // Load personal score
        if (personalScoreText != null) {
            personalScoreText.setText(profileDataManager.getPersonalScore() + " points");
        }
    }
    
    private void loadSustainabilityData() {
        if (profileDataManager == null) return;
        
        if (ecoScoreText != null) {
            ecoScoreText.setText(profileDataManager.getEcoScore() + " points");
        }
        
        if (carbonFootprintText != null) {
            carbonFootprintText.setText(String.format("%.2f kg CO₂", profileDataManager.getCarbonFootprint()));
        }
        
        if (ecoConsciousText != null) {
            ecoConsciousText.setText(String.format("%.1f%%", profileDataManager.getEcoConsciousPercentage()));
        }
    }
    
    private void loadHealthPreferencesData() {
        if (profileDataManager == null) return;
        
        // Load dietary preferences
        Set<String> dietaryPreferences = profileDataManager.getDietaryPreferences();
        if (vegetarianCheckbox != null) vegetarianCheckbox.setChecked(dietaryPreferences.contains("Vegetarian"));
        if (veganCheckbox != null) veganCheckbox.setChecked(dietaryPreferences.contains("Vegan"));
        if (ketoCheckbox != null) ketoCheckbox.setChecked(dietaryPreferences.contains("Keto"));
        if (glutenFreeCheckbox != null) glutenFreeCheckbox.setChecked(dietaryPreferences.contains("Gluten-Free"));
        
        // Load allergies as text
        if (allergiesEditText != null) {
            String allergiesText = profileDataManager.getAllergiesText();
            allergiesEditText.setText(allergiesText);
        }
        
        // Load health goals
        Set<String> healthGoals = profileDataManager.getHealthGoals();
        if (weightLossCheckbox != null) weightLossCheckbox.setChecked(healthGoals.contains("Weight Loss"));
        if (muscleGainCheckbox != null) muscleGainCheckbox.setChecked(healthGoals.contains("Muscle Gain"));
        if (balancedDietCheckbox != null) balancedDietCheckbox.setChecked(healthGoals.contains("Balanced Diet"));
        
        // Load medical conditions as text
        if (medicalConditionsEditText != null) {
            String medicalConditionsText = profileDataManager.getMedicalConditionsText();
            medicalConditionsEditText.setText(medicalConditionsText);
        }
    }
    
    private void updateWaterProgress() {
        if (profileDataManager == null) return;
        
        int consumed = profileDataManager.getWaterConsumed();
        int goal = profileDataManager.getDailyWaterGoal();
        int progress = profileDataManager.getWaterProgress();
        
        if (waterProgressText != null) {
            waterProgressText.setText(consumed + "ml / " + goal + "ml (" + progress + "%)");
        }
        
        if (waterProgressBar != null) {
            waterProgressBar.setProgress(progress);
        }
    }

    private void calculateHealthMetrics() {
        if (bmiValue == null) return;

        try {
            String heightStr = sharedPreferences.getString(KEY_HEIGHT, "175");
            String weightStr = sharedPreferences.getString(KEY_WEIGHT, "70");

            double height = Double.parseDouble(heightStr) / 100; // Convert cm to meters
            double weight = Double.parseDouble(weightStr);

            // Calculate BMI
            double bmi = weight / (height * height);
            DecimalFormat df = new DecimalFormat("#.#");
            bmiValue.setText(df.format(bmi));

            // Update BMI color based on range
            if (bmi < 18.5) {
                bmiValue.setTextColor(getColor(android.R.color.holo_orange_dark));
            } else if (bmi >= 18.5 && bmi < 25) {
                bmiValue.setTextColor(getColor(android.R.color.holo_green_dark));
            } else {
                bmiValue.setTextColor(getColor(android.R.color.holo_red_dark));
            }
        } catch (NumberFormatException e) {
            bmiValue.setText("--");
        }
    }

    private String getTimeAgo(long timeDiff) {
        long minutes = timeDiff / (1000 * 60);
        long hours = timeDiff / (1000 * 60 * 60);
        long days = timeDiff / (1000 * 60 * 60 * 24);

        if (minutes < 60) {
            return minutes + " minutes ago";
        } else if (hours < 24) {
            return hours + " hours ago";
        } else {
            return days + " days ago";
        }
    }

    private void setupClickListeners() {
        // Edit Profile Button
        if (editProfileBtn != null) {
            editProfileBtn.setOnClickListener(v -> {
                performHapticFeedback();
                showEditProfileDialog();
            });
        }

        // Update Metrics Button
        if (updateMetricsBtn != null) {
            updateMetricsBtn.setOnClickListener(v -> {
                performHapticFeedback();
                showUpdateMetricsDialog();
            });
        }

        // View All Activity Button
        if (viewAllActivityBtn != null) {
            viewAllActivityBtn.setOnClickListener(v -> {
            });
        }

        // Export Data Button
        if (exportDataBtn != null) {
            exportDataBtn.setOnClickListener(v -> showExportDataDialog());
        }

        // Privacy Button
        if (privacyBtn != null) {
            privacyBtn.setOnClickListener(v -> showPrivacyDialog());
        }

        // Help Button
        if (helpBtn != null) {
            helpBtn.setOnClickListener(v -> showHelpDialog());
        }

        // About Button
        if (aboutBtn != null) {
            aboutBtn.setOnClickListener(v -> showAboutDialog());
        }

        // Add Water Button
        if (addWaterBtn != null) {
            addWaterBtn.setOnClickListener(v -> {
                performHapticFeedback();
                if (profileDataManager != null) {
                    profileDataManager.addWaterConsumed(250);
                    updateWaterProgress();
                }
            });
        }

        // Logout Button
        if (logoutBtn != null) {
            logoutBtn.setOnClickListener(v -> showLogoutDialog());
        }

        // Camera Icon and Profile Image


        // Settings Switches with null checks and proper error handling
        if (darkModeSwitch != null) {
            darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                try {
                    // Use ThemeHelper to toggle dark mode and apply immediately
                    ThemeHelper.toggleDarkMode(this, isChecked);
                    
                    
                    // Optional: Recreate activity to apply theme immediately
                    // Note: The theme will be applied automatically, but recreating ensures all views update
                    recreate();
                    
                } catch (Exception e) {
                    Log.e("ProfileActivity", "Error updating dark mode setting", e);
                    // Revert switch state on error
                    darkModeSwitch.setChecked(!isChecked);
                }
            });
        }

        // Workout Integration Switch
        if (workoutIntegrationSwitch != null) {
            workoutIntegrationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (profileDataManager != null) {
                    profileDataManager.setWorkoutIntegration(isChecked);
                }
            });
        }

        // Water Tracker Switch
        if (waterTrackerSwitch != null) {
            waterTrackerSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (profileDataManager != null) {
                    profileDataManager.setWaterTrackerEnabled(isChecked);
                    updateWaterProgress();
                }
            });
        }

        // Dietary Preferences Checkboxes
        if (vegetarianCheckbox != null) {
            vegetarianCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> saveDietaryPreferences());
        }
        if (veganCheckbox != null) {
            veganCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> saveDietaryPreferences());
        }
        if (ketoCheckbox != null) {
            ketoCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> saveDietaryPreferences());
        }
        if (glutenFreeCheckbox != null) {
            glutenFreeCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> saveDietaryPreferences());
        }

        // Allergies EditText
        if (allergiesEditText != null) {
            allergiesEditText.setOnFocusChangeListener((v, hasFocus) -> {
                if (!hasFocus) {
                    saveAllergiesText();
                }
            });
        }

        // Medical Conditions EditText
        if (medicalConditionsEditText != null) {
            medicalConditionsEditText.setOnFocusChangeListener((v, hasFocus) -> {
                if (!hasFocus) {
                    saveMedicalConditionsText();
                }
            });
        }

        // Health Goals Checkboxes
        if (weightLossCheckbox != null) {
            weightLossCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> saveHealthGoals());
        }
        if (muscleGainCheckbox != null) {
            muscleGainCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> saveHealthGoals());
        }
        if (balancedDietCheckbox != null) {
            balancedDietCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> saveHealthGoals());
        }
        
        // Save Profile Button
        if (saveProfileBtn != null) {
            saveProfileBtn.setOnClickListener(v -> saveAllProfileData());
        }
    }

    private void saveDietaryPreferences() {
        if (profileDataManager == null) return;
        
        Set<String> dietaryPreferences = new HashSet<>();
        if (vegetarianCheckbox != null && vegetarianCheckbox.isChecked()) dietaryPreferences.add("Vegetarian");
        if (veganCheckbox != null && veganCheckbox.isChecked()) dietaryPreferences.add("Vegan");
        if (ketoCheckbox != null && ketoCheckbox.isChecked()) dietaryPreferences.add("Keto");
        if (glutenFreeCheckbox != null && glutenFreeCheckbox.isChecked()) dietaryPreferences.add("Gluten-Free");
        
        profileDataManager.setDietaryPreferences(dietaryPreferences);
    }
    
    private void saveAllergiesText() {
        if (profileDataManager == null || allergiesEditText == null) return;
        
        String allergiesText = allergiesEditText.getText().toString().trim();
        profileDataManager.setAllergiesText(allergiesText);
    }
    
    private void saveMedicalConditionsText() {
        if (profileDataManager == null || medicalConditionsEditText == null) return;
        
        String medicalConditionsText = medicalConditionsEditText.getText().toString().trim();
        profileDataManager.setMedicalConditionsText(medicalConditionsText);
    }
    
    private void saveHealthGoals() {
        if (profileDataManager == null) return;
        
        Set<String> healthGoals = new HashSet<>();
        if (weightLossCheckbox != null && weightLossCheckbox.isChecked()) healthGoals.add("Weight Loss");
        if (muscleGainCheckbox != null && muscleGainCheckbox.isChecked()) healthGoals.add("Muscle Gain");
        if (balancedDietCheckbox != null && balancedDietCheckbox.isChecked()) healthGoals.add("Balanced Diet");
        
        profileDataManager.setHealthGoals(healthGoals);
    }
    
    private void saveAllProfileData() {
        if (profileDataManager == null) return;
        
        // Save all dietary preferences
        saveDietaryPreferences();
        
        // Save allergies text
        saveAllergiesText();
        
        // Save health goals
        saveHealthGoals();
        
        // Save medical conditions text
        saveMedicalConditionsText();
        
        // Provide visual feedback
        if (saveProfileBtn != null) {
            saveProfileBtn.setText("✅ Saved!");
            saveProfileBtn.setEnabled(false);
            
            // Reset button after 2 seconds
            saveProfileBtn.postDelayed(() -> {
                saveProfileBtn.setText("💾 Save Profile");
                saveProfileBtn.setEnabled(true);
            }, 2000);
        }
        
        // Haptic feedback
        if (vibrator != null) {
            vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE));
        }
    }

    private void showEditProfileDialog() {
        // Dialog functionality removed - no prompts/popups
    }

    private void showUpdateMetricsDialog() {
        // Dialog functionality removed - no prompts/popups
    }

    private void showExportDataDialog() {
        // Dialog functionality removed - no prompts/popups
    }

    private void showPrivacyDialog() {
        // Dialog functionality removed - no prompts/popups
    }

    private void showAboutDialog() {
        // Dialog functionality removed - no prompts/popups
    }

    private void showHelpDialog() {
        // Dialog functionality removed - no prompts/popups
    }

    private void showLogoutDialog() {
        // Dialog functionality removed - no prompts/popups
        performLogout();
    }

    private void performLogout() {
        try {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("is_logged_in", false);
            editor.apply();

            Intent loginIntent = new Intent(this, LoginActivity.class);
            loginIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(loginIntent);
            finish();

        } catch (Exception e) {
            Log.e("ProfileActivity", "Error during logout", e);
        }
    }

    // FIXED: setupBottomNavigation method - NOW FOLLOWS EXACT SAME PATTERN AS MAINACTIVITY
    private void setupBottomNavigation() {
        if (bottomNavigation == null) {
            Log.w("ProfileActivity", "Bottom navigation is null, skipping setup");
            return;
        }

        // Set up navigation item selected listener first
        bottomNavigation.setOnItemSelectedListener(item -> {
            // Don't trigger actions during initialization
            if (!isNavigationInitialized) {
                return true;
            }

            int itemId = item.getItemId();

            if (itemId == R.id.nav_scan) {
                // Navigate to MainActivity
                Intent mainIntent = new Intent(ProfileActivity.this, MainActivity.class);
                startActivity(mainIntent);
                return true;
            } else if (itemId == R.id.nav_history) {
                // Navigate to History Activity
                Intent historyIntent = new Intent(ProfileActivity.this, HistoryActivity.class);
                startActivity(historyIntent);
                return true;
            } else if (itemId == R.id.nav_favorites) {
                // TODO: Navigate to Favorites Activity
                return true;
            } else if (itemId == R.id.nav_profile) {
                // Stay on profile screen
                return true;
            }

            return false;
        });

        // Set the profile item as selected by default AFTER setting up the listener
        bottomNavigation.setSelectedItemId(R.id.nav_profile);

        // Mark navigation as initialized
        isNavigationInitialized = true;
    }

    // Static methods for updating data from other activities
    public static void incrementScanCount(android.content.Context context) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            int currentCount = prefs.getInt(KEY_SCAN_COUNT, 0);
            int monthCount = prefs.getInt(KEY_MONTH_SCANS, 0);

            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt(KEY_SCAN_COUNT, currentCount + 1);
            editor.putInt(KEY_MONTH_SCANS, monthCount + 1);
            editor.apply();
        } catch (Exception e) {
            Log.e("ProfileActivity", "Error incrementing scan count", e);
        }
    }

    public static void updateLastScan(android.content.Context context, String productName) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(KEY_LAST_SCAN, productName);
            editor.putLong(KEY_LAST_SCAN_TIME, System.currentTimeMillis());
            editor.apply();
        } catch (Exception e) {
            Log.e("ProfileActivity", "Error updating last scan", e);
        }
    }

    public static void updateHealthScore(android.content.Context context, int newScore) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt(KEY_HEALTH_SCORE, newScore);
            editor.apply();
        } catch (Exception e) {
            Log.e("ProfileActivity", "Error updating health score", e);
        }
    }
}