package com.example.healthscanner;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * Premium Smoked-Glass Search Activity
 * Allows text-based search of the OpenFoodFacts database and displays rich nutritional results.
 */
public class SearchActivity extends BaseActivity {

    private static final String TAG = "SearchActivity";

    // UI Elements
    private EditText etSearchQuery;
    private ImageView btnClearSearch;
    private RecyclerView searchResultRecyclerView;
    private LinearLayout emptyStateContainer;
    private TextView emptyStateTitle;
    private TextView emptyStateMessage;
    private View progressSearch;
    private ImageView btnBack;

    // Filter Chips
    private TextView chipAll, chipSnacks, chipDrinks, chipDairy, chipBakery;
    private TextView selectedChip;

    // Services & Adapter
    private ProductApiService apiService;
    private SearchResultAdapter adapter;
    private List<ProductApiService.ProductInfo> searchResults = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        // Initialize API Service
        apiService = new ProductApiService(this);

        // Bind Views
        initializeViews();
        setupClickListeners();
        setupSearchInput();
        setupRecyclerView();
        setupFilterChips();

        // Initialize bottom navigation
        initializeBottomNavigation();
    }

    private void initializeViews() {
        etSearchQuery = findViewById(R.id.et_search_query);
        btnClearSearch = findViewById(R.id.btn_clear_search);
        searchResultRecyclerView = findViewById(R.id.searchResultRecyclerView);
        emptyStateContainer = findViewById(R.id.emptyStateContainer);
        emptyStateTitle = findViewById(R.id.emptyStateTitle);
        emptyStateMessage = findViewById(R.id.emptyStateMessage);
        progressSearch = findViewById(R.id.progress_search);
        btnBack = findViewById(R.id.btn_back);

        // Chips
        chipAll = findViewById(R.id.chip_all);
        chipSnacks = findViewById(R.id.chip_snacks);
        chipDrinks = findViewById(R.id.chip_drinks);
        chipDairy = findViewById(R.id.chip_dairy);
        chipBakery = findViewById(R.id.chip_bakery);
        selectedChip = chipAll;
    }

    private void setupClickListeners() {
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (btnClearSearch != null) {
            btnClearSearch.setOnClickListener(v -> {
                etSearchQuery.setText("");
                btnClearSearch.setVisibility(View.GONE);
                showWelcomeState();
            });
        }
    }

    private void setupSearchInput() {
        if (etSearchQuery == null) return;

        etSearchQuery.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (btnClearSearch != null) {
                    btnClearSearch.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() == 0) {
                    showWelcomeState();
                }
            }
        });

        etSearchQuery.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                performSearch(etSearchQuery.getText().toString().trim());
                hideKeyboard();
                return true;
            }
            return false;
        });
    }

    private void setupRecyclerView() {
        if (searchResultRecyclerView == null) return;

        searchResultRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SearchResultAdapter(searchResults, product -> {
            // Navigate to Product Details
            if (product.barcode != null && !product.barcode.isEmpty()) {
                Intent intent = new Intent(SearchActivity.this, ProductDetailsEnhancedActivity.class);
                intent.putExtra("barcode", product.barcode);
                startActivity(intent);
            } else {
                Toast.makeText(SearchActivity.this, "No barcode available for this product", Toast.LENGTH_SHORT).show();
            }
        });
        searchResultRecyclerView.setAdapter(adapter);
    }

    private void setupFilterChips() {
        View.OnClickListener chipClickListener = v -> {
            TextView clickedChip = (TextView) v;
            if (clickedChip == selectedChip) return;

            // Update backgrounds
            selectedChip.setBackgroundResource(R.drawable.bg_smoked_glass_nav);
            selectedChip.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));

            clickedChip.setBackgroundResource(R.drawable.bg_nav_active_capsule);
            clickedChip.setTextColor(ContextCompat.getColor(this, R.color.white));

            selectedChip = clickedChip;

            // Trigger search or filter
            String category = clickedChip.getText().toString();
            if (category.equals("All")) {
                etSearchQuery.setText("");
                showWelcomeState();
            } else {
                etSearchQuery.setText(category);
                etSearchQuery.setSelection(category.length());
                performSearch(category);
            }
        };

        if (chipAll != null) chipAll.setOnClickListener(chipClickListener);
        if (chipSnacks != null) chipSnacks.setOnClickListener(chipClickListener);
        if (chipDrinks != null) chipDrinks.setOnClickListener(chipClickListener);
        if (chipDairy != null) chipDairy.setOnClickListener(chipClickListener);
        if (chipBakery != null) chipBakery.setOnClickListener(chipClickListener);
    }

    private void performSearch(String query) {
        if (query.isEmpty()) return;

        Log.d(TAG, "Searching for: " + query);
        showLoadingState();

        apiService.searchProducts(query, new ProductApiService.SearchCallback() {
            @Override
            public void onSuccess(List<ProductApiService.ProductInfo> products) {
                runOnUiThread(() -> {
                    hideLoadingState();
                    searchResults.clear();
                    if (products != null && !products.isEmpty()) {
                        searchResults.addAll(products);
                        adapter.notifyDataSetChanged();
                        showResultsState();
                    } else {
                        showEmptyResultsState();
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    hideLoadingState();
                    Toast.makeText(SearchActivity.this, "Search failed: " + error, Toast.LENGTH_LONG).show();
                    showEmptyResultsState();
                });
            }
        });
    }

    private void showWelcomeState() {
        searchResults.clear();
        adapter.notifyDataSetChanged();
        if (searchResultRecyclerView != null) searchResultRecyclerView.setVisibility(View.GONE);
        if (emptyStateContainer != null) {
            emptyStateContainer.setVisibility(View.VISIBLE);
            if (emptyStateTitle != null) emptyStateTitle.setText("Find any food item");
            if (emptyStateMessage != null) emptyStateMessage.setText("Type in the search bar above to query global food databases");
        }
    }

    private void showLoadingState() {
        if (progressSearch != null) progressSearch.setVisibility(View.VISIBLE);
        if (emptyStateContainer != null) emptyStateContainer.setVisibility(View.GONE);
        if (searchResultRecyclerView != null) searchResultRecyclerView.setVisibility(View.GONE);
    }

    private void hideLoadingState() {
        if (progressSearch != null) progressSearch.setVisibility(View.GONE);
    }

    private void showResultsState() {
        if (emptyStateContainer != null) emptyStateContainer.setVisibility(View.GONE);
        if (searchResultRecyclerView != null) searchResultRecyclerView.setVisibility(View.VISIBLE);
    }

    private void showEmptyResultsState() {
        searchResults.clear();
        adapter.notifyDataSetChanged();
        if (searchResultRecyclerView != null) searchResultRecyclerView.setVisibility(View.GONE);
        if (emptyStateContainer != null) {
            emptyStateContainer.setVisibility(View.VISIBLE);
            if (emptyStateTitle != null) emptyStateTitle.setText("No products found");
            if (emptyStateMessage != null) emptyStateMessage.setText("Try refining your search terms or check spelling");
        }
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

    @Override
    protected int getCurrentNavigationItemId() {
        return R.id.nav_search;
    }

    // Adapter for search results reusing R.layout.item_history_enhanced
    private static class SearchResultAdapter extends RecyclerView.Adapter<SearchResultAdapter.ViewHolder> {
        private final List<ProductApiService.ProductInfo> items;
        private final OnItemClickListener listener;

        interface OnItemClickListener {
            void onItemClick(ProductApiService.ProductInfo item);
        }

        SearchResultAdapter(List<ProductApiService.ProductInfo> items, OnItemClickListener listener) {
            this.items = items;
            this.listener = listener;
        }

        @Override
        public ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            View view = android.view.LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_history_enhanced, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            ProductApiService.ProductInfo item = items.get(position);

            holder.productName.setText(item.name);
            holder.brandName.setText(item.brand);
            holder.scanTime.setText("Source: " + item.source);

            // Health Score badge configuration
            if (item.healthScore > 0) {
                holder.healthScore.setText(String.format("%.1f", item.healthScore));
                int scoreColorRes;
                if (item.healthScore >= 8.0) {
                    scoreColorRes = R.color.health_excellent;
                } else if (item.healthScore >= 6.0) {
                    scoreColorRes = R.color.health_good;
                } else if (item.healthScore >= 4.0) {
                    scoreColorRes = R.color.health_moderate;
                } else {
                    scoreColorRes = R.color.health_poor;
                }
                holder.healthScoreCard.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), scoreColorRes));
            } else {
                holder.healthScore.setText("--");
                holder.healthScoreCard.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.text_secondary));
            }

            // Calories & Nutrition info
            holder.caloriesText.setText(item.calories > 0 ? item.calories + " cal" : "-- cal");
            holder.proteinText.setText(item.protein > 0 ? String.format("%.1fg protein", item.protein) : "-- protein");
            holder.sugarText.setText(item.sugar > 0 ? String.format("%.1fg sugar", item.sugar) : "-- sugar");

            // Health Insight
            holder.healthInsight.setText(getHealthInsightSummary(item.healthScore));

            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(item);
                }
            });
        }

        private String getHealthInsightSummary(double healthScore) {
            if (healthScore >= 8.0) {
                return "Excellent nutritional profile! 🌟";
            } else if (healthScore >= 6.0) {
                return "Good health score choice. ✅";
            } else if (healthScore >= 4.0) {
                return "Moderate choice. Check values. ⚖️";
            } else if (healthScore > 0) {
                return "Try healthy alternatives. ⚠️";
            } else {
                return "Nutritional analysis pending...";
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView productName, brandName, scanTime, healthScore;
            TextView caloriesText, proteinText, sugarText, healthInsight;
            com.google.android.material.card.MaterialCardView healthScoreCard;

            ViewHolder(View itemView) {
                super(itemView);
                productName = itemView.findViewById(R.id.productName);
                brandName = itemView.findViewById(R.id.brandName);
                scanTime = itemView.findViewById(R.id.scanTime);
                healthScore = itemView.findViewById(R.id.healthScore);
                caloriesText = itemView.findViewById(R.id.caloriesText);
                proteinText = itemView.findViewById(R.id.proteinText);
                sugarText = itemView.findViewById(R.id.sugarText);
                healthInsight = itemView.findViewById(R.id.healthInsight);
                healthScoreCard = itemView.findViewById(R.id.healthScoreCard);
            }
        }
    }
}
