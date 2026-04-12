package com.example.healthscanner.network;

import android.util.Log;

import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class BarcodeApiClient {
    private static final String TAG = "BarcodeApiClient";
    private static final String BASE_URL = "https://world.openfoodfacts.org/api/v0/product/";
    
    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_BACKOFF_MS = 1000;

    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build();

    public interface BarcodeCallback {
        void onSuccess(String sanitizedProductName);
        void onFailure(String errorMsg);
    }

    public static void fetchProductNameWithRetry(String barcode, BarcodeCallback callback) {
        new Thread(() -> {
            int attempt = 0;
            long currentBackoff = INITIAL_BACKOFF_MS;

            while (attempt < MAX_RETRIES) {
                try {
                    String url = BASE_URL + barcode + ".json";
                    Request request = new Request.Builder()
                            .url(url)
                            .get()
                            .build();

                    try (Response response = client.newCall(request).execute()) {
                        if (response.isSuccessful() && response.body() != null) {
                            String responseBody = response.body().string();
                            JSONObject json = new JSONObject(responseBody);
                            
                            if (json.has("status") && json.getInt("status") == 1) {
                                JSONObject product = json.getJSONObject("product");
                                String productName = product.optString("product_name", "");
                                
                                if (!productName.isEmpty()) {
                                    String sanitized = sanitizeLabel(productName);
                                    callback.onSuccess(sanitized);
                                    return; // Success, exit thread
                                }
                            }
                            
                            // If status=0 or product_name is empty, this means it's valid API call but no product found.
                            // We don't retry on 404-like logic, only on network failures.
                            break; 
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Network attempt " + (attempt + 1) + " failed: " + e.getMessage());
                }

                // If not successful and we caught an exception or bad network response, retry
                attempt++;
                if (attempt < MAX_RETRIES) {
                    try {
                        Thread.sleep(currentBackoff);
                        currentBackoff *= 2; // Exponential Backoff
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
            
            // If the loop exits here, we failed all retries or explicitly broke out because product wasn't found in DB
            callback.onFailure("Failed to resolve product after retries or product missing in DB.");
        }).start();
    }

    private static String sanitizeLabel(String rawLabel) {
        if (rawLabel == null) return "unknown";
        
        // Lowercase, trim, remove special chars (keep alphanumeric and spaces)
        String cleaned = rawLabel.toLowerCase().trim();
        cleaned = cleaned.replaceAll("[^a-z0-9\\s]", " ");
        // Collapse multiple spaces into single space
        cleaned = cleaned.replaceAll("\\s+", " ").trim();
        
        return cleaned.isEmpty() ? "unknown" : cleaned;
    }
}
