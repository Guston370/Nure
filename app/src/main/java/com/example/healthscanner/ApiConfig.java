package com.example.healthscanner;

/**
 * API Configuration class containing all API keys and endpoints.
 * In production, these should be stored securely or loaded from BuildConfig.
 */
public class ApiConfig {

    // ==================== NURE Food Recognition API ====================
    // Backend server running the ML food classifier with RLHF
    public static final String NURE_BASE_URL = "http://10.126.145.199:5000";

    // Food recognition via ML model (MobileNetV2 + Transfer Learning)
    public static final String API_URL_PREDICT = NURE_BASE_URL + "/predict";

    // OCR-based food detection fallback
    public static final String API_URL_OCR_DETECT = NURE_BASE_URL + "/ocr-detect";

    // RLHF: Store human feedback to improve the model
    public static final String API_URL_STORE_FEEDBACK = NURE_BASE_URL + "/store-feedback";

    // RLHF: Trigger model retraining with accumulated feedback
    public static final String API_URL_RETRAIN = NURE_BASE_URL + "/retrain";

    // RLHF: Get model performance statistics
    public static final String API_URL_MODEL_STATS = NURE_BASE_URL + "/model-stats";

    // ==================== External Food APIs ====================

    // OpenFoodFacts API (Free, no key required)
    public static final String OPENFOODFACTS_BASE_URL = "https://world.openfoodfacts.org/api/v0/product/";
    
    // UPC Database API (Free tier available)
    public static final String UPC_DATABASE_BASE_URL = "https://api.upcitemdb.com/prod/trial/lookup";
    
    // Nutritionix API (Requires API key)
    public static final String NUTRITIONIX_BASE_URL = "https://trackapi.nutritionix.com/v2/search/item";
    public static final String NUTRITIONIX_APP_ID = "your_nutritionix_app_id"; // Replace with actual key
    public static final String NUTRITIONIX_API_KEY = "your_nutritionix_api_key"; // Replace with actual key
    
    // Spoonacular API (Requires API key)
    public static final String SPOONACULAR_BASE_URL = "https://api.spoonacular.com/food/products/upc/";
    public static final String SPOONACULAR_API_KEY = "your_spoonacular_api_key"; // Replace with actual key
    
    // USDA FoodData Central API (Free, but requires API key)
    public static final String USDA_BASE_URL = "https://api.nal.usda.gov/fdc/v1/foods/search";
    public static final String USDA_API_KEY = "your_usda_api_key"; // Replace with actual key
    
    // Edamam Food Database API (Requires API key)
    public static final String EDAMAM_BASE_URL = "https://api.edamam.com/api/food-database/v2/parser";
    public static final String EDAMAM_APP_ID = "your_edamam_app_id"; // Replace with actual key
    public static final String EDAMAM_API_KEY = "your_edamam_api_key"; // Replace with actual key
    
    // API Priority Order (from most reliable to least)
    public static final String[] API_PRIORITY = {
        "OPENFOODFACTS",
        "UPC_DATABASE", 
        "NUTRITIONIX",
        "SPOONACULAR",
        "USDA",
        "EDAMAM"
    };
    
    // Check if API key is configured
    public static boolean isNutritionixConfigured() {
        return !NUTRITIONIX_API_KEY.equals("your_nutritionix_api_key") && 
               !NUTRITIONIX_APP_ID.equals("your_nutritionix_app_id");
    }
    
    public static boolean isSpoonacularConfigured() {
        return !SPOONACULAR_API_KEY.equals("your_spoonacular_api_key");
    }
    
    public static boolean isUSDAConfigured() {
        return !USDA_API_KEY.equals("your_usda_api_key");
    }
    
    public static boolean isEdamamConfigured() {
        return !EDAMAM_API_KEY.equals("your_edamam_api_key") && 
               !EDAMAM_APP_ID.equals("your_edamam_app_id");
    }
}