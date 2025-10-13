package com.example.healthscanner;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * Profile Activity for user profile management
 * Shows user information and app settings
 */
public class ProfileActivity extends AppCompatActivity {
    
    private static final String TAG = "ProfileActivity";
    
    private BottomNavigationView bottomNavigation;
    private TextView profileContent;
    private Button logoutButton;
    private AuthManager authManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply theme

        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        
        // Initialize AuthManager
        authManager = AuthManager.getInstance(this);
        
        // Check authentication
        if (!authManager.isUserAuthenticated()) {
            authManager.navigateToLogin(this);
            return;
        }
        
        initializeViews();
        setupBottomNavigation();
        loadUserProfile();
        setupClickListeners();
    }
    
    private void initializeViews() {
        bottomNavigation = findViewById(R.id.bottom_navigation);
        profileContent = findViewById(R.id.profile_content);
        logoutButton = findViewById(R.id.logout_button);
    }
    
    private void setupBottomNavigation() {
        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.nav_profile);
            
            bottomNavigation.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_scan) {
                    Intent scanIntent = new Intent(this, MainActivity.class);
                    startActivity(scanIntent);
                    finish();
                    return true;
                } else if (itemId == R.id.nav_history) {
                    Intent historyIntent = new Intent(this, HistoryActivity.class);
                    startActivity(historyIntent);
                    finish();
                    return true;
                } else if (itemId == R.id.nav_profile) {
                    // Already on profile page
                    return true;
                }
                return false;
            });
        }
    }
    
    private void loadUserProfile() {
        if (profileContent != null) {
            String userName = authManager.getCurrentUserName();
            String userEmail = authManager.getCurrentUserEmail();
            String authProvider = authManager.getAuthProvider();
            
            StringBuilder profileText = new StringBuilder();
            profileText.append("👤 User Profile\n\n");
            
            if (userName != null) {
                profileText.append("Name: ").append(userName).append("\n");
            }
            
            if (userEmail != null) {
                profileText.append("Email: ").append(userEmail).append("\n");
            }
            
            profileText.append("Auth Provider: ").append(authProvider).append("\n\n");
            
            if (authManager.isTestMode()) {
                profileText.append("🧪 Test Mode Active\n");
                profileText.append("You are using test credentials for development.\n\n");
            }
            
            profileText.append("App Version: 1.0\n");
            profileText.append("Build: Debug");
            
            profileContent.setText(profileText.toString());
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