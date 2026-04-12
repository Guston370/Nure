package com.example.healthscanner;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;

import android.widget.EditText;
import android.widget.ListView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.app.AlertDialog;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Displays the result of a YOLO product detection API call.
 * Receives the image path, product name, confidence, and nutrition JSON via Intent extras.
 */
public class ApiDetectionResultActivity extends AppCompatActivity {

    private static final String TAG = "ApiDetectionResult";

    // UI elements
    private ImageView capturedImageView;
    private TextView productNameText;
    private TextView confidenceText;
    private ProgressBar confidenceProgressBar;
    private LinearLayout nutritionContainer;
    private CardView nutritionCard;
    private CardView errorCard;
    private TextView errorText;
    private MaterialButton btnScanAnother;
    private MaterialButton btnGoHome;
    
    private OkHttpClient httpClient;

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
            btnScanAnother.setOnClickListener(v -> {
                // Go back to the scanner
                finish();
            });
        }

        if (btnGoHome != null) {
            btnGoHome.setOnClickListener(v -> {
                // Go to main activity
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
        String imagePath = intent.getStringExtra("image_path");
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
        String product = intent.getStringExtra("product");
        if (product != null && !product.isEmpty()) {
            // Capitalize first letter
            String displayName = product.substring(0, 1).toUpperCase() + product.substring(1);
            if (productNameText != null) {
                productNameText.setText(displayName);
            }
        } else {
            if (productNameText != null) {
                productNameText.setText("Unknown Product");
            }
        }

        // --- Confidence ---
        double confidence = intent.getDoubleExtra("confidence", -1);
        if (confidence >= 0) {
            int percent = (int) Math.round(confidence * 100);
            if (confidenceText != null) {
                confidenceText.setText(percent + "%");
            }
            if (confidenceProgressBar != null) {
                confidenceProgressBar.setProgress(percent);

                // Colour the bar based on confidence
                int barColor;
                if (percent >= 80) {
                    barColor = ContextCompat.getColor(this, R.color.health_excellent);
                } else if (percent >= 50) {
                    barColor = ContextCompat.getColor(this, R.color.health_moderate);
                } else {
                    barColor = ContextCompat.getColor(this, R.color.health_unhealthy);
                }
                confidenceProgressBar.setProgressTintList(
                        android.content.res.ColorStateList.valueOf(barColor));
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
        
        // Show feedback popup AFTER result binds
        String similarProductsStr = intent.getStringExtra("similar_products");
        java.util.List<String> similarProducts = new java.util.ArrayList<>();
        if (similarProductsStr != null && !similarProductsStr.isEmpty()) {
            try {
                org.json.JSONArray arr = new org.json.JSONArray(similarProductsStr);
                for (int i = 0; i < arr.length(); i++) {
                    similarProducts.add(arr.getString(i));
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed parsing similar products");
            }
        }
        
        // Show interactive dialog
        showInteractiveFeedbackPopup(product, imagePath, similarProducts);
    }
    
    private void showInteractiveFeedbackPopup(String predictedProduct, String imagePath, java.util.List<String> similarProducts) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Confirm Detected Product");
        
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);
        
        TextView headerText = new TextView(this);
        headerText.setText("Detected match: " + (predictedProduct != null ? predictedProduct : "Unknown"));
        headerText.setTextSize(16);
        headerText.setPadding(0, 0, 0, 20);
        layout.addView(headerText);
        
        final EditText searchInput = new EditText(this);
        searchInput.setHint("Type custom product name...");
        layout.addView(searchInput);
        
        if (!similarProducts.isEmpty()) {
            ListView listView = new ListView(this);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, similarProducts);
            listView.setAdapter(adapter);
            
            // Set max height for listview roughly
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 400);
            listView.setLayoutParams(lp);
            
            layout.addView(listView);
            
            AlertDialog dialog = builder.setView(layout)
                .setPositiveButton("Submit Custom Entry", (d, which) -> {
                    String correctLabel = searchInput.getText().toString().trim();
                    if (!correctLabel.isEmpty()) {
                        sendFeedback(correctLabel, imagePath);
                    } else {
                        Toast.makeText(this, "Empty custom label ignored.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Confirm Detected", (d, which) -> {
                     if (predictedProduct != null && !predictedProduct.equals("Unknown")) {
                         sendFeedback(predictedProduct, imagePath);
                     } else {
                         Toast.makeText(this, "No valid prediction to confirm.", Toast.LENGTH_SHORT).show();
                     }
                })
                .create();
                
            listView.setOnItemClickListener((parent, view, position, id) -> {
                String selected = similarProducts.get(position);
                sendFeedback(selected, imagePath);
                dialog.dismiss();
            });
            
            capturedImageView.postDelayed(dialog::show, 800);
            
        } else {
            builder.setView(layout)
                .setPositiveButton("Submit", (dialog, which) -> {
                     String correctLabel = searchInput.getText().toString().trim();
                     if (!correctLabel.isEmpty()) {
                         sendFeedback(correctLabel, imagePath);
                     } else {
                         Toast.makeText(this, "Empty label ignored.", Toast.LENGTH_SHORT).show();
                     }
                })
                .setNeutralButton("Confirm Detected", (dialog, which) -> {
                     sendFeedback(predictedProduct, imagePath);
                 });
            
            capturedImageView.postDelayed(builder::show, 800);
        }
    }
    
    private void sendFeedback(String label, String imagePath) {
        File file = new File(imagePath);
        if (!file.exists()) return;
        
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("image", file.getName(), RequestBody.create(file, MediaType.parse("image/jpeg")))
                .addFormDataPart("label", label)
                .build();
                
        Request request = new Request.Builder()
                .url(ApiConfig.API_URL_STORE_FEEDBACK)
                .post(requestBody)
                .build();
                
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Feedback submission failed", e);
                runOnUiThread(() -> Toast.makeText(ApiDetectionResultActivity.this, "Failed to send feedback", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) {
                runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        Toast.makeText(ApiDetectionResultActivity.this, "Feedback recorded successfully!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(ApiDetectionResultActivity.this, "Failed to record feedback.", Toast.LENGTH_SHORT).show();
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

        if (nutritionCard != null) {
            nutritionCard.setVisibility(View.VISIBLE);
        }

        Iterator<String> keys = nutrition.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            String value = nutrition.optString(key, "—");

            // Pretty-print the key
            String label = prettifyKey(key);

            addNutritionRow(label, value);
        }
    }

    private void addNutritionRow(String label, String value) {
        if (nutritionContainer == null) return;

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dpToPx(8), 0, dpToPx(8));

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
        valueTv.setTypeface(null, android.graphics.Typeface.BOLD);

        row.addView(labelTv);
        row.addView(valueTv);
        nutritionContainer.addView(row);

        // Add divider
        View divider = new View(this);
        divider.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(1)));
        divider.setBackgroundColor(ContextCompat.getColor(this, R.color.divider_light));
        nutritionContainer.addView(divider);
    }

    private String prettifyKey(String key) {
        // "calories" -> "Calories", "total_fat" -> "Total Fat"
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
        if (errorCard != null) {
            errorCard.setVisibility(View.VISIBLE);
        }
        if (errorText != null) {
            errorText.setText(message);
        }
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }
}
