package com.example.healthscanner;

import android.animation.ValueAnimator;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;

import com.example.healthscanner.AuthManager;
import com.example.healthscanner.HistoryActivity;
import com.example.healthscanner.adapters.SavedProductsAdapter;

/**
 * Profile Activity for user profile management
 * Shows user information and app settings
 */
public class ProfileActivity extends BaseActivity {
    
    private static final String TAG = "ProfileActivity";
    
    private BottomNavigationView bottomNavigation;
    private TextView userName;
    private TextView userEmail;
    private MaterialButton logoutButton;
    private AuthManager authManager;
    private ImageView profileAvatar;
    private MaterialCardView profileHeaderCard;
    private MaterialCardView healthStatsCard;
    private MaterialCardView savedItemsCard;
    private SwitchMaterial darkModeSwitch;
    private TextView totalScans;
    private TextView healthyChoices;
    private TextView unhealthyProducts;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply theme

        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_enhanced);
        
        // Initialize AuthManager
        authManager = AuthManager.getInstance(this);
        
        // Check authentication
        if (!authManager.isUserAuthenticated()) {
            authManager.navigateToLogin(this);
            return;
        }
        
        initializeViews();
        initializeBottomNavigation();
        loadUserProfile();
        setupClickListeners();
    }
    
    private void initializeViews() {
        bottomNavigation = findViewById(R.id.bottom_navigation);
        userName = findViewById(R.id.user_name);
        userEmail = findViewById(R.id.user_email);
        logoutButton = findViewById(R.id.logout_button);
        profileAvatar = findViewById(R.id.profile_avatar);
        profileHeaderCard = findViewById(R.id.profile_header_card);
        healthStatsCard = findViewById(R.id.health_stats_card);
        savedItemsCard = findViewById(R.id.saved_items_card);
        darkModeSwitch = findViewById(R.id.switch_dark_mode);
        totalScans = findViewById(R.id.total_scans);
        healthyChoices = findViewById(R.id.healthy_choices);
        unhealthyProducts = findViewById(R.id.unhealthy_products);

        // Apply fade-in animations
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        profileHeaderCard.startAnimation(fadeIn);
        healthStatsCard.startAnimation(fadeIn);
        savedItemsCard.startAnimation(fadeIn);
    }
    
    @Override
    protected int getCurrentNavigationItemId() {
        return R.id.nav_profile;
    }
    
    private void loadUserProfile() {
        String name = authManager.getCurrentUserName();
        String email = authManager.getCurrentUserEmail();
        
        if (userName != null) {
            userName.setText(name != null ? name : getString(R.string.guest_user));
            userName.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));
        }
        
        if (userEmail != null) {
            userEmail.setText(email != null ? email : getString(R.string.no_email));
            userEmail.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));
        }

        // Sample stats (replace with real data from your backend)
        if (totalScans != null) {
            totalScans.setText("0");
        }
        if (healthyChoices != null) {
            healthyChoices.setText("0");
        }
        if (unhealthyProducts != null) {
            unhealthyProducts.setText("0");
        }
    }
    
    private void setupClickListeners() {
        if (logoutButton != null) {
            logoutButton.setOnClickListener(v -> {
                // Sign out user
                authManager.signOut(this);
            });
        }
    }
}