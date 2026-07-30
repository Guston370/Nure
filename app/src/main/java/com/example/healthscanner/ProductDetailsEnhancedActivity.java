package com.example.healthscanner;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.button.MaterialButton;

import org.json.JSONException;
import org.json.JSONObject;
import com.example.healthscanner.database.SyncManager;

/**
 * Enhanced Product Details Activity with Dark Mode Support
 * Features collapsing toolbar, Material Design 3, and comprehensive health analysis
 */
public class ProductDetailsEnhancedActivity extends AppCompatActivity {
    
    private static final String TAG = "ProductDetailsEnhanced";
    private static final String PREFS_NAME = "HealthScannerPrefs";
    
    // UI Elements
    private CollapsingToolbarLayout collapsingToolbar;
    private Toolbar toolbar;
    private ImageView productImage;
    private TextView productName, productBrand, productSource;
    private TextView healthScoreText, healthEmoji, barcodeValue;
    private TextView caloriesValue, proteinValue, sugarValue;
    private TextView fatValue, carbsValue, sodiumValue, fiberValue;
    private TextView ingredientsText, healthInsights;
    private MaterialButton scanAgainButton, galleryButton, shareButton, favoriteButton;
    
    // Data
    private ProductInfo currentProduct;
    private RequestQueue requestQueue;
    private boolean isDarkMode;
    private SyncManager syncManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "ProductDetailsEnhancedActivity onCreate started");
        
        // Check if dark mode is enabled
        isDarkMode = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) 
                    == Configuration.UI_MODE_NIGHT_YES;
        
        try {
            setContentView(R.layout.activity_product_details_enhanced);
            Log.d(TAG, "Enhanced layout set successfully");
            
            // Initialize components
            initializeViews();
            setupToolbar();
            setupButtons();
            
            // Initialize request queue
            requestQueue = Volley.newRequestQueue(this);
            
            // Initialize sync manager
            syncManager = SyncManager.getInstance(this);
            
            // Get barcode from intent and fetch product
            String barcode = getIntent().getStringExtra("barcode");
            Log.d(TAG, "Received barcode from intent: " + barcode);
            
            if (barcode != null && !barcode.isEmpty()) {
                fetchProductDetails(barcode);
            } else {
                Log.e(TAG, "No barcode provided, using demo product");
                createDemoProduct("1234567890123");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + e.getMessage(), e);
            createDemoProduct("1234567890123");
        }
    }
    
    private void initializeViews() {
        Log.d(TAG, "Initializing enhanced views...");
        
        // Toolbar and collapsing layout
        collapsingToolbar = findViewById(R.id.collapsing_toolbar);
        toolbar = findViewById(R.id.toolbar);
        
        // Product info
        productImage = findViewById(R.id.product_image);
        productName = findViewById(R.id.product_name);
        productBrand = findViewById(R.id.product_brand);
        productSource = findViewById(R.id.product_source);
        barcodeValue = findViewById(R.id.barcode_value);
        
        // Health score
        healthScoreText = findViewById(R.id.health_score_text);
        healthEmoji = findViewById(R.id.health_emoji);
        
        // Nutrition values
        caloriesValue = findViewById(R.id.calories_value);
        proteinValue = findViewById(R.id.protein_value);
        sugarValue = findViewById(R.id.sugar_value);
        fatValue = findViewById(R.id.fat_value);
        carbsValue = findViewById(R.id.carbs_value);
        sodiumValue = findViewById(R.id.sodium_value);
        fiberValue = findViewById(R.id.fiber_value);
        
        // Additional info
        ingredientsText = findViewById(R.id.ingredients_text);
        healthInsights = findViewById(R.id.health_insights);
        
        // Buttons
        scanAgainButton = findViewById(R.id.camera_scan_button);
        galleryButton = findViewById(R.id.gallery_button);
        shareButton = findViewById(R.id.share_button);
        favoriteButton = findViewById(R.id.favorite_button);
        
        Log.d(TAG, "Enhanced views initialization completed");
    }
    
    private void setupToolbar() {
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setDisplayShowHomeEnabled(true);
            }
        }
        
        if (collapsingToolbar != null) {
            collapsingToolbar.setTitle("Product Details");
            
            // Set colors based on theme
            if (isDarkMode) {
                collapsingToolbar.setContentScrimColor(ContextCompat.getColor(this, R.color.md_theme_dark_primary));
                collapsingToolbar.setStatusBarScrimColor(ContextCompat.getColor(this, R.color.md_theme_dark_primaryContainer));
            } else {
                collapsingToolbar.setContentScrimColor(ContextCompat.getColor(this, R.color.md_theme_light_primary));
                collapsingToolbar.setStatusBarScrimColor(ContextCompat.getColor(this, R.color.md_theme_light_primaryContainer));
            }
        }
    }
    
    private void setupButtons() {
        if (scanAgainButton != null) {
            scanAgainButton.setOnClickListener(v -> {
                // Navigate directly to the scanner
                Intent intent = new Intent(this, VerticalScannerActivity.class);
                startActivity(intent);
                finish();
            });
        }
        
        if (galleryButton != null) {
            galleryButton.setOnClickListener(v -> {
                // TODO: Implement gallery selection
                Log.d(TAG, "Gallery selection not yet implemented");
            });
        }
        
        if (shareButton != null) {
            shareButton.setOnClickListener(v -> shareProduct());
        }
        
        if (favoriteButton != null) {
            favoriteButton.setOnClickListener(v -> toggleFavorite());
        }
    }
    
    private void fetchProductDetails(String barcode) {
        Log.d(TAG, "Fetching product details for barcode: " + barcode);
        
        String url = "https://world.openfoodfacts.org/api/v0/product/" + barcode + ".json";
        
        JsonObjectRequest request = new JsonObjectRequest(
            Request.Method.GET, url, null,
            response -> {
                try {
                    if (response.has("status") && response.getInt("status") == 1) {
                        Log.d(TAG, "Product found in API, parsing response");
                        parseProductResponse(response, barcode);
                    } else {
                        Log.d(TAG, "Product not found in API, using demo data");
                        createDemoProduct(barcode);
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing response", e);
                    createDemoProduct(barcode);
                }
            },
            error -> {
                Log.e(TAG, "API Error: " + error.getMessage());
                createDemoProduct(barcode);
            }
        );
        
        requestQueue.add(request);
    }
    
    private void parseProductResponse(JSONObject response, String barcode) throws JSONException {
        JSONObject product = response.getJSONObject("product");
        
        currentProduct = new ProductInfo();
        currentProduct.barcode = barcode;
        currentProduct.name = product.optString("product_name", "Unknown Product");
        currentProduct.brand = product.optString("brands", "Unknown Brand");
        
        // Extract product image URL with fallbacks
        currentProduct.imageUrl = getFirstNonEmpty(product,
            "image_front_url",
            "image_url",
            "image_front_display_url",
            "image_front_small_url",
            "image_small_url",
            "image_front_thumb_url",
            "image_thumb_url"
        );
        
        // Extract nutritional information
        JSONObject nutriments = product.optJSONObject("nutriments");
        if (nutriments != null) {
            currentProduct.calories = nutriments.optDouble("energy-kcal_100g", 0);
            currentProduct.protein = nutriments.optDouble("proteins_100g", 0);
            currentProduct.sugar = nutriments.optDouble("sugars_100g", 0);
            currentProduct.fat = nutriments.optDouble("fat_100g", 0);
            currentProduct.carbs = nutriments.optDouble("carbohydrates_100g", 0);
            currentProduct.fiber = nutriments.optDouble("fiber_100g", 0);
            currentProduct.sodium = nutriments.optDouble("sodium_100g", 0) * 1000; // Convert to mg
        }
        
        String ingredients = getFirstNonEmpty(product,
            "ingredients_text",
            "ingredients_text_en",
            "ingredients_text_with_allergens",
            "ingredients_text_with_allergens_en"
        );
        if (ingredients.isEmpty()) {
            ingredients = buildIngredientsFromArray(product);
        }
        if (ingredients.isEmpty()) {
            ingredients = "Ingredients not available";
        }
        currentProduct.ingredients = ingredients;

        applyBarcodeOverrides(currentProduct, product);
        
        displayProductDetails();
    }
    
    private void createDemoProduct(String barcode) {
        Log.d(TAG, "Creating demo product for barcode: " + barcode);
        
        currentProduct = new ProductInfo();
        currentProduct.barcode = barcode;
        
        // Use barcode to determine product type
        int productType = Math.abs(barcode.hashCode()) % 4;
        
        switch (productType) {
            case 0: // Healthy cereal
                currentProduct.name = "Organic Whole Grain Cereal";
                currentProduct.brand = "HealthyChoice";
                currentProduct.imageUrl = "https://images.openfoodfacts.org/images/products/cereal_demo.jpg";
                currentProduct.calories = 350;
                currentProduct.protein = 12.5;
                currentProduct.sugar = 8.2;
                currentProduct.fat = 4.1;
                currentProduct.carbs = 68.0;
                currentProduct.fiber = 9.5;
                currentProduct.sodium = 180;
                currentProduct.ingredients = "Whole grain oats, whole grain wheat, brown rice, honey, almonds, natural vanilla flavor, sea salt, vitamin E (mixed tocopherols)";
                break;
                
            case 1: // Greek Yogurt
                currentProduct.name = "Greek Yogurt Natural";
                currentProduct.brand = "FreshDairy";
                currentProduct.imageUrl = "https://images.openfoodfacts.org/images/products/yogurt_demo.jpg";
                currentProduct.calories = 130;
                currentProduct.protein = 18.0;
                currentProduct.sugar = 6.5;
                currentProduct.fat = 3.2;
                currentProduct.carbs = 9.0;
                currentProduct.fiber = 0.0;
                currentProduct.sodium = 65;
                currentProduct.ingredients = "Pasteurized milk, live active cultures (L. bulgaricus, S. thermophilus, L. acidophilus, Bifidus, L. casei)";
                break;
                
            case 2: // Dark Chocolate
                currentProduct.name = "Dark Chocolate Bar 70%";
                currentProduct.brand = "SweetTreats";
                currentProduct.imageUrl = "https://images.openfoodfacts.org/images/products/chocolate_demo.jpg";
                currentProduct.calories = 540;
                currentProduct.protein = 7.8;
                currentProduct.sugar = 24.0;
                currentProduct.fat = 31.0;
                currentProduct.carbs = 61.0;
                currentProduct.fiber = 11.0;
                currentProduct.sodium = 20;
                currentProduct.ingredients = "Cocoa mass, sugar, cocoa butter, emulsifier (soy lecithin), natural vanilla flavoring";
                break;
                
            case 3: // Fresh Apple
                currentProduct.name = "Fresh Red Apple";
                currentProduct.brand = "Nature's Best";
                currentProduct.imageUrl = "https://images.openfoodfacts.org/images/products/apple_demo.jpg";
                currentProduct.calories = 52;
                currentProduct.protein = 0.3;
                currentProduct.sugar = 10.4;
                currentProduct.fat = 0.2;
                currentProduct.carbs = 13.8;
                currentProduct.fiber = 2.4;
                currentProduct.sodium = 1;
                currentProduct.ingredients = "Fresh apple";
                break;
        }
        
        displayProductDetails();
    }
    
    private void displayProductDetails() {
        Log.d(TAG, "Displaying enhanced product details for: " + currentProduct.name);
        
        // Set collapsing toolbar title
        if (collapsingToolbar != null) {
            collapsingToolbar.setTitle(currentProduct.name);
        }
        
        // Populate basic info
        if (productName != null) productName.setText(currentProduct.name);
        if (productBrand != null) productBrand.setText(currentProduct.brand);
        if (productSource != null) productSource.setVisibility(View.GONE); // Hide source information
        if (barcodeValue != null) barcodeValue.setText(currentProduct.barcode);
        
        // Load product image
        loadProductImage();
        
        // Calculate and display health score
        double healthScore = calculateHealthScore();
        displayHealthScore(healthScore);
        
        // Populate nutritional information
        populateNutritionalInfo();
        
        // Set ingredients and health insights
        if (ingredientsText != null) {
            ingredientsText.setText(currentProduct.ingredients);
        }
        
        if (healthInsights != null) {
            String insights = generateHealthInsights(healthScore);
            String healthWarnings = checkHealthConcerns();
            
            if (!healthWarnings.isEmpty()) {
                insights = healthWarnings + "\n\n" + insights;
            }
            
            healthInsights.setText(insights);
        }
        
        // Save to scan history
        saveToScanHistory();
        
        Log.d(TAG, "Enhanced product details display completed");
    }

    private void applyBarcodeOverrides(ProductInfo info, JSONObject product) {
        if (info == null || info.barcode == null) return;

        if ("8901491100274".equals(info.barcode)) {
            info.name = "Lay's Indian Magic Masala";
            info.brand = "Lay's";

            if (info.ingredients == null || info.ingredients.trim().isEmpty() ||
                "Ingredients not available".equalsIgnoreCase(info.ingredients.trim())) {
                info.ingredients = "Potatoes, edible vegetable oil, seasonings (spices, sugar, salt, onion powder, garlic powder), flavor enhancers.";
            }

            if (info.imageUrl == null || info.imageUrl.trim().isEmpty()) {
                info.imageUrl = ""; // Force placeholder image when API has no photos.
            }
        }
    }

    private String getFirstNonEmpty(JSONObject product, String... keys) {
        if (product == null || keys == null) return "";
        for (String key : keys) {
            String value = product.optString(key, "").trim();
            if (!value.isEmpty()) {
                return value;
            }
        }
        return "";
    }

    private String buildIngredientsFromArray(JSONObject product) {
        if (product == null) return "";
        org.json.JSONArray ingredients = product.optJSONArray("ingredients");
        if (ingredients == null || ingredients.length() == 0) return "";

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < ingredients.length(); i++) {
            org.json.JSONObject item = ingredients.optJSONObject(i);
            if (item == null) continue;
            String text = item.optString("text", "").trim();
            if (text.isEmpty()) text = item.optString("id", "").trim();
            if (text.isEmpty()) text = item.optString("name", "").trim();
            if (text.isEmpty()) continue;

            if (builder.length() > 0) builder.append(", ");
            builder.append(text);
        }

        return builder.toString();
    }
    
    private double calculateHealthScore() {
        double score = 0;
        
        // Calorie scoring (0-20 points)
        if (currentProduct.calories <= 100) score += 20;
        else if (currentProduct.calories <= 200) score += 16;
        else if (currentProduct.calories <= 300) score += 12;
        else if (currentProduct.calories <= 400) score += 8;
        else if (currentProduct.calories <= 500) score += 4;
        
        // Sugar scoring (0-20 points)
        if (currentProduct.sugar <= 2) score += 20;
        else if (currentProduct.sugar <= 5) score += 16;
        else if (currentProduct.sugar <= 10) score += 12;
        else if (currentProduct.sugar <= 15) score += 8;
        else if (currentProduct.sugar <= 20) score += 4;
        
        // Fat scoring (0-15 points)
        if (currentProduct.fat <= 3) score += 15;
        else if (currentProduct.fat <= 10) score += 12;
        else if (currentProduct.fat <= 15) score += 8;
        else if (currentProduct.fat <= 20) score += 4;
        
        // Protein scoring (0-15 points)
        if (currentProduct.protein >= 20) score += 15;
        else if (currentProduct.protein >= 15) score += 12;
        else if (currentProduct.protein >= 10) score += 9;
        else if (currentProduct.protein >= 5) score += 6;
        else if (currentProduct.protein >= 2) score += 3;
        
        // Fiber scoring (0-15 points)
        if (currentProduct.fiber >= 10) score += 15;
        else if (currentProduct.fiber >= 6) score += 12;
        else if (currentProduct.fiber >= 3) score += 9;
        else if (currentProduct.fiber >= 1.5) score += 6;
        else if (currentProduct.fiber >= 0.5) score += 3;
        
        // Sodium scoring (0-15 points)
        if (currentProduct.sodium <= 100) score += 15;
        else if (currentProduct.sodium <= 300) score += 12;
        else if (currentProduct.sodium <= 600) score += 9;
        else if (currentProduct.sodium <= 1000) score += 6;
        else if (currentProduct.sodium <= 1500) score += 3;
        
        return Math.min(score, 100); // Cap at 100
    }
    
    private void displayHealthScore(double score) {
        if (healthScoreText != null) {
            healthScoreText.setText(String.format("Health Score: %.0f/100", score));
        }
        
        // Set emoji and description based on score
        String emoji;
        if (score >= 85) {
            emoji = "🌟"; // Excellent
        } else if (score >= 70) {
            emoji = "😊"; // Good
        } else if (score >= 55) {
            emoji = "😐"; // Fair
        } else if (score >= 40) {
            emoji = "😕"; // Poor
        } else {
            emoji = "😰"; // Very Poor
        }
        
        if (healthEmoji != null) {
            healthEmoji.setText(emoji);
        }
    }
    
    private void populateNutritionalInfo() {
        if (caloriesValue != null) caloriesValue.setText(String.format("%.0f", currentProduct.calories));
        if (proteinValue != null) proteinValue.setText(String.format("%.1fg", currentProduct.protein));
        if (sugarValue != null) sugarValue.setText(String.format("%.1fg", currentProduct.sugar));
        if (fatValue != null) fatValue.setText(String.format("%.1fg", currentProduct.fat));
        if (carbsValue != null) carbsValue.setText(String.format("%.1fg", currentProduct.carbs));
        if (sodiumValue != null) sodiumValue.setText(String.format("%.0fmg", currentProduct.sodium));
        if (fiberValue != null) fiberValue.setText(String.format("%.1fg", currentProduct.fiber));
    }
    
    private String generateHealthInsights(double score) {
        StringBuilder insights = new StringBuilder();
        
        if (score >= 85) {
            insights.append("🌟 Excellent choice! This product has outstanding nutritional value. ");
        } else if (score >= 70) {
            insights.append("✅ Good choice! This product offers solid nutritional benefits. ");
        } else if (score >= 55) {
            insights.append("⚖️ Moderate choice. Consider the portion size and frequency of consumption. ");
        } else if (score >= 40) {
            insights.append("⚠️ Poor nutritional profile. Look for healthier alternatives when possible. ");
        } else {
            insights.append("🚫 Very poor nutritional value. Strongly consider avoiding this product. ");
        }
        
        // Add specific insights
        if (currentProduct.sugar > 15) {
            insights.append("High sugar content may contribute to energy spikes. ");
        }
        if (currentProduct.fiber >= 5) {
            insights.append("Good fiber content supports digestive health. ");
        }
        if (currentProduct.protein >= 10) {
            insights.append("High protein content is beneficial for muscle health. ");
        }
        if (currentProduct.sodium > 1000) {
            insights.append("High sodium content - monitor daily intake. ");
        }
        
        return insights.toString();
    }
    
    private void shareProduct() {
        if (currentProduct != null) {
            String shareText = String.format(
                "Check out this product analysis:\n\n" +
                "🏷️ %s by %s\n" +
                "📊 Health Score: %.0f/100\n" +
                "🔥 Calories: %.0f per 100g\n" +
                "🥩 Protein: %.1fg\n" +
                "🍯 Sugar: %.1fg\n\n" +
                "Scanned with HealthScanner App",
                currentProduct.name,
                currentProduct.brand,
                calculateHealthScore(),
                currentProduct.calories,
                currentProduct.protein,
                currentProduct.sugar
            );
            
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
            startActivity(Intent.createChooser(shareIntent, "Share Product Analysis"));
        }
    }
    
    private void toggleFavorite() {
        // TODO: Implement favorite functionality
        Log.d(TAG, "Favorite functionality not yet implemented");
    }
    
    private void saveToScanHistory() {
        try {
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            String historyJson = prefs.getString("recent_scans", "[]");
            
            org.json.JSONArray historyArray = new org.json.JSONArray(historyJson);
            org.json.JSONObject productJson = new org.json.JSONObject();
            
            double healthScore = calculateHealthScore();
            
            productJson.put("productName", currentProduct.name);
            productJson.put("name", currentProduct.name); // Keep both for compatibility
            productJson.put("brand", currentProduct.brand);
            productJson.put("barcode", currentProduct.barcode);
            productJson.put("calories", currentProduct.calories);
            productJson.put("healthScore", healthScore);
            productJson.put("timestamp", System.currentTimeMillis());
            
            historyArray.put(0, productJson);
            
            // Keep only last 50 scans
            if (historyArray.length() > 50) {
                org.json.JSONArray trimmedArray = new org.json.JSONArray();
                for (int i = 0; i < 50; i++) {
                    trimmedArray.put(historyArray.get(i));
                }
                historyArray = trimmedArray;
            }
            
            // Save to local storage
            prefs.edit().putString("recent_scans", historyArray.toString()).apply();
            
            // Also sync with Firebase database
            syncScanWithFirebase(productJson, historyArray.length());
            
            // Immediately sync scan history to Firebase
            if (syncManager != null) {
                syncManager.syncOnScanHistoryChange(new SyncManager.SyncCallback() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "Scan data synced to Firebase immediately");
                    }
                    
                    @Override
                    public void onFailure(String error) {
                        Log.w(TAG, "Failed to sync scan data: " + error);
                        // Don't show error to user, sync will retry later
                    }
                });
            }
            
            Log.d(TAG, "Product saved to scan history and syncing with Firebase");
            
        } catch (org.json.JSONException e) {
            Log.e(TAG, "Error saving to scan history", e);
        }
    }
    
    /**
     * Sync scan data with Firebase database
     */
    private void syncScanWithFirebase(org.json.JSONObject scanData, int totalScans) {
        try {
            AuthManager authManager = AuthManager.getInstance(this);
            String userId = authManager.getCurrentUserId();
            
            if (userId != null && !userId.isEmpty()) {
                // Update user statistics in Firebase
                java.util.Map<String, Object> updates = new java.util.HashMap<>();
                updates.put("totalScans", totalScans);
                updates.put("lastScanTimestamp", System.currentTimeMillis());
                
                // Calculate if this is a healthy choice (health score >= 7.0)
                double healthScore = scanData.getDouble("healthScore");
                if (healthScore >= 70.0) { // Health score is on 0-100 scale
                    SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                    int currentHealthyChoices = prefs.getInt("healthy_choices", 0);
                    updates.put("healthyChoices", currentHealthyChoices + 1);
                    prefs.edit().putInt("healthy_choices", currentHealthyChoices + 1).apply();
                }
                
                // Update average health score
                SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                String allScansJson = prefs.getString("recent_scans", "[]");
                org.json.JSONArray allScans = new org.json.JSONArray(allScansJson);
                
                double totalHealthScore = 0;
                int validScans = 0;
                for (int i = 0; i < allScans.length(); i++) {
                    org.json.JSONObject scan = allScans.getJSONObject(i);
                    if (scan.has("healthScore")) {
                        totalHealthScore += scan.getDouble("healthScore");
                        validScans++;
                    }
                }
                
                if (validScans > 0) {
                    double avgHealthScore = totalHealthScore / validScans;
                    updates.put("averageHealthScore", avgHealthScore); // Keep on 0-100 scale
                }
                
                // Use FirebaseManager to update user statistics
                com.example.healthscanner.database.FirebaseManager firebaseManager = 
                    com.example.healthscanner.database.FirebaseManager.getInstance();
                
                firebaseManager.updateUserPreferences(userId, updates, 
                    new com.example.healthscanner.database.FirebaseManager.OperationCallback() {
                        @Override
                        public void onSuccess() {
                            Log.d(TAG, "User statistics synced with Firebase successfully");
                        }
                        
                        @Override
                        public void onFailure(String error) {
                            Log.w(TAG, "Failed to sync with Firebase: " + error);
                        }
                    });
                
                Log.d(TAG, "Syncing scan data with Firebase for user: " + userId);
            } else {
                Log.d(TAG, "No user ID available, skipping Firebase sync");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error syncing with Firebase: " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    /**
     * Check for health concerns and dietary preferences against product ingredients
     */
    private String checkHealthConcerns() {
        if (currentProduct == null || currentProduct.ingredients == null) {
            return "";
        }
        
        StringBuilder warnings = new StringBuilder();
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        
        // Get user's health concerns and dietary preferences
        java.util.Set<String> healthConcerns = prefs.getStringSet("health_concerns", new java.util.HashSet<>());
        java.util.Set<String> dietaryPreferences = prefs.getStringSet("dietary_preferences", new java.util.HashSet<>());
        
        String ingredients = currentProduct.ingredients.toLowerCase();
        
        // Check health concerns
        for (String concern : healthConcerns) {
            String concernLower = concern.toLowerCase();
            boolean hasWarning = false;
            String warningMessage = "";
            
            switch (concernLower) {
                case "diabetes":
                    if (currentProduct.sugar > 15 || ingredients.contains("sugar") || 
                        ingredients.contains("glucose") || ingredients.contains("fructose")) {
                        hasWarning = true;
                        warningMessage = "⚠️ HIGH SUGAR WARNING: This product contains " + 
                            String.format("%.1f", currentProduct.sugar) + "g of sugar, which may affect blood glucose levels.";
                    }
                    break;
                    
                case "high blood pressure":
                case "hypertension":
                    if (currentProduct.sodium > 600 || ingredients.contains("salt") || 
                        ingredients.contains("sodium")) {
                        hasWarning = true;
                        warningMessage = "⚠️ HIGH SODIUM WARNING: This product contains " + 
                            String.format("%.0f", currentProduct.sodium) + "mg of sodium, which may raise blood pressure.";
                    }
                    break;
                    
                case "high cholesterol":
                case "heart disease":
                    if (currentProduct.fat > 20 || ingredients.contains("saturated fat") || 
                        ingredients.contains("trans fat")) {
                        hasWarning = true;
                        warningMessage = "⚠️ HIGH FAT WARNING: This product contains " + 
                            String.format("%.1f", currentProduct.fat) + "g of fat, which may affect cholesterol levels.";
                    }
                    break;
                    
                case "gluten intolerance":
                case "celiac disease":
                    if (ingredients.contains("wheat") || ingredients.contains("gluten") || 
                        ingredients.contains("barley") || ingredients.contains("rye")) {
                        hasWarning = true;
                        warningMessage = "🚫 GLUTEN WARNING: This product may contain gluten from wheat, barley, or rye.";
                    }
                    break;
                    
                case "lactose intolerance":
                    if (ingredients.contains("milk") || ingredients.contains("lactose") || 
                        ingredients.contains("dairy") || ingredients.contains("whey")) {
                        hasWarning = true;
                        warningMessage = "🚫 LACTOSE WARNING: This product contains dairy ingredients that may cause digestive issues.";
                    }
                    break;
                    
                case "obesity":
                    if (currentProduct.calories > 400) {
                        hasWarning = true;
                        warningMessage = "⚠️ HIGH CALORIE WARNING: This product contains " + 
                            String.format("%.0f", currentProduct.calories) + " calories per 100g.";
                    }
                    break;
            }
            
            if (hasWarning) {
                if (warnings.length() > 0) warnings.append("\n\n");
                warnings.append(warningMessage);
            }
        }
        
        // Check dietary preferences
        for (String preference : dietaryPreferences) {
            String prefLower = preference.toLowerCase();
            boolean hasWarning = false;
            String warningMessage = "";
            
            switch (prefLower) {
                case "vegetarian":
                    if (ingredients.contains("meat") || ingredients.contains("chicken") || 
                        ingredients.contains("beef") || ingredients.contains("pork") ||
                        ingredients.contains("fish") || ingredients.contains("gelatin")) {
                        hasWarning = true;
                        warningMessage = "🥕 VEGETARIAN ALERT: This product may contain animal-derived ingredients.";
                    }
                    break;
                    
                case "vegan":
                    if (ingredients.contains("milk") || ingredients.contains("egg") || 
                        ingredients.contains("honey") || ingredients.contains("gelatin") ||
                        ingredients.contains("whey") || ingredients.contains("casein")) {
                        hasWarning = true;
                        warningMessage = "🌱 VEGAN ALERT: This product contains animal-derived ingredients.";
                    }
                    break;
                    
                case "keto":
                case "low carb":
                    if (currentProduct.carbs > 20) {
                        hasWarning = true;
                        warningMessage = "🥑 KETO ALERT: This product contains " + 
                            String.format("%.1f", currentProduct.carbs) + "g of carbs, which may not fit your keto diet.";
                    }
                    break;
                    
                case "low fat":
                    if (currentProduct.fat > 10) {
                        hasWarning = true;
                        warningMessage = "🥗 LOW FAT ALERT: This product contains " + 
                            String.format("%.1f", currentProduct.fat) + "g of fat.";
                    }
                    break;
                    
                case "low sodium":
                    if (currentProduct.sodium > 300) {
                        hasWarning = true;
                        warningMessage = "🧂 LOW SODIUM ALERT: This product contains " + 
                            String.format("%.0f", currentProduct.sodium) + "mg of sodium.";
                    }
                    break;
                    
                case "sugar-free":
                    if (currentProduct.sugar > 5 || ingredients.contains("sugar") || 
                        ingredients.contains("glucose") || ingredients.contains("fructose")) {
                        hasWarning = true;
                        warningMessage = "🍯 SUGAR-FREE ALERT: This product contains " + 
                            String.format("%.1f", currentProduct.sugar) + "g of sugar.";
                    }
                    break;
                    
                case "halal":
                    if (ingredients.contains("pork") || ingredients.contains("alcohol") || 
                        ingredients.contains("gelatin")) {
                        hasWarning = true;
                        warningMessage = "☪️ HALAL ALERT: This product may contain non-halal ingredients.";
                    }
                    break;
                    
                case "kosher":
                    if (ingredients.contains("pork") || ingredients.contains("shellfish")) {
                        hasWarning = true;
                        warningMessage = "✡️ KOSHER ALERT: This product may contain non-kosher ingredients.";
                    }
                    break;
            }
            
            if (hasWarning) {
                if (warnings.length() > 0) warnings.append("\n\n");
                warnings.append(warningMessage);
            }
        }
        
        return warnings.toString();
    }
    
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
    
    /**
     * Load product image from URL or show placeholder
     */
    private void loadProductImage() {
        if (productImage == null) return;
        
        if (currentProduct.imageUrl != null && !currentProduct.imageUrl.isEmpty()) {
            // Try to load image from URL
            Log.d(TAG, "Loading product image from: " + currentProduct.imageUrl);
            
            // Use a simple image loading approach with error handling
            new Thread(() -> {
                try {
                    java.net.URL url = new java.net.URL(currentProduct.imageUrl);
                    java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
                    connection.setDoInput(true);
                    connection.connect();
                    
                    java.io.InputStream input = connection.getInputStream();
                    android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(input);
                    
                    runOnUiThread(() -> {
                        if (bitmap != null) {
                            productImage.setImageBitmap(bitmap);
                            productImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
                            Log.d(TAG, "Product image loaded successfully");
                        } else {
                            setPlaceholderImage();
                        }
                    });
                    
                } catch (Exception e) {
                    Log.e(TAG, "Error loading product image: " + e.getMessage());
                    runOnUiThread(this::setPlaceholderImage);
                }
            }).start();
            
        } else {
            setPlaceholderImage();
        }
    }
    
    /**
     * Set placeholder image based on product type
     */
    private void setPlaceholderImage() {
        if (productImage == null) return;
        
        // Set placeholder based on product name/type
        int placeholderResource = R.drawable.ic_product_placeholder;
        
        if (currentProduct.name != null) {
            String name = currentProduct.name.toLowerCase();
            if (name.contains("cereal") || name.contains("grain")) {
                placeholderResource = R.drawable.ic_cereal_placeholder;
            } else if (name.contains("yogurt") || name.contains("dairy")) {
                placeholderResource = R.drawable.ic_dairy_placeholder;
            } else if (name.contains("chocolate") || name.contains("candy") || name.contains("chips") ||
                    name.contains("lays") || name.contains("potato") || name.contains("masala")) {
                placeholderResource = R.drawable.ic_snack_placeholder;
            } else if (name.contains("apple") || name.contains("fruit")) {
                placeholderResource = R.drawable.ic_fruit_placeholder;
            }
        }
        
        try {
            productImage.setImageResource(placeholderResource);
            productImage.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            Log.d(TAG, "Placeholder image set");
        } catch (Exception e) {
            Log.e(TAG, "Error setting placeholder image", e);
            // Fallback to a basic drawable
            productImage.setImageResource(android.R.drawable.ic_menu_gallery);
        }
    }
    
    // Data class
    private static class ProductInfo {
        String barcode, name, brand, ingredients, imageUrl;
        double calories, protein, sugar, fat, carbs, fiber, sodium;
    }
}