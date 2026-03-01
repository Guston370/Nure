package com.example.healthscanner;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.FrameLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.google.android.material.button.MaterialButton;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import com.example.healthscanner.database.SyncManager;

/**
 * History Activity for displaying scan history
 * Shows previously scanned products and their nutritional information
 */
public class HistoryActivity extends BaseActivity {

    private static final String TAG = "HistoryActivity";
    private static final String PREFS_NAME = "HealthScannerPrefs";
    private static final String KEY_SCAN_HISTORY = "scan_history";

    private FrameLayout historyContent;
    private SyncManager syncManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_enhanced);

        // Simple authentication check - trust navigation from authenticated home page
        AuthManager authManager = AuthManager.getInstance(this);
        boolean fromNavigation = getIntent().getBooleanExtra("from_navigation", false);
        if (!fromNavigation && !authManager.isUserAuthenticated()) {
            // Only check auth for direct launches, not navigation
            authManager.navigateToLogin(this);
            return;
        }

        initializeViews();
        initializeBottomNavigation();
        loadScanHistory();

        // Initialize sync manager and auto-sync if needed
        syncManager = SyncManager.getInstance(this);
        syncManager.autoSyncIfNeeded(null);
    }

    private void initializeViews() {
        historyContent = findViewById(R.id.historyContentContainer);
    }

    private void loadScanHistory() {
        try {
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            String scanHistoryJson = prefs.getString("recent_scans", "[]");
            org.json.JSONArray scanArray = new org.json.JSONArray(scanHistoryJson);

            LinearLayout emptyState = findViewById(R.id.emptyState);
            LinearLayout loadingState = findViewById(R.id.loadingState);
            androidx.recyclerview.widget.RecyclerView historyRecyclerView = findViewById(R.id.historyRecyclerView);

            // Hide loading state
            if (loadingState != null) {
                loadingState.setVisibility(View.GONE);
            }

            if (scanArray.length() == 0) {
                // Show empty state
                if (emptyState != null) {
                    emptyState.setVisibility(View.VISIBLE);
                }
                if (historyRecyclerView != null) {
                    historyRecyclerView.setVisibility(View.GONE);
                }

                // Setup start scanning button
                View startScanningButton = findViewById(R.id.startScanningButton);
                if (startScanningButton != null) {
                    startScanningButton.setOnClickListener(v -> {
                        Intent intent = new Intent(this, VerticalScannerActivity.class);
                        startActivity(intent);
                    });
                }
            } else {
                // Show scan history
                if (emptyState != null) {
                    emptyState.setVisibility(View.GONE);
                }
                if (historyRecyclerView != null) {
                    historyRecyclerView.setVisibility(View.VISIBLE);
                    setupHistoryRecyclerView(scanArray);
                }
            }

            Log.d(TAG, "Loaded " + scanArray.length() + " scans from history");

        } catch (Exception e) {
            Log.e(TAG, "Error loading scan history: " + e.getMessage(), e);
            showEmptyState();
        }
    }

    private void setupHistoryRecyclerView(org.json.JSONArray scanArray) {
        try {
            androidx.recyclerview.widget.RecyclerView historyRecyclerView = findViewById(R.id.historyRecyclerView);
            if (historyRecyclerView == null)
                return;

            // Create list of scan items (limit to last 10)
            java.util.List<ScanHistoryItem> scanItems = new java.util.ArrayList<>();
            int itemCount = Math.min(scanArray.length(), 10);

            for (int i = 0; i < itemCount; i++) {
                org.json.JSONObject scan = scanArray.getJSONObject(i);

                ScanHistoryItem item = new ScanHistoryItem();
                item.productName = scan.optString("productName", scan.optString("name", "Unknown Product"));
                item.brand = scan.optString("brand", "Unknown Brand");
                item.barcode = scan.optString("barcode", "");
                item.calories = scan.optInt("calories", 0);
                item.healthScore = scan.optDouble("healthScore", 0.0);
                item.timestamp = scan.optLong("timestamp", System.currentTimeMillis());

                scanItems.add(item);
            }

            // Setup RecyclerView
            historyRecyclerView.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
            ScanHistoryAdapter adapter = new ScanHistoryAdapter(scanItems, this::onHistoryItemClick);
            historyRecyclerView.setAdapter(adapter);

            Log.d(TAG, "History RecyclerView setup with " + scanItems.size() + " items");

        } catch (Exception e) {
            Log.e(TAG, "Error setting up history RecyclerView: " + e.getMessage(), e);
        }
    }

    private void onHistoryItemClick(ScanHistoryItem item) {
        // Navigate to product details with the scanned product data
        Intent intent = new Intent(this, ProductDetailsEnhancedActivity.class);
        intent.putExtra("barcode", item.barcode);
        intent.putExtra("from_history", true);
        startActivity(intent);
    }

    private void showEmptyState() {
        LinearLayout emptyState = findViewById(R.id.emptyState);
        androidx.recyclerview.widget.RecyclerView historyRecyclerView = findViewById(R.id.historyRecyclerView);

        if (emptyState != null) {
            emptyState.setVisibility(View.VISIBLE);
        }
        if (historyRecyclerView != null) {
            historyRecyclerView.setVisibility(View.GONE);
        }
    }

    // Data class for scan history items
    private static class ScanHistoryItem {
        String productName;
        String brand;
        String barcode;
        int calories;
        double healthScore;
        long timestamp;
    }

    // Adapter for scan history RecyclerView
    private static class ScanHistoryAdapter
            extends androidx.recyclerview.widget.RecyclerView.Adapter<ScanHistoryAdapter.ViewHolder> {
        private final java.util.List<ScanHistoryItem> items;
        private final OnItemClickListener listener;

        interface OnItemClickListener {
            void onItemClick(ScanHistoryItem item);
        }

        ScanHistoryAdapter(java.util.List<ScanHistoryItem> items, OnItemClickListener listener) {
            this.items = items;
            this.listener = listener;
        }

        @Override
        public ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            android.view.View view = android.view.LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_history_enhanced, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            ScanHistoryItem item = items.get(position);

            // Set product details
            holder.productName.setText(item.productName);
            holder.brandName.setText(item.brand);

            // Format and set scan time
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM dd, HH:mm",
                    java.util.Locale.getDefault());
            String timeStr = sdf.format(new java.util.Date(item.timestamp));
            holder.scanTime.setText(timeStr);

            // Set health score and color
            if (item.healthScore > 0) {
                holder.healthScore.setText(String.format("%.1f", item.healthScore));

                // Set health score card color based on score
                int backgroundColor;
                if (item.healthScore >= 80) {
                    backgroundColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.health_excellent);
                } else if (item.healthScore >= 60) {
                    backgroundColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.health_good);
                } else if (item.healthScore >= 40) {
                    backgroundColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.health_moderate);
                } else {
                    backgroundColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.health_poor);
                }
                holder.healthScoreCard.setCardBackgroundColor(backgroundColor);
            } else {
                holder.healthScore.setText("--");
            }

            // Set nutrition info
            if (item.calories > 0) {
                holder.caloriesText.setText(item.calories + " cal");
            } else {
                holder.caloriesText.setText("-- cal");
            }

            // Set protein and sugar (you'll need to add these to ScanHistoryItem)
            holder.proteinText.setText("-- protein");
            holder.sugarText.setText("-- sugar");

            // Set health insight based on score
            String insight = getHealthInsight(item.healthScore);
            holder.healthInsight.setText(insight);

            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(item);
                }
            });
        }

        private String getHealthInsight(double healthScore) {
            if (healthScore >= 80) {
                return "Excellent choice! Great nutritional value. 🌟";
            } else if (healthScore >= 60) {
                return "Good choice! Solid nutritional benefits. ✅";
            } else if (healthScore >= 40) {
                return "Moderate choice. Consider portion size. ⚖️";
            } else if (healthScore > 0) {
                return "Consider healthier alternatives. ⚠️";
            } else {
                return "Nutritional analysis pending...";
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class ViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
            TextView productName;
            TextView brandName;
            TextView scanTime;
            TextView healthScore;
            TextView caloriesText;
            TextView proteinText;
            TextView sugarText;
            TextView healthInsight;
            com.google.android.material.card.MaterialCardView healthScoreCard;

            ViewHolder(android.view.View itemView) {
                super(itemView);
                productName = itemView.findViewById(R.id.productName);
                brandName = itemView.findViewById(R.id.brandName);
                scanTime = itemView.findViewById(R.id.scanTime);
                healthScore = itemView.findViewById(R.id.healthScore);
                caloriesText = itemView.findViewById(R.id.caloriesText);
                proteinText = itemView.findViewById(R.id.proteinText);
                sugarText = itemView.findViewById(R.id.sugarText);
                healthInsight = itemView.findViewById(R.id.healthInsight);
                healthScoreCard = itemView.findViewById(R.id.healthScoreCard);
            }
        }
    }

    @Override
    protected int getCurrentNavigationItemId() {
        return R.id.nav_history;
    }

    /**
     * Add a scan to history (static method for easy access from other activities)
     * 
     * @param context      Application context
     * @param productName  Product name
     * @param brand        Product brand
     * @param barcode      Product barcode
     * @param category     Product category
     * @param healthRating Health rating
     * @param calories     Calories per 100g
     * @param sugar        Sugar content
     * @param protein      Protein content
     * @param ingredients  Ingredients list
     */
    public static void addScanToHistory(Context context, String productName, String brand,
            String barcode, String category, String healthRating,
            int calories, int sugar, int protein, String ingredients) {
        try {
            // For now, just log the scan
            Log.d("HistoryActivity", "Scan added to history: " + productName + " (" + brand + ")");
            Log.d("HistoryActivity", "Nutrition: " + calories + " cal, " + sugar + "g sugar, " + protein + "g protein");

            // TODO: Implement actual history storage using SharedPreferences or database
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            // Add implementation for storing scan history

        } catch (Exception e) {
            Log.e("HistoryActivity", "Error adding scan to history: " + e.getMessage());
        }
    }
}