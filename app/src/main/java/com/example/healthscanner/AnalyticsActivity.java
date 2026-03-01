package com.example.healthscanner;

import android.animation.ValueAnimator;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.healthscanner.database.FirebaseScanManager;
import com.example.healthscanner.models.Scan;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Enhanced Analytics Activity with Firebase data and advanced charts
 * Shows comprehensive statistics with animated charts and real data
 */
public class AnalyticsActivity extends BaseActivity {

    private static final String TAG = "AnalyticsActivity";

    // UI Elements
    private TextView analyticsTitle;
    private TextView analyticsSubtitle;
    private ImageView refreshIcon;

    // Statistics Cards
    private View totalScansCard;
    private View weeklyScansCard;
    private View monthlyScansCard;
    private View avgHealthScoreCard;
    private View avgCaloriesCard;
    private View avgTimeBetweenCard;

    // Data Elements
    private TextView totalScansNumber;
    private TextView weeklyScansNumber;
    private TextView monthlyScansNumber;
    private TextView avgHealthScoreNumber;
    private TextView avgCaloriesNumber;
    private TextView avgTimeBetweenNumber;
    private ProgressBar healthScoreProgress;
    private TextView personalInsightText;

    // Charts
    private PieChart categoryPieChart;
    private BarChart scanFrequencyChart;
    private LineChart trendsLineChart;

