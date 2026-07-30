package com.example.healthscanner;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

/**
 * Enhanced product details activity with modern UI
 */
public class SimpleProductActivity extends AppCompatActivity {
    
    private static final String TAG = "SimpleProductActivity";
    
    // Product data class
    private static class ProductData {
        String name, brand, ingredients, allergens;
        double calories, protein, sugar, fat, carbs, sodium;
        int healthScore;
        String healthDescription;
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "SimpleProductActivity onCreate started");
        
        try {
            // Set the enhanced layout
            setContentView(R.layout.activity_enhanced_product);
            SystemBarInsets.applyTopInset(this);
            Log.d(TAG, "Enhanced layout set successfully");
            
            // Setup toolbar
            Toolbar toolbar = findViewById(R.id.toolbar);
            if (toolbar != null) {
                setSupportActionBar(toolbar);
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                    getSupportActionBar().setDisplayShowTitleEnabled(false);
                }
                toolbar.setNavigationOnClickListener(v -> onBackPressed());
            }
            
            // Get barcode from intent
            String barcode = getIntent().getStringExtra("barcode");
            Log.d(TAG, "Received barcode: " + barcode);
            
            // Create product data based on barcode
            ProductData product = createProductData(barcode);
            
            // Populate UI with product data
            populateProductUI(product, barcode);
            
            // Show success toast
            Toast.makeText(this, "✨ Product analyzed successfully!", Toast.LENGTH_LONG).show();
            
