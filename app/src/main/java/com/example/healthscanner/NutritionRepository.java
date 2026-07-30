package com.example.healthscanner;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Resolves a food name into nutrition facts.
 *
 * <p>This is the single nutrition layer both scan paths use. The barcode path resolves by
 * barcode against OpenFoodFacts; the photo path resolves by name through the tiers below.
 * Both then score the result with {@link HealthScoreCalculator} and store it as a
 * {@link com.example.healthscanner.models.Scan}.</p>
 *
 * <p>Tiers, tried in order:</p>
 * <ol>
 * <li><b>Local CSV</b> — instant and offline, covers common whole foods.</li>
 * <li><b>USDA FoodData Central</b> — best source for unbranded foods, but only used when an
 * API key has been configured in {@link ApiConfig}.</li>
 * <li><b>OpenFoodFacts text search</b> — no key required, strongest for packaged goods.</li>
 * </ol>
 *
 * <p>Replaces the previous approach, where the photo path read a single hand-seeded Firestore
 * document per food and derived the health score from substring checks on the label.</p>
 */
public class NutritionRepository {

    private static final String TAG = "NutritionRepository";

    /** OpenFoodFacts asks that clients identify themselves. */
    private static final String USER_AGENT = "Nure/1.0 (Android; health scanner)";

    /** Free-text product search. The v0/v2 endpoints have no full-text parameter. */
    private static final String OFF_SEARCH_URL = "https://search.openfoodfacts.org/search";

    private static final int SEARCH_PAGE_SIZE = 5;
    private static final int TIMEOUT_SECONDS = 15;

    private static NutritionRepository instance;

    private final Context context;
    private final OkHttpClient httpClient;
    private final ExecutorService executor;
    private final Handler mainHandler;

    /** Parsed once and reused; the CSV is small and never changes at runtime. */
    private volatile LocalNutritionTable localTable;

