package com.example.healthscanner.models;

public class SavedProduct {
    private String id;
    private String name;
    private String imageUrl;
    private int healthScore;
    private boolean isFavorite;

    public SavedProduct() {
        // Required empty constructor for Firebase
    }

    public SavedProduct(String id, String name, String imageUrl, int healthScore) {
        this.id = id;
        this.name = name;
        this.imageUrl = imageUrl;
        this.healthScore = healthScore;
        this.isFavorite = true;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public int getHealthScore() {
        return healthScore;
    }

    public void setHealthScore(int healthScore) {
        this.healthScore = healthScore;
    }

    public boolean isFavorite() {
        return isFavorite;
    }

    public void setFavorite(boolean favorite) {
        isFavorite = favorite;
    }
}