    // Firebase managers
    private AuthManager authManager;
    private FirebaseScanManager scanManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analytics_enhanced);

        // Initialize managers
        authManager = AuthManager.getInstance(this);
        scanManager = FirebaseScanManager.getInstance();

        // Simple authentication check - trust navigation from authenticated home page
        boolean fromNavigation = getIntent().getBooleanExtra("from_navigation", false);
        if (!fromNavigation && !authManager.isUserAuthenticated()) {
            // Only check auth for direct launches, not navigation
            authManager.navigateToLogin(this);
            return;
        }

        initializeViews();
        initializeBottomNavigation();
        setupCharts();
        setupEntranceAnimations();
        setupClickListeners();
        loadRealAnalyticsData();
    }

    private void initializeViews() {
        // Header elements
        analyticsTitle = findViewById(R.id.analyticsTitle);
        analyticsSubtitle = findViewById(R.id.analyticsSubtitle);
        refreshIcon = findViewById(R.id.refreshIcon);

        // Statistics Cards (using existing IDs for now)
        totalScansCard = findViewById(R.id.dailyCaloriesCard); // Temporary mapping
        weeklyScansCard = findViewById(R.id.healthScoreCard); // Temporary mapping
        monthlyScansCard = findViewById(R.id.categoriesCard); // Temporary mapping
        avgHealthScoreCard = findViewById(R.id.trendsCard); // Temporary mapping
        avgCaloriesCard = findViewById(R.id.insightsCard); // Temporary mapping
        // avgTimeBetweenCard = null; // Not in current layout

        // Data elements (using existing IDs for now)
        totalScansNumber = findViewById(R.id.dailyCaloriesNumber); // Temporary mapping
        // weeklyScansNumber = null; // Not in current layout
        // monthlyScansNumber = null; // Not in current layout
        avgHealthScoreNumber = findViewById(R.id.healthScoreNumber); // Temporary mapping
        // avgCaloriesNumber = null; // Not in current layout
        // avgTimeBetweenNumber = null; // Not in current layout
        healthScoreProgress = findViewById(R.id.healthScoreProgress);
        personalInsightText = findViewById(R.id.personalInsightText);

        // Charts (not in current layout - will be null)
        // categoryPieChart = null;
        // scanFrequencyChart = null;
        // trendsLineChart = null;
    }

    @Override
    protected int getCurrentNavigationItemId() {
        return R.id.nav_stats;
    }

    private void setupEntranceAnimations() {
        // Header title animation
        if (analyticsTitle != null) {
            analyticsTitle.postDelayed(() -> {
                analyticsTitle.setAlpha(1f);
                analyticsTitle.setTranslationY(0f);
                analyticsTitle.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in_slide_down));
            }, 300);
        }

        // Staggered card animations
        animateCardsSequentially();
    }

    private void animateCardsSequentially() {
        View[] cards = { totalScansCard, weeklyScansCard, monthlyScansCard, avgHealthScoreCard, avgCaloriesCard,
                avgTimeBetweenCard };
        int[] delays = { 400, 500, 600, 700, 800 };

        for (int i = 0; i < cards.length; i++) {
            if (cards[i] != null) {
                final View card = cards[i];
                final boolean isInsights = (i == cards.length - 1);

                card.postDelayed(() -> {
                    if (isInsights) {
                        // Insights card gets fade animation
                        card.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));
                    } else {
                        // Other cards get slide up animation
                        card.startAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_up));
                    }
                }, delays[i]);
            }
        }
    }

    private void setupClickListeners() {
        // Refresh icon click
        if (refreshIcon != null) {
            refreshIcon.setOnClickListener(v -> {
                // Rotate animation for refresh
                v.animate()
                        .rotation(v.getRotation() + 360f)
                        .setDuration(500)
                        .start();

                // Scale bounce animation
                v.startAnimation(AnimationUtils.loadAnimation(this, R.anim.scale_bounce));

                // Refresh data
                refreshAnalyticsData();
            });
        }

        // Card click listeners for interactive feedback
        setupCardClickListeners();
    }

    private void setupCardClickListeners() {
        // Total scans card click
        if (totalScansCard != null) {
            totalScansCard.setOnClickListener(v -> {
                v.startAnimation(AnimationUtils.loadAnimation(this, R.anim.scale_bounce));
                // Handle total scans detail view
            });
        }

        // Weekly scans card click
        if (weeklyScansCard != null) {
            weeklyScansCard.setOnClickListener(v -> {
                v.startAnimation(AnimationUtils.loadAnimation(this, R.anim.scale_bounce));
                // Handle weekly scans detail view
            });
        }

        // Monthly scans card click
        if (monthlyScansCard != null) {
            monthlyScansCard.setOnClickListener(v -> {
                v.startAnimation(AnimationUtils.loadAnimation(this, R.anim.scale_bounce));
                // Handle monthly scans detail view
            });
        }

        // Average health score card click
        if (avgHealthScoreCard != null) {
            avgHealthScoreCard.setOnClickListener(v -> {
                v.startAnimation(AnimationUtils.loadAnimation(this, R.anim.scale_bounce));
                // Handle health score detail view
            });
        }

        // Average calories card click
        if (avgCaloriesCard != null) {
            avgCaloriesCard.setOnClickListener(v -> {
                v.startAnimation(AnimationUtils.loadAnimation(this, R.anim.scale_bounce));
                // Handle calories detail view
            });
        }
    }

    private void loadAnalyticsData() {
        // Load REAL user analytics data from SharedPreferences and Firebase
        loadRealUserAnalytics();
    }

    private void loadRealUserAnalytics() {
        try {
            android.content.SharedPreferences prefs = getSharedPreferences("HealthScannerPrefs", MODE_PRIVATE);

            // Get real user scan history
            String scanHistoryJson = prefs.getString("recent_scans", "[]");
            org.json.JSONArray scanArray = new org.json.JSONArray(scanHistoryJson);

            // Calculate real statistics
            int totalScans = scanArray.length();
            double totalCalories = 0;
            double totalHealthScore = 0;
            int validScans = 0;

            for (int i = 0; i < scanArray.length(); i++) {
                org.json.JSONObject scan = scanArray.getJSONObject(i);
                if (scan.has("calories")) {
                    totalCalories += scan.getDouble("calories");
                    validScans++;
                }
                if (scan.has("healthScore")) {
                    totalHealthScore += scan.getDouble("healthScore");
                }
            }

            // Calculate averages
            double avgCalories = validScans > 0 ? totalCalories / validScans : 0;
            double avgHealthScore = totalScans > 0 ? totalHealthScore / totalScans : 0;

            // Animate real data
            animateCaloriesCounter((int) avgCalories);
            animateHealthScore(avgHealthScore);

            // Load real insights
            loadRealPersonalInsights(totalScans, avgHealthScore, avgCalories);

            Log.d(TAG, "Real analytics loaded - Scans: " + totalScans + ", Avg Calories: " + avgCalories
                    + ", Avg Health Score: " + avgHealthScore);

        } catch (Exception e) {
            Log.e(TAG, "Error loading real analytics data", e);
            // Fallback to show empty state
            showEmptyAnalytics();
        }
    }

    private void animateCaloriesCounter(int targetCalories) {
        if (avgCaloriesNumber != null) {
            if (targetCalories == 0) {
                avgCaloriesNumber.setText("--");
                return;
            }

            ValueAnimator animator = ValueAnimator.ofInt(0, targetCalories);
            animator.setDuration(2000);
            animator.addUpdateListener(animation -> {
                int value = (int) animation.getAnimatedValue();
                avgCaloriesNumber.setText(String.format("%,d", value));
            });

            // Start animation after card appears
            new Handler().postDelayed(() -> animator.start(), 600);
        }
    }

    private void animateHealthScore(double targetScore) {
        if (avgHealthScoreNumber != null && healthScoreProgress != null) {
            if (targetScore == 0) {
                avgHealthScoreNumber.setText("--");
                healthScoreProgress.setProgress(0);
                return;
            }

            // Animate the score number
            ValueAnimator scoreAnimator = ValueAnimator.ofFloat(0f, (float) targetScore);
            scoreAnimator.setDuration(2000);
            scoreAnimator.addUpdateListener(animation -> {
                float value = (float) animation.getAnimatedValue();
                avgHealthScoreNumber.setText(String.format("%.1f", value));
            });

            // Animate the progress bar
            int progressTarget = (int) (targetScore * 10); // Convert to 0-100 scale
            ValueAnimator progressAnimator = ValueAnimator.ofInt(0, progressTarget);
            progressAnimator.setDuration(2000);
            progressAnimator.addUpdateListener(animation -> {
                int value = (int) animation.getAnimatedValue();
                healthScoreProgress.setProgress(value);
            });

            // Start animations after card appears
            new Handler().postDelayed(() -> {
                scoreAnimator.start();
                progressAnimator.start();
            }, 700);
        }
    }

    private void loadRealPersonalInsights(int totalScans, double avgHealthScore, double avgCalories) {
        if (personalInsightText == null)
            return;

        StringBuilder insights = new StringBuilder();

        if (totalScans == 0) {
            insights.append(
                    "🌟 Welcome to Health Scanner! Start scanning products to see your personalized health insights here. Your journey to healthier choices begins with your first scan!");
        } else if (totalScans < 5) {
            insights.append("🚀 Great start! You've scanned ").append(totalScans).append(" product")
                    .append(totalScans > 1 ? "s" : "")
                    .append(". Keep scanning to build your health profile and get more detailed insights!");
        } else {
            // Generate insights based on real data
            if (avgHealthScore >= 7.0) {
                insights.append("🌟 Excellent choices! Your average health score of ")
                        .append(String.format("%.1f", avgHealthScore))
                        .append(" shows you're making great nutritional decisions. ");
            } else if (avgHealthScore >= 5.0) {
                insights.append("👍 Good progress! Your average health score is ")
                        .append(String.format("%.1f", avgHealthScore))
                        .append(". Consider choosing more products with higher nutritional value. ");
            } else if (avgHealthScore > 0) {
                insights.append("💪 Room for improvement! Your average health score is ")
                        .append(String.format("%.1f", avgHealthScore))
                        .append(". Try scanning more fruits, vegetables, and whole grain products. ");
            }

            if (avgCalories > 400) {
                insights.append("Consider choosing lower-calorie options to maintain a balanced diet. ");
            } else if (avgCalories > 0 && avgCalories <= 200) {
                insights.append("Great job choosing lower-calorie products! ");
            }

            insights.append("You've scanned ").append(totalScans).append(" products total. Keep it up! 🎯");
        }

        personalInsightText.setText(insights.toString());
    }

    private void showEmptyAnalytics() {
        if (totalScansNumber != null)
            totalScansNumber.setText("--");
        if (weeklyScansNumber != null)
            weeklyScansNumber.setText("--");
        if (monthlyScansNumber != null)
            monthlyScansNumber.setText("--");
        if (avgHealthScoreNumber != null)
            avgHealthScoreNumber.setText("--");
        if (avgCaloriesNumber != null)
            avgCaloriesNumber.setText("--");
        if (avgTimeBetweenNumber != null)
            avgTimeBetweenNumber.setText("--");
        if (healthScoreProgress != null)
            healthScoreProgress.setProgress(0);
        if (personalInsightText != null) {
            personalInsightText
                    .setText("🌟 Start scanning products to see your personalized health analytics and insights!");
        }
    }

    private void refreshAnalyticsData() {
        // Refresh real analytics data
        new Handler().postDelayed(() -> {
            loadRealUserAnalytics();
            android.widget.Toast.makeText(this, "Analytics refreshed!", android.widget.Toast.LENGTH_SHORT).show();
        }, 1000);
    }

    /**
     * Show authentication dialog for navigation context
     */
    private void showAuthenticationDialog() {
        try {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Authentication Required")
                    .setMessage("Please sign in to access your analytics data.")
                    .setPositiveButton("Sign In", (dialog, which) -> {
                        authManager.navigateToLogin(this);
                    })
                    .setNegativeButton("Go Back", (dialog, which) -> {
                        finish();
                    })
                    .setCancelable(false)
                    .show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing authentication dialog", e);
            authManager.navigateToLogin(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data when returning to the activity
        loadRealAnalyticsData();
    }

    /**
     * Load real analytics data from Firebase
     */
    private void loadRealAnalyticsData() {
        String userId = authManager.getCurrentUserId();
        if (userId == null || userId.isEmpty()) {
            Log.w(TAG, "No user ID available for analytics");
            showEmptyAnalytics();
            return;
        }

        Log.d(TAG, "🔥 Loading real analytics data from Firebase for user: " + userId);

        // Show loading state
        showLoadingState();

        // Get comprehensive statistics from Firebase
        scanManager.getScanStatistics(userId, new FirebaseScanManager.StatisticsCallback() {
            @Override
            public void onSuccess(FirebaseScanManager.ScanStatistics statistics) {
                Log.d(TAG, "✅ Analytics data loaded successfully");
                runOnUiThread(() -> {
                    displayStatistics(statistics);
                    loadWeeklyAndMonthlyData(userId);
                    setupChartsWithData(statistics);
                });
            }

            @Override
            public void onFailure(String error) {
                Log.e(TAG, "❌ Failed to load analytics data: " + error);
                runOnUiThread(() -> {
                    showEmptyAnalytics();
                    android.widget.Toast.makeText(AnalyticsActivity.this,
                            "Unable to load analytics data. Please check your connection.",
                            android.widget.Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    /**
     * Load weekly and monthly scan counts
     */
    private void loadWeeklyAndMonthlyData(String userId) {
        // Get weekly scans
        scanManager.getWeeklyScans(userId, new FirebaseScanManager.ScanListCallback() {
            @Override
            public void onSuccess(List<Scan> scans) {
                runOnUiThread(() -> {
                    animateCounterValue(weeklyScansNumber, scans.size());
                });
            }

            @Override
            public void onFailure(String error) {
                Log.w(TAG, "Failed to load weekly scans: " + error);
            }
        });

        // Get monthly scans
        scanManager.getMonthlyScans(userId, new FirebaseScanManager.ScanListCallback() {
            @Override
            public void onSuccess(List<Scan> scans) {
                runOnUiThread(() -> {
                    animateCounterValue(monthlyScansNumber, scans.size());
                });
            }

            @Override
            public void onFailure(String error) {
                Log.w(TAG, "Failed to load monthly scans: " + error);
            }
        });
    }

    /**
     * Display statistics with animations
     */
    private void displayStatistics(FirebaseScanManager.ScanStatistics stats) {
        // Animate total scans
        animateCounterValue(totalScansNumber, stats.totalScans);

        // Animate health score
        if (stats.averageHealthScore > 0) {
            animateHealthScore(stats.averageHealthScore);
            animateCounterValue(avgHealthScoreNumber, (int) (stats.averageHealthScore * 10));
        } else {
            avgHealthScoreNumber.setText("--");
            if (healthScoreProgress != null)
                healthScoreProgress.setProgress(0);
        }

        // Animate average calories
        if (stats.averageCalories > 0) {
            animateCounterValue(avgCaloriesNumber, (int) stats.averageCalories);
        } else {
            avgCaloriesNumber.setText("--");
        }

        // Calculate and display average time between scans
        if (stats.averageTimeBetweenScans > 0) {
            long hours = stats.averageTimeBetweenScans / (1000 * 60 * 60);
            if (hours > 24) {
                long days = hours / 24;
                avgTimeBetweenNumber.setText(days + "d");
            } else {
                avgTimeBetweenNumber.setText(hours + "h");
            }
        } else {
            avgTimeBetweenNumber.setText("--");
        }

        // Generate insights
        generateRealInsights(stats);
    }

    /**
     * Setup charts with real data
     */
    private void setupChartsWithData(FirebaseScanManager.ScanStatistics stats) {
        // Setup category pie chart
        setupCategoryPieChart(stats.categoryBreakdown);

        // Setup scan frequency chart (placeholder for now)
        setupScanFrequencyChart();

        // Setup trends line chart (placeholder for now)
        setupTrendsLineChart();
    }

    /**
     * Setup category breakdown pie chart
     */
    private void setupCategoryPieChart(Map<String, Integer> categoryData) {
        if (categoryPieChart == null || categoryData.isEmpty()) {
            return;
        }

        List<PieEntry> entries = new ArrayList<>();
        int[] colors = {
                Color.parseColor("#4CAF50"), // Food - Green
                Color.parseColor("#FF9800"), // Cosmetics - Orange
                Color.parseColor("#2196F3"), // Beverages - Blue
                Color.parseColor("#9C27B0"), // Personal Care - Purple
                Color.parseColor("#F44336"), // Other - Red
        };

        for (Map.Entry<String, Integer> entry : categoryData.entrySet()) {
            entries.add(new PieEntry(entry.getValue(), entry.getKey()));
        }

        PieDataSet dataSet = new PieDataSet(entries, "Categories");
        dataSet.setColors(colors);
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.WHITE);

        PieData data = new PieData(dataSet);
        categoryPieChart.setData(data);
        categoryPieChart.getDescription().setEnabled(false);
        categoryPieChart.setDrawHoleEnabled(true);
        categoryPieChart.setHoleColor(Color.TRANSPARENT);
        categoryPieChart.setHoleRadius(40f);
        categoryPieChart.setTransparentCircleRadius(45f);
        categoryPieChart.animateY(1000);
        categoryPieChart.invalidate();
    }

    /**
     * Setup scan frequency bar chart
     */
    private void setupScanFrequencyChart() {
        if (scanFrequencyChart == null) {
            return;
        }

        // Placeholder data - will be enhanced with real daily scan data
        List<BarEntry> entries = new ArrayList<>();
        String[] days = { "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun" };

        for (int i = 0; i < 7; i++) {
            entries.add(new BarEntry(i, (float) (Math.random() * 10))); // Placeholder
        }

        BarDataSet dataSet = new BarDataSet(entries, "Scans per Day");
        dataSet.setColor(Color.parseColor("#4CAF50"));
        dataSet.setValueTextSize(10f);

        BarData data = new BarData(dataSet);
        scanFrequencyChart.setData(data);

        XAxis xAxis = scanFrequencyChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(days));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);

        scanFrequencyChart.getDescription().setEnabled(false);
        scanFrequencyChart.animateY(1000);
        scanFrequencyChart.invalidate();
    }

    /**
     * Setup trends line chart
     */
    private void setupTrendsLineChart() {
        if (trendsLineChart == null) {
            return;
        }

        // Placeholder data - will be enhanced with real trend data
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            entries.add(new Entry(i, (float) (Math.random() * 10)));
        }

        LineDataSet dataSet = new LineDataSet(entries, "Health Score Trend");
        dataSet.setColor(Color.parseColor("#2196F3"));
        dataSet.setCircleColor(Color.parseColor("#2196F3"));
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(3f);
        dataSet.setDrawCircleHole(false);
        dataSet.setValueTextSize(9f);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.parseColor("#2196F3"));
        dataSet.setFillAlpha(50);

        LineData data = new LineData(dataSet);
        trendsLineChart.setData(data);
        trendsLineChart.getDescription().setEnabled(false);
        trendsLineChart.animateX(1000);
        trendsLineChart.invalidate();
    }

    /**
     * Generate real insights based on statistics
     */
    private void generateRealInsights(FirebaseScanManager.ScanStatistics stats) {
        if (personalInsightText == null)
            return;

        StringBuilder insights = new StringBuilder();

        if (stats.totalScans == 0) {
            insights.append(
                    "🌟 Welcome to Health Scanner! Start scanning products to see your personalized health insights here. Your journey to healthier choices begins with your first scan!");
        } else if (stats.totalScans < 5) {
            insights.append("🚀 Great start! You've scanned ").append(stats.totalScans).append(" product")
                    .append(stats.totalScans > 1 ? "s" : "")
                    .append(". Keep scanning to build your health profile and get more detailed insights!");
        } else {
            // Generate insights based on real data
            if (stats.averageHealthScore >= 7.0) {
                insights.append("🌟 Excellent choices! Your average health score of ")
                        .append(String.format("%.1f", stats.averageHealthScore))
                        .append(" shows you're making great nutritional decisions. ");
            } else if (stats.averageHealthScore >= 5.0) {
                insights.append("👍 Good progress! Your average health score is ")
                        .append(String.format("%.1f", stats.averageHealthScore))
                        .append(". Consider choosing more products with higher nutritional value. ");
            } else if (stats.averageHealthScore > 0) {
                insights.append("💪 Room for improvement! Your average health score is ")
                        .append(String.format("%.1f", stats.averageHealthScore))
                        .append(". Try scanning more fruits, vegetables, and whole grain products. ");
            }

            if (stats.averageCalories > 400) {
                insights.append("Consider choosing lower-calorie options to maintain a balanced diet. ");
            } else if (stats.averageCalories > 0 && stats.averageCalories <= 200) {
                insights.append("Great job choosing lower-calorie products! ");
            }

            // Category insights
            if (!stats.categoryBreakdown.isEmpty()) {
                String topCategory = stats.categoryBreakdown.entrySet().stream()
                        .max(Map.Entry.comparingByValue())
                        .map(Map.Entry::getKey)
                        .orElse("products");
                insights.append("You scan ").append(topCategory).append(" most frequently. ");
            }

            insights.append("You've scanned ").append(stats.totalScans).append(" products total. Keep it up! 🎯");
        }

        personalInsightText.setText(insights.toString());
    }

    /**
     * Show loading state
     */
    private void showLoadingState() {
        if (totalScansNumber != null)
            totalScansNumber.setText("...");
        if (weeklyScansNumber != null)
            weeklyScansNumber.setText("...");
        if (monthlyScansNumber != null)
            monthlyScansNumber.setText("...");
        if (avgHealthScoreNumber != null)
            avgHealthScoreNumber.setText("...");
        if (avgCaloriesNumber != null)
            avgCaloriesNumber.setText("...");
        if (avgTimeBetweenNumber != null)
            avgTimeBetweenNumber.setText("...");
        if (personalInsightText != null) {
            personalInsightText.setText("📊 Loading your personalized analytics from Firebase...");
        }
    }

    /**
     * Animate counter values with count-up effect
     */
    private void animateCounterValue(TextView textView, int targetValue) {
        if (textView == null)
            return;

        if (targetValue == 0) {
            textView.setText("--");
            return;
        }

        ValueAnimator animator = ValueAnimator.ofInt(0, targetValue);
        animator.setDuration(1500);
        animator.addUpdateListener(animation -> {
            int value = (int) animation.getAnimatedValue();
            textView.setText(String.valueOf(value));
        });

        // Start animation after a delay for staggered effect
        new Handler().postDelayed(() -> animator.start(), 300);
    }

    /**
     * Setup charts (initialize chart views)
     */
    private void setupCharts() {
        // Initialize chart views - will be populated with data later
        if (categoryPieChart != null) {
            categoryPieChart.setNoDataText("Loading category data...");
        }
        if (scanFrequencyChart != null) {
            scanFrequencyChart.setNoDataText("Loading frequency data...");
        }
        if (trendsLineChart != null) {
            trendsLineChart.setNoDataText("Loading trend data...");
        }
    }
}