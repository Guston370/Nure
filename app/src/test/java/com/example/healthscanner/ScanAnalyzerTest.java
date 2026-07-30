package com.example.healthscanner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.healthscanner.models.Scan;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Unit tests for the analytics aggregation that drives the stats screen.
 */
public class ScanAnalyzerTest {

    private static final long DAY_MS = 24L * 60 * 60 * 1000;
    private static final double DELTA = 0.001;

    /** Fixed reference point so day bucketing is deterministic: noon, to avoid DST edges. */
    private static long now() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(2026, Calendar.MARCH, 15, 12, 0, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private static Scan scan(String name, String category, double healthScore, int calories, long timestamp) {
        Scan scan = new Scan();
        scan.setProductName(name);
        scan.setCategory(category);
        scan.setHealthScore(healthScore);
        scan.setCalories(calories);
        scan.setScanDate(new Date(timestamp));
        return scan;
    }

    @Test
    public void emptyHistoryProducesEmptyStats() {
        ScanAnalyzer.Stats stats = ScanAnalyzer.analyze(Collections.emptyList(), now());

        assertTrue(stats.isEmpty());
        assertEquals(0, stats.totalScans);
        assertEquals(0, stats.weeklyScans);
        assertEquals(0, stats.averageHealthScore, DELTA);
        assertTrue(stats.topCategories.isEmpty());
        // The trend still has seven labelled buckets so the chart renders an empty week.
        assertEquals(ScanAnalyzer.TREND_DAYS, stats.weeklyTrend.size());
        for (ScanAnalyzer.DayBucket bucket : stats.weeklyTrend) {
            assertEquals(0, bucket.count);
        }
    }

    @Test
    public void nullListIsTolerated() {
        ScanAnalyzer.Stats stats = ScanAnalyzer.analyze(null, now());

        assertEquals(0, stats.totalScans);
        assertEquals(ScanAnalyzer.TREND_DAYS, stats.weeklyTrend.size());
    }

    @Test
    public void averagesIgnoreMissingValues() {
        long now = now();
        List<Scan> scans = new ArrayList<>();
        scans.add(scan("A", "Snacks", 80, 200, now));
        scans.add(scan("B", "Snacks", 40, 400, now));
        // Zero score and zero calories mean "unknown" and must not drag the averages down.
        scans.add(scan("C", "Dairy", 0, 0, now));

        ScanAnalyzer.Stats stats = ScanAnalyzer.analyze(scans, now);

        assertEquals(3, stats.totalScans);
        assertEquals(60, stats.averageHealthScore, DELTA);
        assertEquals(300, stats.averageCalories, DELTA);
    }

    @Test
    public void weeklyAndMonthlyWindowsExcludeOlderScans() {
        long now = now();
        List<Scan> scans = new ArrayList<>();
        scans.add(scan("today", "Snacks", 70, 100, now));
        scans.add(scan("threeDaysAgo", "Snacks", 70, 100, now - 3 * DAY_MS));
        scans.add(scan("tenDaysAgo", "Dairy", 70, 100, now - 10 * DAY_MS));
        scans.add(scan("fortyDaysAgo", "Dairy", 70, 100, now - 40 * DAY_MS));

        ScanAnalyzer.Stats stats = ScanAnalyzer.analyze(scans, now);

        assertEquals(4, stats.totalScans);
        assertEquals(2, stats.weeklyScans);
        assertEquals(3, stats.monthlyScans);
    }

    @Test
    public void trendBucketsScansByDay() {
        long now = now();
        List<Scan> scans = new ArrayList<>();
        scans.add(scan("a", "Snacks", 70, 100, now));
        scans.add(scan("b", "Snacks", 70, 100, now));
        scans.add(scan("c", "Snacks", 70, 100, now - 2 * DAY_MS));

        ScanAnalyzer.Stats stats = ScanAnalyzer.analyze(scans, now);

        // Last bucket is today, index 4 is two days ago.
        assertEquals(2, stats.weeklyTrend.get(ScanAnalyzer.TREND_DAYS - 1).count);
        assertEquals(1, stats.weeklyTrend.get(ScanAnalyzer.TREND_DAYS - 3).count);
        assertEquals(0, stats.weeklyTrend.get(0).count);
    }

    @Test
    public void categoriesAreRankedAndScaledAgainstTheTop() {
        long now = now();
        List<Scan> scans = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            scans.add(scan("snack" + i, "Snacks", 70, 100, now));
        }
        for (int i = 0; i < 2; i++) {
            scans.add(scan("dairy" + i, "Dairy", 70, 100, now));
        }
        scans.add(scan("drink", "Beverages", 70, 100, now));

        ScanAnalyzer.Stats stats = ScanAnalyzer.analyze(scans, now);

        assertEquals(3, stats.topCategories.size());
        assertEquals("Snacks", stats.topCategories.get(0).category);
        assertEquals(4, stats.topCategories.get(0).count);
        assertEquals(100, stats.topCategories.get(0).percentOfTop);
        assertEquals("Dairy", stats.topCategories.get(1).category);
        assertEquals(50, stats.topCategories.get(1).percentOfTop);
        assertEquals(25, stats.topCategories.get(2).percentOfTop);
    }

