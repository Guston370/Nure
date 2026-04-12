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
            copyAssetsToInternalStorage(context);
        }

        try {
            FileInputStream fis = new FileInputStream(file);
            int size = fis.available();
            byte[] buffer = new byte[size];
            fis.read(buffer);
            fis.close();
            String jsonStr = new String(buffer, StandardCharsets.UTF_8);
            return new JSONArray(jsonStr);
        } catch (Exception e) {
            Log.e(TAG, "Error parsing internal JSON DB", e);
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
        } catch (Exception e) {
            Log.e(TAG, "Error migrating assets DB to persistent storage", e);
        }
    }

    public static boolean checkDuplicateBarcode(Context context, String barcode) {
        try {
            JSONArray array = loadLocalDatabase(context);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                if (obj.getString("barcode").equals(barcode)) {
                    return true;
                }
            }
        } catch (Exception e) {}
        return false;
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
