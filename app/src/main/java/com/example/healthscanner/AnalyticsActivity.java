package com.example.healthscanner;

import android.animation.ValueAnimator;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.healthscanner.database.FirebaseScanManager;
import com.example.healthscanner.database.ScanHistoryStore;
import com.example.healthscanner.models.Scan;

import java.util.List;
import java.util.Locale;

/**
 * Analytics screen.
 *
 * <p>Numbers are derived by {@link ScanAnalyzer} from the user's scans. The local history is
 * rendered first so the screen is useful offline and on first paint, then the Firestore
 * {@code scans} collection is queried and, when it holds more history than the device does,
 * the screen re-renders from it.</p>
 */
public class AnalyticsActivity extends BaseActivity {

    private static final String TAG = "AnalyticsActivity";

    /** Daily scan target shown in the goal strip. */
    private static final int DAILY_GOAL = 5;

    private static final int TREND_BAR_MAX_HEIGHT_DP = 110;
    private static final int TREND_BAR_MIN_HEIGHT_DP = 6;

    // Header
    private TextView analyticsTitle;
    private View refreshIcon;
    private View backButton;

    // Metric cards
    private View totalScansCard;
    private View weeklyScansCard;
    private View healthScoreCard;
    private View avgCaloriesCard;
    private View categoriesCard;
    private View trendsCard;
    private View insightsCard;

    // Metric values
    private TextView totalScansNumber;
    private TextView weeklyScansNumber;
    private TextView avgHealthScoreNumber;
    private TextView avgCaloriesNumber;
    private ProgressBar healthScoreProgress;

    // Breakdown + trend + insight
    private LinearLayout categoryBarsContainer;
    private TextView categoriesEmptyText;
    private LinearLayout trendBarsContainer;
    private LinearLayout trendLabelsContainer;
    private TextView personalInsightText;
    private TextView goalProgressText;

    private AuthManager authManager;
    private FirebaseScanManager scanManager;
    private ScanHistoryStore scanHistoryStore;

