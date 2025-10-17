package com.example.healthscanner;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;

public class ScannerActivity extends AppCompatActivity {
    private DecoratedBarcodeView barcodeView;
    private MaterialCardView productDetailsCard;
    private TextView productName, productBrand;
    private TextView caloriesValue, proteinValue, sugarValue;
    private MaterialButton cameraScanButton, galleryScanButton;
    private TextView scanStatusText;
    private BottomNavigationView bottomNavigation;
    private AuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner_enhanced);

        // Initialize views
        initializeViews();
        setupBarcodeScanner();
        setupClickListeners();
        setupBottomNavigation();
    }

    private void initializeViews() {
        barcodeView = findViewById(R.id.scanner_view);
        productDetailsCard = findViewById(R.id.product_details_card);
        productName = findViewById(R.id.product_name);
        productBrand = findViewById(R.id.product_brand);
        caloriesValue = findViewById(R.id.calories_value);
        proteinValue = findViewById(R.id.protein_value);
        sugarValue = findViewById(R.id.sugar_value);
        cameraScanButton = findViewById(R.id.camera_scan_button);
        galleryScanButton = findViewById(R.id.gallery_scan_button);
        scanStatusText = findViewById(R.id.scan_status_text);
        bottomNavigation = findViewById(R.id.bottom_navigation);

        // Initialize Authentication Manager
        authManager = AuthManager.getInstance(this);
    }

    private void setupBarcodeScanner() {
        barcodeView.decodeContinuous(new BarcodeCallback() {
            @Override
            public void barcodeResult(BarcodeResult result) {
                if (result.getText() != null) {
                    // Temporarily pause scanning
                    barcodeView.pauseAndWait();
                    
                    // Show the barcode content
                    handleBarcodeScan(result.getText());
                }
            }
        });

        // Lock scanner to vertical orientation
        barcodeView.setStatusText("");
    }

    private void setupClickListeners() {
        cameraScanButton.setOnClickListener(v -> {
            barcodeView.resume();
            productDetailsCard.setVisibility(View.GONE);
            scanStatusText.setText("Position barcode in frame to scan");
        });

        galleryScanButton.setOnClickListener(v -> {
            // Implement gallery image selection
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), 1);
        });
    }

    private void setupBottomNavigation() {
        bottomNavigation.setSelectedItemId(R.id.nav_scan);
        
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_history) {
                startActivity(new Intent(this, HistoryActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_scan) {
                return true;
            }
            return false;
        });
    }

    private void handleBarcodeScan(String barcode) {
        // Show scanning in progress
        scanStatusText.setText("🔍 Analyzing product...");
        
        // Mock product details (replace with actual API call)
        showProductDetails("Sample Product", "Brand Name", "200 kcal", "10g", "5g");
    }

    private void showProductDetails(String name, String brand, String calories, String protein, String sugar) {
        productName.setText(name);
        productBrand.setText(brand);
        caloriesValue.setText(calories);
        proteinValue.setText(protein);
        sugarValue.setText(sugar);

        productDetailsCard.setVisibility(View.VISIBLE);
        scanStatusText.setText("✅ Product found! Tap camera to scan again");
    }

    @Override
    protected void onResume() {
        super.onResume();
        barcodeView.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        barcodeView.pause();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
            // Handle gallery image processing
            // You would need to implement barcode detection from image here
            // For now, just show a message
            scanStatusText.setText("Gallery image processing coming soon!");
        }
    }
}