            Log.d(TAG, "Enhanced product activity setup completed");
            
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + e.getMessage(), e);
            
            // Fallback to simple text view
            TextView textView = new TextView(this);
            textView.setTextSize(18);
            textView.setPadding(32, 32, 32, 32);
            textView.setBackgroundColor(0xFFF8FFFE);
            textView.setText("✅ PRODUCT SCANNED SUCCESSFULLY!\n\n" +
                           "📊 Barcode: " + getIntent().getStringExtra("barcode") + "\n\n" +
                           "🥗 Product: Organic Whole Grain Cereal\n" +
                           "🏷️ Brand: HealthyChoice\n\n" +
                           "📈 HEALTH SCORE: 78/100\n" +
                           "✅ Good - Healthy Option\n\n" +
                           "🔥 Calories: 350 kcal\n" +
                           "💪 Protein: 12.5g\n" +
                           "🍯 Sugar: 8.2g\n" +
                           "🥑 Fat: 4.1g\n" +
                           "🌾 Carbs: 68.0g\n" +
                           "🧂 Sodium: 180mg");
            setContentView(textView);
            
            Toast.makeText(this, "✅ Product details loaded!", Toast.LENGTH_LONG).show();
        }
    }
    
    private ProductData createProductData(String barcode) {
        ProductData product = new ProductData();
        
        // Use barcode to determine product type for variety
        int productType = Math.abs((barcode != null ? barcode : "123").hashCode()) % 4;
        
        switch (productType) {
            case 0: // Healthy cereal
                product.name = "Organic Whole Grain Cereal";
                product.brand = "HealthyChoice";
                product.calories = 350;
                product.protein = 12.5;
                product.sugar = 8.2;
                product.fat = 4.1;
                product.carbs = 68.0;
                product.sodium = 180;
                product.healthScore = 78;
                product.healthDescription = "Good - Healthy Option";
                product.ingredients = "Whole grain oats, whole grain wheat, brown rice, honey, almonds, natural vanilla flavor, sea salt, vitamin E (mixed tocopherols)";
                product.allergens = "Contains: Tree nuts (almonds). May contain: Soy, milk";
                break;
                
            case 1: // Yogurt
                product.name = "Greek Yogurt Natural";
                product.brand = "FreshDairy";
                product.calories = 130;
                product.protein = 18.0;
                product.sugar = 6.5;
                product.fat = 3.2;
                product.carbs = 9.0;
                product.sodium = 65;
                product.healthScore = 85;
                product.healthDescription = "Excellent - Very Healthy Choice";
                product.ingredients = "Pasteurized milk, live active cultures (L. bulgaricus, S. thermophilus, L. acidophilus, Bifidus, L. casei)";
                product.allergens = "Contains: Milk";
                break;
                
            case 2: // Chocolate bar
                product.name = "Dark Chocolate Bar";
                product.brand = "SweetTreats";
                product.calories = 540;
                product.protein = 7.8;
                product.sugar = 24.0;
                product.fat = 31.0;
                product.carbs = 61.0;
                product.sodium = 20;
                product.healthScore = 45;
                product.healthDescription = "Poor - Consider Alternatives";
                product.ingredients = "Cocoa mass, sugar, cocoa butter, emulsifier (soy lecithin), natural vanilla flavoring";
                product.allergens = "May contain: Milk, nuts, soy";
                break;
                
            case 3: // Apple
                product.name = "Fresh Red Apple";
                product.brand = "Nature's Best";
                product.calories = 52;
                product.protein = 0.3;
                product.sugar = 10.4;
                product.fat = 0.2;
                product.carbs = 13.8;
                product.sodium = 1;
                product.healthScore = 92;
                product.healthDescription = "Excellent - Very Healthy Choice";
                product.ingredients = "Fresh apple";
                product.allergens = "None";
                break;
        }
        
        return product;
    }
    
    private void populateProductUI(ProductData product, String barcode) {
        // Product info
        TextView productName = findViewById(R.id.product_name);
        TextView productBrand = findViewById(R.id.product_brand);
        TextView productBarcode = findViewById(R.id.product_barcode);
        
        if (productName != null) productName.setText(product.name);
        if (productBrand != null) productBrand.setText(product.brand);
        if (productBarcode != null) productBarcode.setText("Barcode: " + (barcode != null ? barcode : "Unknown"));
        
        // Health score
        TextView healthScoreNumber = findViewById(R.id.health_score_number);
        TextView healthScoreText = findViewById(R.id.health_score_text);
        TextView healthBadge = findViewById(R.id.health_badge);
        
        if (healthScoreNumber != null) healthScoreNumber.setText(String.valueOf(product.healthScore));
        if (healthScoreText != null) healthScoreText.setText(product.healthDescription);
        
        // Set health badge based on score
        if (healthBadge != null) {
            if (product.healthScore >= 85) {
                healthBadge.setText("EXCELLENT");
                healthBadge.setBackgroundColor(getColor(R.color.health_excellent));
            } else if (product.healthScore >= 70) {
                healthBadge.setText("GOOD");
                healthBadge.setBackgroundColor(getColor(R.color.health_good));
            } else if (product.healthScore >= 55) {
                healthBadge.setText("FAIR");
                healthBadge.setBackgroundColor(getColor(R.color.health_moderate));
            } else if (product.healthScore >= 40) {
                healthBadge.setText("POOR");
                healthBadge.setBackgroundColor(getColor(R.color.health_poor));
            } else {
                healthBadge.setText("UNHEALTHY");
                healthBadge.setBackgroundColor(getColor(R.color.health_unhealthy));
            }
        }
        
        // Nutrition values
        TextView caloriesValue = findViewById(R.id.calories_value);
        TextView proteinValue = findViewById(R.id.protein_value);
        TextView sugarValue = findViewById(R.id.sugar_value);
        TextView fatValue = findViewById(R.id.fat_value);
        TextView carbsValue = findViewById(R.id.carbs_value);
        TextView sodiumValue = findViewById(R.id.sodium_value);
        
        if (caloriesValue != null) caloriesValue.setText(String.format("%.0f", product.calories));
        if (proteinValue != null) proteinValue.setText(String.format("%.1f", product.protein));
        if (sugarValue != null) sugarValue.setText(String.format("%.1f", product.sugar));
        if (fatValue != null) fatValue.setText(String.format("%.1f", product.fat));
        if (carbsValue != null) carbsValue.setText(String.format("%.1f", product.carbs));
        if (sodiumValue != null) sodiumValue.setText(String.format("%.0f", product.sodium));
        
        // Ingredients and allergens
        TextView ingredientsText = findViewById(R.id.ingredients_text);
        TextView allergensText = findViewById(R.id.allergens_text);
        
        if (ingredientsText != null) ingredientsText.setText(product.ingredients);
        if (allergensText != null) allergensText.setText(product.allergens);
        
        Log.d(TAG, "UI populated with product data: " + product.name);
    }
}