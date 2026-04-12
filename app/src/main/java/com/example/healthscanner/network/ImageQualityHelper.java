package com.example.healthscanner.network;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;

public class ImageQualityHelper {

    private static final String TAG = "ImageQualityHelper";
    
    // Thresholds (tune these mathematically during MLOps feedback)
    private static final double MIN_VARIANCE_THRESHOLD = 50.0; // Below this = Blurry
    private static final double MIN_BRIGHTNESS_THRESHOLD = 30.0; // Below this = Dark
    private static final int MIN_DIMENSION = 200; // Small bounds
    
    public enum QualityResult {
        VALID,
        TOO_SMALL,
        TOO_DARK,
        TOO_BLURRY
    }

    public static QualityResult assessImageQuality(Bitmap bitmap) {
        if (bitmap.getWidth() < MIN_DIMENSION || bitmap.getHeight() < MIN_DIMENSION) {
            return QualityResult.TOO_SMALL;
        }
        
        // Scale down matrix drastically to prevent massive CPU allocation blocks on Laplacian 2D mapping
        int MAX_ANALYSE_SIZE = 256;
        Bitmap scaled;
        if (bitmap.getWidth() > MAX_ANALYSE_SIZE || bitmap.getHeight() > MAX_ANALYSE_SIZE) {
            float ratio = Math.min(
                (float) MAX_ANALYSE_SIZE / bitmap.getWidth(),
                (float) MAX_ANALYSE_SIZE / bitmap.getHeight()
            );
            scaled = Bitmap.createScaledBitmap(
                bitmap,
                Math.round(bitmap.getWidth() * ratio),
                Math.round(bitmap.getHeight() * ratio),
                false
            );
        } else {
            scaled = bitmap;
        }

        int width = scaled.getWidth();
        int height = scaled.getHeight();
        int[] pixels = new int[width * height];
        scaled.getPixels(pixels, 0, width, 0, 0, width, height);

        // Subroutine 1: Convert to intensity array and check Dark/Brightness
        double totalBrightness = 0;
        int[][] grayMatrix = new int[height][width];
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = pixels[y * width + x];
                int r = Color.red(pixel);
                int g = Color.green(pixel);
                int b = Color.blue(pixel);
                // standard luminosity logic
                int intensity = (int) (0.299 * r + 0.587 * g + 0.114 * b);
                grayMatrix[y][x] = intensity;
                totalBrightness += intensity;
            }
        }
        
        double avgBrightness = totalBrightness / (width * height);
        if (avgBrightness < MIN_BRIGHTNESS_THRESHOLD) {
            return QualityResult.TOO_DARK;
        }

        // Subroutine 2: Laplacian Kernel Filter to find Variance (Blur ratio)
        // Standard kernel:
        // [ 0  1  0]
        // [ 1 -4  1]
        // [ 0  1  0]
        double laplacianSum = 0;
        long pixelCount = 0;
        
        // Loop inner matrix avoiding bounds
        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                int l = grayMatrix[y-1][x] 
                      + grayMatrix[y+1][x]
                      + grayMatrix[y][x-1]
                      + grayMatrix[y][x+1]
                      - 4 * grayMatrix[y][x];
                
                laplacianSum += l;
                pixelCount++;
            }
        }
        
        double mean = laplacianSum / pixelCount;
        double varianceSquaredSum = 0;
        
        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                int l = grayMatrix[y-1][x] 
                      + grayMatrix[y+1][x]
                      + grayMatrix[y][x-1]
                      + grayMatrix[y][x+1]
                      - 4 * grayMatrix[y][x];
                
                varianceSquaredSum += Math.pow((l - mean), 2);
            }
        }
        
        double varianceOut = varianceSquaredSum / pixelCount;
        Log.d(TAG, "Laplacian Variance Checked: " + varianceOut);

        if (varianceOut < MIN_VARIANCE_THRESHOLD) {
            return QualityResult.TOO_BLURRY;
        }

        return QualityResult.VALID;
    }
}
