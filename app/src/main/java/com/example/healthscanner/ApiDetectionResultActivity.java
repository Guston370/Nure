package com.example.healthscanner;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.example.healthscanner.database.ScanHistoryStore;
import com.example.healthscanner.models.Scan;
import com.google.android.material.button.MaterialButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * Displays the result of a photo food scan.
 *
 * <p>Renders a {@link Scan} that {@code VerticalScannerActivity} has already resolved and
 * persisted, showing nutrition, the shared health score, diet badges and where the numbers
 * came from. If the recognised food is wrong, the user can correct it and the nutrition is
 * re-resolved through {@link NutritionRepository}.</p>
 */
public class ApiDetectionResultActivity extends AppCompatActivity {

    private static final String TAG = "FoodRecognitionResult";

    // UI elements
    private ImageView capturedImageView;
    private TextView productNameText;
    private TextView confidenceText;
    private ProgressBar confidenceProgressBar;
    private LinearLayout nutritionContainer;
    private CardView nutritionCard;
    private LinearLayout ingredientsContainer;
    private CardView ingredientsCard;
    private LinearLayout healthTagsContainer;
    private CardView healthInfoCard;
    private TextView healthScoreText;
    private TextView servingSizeText;
    private TextView dietBadgeText;
    private LinearLayout allergensContainer;
    private CardView errorCard;
    private TextView errorText;
    private MaterialButton btnScanAnother;
    private MaterialButton btnGoHome;

    private String currentProduct;
    private String imagePath;

