package com.example.healthscanner;

import com.example.healthscanner.models.Scan;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Derives the numbers shown on the analytics screen from a list of scans.
 *
 * <p>Kept free of Android and Firebase dependencies so the aggregation rules can be unit
 * tested, and so analytics works identically whether the scans came from the local history
 * or from Firestore.</p>
 */
public final class ScanAnalyzer {

    /** Number of days shown in the weekly trend chart. */
    public static final int TREND_DAYS = 7;

    /** Number of category rows rendered on the analytics screen. */
    public static final int MAX_CATEGORY_ROWS = 4;

    private static final long DAY_MS = 24L * 60 * 60 * 1000;

    private ScanAnalyzer() {
        // Utility class.
    }

    /**
     * A single bar in the weekly trend chart.
     */
    public static class DayBucket {
        /** Short weekday label, e.g. "Mon". */
        public final String label;
        /** Number of scans recorded on that day. */
        public final int count;

        DayBucket(String label, int count) {
            this.label = label;
            this.count = count;
        }
    }

    /**
     * A single category row on the analytics screen.
     */
    public static class CategoryShare {
        public final String category;
        public final int count;
        /** Share of the most-scanned category, 0-100, used for the progress bar width. */
        public final int percentOfTop;

        CategoryShare(String category, int count, int percentOfTop) {
            this.category = category;
            this.count = count;
            this.percentOfTop = percentOfTop;
        }
    }

    /**
     * Aggregated analytics for one user.
     */
    public static class Stats {
        public int totalScans;
        public int weeklyScans;
        public int monthlyScans;
        public double averageHealthScore;
        public double averageCalories;
        /** Average gap between consecutive scans, in milliseconds. */
        public long averageTimeBetweenScans;
        public int healthyChoices;
        public List<DayBucket> weeklyTrend = new ArrayList<>();
        public List<CategoryShare> topCategories = new ArrayList<>();
        public Map<String, Integer> categoryBreakdown = new LinkedHashMap<>();

        public boolean isEmpty() {
            return totalScans == 0;
        }
    }

    /**
     * A scan with a health score at or above this threshold counts as a "healthy choice".
     */
    public static final double HEALTHY_CHOICE_THRESHOLD = 70.0;

    /**
     * Analyse scans relative to the current time.
     */
    public static Stats analyze(List<Scan> scans) {
        return analyze(scans, System.currentTimeMillis());
    }

    /**
     * Analyse scans relative to an explicit "now", which keeps the day bucketing and the
     * weekly/monthly windows deterministic in tests.
     */
    public static Stats analyze(List<Scan> scans, long nowMillis) {
        Stats stats = new Stats();
        stats.weeklyTrend = buildEmptyTrend(nowMillis);

        if (scans == null || scans.isEmpty()) {
            return stats;
        }

        long weekCutoff = startOfDay(nowMillis - (TREND_DAYS - 1) * DAY_MS);
        long monthCutoff = startOfDay(nowMillis - 29 * DAY_MS);

        double healthScoreTotal = 0;
        int healthScoreCount = 0;
        long caloriesTotal = 0;
        int caloriesCount = 0;
        Long firstScan = null;
        Long lastScan = null;
        int[] dayCounts = new int[TREND_DAYS];
        Map<String, Integer> categories = new TreeMap<>();

        for (Scan scan : scans) {
            if (scan == null) {
                continue;
            }

            stats.totalScans++;

            if (scan.getHealthScore() > 0) {
                healthScoreTotal += scan.getHealthScore();
                healthScoreCount++;
                if (scan.getHealthScore() >= HEALTHY_CHOICE_THRESHOLD) {
                    stats.healthyChoices++;
                }
            }

            if (scan.getCalories() > 0) {
                caloriesTotal += scan.getCalories();
                caloriesCount++;
            }

            String category = normaliseCategory(scan.getCategory());
            categories.put(category, categories.getOrDefault(category, 0) + 1);

            Date scanDate = scan.getScanDate();
            if (scanDate == null) {
                continue;
            }
            long time = scanDate.getTime();

            if (firstScan == null || time < firstScan) {
                firstScan = time;
            }
            if (lastScan == null || time > lastScan) {
                lastScan = time;
            }

            if (time >= weekCutoff && time <= nowMillis) {
                stats.weeklyScans++;
                int index = dayIndex(time, nowMillis);
                if (index >= 0 && index < TREND_DAYS) {
                    dayCounts[index]++;
                }
            }
            if (time >= monthCutoff && time <= nowMillis) {
                stats.monthlyScans++;
            }
        }

        stats.averageHealthScore = healthScoreCount > 0 ? healthScoreTotal / healthScoreCount : 0;
        stats.averageCalories = caloriesCount > 0 ? (double) caloriesTotal / caloriesCount : 0;

        if (firstScan != null && lastScan != null && stats.totalScans > 1) {
            stats.averageTimeBetweenScans = (lastScan - firstScan) / (stats.totalScans - 1);
        }

        // Rebuild the trend with real counts, preserving the pre-computed labels.
        List<DayBucket> trend = new ArrayList<>(TREND_DAYS);
        for (int i = 0; i < TREND_DAYS; i++) {
            trend.add(new DayBucket(stats.weeklyTrend.get(i).label, dayCounts[i]));
        }
        stats.weeklyTrend = trend;

        stats.categoryBreakdown = new LinkedHashMap<>(categories);
        stats.topCategories = buildTopCategories(categories);

        return stats;
    }

