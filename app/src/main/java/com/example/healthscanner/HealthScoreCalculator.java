package com.example.healthscanner;

/**
 * Health score calculation for scanned products.
 *
 * <p>The scoring model is intentionally kept free of Android dependencies so it can be
 * unit tested and reused by every screen that needs a score (product details, analytics,
 * CSV export). Scores are on a 0-100 scale.</p>
 */
public final class HealthScoreCalculator {

    /** Maximum points awarded per nutrient bucket. */
    private static final int CALORIE_POINTS = 20;
    private static final int SUGAR_POINTS = 20;
    private static final int FAT_POINTS = 15;
    private static final int PROTEIN_POINTS = 15;
    private static final int FIBER_POINTS = 15;
    private static final int SODIUM_POINTS = 15;

    private HealthScoreCalculator() {
        // Utility class.
    }

    /**
     * Nutrition facts per 100g/100ml used as scoring input.
     */
    public static class Nutrition {
        public double calories;
        public double protein;
        public double sugar;
        public double fat;
        public double carbs;
        public double fiber;
        /** Sodium in milligrams. */
        public double sodium;

        public Nutrition() {
        }

        public Nutrition(double calories, double protein, double sugar, double fat,
                double carbs, double fiber, double sodium) {
            this.calories = calories;
            this.protein = protein;
            this.sugar = sugar;
            this.fat = fat;
            this.carbs = carbs;
            this.fiber = fiber;
            this.sodium = sodium;
        }
    }

    /**
     * Calculate the 0-100 health score for the supplied nutrition facts.
     *
     * @param nutrition nutrition facts per 100g; {@code null} yields 0
     * @return score clamped to the 0-100 range
     */
    public static double calculate(Nutrition nutrition) {
        if (nutrition == null) {
            return 0;
        }

        double score = 0;
        score += scoreCalories(nutrition.calories);
        score += scoreSugar(nutrition.sugar);
        score += scoreFat(nutrition.fat);
        score += scoreProtein(nutrition.protein);
        score += scoreFiber(nutrition.fiber);
        score += scoreSodium(nutrition.sodium);

        return Math.max(0, Math.min(score, 100));
    }

    /** Lower calories score higher. */
    static int scoreCalories(double calories) {
        if (calories <= 100)
            return CALORIE_POINTS;
        if (calories <= 200)
            return 16;
        if (calories <= 300)
            return 12;
        if (calories <= 400)
            return 8;
        if (calories <= 500)
            return 4;
        return 0;
    }

    /** Lower sugar scores higher. */
    static int scoreSugar(double sugar) {
        if (sugar <= 2)
            return SUGAR_POINTS;
        if (sugar <= 5)
            return 16;
        if (sugar <= 10)
            return 12;
        if (sugar <= 15)
            return 8;
        if (sugar <= 20)
            return 4;
        return 0;
    }

    /** Lower fat scores higher. */
    static int scoreFat(double fat) {
        if (fat <= 3)
            return FAT_POINTS;
        if (fat <= 10)
            return 12;
        if (fat <= 15)
            return 8;
        if (fat <= 20)
            return 4;
        return 0;
    }

    /** Higher protein scores higher. */
    static int scoreProtein(double protein) {
        if (protein >= 20)
            return PROTEIN_POINTS;
        if (protein >= 15)
            return 12;
        if (protein >= 10)
            return 9;
        if (protein >= 5)
            return 6;
        if (protein >= 2)
            return 3;
        return 0;
    }

    /** Higher fiber scores higher. */
    static int scoreFiber(double fiber) {
        if (fiber >= 10)
            return FIBER_POINTS;
        if (fiber >= 6)
            return 12;
        if (fiber >= 3)
            return 9;
        if (fiber >= 1.5)
            return 6;
        if (fiber >= 0.5)
            return 3;
        return 0;
    }

    /** Lower sodium (mg) scores higher. */
    static int scoreSodium(double sodiumMg) {
        if (sodiumMg <= 100)
            return SODIUM_POINTS;
        if (sodiumMg <= 300)
            return 12;
        if (sodiumMg <= 600)
            return 9;
        if (sodiumMg <= 1000)
            return 6;
        if (sodiumMg <= 1500)
            return 3;
        return 0;
    }

    /**
     * Map a score onto the A-E grade shown on product details and stored with each scan.
     */
    public static String gradeFor(double score) {
        if (score >= 85)
            return "A";
        if (score >= 70)
            return "B";
        if (score >= 55)
            return "C";
        if (score >= 40)
            return "D";
        return "E";
    }

    /**
     * Emoji used alongside the score in the UI.
     */
    public static String emojiFor(double score) {
        if (score >= 85)
            return "\uD83C\uDF1F"; // star
        if (score >= 70)
            return "\uD83D\uDE0A"; // smile
        if (score >= 55)
            return "\uD83D\uDE10"; // neutral
        if (score >= 40)
            return "\uD83D\uDE15"; // confused
        return "\uD83D\uDE30"; // anxious
    }
}
