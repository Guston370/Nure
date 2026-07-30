package com.example.healthscanner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.json.JSONObject;
import org.junit.Test;

/**
 * Unit tests for the guards that stop wrong nutrition reaching the UI.
 *
 * <p>Regression cover for the bug where a photo of bananas resolved to "Blue Bird Chips" at
 * 2250 kcal per 100g: the search returned an unrelated branded product, and nothing checked
 * either the relevance of the match or the sanity of the numbers.</p>
 */
public class NutritionPlausibilityTest {

    private static HealthScoreCalculator.Nutrition nutrition(double calories, double protein,
            double carbs, double fat, double sugar, double fiber, double sodium) {
        HealthScoreCalculator.Nutrition n = new HealthScoreCalculator.Nutrition();
        n.calories = calories;
        n.protein = protein;
        n.carbs = carbs;
        n.fat = fat;
        n.sugar = sugar;
        n.fiber = fiber;
        n.sodium = sodium;
        return n;
    }

    @Test
    public void realisticFoodIsPlausible() {
        // Banana per 100g.
        assertTrue(NutritionRepository.isPlausible(
                nutrition(89, 1.1, 22.8, 0.3, 12.2, 2.6, 1)));
        // Crisps per 100g: high calorie but still physically possible.
        assertTrue(NutritionRepository.isPlausible(
                nutrition(536, 6.6, 53, 34, 0.6, 4.8, 500)));
    }

    @Test
    public void impossibleEnergyIsRejected() {
        // The exact failure that shipped: a per-pack value in a per-100g field.
        assertFalse(NutritionRepository.isPlausible(
                nutrition(2250, 10, 50, 30, 5, 2, 400)));
        assertFalse(NutritionRepository.isPlausible(
                nutrition(901, 0, 0, 100, 0, 0, 0)));
    }

    @Test
    public void zeroOrNegativeEnergyIsRejected() {
        assertFalse(NutritionRepository.isPlausible(nutrition(0, 1, 1, 1, 0, 0, 0)));
        assertFalse(NutritionRepository.isPlausible(nutrition(-10, 1, 1, 1, 0, 0, 0)));
        assertFalse(NutritionRepository.isPlausible(null));
    }

    @Test
    public void macrosCannotExceedOneHundredGrams() {
        assertFalse(NutritionRepository.isPlausible(
                nutrition(400, 120, 10, 5, 0, 0, 0)));
        // Protein + carbs + fat well over 100g in total.
        assertFalse(NutritionRepository.isPlausible(
                nutrition(400, 60, 60, 40, 0, 0, 0)));
    }

    @Test
    public void sugarCannotExceedCarbohydrate() {
        assertFalse(NutritionRepository.isPlausible(
                nutrition(300, 5, 10, 5, 40, 1, 100)));
        // Equal is fine: an all-sugar product.
        assertTrue(NutritionRepository.isPlausible(
                nutrition(387, 0, 100, 0, 100, 0, 1)));
    }

    @Test
    public void absurdSodiumIsRejected() {
        assertFalse(NutritionRepository.isPlausible(
                nutrition(300, 5, 10, 5, 5, 1, 50000)));
    }

    @Test
    public void relevanceRequiresATokenOverlap() {
        // The shipped bug: query "banana", hit "Blue Bird Chips".
        assertEquals(0, NutritionRepository.relevanceScore("Blue Bird Chips", "Snacks", "banana"));
        assertTrue(NutritionRepository.relevanceScore("Banana Chips", "Snacks", "banana") > 0);
    }

    @Test
    public void nameMatchesOutrankCategoryOnlyMatches() {
        int nameMatch = NutritionRepository.relevanceScore("Banana Bread", "Bakery", "banana");
        int categoryMatch = NutritionRepository.relevanceScore("Loaf", "Banana breads", "banana");

        assertTrue(nameMatch > categoryMatch);
        assertTrue(categoryMatch > 0);
    }

    @Test
    public void shortTokensCarryNoSignal() {
        // "of" and "a" would otherwise match nearly every product name.
        assertEquals(0, NutritionRepository.relevanceScore("Tin of Beans", "Canned", "of a"));
    }

