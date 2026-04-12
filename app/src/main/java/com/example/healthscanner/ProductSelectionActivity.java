package com.example.healthscanner;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ProductSelectionActivity extends AppCompatActivity {

    private static final String TAG = "ProductSelection";
    
    private TextInputEditText searchEditText;
    private MaterialButton btnSearch;
    private MaterialButton btnScanBarcodeFallback;
    private ProgressBar loadingProgress;
    private TextView errorText;
    private RecyclerView productRecyclerView;
    
    private ProductSelectionAdapter adapter;
    private List<ProductSelectionAdapter.ProductItem> masterProductList;
    private List<ProductSelectionAdapter.ProductItem> filteredList;
    
    private Handler debounceHandler;
    private Runnable searchRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_selection);

        debounceHandler = new Handler(Looper.getMainLooper());
        masterProductList = new ArrayList<>();
        filteredList = new ArrayList<>();

        setupUI();
        loadLocalDataset();
    }

    private void setupUI() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        searchEditText = findViewById(R.id.search_edit_text);
        btnSearch = findViewById(R.id.btn_search);
        btnScanBarcodeFallback = findViewById(R.id.btn_scan_barcode_fallback);
        loadingProgress = findViewById(R.id.loading_progress);
        errorText = findViewById(R.id.error_text);
        productRecyclerView = findViewById(R.id.product_recycler_view);

        btnScanBarcodeFallback.setOnClickListener(v -> {
            Intent intent = new Intent(this, VerticalScannerActivity.class);
            startActivity(intent);
            finish();
        });

        adapter = new ProductSelectionAdapter(filteredList, item -> {
            submitCorrectionAndFinish(item.barcode);
        });

        productRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        productRecyclerView.setAdapter(adapter);

        btnSearch.setOnClickListener(v -> {
            String query = searchEditText.getText().toString().trim();
            filterProducts(query);
        });

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (searchRunnable != null) {
                    debounceHandler.removeCallbacks(searchRunnable);
                }
                searchRunnable = () -> filterProducts(s.toString().trim());
                debounceHandler.postDelayed(searchRunnable, 300); // 300ms debounce
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadLocalDataset() {
        loadingProgress.setVisibility(View.VISIBLE);
        new Thread(() -> {
            try {
                JSONArray jsonArray = DatabaseHelper.loadLocalDatabase(this);
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject obj = jsonArray.getJSONObject(i);
                    masterProductList.add(new ProductSelectionAdapter.ProductItem(
                            obj.getString("barcode"),
                            obj.getString("product_name"),
                            obj.optString("brand", "Unknown")
                    ));
                }

                runOnUiThread(() -> {
                    loadingProgress.setVisibility(View.GONE);
                    // Initially show all
                    filteredList.addAll(masterProductList);
                    adapter.updateData(filteredList);
                });
            } catch (Exception e) {
                Log.e(TAG, "Error loading local dataset", e);
                runOnUiThread(() -> {
                    loadingProgress.setVisibility(View.GONE);
                    errorText.setText("Failed to load local product database.");
                    errorText.setVisibility(View.VISIBLE);
                });
            }
        }).start();
    }

    private void filterProducts(String query) {
        filteredList.clear();
        errorText.setVisibility(View.GONE);
        btnScanBarcodeFallback.setVisibility(View.GONE);

        if (query.isEmpty()) {
            filteredList.addAll(masterProductList);
            adapter.updateData(filteredList);
            return;
        }

        String lowerQuery = query.toLowerCase();
        for (ProductSelectionAdapter.ProductItem item : masterProductList) {
            if (item.name.toLowerCase().contains(lowerQuery) || item.brand.toLowerCase().contains(lowerQuery)) {
                filteredList.add(item);
            }
        }

        if (filteredList.isEmpty()) {
            errorText.setText("Product not found in verified database.");
            errorText.setVisibility(View.VISIBLE);
            btnScanBarcodeFallback.setVisibility(View.VISIBLE);
        }

        adapter.updateData(filteredList);
    }

    private void submitCorrectionAndFinish(String barcode) {
        // Enforce Read-Only System. Directly load the Barcode details.
        Intent intent = new Intent(this, ProductDetailsEnhancedActivity.class);
        intent.putExtra("barcode", barcode);
        startActivity(intent);
        finish();
    }
}
