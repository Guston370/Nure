package com.example.healthscanner;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class DatabaseHelper {
    private static final String TAG = "DatabaseHelper";
    private static final String FILENAME = "products.json";

    public static JSONArray loadLocalDatabase(Context context) {
        File file = new File(context.getFilesDir(), FILENAME);
        
        // If it doesn't exist in Internal Storage, copy it from assets
        if (!file.exists()) {
            Log.d(TAG, "[DB] products.json not in internal storage — copying from assets...");
            copyAssetsToInternalStorage(context);
        }

        try {
            FileInputStream fis = new FileInputStream(file);
            int size = fis.available();
            byte[] buffer = new byte[size];
            fis.read(buffer);
            fis.close();
            String jsonStr = new String(buffer, StandardCharsets.UTF_8);
            JSONArray arr = new JSONArray(jsonStr);
            Log.d(TAG, "[DB] Loaded local database: " + arr.length() + " products from " + file.getAbsolutePath());
            return arr;
        } catch (Exception e) {
            Log.e(TAG, "[DB] Error parsing internal JSON DB: " + e.getMessage(), e);
            return new JSONArray();
        }
    }

    private static void copyAssetsToInternalStorage(Context context) {
        try {
            InputStream is = context.getAssets().open(FILENAME);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();

            File outFile = new File(context.getFilesDir(), FILENAME);
            FileOutputStream fos = new FileOutputStream(outFile);
            fos.write(buffer);
            fos.close();
            Log.d(TAG, "[DB] Successfully copied products.json from assets to " + outFile.getAbsolutePath());
        } catch (Exception e) {
            Log.e(TAG, "[DB] Error migrating assets DB to persistent storage: " + e.getMessage(), e);
        }
    }

    public static boolean isBarcodeMatch(String barcode1, String barcode2) {
        if (barcode1 == null || barcode2 == null) {
            return false;
        }
        String clean1 = barcode1.trim();
        String clean2 = barcode2.trim();
        if (clean1.equalsIgnoreCase(clean2)) {
            return true;
        }
        // Normalize leading zeros (e.g., UPC-A 12-digit vs EAN-13 13-digit)
        String norm1 = clean1.replaceAll("^0+", "");
        String norm2 = clean2.replaceAll("^0+", "");
        if (!norm1.isEmpty() && norm1.equalsIgnoreCase(norm2)) {
            return true;
        }
        return false;
    }

    public static JSONObject findLocalProductByBarcode(Context context, String barcode) {
        if (barcode == null || barcode.trim().isEmpty()) {
            return null;
        }
        try {
            JSONArray array = loadLocalDatabase(context);
            Log.d(TAG, "[DB] findLocalProductByBarcode: searching '" + barcode + "' in " + array.length() + " entries");
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                String storedBarcode = obj.optString("barcode", "");
                boolean match = isBarcodeMatch(storedBarcode, barcode);
                if (match) {
                    Log.d(TAG, "[DB] MATCH found: stored='" + storedBarcode + "' query='" + barcode + "' product='" + obj.optString("product_name") + "'");
                    return obj;
                }
            }
            Log.d(TAG, "[DB] NO MATCH: barcode '" + barcode + "' not in local DB");
        } catch (Exception e) {
            Log.e(TAG, "[DB] Error finding local product by barcode: " + e.getMessage(), e);
        }
        return null;
    }

    public static boolean checkDuplicateBarcode(Context context, String barcode) {
        return findLocalProductByBarcode(context, barcode) != null;
    }

    public static void addNewProduct(Context context, JSONObject newProduct) {
        try {
            JSONArray array = loadLocalDatabase(context);
            array.put(newProduct);

            File file = new File(context.getFilesDir(), FILENAME);
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(array.toString(2).getBytes(StandardCharsets.UTF_8));
            fos.close();
        } catch (Exception e) {
            Log.e(TAG, "Failed to save mutated database", e);
        }
    }
}