    @Test
    public void relevanceHandlesEmptyInput() {
        assertEquals(0, NutritionRepository.relevanceScore("Anything", "Any", null));
        assertEquals(0, NutritionRepository.relevanceScore("Anything", "Any", "  "));
        assertEquals(0, NutritionRepository.relevanceScore(null, null, "banana"));
    }

    @Test
    public void irrelevantHitsAreNotReturnedFromSearch() throws Exception {
        // Exactly the response shape that produced the bad result.
        String json = "{\"hits\": [{"
                + "\"product_name\": \"Blue Bird Chips\", \"categories\": \"Snacks\","
                + "\"nutriments\": {\"energy-kcal_100g\": 2250, \"proteins_100g\": 10,"
                + " \"carbohydrates_100g\": 50, \"fat_100g\": 30}}]}";

        assertNull(NutritionRepository.parseOpenFoodFactsSearch(new JSONObject(json), "banana"));
    }

    @Test
    public void implausibleHitsAreSkippedInFavourOfGoodOnes() throws Exception {
        String json = "{\"hits\": ["
                + " {\"product_name\": \"Banana pack\", \"categories\": \"Fruits\","
                + "  \"nutriments\": {\"energy-kcal_100g\": 2250, \"carbohydrates_100g\": 50}},"
                + " {\"product_name\": \"Banana\", \"categories\": \"Fruits\","
                + "  \"nutriments\": {\"energy-kcal_100g\": 89, \"proteins_100g\": 1.1,"
                + "   \"carbohydrates_100g\": 22.8, \"fat_100g\": 0.3, \"sugars_100g\": 12.2}}"
                + "]}";

        NutritionRepository.Resolution resolution =
                NutritionRepository.parseOpenFoodFactsSearch(new JSONObject(json), "banana");

        assertNotNull(resolution);
        assertEquals("Banana", resolution.foodName);
        assertEquals(89, resolution.nutrition.calories, 0.001);
    }

    @Test
    public void bestScoringRelevantHitWins() throws Exception {
        String json = "{\"hits\": ["
                + " {\"product_name\": \"Bread\", \"categories\": \"Banana breads\","
                + "  \"nutriments\": {\"energy-kcal_100g\": 300, \"carbohydrates_100g\": 50}},"
                + " {\"product_name\": \"Banana\", \"categories\": \"Fruits\","
                + "  \"nutriments\": {\"energy-kcal_100g\": 89, \"carbohydrates_100g\": 22.8}}"
                + "]}";

        NutritionRepository.Resolution resolution =
                NutritionRepository.parseOpenFoodFactsSearch(new JSONObject(json), "banana");

        // The hit naming banana directly beats the one that only matches via category.
        assertEquals("Banana", resolution.foodName);
    }

    @Test
    public void usdaHitsAreAlsoRelevanceAndSanityChecked() throws Exception {
        String irrelevant = "{\"foods\": [{\"description\": \"CANDY BAR\","
                + " \"foodCategory\": \"Sweets\", \"foodNutrients\": ["
                + " {\"nutrientNumber\": \"208\", \"value\": 500}]}]}";
        assertNull(NutritionRepository.parseUsdaSearch(new JSONObject(irrelevant), "banana"));

        String implausible = "{\"foods\": [{\"description\": \"BANANA\","
                + " \"foodCategory\": \"Fruits\", \"foodNutrients\": ["
                + " {\"nutrientNumber\": \"208\", \"value\": 5000}]}]}";
        assertNull(NutritionRepository.parseUsdaSearch(new JSONObject(implausible), "banana"));

        String good = "{\"foods\": [{\"description\": \"BANANA, RAW\","
                + " \"foodCategory\": \"Fruits\", \"foodNutrients\": ["
                + " {\"nutrientNumber\": \"208\", \"value\": 89},"
                + " {\"nutrientNumber\": \"205\", \"value\": 22.8}]}]}";
        assertNotNull(NutritionRepository.parseUsdaSearch(new JSONObject(good), "banana"));
    }
}
