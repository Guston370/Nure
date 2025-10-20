package com.example.healthscanner;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * Test Activity to verify Firebase connection and data creation
 */
public class FirebaseTestActivity extends AppCompatActivity {
    
    private static final String TAG = "FirebaseTest";
    
    private TextView statusText;
    private Button testButton;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Create simple layout
        createTestLayout();
        
        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        
        // Views are already assigned in createTestLayout()
        // statusText and testButton are set in createTestLayout()
        
        testButton.setOnClickListener(v -> testFirebaseConnection());
        
        updateStatus("Firebase Test Activity Ready");
    }
    
    private void createTestLayout() {
        // Create a simple layout programmatically
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(50, 50, 50, 50);
        
        statusText = new TextView(this);
        statusText.setText("Firebase Test");
        statusText.setTextSize(16);
        statusText.setPadding(0, 0, 0, 30);
        
        testButton = new Button(this);
        testButton.setText("Test Firebase Connection");
        
        layout.addView(statusText);
        layout.addView(testButton);
        
        setContentView(layout);
    }
    
    private void testFirebaseConnection() {
        updateStatus("Testing Firebase connection...");
        
        // Check authentication
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            updateStatus("❌ No authenticated user found");
            return;
        }
        
        String userId = currentUser.getUid();
        updateStatus("✅ User authenticated: " + userId);
        
        // Create test data
        Map<String, Object> testData = new HashMap<>();
        testData.put("message", "Hello from Firebase Test!");
        testData.put("timestamp", new java.util.Date());
        testData.put("userId", userId);
        testData.put("email", currentUser.getEmail());
        testData.put("testNumber", Math.random() * 1000);
        
        // Write to Firestore
        db.collection("users")
            .document(userId)
            .collection("test")
            .add(testData)
            .addOnSuccessListener(documentReference -> {
                String message = "✅ SUCCESS! Data written to Firebase!\n" +
                               "Document ID: " + documentReference.getId() + "\n" +
                               "Collection: users/" + userId + "/test\n" +
                               "Check Firebase Console: https://console.firebase.google.com/project/nure-70d49/firestore";
                
                updateStatus(message);
                Log.d(TAG, message);
                
                Toast.makeText(this, "Data successfully written to Firebase!", Toast.LENGTH_LONG).show();
            })
            .addOnFailureListener(e -> {
                String error = "❌ FAILED to write to Firebase: " + e.getMessage();
                updateStatus(error);
                Log.e(TAG, error, e);
                
                Toast.makeText(this, "Firebase write failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            });
    }
    
    private void updateStatus(String message) {
        if (statusText != null) {
            statusText.setText(message);
        }
        Log.d(TAG, message);
    }
}