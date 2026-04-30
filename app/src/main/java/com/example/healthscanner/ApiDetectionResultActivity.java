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

import com.google.android.material.button.MaterialButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Displays the result of Food Recognition via ML model.
 * Shows: Food name, confidence, ingredients, nutrition, health score,
 * allergens, diet info, and RLHF feedback mechanism.
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

    private OkHttpClient httpClient;
    private String currentProduct;
    private String imagePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_api_detection_result);

        httpClient = new OkHttpClient();

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

        // --- Product name ---
        currentProduct = intent.getStringExtra("product");
        if (currentProduct != null && !currentProduct.isEmpty()) {
            String displayName = currentProduct.substring(0, 1).toUpperCase() + currentProduct.substring(1);
            if (productNameText != null) {
                productNameText.setText(displayName);
            }
        } else {
            if (productNameText != null) productNameText.setText("Unknown Food");
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

        // --- Serving Size ---
        String servingSize = intent.getStringExtra("serving_size");
        if (servingSize != null && servingSizeText != null) {
            servingSizeText.setText("Serving: " + servingSize);
            servingSizeText.setVisibility(View.VISIBLE);
        }

        // --- Health Score ---
        int healthScore = intent.getIntExtra("health_score", -1);
        if (healthScore >= 0 && healthScoreText != null) {
            healthScoreText.setText(String.valueOf(healthScore));
            int scoreColor;
            if (healthScore >= 75) scoreColor = ContextCompat.getColor(this, R.color.health_excellent);
            else if (healthScore >= 50) scoreColor = ContextCompat.getColor(this, R.color.health_moderate);
            else scoreColor = ContextCompat.getColor(this, R.color.health_unhealthy);
            healthScoreText.setTextColor(scoreColor);
        }

        // --- Diet Badge ---
        boolean isVeg = intent.getBooleanExtra("is_vegetarian", false);
        if (dietBadgeText != null) {
            if (isVeg) {
                dietBadgeText.setText("🟢 Vegetarian");
                dietBadgeText.setTextColor(ContextCompat.getColor(this, R.color.health_excellent));
            } else {
                dietBadgeText.setText("🔴 Non-Vegetarian");
                dietBadgeText.setTextColor(ContextCompat.getColor(this, R.color.health_unhealthy));
            }
            dietBadgeText.setVisibility(View.VISIBLE);
        }

        // --- Ingredients ---
        String ingredientsJson = intent.getStringExtra("ingredients_json");
        if (ingredientsJson != null && !ingredientsJson.isEmpty()) {
            try {
                JSONArray arr = new JSONArray(ingredientsJson);
                populateIngredients(arr);
            } catch (JSONException e) {
                Log.e(TAG, "Error parsing ingredients", e);
            }
        }

        // --- Nutrition ---
        String nutritionJson = intent.getStringExtra("nutrition_json");
        if (nutritionJson != null && !nutritionJson.isEmpty()) {
            try {
                JSONObject nutrition = new JSONObject(nutritionJson);
                populateNutrition(nutrition);
            } catch (JSONException e) {
                Log.e(TAG, "Error parsing nutrition JSON", e);
            }
        }

        // --- Health Tags ---
        String healthTagsJson = intent.getStringExtra("health_tags_json");
        if (healthTagsJson != null && !healthTagsJson.isEmpty()) {
            try {
                JSONArray tags = new JSONArray(healthTagsJson);
                populateHealthTags(tags);
            } catch (JSONException e) {
                Log.e(TAG, "Error parsing health tags", e);
            }
        }

        // --- Allergens ---
        String allergensJson = intent.getStringExtra("allergens_json");
        if (allergensJson != null && !allergensJson.isEmpty()) {
            try {
                JSONArray allergens = new JSONArray(allergensJson);
                populateAllergens(allergens);
            } catch (JSONException e) {
                Log.e(TAG, "Error parsing allergens", e);
            }
        }

        // --- RLHF Feedback Popup ---
        List<String> similarProducts = new ArrayList<>();
        String similarProductsStr = intent.getStringExtra("similar_products");
        if (similarProductsStr != null && !similarProductsStr.isEmpty()) {
            try {
                JSONArray arr = new JSONArray(similarProductsStr);
                for (int i = 0; i < arr.length(); i++) {
                    similarProducts.add(arr.getString(i));
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed parsing similar products");
            }
        }

        // Show RLHF feedback dialog after a short delay
        if (capturedImageView != null) {
            capturedImageView.postDelayed(() ->
                    showRLHFFeedbackDialog(currentProduct, imagePath, similarProducts), 1200);
        }
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
     * RLHF Interactive Feedback Dialog.
     * The user can confirm, correct, or select from similar foods.
     * This feedback is sent back to the server to improve the model.
     */
    private void showRLHFFeedbackDialog(String predictedProduct, String imgPath, List<String> similarProducts) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("🧠 Help Improve Recognition");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dpToPx(24), dpToPx(16), dpToPx(24), dpToPx(16));

        // Header
        TextView headerText = new TextView(this);
        headerText.setText("Is this correct?\nDetected: " + (predictedProduct != null ? predictedProduct : "Unknown"));
        headerText.setTextSize(15);
        headerText.setLineSpacing(dpToPx(4), 1f);
        headerText.setPadding(0, 0, 0, dpToPx(16));
        layout.addView(headerText);

        // RLHF explanation
        TextView rlhfNote = new TextView(this);
        rlhfNote.setText("Your feedback helps the AI learn and improve over time (RLHF).");
        rlhfNote.setTextSize(12);
        rlhfNote.setTextColor(ContextCompat.getColor(this, R.color.text_secondary_light));
        rlhfNote.setPadding(0, 0, 0, dpToPx(12));
        layout.addView(rlhfNote);

        // Custom input
        final EditText searchInput = new EditText(this);
        searchInput.setHint("Or type the correct food name...");
        layout.addView(searchInput);

        if (!similarProducts.isEmpty()) {
            // Similar products list
            TextView suggestLabel = new TextView(this);
            suggestLabel.setText("Similar foods:");
            suggestLabel.setTextSize(13);
            suggestLabel.setTypeface(null, Typeface.BOLD);
            suggestLabel.setPadding(0, dpToPx(12), 0, dpToPx(4));
            layout.addView(suggestLabel);

            ListView listView = new ListView(this);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_list_item_1, similarProducts);
            listView.setAdapter(adapter);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(200));
            listView.setLayoutParams(lp);
            layout.addView(listView);

            AlertDialog dialog = builder.setView(layout)
                    .setPositiveButton("Submit Correction", (d, which) -> {
                        String correctLabel = searchInput.getText().toString().trim();
                        if (!correctLabel.isEmpty()) {
                            sendRLHFFeedback(correctLabel, predictedProduct, imgPath);
                        }
                    })
                    .setNegativeButton("✅ Confirm Correct", (d, which) -> {
                        if (predictedProduct != null && !predictedProduct.equals("Unknown")) {
                            sendRLHFFeedback(predictedProduct, predictedProduct, imgPath);
                            Toast.makeText(this, "Thanks! Positive feedback recorded.", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNeutralButton("Skip", null)
                    .create();

            listView.setOnItemClickListener((parent, view, position, id) -> {
                String selected = similarProducts.get(position);
                sendRLHFFeedback(selected, predictedProduct, imgPath);
                Toast.makeText(this, "Feedback: " + selected, Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });

            dialog.show();
        } else {
            builder.setView(layout)
                    .setPositiveButton("Submit", (dialog, which) -> {
                        String correctLabel = searchInput.getText().toString().trim();
                        if (!correctLabel.isEmpty()) {
                            sendRLHFFeedback(correctLabel, predictedProduct, imgPath);
                        }
                    })
                    .setNeutralButton("✅ Confirm Correct", (dialog, which) -> {
                        sendRLHFFeedback(predictedProduct, predictedProduct, imgPath);
                        Toast.makeText(this, "Positive feedback recorded!", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Skip", null)
                    .show();
        }
    }

    /**
     * Send RLHF feedback to the server.
     * The server stores this and uses it to fine-tune the model.
     */
    private void sendRLHFFeedback(String correctLabel, String predictedLabel, String imgPath) {
        File file = new File(imgPath);
        if (!file.exists()) return;

        MultipartBody.Builder bodyBuilder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("image", file.getName(),
                        RequestBody.create(file, MediaType.parse("image/jpeg")))
                .addFormDataPart("label", correctLabel);

        if (predictedLabel != null) {
            bodyBuilder.addFormDataPart("predicted_label", predictedLabel);
        }

        Request request = new Request.Builder()
                .url(ApiConfig.API_URL_STORE_FEEDBACK)
                .post(bodyBuilder.build())
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "RLHF feedback failed", e);
                runOnUiThread(() -> Toast.makeText(ApiDetectionResultActivity.this,
                        "Failed to send feedback", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) {
                runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        Toast.makeText(ApiDetectionResultActivity.this,
                                "🧠 Feedback recorded! Model will learn from this.",
                                Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(ApiDetectionResultActivity.this,
                                "Failed to record feedback.", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
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