    private NutritionRepository(Context context) {
        this.context = context.getApplicationContext();
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(TIMEOUT_SECONDS, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(TIMEOUT_SECONDS, java.util.concurrent.TimeUnit.SECONDS)
                .build();
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public static synchronized NutritionRepository getInstance(Context context) {
        if (instance == null) {
            instance = new NutritionRepository(context);
        }
        return instance;
    }

    /** Where a resolution came from, surfaced in the UI so numbers are attributable. */
    public enum Source {
        LOCAL_DATASET("Nure offline dataset"),
        USDA("USDA FoodData Central"),
        OPEN_FOOD_FACTS("Open Food Facts"),
        NONE("No nutrition data found");

        public final String displayName;

        Source(String displayName) {
            this.displayName = displayName;
        }
    }

    /**
     * A resolved food: what it is, its nutrition per 100g, and where the numbers came from.
     */
    public static class Resolution {
        public final String foodName;
        public final String brand;
        public final String category;
        public final String imageUrl;
        public final String ingredients;
        public final HealthScoreCalculator.Nutrition nutrition;
        public final Source source;

        public Resolution(String foodName, String brand, String category, String imageUrl,
                String ingredients, HealthScoreCalculator.Nutrition nutrition, Source source) {
            this.foodName = foodName;
            this.brand = brand;
            this.category = category;
            this.imageUrl = imageUrl;
            this.ingredients = ingredients;
            this.nutrition = nutrition;
            this.source = source;
        }

        public boolean hasNutrition() {
            return nutrition != null && source != Source.NONE;
        }

        /** The score every screen shows, computed by the one shared calculator. */
        public double healthScore() {
            return HealthScoreCalculator.calculate(nutrition);
        }

        static Resolution notFound(String foodName) {
            return new Resolution(foodName, null, null, null, null,
                    new HealthScoreCalculator.Nutrition(), Source.NONE);
        }
    }

    public interface Callback {
        void onResolved(Resolution resolution);
    }

    /**
     * Resolve a food name off the main thread; the callback runs on the main thread.
     */
    public void resolve(String foodName, Callback callback) {
        if (foodName == null || foodName.trim().isEmpty()) {
            callback.onResolved(Resolution.notFound(""));
            return;
        }

        final String query = foodName.trim();
        executor.execute(() -> {
            Resolution resolution = resolveBlocking(query);
            mainHandler.post(() -> callback.onResolved(resolution));
        });
    }

    /**
     * Synchronous resolution. Package-visible so it can be driven directly from a worker.
     */
    Resolution resolveBlocking(String foodName) {
        // Tier 1: offline dataset.
        LocalNutritionTable table = getLocalTable();
        HealthScoreCalculator.Nutrition local = table.lookup(foodName);
        if (local != null) {
            String matched = table.matchedLabel(foodName);
            Log.d(TAG, "Resolved '" + foodName + "' from local dataset as '" + matched + "'");
            return new Resolution(
                    FoodLabelMapper.toDisplayCase(matched != null ? matched : foodName),
                    null,
                    "Whole foods",
                    null,
                    FoodLabelMapper.toDisplayCase(matched != null ? matched : foodName),
                    local,
                    Source.LOCAL_DATASET);
        }

        // Tier 2: USDA, when a key is configured.
        if (ApiConfig.isUSDAConfigured()) {
            Resolution usda = lookupUsda(foodName);
            if (usda != null) {
                Log.d(TAG, "Resolved '" + foodName + "' from USDA");
                return usda;
            }
        }

        // Tier 3: OpenFoodFacts free-text search.
        Resolution off = lookupOpenFoodFacts(foodName);
        if (off != null) {
            Log.d(TAG, "Resolved '" + foodName + "' from Open Food Facts");
            return off;
        }

        Log.w(TAG, "No nutrition found for '" + foodName + "'");
        return Resolution.notFound(FoodLabelMapper.toDisplayCase(foodName));
    }

    private LocalNutritionTable getLocalTable() {
        LocalNutritionTable table = localTable;
        if (table != null) {
            return table;
        }
        synchronized (this) {
            if (localTable == null) {
                try (InputStreamReader reader = new InputStreamReader(
                        context.getAssets().open("nutrition_dataset.csv"), StandardCharsets.UTF_8)) {
                    localTable = LocalNutritionTable.fromCsv(reader);
                    Log.d(TAG, "Loaded " + localTable.size() + " local nutrition rows");
                } catch (IOException e) {
                    Log.e(TAG, "Could not read nutrition_dataset.csv: " + e.getMessage(), e);
                    localTable = LocalNutritionTable.empty();
                }
            }
            return localTable;
        }
    }

    private Resolution lookupOpenFoodFacts(String foodName) {
        String url = OFF_SEARCH_URL
                + "?q=" + encode(foodName)
                + "&page_size=" + SEARCH_PAGE_SIZE
                + "&fields=product_name,brands,categories,nutriments,image_front_url,ingredients_text";

        String body = get(url);
        if (body == null) {
            return null;
        }

        try {
            return parseOpenFoodFactsSearch(new JSONObject(body), foodName);
        } catch (Exception e) {
            Log.w(TAG, "Could not parse Open Food Facts response: " + e.getMessage());
            return null;
        }
    }

    /**
     * Pick the first search hit that actually carries an energy value.
     *
     * <p>Static and pure so the parsing (including the grams-to-milligrams sodium
     * conversion) is unit testable.</p>
     *
     * @return a resolution, or {@code null} when no hit has usable nutrition
     */
    static Resolution parseOpenFoodFactsSearch(JSONObject root, String queriedName) {
        if (root == null) {
            return null;
        }

        // The search service returns "hits"; the classic endpoints return "products".
        JSONArray hits = root.optJSONArray("hits");
        if (hits == null) {
            hits = root.optJSONArray("products");
        }
        if (hits == null) {
            return null;
        }

        for (int i = 0; i < hits.length(); i++) {
            JSONObject product = hits.optJSONObject(i);
            if (product == null) {
                continue;
            }

            JSONObject nutriments = product.optJSONObject("nutriments");
            if (nutriments == null) {
                continue;
            }

            double calories = nutriments.optDouble("energy-kcal_100g", 0);
            if (calories <= 0) {
                continue; // Nothing useful without an energy value.
            }

            HealthScoreCalculator.Nutrition nutrition = new HealthScoreCalculator.Nutrition();
            nutrition.calories = calories;
            nutrition.protein = nutriments.optDouble("proteins_100g", 0);
            nutrition.sugar = nutriments.optDouble("sugars_100g", 0);
            nutrition.fat = nutriments.optDouble("fat_100g", 0);
            nutrition.carbs = nutriments.optDouble("carbohydrates_100g", 0);
            nutrition.fiber = nutriments.optDouble("fiber_100g", 0);
            // Open Food Facts reports sodium in grams; the score model expects milligrams.
            nutrition.sodium = nutriments.optDouble("sodium_100g", 0) * 1000;

            String name = product.optString("product_name", "").trim();
            if (name.isEmpty()) {
                name = FoodLabelMapper.toDisplayCase(queriedName);
            }

            return new Resolution(
                    name,
                    emptyToNull(product.optString("brands", "")),
                    firstCategory(product.optString("categories", "")),
                    emptyToNull(product.optString("image_front_url", "")),
                    emptyToNull(product.optString("ingredients_text", "")),
                    nutrition,
                    Source.OPEN_FOOD_FACTS);
        }

        return null;
    }

    private Resolution lookupUsda(String foodName) {
        String url = ApiConfig.USDA_BASE_URL
                + "?api_key=" + encode(ApiConfig.USDA_API_KEY)
                + "&query=" + encode(foodName)
                + "&pageSize=" + SEARCH_PAGE_SIZE
                + "&dataType=Foundation,SR%20Legacy";

        String body = get(url);
        if (body == null) {
            return null;
        }

        try {
            return parseUsdaSearch(new JSONObject(body), foodName);
        } catch (Exception e) {
            Log.w(TAG, "Could not parse USDA response: " + e.getMessage());
            return null;
        }
    }

    /**
     * Map a USDA FoodData Central search response onto our nutrition model.
     *
     * <p>USDA returns a flat {@code foodNutrients} array keyed by nutrient number rather than
     * named fields, so nutrients are matched on the standard numbers: 208 energy, 203
     * protein, 204 fat, 205 carbohydrate, 269 total sugars, 291 fibre, 307 sodium (mg).</p>
     */
    static Resolution parseUsdaSearch(JSONObject root, String queriedName) {
        if (root == null) {
            return null;
        }

        JSONArray foods = root.optJSONArray("foods");
        if (foods == null) {
            return null;
        }

        for (int i = 0; i < foods.length(); i++) {
            JSONObject food = foods.optJSONObject(i);
            if (food == null) {
                continue;
            }

            JSONArray nutrients = food.optJSONArray("foodNutrients");
            if (nutrients == null) {
                continue;
            }

            HealthScoreCalculator.Nutrition nutrition = new HealthScoreCalculator.Nutrition();
            boolean hasEnergy = false;

            for (int n = 0; n < nutrients.length(); n++) {
                JSONObject nutrient = nutrients.optJSONObject(n);
                if (nutrient == null) {
                    continue;
                }
                String number = nutrient.optString("nutrientNumber",
                        nutrient.optString("number", ""));
                double value = nutrient.optDouble("value", 0);

                switch (number) {
                    case "208":
                        nutrition.calories = value;
                        hasEnergy = value > 0;
                        break;
                    case "203":
                        nutrition.protein = value;
                        break;
                    case "204":
                        nutrition.fat = value;
                        break;
                    case "205":
                        nutrition.carbs = value;
                        break;
                    case "269":
                        nutrition.sugar = value;
                        break;
                    case "291":
                        nutrition.fiber = value;
                        break;
                    case "307":
                        nutrition.sodium = value; // already milligrams
                        break;
                    default:
                        break;
                }
            }

            if (!hasEnergy) {
                continue;
            }

            String description = food.optString("description", "").trim();
            return new Resolution(
                    !description.isEmpty()
                            ? FoodLabelMapper.toDisplayCase(description)
                            : FoodLabelMapper.toDisplayCase(queriedName),
                    emptyToNull(food.optString("brandOwner", "")),
                    emptyToNull(food.optString("foodCategory", "")),
                    null,
                    null,
                    nutrition,
                    Source.USDA);
        }

        return null;
    }

    /** GET a URL, returning the body or {@code null} on any failure. */
    private String get(String url) {
        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                Log.w(TAG, "Request failed (" + response.code() + "): " + url);
                return null;
            }
            return response.body().string();
        } catch (Exception e) {
            Log.w(TAG, "Request error for " + url + ": " + e.getMessage());
            return null;
        }
    }

