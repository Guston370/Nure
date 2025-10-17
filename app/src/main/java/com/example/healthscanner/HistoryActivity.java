package com.example.healthscanner;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import android.content.Intent;
import android.util.Log;

/**
 * History Activity for displaying scan history
 * Shows previously scanned products and their nutritional information
 */
public class HistoryActivity extends BaseActivity {
    
    private static final String TAG = "HistoryActivity";
    private static final String PREFS_NAME = "HealthScannerPrefs";
    private static final String KEY_SCAN_HISTORY = "scan_history";
    
    private BottomNavigationView bottomNavigation;
    private TextView historyContent;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        
        // Check authentication
        AuthManager authManager = AuthManager.getInstance(this);
        if (!authManager.isUserAuthenticated()) {
            authManager.navigateToLogin(this);
            return;
        }
        
        initializeViews();
        initializeBottomNavigation();
        loadScanHistory();
    }
    
    private void initializeViews() {
        bottomNavigation = findViewById(R.id.bottom_navigation);
        historyContent = findViewById(R.id.history_content);
    }
    
    private void setupBottomNavigation() {
        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.nav_history);
            
            bottomNavigation.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_scan) {
                    Intent scanIntent = new Intent(this, MainActivity.class);
                    startActivity(scanIntent);
                    finish();
                    return true;
                } else if (itemId == R.id.nav_history) {
                    // Already on history page
                    return true;
                } else if (itemId == R.id.nav_profile) {
                    Intent profileIntent = new Intent(this, ProfileActivity.class);
                    startActivity(profileIntent);
                    return true;
                }
                return false;
            });
        }
    }
    
    private void loadScanHistory() {
        // For now, show a simple message
        if (historyContent != null) {
            historyContent.setText("📊 Scan History\n\nYour scan history will appear here.\nStart scanning products to build your history!");
        }
    }
    
    @Override
    protected int getCurrentNavigationItemId() {
        return R.id.nav_history;
    }
    
    /**
     * Add a scan to history (static method for easy access from other activities)
     * @param context Application context
     * @param productName Product name
     * @param brand Product brand
     * @param barcode Product barcode
     * @param category Product category
     * @param healthRating Health rating
     * @param calories Calories per 100g
     * @param sugar Sugar content
     * @param protein Protein content
     * @param ingredients Ingredients list
     */
    public static void addScanToHistory(Context context, String productName, String brand, 
                                      String barcode, String category, String healthRating,
                                      int calories, int sugar, int protein, String ingredients) {
        try {
            // For now, just log the scan
            Log.d("HistoryActivity", "Scan added to history: " + productName + " (" + brand + ")");
            Log.d("HistoryActivity", "Nutrition: " + calories + " cal, " + sugar + "g sugar, " + protein + "g protein");
            
            // TODO: Implement actual history storage using SharedPreferences or database
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            // Add implementation for storing scan history
            
        } catch (Exception e) {
            Log.e("HistoryActivity", "Error adding scan to history: " + e.getMessage());
        }
    }
}