package com.example.healthscanner;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Translates the raw labels produced by ML Kit's image labeller into food names Nure can
 * look nutrition up for.
 *
 * <p>ML Kit's general model returns a mixture of specific foods ("Pizza"), broad food
 * concepts ("Dessert", "Fruit") and scene furniture ("Plate", "Tableware"). Only the first
 * kind is a usable search term. The classification lives in {@code assets/food_labels.json}
 * so the vocabulary can grow without code changes.</p>
 *
 * <p>Deliberately free of Android dependencies so the ranking rules are unit testable.</p>
 */
public final class FoodLabelMapper {

    /**
     * Confidence floor for a label to be considered at all. ML Kit's default threshold is
     * 0.5; we go lower because a weak but specific label plus a strong generic "Food" label
     * is still a useful signal.
     */
    public static final float MIN_CONFIDENCE = 0.30f;

    private final Map<String, String> specific;
    private final Set<String> generic;

    private FoodLabelMapper(Map<String, String> specific, Set<String> generic) {
        this.specific = specific;
        this.generic = generic;
    }

    /** An empty mapper: everything is treated as an unrecognised label. */
    public static FoodLabelMapper empty() {
        return new FoodLabelMapper(new HashMap<>(), new HashSet<>());
    }

    /**
     * Parse {@code food_labels.json}. A malformed or missing file degrades to
     * {@link #empty()} rather than breaking recognition.
     */
    public static FoodLabelMapper fromJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return empty();
        }

        Map<String, String> specific = new HashMap<>();
        Set<String> generic = new HashSet<>();

        try {
            JSONObject root = new JSONObject(json);

            JSONObject specificJson = root.optJSONObject("specific");
            if (specificJson != null) {
                for (java.util.Iterator<String> it = specificJson.keys(); it.hasNext();) {
                    String key = it.next();
                    String value = specificJson.optString(key, "").trim();
                    if (!value.isEmpty()) {
                        specific.put(normalise(key), value);
                    }
                }
            }

            JSONArray genericJson = root.optJSONArray("generic");
            if (genericJson != null) {
                for (int i = 0; i < genericJson.length(); i++) {
                    String value = genericJson.optString(i, "").trim();
                    if (!value.isEmpty()) {
                        generic.add(normalise(value));
                    }
                }
            }
        } catch (Exception e) {
            return empty();
        }

        return new FoodLabelMapper(specific, generic);
    }

    /**
     * How confident we are that a label identifies a particular food.
     */
    public enum Kind {
        /** Names an actual food; usable as a nutrition search term. */
        SPECIFIC,
        /** Only says "this is food"; not a search term on its own. */
        GENERIC,
        /** Not in the map. Offered to the user but ranked last. */
        UNKNOWN
    }

    public Kind kindOf(String label) {
        String key = normalise(label);
        if (specific.containsKey(key)) {
            return Kind.SPECIFIC;
        }
        if (generic.contains(key)) {
            return Kind.GENERIC;
        }
        return Kind.UNKNOWN;
    }

    /** The canonical food name for a label, or the label itself when unmapped. */
    public String canonicalNameFor(String label) {
        String mapped = specific.get(normalise(label));
        return mapped != null ? mapped : (label == null ? "" : label.trim());
    }

    /**
     * A single recognition candidate, ready to be shown to the user or searched for.
     */
    public static class Candidate {
        /** Canonical, lower-case food name used as the nutrition search term. */
        public final String foodName;
        /** The raw ML Kit label, kept for display and debugging. */
        public final String rawLabel;
        public final float confidence;
        public final Kind kind;

        Candidate(String foodName, String rawLabel, float confidence, Kind kind) {
            this.foodName = foodName;
            this.rawLabel = rawLabel;
            this.confidence = confidence;
            this.kind = kind;
        }

        /** Title-cased name for display, e.g. "french fries" to "French Fries". */
        public String displayName() {
            return toDisplayCase(foodName);
        }

        @Override
        public String toString() {
            return displayName();
        }
    }

    /**
     * The outcome of mapping one image's labels.
     */
    public static class Outcome {
        /** Searchable candidates, best first. May be empty. */
        public final List<Candidate> candidates;
        /**
         * True when at least one label said "this is food", even if nothing specific was
         * identified. Lets the UI distinguish "that isn't food" from "I can't name it".
         */
        public final boolean looksLikeFood;

        Outcome(List<Candidate> candidates, boolean looksLikeFood) {
            this.candidates = candidates;
            this.looksLikeFood = looksLikeFood;
        }

        public boolean hasIdentifiedFood() {
            return !candidates.isEmpty() && candidates.get(0).kind == Kind.SPECIFIC;
        }
    }

    /**
     * Rank ML Kit labels into usable candidates.
     *
     * <p>Specific foods always outrank unknown labels regardless of confidence, because a
     * 40%-confidence "Pizza" is far more actionable than a 90%-confidence "Tableware".
     * Generic labels are dropped from the candidate list but recorded via
     * {@link Outcome#looksLikeFood}.</p>
     *
     * @param labels raw label text paired with confidence, in any order
     * @param limit  maximum number of candidates to return
     */
    public Outcome rank(List<String> labels, List<Float> confidences, int limit) {
        List<Candidate> candidates = new ArrayList<>();
        boolean looksLikeFood = false;

        int count = Math.min(
                labels == null ? 0 : labels.size(),
                confidences == null ? 0 : confidences.size());

        for (int i = 0; i < count; i++) {
            String label = labels.get(i);
            float confidence = confidences.get(i) == null ? 0f : confidences.get(i);

            if (label == null || label.trim().isEmpty() || confidence < MIN_CONFIDENCE) {
                continue;
            }

            Kind kind = kindOf(label);
            if (kind == Kind.GENERIC) {
                looksLikeFood = true;
                continue;
            }
            if (kind == Kind.SPECIFIC) {
                looksLikeFood = true;
            }

            candidates.add(new Candidate(
                    canonicalNameFor(label).toLowerCase(Locale.US), label.trim(), confidence, kind));
        }

        // Specific labels first, then by descending confidence.
        Collections.sort(candidates, (a, b) -> {
            if (a.kind != b.kind) {
                return a.kind == Kind.SPECIFIC ? -1 : 1;
            }
            return Float.compare(b.confidence, a.confidence);
        });

        // Drop duplicates that collapsed onto the same canonical name.
        List<Candidate> deduped = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Candidate candidate : candidates) {
            if (seen.add(candidate.foodName) && deduped.size() < limit) {
                deduped.add(candidate);
            }
        }

        return new Outcome(deduped, looksLikeFood);
    }

    private static String normalise(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.US);
    }

    /** Title-case a food name for display. */
    public static String toDisplayCase(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "";
        }
        String[] words = value.trim().replace('_', ' ').split("\\s+");
        StringBuilder builder = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(word.charAt(0)))
                    .append(word.substring(1).toLowerCase(Locale.US));
        }
        return builder.toString();
    }
}