    private static String encode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (Exception e) {
            return value.replace(' ', '+');
        }
    }

    /** OpenFoodFacts categories are a comma separated hierarchy; take the most specific. */
    private static String firstCategory(String categories) {
        if (categories == null || categories.trim().isEmpty()) {
            return null;
        }
        String[] parts = categories.split(",");
        String candidate = parts[parts.length - 1].trim();
        int colon = candidate.indexOf(':');
        if (colon >= 0 && colon < candidate.length() - 1) {
            candidate = candidate.substring(colon + 1);
        }
        candidate = candidate.replace('-', ' ').trim();
        return candidate.isEmpty() ? null : candidate;
    }

    private static String emptyToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() || "null".equalsIgnoreCase(trimmed) ? null : trimmed;
    }

    /**
     * Dietary classification from the food name and ingredients.
     *
     * <p>Kept as an explicit, testable helper rather than the inline substring chain the
     * scanner used to carry.</p>
     */
    public static class DietInfo {
        public final boolean vegetarian;
        public final boolean vegan;

        DietInfo(boolean vegetarian, boolean vegan) {
            this.vegetarian = vegetarian;
            this.vegan = vegan;
        }
    }

    private static final String[] NON_VEGETARIAN_TERMS = {
            "chicken", "beef", "pork", "mutton", "lamb", "bacon", "ham", "sausage",
            "fish", "tuna", "salmon", "prawn", "shrimp", "crab", "lobster", "anchovy",
            "meat", "steak", "gelatin", "egg", "omelette", "seafood", "shellfish"
    };

    private static final String[] NON_VEGAN_TERMS = {
            "milk", "butter", "ghee", "cheese", "cream", "yogurt", "yoghurt", "curd",
            "paneer", "lassi", "kheer", "honey", "whey", "casein", "custard", "ice cream"
    };

    /**
     * Classify a food as vegetarian/vegan. Absence of evidence is treated as vegetarian,
     * which matches the previous behaviour, so the badge stays conservative rather than
     * claiming a food is vegan when we simply have no ingredient list.
     */
    public static DietInfo classifyDiet(String foodName, String ingredients) {
        String haystack = ((foodName == null ? "" : foodName) + " "
                + (ingredients == null ? "" : ingredients)).toLowerCase(Locale.US);

        for (String term : NON_VEGETARIAN_TERMS) {
            if (haystack.contains(term)) {
                return new DietInfo(false, false);
            }
        }
        for (String term : NON_VEGAN_TERMS) {
            if (haystack.contains(term)) {
                return new DietInfo(true, false);
            }
        }
        return new DietInfo(true, true);
    }
}
