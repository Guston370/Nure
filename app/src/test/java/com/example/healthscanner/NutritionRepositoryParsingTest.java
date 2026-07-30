package com.example.healthscanner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.json.JSONObject;
import org.junit.Test;

/**
 * Unit tests for the online nutrition tiers: response parsing, unit conversion and the
 * vegetarian/vegan classification.
 */
public class NutritionRepositoryParsingTest {

    private static final double DELTA = 0.001;

    /** Trimmed shape of a real search.openfoodfacts.org response. */
    private static final String OFF_RESPONSE = "{"
            + "\"count\": 2,"
            + "\"hits\": ["
            + "  {\"product_name\": \"Classic American Pepperoni Pizza\","
            + "   \"brands\": \"Dr. Oetker\","
            + "   \"categories\": \"Meals, Pizzas and pies, Pizzas\","
            + "   \"image_front_url\": \"https://images.test/pizza.jpg\","
            + "   \"ingredients_text\": \"Wheat flour, tomato, mozzarella, pepperoni\","
            + "   \"nutriments\": {\"energy-kcal_100g\": 256, \"proteins_100g\": 12,"
            + "     \"sugars_100g\": 3.52, \"fat_100g\": 8.56, \"carbohydrates_100g\": 30.1,"
            + "     \"fiber_100g\": 2.32, \"sodium_100g\": 0.904}}"
            + "]}";

    @Test
    public void openFoodFactsHitIsMappedOntoTheNutritionModel() {
        NutritionRepository.Resolution resolution = NutritionRepository.parseOpenFoodFactsSearch(
                new JSONObject(OFF_RESPONSE), "pizza");

        assertNotNull(resolution);
        assertEquals("Classic American Pepperoni Pizza", resolution.foodName);
        assertEquals("Dr. Oetker", resolution.brand);
        // Only the most specific category is kept.
        assertEquals("Pizzas", resolution.category);
        assertEquals("https://images.test/pizza.jpg", resolution.imageUrl);
        assertEquals(NutritionRepository.Source.OPEN_FOOD_FACTS, resolution.source);
        assertTrue(resolution.hasNutrition());

        assertEquals(256, resolution.nutrition.calories, DELTA);
        assertEquals(12, resolution.nutrition.protein, DELTA);
        assertEquals(3.52, resolution.nutrition.sugar, DELTA);
        assertEquals(8.56, resolution.nutrition.fat, DELTA);
        assertEquals(30.1, resolution.nutrition.carbs, DELTA);
        assertEquals(2.32, resolution.nutrition.fiber, DELTA);
    }

    @Test
    public void sodiumIsConvertedFromGramsToMilligrams() {
        // Open Food Facts reports sodium in grams; the score model works in milligrams.
        NutritionRepository.Resolution resolution = NutritionRepository.parseOpenFoodFactsSearch(
                new JSONObject(OFF_RESPONSE), "pizza");

        assertEquals(904, resolution.nutrition.sodium, DELTA);
    }

    @Test
    public void hitsWithoutAnEnergyValueAreSkipped() {
        String json = "{\"hits\": ["
                + " {\"product_name\": \"No data\", \"nutriments\": {\"proteins_100g\": 5}},"
                + " {\"product_name\": \"Has data\", \"nutriments\": {\"energy-kcal_100g\": 120}}"
                + "]}";

        NutritionRepository.Resolution resolution =
                NutritionRepository.parseOpenFoodFactsSearch(new JSONObject(json), "thing");

        assertNotNull(resolution);
        assertEquals("Has data", resolution.foodName);
    }

    @Test
    public void theLegacyProductsArrayIsAlsoAccepted() {
        String json = "{\"products\": [{\"product_name\": \"Legacy\","
                + " \"nutriments\": {\"energy-kcal_100g\": 200}}]}";

        NutritionRepository.Resolution resolution =
                NutritionRepository.parseOpenFoodFactsSearch(new JSONObject(json), "thing");

        assertNotNull(resolution);
        assertEquals("Legacy", resolution.foodName);
    }

    @Test
    public void missingProductNameFallsBackToTheQuery() {
        String json = "{\"hits\": [{\"nutriments\": {\"energy-kcal_100g\": 200}}]}";

        NutritionRepository.Resolution resolution =
                NutritionRepository.parseOpenFoodFactsSearch(new JSONObject(json), "french fries");

        assertEquals("French Fries", resolution.foodName);
    }

    @Test
    public void emptyOrUnusableResponsesReturnNull() {
        assertNull(NutritionRepository.parseOpenFoodFactsSearch(null, "x"));
        assertNull(NutritionRepository.parseOpenFoodFactsSearch(new JSONObject("{}"), "x"));
        assertNull(NutritionRepository.parseOpenFoodFactsSearch(
                new JSONObject("{\"hits\": []}"), "x"));
        assertNull(NutritionRepository.parseOpenFoodFactsSearch(
                new JSONObject("{\"hits\": [{\"product_name\": \"No nutriments\"}]}"), "x"));
    }

