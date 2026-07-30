package com.example.healthscanner;

import com.example.healthscanner.models.Scan;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Renders scan history as RFC 4180 style CSV for the "Export Scan History" setting.
 *
 * <p>Pure string building, no Android dependencies, so the escaping rules can be unit
 * tested. File writing lives in {@code SettingsActivity}.</p>
 */
public final class ScanCsvExporter {

    static final String HEADER = "Scanned At,Product Name,Brand,Barcode,Category,Health Score,"
            + "Health Grade,Calories (kcal/100g),Protein (g),Carbs (g),Fat (g),Sugar (g),"
            + "Sodium (mg),Fiber (g),Favourite";

    private static final String DATE_PATTERN = "yyyy-MM-dd HH:mm";

    private ScanCsvExporter() {
        // Utility class.
    }

    /**
     * Build the CSV document. A header row is always emitted, even for an empty history,
     * so the exported file is still valid.
     */
    public static String toCsv(List<Scan> scans) {
        StringBuilder csv = new StringBuilder(HEADER).append('\n');
        if (scans == null) {
            return csv.toString();
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_PATTERN, Locale.US);
        for (Scan scan : scans) {
            if (scan == null) {
                continue;
            }
            Date scanDate = scan.getScanDate();
            csv.append(escape(scanDate != null ? dateFormat.format(scanDate) : "")).append(',')
                    .append(escape(scan.getProductName())).append(',')
                    .append(escape(scan.getBrand())).append(',')
                    .append(escape(scan.getBarcode())).append(',')
                    .append(escape(ScanAnalyzer.normaliseCategory(scan.getCategory()))).append(',')
                    .append(number(scan.getHealthScore(), 0)).append(',')
                    .append(escape(scan.getHealthGrade() != null
                            ? scan.getHealthGrade()
                            : HealthScoreCalculator.gradeFor(scan.getHealthScore())))
                    .append(',')
                    .append(scan.getCalories()).append(',')
                    .append(number(scan.getProtein(), 1)).append(',')
                    .append(number(scan.getCarbs(), 1)).append(',')
                    .append(number(scan.getFat(), 1)).append(',')
                    .append(number(scan.getSugar(), 1)).append(',')
                    .append(number(scan.getSodium(), 0)).append(',')
                    .append(number(scan.getFiber(), 1)).append(',')
                    .append(scan.isFavorite() ? "yes" : "no")
                    .append('\n');
        }
        return csv.toString();
    }

    /** Suggested file name, unique per export so repeated exports don't collide. */
    public static String suggestedFileName(long timestamp) {
        return "nure-scan-history-"
                + new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(new Date(timestamp))
                + ".csv";
    }

    private static String number(double value, int decimals) {
        return String.format(Locale.US, "%." + decimals + "f", value);
    }

    /**
     * Quote fields containing commas, quotes or newlines; double up embedded quotes.
     */
    static String escape(String value) {
        if (value == null) {
            return "";
        }
        boolean needsQuoting = value.indexOf(',') >= 0
                || value.indexOf('"') >= 0
                || value.indexOf('\n') >= 0
                || value.indexOf('\r') >= 0;
        if (!needsQuoting) {
            return value;
        }
        return '"' + value.replace("\"", "\"\"") + '"';
    }
}
