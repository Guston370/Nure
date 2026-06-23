package com.example.healthscanner;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ProductCreationActivity extends AppCompatActivity {
    private static final String TAG = "ProductCreation";

    private String barcode;
    private TextInputEditText inputName;
    private TextInputEditText inputCategory;
    private TextInputEditText inputIngredients;
    private ImageView imagePreview;
    private ProgressBar loadingProgress;
    private MaterialButton btnSubmit;

    private String currentPhotoPath = null;
    private OkHttpClient httpClient;

    private final ActivityResultLauncher<Intent> takePictureLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && currentPhotoPath != null) {
                    imagePreview.setVisibility(View.VISIBLE);
                    imagePreview.setImageURI(Uri.fromFile(new File(currentPhotoPath)));
                } else {
                    currentPhotoPath = null;
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_creation);

        httpClient = new OkHttpClient();
        barcode = getIntent().getStringExtra("barcode");

        if (barcode == null || barcode.isEmpty()) {
            Toast.makeText(this, "Barcode verification required to add products.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        setupUI();
    }

    private void setupUI() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        TextView textBarcode = findViewById(R.id.text_barcode_display);
        textBarcode.setText("Barcode: " + barcode);

        inputName = findViewById(R.id.input_name);
        inputCategory = findViewById(R.id.input_category);
        inputIngredients = findViewById(R.id.input_ingredients);
        imagePreview = findViewById(R.id.image_preview);
        loadingProgress = findViewById(R.id.loading_progress);
        btnSubmit = findViewById(R.id.btn_submit_product);

        MaterialButton btnCapture = findViewById(R.id.btn_capture_image);
        btnCapture.setOnClickListener(v -> dispatchTakePictureIntent());

        btnSubmit.setOnClickListener(v -> attemptSubmission());
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
                File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                photoFile = File.createTempFile("JPEG_" + timeStamp + "_", ".jpg", storageDir);
                currentPhotoPath = photoFile.getAbsolutePath();
            } catch (IOException ex) {
                Log.e(TAG, "Photo creation failed", ex);
            }
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.healthscanner.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                takePictureLauncher.launch(takePictureIntent);
            }
        }
    }

    private boolean isNetworkAvailable() {
        android.net.ConnectivityManager cm = (android.net.ConnectivityManager) getSystemService(android.content.Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            android.net.NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        }
        return false;
    }

    private void attemptSubmission() {
        String name = inputName.getText().toString().trim();
        String category = inputCategory.getText().toString().trim();
        String ingredients = inputIngredients.getText().toString().trim();

        if (name.isEmpty()) {
            inputName.setError("Product Name is mandatory");
            return;
        }

        if (DatabaseHelper.checkDuplicateBarcode(this, barcode)) {
            Toast.makeText(this, "CRITICAL CONFLICT: This barcode already exists in the dataset!", Toast.LENGTH_LONG).show();
            return;
        }

        loadingProgress.setVisibility(View.VISIBLE);
        btnSubmit.setEnabled(false);

        try {
            JSONObject metadata = new JSONObject();
            metadata.put("barcode", barcode);
            metadata.put("product_name", name);
            metadata.put("brand", category);
            metadata.put("ingredients", ingredients);
            
            // Persist to local preview db to keep local features alive
            DatabaseHelper.addNewProduct(ProductCreationActivity.this, metadata);

            boolean isOnline = isNetworkAvailable();
            String deviceId = android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
            
            com.example.healthscanner.database.queue.DataQueueEntity entity = new com.example.healthscanner.database.queue.DataQueueEntity(
                    java.util.UUID.randomUUID().toString(),
                    name,
                    barcode,
                    currentPhotoPath,
                    System.currentTimeMillis(),
                    "manual",
                    deviceId != null ? deviceId : "unknown_device",
                    "pending",
                    0.0
            );

            new Thread(() -> {
                com.example.healthscanner.database.queue.AppDatabase.getDatabase(this).queueDao().insert(entity);
                
                androidx.work.Constraints constraints = new androidx.work.Constraints.Builder()
                        .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                        .build();
                        
                androidx.work.OneTimeWorkRequest syncWorkRequest = new androidx.work.OneTimeWorkRequest.Builder(com.example.healthscanner.database.queue.DataSyncWorker.class)
                        .setConstraints(constraints)
                        .build();
                        
                androidx.work.WorkManager.getInstance(this).enqueue(syncWorkRequest);

                runOnUiThread(() -> {
                    loadingProgress.setVisibility(View.GONE);
                    if (!isOnline) {
                        Toast.makeText(ProductCreationActivity.this, "Saved offline", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(ProductCreationActivity.this, "Product queued for upload!", Toast.LENGTH_SHORT).show();
                    }
                    
                    Intent intent = new Intent(ProductCreationActivity.this, ProductDetailsEnhancedActivity.class);
                    intent.putExtra("barcode", barcode);
                    startActivity(intent);
                    finish();
                });
            }).start();
            
        } catch (Exception e) {
            loadingProgress.setVisibility(View.GONE);
            btnSubmit.setEnabled(true);
            Log.e(TAG, "Submission breakdown", e);
        }
    }
}
