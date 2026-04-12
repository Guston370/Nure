package com.example.healthscanner;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ProductSelectionActivity extends AppCompatActivity {

    private static final String TAG = "ProductSelection";
    
    private TextInputEditText searchEditText;
    private MaterialButton btnSearch;
    private MaterialButton btnSubmitManual;
    private ProgressBar loadingProgress;
    private TextView errorText;
    private RecyclerView productRecyclerView;
    
    private ProductSelectionAdapter adapter;
    private List<ProductSelectionAdapter.ProductItem> productList;
    
    private RequestQueue requestQueue;
    private OkHttpClient httpClient;
    
    private String capturedImagePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_selection);

        capturedImagePath = getIntent().getStringExtra("image_path");
        if (capturedImagePath == null) {
            Toast.makeText(this, "No image provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        httpClient = new OkHttpClient();
        requestQueue = Volley.newRequestQueue(this);

        setupUI();
    }

    private void setupUI() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        searchEditText = findViewById(R.id.search_edit_text);
        btnSearch = findViewById(R.id.btn_search);
        btnSubmitManual = findViewById(R.id.btn_submit_manual);
        loadingProgress = findViewById(R.id.loading_progress);
        errorText = findViewById(R.id.error_text);
        productRecyclerView = findViewById(R.id.product_recycler_view);

        productList = new ArrayList<>();
        adapter = new ProductSelectionAdapter(productList, item -> {
            submitCorrectionAndFinish(item.name);
        });

        productRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        productRecyclerView.setAdapter(adapter);

        btnSearch.setOnClickListener(v -> {
            String query = searchEditText.getText().toString().trim();
            if (!query.isEmpty()) {
                searchOpenFoodFacts(query);
            }
        });

        btnSubmitManual.setOnClickListener(v -> {
            String manualLabel = searchEditText.getText().toString().trim();
            if (!manualLabel.isEmpty()) {
                submitCorrectionAndFinish(manualLabel);
            } else {
                searchEditText.setError("Please type a product name");
            }
        });
    }

    private void searchOpenFoodFacts(String query) {
        loadingProgress.setVisibility(View.VISIBLE);
        errorText.setVisibility(View.GONE);
        productRecyclerView.setVisibility(View.GONE);

        String url = "https://world.openfoodfacts.org/cgi/search.pl?search_terms=" + 
                android.net.Uri.encode(query) + "&search_simple=1&action=process&json=1";

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    loadingProgress.setVisibility(View.GONE);
                    try {
                        JSONArray productsObj = response.optJSONArray("products");
                        productList.clear();
                        if (productsObj != null && productsObj.length() > 0) {
                            for (int i = 0; i < Math.min(20, productsObj.length()); i++) {
                                JSONObject item = productsObj.getJSONObject(i);
                                String name = item.optString("product_name", "");
                                String brand = item.optString("brands", "");
                                if (!name.isEmpty()) {
                                    productList.add(new ProductSelectionAdapter.ProductItem(name, brand));
                                }
                            }
                            adapter.updateData(productList);
                            productRecyclerView.setVisibility(View.VISIBLE);
                        } else {
                            errorText.setText("No products found for that search. You can enter a custom text and tap 'Submit Typed Text Directly'.");
                            errorText.setVisibility(View.VISIBLE);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Parsing error", e);
                        errorText.setText("Failed to parse directory results.");
                        errorText.setVisibility(View.VISIBLE);
                    }
                },
                error -> {
                    loadingProgress.setVisibility(View.GONE);
                    Log.e(TAG, "Search network error", error);
                    errorText.setText("Network error searching database.");
                    errorText.setVisibility(View.VISIBLE);
                });

        requestQueue.add(request);
    }

    private void submitCorrectionAndFinish(String correctLabel) {
        loadingProgress.setVisibility(View.VISIBLE);
        Toast.makeText(this, "Uploading feedback...", Toast.LENGTH_SHORT).show();

        File photoFile = new File(capturedImagePath);
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("image", photoFile.getName(),
                        RequestBody.create(photoFile, MediaType.parse("image/jpeg")))
                .addFormDataPart("label", correctLabel)
                .build();

        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(ApiConfig.API_URL_STORE_FEEDBACK)
                .post(requestBody)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Feedback submission failed", e);
                runOnUiThread(() -> {
                    loadingProgress.setVisibility(View.GONE);
                    Toast.makeText(ProductSelectionActivity.this, "Network Error Submitting Feedback", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                runOnUiThread(() -> {
                    loadingProgress.setVisibility(View.GONE);
                    if (response.isSuccessful()) {
                        Toast.makeText(ProductSelectionActivity.this, "Correction Saved for Improvement!", Toast.LENGTH_LONG).show();
                        
                        // Proceed to the result screen with the new confirmed label
                        Intent intent = new Intent(ProductSelectionActivity.this, ApiDetectionResultActivity.class);
                        intent.putExtra("image_path", capturedImagePath);
                        intent.putExtra("product", correctLabel);
                        intent.putExtra("confidence", 1.0); // Forcing 100% since human verified
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(ProductSelectionActivity.this, "Failed to store feedback.", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }
}
