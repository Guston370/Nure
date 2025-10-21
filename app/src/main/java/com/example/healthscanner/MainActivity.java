package com.example.healthscanner;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.example.healthscanner.database.SyncManager;
import com.example.healthscanner.services.AutoSyncService;

import java.util.HashMap;
import java.util.Map;
import com.example.healthscanner.services.AutoSyncService;

public class MainActivity extends BaseActivity {

    private static final String TAG = "MainActivity";
    private static final int CAMERA_PERMISSION_REQUEST = 100;
    private static final String PREFS_NAME = "HealthScannerPrefs";

    // UI Elements
    private ImageView scanIcon;
    private RequestQueue requestQueue;
    private Vibrator vibrator;
    private AuthManager authManager;
    private SyncManager syncManager;
    private DarkModeManager darkModeManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check if activity is in valid state
        if (isFinishing() || isDestroyed()) {
            Log.w(TAG, "Activity is finishing or destroyed, skipping initialization");
            return;
        }

        try {
            setContentView(R.layout.activity_home_enhanced);
            Log.d(TAG, "Content view set successfully");

            // Initialize Authentication Manager
            authManager = AuthManager.getInstance(this);
            
            // Initialize Sync Manager
            syncManager = SyncManager.getInstance(this);
            
            // Initialize Dark Mode Manager and apply user preference
            darkModeManager = DarkModeManager.getInstance(this);
            darkModeManager.applyUserPreference();

            // Check if coming from Google Sign-In or other authenticated source
            Intent intent = getIntent();
            boolean fromGoogleSignIn = intent != null && intent.getBooleanExtra("from_google_signin", false);
            boolean bypassAuthCheck = intent != null && intent.getBooleanExtra("bypass_auth_check", false);
            boolean userAuthenticated = intent != null && intent.getBooleanExtra("user_authenticated", false);
            boolean fromNavigation = intent != null && intent.getBooleanExtra("from_navigation", false);

            if (fromGoogleSignIn || bypassAuthCheck || userAuthenticated) {
                Log.d(TAG, "=== ENTERING MAIN APP FROM AUTHENTICATED SOURCE ===");
                Log.d(TAG, "User successfully authenticated, loading main app interface...");
                
                // Immediately sync data after successful authentication
                if (syncManager != null) {
                    syncManager.syncImmediately(new SyncManager.SyncCallback() {
                        @Override
                        public void onSuccess() {
                            Log.d(TAG, "Initial sync completed after authentication");
                            
                            // Check if this is a Google user and ensure Firebase profile exists
                            ensureGoogleUserInFirebase();
                        }
                        
                        @Override
                        public void onFailure(String error) {
                            Log.w(TAG, "Initial sync failed after authentication: " + error);
                        }
                    });
                }
            } else if (fromNavigation) {
                // Coming from bottom navigation - check auth but don't redirect if failed
                // This prevents redirect loops during navigation
                if (!authManager.isUserAuthenticated()) {
                    Log.d(TAG, "User not authenticated during navigation, showing login prompt");
                    showAuthenticationRequiredDialog();
                    return;
                }
            } else {
                // Check authentication state normally for direct app launches
                if (!authManager.isUserAuthenticated()) {
                    Log.d(TAG, "User not authenticated, enabling test mode for development");
                    authManager.enableTestMode();
                    // Continue with app initialization
                    
                    // Also create test data immediately
                    android.os.Handler handler = new android.os.Handler();
                    handler.postDelayed(() -> createTestDataInFirebase(), 1000);
                }
            }

            Log.d(TAG, "Starting main app initialization...");

            // Check and restore Firebase authentication state
            checkFirebaseAuthState();
            
            // Check if this is a new user from Google Sign-In or successful login
            checkAndWelcomeUser();

            // Initialize all main app components
            initializeComponents();

            Log.d(TAG, "=== MAIN APP LOADED SUCCESSFULLY ===");
            Log.d(TAG, "User is now inside the main application interface");

        } catch (Exception e) {
            Log.e(TAG, "Error loading main app: " + e.getMessage(), e);

            // Try to recover by showing a basic interface
            try {
                // Show error message to user
                android.widget.Toast.makeText(this,
                        "Loading main app... Please wait.",
                        android.widget.Toast.LENGTH_SHORT).show();

                // Try to initialize basic components only
                initializeViews();
                initializeBottomNavigation();

                Log.d(TAG, "Main app loaded with basic interface after error recovery");

            } catch (Exception e2) {
                Log.e(TAG, "Failed to recover main app interface", e2);
                if (!isFinishing()) {
                    finish();
                }
            }
        }
    }

    private void initializeComponents() {
        try {
            // Initialize basic components
            initializeViews();
            initializeHapticFeedback();
            setupClickListeners();
            initializeBottomNavigation();
            setupRecentScansRecyclerView();
            updateStatsCards();

            // Initialize request queue
            requestQueue = Volley.newRequestQueue(this);

            // Check if we should start scanner immediately
            if (getIntent().getBooleanExtra("start_scanner", false)) {
                launchVerticalScanner();
            }
            
            // Start automatic sync service
            startAutoSync();

        } catch (Exception e) {
            Log.e(TAG, "Error initializing components: " + e.getMessage(), e);
        }
    }

    private void initializeViews() {
        try {
            // Enhanced Home Layout Views
            TextView welcomeText = findViewById(R.id.welcomeText);
            TextView totalScansNumber = findViewById(R.id.totalScansNumber);
            TextView healthScoreNumber = findViewById(R.id.healthScoreNumber);
            TextView savedItemsNumber = findViewById(R.id.savedItemsNumber);
            TextView healthEmoji = findViewById(R.id.healthEmoji);

            // Search and scan elements
            scanIcon = findViewById(R.id.scanIcon);

            // Set personalized welcome text with Google account integration
            if (welcomeText != null) {
                String realUserName = getRealUserName();
                String firstName = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getString("current_user_first_name", "");
                String authProvider = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getString("auth_provider", "");
                boolean freshSignIn = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getBoolean("fresh_google_signin", false);
                
                if (freshSignIn && !firstName.isEmpty()) {
                    // Special welcome for fresh Google sign-in
                    welcomeText.setText("Welcome back, " + firstName + "! ✨");
                } else if (!firstName.isEmpty()) {
                    // Use first name for more personal feel
                    welcomeText.setText("Hi, " + firstName + "! 👋");
                } else if (realUserName != null && !realUserName.isEmpty()) {
                    // Fallback to full name
                    welcomeText.setText("Hi, " + realUserName + " 👋");
                } else {
                    // Default greeting
                    welcomeText.setText("Hi there! 👋");
                }
            }

            // Set REAL stats (no artificial data)
            setRealUserStats(totalScansNumber, healthScoreNumber, savedItemsNumber, healthEmoji);

            Log.d(TAG, "Views initialized with real user data");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing views: " + e.getMessage(), e);
        }
    }

    /**
     * Get real user name from Google Sign-In data stored in SharedPreferences
     */
    private String getRealUserName() {
        try {
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

            // Try display name first
            String displayName = prefs.getString("current_user_name", "");
            if (!displayName.isEmpty()) {
                return displayName;
            }

            // Try first name
            String firstName = prefs.getString("current_user_first_name", "");
            if (!firstName.isEmpty()) {
                return firstName;
            }

            // Fallback to email prefix
            String email = prefs.getString("current_user_email", "");
            if (!email.isEmpty() && email.contains("@")) {
                return email.split("@")[0];
            }

            return null;
        } catch (Exception e) {
            Log.e(TAG, "Error getting real user name", e);
            return null;
        }
    }

    /**
     * Set real user statistics (no artificial data)
     */
    private void setRealUserStats(TextView totalScansNumber, TextView healthScoreNumber,
            TextView savedItemsNumber, TextView healthEmoji) {
        try {
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

            // Get real scan count from actual user scans
            int realScanCount = getRealScanCount(prefs);
            if (totalScansNumber != null) {
                totalScansNumber.setText(String.valueOf(realScanCount));
            }

            // Get real health score from actual scans
            double realHealthScore = getRealHealthScore(prefs);
            if (healthScoreNumber != null) {
                if (realScanCount > 0) {
                    healthScoreNumber.setText(String.format("%.1f", realHealthScore));
                } else {
                    healthScoreNumber.setText("--");
                }
            }

            // Get real saved items count
            int realSavedCount = getRealSavedItemsCount(prefs);
            if (savedItemsNumber != null) {
                savedItemsNumber.setText(String.valueOf(realSavedCount));
            }

            // Set appropriate emoji based on real data
            if (healthEmoji != null) {
                if (realScanCount > 0) {
                    healthEmoji.setText(getHealthEmoji(realHealthScore));
                } else {
                    healthEmoji.setText("🌟"); // New user emoji
                }
            }

            Log.d(TAG, "Real stats - Scans: " + realScanCount + ", Health Score: " + realHealthScore + ", Saved: "
                    + realSavedCount);

        } catch (Exception e) {
            Log.e(TAG, "Error setting real user stats", e);
            // Fallback to zeros if error
            if (totalScansNumber != null)
                totalScansNumber.setText("0");
            if (healthScoreNumber != null)
                healthScoreNumber.setText("--");
            if (savedItemsNumber != null)
                savedItemsNumber.setText("0");
            if (healthEmoji != null)
                healthEmoji.setText("🌟");
        }
    }

    /**
     * Get real scan count from user's actual scan history
     */
    private int getRealScanCount(SharedPreferences prefs) {
        try {
            String scanHistoryJson = prefs.getString("recent_scans", "[]");
            org.json.JSONArray scanArray = new org.json.JSONArray(scanHistoryJson);
            return scanArray.length();
        } catch (Exception e) {
            Log.e(TAG, "Error getting real scan count", e);
            return 0;
        }
    }

    /**
     * Get real health score from user's actual scans
     */
    private double getRealHealthScore(SharedPreferences prefs) {
        try {
            String scanHistoryJson = prefs.getString("recent_scans", "[]");
            org.json.JSONArray scanArray = new org.json.JSONArray(scanHistoryJson);

            if (scanArray.length() == 0) {
                return 0.0;
            }

            double totalScore = 0;
            int validScans = 0;

            for (int i = 0; i < scanArray.length(); i++) {
                org.json.JSONObject scan = scanArray.getJSONObject(i);
                if (scan.has("healthScore")) {
                    totalScore += scan.getDouble("healthScore");
                    validScans++;
                }
            }

            return validScans > 0 ? totalScore / validScans : 0.0;

        } catch (Exception e) {
            Log.e(TAG, "Error getting real health score", e);
            return 0.0;
        }
    }

    /**
     * Get real saved items count from user's favorites
     */
    private int getRealSavedItemsCount(SharedPreferences prefs) {
        try {
            String savedItemsJson = prefs.getString("user_saved_items", "[]");
            org.json.JSONArray savedArray = new org.json.JSONArray(savedItemsJson);
            return savedArray.length();
        } catch (Exception e) {
            Log.e(TAG, "Error getting real saved items count", e);
            return 0;
        }
    }

    /**
     * Show authentication required dialog instead of redirecting
     */
    private void showAuthenticationRequiredDialog() {
        try {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Authentication Required")
                    .setMessage("Please sign in to access your health scanner data.")
                    .setPositiveButton("Sign In", (dialog, which) -> {
                        authManager.navigateToLogin(this);
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> {
                        // Go back to previous activity or close app
                        finish();
                    })
                    .setCancelable(false)
                    .show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing authentication dialog", e);
            // Fallback to redirect
            authManager.navigateToLogin(this);
        }
    }

    /**
     * Check and restore Firebase authentication state
     */
    private void checkFirebaseAuthState() {
        try {
            com.google.firebase.auth.FirebaseAuth firebaseAuth = com.google.firebase.auth.FirebaseAuth.getInstance();
            com.google.firebase.auth.FirebaseUser currentUser = firebaseAuth.getCurrentUser();
            
            if (currentUser != null) {
                Log.d(TAG, "Firebase user found: " + currentUser.getEmail());
                
                // Refresh the token to ensure it's still valid
                currentUser.getIdToken(true)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Firebase token is valid and refreshed");
                            
                            // Update SharedPreferences with current Firebase user data
                            SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
                            editor.putBoolean("is_logged_in", true);
                            editor.putLong("login_timestamp", System.currentTimeMillis());
                            editor.putString("current_user_email", currentUser.getEmail());
                            editor.putString("current_user_name", currentUser.getDisplayName());
                            editor.putString("current_user_id", currentUser.getUid());
                            
                            if (currentUser.getProviderData() != null && !currentUser.getProviderData().isEmpty()) {
                                editor.putString("auth_provider", currentUser.getProviderData().get(0).getProviderId());
                            }
                            
                            if (currentUser.getPhotoUrl() != null) {
                                editor.putString("current_user_photo", currentUser.getPhotoUrl().toString());
                            }
                            
                            editor.apply();
                            Log.d(TAG, "SharedPreferences updated with current Firebase user data");
                            
                        } else {
                            Log.w(TAG, "Firebase token refresh failed: " + task.getException());
                            // Token is invalid, user needs to sign in again
                            authManager.clearAuthState();
                        }
                    });
                    
            } else {
                Log.d(TAG, "No Firebase user found");
                // Check if SharedPreferences thinks user is logged in but Firebase doesn't
                SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                boolean isLoggedIn = prefs.getBoolean("is_logged_in", false);
                if (isLoggedIn) {
                    Log.w(TAG, "SharedPreferences shows logged in but no Firebase user - clearing auth state");
                    authManager.clearAuthState();
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error checking Firebase auth state: " + e.getMessage(), e);
        }
    }

    /**
     * Check if this is a new user or successful login and show appropriate welcome
     * message
     */
    private void checkAndWelcomeUser() {
        try {
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            boolean isFirstLaunch = prefs.getBoolean("is_first_launch_after_signin", true);

            if (isFirstLaunch) {
                String userName = getRealUserName();
                if (userName != null && !userName.isEmpty()) {
                    // Log welcome for new user (no toast)
                    Log.d(TAG, "New user signed up: " + userName);
                }

                // Mark as no longer first launch
                prefs.edit().putBoolean("is_first_launch_after_signin", false).apply();
            }

            // Check if coming from successful login
            Intent intent = getIntent();
            boolean loginSuccess = intent != null && intent.getBooleanExtra("login_success", false);
            boolean showWelcome = intent != null && intent.getBooleanExtra("show_welcome", false);

            if (loginSuccess && showWelcome) {
                String userName = getRealUserName();
                if (userName != null && !userName.isEmpty()) {
                    // Log successful login (no toast)
                    Log.d(TAG, "User successfully logged in: " + userName);
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Error checking user welcome status", e);
        }
    }

    private void initializeHapticFeedback() {
        try {
            vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            Log.d(TAG, "Haptic feedback initialized");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing haptic feedback: " + e.getMessage(), e);
        }
    }

    private void setupClickListeners() {
        try {
            // Search card click - Launch vertical scanner
            View searchCard = findViewById(R.id.searchCard);
            if (searchCard != null) {
                searchCard.setOnClickListener(v -> {
                    performHapticFeedback();
                    animateView(v);
                    launchVerticalScanner();
                });
            }

            // Scan icon click - Launch vertical scanner
            if (scanIcon != null) {
                scanIcon.setOnClickListener(v -> {
                    performHapticFeedback();
                    animateView(v);
                    launchVerticalScanner();
                });
            }

            // View all scans click (if the view exists)
            try {
                TextView viewAllScans = findViewById(R.id.viewAllScans);
                if (viewAllScans != null) {
                    viewAllScans.setOnClickListener(v -> {
                        performHapticFeedback();
                        Intent intent = new Intent(this, HistoryActivity.class);
                        startActivity(intent);
                    });
                }
            } catch (Exception e) {
                Log.d(TAG, "viewAllScans view not found in layout");
            }

            // Floating Action Button for quick scan
            com.google.android.material.floatingactionbutton.FloatingActionButton fabScan = findViewById(R.id.fab_scan);
            if (fabScan != null) {
                fabScan.setOnClickListener(v -> {
                    performHapticFeedback();
                    animateView(v);
                    launchVerticalScanner();
                });
            }

            Log.d(TAG, "Click listeners setup successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error setting up click listeners: " + e.getMessage(), e);
        }
    }

    private void setupRecentScansRecyclerView() {
        try {
            RecyclerView recentScansRecyclerView = findViewById(R.id.recentScansRecyclerView);
            if (recentScansRecyclerView != null) {
                LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL,
                        false);
                recentScansRecyclerView.setLayoutManager(layoutManager);

                // Load ONLY real user scan history (no fake data)
                java.util.List<RecentScansAdapter.ScanItem> scanItems = loadRealScanHistory();

                if (scanItems.isEmpty()) {
                    // Hide recent scans section completely if no real data
                    recentScansRecyclerView.setVisibility(View.GONE);
                    Log.d(TAG, "No real scan history found - hiding recent scans section");
                } else {
                    // Show recent scans section with real data
                    recentScansRecyclerView.setVisibility(View.VISIBLE);

                    RecentScansAdapter adapter = new RecentScansAdapter(this, scanItems);
                    adapter.setOnItemClickListener(item -> {
                        Log.d(TAG, "Clicked on real scan: " + item.getProductName());
                        // Navigate to product details with real data
                        navigateToProductDetails(item);
                    });
                    recentScansRecyclerView.setAdapter(adapter);

                    Log.d(TAG, "Showing " + scanItems.size() + " real scan items");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up RecyclerView: " + e.getMessage(), e);
        }
    }

    /**
     * Navigate to product details with real scan data
     */
    private void navigateToProductDetails(RecentScansAdapter.ScanItem item) {
        try {
            Intent intent = new Intent(this, ProductDetailsEnhancedActivity.class);
            intent.putExtra("product_name", item.getProductName());
            intent.putExtra("calories", item.getCalories());
            intent.putExtra("health_score", item.getHealthScore());
            intent.putExtra("from_recent_scans", true);
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error navigating to product details", e);
        }
    }

    private java.util.List<RecentScansAdapter.ScanItem> loadRealScanHistory() {
        java.util.List<RecentScansAdapter.ScanItem> items = new java.util.ArrayList<>();
        try {
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

            // Load ONLY real user scan history (no artificial data)
            String scanHistoryJson = prefs.getString("recent_scans", "[]");

            org.json.JSONArray scanArray = new org.json.JSONArray(scanHistoryJson);

            // Show only the most recent 4 scans that the user actually performed
            int recentCount = Math.min(scanArray.length(), 4);

            for (int i = scanArray.length() - 1; i >= scanArray.length() - recentCount; i--) {
                org.json.JSONObject scan = scanArray.getJSONObject(i);

                // Only add if it has required fields (real scan data)
                if (scan.has("productName") && scan.has("calories") && scan.has("healthScore")) {
                    String productName = scan.getString("productName");
                    int calories = scan.optInt("calories", 0);
                    double healthScore = scan.optDouble("healthScore", 0.0);

                    items.add(new RecentScansAdapter.ScanItem(productName, calories, healthScore));
                }
            }

            Log.d(TAG, "Loaded " + items.size() + " real scan items from user history");

        } catch (Exception e) {
            Log.e(TAG, "Error loading real scan history: " + e.getMessage(), e);
        }

        return items;
    }

    private void updateStatsCards() {
        try {
            updateHealthScore();
            TextView savedItemsNumber = findViewById(R.id.savedItemsNumber);
            if (savedItemsNumber != null) {
                savedItemsNumber.setText("0");
            }
            Log.d(TAG, "Stats cards updated successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error updating stats cards: " + e.getMessage(), e);
        }
    }

    private void updateHealthScore() {
        try {
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

            TextView healthScoreNumber = findViewById(R.id.healthScoreNumber);
            TextView healthEmoji = findViewById(R.id.healthEmoji);
            TextView totalScansNumber = findViewById(R.id.totalScansNumber);

            // Use REAL user data only
            double realHealthScore = getRealHealthScore(prefs);
            int realScanCount = getRealScanCount(prefs);

            // Update total scans with real count
            if (totalScansNumber != null) {
                totalScansNumber.setText(String.valueOf(realScanCount));
            }

            // Update health score with real data
            if (healthScoreNumber != null) {
                if (realScanCount > 0) {
                    healthScoreNumber.setText(String.format("%.1f", realHealthScore));
                } else {
                    healthScoreNumber.setText("--");
                }
            }

            // Update emoji based on real health score
            if (healthEmoji != null) {
                if (realScanCount > 0) {
                    healthEmoji.setText(getHealthEmoji(realHealthScore));
                } else {
                    healthEmoji.setText("🌟"); // New user - no scans yet
                }
            }

            Log.d(TAG, "Updated with real data - Scans: " + realScanCount + ", Health Score: " + realHealthScore);

        } catch (Exception e) {
            Log.e(TAG, "Error updating health score with real data: " + e.getMessage(), e);
        }
    }

    private String getHealthEmoji(double score) {
        if (score >= 8.0)
            return "😄";
        else if (score >= 6.0)
            return "😊";
        else if (score >= 4.0)
            return "😐";
        else if (score >= 2.0)
            return "😕";
        else
            return "😞";
    }

    @Override
    protected int getCurrentNavigationItemId() {
        if (getIntent().getBooleanExtra("start_scanner", false)) {
            return R.id.nav_scan;
        }
        return R.id.nav_home;
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (isFinishing() || isDestroyed()) {
            Log.w(TAG, "Activity is finishing or destroyed, skipping onResume operations");
            return;
        }

        try {
            updateStatsCards();
        } catch (Exception e) {
            Log.e(TAG, "Error in onResume: " + e.getMessage(), e);
        }
    }

    // Camera permission methods
    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.CAMERA }, CAMERA_PERMISSION_REQUEST);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startBarcodeScanner();
            } else {
                Log.d(TAG, "Camera permission denied");
            }
        }
    }

    private void startBarcodeScanner() {
        try {
            IntentIntegrator integrator = new IntentIntegrator(this);
            integrator.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES);
            integrator.setPrompt("Scan a product barcode");
            integrator.setCameraId(0);
            integrator.setBeepEnabled(false);
            integrator.setBarcodeImageEnabled(true);
            integrator.initiateScan();
            Log.d(TAG, "Barcode scanner started");
        } catch (Exception e) {
            Log.e(TAG, "Error starting barcode scanner: " + e.getMessage(), e);
        }
    }

    private void launchVerticalScanner() {
        try {
            Intent intent = new Intent(this, VerticalScannerActivity.class);
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error launching vertical scanner: " + e.getMessage(), e);
            // Fallback to old scanner
            startBarcodeScanner();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() == null) {
                Log.d(TAG, "Scan cancelled");
            } else {
                String barcode = result.getContents();
                Log.d(TAG, "Scanned barcode: " + barcode);
                // Navigate to enhanced product details activity
                Intent intent = new Intent(this, ProductDetailsEnhancedActivity.class);
                intent.putExtra("barcode", barcode);
                startActivity(intent);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    // Helper methods
    private void performHapticFeedback() {
        try {
            if (vibrator != null && vibrator.hasVibrator()) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(50);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error performing haptic feedback: " + e.getMessage(), e);
        }
    }

    private void animateView(View view) {
        try {
            Animation scaleAnimation = AnimationUtils.loadAnimation(this, R.anim.scale_bounce);
            view.startAnimation(scaleAnimation);
        } catch (Exception e) {
            Log.e(TAG, "Error animating view: " + e.getMessage(), e);
        }
    }
    
    /**
     * Start automatic sync service
     */
    private void startAutoSync() {
        try {
            if (authManager.isUserAuthenticated()) {
                Intent syncIntent = new Intent(this, AutoSyncService.class);
                startService(syncIntent);
                Log.d(TAG, "Auto-sync service started");
                
                // Create test data to verify Firebase connection
                createTestDataInFirebase();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error starting auto-sync service: " + e.getMessage(), e);
        }
    }
    
    /**
     * Create test data in Firebase to verify connection
     */
    private void createTestDataInFirebase() {
        try {
            // Check authentication status
            boolean isAuthenticated = authManager.isUserAuthenticated();
            String userId = authManager.getCurrentUserId();
            
            Log.d(TAG, "🔍 FIREBASE TEST DEBUG:");
            Log.d(TAG, "   - Is Authenticated: " + isAuthenticated);
            Log.d(TAG, "   - User ID: " + (userId != null ? userId : "NULL"));
            
            // Check Firebase Auth user
            com.google.firebase.auth.FirebaseUser firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
            if (firebaseUser != null) {
                Log.d(TAG, "   - Firebase User: " + firebaseUser.getEmail());
                Log.d(TAG, "   - Firebase UID: " + firebaseUser.getUid());
                
                // Use Firebase UID if local user ID is null
                if (userId == null || userId.isEmpty()) {
                    userId = firebaseUser.getUid();
                    Log.d(TAG, "   - Using Firebase UID as user ID: " + userId);
                }
            } else {
                Log.w(TAG, "   - No Firebase user found");
            }
            
            if (userId != null && !userId.isEmpty()) {
                final String finalUserId = userId; // Make final for lambda
                
                // Create comprehensive test data
                Map<String, Object> testData = new HashMap<>();
                testData.put("testMessage", "Hello from Health Scanner App!");
                testData.put("timestamp", new java.util.Date());
                testData.put("appVersion", "1.0.0");
                testData.put("userId", finalUserId);
                testData.put("deviceInfo", android.os.Build.MODEL + " " + android.os.Build.VERSION.RELEASE);
                testData.put("isAuthenticated", isAuthenticated);
                SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                testData.put("authProvider", prefs.getString("auth_provider", "unknown"));
                testData.put("userEmail", prefs.getString("current_user_email", "unknown"));
                testData.put("userName", prefs.getString("current_user_name", "unknown"));
                
                Log.d(TAG, "🚀 Creating test data for user: " + finalUserId);
                
                // Get Firebase instance and create test document
                com.google.firebase.firestore.FirebaseFirestore db = com.google.firebase.firestore.FirebaseFirestore.getInstance();
                
                db.collection("users")
                    .document(finalUserId)
                    .set(testData, com.google.firebase.firestore.SetOptions.merge())
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "✅ TEST DATA CREATED IN FIREBASE!");
                        Log.d(TAG, "🔍 Check Firebase Console: https://console.firebase.google.com/project/nure-70d49/firestore/data/users/" + finalUserId);
                        Log.d(TAG, "📍 Direct link: https://console.firebase.google.com/project/nure-70d49/firestore/data/~2Fusers~2F" + finalUserId);
                        
                        android.widget.Toast.makeText(this, 
                            "✅ Test data created! User: " + finalUserId, 
                            android.widget.Toast.LENGTH_LONG).show();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "❌ FAILED TO CREATE TEST DATA IN FIREBASE: " + e.getMessage(), e);
                        Log.e(TAG, "   - Error details: " + e.getClass().getSimpleName());
                        if (e.getCause() != null) {
                            Log.e(TAG, "   - Cause: " + e.getCause().getMessage());
                        }
                        
                        android.widget.Toast.makeText(this, 
                            "❌ Firebase failed: " + e.getMessage(), 
                            android.widget.Toast.LENGTH_LONG).show();
                    });
                    
            } else {
                Log.e(TAG, "❌ CANNOT CREATE TEST DATA - NO USER ID AVAILABLE");
                Log.e(TAG, "   - Check authentication flow");
                Log.e(TAG, "   - Make sure user is signed in");
                
                android.widget.Toast.makeText(this, 
                    "❌ No user ID - please sign in first", 
                    android.widget.Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ ERROR CREATING TEST DATA: " + e.getMessage(), e);
            android.widget.Toast.makeText(this, 
                "❌ Test data error: " + e.getMessage(), 
                android.widget.Toast.LENGTH_LONG).show();
        }
    }
    
    /**
     * Ensure Google users are properly stored in Firebase database
     */
    private void ensureGoogleUserInFirebase() {
        try {
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            String authProvider = prefs.getString("auth_provider", "");
            String userId = authManager.getCurrentUserId();
            boolean firebaseProfileCreated = prefs.getBoolean("firebase_profile_created", false);
            
            if ("google.com".equals(authProvider) && userId != null && !firebaseProfileCreated) {
                Log.d(TAG, "🔍 Google user detected without Firebase profile, creating...");
                
                // Create comprehensive user data from stored Google account info
                java.util.Map<String, Object> userData = new java.util.HashMap<>();
                userData.put("email", prefs.getString("current_user_email", ""));
                userData.put("displayName", prefs.getString("current_user_name", ""));
                userData.put("firstName", prefs.getString("current_user_first_name", ""));
                userData.put("lastName", prefs.getString("current_user_last_name", ""));
                userData.put("photoUrl", prefs.getString("current_user_photo", ""));
                userData.put("userId", userId);
                userData.put("authProvider", "google.com");
                userData.put("accountType", "Google Account");
                userData.put("isGoogleAccount", true);
                userData.put("emailVerified", true);
                userData.put("createdAt", new java.util.Date());
                userData.put("lastLoginAt", new java.util.Date());
                userData.put("appVersion", "1.0.0");
                userData.put("platform", "Android");
                
                // Initialize health app defaults
                userData.put("totalScans", 0);
                userData.put("healthyChoices", 0);
                userData.put("averageHealthScore", 0.0);
                userData.put("notificationsEnabled", true);
                userData.put("darkModeEnabled", darkModeManager.isDarkModeEnabled());
                userData.put("scanHistory", "[]");
                userData.put("healthConcerns", new java.util.ArrayList<String>());
                userData.put("dietaryPreferences", new java.util.ArrayList<String>());
                
                // Save to Firebase
                com.example.healthscanner.database.FirebaseManager firebaseManager = 
                    com.example.healthscanner.database.FirebaseManager.getInstance();
                    
                firebaseManager.syncCompleteUserData(userId, userData, 
                    new com.example.healthscanner.database.FirebaseManager.OperationCallback() {
                        @Override
                        public void onSuccess() {
                            Log.d(TAG, "✅ Google user profile created in Firebase from MainActivity!");
                            Log.d(TAG, "🔍 Check: https://console.firebase.google.com/project/nure-70d49/firestore/data/users/" + userId);
                            
                            prefs.edit()
                                .putBoolean("firebase_profile_created", true)
                                .putLong("firebase_profile_timestamp", System.currentTimeMillis())
                                .apply();
                        }
                        
                        @Override
                        public void onFailure(String error) {
                            Log.e(TAG, "❌ Failed to create Google user profile in Firebase: " + error);
                        }
                    });
                    
            } else if ("google.com".equals(authProvider) && firebaseProfileCreated) {
                Log.d(TAG, "✅ Google user already has Firebase profile");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error ensuring Google user in Firebase: " + e.getMessage(), e);
        }
    }

}