    /** The scan being displayed, as persisted by {@link ScanHistoryStore}. */
    private Scan currentScan;
    /** Other food names the recogniser considered, offered for correction. */
    private final List<String> alternatives = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_api_detection_result);
        SystemBarInsets.applyTopInset(this);

        initViews();
        setupButtons();
        loadResultData();
    }

    private void initViews() {
        capturedImageView = findViewById(R.id.capturedImageView);
        productNameText = findViewById(R.id.productNameText);
        confidenceText = findViewById(R.id.confidenceText);
        confidenceProgressBar = findViewById(R.id.confidenceProgressBar);
        nutritionContainer = findViewById(R.id.nutritionContainer);
        nutritionCard = findViewById(R.id.nutritionCard);
        ingredientsContainer = findViewById(R.id.ingredientsContainer);
        ingredientsCard = findViewById(R.id.ingredientsCard);
        healthInfoCard = findViewById(R.id.healthInfoCard);
        healthTagsContainer = findViewById(R.id.healthTagsContainer);
        healthScoreText = findViewById(R.id.healthScoreText);
        servingSizeText = findViewById(R.id.servingSizeText);
        dietBadgeText = findViewById(R.id.dietBadgeText);
        allergensContainer = findViewById(R.id.allergensContainer);
        errorCard = findViewById(R.id.errorCard);
        errorText = findViewById(R.id.errorText);
        btnScanAnother = findViewById(R.id.btnScanAnother);
        btnGoHome = findViewById(R.id.btnGoHome);

        // Back button
        ImageView btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
    }

    private void setupButtons() {
        if (btnScanAnother != null) {
            btnScanAnother.setOnClickListener(v -> finish());
        }

        // Recognition is probabilistic, so make correcting it a first-class action rather
        // than a hidden gesture.
        if (productNameText != null) {
            productNameText.setOnClickListener(v -> {
                if (currentScan != null) {
                    showCorrectionDialog();
                }
            });
            productNameText.setContentDescription("Tap to correct the recognised food name");
        }

        if (btnGoHome != null) {
            btnGoHome.setOnClickListener(v -> {
                android.content.Intent intent = new android.content.Intent(this, MainActivity.class);
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra("from_navigation", true);
                startActivity(intent);
                finish();
            });
        }
    }

    private void loadResultData() {
        android.content.Intent intent = getIntent();
        if (intent == null) {
            showError("No data received.");
            return;
        }

        // --- Load captured image ---
        imagePath = intent.getStringExtra("image_path");
        if (imagePath != null && !imagePath.isEmpty()) {
            File imageFile = new File(imagePath);
            if (imageFile.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
                if (bitmap != null && capturedImageView != null) {
                    capturedImageView.setImageBitmap(bitmap);
                }
            }
        }

        // --- Check for error ---
        String error = intent.getStringExtra("error");
        if (error != null && !error.isEmpty()) {
            showError(error);
            return;
        }

        // --- The resolved scan ---
        String scanJson = intent.getStringExtra("scan_json");
        if (scanJson != null && !scanJson.isEmpty()) {
            try {
                currentScan = Scan.fromJson(new JSONObject(scanJson));
            } catch (JSONException e) {
                Log.e(TAG, "Could not parse scan payload", e);
            }
        }

        if (currentScan == null) {
            showError("Could not read the scan result.");
            return;
        }

        // --- Product name ---
        currentProduct = currentScan.getProductName();
        if (productNameText != null) {
            productNameText.setText(FoodLabelMapper.toDisplayCase(currentProduct));
        }

        // --- Alternatives the recogniser also considered ---
        String alternativesJson = intent.getStringExtra("alternatives");
        if (alternativesJson != null && !alternativesJson.isEmpty()) {
            try {
                JSONArray arr = new JSONArray(alternativesJson);
                for (int i = 0; i < arr.length(); i++) {
                    String name = arr.optString(i, "").trim();
                    if (!name.isEmpty()) {
                        alternatives.add(name);
                    }
                }
            } catch (JSONException e) {
                Log.w(TAG, "Could not parse alternatives");
            }
        }

        // --- Confidence ---
        double confidence = intent.getDoubleExtra("confidence", -1);
        if (confidence >= 0) {
            int percent = (int) Math.round(confidence * 100);
            if (confidenceText != null) confidenceText.setText(percent + "%");
            if (confidenceProgressBar != null) {
                confidenceProgressBar.setProgress(percent);
                int barColor;
                if (percent >= 80) barColor = ContextCompat.getColor(this, R.color.health_excellent);
                else if (percent >= 50) barColor = ContextCompat.getColor(this, R.color.health_moderate);
                else barColor = ContextCompat.getColor(this, R.color.health_unhealthy);
                confidenceProgressBar.setProgressTintList(
                        android.content.res.ColorStateList.valueOf(barColor));
            }
        }

        // --- Serving size + data source, so the numbers are attributable ---
        String source = intent.getStringExtra("nutrition_source");
        if (servingSizeText != null) {
            String servingLine = "Per 100g";
            if (source != null && !source.isEmpty()) {
                servingLine += "  •  " + source;
            }
            servingSizeText.setText(servingLine);
            servingSizeText.setVisibility(View.VISIBLE);
        }

        // --- Health Score, from the one shared calculator ---
        renderHealthScore((int) Math.round(currentScan.getHealthScore()));

        // --- Diet Badge ---
        boolean isVeg = intent.getBooleanExtra("is_vegetarian", true);
        boolean isVegan = intent.getBooleanExtra("is_vegan", false);
        if (dietBadgeText != null) {
            if (isVegan) {
                dietBadgeText.setText("🟢 Vegan");
                dietBadgeText.setTextColor(ContextCompat.getColor(this, R.color.health_excellent));
            } else if (isVeg) {
                dietBadgeText.setText("🟢 Vegetarian");
                dietBadgeText.setTextColor(ContextCompat.getColor(this, R.color.health_excellent));
            } else {
                dietBadgeText.setText("🔴 Non-Vegetarian");
                dietBadgeText.setTextColor(ContextCompat.getColor(this, R.color.health_unhealthy));
            }
            dietBadgeText.setVisibility(View.VISIBLE);
        }

        // --- Ingredients, when the data source provided them ---
        String ingredients = intent.getStringExtra("ingredients");
        if (ingredients != null && !ingredients.trim().isEmpty()) {
            populateIngredients(splitIngredients(ingredients));
        }

        populateNutrition(buildNutritionJson(currentScan));
        populateHealthTags(buildHealthTags(currentScan));
    }

    /**
     * Build the nutrition rows from the scan. Replaces the previous contract, where the
     * scanner pre-formatted a JSON blob of display strings and passed it through the intent.
     */
    private JSONObject buildNutritionJson(Scan scan) {
        JSONObject nutrition = new JSONObject();
        try {
            nutrition.put("calories", scan.getCalories() + " kcal");
            nutrition.put("protein", format(scan.getProtein()) + "g");
            nutrition.put("total_fat", format(scan.getFat()) + "g");
            nutrition.put("carbohydrates", format(scan.getCarbs()) + "g");
            nutrition.put("fiber", format(scan.getFiber()) + "g");
            nutrition.put("sugar", format(scan.getSugar()) + "g");
            nutrition.put("sodium", String.format(Locale.getDefault(), "%.0fmg", scan.getSodium()));
        } catch (JSONException e) {
            Log.e(TAG, "Could not build nutrition rows", e);
        }
        return nutrition;
    }

    /**
     * Derive the health chips from the resolved nutrition rather than from a hardcoded list.
     */
    private JSONArray buildHealthTags(Scan scan) {
        JSONArray tags = new JSONArray();
        if (scan.getCalories() > 0 && scan.getCalories() < 100) {
            tags.put("Low Calorie");
        }
        if (scan.getFiber() >= 3) {
            tags.put("High Fibre");
        }
        if (scan.getProtein() >= 10) {
            tags.put("Good Protein Source");
        }
        if (scan.getSugar() > 15) {
            tags.put("High Sugar");
        }
        if (scan.getSodium() > 600) {
            tags.put("High Sodium");
        }
        if (scan.getFat() <= 3) {
            tags.put("Low Fat");
        }
        tags.put("Grade " + (scan.getHealthGrade() != null
                ? scan.getHealthGrade()
                : HealthScoreCalculator.gradeFor(scan.getHealthScore())));
        return tags;
    }

    /** Split a free-text ingredients string into a displayable list. */
    private JSONArray splitIngredients(String ingredients) {
        JSONArray list = new JSONArray();
        for (String part : ingredients.split("[,;]")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                list.put(trimmed);
            }
        }
        if (list.length() == 0) {
            list.put(ingredients.trim());
        }
        return list;
    }

    private String format(double value) {
        return String.format(Locale.getDefault(), "%.1f", value);
    }

    /**
     * Let the user fix a wrong recognition.
     *
     * <p>Shows the runners-up from the recogniser plus a free-text field, then re-resolves
     * nutrition for the corrected name and rewrites the stored scan. The previous version of
     * this screen posted corrections to the LAN-hosted RLHF endpoint to retrain a model that
     * no longer exists, and left the displayed data untouched.</p>
     */
    private void showCorrectionDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dpToPx(24), dpToPx(8), dpToPx(24), dpToPx(8));

        final EditText input = new EditText(this);
        input.setHint("Correct food name");
        if (currentProduct != null) {
            input.setText(currentProduct);
        }
        layout.addView(input);

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle("Not quite right?")
                .setView(layout)
                .setPositiveButton("Look up", (dialog, which) -> {
                    String corrected = input.getText().toString().trim();
                    if (!corrected.isEmpty()) {
                        reresolve(corrected);
                    }
                })
                .setNegativeButton("Cancel", null);

        if (!alternatives.isEmpty()) {
            TextView label = new TextView(this);
            label.setText("Did you mean:");
            label.setTextSize(13);
            label.setTypeface(null, Typeface.BOLD);
            label.setPadding(0, dpToPx(12), 0, dpToPx(4));
            layout.addView(label);

            ListView listView = new ListView(this);
            List<String> display = new ArrayList<>();
            for (String alternative : alternatives) {
                display.add(FoodLabelMapper.toDisplayCase(alternative));
            }
            listView.setAdapter(new ArrayAdapter<>(this,
                    android.R.layout.simple_list_item_1, display));
            listView.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(160)));
            layout.addView(listView);

            AlertDialog dialog = builder.create();
            listView.setOnItemClickListener((parent, view, position, id) -> {
                dialog.dismiss();
                reresolve(alternatives.get(position));
            });
            dialog.show();
        } else {
            builder.show();
        }
    }

    /**
     * Re-run the nutrition lookup for a corrected food name and refresh the screen.
     */
    private void reresolve(String foodName) {
        Toast.makeText(this, "Looking up " + foodName + "...", Toast.LENGTH_SHORT).show();

        NutritionRepository.getInstance(this).resolve(foodName, resolution -> {
            if (!resolution.hasNutrition()) {
                Toast.makeText(this, "No nutrition data found for " + foodName,
                        Toast.LENGTH_LONG).show();
                return;
            }

            double healthScore = resolution.healthScore();
            currentScan.setProductName(resolution.foodName);
            currentScan.setBrand(resolution.brand);
            currentScan.setCategory(resolution.category != null ? resolution.category : "Photo scan");
            currentScan.setHealthScore(healthScore);
            currentScan.setHealthGrade(HealthScoreCalculator.gradeFor(healthScore));
            currentScan.setCalories((int) Math.round(resolution.nutrition.calories));
            currentScan.setProtein(resolution.nutrition.protein);
            currentScan.setCarbs(resolution.nutrition.carbs);
            currentScan.setFat(resolution.nutrition.fat);
            currentScan.setSugar(resolution.nutrition.sugar);
            currentScan.setFiber(resolution.nutrition.fiber);
            currentScan.setSodium(resolution.nutrition.sodium);
            currentScan.setScanMethod("photo_corrected");

            // Overwrite the history entry rather than adding a second one for the same photo.
            ScanHistoryStore.getInstance(this).addScan(currentScan);

            // Rebuild the dynamic sections in place.
            if (nutritionContainer != null) nutritionContainer.removeAllViews();
            if (healthTagsContainer != null) healthTagsContainer.removeAllViews();
            if (ingredientsContainer != null) ingredientsContainer.removeAllViews();

            currentProduct = resolution.foodName;
            if (productNameText != null) {
                productNameText.setText(FoodLabelMapper.toDisplayCase(currentProduct));
            }
            if (servingSizeText != null) {
                servingSizeText.setText("Per 100g  •  " + resolution.source.displayName);
            }
            renderHealthScore((int) Math.round(healthScore));

            NutritionRepository.DietInfo diet =
                    NutritionRepository.classifyDiet(resolution.foodName, resolution.ingredients);
            if (dietBadgeText != null) {
                if (diet.vegan) {
                    dietBadgeText.setText("🟢 Vegan");
                    dietBadgeText.setTextColor(ContextCompat.getColor(this, R.color.health_excellent));
                } else if (diet.vegetarian) {
                    dietBadgeText.setText("🟢 Vegetarian");
                    dietBadgeText.setTextColor(ContextCompat.getColor(this, R.color.health_excellent));
                } else {
                    dietBadgeText.setText("🔴 Non-Vegetarian");
                    dietBadgeText.setTextColor(ContextCompat.getColor(this, R.color.health_unhealthy));
                }
            }

            if (resolution.ingredients != null && !resolution.ingredients.trim().isEmpty()) {
                populateIngredients(splitIngredients(resolution.ingredients));
            }
            populateNutrition(buildNutritionJson(currentScan));
            populateHealthTags(buildHealthTags(currentScan));

            Toast.makeText(this, "Updated to " + resolution.foodName, Toast.LENGTH_SHORT).show();
        });
    }

    /** Apply a score to the score view, colour-coded by band. */
    private void renderHealthScore(int healthScore) {
        if (healthScoreText == null) {
            return;
        }
        healthScoreText.setText(String.valueOf(healthScore));
        int scoreColor;
        if (healthScore >= 75) scoreColor = ContextCompat.getColor(this, R.color.health_excellent);
        else if (healthScore >= 50) scoreColor = ContextCompat.getColor(this, R.color.health_moderate);
        else scoreColor = ContextCompat.getColor(this, R.color.health_unhealthy);
        healthScoreText.setTextColor(scoreColor);
    }

    /**
     * Populate the ingredients list dynamically.
     */
    private void populateIngredients(JSONArray ingredients) {
        if (ingredientsCard != null) ingredientsCard.setVisibility(View.VISIBLE);
        if (ingredientsContainer == null) return;

        for (int i = 0; i < ingredients.length(); i++) {
            String ingredient = ingredients.optString(i, "");
            if (ingredient.isEmpty()) continue;

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dpToPx(6), 0, dpToPx(6));

            // Bullet
            TextView bullet = new TextView(this);
            bullet.setText("•");
            bullet.setTextSize(18);
            bullet.setTextColor(ContextCompat.getColor(this, R.color.primary_teal));
            bullet.setPadding(0, 0, dpToPx(12), 0);

            // Ingredient name
            TextView nameTv = new TextView(this);
            nameTv.setText(ingredient);
            nameTv.setTextSize(15);
            nameTv.setTextColor(ContextCompat.getColor(this, R.color.text_primary_light));

            row.addView(bullet);
            row.addView(nameTv);
            ingredientsContainer.addView(row);
        }
    }

    /**
     * Populate health tags as chips/badges.
     */
    private void populateHealthTags(JSONArray tags) {
        if (healthInfoCard != null) healthInfoCard.setVisibility(View.VISIBLE);
        if (healthTagsContainer == null) return;

        for (int i = 0; i < tags.length(); i++) {
            String tag = tags.optString(i, "");
            if (tag.isEmpty()) continue;

            TextView chip = new TextView(this);
            chip.setText(tag);
            chip.setTextSize(12);
            chip.setTextColor(ContextCompat.getColor(this, R.color.primary_teal));
            chip.setBackgroundResource(R.drawable.bg_stat_chip);
            chip.setPadding(dpToPx(12), dpToPx(6), dpToPx(12), dpToPx(6));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, dpToPx(8), dpToPx(8));
            chip.setLayoutParams(params);

            healthTagsContainer.addView(chip);
        }
    }

    /**
     * Populate allergen warnings.
     */
    private void populateAllergens(JSONArray allergens) {
        if (allergens.length() == 0 || allergensContainer == null) return;

        allergensContainer.setVisibility(View.VISIBLE);

        for (int i = 0; i < allergens.length(); i++) {
            String allergen = allergens.optString(i, "");
            if (allergen.isEmpty()) continue;

            TextView tv = new TextView(this);
            tv.setText("⚠️ " + allergen);
            tv.setTextSize(14);
            tv.setTextColor(ContextCompat.getColor(this, R.color.warning_orange));
            tv.setTypeface(null, Typeface.BOLD);
            tv.setPadding(0, dpToPx(4), 0, dpToPx(4));

            allergensContainer.addView(tv);
        }
    }

    /**
     * Dynamically builds nutrition rows from the JSON object.
     */
    private void populateNutrition(JSONObject nutrition) {
        if (nutrition.length() == 0) return;

        if (nutritionCard != null) nutritionCard.setVisibility(View.VISIBLE);

        // Priority order for nutrition display
        String[] priorityKeys = {"calories", "protein", "total_fat", "carbohydrates",
                "fiber", "sugar", "sodium", "cholesterol", "saturated_fat"};

        for (String key : priorityKeys) {
            if (nutrition.has(key)) {
                addNutritionRow(prettifyKey(key), nutrition.optString(key, "—"), key);
            }
        }

        // Add any remaining keys
        Iterator<String> keys = nutrition.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            boolean alreadyAdded = false;
            for (String pk : priorityKeys) {
                if (pk.equals(key)) { alreadyAdded = true; break; }
            }
            if (!alreadyAdded) {
                addNutritionRow(prettifyKey(key), nutrition.optString(key, "—"), key);
            }
        }
    }

    private void addNutritionRow(String label, String value, String key) {
        if (nutritionContainer == null) return;

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dpToPx(10), 0, dpToPx(10));

        // Icon indicator based on nutrient type
        TextView icon = new TextView(this);
        String emoji = getNutrientEmoji(key);
        icon.setText(emoji);
        icon.setTextSize(16);
        icon.setPadding(0, 0, dpToPx(10), 0);

        // Label
        TextView labelTv = new TextView(this);
        labelTv.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        labelTv.setText(label);
        labelTv.setTextSize(15);
        labelTv.setTextColor(ContextCompat.getColor(this, R.color.text_secondary_light));

        // Value
        TextView valueTv = new TextView(this);
        valueTv.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        valueTv.setText(value);
        valueTv.setTextSize(16);
        valueTv.setTextColor(ContextCompat.getColor(this, R.color.text_primary_light));
        valueTv.setTypeface(null, Typeface.BOLD);

        row.addView(icon);
        row.addView(labelTv);
        row.addView(valueTv);
        nutritionContainer.addView(row);

        // Divider
        View divider = new View(this);
        divider.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(1)));
        divider.setBackgroundColor(ContextCompat.getColor(this, R.color.divider_light));
        nutritionContainer.addView(divider);
    }

    private String getNutrientEmoji(String key) {
        switch (key) {
            case "calories": return "🔥";
            case "protein": return "💪";
            case "total_fat": case "saturated_fat": return "🧈";
            case "carbohydrates": return "🌾";
            case "fiber": return "🥬";
            case "sugar": return "🍬";
            case "sodium": return "🧂";
            case "cholesterol": return "💊";
            default: return "📊";
        }
    }

    private String prettifyKey(String key) {
        String result = key.replace("_", " ");
        StringBuilder sb = new StringBuilder();
        for (String word : result.split(" ")) {
            if (word.length() > 0) {
                sb.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1)).append(" ");
            }
        }
        return sb.toString().trim();
    }

    private void showError(String message) {
        if (errorCard != null) errorCard.setVisibility(View.VISIBLE);
        if (errorText != null) errorText.setText(message);
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }
}
