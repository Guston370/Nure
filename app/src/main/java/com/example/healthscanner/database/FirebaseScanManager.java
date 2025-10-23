package com.example.healthscanner.database;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.healthscanner.models.Scan;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Firebase Scan Manager for handling all scan-related Firebase operations
 * Primary data storage - no local storage used
 */
public class FirebaseScanManager {
    
    private static final String TAG = "FirebaseScanManager";
    private static final String SCANS_COLLECTION = "scans";
    
    private static FirebaseScanManager instance;
    private FirebaseFirestore db;
    
    private FirebaseScanManager() {
        db = FirebaseFirestore.getInstance();
    }
    
    public static synchronized FirebaseScanManager getInstance() {
        if (instance == null) {
            instance = new FirebaseScanManager();
        }
        return instance;
    }
    
    /**
     * Save a new scan to Firebase
     */
    public void saveScan(Scan scan, OperationCallback callback) {
        if (scan == null || scan.getUserId() == null) {
            callback.onFailure("Invalid scan data");
            return;
        }
        
        Log.d(TAG, "🔥 Saving scan to Firebase: " + scan.getProductName());
        
        DocumentReference scanRef = db.collection(SCANS_COLLECTION).document(scan.getScanId());
        
        scanRef.set(scan.toMap())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "✅ Scan saved successfully: " + scan.getProductName());
                        callback.onSuccess();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "❌ Failed to save scan: " + e.getMessage(), e);
                        callback.onFailure("Failed to save scan: " + e.getMessage());
                    }
                });
    }
    
    /**
     * Get all scans for a user
     */
    public void getUserScans(String userId, ScanListCallback callback) {
        if (userId == null || userId.isEmpty()) {
            callback.onFailure("No user ID provided");
            return;
        }
        
        Log.d(TAG, "📊 Fetching all scans for user: " + userId);
        
        db.collection(SCANS_COLLECTION)
                .whereEqualTo("userId", userId)
                .orderBy("scanDate", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        List<Scan> scans = new ArrayList<>();
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            try {
                                Scan scan = documentToScan(document);
                                if (scan != null) {
                                    scans.add(scan);
                                }
                            } catch (Exception e) {
                                Log.w(TAG, "Error parsing scan document: " + e.getMessage());
                            }
                        }
                        Log.d(TAG, "✅ Retrieved " + scans.size() + " scans for user");
                        callback.onSuccess(scans);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "❌ Failed to fetch user scans: " + e.getMessage(), e);
                        callback.onFailure("Failed to fetch scans: " + e.getMessage());
                    }
                });
    }
    
    /**
     * Get scans for a specific time period
     */
    public void getScansForPeriod(String userId, Date startDate, Date endDate, ScanListCallback callback) {
        if (userId == null || userId.isEmpty()) {
            callback.onFailure("No user ID provided");
            return;
        }
        
        Log.d(TAG, "📅 Fetching scans for period: " + startDate + " to " + endDate);
        
        db.collection(SCANS_COLLECTION)
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("scanDate", startDate)
                .whereLessThanOrEqualTo("scanDate", endDate)
                .orderBy("scanDate", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        List<Scan> scans = new ArrayList<>();
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            try {
                                Scan scan = documentToScan(document);
                                if (scan != null) {
                                    scans.add(scan);
                                }
                            } catch (Exception e) {
                                Log.w(TAG, "Error parsing scan document: " + e.getMessage());
                            }
                        }
                        Log.d(TAG, "✅ Retrieved " + scans.size() + " scans for period");
                        callback.onSuccess(scans);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "❌ Failed to fetch period scans: " + e.getMessage(), e);
                        callback.onFailure("Failed to fetch period scans: " + e.getMessage());
                    }
                });
    }
    
    /**
     * Get weekly scans (last 7 days)
     */
    public void getWeeklyScans(String userId, ScanListCallback callback) {
        Calendar calendar = Calendar.getInstance();
        Date endDate = calendar.getTime();
        calendar.add(Calendar.DAY_OF_YEAR, -7);
        Date startDate = calendar.getTime();
        
        getScansForPeriod(userId, startDate, endDate, callback);
    }
    
    /**
     * Get monthly scans (last 30 days)
     */
    public void getMonthlyScans(String userId, ScanListCallback callback) {
        Calendar calendar = Calendar.getInstance();
        Date endDate = calendar.getTime();
        calendar.add(Calendar.DAY_OF_YEAR, -30);
        Date startDate = calendar.getTime();
        
        getScansForPeriod(userId, startDate, endDate, callback);
    }
    
    /**
     * Get scan statistics for analytics
     */
    public void getScanStatistics(String userId, StatisticsCallback callback) {
        getUserScans(userId, new ScanListCallback() {
            @Override
            public void onSuccess(List<Scan> scans) {
                ScanStatistics stats = calculateStatistics(scans);
                callback.onSuccess(stats);
            }
            
            @Override
            public void onFailure(String error) {
                callback.onFailure(error);
            }
        });
    }
    
    /**
     * Calculate comprehensive statistics from scans
     */
    private ScanStatistics calculateStatistics(List<Scan> scans) {
        ScanStatistics stats = new ScanStatistics();
        
        if (scans.isEmpty()) {
            return stats; // Return empty stats
        }
        
        // Basic counts
        stats.totalScans = scans.size();
        
        // Category breakdown
        Map<String, Integer> categoryCount = new HashMap<>();
        Map<String, Integer> subCategoryCount = new HashMap<>();
        
        // Health and nutrition totals
        double totalHealthScore = 0;
        int totalCalories = 0;
        int validHealthScores = 0;
        int validCalories = 0;
        
        // Time analysis
        Date firstScan = null;
        Date lastScan = null;
        
        // Most scanned products
        Map<String, Integer> productCount = new HashMap<>();
        
        for (Scan scan : scans) {
            // Category analysis
            String category = scan.getCategory();
            if (category != null && !category.isEmpty()) {
                categoryCount.put(category, categoryCount.getOrDefault(category, 0) + 1);
            }
            
            String subCategory = scan.getSubCategory();
            if (subCategory != null && !subCategory.isEmpty()) {
                subCategoryCount.put(subCategory, subCategoryCount.getOrDefault(subCategory, 0) + 1);
            }
            
            // Health score analysis
            if (scan.getHealthScore() > 0) {
                totalHealthScore += scan.getHealthScore();
                validHealthScores++;
            }
            
            // Calorie analysis
            if (scan.getCalories() > 0) {
                totalCalories += scan.getCalories();
                validCalories++;
            }
            
            // Time analysis
            Date scanDate = scan.getScanDate();
            if (scanDate != null) {
                if (firstScan == null || scanDate.before(firstScan)) {
                    firstScan = scanDate;
                }
                if (lastScan == null || scanDate.after(lastScan)) {
                    lastScan = scanDate;
                }
            }
            
            // Product frequency
            String productName = scan.getProductName();
            if (productName != null && !productName.isEmpty()) {
                productCount.put(productName, productCount.getOrDefault(productName, 0) + 1);
            }
        }
        
        // Calculate averages
        stats.averageHealthScore = validHealthScores > 0 ? totalHealthScore / validHealthScores : 0;
        stats.averageCalories = validCalories > 0 ? (double) totalCalories / validCalories : 0;
        
        // Set category data
        stats.categoryBreakdown = categoryCount;
        stats.subCategoryBreakdown = subCategoryCount;
        
        // Calculate time between scans
        if (firstScan != null && lastScan != null && stats.totalScans > 1) {
            long timeDiff = lastScan.getTime() - firstScan.getTime();
            stats.averageTimeBetweenScans = timeDiff / (stats.totalScans - 1);
        }
        
        // Most scanned products (top 5)
        stats.mostScannedProducts = productCount.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .collect(HashMap::new, (m, e) -> m.put(e.getKey(), e.getValue()), HashMap::putAll);
        
        return stats;
    }
    
    /**
     * Convert Firestore document to Scan object
     */
    private Scan documentToScan(QueryDocumentSnapshot document) {
        try {
            Scan scan = new Scan();
            scan.setScanId(document.getString("scanId"));
            scan.setUserId(document.getString("userId"));
            scan.setProductName(document.getString("productName"));
            scan.setBarcode(document.getString("barcode"));
            scan.setCategory(document.getString("category"));
            scan.setSubCategory(document.getString("subCategory"));
            scan.setScanDate(document.getDate("scanDate"));
            scan.setHealthScore(document.getDouble("healthScore") != null ? document.getDouble("healthScore") : 0);
            scan.setCalories(document.getLong("calories") != null ? document.getLong("calories").intValue() : 0);
            scan.setBrand(document.getString("brand"));
            scan.setImageUrl(document.getString("imageUrl"));
            scan.setFavorite(document.getBoolean("isFavorite") != null ? document.getBoolean("isFavorite") : false);
            
            // Nutritional information
            scan.setProtein(document.getDouble("protein") != null ? document.getDouble("protein") : 0);
            scan.setCarbs(document.getDouble("carbs") != null ? document.getDouble("carbs") : 0);
            scan.setFat(document.getDouble("fat") != null ? document.getDouble("fat") : 0);
            scan.setSugar(document.getDouble("sugar") != null ? document.getDouble("sugar") : 0);
            scan.setSodium(document.getDouble("sodium") != null ? document.getDouble("sodium") : 0);
            scan.setFiber(document.getDouble("fiber") != null ? document.getDouble("fiber") : 0);
            
            // Health analysis
            scan.setHealthGrade(document.getString("healthGrade"));
            
            // Scan metadata
            scan.setScanLocation(document.getString("scanLocation"));
            scan.setScanDuration(document.getLong("scanDuration") != null ? document.getLong("scanDuration") : 0);
            scan.setScanMethod(document.getString("scanMethod"));
            
            return scan;
        } catch (Exception e) {
            Log.e(TAG, "Error converting document to scan: " + e.getMessage(), e);
            return null;
        }
    }
    
    // Callback interfaces
    public interface OperationCallback {
        void onSuccess();
        void onFailure(String error);
    }
    
    public interface ScanListCallback {
        void onSuccess(List<Scan> scans);
        void onFailure(String error);
    }
    
    public interface StatisticsCallback {
        void onSuccess(ScanStatistics statistics);
        void onFailure(String error);
    }
    
    // Statistics data class
    public static class ScanStatistics {
        public int totalScans = 0;
        public double averageHealthScore = 0;
        public double averageCalories = 0;
        public long averageTimeBetweenScans = 0; // in milliseconds
        public Map<String, Integer> categoryBreakdown = new HashMap<>();
        public Map<String, Integer> subCategoryBreakdown = new HashMap<>();
        public Map<String, Integer> mostScannedProducts = new HashMap<>();
    }
}