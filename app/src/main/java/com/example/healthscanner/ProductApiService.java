package com.example.healthscanner;

import android.content.Context;
import android.util.Log;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;
import java.util.HashMap;
import java.util.Map;

/**
 * Comprehensive API service for fetching product information from multiple sources
 */
public class ProductApiService {
    private static final String TAG = "ProductApiService";
    
    private RequestQueue requestQueue;
    private Context context;
    private int currentApiIndex = 0;
    
    public interface ProductCallback {
        void onSuccess(ProductInfo product);
        void onError(String error);
    }
    
    public static class ProductInfo {
        public String name;
        public String brand;
        public String barcode;
        public int calories;
        public double protein;
        public double sugar;
        public double fat;
        public double carbs;
        public double sodium;
        public double fiber;
        public String ingredients;
        public String imageUrl;
        public String source;
        public double healthScore;
        
        public ProductInfo() {
            this.name = "Unknown Product";
            this.brand = "Unknown Brand";
            this.calories = 0;
            this.protein = 0.0;
            this.sugar = 0.0;
            this.fat = 0.0;
            this.carbs = 0.0;
            this.sodium = 0.0;
            this.fiber = 0.0;
            this.ingredients = "Not available";
            this.imageUrl = "";
            this.source = "Unknown";
            this.healthScore = 0.0;
        }
    }
    
    public ProductApiService(Context context) {
        this.context = context;
        this.requestQueue = Volley.newRequestQueue(context);
    }
    
    public void fetchProductInfo(String barcode, ProductCallback callback) {
        currentApiIndex = 0;
        tryNextApi(barcode, callback);
    }
    
    private void tryNextApi(String barcode, ProductCallback callback) {
        if (currentApiIndex >= ApiConfig.API_PRIORITY.length) {
            callback.onError("Product not found in any database");
            return;
        }
        
        String apiName = ApiConfig.API_PRIORITY[currentApiIndex];
        Log.d(TAG, "Trying API: " + apiName + " for barcode: " + barcode);
        
        switch (apiName) {
            case "OPENFOODFACTS":
                tryOpenFoodFacts(barcode, callback);
                break;
            case "UPC_DATABASE":
                tryUPCDatabase(barcode, callback);
                break;
            case "NUTRITIONIX":
                if (ApiConfig.isNutritionixConfigured()) {
                    tryNutritionix(barcode, callback);
                } else {
                    moveToNextApi(barcode, callback);
                }
                break;
            case "SPOONACULAR":
                if (ApiConfig.isSpoonacularConfigured()) {
                    trySpoonacular(barcode, callback);
                } else {
                    moveToNextApi(barcode, callback);
                }
                break;
            case "USDA":
                if (ApiConfig.isUSDAConfigured()) {
                    tryUSDA(barcode, callback);
                } else {
                    moveToNextApi(barcode, callback);
                }
                break;
            case "EDAMAM":
                if (ApiConfig.isEdamamConfigured()) {
                    tryEdamam(barcode, callback);
                } else {
                    moveToNextApi(barcode, callback);
                }
                break;
            default:
                moveToNextApi(barcode, callback);
                break;
        }
    }
    
    private void moveToNextApi(String barcode, ProductCallback callback) {
        currentApiIndex++;
        tryNextApi(barcode, callback);
    }
    
    private void tryOpenFoodFacts(String barcode, ProductCallback callback) {
        String url = ApiConfig.OPENFOODFACTS_BASE_URL + barcode + ".json";
        
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
            response -> {
                try {
                    if (response.getInt("status") == 1) {
                        ProductInfo product = parseOpenFoodFactsResponse(response.getJSONObject("product"), barcode);
                        callback.onSuccess(product);
                    } else {
                        moveToNextApi(barcode, callback);
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "OpenFoodFacts parsing error", e);
                    moveToNextApi(barcode, callback);
                }
            },
            error -> {
                Log.e(TAG, "OpenFoodFacts API error", error);
                moveToNextApi(barcode, callback);
            });
        
