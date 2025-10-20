package com.example.healthscanner;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Simplified Product Details Activity with Health Scoring System
 * Shows detailed nutritional information and calculates health score out of 100
 */
public class ProductDetailsActivitySimple extends BaseActivity {
    
    private static final String TAG = "ProductDetailsSimple";
    private static final String PREFS_NAME = "HealthScannerPrefs";
    
    // UI Elements - Basic Info
    private TextView productName, productBrand, productBarcode;
    private TextView healthScoreNumber, healthScoreText;
    private TextView caloriesValue, proteinValue, sugarValue;
    private TextView fatValue, carbsValue, sodiumValue;
    private TextView servingSizeText, ingredientsText, allergensText;
    
    // RecyclerView for category scores
    private RecyclerView categoryScoresRecycler;
    private CategoryScoresAdapter categoryAdapter;
    private List<CategoryScoresAdapter.CategoryScore> categoryScores;
    
    // Data
    private ProductInfo currentProduct;
    private RequestQueue requestQueue;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "ProductDetailsActivitySimple onCreate started");
        
        try {
            setContentView(R.layout.activity_product_details_simple);
            Log.d(TAG, "Layout set successfully");
            
            // Initialize components
            initializeViews();
            Log.d(TAG, "Views initialized");
            
            initializeBottomNavigation();
            Log.d(TAG, "Bottom navigation initialized");
            
            setupCategoryScoresRecycler();
            Log.d(TAG, "Category scores recycler setup");
            
            // Initialize request queue
            requestQueue = Volley.newRequestQueue(this);
            Log.d(TAG, "Request queue initialized");
            
            // Get barcode from intent and fetch product
            String barcode = getIntent().getStringExtra("barcode");
            Log.d(TAG, "Received barcode from intent: " + barcode);
            
            if (barcode != null && !barcode.isEmpty()) {
                Log.d(TAG, "Fetching product details for barcode: " + barcode);
                fetchProductDetails(barcode);
            } else {
                Log.e(TAG, "No barcode provided in intent, using demo product");
                // Show demo product for testing
                createDemoProduct("1234567890123");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + e.getMessage(), e);
            // Fallback to demo product
            createDemoProduct("1234567890123");
        }
    }
    
    private void initializeViews() {
        Log.d(TAG, "Initializing views...");
        
        // Basic product info
        productName = findViewById(R.id.product_name);
        productBrand = findViewById(R.id.product_brand);
        productBarcode = findViewById(R.id.product_barcode);
        
        Log.d(TAG, "Basic info views: name=" + (productName != null) + 
                   ", brand=" + (productBrand != null) + 
                   ", barcode=" + (productBarcode != null));
        
        // Health score
        healthScoreNumber = findViewById(R.id.health_score_number);
        healthScoreText = findViewById(R.id.health_score_text);
        
        Log.d(TAG, "Health score views: number=" + (healthScoreNumber != null) + 
                   ", text=" + (healthScoreText != null));
        
        // Nutritional information
        caloriesValue = findViewById(R.id.calories_value);
        proteinValue = findViewById(R.id.protein_value);
        sugarValue = findViewById(R.id.sugar_value);
        fatValue = findViewById(R.id.fat_value);
        carbsValue = findViewById(R.id.carbs_value);
        sodiumValue = findViewById(R.id.sodium_value);
        
        Log.d(TAG, "Nutrition views found: " + 
                   (caloriesValue != null) + ", " + 
                   (proteinValue != null) + ", " + 
                   (sugarValue != null) + ", " + 
                   (fatValue != null) + ", " + 
                   (carbsValue != null) + ", " + 
                   (sodiumValue != null));
        
        // Additional info
        servingSizeText = findViewById(R.id.serving_size_text);
        ingredientsText = findViewById(R.id.ingredients_text);
        allergensText = findViewById(R.id.allergens_text);
        
        Log.d(TAG, "Additional info views: serving=" + (servingSizeText != null) + 
                   ", ingredients=" + (ingredientsText != null) + 
                   ", allergens=" + (allergensText != null));
        
        // Category scores
        categoryScoresRecycler = findViewById(R.id.category_scores_recycler);
        
        Log.d(TAG, "Category recycler view: " + (categoryScoresRecycler != null));
        Log.d(TAG, "Views initialization completed");
    }
    
    private void setupCategoryScoresRecycler() {
        if (categoryScoresRecycler != null) {
            categoryScores = new ArrayList<>();
            categoryAdapter = new CategoryScoresAdapter(categoryScores);
            categoryScoresRecycler.setLayoutManager(new LinearLayoutManager(this));
            categoryScoresRecycler.setAdapter(categoryAdapter);
        }
    }
    
    private void fetchProductDetails(String barcode) {
        Log.d(TAG, "Fetching product details for barcode: " + barcode);
        
        // Try Open Food Facts API first
        String url = "https://world.openfoodfacts.org/api/v0/product/" + barcode + ".json";
        
        JsonObjectRequest request = new JsonObjectRequest(
            Request.Method.GET, url, null,
            new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    try {
                        if (response.has("status") && response.getInt("status") == 1) {
                            Log.d(TAG, "Product found in API, parsing response");
                            parseProductResponse(response, barcode);
                        } else {
                            Log.d(TAG, "Product not found in API, using demo data");
                            // Fallback to demo data
                            createDemoProduct(barcode);
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing response", e);
                        createDemoProduct(barcode);
                    }
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e(TAG, "API Error: " + error.getMessage());
                    Log.d(TAG, "Falling back to demo product");
                    createDemoProduct(barcode);
                }
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
        currentProduct.servingSize = product.optString("serving_size", "100g");
        
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
        
        currentProduct.ingredients = product.optString("ingredients_text", "Ingredients not available");
        currentProduct.allergens = product.optString("allergens", "No allergen information");
        
        displayProductDetails();
    }
    
    private void createDemoProduct(String barcode) {
        Log.d(TAG, "Creating demo product for barcode: " + barcode);
        
        // Create different demo products based on barcode for variety
        currentProduct = new ProductInfo();
        currentProduct.barcode = barcode;
        
        // Use barcode to determine product type
        int productType = Math.abs(barcode.hashCode()) % 4;
        
        switch (productType) {
            case 0: // Healthy cereal
                currentProduct.name = "Organic Whole Grain Cereal";
                currentProduct.brand = "HealthyChoice";
                currentProduct.servingSize = "30g (1/2 cup)";
                currentProduct.calories = 350;
                currentProduct.protein = 12.5;
                currentProduct.sugar = 8.2;
                currentProduct.fat = 4.1;
                currentProduct.carbs = 68.0;
                currentProduct.fiber = 9.5;
                currentProduct.sodium = 180;
                currentProduct.ingredients = "Whole grain oats, whole grain wheat, brown rice, honey, almonds, natural vanilla flavor, sea salt, vitamin E (mixed tocopherols)";
                currentProduct.allergens = "Contains: Tree nuts (almonds). May contain: Soy, milk";
                break;
                
            case 1: // Yogurt
                currentProduct.name = "Greek Yogurt Natural";
                currentProduct.brand = "FreshDairy";
                currentProduct.servingSize = "150g (1 cup)";
                currentProduct.calories = 130;
                currentProduct.protein = 18.0;
                currentProduct.sugar = 6.5;
                currentProduct.fat = 3.2;
                currentProduct.carbs = 9.0;
                currentProduct.fiber = 0.0;
                currentProduct.sodium = 65;
                currentProduct.ingredients = "Pasteurized milk, live active cultures (L. bulgaricus, S. thermophilus, L. acidophilus, Bifidus, L. casei)";
                currentProduct.allergens = "Contains: Milk";
                break;
                
            case 2: // Chocolate bar (less healthy)
                currentProduct.name = "Dark Chocolate Bar";
                currentProduct.brand = "SweetTreats";
                currentProduct.servingSize = "40g (4 squares)";
                currentProduct.calories = 540;
                currentProduct.protein = 7.8;
                currentProduct.sugar = 24.0;
                currentProduct.fat = 31.0;
                currentProduct.carbs = 61.0;
                currentProduct.fiber = 11.0;
                currentProduct.sodium = 20;
                currentProduct.ingredients = "Cocoa mass, sugar, cocoa butter, emulsifier (soy lecithin), natural vanilla flavoring";
                currentProduct.allergens = "May contain: Milk, nuts, soy";
                break;
                
            case 3: // Apple (very healthy)
                currentProduct.name = "Fresh Red Apple";
                currentProduct.brand = "Nature's Best";
                currentProduct.servingSize = "182g (1 medium apple)";
                currentProduct.calories = 52;
                currentProduct.protein = 0.3;
                currentProduct.sugar = 10.4;
                currentProduct.fat = 0.2;
                currentProduct.carbs = 13.8;
                currentProduct.fiber = 2.4;
                currentProduct.sodium = 1;
                currentProduct.ingredients = "Fresh apple";
                currentProduct.allergens = "None";
                break;
        }
        
        Log.d(TAG, "Demo product created: " + currentProduct.name + ", displaying details");
        displayProductDetails();
    }
    
    private void displayProductDetails() {
        Log.d(TAG, "Displaying product details for: " + currentProduct.name);
        
        // Populate basic info
        populateBasicInfo();
        Log.d(TAG, "Basic info populated");
        
        // Calculate and display health score
        calculateHealthScore();
        Log.d(TAG, "Health score calculated");
        
        // Populate nutritional information
        populateNutritionalInfo();
        Log.d(TAG, "Nutritional info populated");
        
        // Save to scan history
        saveToScanHistory();
        Log.d(TAG, "Product details display completed");
    }
    
    private void populateBasicInfo() {
        if (productName != null) productName.setText(currentProduct.name);
        if (productBrand != null) productBrand.setText(currentProduct.brand);
        if (productBarcode != null) productBarcode.setText("Barcode: " + currentProduct.barcode);
        if (servingSizeText != null) servingSizeText.setText("Per " + currentProduct.servingSize);
        if (ingredientsText != null) ingredientsText.setText(currentProduct.ingredients);
        if (allergensText != null) allergensText.setText(currentProduct.allergens);
    }
    
    private void calculateHealthScore() {
        categoryScores.clear();
        
        // 1. Calorie Density Score (20 points)
        double calorieScore = calculateCalorieScore(currentProduct.calories);
        categoryScores.add(new CategoryScoresAdapter.CategoryScore("Calorie Density", calorieScore, 20, getCalorieAnalysis()));
        
        // 2. Sugar Content Score (20 points)
        double sugarScore = calculateSugarScore(currentProduct.sugar);
        categoryScores.add(new CategoryScoresAdapter.CategoryScore("Sugar Content", sugarScore, 20, getSugarAnalysis()));
        
        // 3. Fat Quality Score (15 points)
        double fatScore = calculateFatScore(currentProduct.fat);
        categoryScores.add(new CategoryScoresAdapter.CategoryScore("Fat Content", fatScore, 15, getFatAnalysis()));
        
        // 4. Protein Content Score (15 points)
        double proteinScore = calculateProteinScore(currentProduct.protein);
        categoryScores.add(new CategoryScoresAdapter.CategoryScore("Protein Content", proteinScore, 15, getProteinAnalysis()));
        
        // 5. Fiber Content Score (10 points)
        double fiberScore = calculateFiberScore(currentProduct.fiber);
        categoryScores.add(new CategoryScoresAdapter.CategoryScore("Fiber Content", fiberScore, 10, getFiberAnalysis()));
        
        // 6. Sodium Content Score (10 points)
        double sodiumScore = calculateSodiumScore(currentProduct.sodium);
        categoryScores.add(new CategoryScoresAdapter.CategoryScore("Sodium Content", sodiumScore, 10, getSodiumAnalysis()));
        
        // 7. Processing Level Score (10 points)
        double processingScore = calculateProcessingScore(currentProduct.ingredients);
        categoryScores.add(new CategoryScoresAdapter.CategoryScore("Processing Level", processingScore, 10, getProcessingAnalysis()));
        
        // Calculate total score
        double totalScore = calorieScore + sugarScore + fatScore + proteinScore + fiberScore + sodiumScore + processingScore;
        
        // Display health score
        displayHealthScore(totalScore);
        
        // Update adapter
        if (categoryAdapter != null) {
            categoryAdapter.notifyDataSetChanged();
        }
    }
    
    private double calculateCalorieScore(double calories) {
        if (calories <= 100) return 20;
        else if (calories <= 200) return 16;
        else if (calories <= 300) return 12;
        else if (calories <= 400) return 8;
        else if (calories <= 500) return 4;
        else return 0;
    }
    
    private double calculateSugarScore(double sugar) {
        if (sugar <= 2) return 20;
        else if (sugar <= 5) return 16;
        else if (sugar <= 10) return 12;
        else if (sugar <= 15) return 8;
        else if (sugar <= 20) return 4;
        else return 0;
    }
    
    private double calculateFatScore(double fat) {
        if (fat <= 3) return 15;
        else if (fat <= 10) return 12;
        else if (fat <= 15) return 8;
        else if (fat <= 20) return 4;
        else return 0;
    }
    
    private double calculateProteinScore(double protein) {
        if (protein >= 20) return 15;
        else if (protein >= 15) return 12;
        else if (protein >= 10) return 9;
        else if (protein >= 5) return 6;
        else if (protein >= 2) return 3;
        else return 0;
    }
    
    private double calculateFiberScore(double fiber) {
        if (fiber >= 10) return 10;
        else if (fiber >= 6) return 8;
        else if (fiber >= 3) return 6;
        else if (fiber >= 1.5) return 4;
        else if (fiber >= 0.5) return 2;
        else return 0;
    }
    
    private double calculateSodiumScore(double sodium) {
        if (sodium <= 100) return 10;
        else if (sodium <= 300) return 8;
        else if (sodium <= 600) return 6;
        else if (sodium <= 1000) return 4;
        else if (sodium <= 1500) return 2;
        else return 0;
    }
    
    private double calculateProcessingScore(String ingredients) {
        if (ingredients == null) return 5;
        
        String lowerIngredients = ingredients.toLowerCase();
        int artificialCount = 0;
        
        // Check for artificial ingredients
        String[] artificialTerms = {"artificial", "preservative", "color", "flavor enhancer", "stabilizer", "emulsifier"};
        for (String term : artificialTerms) {
            if (lowerIngredients.contains(term)) artificialCount++;
        }
        
        if (artificialCount == 0) return 10;
        else if (artificialCount <= 2) return 7;
        else if (artificialCount <= 4) return 4;
        else return 0;
    }
    
    private void displayHealthScore(double score) {
        if (healthScoreNumber != null) {
            healthScoreNumber.setText(String.format("%.0f", score));
        }
        
        if (healthScoreText != null) {
            healthScoreText.setText(getHealthScoreDescription(score));
        }
        
        // Update progress bar
        android.widget.ProgressBar healthScoreProgress = findViewById(R.id.health_score_progress);
        if (healthScoreProgress != null) {
            healthScoreProgress.setProgress((int) score);
            
            // Set color based on score
            int color;
            if (score >= 85) {
                color = ContextCompat.getColor(this, R.color.health_excellent);
            } else if (score >= 70) {
                color = ContextCompat.getColor(this, R.color.health_good);
            } else if (score >= 55) {
                color = ContextCompat.getColor(this, R.color.health_moderate);
            } else if (score >= 40) {
                color = ContextCompat.getColor(this, R.color.health_poor);
            } else {
                color = ContextCompat.getColor(this, R.color.health_unhealthy);
            }
            healthScoreProgress.setProgressTintList(android.content.res.ColorStateList.valueOf(color));
        }
        
        Log.d(TAG, "Health score displayed: " + score + " - " + getHealthScoreDescription(score));
    }
    
    private String getHealthScoreDescription(double score) {
        if (score >= 85) return "Excellent - Very Healthy Choice";
        else if (score >= 70) return "Good - Healthy Option";
        else if (score >= 55) return "Fair - Moderate Choice";
        else if (score >= 40) return "Poor - Consider Alternatives";
        else return "Very Poor - Avoid if Possible";
    }
    
    private void populateNutritionalInfo() {
        // Populate values
        if (caloriesValue != null) caloriesValue.setText(String.format("%.0f kcal", currentProduct.calories));
        if (proteinValue != null) proteinValue.setText(String.format("%.1fg", currentProduct.protein));
        if (sugarValue != null) sugarValue.setText(String.format("%.1fg", currentProduct.sugar));
        if (fatValue != null) fatValue.setText(String.format("%.1fg", currentProduct.fat));
        if (carbsValue != null) carbsValue.setText(String.format("%.1fg", currentProduct.carbs));
        if (sodiumValue != null) sodiumValue.setText(String.format("%.0fmg", currentProduct.sodium));
    }
    
    // Analysis methods for each category
    private String getCalorieAnalysis() {
        if (currentProduct.calories <= 100) return "Low calorie density - Great for weight management";
        else if (currentProduct.calories <= 300) return "Moderate calorie density - Reasonable portion control needed";
        else return "High calorie density - Consider smaller portions";
    }
    
    private String getSugarAnalysis() {
        if (currentProduct.sugar <= 5) return "Low sugar content - Excellent choice";
        else if (currentProduct.sugar <= 15) return "Moderate sugar content - Consume in moderation";
        else return "High sugar content - Limit consumption";
    }
    
    private String getFatAnalysis() {
        if (currentProduct.fat <= 3) return "Low fat content - Heart healthy";
        else if (currentProduct.fat <= 15) return "Moderate fat content - Watch portions";
        else return "High fat content - Consider alternatives";
    }
    
    private String getProteinAnalysis() {
        if (currentProduct.protein >= 15) return "High protein content - Great for muscle health";
        else if (currentProduct.protein >= 5) return "Moderate protein content - Good source";
        else return "Low protein content - Consider protein-rich alternatives";
    }
    
    private String getFiberAnalysis() {
        if (currentProduct.fiber >= 6) return "High fiber content - Excellent for digestion";
        else if (currentProduct.fiber >= 3) return "Good fiber content - Supports digestive health";
        else return "Low fiber content - Add more fiber-rich foods";
    }
    
    private String getSodiumAnalysis() {
        if (currentProduct.sodium <= 300) return "Low sodium content - Heart healthy choice";
        else if (currentProduct.sodium <= 1000) return "Moderate sodium content - Monitor daily intake";
        else return "High sodium content - Limit consumption";
    }
    
    private String getProcessingAnalysis() {
        return "Based on ingredient analysis - Natural ingredients score higher";
    }
    
    private void showError(String message) {
        Log.e(TAG, "Error: " + message);
    }
    
    private void saveToScanHistory() {
        try {
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            String historyJson = prefs.getString("recent_scans", "[]");
            
            org.json.JSONArray historyArray = new org.json.JSONArray(historyJson);
            org.json.JSONObject productJson = new org.json.JSONObject();
            
            productJson.put("name", currentProduct.name);
            productJson.put("brand", currentProduct.brand);
            productJson.put("barcode", currentProduct.barcode);
            productJson.put("calories", currentProduct.calories);
            productJson.put("healthScore", calculateTotalScore());
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
            
            prefs.edit().putString("recent_scans", historyArray.toString()).apply();
            Log.d(TAG, "Product saved to scan history");
            
        } catch (org.json.JSONException e) {
            Log.e(TAG, "Error saving to scan history", e);
        }
    }
    
    private double calculateTotalScore() {
        double total = 0;
        for (CategoryScoresAdapter.CategoryScore score : categoryScores) {
            total += score.score;
        }
        return total;
    }
    
    @Override
    protected int getCurrentNavigationItemId() {
        return R.id.nav_scan;
    }
    
    // Data class
    private static class ProductInfo {
        String barcode, name, brand, servingSize;
        String ingredients, allergens;
        double calories, protein, sugar, fat;
        double carbs, fiber, sodium;
    }
}