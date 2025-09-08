// MainActivity.java - Simplified with working UI elements
package com.example.healthscanner;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import org.json.JSONObject;
import org.json.JSONException;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    // UI Elements
    private Button scanBtn, manualSearchBtn;
    private EditText manualBarcodeInput;
    private CardView resultCard, nutritionTrackerCard, scanSectionCard;
    private ImageView scanIcon, scanningIndicator, favoriteBtn;
    private TextView scanStatusText;
    private TextView productName, productBrand, productIngredients;
    private TextView caloriesValue, sugarValue, proteinValue, fatValue, carbsValue, saltValue;
    private TextView caloriesProgress, sugarProgress, proteinProgress;
    private ProgressBar caloriesProgressBar, sugarProgressBar, proteinProgressBar;
    
    private BottomNavigationView bottomNavigation;
    private RequestQueue requestQueue;
    
    // Navigation tracking
    private boolean isNavigationInitialized = false;
    
    // Haptic feedback
    private Vibrator vibrator;
    
    // SharedPreferences keys
    private static final String PREFS_NAME = "HealthScannerPrefs";
    private static final String KEY_DAILY_CALORIES = "daily_calories";
    private static final String KEY_DAILY_SUGAR = "daily_sugar";
    private static final String KEY_DAILY_PROTEIN = "daily_protein";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply theme before calling super.onCreate()
        ThemeHelper.applyTheme(this);
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Check if user is logged in - TEMPORARILY DISABLED FOR TESTING
        SharedPreferences prefs = getSharedPreferences("HealthScannerPrefs", MODE_PRIVATE);
        // Set logged in to true for testing
        prefs.edit().putBoolean("is_logged_in", true).apply();
        
        if (!prefs.getBoolean("is_logged_in", false)) {
            Intent loginIntent = new Intent(this, LoginActivity.class);
            startActivity(loginIntent);
            finish();
            return;
        }

        try {
            Log.d("MainActivity", "Starting initialization...");
            initializeViews();
            Log.d("MainActivity", "Views initialized");
            initializeHapticFeedback();
            Log.d("MainActivity", "Haptic feedback initialized");
            setupClickListeners();
            Log.d("MainActivity", "Click listeners setup");
            setupBottomNavigation();
            Log.d("MainActivity", "Bottom navigation setup");
            updateDailyNutritionTracker();
            Log.d("MainActivity", "Daily nutrition tracker updated");
            Log.d("MainActivity", "Initialization completed successfully");
            
            // Check if we should start scanner immediately
            if (getIntent().getBooleanExtra("start_scanner", false)) {
                if (checkCameraPermission()) {
                    startBarcodeScanner();
                } else {
                    requestCameraPermission();
                }
            }
        } catch (Exception e) {
            Log.e("MainActivity", "Error in onCreate: " + e.getMessage(), e);
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bottomNavigation != null && isNavigationInitialized) {
            bottomNavigation.setSelectedItemId(R.id.nav_scan);
        }
        updateDailyNutritionTracker();
    }

    private void initializeViews() {
        // Main UI elements
        scanBtn = findViewById(R.id.scanBtn);
        manualSearchBtn = findViewById(R.id.manualSearchBtn);
        manualBarcodeInput = findViewById(R.id.manualBarcodeInput);
        resultCard = findViewById(R.id.resultCard);
        scanSectionCard = findViewById(R.id.scanSectionCard);
        
        // Scan section
        scanIcon = findViewById(R.id.scanIcon);
        scanningIndicator = findViewById(R.id.scanningIndicator);
        scanStatusText = findViewById(R.id.scanStatusText);
        
        // Nutrition tracker
        nutritionTrackerCard = findViewById(R.id.nutritionTrackerCard);
        caloriesProgress = findViewById(R.id.caloriesProgress);
        sugarProgress = findViewById(R.id.sugarProgress);
        proteinProgress = findViewById(R.id.proteinProgress);
        caloriesProgressBar = findViewById(R.id.caloriesProgressBar);
        sugarProgressBar = findViewById(R.id.sugarProgressBar);
        proteinProgressBar = findViewById(R.id.proteinProgressBar);
        
        // Product result
        productName = findViewById(R.id.productName);
        productBrand = findViewById(R.id.productBrand);
        productIngredients = findViewById(R.id.productIngredients);
        favoriteBtn = findViewById(R.id.favoriteBtn);
        
        // Nutrition values
        caloriesValue = findViewById(R.id.caloriesValue);
        sugarValue = findViewById(R.id.sugarValue);
        proteinValue = findViewById(R.id.proteinValue);
        fatValue = findViewById(R.id.fatValue);
        carbsValue = findViewById(R.id.carbsValue);
        saltValue = findViewById(R.id.saltValue);
        
        // Bottom navigation
        bottomNavigation = findViewById(R.id.bottom_navigation);
        
        // Request queue
        requestQueue = Volley.newRequestQueue(this);
        
        // Check critical views
        if (scanBtn == null || resultCard == null) {
            Log.e("MainActivity", "Critical views not found in layout");
            return;
        }
        
        // Start scan icon pulse animation
        startScanIconAnimation();
    }
    
    private void initializeHapticFeedback() {
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
    }

    private void setupClickListeners() {
        // Scan button
        scanBtn.setOnClickListener(v -> {
            performHapticFeedback();
            animateScanButtonPress();
            if (checkCameraPermission()) {
                startBarcodeScanner();
            } else {
                requestCameraPermission();
            }
        });

        // Manual search button
        manualSearchBtn.setOnClickListener(v -> {
            performHapticFeedback();
            String barcode = manualBarcodeInput.getText().toString().trim();
            if (!barcode.isEmpty()) {
                searchProduct(barcode);
            }
        });
        
        // Add card press animations for interactive elements
        setupCardInteractions();

        // Favorite button
        if (favoriteBtn != null) {
            favoriteBtn.setOnClickListener(v -> {
                performHapticFeedback();
                toggleFavorite();
            });
        }
    }

    private void setupBottomNavigation() {
        if (bottomNavigation == null) {
            Log.w("MainActivity", "Bottom navigation is null, skipping setup");
            return;
        }

        // Set up navigation item selected listener first
        bottomNavigation.setOnItemSelectedListener(item -> {
            // Don't trigger actions during initialization
            if (!isNavigationInitialized) {
                return false;
            }

            int itemId = item.getItemId();
            if (itemId == R.id.nav_scan) {
                // Already on scan page
                return true;
            } else if (itemId == R.id.nav_history) {
                Intent historyIntent = new Intent(this, HistoryActivity.class);
                startActivity(historyIntent);
                return true;
            } else if (itemId == R.id.nav_profile) {
                Intent profileIntent = new Intent(this, ProfileActivity.class);
                startActivity(profileIntent);
                return true;
            }
            return false;
        });

        // Mark navigation as initialized
        isNavigationInitialized = true;
        
        // Set the correct item as selected
        bottomNavigation.setSelectedItemId(R.id.nav_scan);
    }

    private void updateDailyNutritionTracker() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        
        int dailyCalories = prefs.getInt(KEY_DAILY_CALORIES, 0);
        int dailySugar = prefs.getInt(KEY_DAILY_SUGAR, 0);
        int dailyProtein = prefs.getInt(KEY_DAILY_PROTEIN, 0);
        
        // Update progress text
        if (caloriesProgress != null) {
            caloriesProgress.setText(dailyCalories + " / 2,000");
        }
        if (sugarProgress != null) {
            sugarProgress.setText(dailySugar + "g / 50g");
        }
        if (proteinProgress != null) {
            proteinProgress.setText(dailyProtein + "g / 100g");
        }
        
        // Update progress bars
        if (caloriesProgressBar != null) {
            caloriesProgressBar.setProgress((dailyCalories * 100) / 2000);
        }
        if (sugarProgressBar != null) {
            sugarProgressBar.setProgress((dailySugar * 100) / 50);
        }
        if (proteinProgressBar != null) {
            proteinProgressBar.setProgress((dailyProtein * 100) / 100);
        }
    }

    private void searchProduct(String barcode) {
        // Show scanning animation
        showScanningAnimation();
        
        // Show loading state
        resultCard.setVisibility(View.VISIBLE);
        productName.setText("🔍 Analyzing product...");
        
        // Call real API for product information
        fetchProductFromAPI(barcode);
    }

    private void fetchProductFromAPI(String barcode) {
        // Try APIs in priority order with fallback logic
        tryOpenFoodFactsAPI(barcode);
    }
    
    private void tryOpenFoodFactsAPI(String barcode) {
        String url = ApiConfig.Endpoints.getOpenFoodFactsUrl(barcode);
        
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            if (response.has("status") && response.getInt("status") == 1) {
                                // Success - parse and display
                                parseOpenFoodFactsResponse(response, barcode);
                            } else {
                                // Product not found, try next API
                                Log.d("MainActivity", "Open Food Facts: Product not found, trying Nutritionix");
                                tryNutritionixAPI(barcode);
                            }
                        } catch (JSONException e) {
                            Log.e("MainActivity", "Error parsing Open Food Facts response: " + e.getMessage());
                            tryNutritionixAPI(barcode);
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("MainActivity", "Open Food Facts API Error: " + error.getMessage());
                        tryNutritionixAPI(barcode);
                    }
                }
        );
        
        requestQueue.add(jsonObjectRequest);
    }
    
    private void tryNutritionixAPI(String barcode) {
        String url = ApiConfig.Endpoints.getNutritionixUrl() + barcode;
        
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            if (response.has("foods") && response.getJSONArray("foods").length() > 0) {
                                // Success - parse and display
                                parseNutritionixResponse(response, barcode);
                            } else {
                                // Product not found, try next API
                                Log.d("MainActivity", "Nutritionix: Product not found, trying UPCItemDB");
                                tryUPCItemDBAPI(barcode);
                            }
                        } catch (JSONException e) {
                            Log.e("MainActivity", "Error parsing Nutritionix response: " + e.getMessage());
                            tryUPCItemDBAPI(barcode);
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("MainActivity", "Nutritionix API Error: " + error.getMessage());
                        tryUPCItemDBAPI(barcode);
                    }
                }
        ) {
            @Override
            public java.util.Map<String, String> getHeaders() {
                java.util.Map<String, String> headers = new java.util.HashMap<>();
                headers.put("x-app-id", ApiConfig.Headers.getNutritionixAppId());
                headers.put("x-app-key", ApiConfig.Headers.getNutritionixApiKey());
                return headers;
            }
        };
        
        requestQueue.add(jsonObjectRequest);
    }
    
    private void tryUPCItemDBAPI(String barcode) {
        String url = ApiConfig.Endpoints.getUPCItemDBUrl(barcode);
        
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            if (response.has("items") && response.getJSONArray("items").length() > 0) {
                                // Success - parse and display
                                parseUPCItemDBResponse(response, barcode);
                            } else {
                                // Product not found, try next API
                                Log.d("MainActivity", "UPCItemDB: Product not found, trying USDA");
                                tryUSDAAPI(barcode);
                            }
                        } catch (JSONException e) {
                            Log.e("MainActivity", "Error parsing UPCItemDB response: " + e.getMessage());
                            tryUSDAAPI(barcode);
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("MainActivity", "UPCItemDB API Error: " + error.getMessage());
                        tryUSDAAPI(barcode);
                    }
                }
        );
        
        requestQueue.add(jsonObjectRequest);
    }
    
    private void tryUSDAAPI(String barcode) {
        String url = ApiConfig.Endpoints.getUSDAUrl(barcode);
        
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            if (response.has("foods") && response.getJSONArray("foods").length() > 0) {
                                // Success - parse and display
                                parseUSDAResponse(response, barcode);
                            } else {
                                // Product not found, try last API
                                Log.d("MainActivity", "USDA: Product not found, trying Spoonacular");
                                trySpoonacularAPI(barcode);
                            }
                        } catch (JSONException e) {
                            Log.e("MainActivity", "Error parsing USDA response: " + e.getMessage());
                            trySpoonacularAPI(barcode);
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("MainActivity", "USDA API Error: " + error.getMessage());
                        trySpoonacularAPI(barcode);
                    }
                }
        );
        
        requestQueue.add(jsonObjectRequest);
    }
    
    private void trySpoonacularAPI(String barcode) {
        String url = ApiConfig.Endpoints.getSpoonacularUrl(barcode);
        
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            if (response.has("id")) {
                                // Success - parse and display
                                parseSpoonacularResponse(response, barcode);
                            } else {
                                // All APIs failed
                                Log.d("MainActivity", "All APIs failed to find product");
                                showErrorState("Product not found in any database");
                            }
                        } catch (JSONException e) {
                            Log.e("MainActivity", "Error parsing Spoonacular response: " + e.getMessage());
                            showErrorState("Product not found in any database");
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("MainActivity", "Spoonacular API Error: " + error.getMessage());
                        showErrorState("Product not found in any database");
                    }
                }
        );
        
        requestQueue.add(jsonObjectRequest);
    }

    // Open Food Facts API Parser
    private void parseOpenFoodFactsResponse(JSONObject response, String barcode) throws JSONException {
        hideScanningAnimation();
        
        JSONObject product = response.getJSONObject("product");
        
        // Extract product information
        String productName = product.optString("product_name", "Unknown Product");
        String brand = product.optString("brands", "Unknown Brand");
        String ingredients = product.optString("ingredients_text", "Ingredients not available");
        
        // Extract nutritional information
        JSONObject nutriments = product.optJSONObject("nutriments");
        int calories = 0, sugar = 0, protein = 0, fat = 0, carbs = 0, salt = 0;
        
        if (nutriments != null) {
            calories = nutriments.optInt("energy-kcal_100g", 0);
            sugar = nutriments.optInt("sugars_100g", 0);
            protein = nutriments.optInt("proteins_100g", 0);
            fat = nutriments.optInt("fat_100g", 0);
            carbs = nutriments.optInt("carbohydrates_100g", 0);
            salt = nutriments.optInt("sodium_100g", 0);
        }
        
        displayProductResult(productName, brand, calories, sugar, protein, fat, carbs, salt, ingredients, barcode, "Open Food Facts");
    }
    
    // Nutritionix API Parser
    private void parseNutritionixResponse(JSONObject response, String barcode) throws JSONException {
        hideScanningAnimation();
        
        org.json.JSONArray foods = response.getJSONArray("foods");
        if (foods.length() > 0) {
            JSONObject food = foods.getJSONObject(0);
            
            String productName = food.optString("food_name", "Unknown Product");
            String brand = food.optString("brand_name", "Unknown Brand");
            
            // Extract nutritional information
            JSONObject fullNutrients = food.optJSONObject("full_nutrients");
            int calories = 0, sugar = 0, protein = 0, fat = 0, carbs = 0, salt = 0;
            
            if (fullNutrients != null) {
                calories = fullNutrients.optInt("208", 0); // Energy (kcal)
                sugar = fullNutrients.optInt("269", 0);    // Sugars
                protein = fullNutrients.optInt("203", 0); // Protein
                fat = fullNutrients.optInt("204", 0);      // Fat
                carbs = fullNutrients.optInt("205", 0);   // Carbohydrates
                salt = fullNutrients.optInt("307", 0);    // Sodium
            }
            
            String ingredients = food.optString("ingredients", "Ingredients not available");
            displayProductResult(productName, brand, calories, sugar, protein, fat, carbs, salt, ingredients, barcode, "Nutritionix");
        }
    }
    
    // UPCItemDB API Parser
    private void parseUPCItemDBResponse(JSONObject response, String barcode) throws JSONException {
        hideScanningAnimation();
        
        org.json.JSONArray items = response.getJSONArray("items");
        if (items.length() > 0) {
            JSONObject item = items.getJSONObject(0);
            
            String productName = item.optString("title", "Unknown Product");
            String brand = item.optString("brand", "Unknown Brand");
            String ingredients = item.optString("description", "Ingredients not available");
            
            // UPCItemDB doesn't provide detailed nutrition, use defaults
            int calories = 0, sugar = 0, protein = 0, fat = 0, carbs = 0, salt = 0;
            
            displayProductResult(productName, brand, calories, sugar, protein, fat, carbs, salt, ingredients, barcode, "UPCItemDB");
        }
    }
    
    // USDA API Parser
    private void parseUSDAResponse(JSONObject response, String barcode) throws JSONException {
        hideScanningAnimation();
        
        org.json.JSONArray foods = response.getJSONArray("foods");
        if (foods.length() > 0) {
            JSONObject food = foods.getJSONObject(0);
            
            String productName = food.optString("description", "Unknown Product");
            String brand = "USDA Database";
            
            // Extract nutritional information
            org.json.JSONArray nutrients = food.optJSONArray("foodNutrients");
            int calories = 0, sugar = 0, protein = 0, fat = 0, carbs = 0, salt = 0;
            
            if (nutrients != null) {
                for (int i = 0; i < nutrients.length(); i++) {
                    JSONObject nutrient = nutrients.getJSONObject(i);
                    String nutrientName = nutrient.optString("nutrientName", "").toLowerCase();
                    double amount = nutrient.optDouble("amount", 0);
                    
                    if (nutrientName.contains("energy")) calories = (int) amount;
                    else if (nutrientName.contains("sugar")) sugar = (int) amount;
                    else if (nutrientName.contains("protein")) protein = (int) amount;
                    else if (nutrientName.contains("fat")) fat = (int) amount;
                    else if (nutrientName.contains("carbohydrate")) carbs = (int) amount;
                    else if (nutrientName.contains("sodium")) salt = (int) amount;
                }
            }
            
            String ingredients = "Nutritional data from USDA database";
            displayProductResult(productName, brand, calories, sugar, protein, fat, carbs, salt, ingredients, barcode, "USDA");
        }
    }
    
    // Spoonacular API Parser
    private void parseSpoonacularResponse(JSONObject response, String barcode) throws JSONException {
        hideScanningAnimation();
        
        String productName = response.optString("title", "Unknown Product");
        String brand = response.optString("brand", "Unknown Brand");
        String ingredients = response.optString("ingredients", "Ingredients not available");
        
        // Extract nutritional information
        JSONObject nutrition = response.optJSONObject("nutrition");
        int calories = 0, sugar = 0, protein = 0, fat = 0, carbs = 0, salt = 0;
        
        if (nutrition != null) {
            org.json.JSONArray nutrients = nutrition.optJSONArray("nutrients");
            if (nutrients != null) {
                for (int i = 0; i < nutrients.length(); i++) {
                    JSONObject nutrient = nutrients.getJSONObject(i);
                    String name = nutrient.optString("name", "").toLowerCase();
                    double amount = nutrient.optDouble("amount", 0);
                    
                    if (name.contains("calories")) calories = (int) amount;
                    else if (name.contains("sugar")) sugar = (int) amount;
                    else if (name.contains("protein")) protein = (int) amount;
                    else if (name.contains("fat")) fat = (int) amount;
                    else if (name.contains("carbohydrate")) carbs = (int) amount;
                    else if (name.contains("sodium")) salt = (int) amount;
                }
            }
        }
        
        displayProductResult(productName, brand, calories, sugar, protein, fat, carbs, salt, ingredients, barcode, "Spoonacular");
    }
    
    // Common method to display product results
    private void displayProductResult(String productName, String brand, int calories, int sugar, 
                                    int protein, int fat, int carbs, int salt, String ingredients, 
                                    String barcode, String source) {
        // Determine health rating based on nutrition
        String healthRating = determineHealthRating(calories, sugar, protein, fat);
        
        // Update UI with product data
        updateProductUI(productName, brand, calories, sugar, protein, fat, carbs, salt);
        updateProductIngredients(ingredients);
        
        // Update nutrition bars
        updateNutritionBars(calories, sugar, protein);
        
        // Show product result animation
        showProductResultAnimation();
        
        // Success haptic feedback
        performSuccessHaptic();
        
        // Add to history with real data
        HistoryActivity.addScanToHistory(this, productName, brand, barcode, "Food & Beverages", 
                healthRating, calories, sugar, protein, ingredients);
        
    }

    private String determineHealthRating(int calories, int sugar, int protein, int fat) {
        int score = 0;
        
        // Score based on calories (per 100g)
        if (calories < 50) score += 3;
        else if (calories < 150) score += 2;
        else if (calories < 300) score += 1;
        
        // Score based on sugar (per 100g)
        if (sugar < 5) score += 3;
        else if (sugar < 15) score += 2;
        else if (sugar < 25) score += 1;
        
        // Score based on protein (per 100g)
        if (protein > 15) score += 2;
        else if (protein > 8) score += 1;
        
        // Score based on fat (per 100g)
        if (fat < 3) score += 2;
        else if (fat < 10) score += 1;
        
        // Determine rating
        if (score >= 8) return "Excellent";
        else if (score >= 6) return "Good";
        else if (score >= 4) return "Moderate";
        else if (score >= 2) return "Poor";
        else return "Unhealthy";
    }

    private void showErrorState(String message) {
        if (resultCard != null) {
            resultCard.setVisibility(View.VISIBLE);
        }
        if (productName != null) {
            productName.setText("❌ " + message);
        }
        if (productBrand != null) {
            productBrand.setText("Please try scanning again");
        }
        if (productIngredients != null) {
            productIngredients.setText("Make sure the barcode is clear and try again");
        }
        
        // Reset nutrition values
        if (caloriesValue != null) caloriesValue.setText("--");
        if (sugarValue != null) sugarValue.setText("--");
        if (proteinValue != null) proteinValue.setText("--");
        if (fatValue != null) fatValue.setText("--");
        if (carbsValue != null) carbsValue.setText("--");
        if (saltValue != null) saltValue.setText("--");
        
    }

    private void updateProductUI(String name, String brand, int calories, int sugar, int protein, int fat, int carbs, int salt) {
        if (productName != null) productName.setText(name);
        if (productBrand != null) productBrand.setText(brand);
        
        // Update nutrition values with icons
        if (caloriesValue != null) caloriesValue.setText("🔥 " + calories + " kcal");
        if (sugarValue != null) sugarValue.setText("🍯 " + sugar + "g");
        if (proteinValue != null) proteinValue.setText("🥩 " + protein + "g");
        if (fatValue != null) fatValue.setText("🧈 " + fat + "g");
        if (carbsValue != null) carbsValue.setText("🍞 " + carbs + "g");
        if (saltValue != null) saltValue.setText("🧂 " + salt + "mg");
        
        // Update daily tracker
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_DAILY_CALORIES, prefs.getInt(KEY_DAILY_CALORIES, 0) + calories);
        editor.putInt(KEY_DAILY_SUGAR, prefs.getInt(KEY_DAILY_SUGAR, 0) + sugar);
        editor.putInt(KEY_DAILY_PROTEIN, prefs.getInt(KEY_DAILY_PROTEIN, 0) + protein);
        editor.apply();
        
        // Refresh the tracker display
        updateDailyNutritionTracker();
    }

    private void updateProductIngredients(String ingredients) {
        if (productIngredients != null) {
            if (ingredients != null && !ingredients.isEmpty()) {
                productIngredients.setText("📋 Ingredients: " + ingredients);
            } else {
                productIngredients.setText("📋 Ingredients: Not available");
            }
        }
    }

    private void updateNutritionBars(int calories, int sugar, int protein) {
        // This method is simplified since we don't have the bar elements in the current layout
        // The nutrition values are displayed in the product result card
        Log.d("MainActivity", "Nutrition updated: " + calories + " cal, " + sugar + "g sugar, " + protein + "g protein");
    }

    private void toggleFavorite() {
        if (favoriteBtn != null) {
            // Toggle favorite state
            boolean isFavorite = favoriteBtn.getTag() != null && (Boolean) favoriteBtn.getTag();
            isFavorite = !isFavorite;
            favoriteBtn.setTag(isFavorite);
            
            // Update icon color
            if (isFavorite) {
                favoriteBtn.setColorFilter(getResources().getColor(R.color.accent_color));
            } else {
                favoriteBtn.setColorFilter(getResources().getColor(R.color.adaptive_text_secondary));
            }
        }
    }

    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this, 
                new String[]{Manifest.permission.CAMERA}, 100);
    }

    private void startBarcodeScanner() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES);
        integrator.setPrompt("Scan a barcode");
        integrator.setCameraId(0);
        integrator.setBeepEnabled(false);
        integrator.setBarcodeImageEnabled(true);
        
        // Force vertical (portrait) orientation
        integrator.setOrientationLocked(true);
        integrator.setCaptureActivity(VerticalCaptureActivity.class);
        
        integrator.initiateScan();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() == null) {
            } else {
                String barcode = result.getContents();
                searchProduct(barcode);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, 
                                         @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startBarcodeScanner();
            } else {
            }
        }
    }
    
    // Animation Methods
    private void startScanIconAnimation() {
        if (scanIcon != null) {
            android.view.animation.Animation pulseAnim = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.pulse_animation);
            scanIcon.startAnimation(pulseAnim);
        }
    }
    
    private void showScanningAnimation() {
        if (scanningIndicator != null && scanStatusText != null) {
            scanningIndicator.setVisibility(View.VISIBLE);
            scanStatusText.setText("🔍 Scanning product...");
            
            // Start the scanning animation
            android.graphics.drawable.AnimationDrawable scanningDrawable = (android.graphics.drawable.AnimationDrawable) scanningIndicator.getDrawable();
            scanningDrawable.start();
        }
    }
    
    private void hideScanningAnimation() {
        if (scanningIndicator != null && scanStatusText != null) {
            scanningIndicator.setVisibility(View.GONE);
            scanStatusText.setText("✅ Product analyzed successfully!");
            
            // Stop the scanning animation
            android.graphics.drawable.AnimationDrawable scanningDrawable = (android.graphics.drawable.AnimationDrawable) scanningIndicator.getDrawable();
            scanningDrawable.stop();
        }
    }
    
    private void showProductResultAnimation() {
        if (resultCard != null) {
            android.view.animation.Animation enterAnim = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.product_result_enter);
            resultCard.startAnimation(enterAnim);
        }
    }
    
    private void animateScanButtonPress() {
        if (scanBtn != null) {
            android.view.animation.Animation pressAnim = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.scan_button_press);
            scanBtn.startAnimation(pressAnim);
        }
    }
    
    // Haptic Feedback Methods
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
            Log.w("MainActivity", "Vibration permission not granted: " + e.getMessage());
            // Silently fail - haptic feedback is optional
        } catch (Exception e) {
            Log.w("MainActivity", "Haptic feedback error: " + e.getMessage());
            // Silently fail - haptic feedback is optional
        }
    }
    
    private void performSuccessHaptic() {
        try {
            if (vibrator != null && vibrator.hasVibrator()) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(100);
                }
            }
        } catch (SecurityException e) {
            Log.w("MainActivity", "Vibration permission not granted: " + e.getMessage());
            // Silently fail - haptic feedback is optional
        } catch (Exception e) {
            Log.w("MainActivity", "Haptic feedback error: " + e.getMessage());
            // Silently fail - haptic feedback is optional
        }
    }
    
    // Card Interaction Methods
    private void setupCardInteractions() {
        // Add press animations to interactive cards
        if (nutritionTrackerCard != null) {
            nutritionTrackerCard.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, android.view.MotionEvent event) {
                    switch (event.getAction()) {
                        case android.view.MotionEvent.ACTION_DOWN:
                            animateCardPress(v);
                            break;
                        case android.view.MotionEvent.ACTION_UP:
                        case android.view.MotionEvent.ACTION_CANCEL:
                            animateCardRelease(v);
                            break;
                    }
                    return false;
                }
            });
        }
    }
    
    private void animateCardPress(View card) {
        Animation pressAnim = AnimationUtils.loadAnimation(this, R.anim.card_press);
        card.startAnimation(pressAnim);
    }
    
    private void animateCardRelease(View card) {
        Animation releaseAnim = AnimationUtils.loadAnimation(this, R.anim.card_release);
        card.startAnimation(releaseAnim);
    }
    
}