        requestQueue.add(request);
    }
    
    private void tryUPCDatabase(String barcode, ProductCallback callback) {
        String url = ApiConfig.UPC_DATABASE_BASE_URL + "?upc=" + barcode;
        
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
            response -> {
                try {
                    if (response.getString("code").equals("OK") && response.has("items")) {
                        JSONArray items = response.getJSONArray("items");
                        if (items.length() > 0) {
                            ProductInfo product = parseUPCDatabaseResponse(items.getJSONObject(0), barcode);
                            callback.onSuccess(product);
                        } else {
                            moveToNextApi(barcode, callback);
                        }
                    } else {
                        moveToNextApi(barcode, callback);
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "UPC Database parsing error", e);
                    moveToNextApi(barcode, callback);
                }
            },
            error -> {
                Log.e(TAG, "UPC Database API error", error);
                moveToNextApi(barcode, callback);
            });
        
        requestQueue.add(request);
    }
    
    private void tryNutritionix(String barcode, ProductCallback callback) {
        String url = ApiConfig.NUTRITIONIX_BASE_URL + "?upc=" + barcode;
        
        Map<String, String> headers = new HashMap<>();
        headers.put("x-app-id", ApiConfig.NUTRITIONIX_APP_ID);
        headers.put("x-app-key", ApiConfig.NUTRITIONIX_API_KEY);
        
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
            response -> {
                try {
                    if (response.has("foods")) {
                        JSONArray foods = response.getJSONArray("foods");
                        if (foods.length() > 0) {
                            ProductInfo product = parseNutritionixResponse(foods.getJSONObject(0), barcode);
                            callback.onSuccess(product);
                        } else {
                            moveToNextApi(barcode, callback);
                        }
                    } else {
                        moveToNextApi(barcode, callback);
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Nutritionix parsing error", e);
                    moveToNextApi(barcode, callback);
                }
            },
            error -> {
                Log.e(TAG, "Nutritionix API error", error);
                moveToNextApi(barcode, callback);
            }) {
            @Override
            public Map<String, String> getHeaders() {
                return headers;
            }
        };
        
        requestQueue.add(request);
    }
    
    private void trySpoonacular(String barcode, ProductCallback callback) {
        String url = ApiConfig.SPOONACULAR_BASE_URL + barcode + "?apiKey=" + ApiConfig.SPOONACULAR_API_KEY;
        
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
            response -> {
                try {
                    ProductInfo product = parseSpoonacularResponse(response, barcode);
                    callback.onSuccess(product);
                } catch (JSONException e) {
                    Log.e(TAG, "Spoonacular parsing error", e);
                    moveToNextApi(barcode, callback);
                }
            },
            error -> {
                Log.e(TAG, "Spoonacular API error", error);
                moveToNextApi(barcode, callback);
            });
        
        requestQueue.add(request);
    }
    
    private void tryUSDA(String barcode, ProductCallback callback) {
        String url = ApiConfig.USDA_BASE_URL + "?api_key=" + ApiConfig.USDA_API_KEY + "&query=" + barcode;
        
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
            response -> {
                try {
                    if (response.has("foods")) {
                        JSONArray foods = response.getJSONArray("foods");
                        if (foods.length() > 0) {
                            ProductInfo product = parseUSDAResponse(foods.getJSONObject(0), barcode);
                            callback.onSuccess(product);
                        } else {
                            moveToNextApi(barcode, callback);
                        }
                    } else {
                        moveToNextApi(barcode, callback);
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "USDA parsing error", e);
                    moveToNextApi(barcode, callback);
                }
            },
            error -> {
                Log.e(TAG, "USDA API error", error);
                moveToNextApi(barcode, callback);
            });
        
        requestQueue.add(request);
    }
    
    private void tryEdamam(String barcode, ProductCallback callback) {
        String url = ApiConfig.EDAMAM_BASE_URL + "?app_id=" + ApiConfig.EDAMAM_APP_ID + 
                    "&app_key=" + ApiConfig.EDAMAM_API_KEY + "&upc=" + barcode;
        
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
            response -> {
                try {
                    if (response.has("parsed")) {
                        JSONArray parsed = response.getJSONArray("parsed");
                        if (parsed.length() > 0) {
                            ProductInfo product = parseEdamamResponse(parsed.getJSONObject(0), barcode);
                            callback.onSuccess(product);
                        } else {
                            moveToNextApi(barcode, callback);
                        }
                    } else {
                        moveToNextApi(barcode, callback);
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Edamam parsing error", e);
                    moveToNextApi(barcode, callback);
                }
            },
            error -> {
                Log.e(TAG, "Edamam API error", error);
                moveToNextApi(barcode, callback);
            });
        
        requestQueue.add(request);
    }
    
    // Response parsers for each API
    private ProductInfo parseOpenFoodFactsResponse(JSONObject product, String barcode) throws JSONException {
        ProductInfo info = new ProductInfo();
        info.barcode = barcode;
        info.name = product.optString("product_name", "Unknown Product");
        info.brand = product.optString("brands", "Unknown Brand");
        info.ingredients = product.optString("ingredients_text", "Not available");
        info.imageUrl = product.optString("image_url", "");
        info.source = "OpenFoodFacts";
        
        JSONObject nutriments = product.optJSONObject("nutriments");
        if (nutriments != null) {
            info.calories = (int) nutriments.optDouble("energy-kcal_100g", 0);
            info.protein = nutriments.optDouble("proteins_100g", 0);
            info.sugar = nutriments.optDouble("sugars_100g", 0);
            info.fat = nutriments.optDouble("fat_100g", 0);
            info.carbs = nutriments.optDouble("carbohydrates_100g", 0);
            info.sodium = nutriments.optDouble("sodium_100g", 0);
            info.fiber = nutriments.optDouble("fiber_100g", 0);
        }
        
        info.healthScore = calculateHealthScore(info);
        return info;
    }
    
    private ProductInfo parseUPCDatabaseResponse(JSONObject item, String barcode) throws JSONException {
        ProductInfo info = new ProductInfo();
        info.barcode = barcode;
        info.name = item.optString("title", "Unknown Product");
        info.brand = item.optString("brand", "Unknown Brand");
        info.source = "UPC Database";
        // UPC Database doesn't provide nutrition info
        info.healthScore = 5.0; // Neutral score
        return info;
    }
    
    private ProductInfo parseNutritionixResponse(JSONObject food, String barcode) throws JSONException {
        ProductInfo info = new ProductInfo();
        info.barcode = barcode;
        info.name = food.optString("food_name", "Unknown Product");
        info.brand = food.optString("brand_name", "Unknown Brand");
        info.source = "Nutritionix";
        
        info.calories = (int) food.optDouble("nf_calories", 0);
        info.protein = food.optDouble("nf_protein", 0);
        info.sugar = food.optDouble("nf_sugars", 0);
        info.fat = food.optDouble("nf_total_fat", 0);
        info.carbs = food.optDouble("nf_total_carbohydrate", 0);
        info.sodium = food.optDouble("nf_sodium", 0);
        info.fiber = food.optDouble("nf_dietary_fiber", 0);
        
        info.healthScore = calculateHealthScore(info);
        return info;
    }
    
    private ProductInfo parseSpoonacularResponse(JSONObject response, String barcode) throws JSONException {
        ProductInfo info = new ProductInfo();
        info.barcode = barcode;
        info.name = response.optString("title", "Unknown Product");
        info.brand = response.optString("brand", "Unknown Brand");
        info.source = "Spoonacular";
        
        JSONObject nutrition = response.optJSONObject("nutrition");
        if (nutrition != null) {
            JSONArray nutrients = nutrition.optJSONArray("nutrients");
            if (nutrients != null) {
                for (int i = 0; i < nutrients.length(); i++) {
                    JSONObject nutrient = nutrients.getJSONObject(i);
                    String name = nutrient.optString("name", "").toLowerCase();
                    double amount = nutrient.optDouble("amount", 0);
                    
                    if (name.contains("calories")) info.calories = (int) amount;
                    else if (name.contains("protein")) info.protein = amount;
                    else if (name.contains("sugar")) info.sugar = amount;
                    else if (name.contains("fat")) info.fat = amount;
                    else if (name.contains("carbohydrate")) info.carbs = amount;
                    else if (name.contains("sodium")) info.sodium = amount;
                    else if (name.contains("fiber")) info.fiber = amount;
                }
            }
        }
        
        info.healthScore = calculateHealthScore(info);
        return info;
    }
    
    private ProductInfo parseUSDAResponse(JSONObject food, String barcode) throws JSONException {
        ProductInfo info = new ProductInfo();
        info.barcode = barcode;
        info.name = food.optString("description", "Unknown Product");
        info.brand = "USDA Database";
        info.source = "USDA";
        
        JSONArray nutrients = food.optJSONArray("foodNutrients");
        if (nutrients != null) {
            for (int i = 0; i < nutrients.length(); i++) {
                JSONObject nutrient = nutrients.getJSONObject(i);
                String name = nutrient.optString("nutrientName", "").toLowerCase();
                double amount = nutrient.optDouble("amount", 0);
                
                if (name.contains("energy")) info.calories = (int) amount;
                else if (name.contains("protein")) info.protein = amount;
                else if (name.contains("sugar")) info.sugar = amount;
                else if (name.contains("fat")) info.fat = amount;
                else if (name.contains("carbohydrate")) info.carbs = amount;
                else if (name.contains("sodium")) info.sodium = amount;
                else if (name.contains("fiber")) info.fiber = amount;
            }
        }
        
        info.healthScore = calculateHealthScore(info);
        return info;
    }
    
    private ProductInfo parseEdamamResponse(JSONObject parsed, String barcode) throws JSONException {
        ProductInfo info = new ProductInfo();
        info.barcode = barcode;
        info.source = "Edamam";
        
        JSONObject food = parsed.getJSONObject("food");
        info.name = food.optString("label", "Unknown Product");
        info.brand = food.optString("brand", "Unknown Brand");
        
        JSONObject nutrients = food.optJSONObject("nutrients");
        if (nutrients != null) {
            info.calories = (int) nutrients.optDouble("ENERC_KCAL", 0);
            info.protein = nutrients.optDouble("PROCNT", 0);
            info.sugar = nutrients.optDouble("SUGAR", 0);
            info.fat = nutrients.optDouble("FAT", 0);
            info.carbs = nutrients.optDouble("CHOCDF", 0);
            info.sodium = nutrients.optDouble("NA", 0);
            info.fiber = nutrients.optDouble("FIBTG", 0);
        }
        
        info.healthScore = calculateHealthScore(info);
        return info;
    }
    
    private double calculateHealthScore(ProductInfo info) {
        double score = 5.0; // Base score
        
        // Adjust based on calories (per 100g)
        if (info.calories < 100) score += 1.5;
        else if (info.calories > 300) score -= 1.0;
        
        // Adjust based on sugar
        if (info.sugar < 5) score += 2.0;
        else if (info.sugar > 15) score -= 1.5;
        
        // Adjust based on protein
        if (info.protein > 10) score += 1.0;
        
        // Adjust based on sodium
        if (info.sodium < 300) score += 0.5;
        else if (info.sodium > 1000) score -= 1.0;
        
        // Adjust based on fiber
        if (info.fiber > 5) score += 1.0;
        
        return Math.max(0, Math.min(10, score));
    }
}