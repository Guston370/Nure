package com.example.healthscanner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The offline tier of nutrition lookup, backed by {@code assets/nutrition_dataset.csv}.
 *
 * <p>Covers common whole foods (fruit and vegetables) so the most frequent photo scans
 * resolve instantly with no network call. Anything else falls through to the online tiers in
 * {@link NutritionRepository}.</p>
 *
 * <p>Pure Java, constructed from a {@link Reader}, so the parsing and fuzzy-matching rules
 * are unit testable without an Android context.</p>
 */
public final class LocalNutritionTable {

    /** Column order in nutrition_dataset.csv. */
    private static final int COL_LABEL = 0;
    private static final int COL_CALORIES = 2;
    private static final int COL_PROTEIN = 3;
    private static final int COL_FAT = 4;
    private static final int COL_CARBS = 5;
    private static final int COL_FIBER = 6;
    private static final int MIN_COLUMNS = 7;

    /** label (normalised) to nutrition facts per 100g. */
    private final Map<String, HealthScoreCalculator.Nutrition> byLabel;
    /** normalised label to the original display label. */
    private final Map<String, String> displayLabels;

    private LocalNutritionTable(Map<String, HealthScoreCalculator.Nutrition> byLabel,
            Map<String, String> displayLabels) {
        this.byLabel = byLabel;
        this.displayLabels = displayLabels;
    }

    public static LocalNutritionTable empty() {
        return new LocalNutritionTable(new LinkedHashMap<>(), new LinkedHashMap<>());
    }

    /**
     * Parse the CSV. Rows that are malformed or have unparseable numbers are skipped so one
     * bad line can't take out the whole table.
     *
     * <p>The dataset has no sugar or sodium columns, so those stay at zero. The health score
     * calculator treats zero as "unknown-and-therefore-best-case" for those nutrients, which
     * is the same assumption the rest of the app already makes.</p>
     */
    public static LocalNutritionTable fromCsv(Reader reader) {
        Map<String, HealthScoreCalculator.Nutrition> byLabel = new LinkedHashMap<>();
        Map<String, String> displayLabels = new LinkedHashMap<>();

        if (reader == null) {
            return empty();
        }

        try (BufferedReader buffered = new BufferedReader(reader)) {
            String line = buffered.readLine(); // header
            while ((line = buffered.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }

                String[] columns = parseCsvLine(line);
                if (columns.length < MIN_COLUMNS) {
                    continue;
                }

                String label = columns[COL_LABEL].trim();
                if (label.isEmpty()) {
                    continue;
                }

                try {
                    HealthScoreCalculator.Nutrition nutrition = new HealthScoreCalculator.Nutrition();
                    nutrition.calories = Double.parseDouble(columns[COL_CALORIES].trim());
                    nutrition.protein = Double.parseDouble(columns[COL_PROTEIN].trim());
                    nutrition.fat = Double.parseDouble(columns[COL_FAT].trim());
                    nutrition.carbs = Double.parseDouble(columns[COL_CARBS].trim());
                    nutrition.fiber = Double.parseDouble(columns[COL_FIBER].trim());

                    String key = normalise(label);
                    byLabel.put(key, nutrition);
                    displayLabels.put(key, label);
                } catch (NumberFormatException ignored) {
                    // Skip rows with non-numeric nutrition values.
                }
            }
        } catch (IOException e) {
            // Return whatever parsed before the failure.
        }

        return new LocalNutritionTable(byLabel, displayLabels);
    }

    public int size() {
        return byLabel.size();
    }

    public List<String> labels() {
        return new ArrayList<>(displayLabels.values());
    }

    /**
     * Look a food name up, tolerating the naming differences between the recognition
     * vocabulary and the dataset.
     *
     * <p>Tried in order: exact match, simple plural/singular variants, then a containment
     * match in either direction so "green apple" finds "apple" and "corn" finds
     * "sweetcorn". Returns {@code null} when nothing matches.</p>
     */
    public HealthScoreCalculator.Nutrition lookup(String foodName) {
        String key = matchKey(foodName);
        return key == null ? null : byLabel.get(key);
    }

    /** The dataset's own label for a matched food, useful for display. */
    public String matchedLabel(String foodName) {
        String key = matchKey(foodName);
        return key == null ? null : displayLabels.get(key);
    }

    private String matchKey(String foodName) {
        String key = normalise(foodName);
        if (key.isEmpty()) {
            return null;
        }

        if (byLabel.containsKey(key)) {
            return key;
        }

        for (String variant : variantsOf(key)) {
            if (byLabel.containsKey(variant)) {
                return variant;
            }
        }

        // Containment, longest dataset label first so "sweet potato" beats "potato".
        String best = null;
        for (String candidate : byLabel.keySet()) {
            if (candidate.contains(key) || key.contains(candidate)) {
                if (best == null || candidate.length() > best.length()) {
                    best = candidate;
                }
            }
        }
        return best;
    }

    /** Cheap plural/singular variants; deliberately not a full stemmer. */
    static List<String> variantsOf(String key) {
        List<String> variants = new ArrayList<>();
        if (key.endsWith("ies") && key.length() > 3) {
            variants.add(key.substring(0, key.length() - 3) + "y");
        }
        if (key.endsWith("es") && key.length() > 2) {
            variants.add(key.substring(0, key.length() - 2));
        }
        if (key.endsWith("s") && key.length() > 1) {
            variants.add(key.substring(0, key.length() - 1));
        }
        variants.add(key + "s");
        variants.add(key + "es");
        return variants;
    }

    /**
     * Split a CSV line, honouring double-quoted fields (the dataset's {@code image_paths}
     * column contains commas inside quotes).
     */
    static String[] parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                fields.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString());

        return fields.toArray(new String[0]);
    }

    private static String normalise(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.US).replace('_', ' ').replaceAll("\\s+", " ");
    }
}