    @Test
    public void usdaNutrientNumbersAreMappedToTheRightFields() {
        String json = "{\"foods\": [{"
                + "\"description\": \"APPLE, RAW\","
                + "\"foodCategory\": \"Fruits\","
                + "\"foodNutrients\": ["
                + " {\"nutrientNumber\": \"208\", \"value\": 52},"
                + " {\"nutrientNumber\": \"203\", \"value\": 0.26},"
                + " {\"nutrientNumber\": \"204\", \"value\": 0.17},"
                + " {\"nutrientNumber\": \"205\", \"value\": 13.8},"
                + " {\"nutrientNumber\": \"269\", \"value\": 10.4},"
                + " {\"nutrientNumber\": \"291\", \"value\": 2.4},"
                + " {\"nutrientNumber\": \"307\", \"value\": 1}"
                + "]}]}";

        NutritionRepository.Resolution resolution =
                NutritionRepository.parseUsdaSearch(new JSONObject(json), "apple");

        assertNotNull(resolution);
        assertEquals(NutritionRepository.Source.USDA, resolution.source);
        assertEquals("Apple, Raw", resolution.foodName);
        assertEquals("Fruits", resolution.category);
        assertEquals(52, resolution.nutrition.calories, DELTA);
        assertEquals(0.26, resolution.nutrition.protein, DELTA);
        assertEquals(0.17, resolution.nutrition.fat, DELTA);
        assertEquals(13.8, resolution.nutrition.carbs, DELTA);
        assertEquals(10.4, resolution.nutrition.sugar, DELTA);
        assertEquals(2.4, resolution.nutrition.fiber, DELTA);
        // USDA already reports sodium in milligrams, so no conversion.
        assertEquals(1, resolution.nutrition.sodium, DELTA);
    }

    @Test
    public void usdaFoodsWithoutEnergyAreSkipped() {
        String json = "{\"foods\": ["
                + " {\"description\": \"No energy\", \"foodNutrients\": ["
                + "   {\"nutrientNumber\": \"203\", \"value\": 5}]},"
                + " {\"description\": \"Good\", \"foodNutrients\": ["
                + "   {\"nutrientNumber\": \"208\", \"value\": 100}]}"
                + "]}";

        NutritionRepository.Resolution resolution =
                NutritionRepository.parseUsdaSearch(new JSONObject(json), "x");

        assertEquals("Good", resolution.foodName);
    }

    @Test
    public void usdaLegacyNumberFieldIsAlsoRead() {
        String json = "{\"foods\": [{\"description\": \"Legacy\", \"foodNutrients\": ["
                + " {\"number\": \"208\", \"value\": 90}]}]}";

        NutritionRepository.Resolution resolution =
                NutritionRepository.parseUsdaSearch(new JSONObject(json), "x");

        assertNotNull(resolution);
        assertEquals(90, resolution.nutrition.calories, DELTA);
    }

    @Test
    public void emptyUsdaResponsesReturnNull() {
        assertNull(NutritionRepository.parseUsdaSearch(null, "x"));
        assertNull(NutritionRepository.parseUsdaSearch(new JSONObject("{}"), "x"));
        assertNull(NutritionRepository.parseUsdaSearch(new JSONObject("{\"foods\": []}"), "x"));
    }

    @Test
    public void resolvedNutritionFeedsTheSharedHealthScore() {
        NutritionRepository.Resolution resolution = NutritionRepository.parseOpenFoodFactsSearch(
                new JSONObject(OFF_RESPONSE), "pizza");

        // Same calculator the barcode path uses; verify the wiring, not the exact number.
        assertEquals(HealthScoreCalculator.calculate(resolution.nutrition),
                resolution.healthScore(), DELTA);
    }

    @Test
    public void meatAndFishAreClassifiedNonVegetarian() {
        assertFalse(NutritionRepository.classifyDiet("butter chicken", null).vegetarian);
        assertFalse(NutritionRepository.classifyDiet("Fish Curry", null).vegetarian);
        assertFalse(NutritionRepository.classifyDiet("cake", "flour, gelatin, sugar").vegetarian);
        assertFalse(NutritionRepository.classifyDiet("omelette", null).vegetarian);
    }

    @Test
    public void dairyIsVegetarianButNotVegan() {
        NutritionRepository.DietInfo paneer = NutritionRepository.classifyDiet("paneer tikka", null);

        assertTrue(paneer.vegetarian);
        assertFalse(paneer.vegan);
    }

    @Test
    public void dairyInIngredientsIsDetectedEvenWhenTheNameIsNeutral() {
        NutritionRepository.DietInfo resolution =
                NutritionRepository.classifyDiet("pizza", "wheat flour, tomato, mozzarella cheese");

        assertTrue(resolution.vegetarian);
        assertFalse(resolution.vegan);
    }

    @Test
    public void plantFoodsAreVegan() {
        NutritionRepository.DietInfo apple = NutritionRepository.classifyDiet("apple", "apple");

        assertTrue(apple.vegetarian);
        assertTrue(apple.vegan);
    }

    @Test
    public void unknownFoodsDefaultToVegetarian() {
        // No evidence either way: stay conservative rather than guessing non-vegetarian.
        NutritionRepository.DietInfo unknown = NutritionRepository.classifyDiet(null, null);

        assertTrue(unknown.vegetarian);
    }

    @Test
    public void notFoundResolutionsReportNoNutrition() {
        NutritionRepository.Resolution notFound =
                NutritionRepository.Resolution.notFound("mystery dish");

        assertFalse(notFound.hasNutrition());
        assertEquals(NutritionRepository.Source.NONE, notFound.source);
    }
}