    @Test
    public void categoryListIsCappedForTheLayout() {
        long now = now();
        List<Scan> scans = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            scans.add(scan("p" + i, "Category" + i, 70, 100, now));
        }

        ScanAnalyzer.Stats stats = ScanAnalyzer.analyze(scans, now);

        assertEquals(ScanAnalyzer.MAX_CATEGORY_ROWS, stats.topCategories.size());
        assertEquals(8, stats.categoryBreakdown.size());
    }

    @Test
    public void blankCategoriesCollapseIntoOther() {
        assertEquals("Other", ScanAnalyzer.normaliseCategory(null));
        assertEquals("Other", ScanAnalyzer.normaliseCategory("   "));
        assertEquals("Other", ScanAnalyzer.normaliseCategory("unknown"));
        assertEquals("Snacks", ScanAnalyzer.normaliseCategory("snacks"));
        assertEquals("Snacks", ScanAnalyzer.normaliseCategory(" Snacks "));
    }

    @Test
    public void healthyChoicesCountScoresAtOrAboveThreshold() {
        long now = now();
        List<Scan> scans = new ArrayList<>();
        scans.add(scan("good", "Snacks", 70, 100, now));
        scans.add(scan("great", "Snacks", 95, 100, now));
        scans.add(scan("poor", "Snacks", 69.9, 100, now));

        ScanAnalyzer.Stats stats = ScanAnalyzer.analyze(scans, now);

        assertEquals(2, stats.healthyChoices);
    }

    @Test
    public void averageTimeBetweenScansSpansFirstToLast() {
        long now = now();
        List<Scan> scans = new ArrayList<>();
        scans.add(scan("a", "Snacks", 70, 100, now));
        scans.add(scan("b", "Snacks", 70, 100, now - 2 * DAY_MS));
        scans.add(scan("c", "Snacks", 70, 100, now - 4 * DAY_MS));

        ScanAnalyzer.Stats stats = ScanAnalyzer.analyze(scans, now);

        assertEquals(2 * DAY_MS, stats.averageTimeBetweenScans);
        assertEquals("2d", ScanAnalyzer.formatTimeBetween(stats.averageTimeBetweenScans));
    }

    @Test
    public void singleScanHasNoAverageGap() {
        long now = now();
        ScanAnalyzer.Stats stats = ScanAnalyzer.analyze(
                Collections.singletonList(scan("a", "Snacks", 70, 100, now)), now);

        assertEquals(0, stats.averageTimeBetweenScans);
        assertEquals("--", ScanAnalyzer.formatTimeBetween(0));
    }

    @Test
    public void timeBetweenFormattingCoversMinutesHoursAndDays() {
        assertEquals("--", ScanAnalyzer.formatTimeBetween(0));
        assertEquals("--", ScanAnalyzer.formatTimeBetween(-1));
        assertEquals("1m", ScanAnalyzer.formatTimeBetween(30 * 1000));
        assertEquals("45m", ScanAnalyzer.formatTimeBetween(45 * 60 * 1000L));
        assertEquals("5h", ScanAnalyzer.formatTimeBetween(5 * 60 * 60 * 1000L));
        assertEquals("3d", ScanAnalyzer.formatTimeBetween(3 * DAY_MS));
    }

    @Test
    public void insightTextAdaptsToHistorySize() {
        ScanAnalyzer.Stats empty = ScanAnalyzer.analyze(Collections.emptyList(), now());
        assertTrue(ScanAnalyzer.buildInsight(empty).contains("first scan"));
        assertTrue(ScanAnalyzer.buildInsight(null).contains("first scan"));

        long now = now();
        List<Scan> few = new ArrayList<>();
        few.add(scan("a", "Snacks", 70, 100, now));
        assertTrue(ScanAnalyzer.buildInsight(ScanAnalyzer.analyze(few, now)).contains("Great start"));

        List<Scan> many = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            many.add(scan("p" + i, "Snacks", 90, 100, now));
        }
        String insight = ScanAnalyzer.buildInsight(ScanAnalyzer.analyze(many, now));
        assertTrue(insight.contains("Excellent choices"));
        assertTrue(insight.contains("snacks"));
        assertFalse(insight.contains("first scan"));
    }

    @Test
    public void scansWithoutDatesStillCountTowardsTotals() {
        Scan undated = new Scan();
        undated.setProductName("undated");
        undated.setCategory("Snacks");
        undated.setHealthScore(80);
        undated.setCalories(150);
        undated.setScanDate(null);

        ScanAnalyzer.Stats stats = ScanAnalyzer.analyze(Collections.singletonList(undated), now());

        assertEquals(1, stats.totalScans);
        assertEquals(80, stats.averageHealthScore, DELTA);
        assertEquals(0, stats.weeklyScans);
    }
}
