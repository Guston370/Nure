package com.example.healthscanner.models;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Scan model for Firebase Firestore
 * Represents a product scan in the HealthScanner app
 */
public class Scan {
    private String scanId;
    private String userId;
    private String productName;
    private String barcode;
    private String category; // "food", "cosmetics", "beverages", etc.
    private String subCategory; // "snacks", "skincare", "makeup", etc.
    private Date scanDate;
    private double healthScore; // 0-10 scale
    private int calories;
    private String brand;
    private String imageUrl;
    private boolean isFavorite;
    
    // Nutritional information
    private double protein;
    private double carbs;
    private double fat;
    private double sugar;
    private double sodium;
    private double fiber;
    
    // Health analysis
    private String healthGrade; // A, B, C, D, E
    private String[] healthConcerns; // "high_sugar", "high_sodium", etc.
    private String[] positiveAspects; // "high_fiber", "low_fat", etc.
    
    // Scan metadata
    private String scanLocation; // GPS coordinates or location name
    private long scanDuration; // Time taken to scan in milliseconds
    private String scanMethod; // "camera", "manual_entry", "gallery"

    // Default constructor required for Firestore
    public Scan() {}

    // Constructor for new scan
    public Scan(String userId, String productName, String barcode, String category) {
        this.scanId = generateScanId();
        this.userId = userId;
        this.productName = productName;
        this.barcode = barcode;
        this.category = category;
        this.scanDate = new Date();
        this.isFavorite = false;
    }

    // Generate unique scan ID
    private String generateScanId() {
        return "scan_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);
    }

    // Convert to Map for Firestore
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("scanId", scanId);
        map.put("userId", userId);
        map.put("productName", productName);
        map.put("barcode", barcode);
        map.put("category", category);
        map.put("subCategory", subCategory);
        map.put("scanDate", scanDate);
        map.put("healthScore", healthScore);
        map.put("calories", calories);
        map.put("brand", brand);
        map.put("imageUrl", imageUrl);
        map.put("isFavorite", isFavorite);
        
        // Nutritional information
        map.put("protein", protein);
        map.put("carbs", carbs);
        map.put("fat", fat);
        map.put("sugar", sugar);
        map.put("sodium", sodium);
        map.put("fiber", fiber);
        
        // Health analysis
        map.put("healthGrade", healthGrade);
        map.put("healthConcerns", healthConcerns);
        map.put("positiveAspects", positiveAspects);
        
        // Scan metadata
        map.put("scanLocation", scanLocation);
        map.put("scanDuration", scanDuration);
        map.put("scanMethod", scanMethod);
        
        return map;
    }

    // Getters and Setters
    public String getScanId() { return scanId; }
    public void setScanId(String scanId) { this.scanId = scanId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getSubCategory() { return subCategory; }
    public void setSubCategory(String subCategory) { this.subCategory = subCategory; }

    public Date getScanDate() { return scanDate; }
    public void setScanDate(Date scanDate) { this.scanDate = scanDate; }

    public double getHealthScore() { return healthScore; }
    public void setHealthScore(double healthScore) { this.healthScore = healthScore; }

    public int getCalories() { return calories; }
    public void setCalories(int calories) { this.calories = calories; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public boolean isFavorite() { return isFavorite; }
    public void setFavorite(boolean favorite) { isFavorite = favorite; }

    public double getProtein() { return protein; }
    public void setProtein(double protein) { this.protein = protein; }

    public double getCarbs() { return carbs; }
    public void setCarbs(double carbs) { this.carbs = carbs; }

    public double getFat() { return fat; }
    public void setFat(double fat) { this.fat = fat; }

    public double getSugar() { return sugar; }
    public void setSugar(double sugar) { this.sugar = sugar; }

    public double getSodium() { return sodium; }
    public void setSodium(double sodium) { this.sodium = sodium; }

    public double getFiber() { return fiber; }
    public void setFiber(double fiber) { this.fiber = fiber; }

    public String getHealthGrade() { return healthGrade; }
    public void setHealthGrade(String healthGrade) { this.healthGrade = healthGrade; }

    public String[] getHealthConcerns() { return healthConcerns; }
    public void setHealthConcerns(String[] healthConcerns) { this.healthConcerns = healthConcerns; }

    public String[] getPositiveAspects() { return positiveAspects; }
    public void setPositiveAspects(String[] positiveAspects) { this.positiveAspects = positiveAspects; }

    public String getScanLocation() { return scanLocation; }
    public void setScanLocation(String scanLocation) { this.scanLocation = scanLocation; }

    public long getScanDuration() { return scanDuration; }
    public void setScanDuration(long scanDuration) { this.scanDuration = scanDuration; }

    public String getScanMethod() { return scanMethod; }
    public void setScanMethod(String scanMethod) { this.scanMethod = scanMethod; }
}