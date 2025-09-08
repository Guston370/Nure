package com.example.healthscanner;

/**
 * API Configuration class for HealthScanner app
 * ⚠️ TEMPORARY: API Keys in code (not secure for production)
 * In production, these should be stored securely or retrieved from a secure server
 */
public class ApiConfig {
    
    // USDA API - Food Data Central
    public static final String USDA_API_KEY = "z2yqsAgdXihaAA3KVcz2A8Mtcgsuqb97lw2dGq2a";
    public static final String USDA_BASE_URL = "https://api.nal.usda.gov/fdc/v1/";
    
    // Spoonacular API - Recipe and Food Information
    public static final String SPOONACULAR_API_KEY = "24becc6301da40ff89c93d2658cf2006";
    public static final String SPOONACULAR_BASE_URL = "https://api.spoonacular.com/";
    
    // Nutritionix API - Nutrition Database
    public static final String NUTRITIONIX_APP_ID = "329c56f2";
    public static final String NUTRITIONIX_API_KEY = "20ac7538a21f810c6d5bc2e523facd00";
    public static final String NUTRITIONIX_BASE_URL = "https://trackapi.nutritionix.com/v2/";
    
    // UPCItemDB API - Universal Product Code Database
    public static final String UPCITEMDB_API_URL = "https://api.upcitemdb.com/prod/trial/lookup?upc=";
    
    // Open Food Facts API (existing)
    public static final String OPENFOODFACTS_BASE_URL = "https://world.openfoodfacts.org/api/v0/product/";
    
    // API Priority Order (fallback sequence)
    public static final String[] API_PRIORITY = {
        "OPENFOODFACTS",  // Primary - most comprehensive
        "NUTRITIONIX",    // Secondary - good nutrition data
        "UPCITEMDB",      // Tertiary - basic product info
        "USDA",           // Quaternary - government data
        "SPOONACULAR"     // Last resort - recipe-based
    };
    
    // API Endpoints
    public static class Endpoints {
        // Open Food Facts
        public static String getOpenFoodFactsUrl(String barcode) {
            return OPENFOODFACTS_BASE_URL + barcode + ".json";
        }
        
        // Nutritionix
        public static String getNutritionixUrl() {
            return NUTRITIONIX_BASE_URL + "search/item?upc=";
        }
        
        // UPCItemDB
        public static String getUPCItemDBUrl(String barcode) {
            return UPCITEMDB_API_URL + barcode;
        }
        
        // USDA
        public static String getUSDAUrl(String barcode) {
            return USDA_BASE_URL + "foods/search?query=" + barcode + "&api_key=" + USDA_API_KEY;
        }
        
        // Spoonacular
        public static String getSpoonacularUrl(String barcode) {
            return SPOONACULAR_BASE_URL + "food/products/upc/" + barcode + "?apiKey=" + SPOONACULAR_API_KEY;
        }
    }
    
    // API Headers
    public static class Headers {
        // Nutritionix headers
        public static String getNutritionixAppId() {
            return NUTRITIONIX_APP_ID;
        }
        
        public static String getNutritionixApiKey() {
            return NUTRITIONIX_API_KEY;
        }
    }
}
