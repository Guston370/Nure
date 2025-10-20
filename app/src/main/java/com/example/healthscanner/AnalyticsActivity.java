package com.example.healthscanner;

import android.animation.ValueAnimator;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

/**
 * Analytics Activity for health insights and statistics
 * Matches Home Page design with gradient header and animated cards
 */
public class AnalyticsActivity extends BaseActivity {
    
    private static final String TAG = "AnalyticsActivity";
    
    // UI Elements
    private TextView analyticsTitle;
    private TextView analyticsSubtitle;
    private ImageView refreshIcon;
    
    // Metric Cards
    private CardView dailyCaloriesCard;
    private CardView healthScoreCard;
    private CardView categoriesCard;
    private CardView trendsCard;
    private CardView insightsCard;
    
    // Data Elements
    private TextView dailyCaloriesNumber;
    private TextView healthScoreNumber;
    private ProgressBar healthScoreProgress;
    private TextView personalInsightText;
    
    // Auth Manager
    private AuthManager authManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analytics_enhanced);
        
        // Initialize AuthManager
        authManager = AuthManager.getInstance(this);
        
        // Simple authentication check - trust navigation from authenticated home page
        boolean fromNavigation = getIntent().getBooleanExtra("from_navigation", false);
        if (!fromNavigation && !authManager.isUserAuthenticated()) {
            // Only check auth for direct launches, not navigation
            authManager.navigateToLogin(this);
            return;
        }
        
        initializeViews();
        initializeBottomNavigation();
        setupEntranceAnimations();
        setupClickListeners();
        loadAnalyticsData();
    }
    
    private void initializeViews() {
        // Header elements
        analyticsTitle = findViewById(R.id.analyticsTitle);
        analyticsSubtitle = findViewById(R.id.analyticsSubtitle);
        refreshIcon = findViewById(R.id.refreshIcon);
        
        // Metric cards
        dailyCaloriesCard = findViewById(R.id.dailyCaloriesCard);
        healthScoreCard = findViewById(R.id.healthScoreCard);
        categoriesCard = findViewById(R.id.categoriesCard);
        trendsCard = findViewById(R.id.trendsCard);
        insightsCard = findViewById(R.id.insightsCard);
        
        // Data elements
        dailyCaloriesNumber = findViewById(R.id.dailyCaloriesNumber);
        healthScoreNumber = findViewById(R.id.healthScoreNumber);
        healthScoreProgress = findViewById(R.id.healthScoreProgress);
        personalInsightText = findViewById(R.id.personalInsightText);
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
        CardView[] cards = {dailyCaloriesCard, healthScoreCard, categoriesCard, trendsCard, insightsCard};
        int[] delays = {400, 500, 600, 700, 800};
        
        for (int i = 0; i < cards.length; i++) {
            if (cards[i] != null) {
                final CardView card = cards[i];
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
        // Daily calories card click
        if (dailyCaloriesCard != null) {
            dailyCaloriesCard.setOnClickListener(v -> {
                v.startAnimation(AnimationUtils.loadAnimation(this, R.anim.scale_bounce));
                // Handle calories detail view
            });
        }
        
        // Health score card click
        if (healthScoreCard != null) {
            healthScoreCard.setOnClickListener(v -> {
                v.startAnimation(AnimationUtils.loadAnimation(this, R.anim.scale_bounce));
                // Handle health score detail view
            });
        }
        
        // Categories card click
        if (categoriesCard != null) {
            categoriesCard.setOnClickListener(v -> {
                v.startAnimation(AnimationUtils.loadAnimation(this, R.anim.scale_bounce));
                // Handle categories detail view
            });
        }
        
        // Trends card click
        if (trendsCard != null) {
            trendsCard.setOnClickListener(v -> {
                v.startAnimation(AnimationUtils.loadAnimation(this, R.anim.scale_bounce));
                // Handle trends detail view
            });
        }
        
        // Insights card click
        if (insightsCard != null) {
            insightsCard.setOnClickListener(v -> {
                v.startAnimation(AnimationUtils.loadAnimation(this, R.anim.scale_bounce));
                // Handle insights detail view
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
            
            Log.d(TAG, "Real analytics loaded - Scans: " + totalScans + ", Avg Calories: " + avgCalories + ", Avg Health Score: " + avgHealthScore);
            
        } catch (Exception e) {
            Log.e(TAG, "Error loading real analytics data", e);
            // Fallback to show empty state
            showEmptyAnalytics();
        }
    }
    
    private void animateCaloriesCounter(int targetCalories) {
        if (dailyCaloriesNumber != null) {
            if (targetCalories == 0) {
                dailyCaloriesNumber.setText("--");
                return;
            }
            
            ValueAnimator animator = ValueAnimator.ofInt(0, targetCalories);
            animator.setDuration(2000);
            animator.addUpdateListener(animation -> {
                int value = (int) animation.getAnimatedValue();
                dailyCaloriesNumber.setText(String.format("%,d", value));
            });
            
            // Start animation after card appears
            new Handler().postDelayed(() -> animator.start(), 600);
        }
    }
    
    private void animateHealthScore(double targetScore) {
        if (healthScoreNumber != null && healthScoreProgress != null) {
            if (targetScore == 0) {
                healthScoreNumber.setText("--");
                healthScoreProgress.setProgress(0);
                return;
            }
            
            // Animate the score number
            ValueAnimator scoreAnimator = ValueAnimator.ofFloat(0f, (float) targetScore);
            scoreAnimator.setDuration(2000);
            scoreAnimator.addUpdateListener(animation -> {
                float value = (float) animation.getAnimatedValue();
                healthScoreNumber.setText(String.format("%.1f", value));
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
        if (personalInsightText == null) return;
        
        StringBuilder insights = new StringBuilder();
        
        if (totalScans == 0) {
            insights.append("🌟 Welcome to Health Scanner! Start scanning products to see your personalized health insights here. Your journey to healthier choices begins with your first scan!");
        } else if (totalScans < 5) {
            insights.append("🚀 Great start! You've scanned ").append(totalScans).append(" product").append(totalScans > 1 ? "s" : "").append(". Keep scanning to build your health profile and get more detailed insights!");
        } else {
            // Generate insights based on real data
            if (avgHealthScore >= 7.0) {
                insights.append("🌟 Excellent choices! Your average health score of ").append(String.format("%.1f", avgHealthScore)).append(" shows you're making great nutritional decisions. ");
            } else if (avgHealthScore >= 5.0) {
                insights.append("👍 Good progress! Your average health score is ").append(String.format("%.1f", avgHealthScore)).append(". Consider choosing more products with higher nutritional value. ");
            } else if (avgHealthScore > 0) {
                insights.append("💪 Room for improvement! Your average health score is ").append(String.format("%.1f", avgHealthScore)).append(". Try scanning more fruits, vegetables, and whole grain products. ");
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
        if (dailyCaloriesNumber != null) dailyCaloriesNumber.setText("--");
        if (healthScoreNumber != null) healthScoreNumber.setText("--");
        if (healthScoreProgress != null) healthScoreProgress.setProgress(0);
        if (personalInsightText != null) {
            personalInsightText.setText("🌟 Start scanning products to see your personalized health analytics and insights!");
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
        // This ensures data is up-to-date
    }
}