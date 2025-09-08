package com.example.healthscanner;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryActivity extends AppCompatActivity {

    private RecyclerView historyRecyclerView;
    private LinearLayout emptyStateLayout;
    private BottomNavigationView bottomNavigation;
    private HistoryAdapter historyAdapter;
    private List<ScanHistoryItem> historyList;
    private TextView historyCount;

    // Add this field for navigation initialization tracking - SAME PATTERN AS MAINACTIVITY
    private boolean isNavigationInitialized = false;

    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "HealthScannerPrefs";
    private static final String KEY_HISTORY_COUNT = "history_count";
    private static final String KEY_HISTORY_PREFIX = "history_item_";
    private static final int MAX_HISTORY_ITEMS = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply theme before calling super.onCreate()
        ThemeHelper.applyTheme(this);
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        try {
            initializeViews();
            setupSharedPreferences();
            loadHistoryData();
            setupRecyclerView();
            setupBottomNavigation(); // FIXED: Following MainActivity pattern
        } catch (Exception e) {
            Log.e("HistoryActivity", "Error in onCreate: " + e.getMessage(), e);
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // FIXED: Set the correct navigation item when returning to this activity - SAME PATTERN
        if (bottomNavigation != null && isNavigationInitialized) {
            bottomNavigation.setSelectedItemId(R.id.nav_history);
        }

        // Refresh history when returning to this activity
        loadHistoryData();
        updateHistoryCount();
        if (historyAdapter != null) {
            historyAdapter.notifyDataSetChanged();
        }
    }

    private void initializeViews() {
        historyRecyclerView = findViewById(R.id.historyRecyclerView);
        emptyStateLayout = findViewById(R.id.emptyStateText);
        bottomNavigation = findViewById(R.id.bottom_navigation);
        historyCount = findViewById(R.id.historyCount);

        // Check if critical views exist - SAME PATTERN AS MAINACTIVITY
        if (historyRecyclerView == null || emptyStateLayout == null) {
            Log.e("HistoryActivity", "Critical views not found in layout");
            return;
        }

        if (bottomNavigation == null) {
            Log.e("HistoryActivity", "Bottom navigation not found in layout");
        }

        // Setup "Start Scanning" button click listener
        setupStartScanningButton();
    }

    private void setupStartScanningButton() {
        // Find the "Start Scanning" button within the empty state layout
        if (emptyStateLayout != null) {
            android.view.View startScanningButton = findStartScanningButton(emptyStateLayout);
            if (startScanningButton != null) {
                startScanningButton.setOnClickListener(v -> navigateToHome());
            }
        }
    }

    private android.view.View findStartScanningButton(android.view.ViewGroup parent) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            android.view.View child = parent.getChildAt(i);
            if (child instanceof androidx.cardview.widget.CardView) {
                return child; // Found the CardView containing "Start Scanning"
            } else if (child instanceof android.view.ViewGroup) {
                android.view.View result = findStartScanningButton((android.view.ViewGroup) child);
                if (result != null) return result;
            }
        }
        return null;
    }

    private void navigateToHome() {
        Intent homeIntent = new Intent(this, MainActivity.class);
        homeIntent.putExtra("start_scanner", true); // Flag to start scanner immediately
        startActivity(homeIntent);
        // Don't finish() here to maintain back stack
    }

    private void setupSharedPreferences() {
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
    }

    private void loadHistoryData() {
        historyList = new ArrayList<>();
        int historyCount = sharedPreferences.getInt(KEY_HISTORY_COUNT, 0);

        for (int i = 0; i < historyCount && i < MAX_HISTORY_ITEMS; i++) {
            String historyData = sharedPreferences.getString(KEY_HISTORY_PREFIX + i, "");
            if (!historyData.isEmpty()) {
                ScanHistoryItem item = ScanHistoryItem.fromString(historyData);
                if (item != null) {
                    historyList.add(item);
                }
            }
        }

        // Show/hide empty state
        if (historyList.isEmpty()) {
            if (historyRecyclerView != null) {
                historyRecyclerView.setVisibility(View.GONE);
            }
            if (emptyStateLayout != null) {
                emptyStateLayout.setVisibility(View.VISIBLE);
            }
        } else {
            if (historyRecyclerView != null) {
                historyRecyclerView.setVisibility(View.VISIBLE);
            }
            if (emptyStateLayout != null) {
                emptyStateLayout.setVisibility(View.GONE);
            }
        }
    }

    private void setupRecyclerView() {
        historyAdapter = new HistoryAdapter(historyList, this);
        if (historyRecyclerView != null) {
            historyRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            historyRecyclerView.setAdapter(historyAdapter);
        }
        updateHistoryCount();
    }

    // FIXED: setupBottomNavigation method - NOW FOLLOWS EXACT SAME PATTERN AS MAINACTIVITY
    private void setupBottomNavigation() {
        if (bottomNavigation == null) {
            Log.w("HistoryActivity", "Bottom navigation is null, skipping setup");
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
                Intent mainIntent = new Intent(HistoryActivity.this, MainActivity.class);
                startActivity(mainIntent);
                return true;
            } else if (itemId == R.id.nav_history) {
                // Stay on history screen
                return true;
            } else if (itemId == R.id.nav_favorites) {
                // TODO: Navigate to Favorites Activity
                return true;
            } else if (itemId == R.id.nav_profile) {
                // Navigate to Profile Activity
                Intent profileIntent = new Intent(HistoryActivity.this, ProfileActivity.class);
                startActivity(profileIntent);
                return true;
            }

            return false;
        });

        // Set the history item as selected by default AFTER setting up the listener
        bottomNavigation.setSelectedItemId(R.id.nav_history);

        // Mark navigation as initialized
        isNavigationInitialized = true;
    }

    private void updateHistoryCount() {
        if (historyCount != null) {
            int count = historyList != null ? historyList.size() : 0;
            historyCount.setText(count + " item" + (count != 1 ? "s" : ""));
        }
    }

    public void showProductDetailsDialog(ScanHistoryItem item) {
        // Dialog functionality removed - no prompts/popups
    }
    
    private double calculateHealthScore(ScanHistoryItem item) {
        double score = 10.0;
        
        // Deduct points for high sugar
        if (item.getSugar() > 30) {
            score -= 2.0;
        } else if (item.getSugar() > 15) {
            score -= 1.0;
        }
        
        // Deduct points for high calories
        if (item.getCalories() > 200) {
            score -= 1.5;
        } else if (item.getCalories() > 100) {
            score -= 0.5;
        }
        
        // Add points for protein
        if (item.getProtein() > 10) {
            score += 1.0;
        } else if (item.getProtein() > 5) {
            score += 0.5;
        }
        
        return Math.max(0, Math.min(10, score));
    }
    
    private String generateHealthNotes(ScanHistoryItem item, double healthScore) {
        StringBuilder notes = new StringBuilder();
        
        if (item.getSugar() > 30) {
            notes.append("⚠️ High sugar content. Consider healthier alternatives.\n");
        }
        
        if (item.getCalories() > 200) {
            notes.append("🔥 High calorie content. Monitor portion size.\n");
        }
        
        if (item.getProtein() > 10) {
            notes.append("✅ Good protein content.\n");
        }
        
        if (healthScore >= 8) {
            notes.append("🌟 Excellent choice for your health goals!");
        } else if (healthScore >= 6) {
            notes.append("👍 Moderate choice. Consider healthier options.");
        } else {
            notes.append("⚠️ Consider healthier alternatives for better nutrition.");
        }
        
        return notes.toString();
    }
    
    private void addToFavorites(ScanHistoryItem item) {
        try {
            SharedPreferences favoritesPrefs = getSharedPreferences("FavoritesPrefs", MODE_PRIVATE);
            int favoriteCount = favoritesPrefs.getInt("favorite_count", 0);
            
            SharedPreferences.Editor editor = favoritesPrefs.edit();
            editor.putString("favorite_" + favoriteCount, item.getProductName());
            editor.putString("favorite_brand_" + favoriteCount, item.getBrand());
            editor.putString("favorite_barcode_" + favoriteCount, item.getBarcode());
            editor.putInt("favorite_calories_" + favoriteCount, item.getCalories());
            editor.putInt("favorite_sugar_" + favoriteCount, item.getSugar());
            editor.putInt("favorite_protein_" + favoriteCount, item.getProtein());
            editor.putString("favorite_category_" + favoriteCount, item.getCategory());
            editor.putString("favorite_ingredients_" + favoriteCount, item.getIngredients());
            editor.putString("favorite_timestamp_" + favoriteCount, item.getFormattedTime());
            editor.putInt("favorite_count", favoriteCount + 1);
            editor.apply();
            
        } catch (Exception e) {
            Log.e("HistoryActivity", "Error adding to favorites: " + e.getMessage(), e);
        }
    }

    // Static method to add a new scan to history
    public static void addScanToHistory(android.content.Context context, String productName, String brand,
                                        String barcode, String category, String healthRating, 
                                        int calories, int sugar, int protein, String ingredients) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        // Create new history item
        ScanHistoryItem newItem = new ScanHistoryItem(
                productName,
                brand,
                barcode,
                category,
                healthRating,
                System.currentTimeMillis(),
                calories,
                sugar,
                protein,
                ingredients
        );

        // Get current history count
        int historyCount = prefs.getInt(KEY_HISTORY_COUNT, 0);

        // Shift existing items down (move newer items up in the list)
        for (int i = Math.min(historyCount, MAX_HISTORY_ITEMS - 1); i > 0; i--) {
            String prevItem = prefs.getString(KEY_HISTORY_PREFIX + (i - 1), "");
            if (!prevItem.isEmpty()) {
                editor.putString(KEY_HISTORY_PREFIX + i, prevItem);
            }
        }

        // Add new item at position 0 (most recent)
        editor.putString(KEY_HISTORY_PREFIX + "0", newItem.toString());

        // Update history count (max 10)
        int newCount = Math.min(historyCount + 1, MAX_HISTORY_ITEMS);
        editor.putInt(KEY_HISTORY_COUNT, newCount);

        editor.apply();

        // Also update ProfileActivity last scan data if ProfileActivity exists
        try {
            Class.forName("com.example.healthscanner.ProfileActivity");
            ProfileActivity.updateLastScan(context, productName);
        } catch (ClassNotFoundException e) {
            // ProfileActivity doesn't exist, skip updating last scan
        }
    }

    // Inner class for scan history items
    public static class ScanHistoryItem {
        private String productName;
        private String brand;
        private String barcode;
        private String category;
        private String healthRating;
        private long timestamp;
        private int calories;
        private int sugar;
        private int protein;
        private String ingredients;

        public ScanHistoryItem(String productName, String brand, String barcode, String category,
                               String healthRating, long timestamp, int calories, int sugar, int protein, String ingredients) {
            this.productName = productName;
            this.brand = brand;
            this.barcode = barcode;
            this.category = category;
            this.healthRating = healthRating;
            this.timestamp = timestamp;
            this.calories = calories;
            this.sugar = sugar;
            this.protein = protein;
            this.ingredients = ingredients;
        }

        // Getters
        public String getProductName() { return productName; }
        public String getBrand() { return brand; }
        public String getBarcode() { return barcode; }
        public String getCategory() { return category; }
        public String getHealthRating() { return healthRating; }
        public long getTimestamp() { return timestamp; }
        public int getCalories() { return calories; }
        public int getSugar() { return sugar; }
        public int getProtein() { return protein; }
        public String getIngredients() { return ingredients; }

        public String getFormattedTime() {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
            return sdf.format(new Date(timestamp));
        }

        // Convert to string for storage
        @Override
        public String toString() {
            return productName + "|" + brand + "|" + barcode + "|" + category + "|" + healthRating + "|" + timestamp + "|" + calories + "|" + sugar + "|" + protein + "|" + ingredients;
        }

        // Create from string
        public static ScanHistoryItem fromString(String data) {
            try {
                String[] parts = data.split("\\|");
                if (parts.length >= 6) {
                    String productName = parts[0];
                    String brand = parts.length > 1 ? parts[1] : "Unknown Brand";
                    String barcode = parts[2];
                    String category = parts[3];
                    String healthRating = parts[4];
                    long timestamp = Long.parseLong(parts[5]);
                    int calories = parts.length > 6 ? Integer.parseInt(parts[6]) : 0;
                    int sugar = parts.length > 7 ? Integer.parseInt(parts[7]) : 0;
                    int protein = parts.length > 8 ? Integer.parseInt(parts[8]) : 0;
                    String ingredients = parts.length > 9 ? parts[9] : "Ingredients not available";
                    
                    return new ScanHistoryItem(productName, brand, barcode, category, healthRating, timestamp, calories, sugar, protein, ingredients);
                }
            } catch (Exception e) {
                Log.e("ScanHistoryItem", "Error parsing history item: " + e.getMessage());
            }
            return null;
        }

    }
}