    /**
     * Order categories by scan count (descending) and express each as a percentage of the
     * most-scanned category so the progress bars always fill the row width.
     */
    private static List<CategoryShare> buildTopCategories(Map<String, Integer> categories) {
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(categories.entrySet());
        entries.sort((a, b) -> {
            int byCount = b.getValue().compareTo(a.getValue());
            return byCount != 0 ? byCount : a.getKey().compareTo(b.getKey());
        });

        List<CategoryShare> shares = new ArrayList<>();
        if (entries.isEmpty()) {
            return shares;
        }

        int top = entries.get(0).getValue();
        int limit = Math.min(entries.size(), MAX_CATEGORY_ROWS);
        for (int i = 0; i < limit; i++) {
            Map.Entry<String, Integer> entry = entries.get(i);
            int percent = top > 0 ? (int) Math.round(entry.getValue() * 100.0 / top) : 0;
            shares.add(new CategoryShare(entry.getKey(), entry.getValue(), percent));
        }
        return shares;
    }

    /** Seven zero-valued buckets ending today, labelled Mon/Tue/... */
    private static List<DayBucket> buildEmptyTrend(long nowMillis) {
        SimpleDateFormat labelFormat = new SimpleDateFormat("EEE", Locale.getDefault());
        List<DayBucket> trend = new ArrayList<>(TREND_DAYS);
        for (int i = TREND_DAYS - 1; i >= 0; i--) {
            long day = nowMillis - i * DAY_MS;
            trend.add(new DayBucket(labelFormat.format(new Date(day)), 0));
        }
        return Collections.unmodifiableList(trend);
    }

    /** Index into the trend array: 0 is six days ago, {@code TREND_DAYS - 1} is today. */
    private static int dayIndex(long time, long nowMillis) {
        long daysAgo = (startOfDay(nowMillis) - startOfDay(time)) / DAY_MS;
        return (int) (TREND_DAYS - 1 - daysAgo);
    }

    private static long startOfDay(long millis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(millis);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    /**
     * Collapse blank/unknown categories into a single "Other" bucket so the chart never
     * renders an empty label.
     */
    public static String normaliseCategory(String category) {
        if (category == null) {
            return "Other";
        }
        String trimmed = category.trim();
        if (trimmed.isEmpty() || "unknown".equalsIgnoreCase(trimmed) || "null".equalsIgnoreCase(trimmed)) {
            return "Other";
        }
        // Titlecase the first letter for consistent presentation.
        return Character.toUpperCase(trimmed.charAt(0)) + trimmed.substring(1);
    }

    /**
     * Human readable average gap between scans, e.g. "4h" or "2d".
     */
    public static String formatTimeBetween(long millis) {
        if (millis <= 0) {
            return "--";
        }
        long hours = millis / (60 * 60 * 1000);
        if (hours >= 24) {
            return (hours / 24) + "d";
        }
        if (hours >= 1) {
            return hours + "h";
        }
        long minutes = millis / (60 * 1000);
        return Math.max(minutes, 1) + "m";
    }

    /**
     * Build the personalised insight paragraph shown at the bottom of the analytics screen.
     */
    public static String buildInsight(Stats stats) {
        if (stats == null || stats.totalScans == 0) {
            return "\uD83C\uDF1F Welcome to Nure! Start scanning products to see your personalised "
                    + "health insights here. Your journey to healthier choices begins with your first scan.";
        }

        if (stats.totalScans < 5) {
            return "\uD83D\uDE80 Great start! You've scanned " + stats.totalScans + " product"
                    + (stats.totalScans > 1 ? "s" : "")
                    + ". Keep scanning to build your health profile and unlock more detailed insights.";
        }

        StringBuilder insight = new StringBuilder();
        if (stats.averageHealthScore >= 70) {
            insight.append("\uD83C\uDF1F Excellent choices! Your average health score of ")
                    .append(String.format(Locale.getDefault(), "%.0f", stats.averageHealthScore))
                    .append("/100 shows you're making great nutritional decisions. ");
        } else if (stats.averageHealthScore >= 50) {
            insight.append("\uD83D\uDC4D Good progress! Your average health score is ")
                    .append(String.format(Locale.getDefault(), "%.0f", stats.averageHealthScore))
                    .append("/100. Consider choosing more products with higher nutritional value. ");
        } else if (stats.averageHealthScore > 0) {
            insight.append("\uD83D\uDCAA Room for improvement! Your average health score is ")
                    .append(String.format(Locale.getDefault(), "%.0f", stats.averageHealthScore))
                    .append("/100. Try scanning more fruits, vegetables and whole grain products. ");
        }

        if (stats.averageCalories > 400) {
            insight.append("Consider lower-calorie options to keep your diet balanced. ");
        } else if (stats.averageCalories > 0 && stats.averageCalories <= 200) {
            insight.append("Nice work picking lower-calorie products. ");
        }

        if (!stats.topCategories.isEmpty()) {
            insight.append("You scan ")
                    .append(stats.topCategories.get(0).category.toLowerCase(Locale.getDefault()))
                    .append(" most often. ");
        }

        insight.append("That's ").append(stats.totalScans).append(" scans in total");
        if (stats.weeklyScans > 0) {
            insight.append(", ").append(stats.weeklyScans).append(" this week");
        }
        insight.append(". Keep it up! \uD83C\uDFAF");

        return insight.toString();
    }
}