    /** Number of scans the currently rendered stats were built from. */
    private int renderedScanCount = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analytics_enhanced);

        authManager = AuthManager.getInstance(this);
        scanManager = FirebaseScanManager.getInstance();
        scanHistoryStore = ScanHistoryStore.getInstance(this);

        // Trust navigation from the authenticated home screen; only guard direct launches.
        boolean fromNavigation = getIntent().getBooleanExtra("from_navigation", false);
        if (!fromNavigation && !authManager.isUserAuthenticated()) {
            authManager.navigateToLogin(this);
            return;
        }

        initializeViews();
        initializeBottomNavigation();
        setupEntranceAnimations();
        setupClickListeners();
        loadAnalytics(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAnalytics(false);
    }

    @Override
    protected int getCurrentNavigationItemId() {
        return R.id.nav_stats;
    }

    private void initializeViews() {
        analyticsTitle = findViewById(R.id.analyticsTitle);
        refreshIcon = findViewById(R.id.refreshIcon);
        backButton = findViewById(R.id.backButton);

        totalScansCard = findViewById(R.id.totalScansCard);
        weeklyScansCard = findViewById(R.id.weeklyScansCard);
        healthScoreCard = findViewById(R.id.healthScoreCard);
        avgCaloriesCard = findViewById(R.id.avgCaloriesCard);
        categoriesCard = findViewById(R.id.categoriesCard);
        trendsCard = findViewById(R.id.trendsCard);
        insightsCard = findViewById(R.id.insightsCard);

        totalScansNumber = findViewById(R.id.totalScansNumber);
        weeklyScansNumber = findViewById(R.id.weeklyScansNumber);
        avgHealthScoreNumber = findViewById(R.id.healthScoreNumber);
        avgCaloriesNumber = findViewById(R.id.avgCaloriesNumber);
        healthScoreProgress = findViewById(R.id.healthScoreProgress);

        categoryBarsContainer = findViewById(R.id.categoryBarsContainer);
        categoriesEmptyText = findViewById(R.id.categoriesEmptyText);
        trendBarsContainer = findViewById(R.id.trendBarsContainer);
        trendLabelsContainer = findViewById(R.id.trendLabelsContainer);
        personalInsightText = findViewById(R.id.personalInsightText);
        goalProgressText = findViewById(R.id.goalProgressText);
    }

    private void setupEntranceAnimations() {
        if (analyticsTitle != null) {
            analyticsTitle.postDelayed(
                    () -> analyticsTitle.startAnimation(
                            AnimationUtils.loadAnimation(this, R.anim.fade_in_slide_down)),
                    150);
        }

        View[] cards = { totalScansCard, healthScoreCard, weeklyScansCard, avgCaloriesCard,
                categoriesCard, trendsCard, insightsCard };
        for (int i = 0; i < cards.length; i++) {
            final View card = cards[i];
            if (card == null) {
                continue;
            }
            final boolean isLast = i == cards.length - 1;
            card.postDelayed(() -> card.startAnimation(AnimationUtils.loadAnimation(
                    this, isLast ? R.anim.fade_in : R.anim.slide_up)), 250 + i * 80L);
        }
    }

    private void setupClickListeners() {
        if (backButton != null) {
            backButton.setOnClickListener(v -> finish());
        }

        if (refreshIcon != null) {
            refreshIcon.setOnClickListener(v -> {
                v.animate().rotation(v.getRotation() + 360f).setDuration(500).start();
                v.startAnimation(AnimationUtils.loadAnimation(this, R.anim.scale_bounce));
                loadAnalytics(true);
            });
        }
    }

    /**
     * Render local history, then reconcile with Firestore.
     *
     * @param showFeedback whether to toast the outcome (used by the refresh button)
     */
    private void loadAnalytics(boolean showFeedback) {
        List<Scan> localScans = scanHistoryStore.getScans();
        renderedScanCount = -1;
        render(ScanAnalyzer.analyze(localScans), localScans.size());

        String userId = authManager.getCurrentUserId();
        if (userId == null || userId.isEmpty()) {
            Log.d(TAG, "No signed-in user, showing local analytics only");
            if (showFeedback) {
                Toast.makeText(this, R.string.analytics_refreshed, Toast.LENGTH_SHORT).show();
            }
            return;
        }

        scanManager.getUserScans(userId, new FirebaseScanManager.ScanListCallback() {
            @Override
            public void onSuccess(List<Scan> scans) {
                runOnUiThread(() -> {
                    // The device only keeps the most recent scans, so Firestore wins when it
                    // has a longer history. Otherwise the local render already stands.
                    if (scans.size() > renderedScanCount) {
                        render(ScanAnalyzer.analyze(scans), scans.size());
                    }
                    if (showFeedback) {
                        Toast.makeText(AnalyticsActivity.this,
                                R.string.analytics_refreshed, Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onFailure(String error) {
                Log.w(TAG, "Firestore analytics unavailable, keeping local view: " + error);
                if (showFeedback) {
                    runOnUiThread(() -> Toast.makeText(AnalyticsActivity.this,
                            R.string.analytics_load_failed, Toast.LENGTH_LONG).show());
                }
            }
        });
    }

    private void render(ScanAnalyzer.Stats stats, int sourceScanCount) {
        renderedScanCount = sourceScanCount;

        animateCounter(totalScansNumber, stats.totalScans, false);
        animateCounter(weeklyScansNumber, stats.weeklyScans, false);
        animateCounter(avgCaloriesNumber, (int) Math.round(stats.averageCalories), true);
        renderHealthScore(stats.averageHealthScore);
        renderCategories(stats);
        renderTrend(stats);
        renderInsight(stats);
    }

    /**
     * Count up to the target value.
     *
     * @param blankWhenZero show "--" instead of 0, used for averages that have no data yet
     */
    private void animateCounter(TextView view, int target, boolean blankWhenZero) {
        if (view == null) {
            return;
        }
        if (target <= 0) {
            view.setText(blankWhenZero ? "--" : "0");
            return;
        }

        ValueAnimator animator = ValueAnimator.ofInt(0, target);
        animator.setDuration(900);
        animator.addUpdateListener(animation ->
                view.setText(String.format(Locale.getDefault(), "%,d", (int) animation.getAnimatedValue())));
        animator.start();
    }

    private void renderHealthScore(double score) {
        int rounded = (int) Math.round(score);

        if (avgHealthScoreNumber != null) {
            if (rounded <= 0) {
                avgHealthScoreNumber.setText("--");
            } else {
                ValueAnimator animator = ValueAnimator.ofInt(0, rounded);
                animator.setDuration(900);
                animator.addUpdateListener(animation ->
                        avgHealthScoreNumber.setText(String.valueOf((int) animation.getAnimatedValue())));
                animator.start();
            }
        }

        if (healthScoreProgress != null) {
            ValueAnimator progressAnimator = ValueAnimator.ofInt(0, Math.max(rounded, 0));
            progressAnimator.setDuration(900);
            progressAnimator.addUpdateListener(animation ->
                    healthScoreProgress.setProgress((int) animation.getAnimatedValue()));
            progressAnimator.start();
        }
    }

    /**
     * Rebuild the "Most Scanned" rows from the real category breakdown.
     */
    private void renderCategories(ScanAnalyzer.Stats stats) {
        if (categoryBarsContainer == null) {
            return;
        }

        categoryBarsContainer.removeAllViews();

        if (stats.topCategories.isEmpty()) {
            categoryBarsContainer.setVisibility(View.GONE);
            if (categoriesEmptyText != null) {
                categoriesEmptyText.setVisibility(View.VISIBLE);
            }
            return;
        }

        categoryBarsContainer.setVisibility(View.VISIBLE);
        if (categoriesEmptyText != null) {
            categoriesEmptyText.setVisibility(View.GONE);
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        for (ScanAnalyzer.CategoryShare share : stats.topCategories) {
            View row = inflater.inflate(R.layout.item_category_bar, categoryBarsContainer, false);

            TextView name = row.findViewById(R.id.categoryName);
            TextView count = row.findViewById(R.id.categoryCount);
            ProgressBar progress = row.findViewById(R.id.categoryProgress);

            name.setText(share.category);
            count.setText(getResources().getQuantityString(
                    R.plurals.analytics_category_scans, share.count, share.count));
            progress.setProgress(share.percentOfTop);
            progress.setContentDescription(share.category + ": " + share.count + " scans");

            categoryBarsContainer.addView(row);
        }
    }

    /**
     * Draw one bar per day for the last week, scaled against the busiest day.
     */
    private void renderTrend(ScanAnalyzer.Stats stats) {
        if (trendBarsContainer == null || trendLabelsContainer == null) {
            return;
        }

        trendBarsContainer.removeAllViews();
        trendLabelsContainer.removeAllViews();

        int busiestDay = 0;
        for (ScanAnalyzer.DayBucket bucket : stats.weeklyTrend) {
            busiestDay = Math.max(busiestDay, bucket.count);
        }

        float density = getResources().getDisplayMetrics().density;
        int minHeight = (int) (TREND_BAR_MIN_HEIGHT_DP * density);
        int maxHeight = (int) (TREND_BAR_MAX_HEIGHT_DP * density);
        int margin = (int) (4 * density);

        for (ScanAnalyzer.DayBucket bucket : stats.weeklyTrend) {
            int height = minHeight;
            if (busiestDay > 0 && bucket.count > 0) {
                height = minHeight + Math.round((maxHeight - minHeight) * (bucket.count / (float) busiestDay));
            }

            View bar = new View(this);
            LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(0, height, 1f);
            barParams.setMargins(margin, 0, margin, 0);
            bar.setLayoutParams(barParams);
            bar.setBackgroundResource(R.drawable.chart_bar_gradient);
            // Flatten empty days so the chart reads as "nothing scanned" rather than "a little".
            bar.setAlpha(bucket.count > 0 ? 1f : 0.25f);
            bar.setContentDescription(bucket.label + ": " + bucket.count + " scans");
            trendBarsContainer.addView(bar);

            TextView label = new TextView(this);
            label.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            label.setText(bucket.label);
            label.setTextSize(11f);
            label.setGravity(android.view.Gravity.CENTER);
            label.setTextColor(0xFF999B96);
            trendLabelsContainer.addView(label);
        }
    }

    private void renderInsight(ScanAnalyzer.Stats stats) {
        if (personalInsightText != null) {
            personalInsightText.setText(ScanAnalyzer.buildInsight(stats));
        }

        if (goalProgressText != null) {
            // Today is the last bucket in the weekly trend.
            int today = stats.weeklyTrend.isEmpty()
                    ? 0
                    : stats.weeklyTrend.get(stats.weeklyTrend.size() - 1).count;
            goalProgressText.setText(Math.min(today, DAILY_GOAL) + "/" + DAILY_GOAL);
        }
    }
}
