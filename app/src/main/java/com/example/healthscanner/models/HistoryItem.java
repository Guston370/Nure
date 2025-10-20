package com.example.healthscanner.models;

/**
 * Model class for history items
 * Represents a scanned product in the user's history
 */
public class HistoryItem {
    private String productName;
    private String brandName;
    private String calories;
    private String protein;
    private String sugar;
    private String healthEmoji;
    private String healthCategory; // "healthy", "moderate", "unhealthy"
    private String healthRating;
    private String scanDateTime;
    private String barcode;
    private String imageUrl;
    
    public HistoryItem() {
        // Default constructor for Firebase
    }
    
    public HistoryItem(String productName, String brandName, String calories, String protein, 
                      String sugar, String healthEmoji, String healthCategory, 
                      String healthRating, String scanDateTime) {
        this.productName = productName;
        this.brandName = brandName;
        this.calories = calories;
        this.protein = protein;
        this.sugar = sugar;
        this.healthEmoji = healthEmoji;
        this.healthCategory = healthCategory;
        this.healthRating = healthRating;
        this.scanDateTime = scanDateTime;
    }
    
    // Getters
    public String getProductName() { return productName; }
    public String getBrandName() { return brandName; }
    public String getCalories() { return calories; }
    public String getProtein() { return protein; }
    public String getSugar() { return sugar; }
    public String getHealthEmoji() { return healthEmoji; }
    public String getHealthCategory() { return healthCategory; }
    public String getHealthRating() { return healthRating; }
    public String getScanDateTime() { return scanDateTime; }
    public String getBarcode() { return barcode; }
    public String getImageUrl() { return imageUrl; }
    
    // Setters
    public void setProductName(String productName) { this.productName = productName; }
    public void setBrandName(String brandName) { this.brandName = brandName; }
    public void setCalories(String calories) { this.calories = calories; }
    public void setProtein(String protein) { this.protein = protein; }
    public void setSugar(String sugar) { this.sugar = sugar; }
    public void setHealthEmoji(String healthEmoji) { this.healthEmoji = healthEmoji; }
    public void setHealthCategory(String healthCategory) { this.healthCategory = healthCategory; }
    public void setHealthRating(String healthRating) { this.healthRating = healthRating; }
    public void setScanDateTime(String scanDateTime) { this.scanDateTime = scanDateTime; }
    public void setBarcode(String barcode) { this.barcode = barcode; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}