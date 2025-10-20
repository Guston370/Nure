package com.example.healthscanner.services;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;

import com.example.healthscanner.AuthManager;
import com.example.healthscanner.database.SyncManager;

/**
 * Background service for automatic data synchronization
 * Runs more aggressively to ensure data is always synced
 */
public class AutoSyncService extends Service {
    
    private static final String TAG = "AutoSyncService";
    private static final long SYNC_INTERVAL = 30 * 1000; // 30 seconds
    
    private SyncManager syncManager;
    private AuthManager authManager;
    private Handler handler;
    private Runnable syncRunnable;
    private boolean isRunning = false;
    
    @Override
    public void onCreate() {
        super.onCreate();
        syncManager = SyncManager.getInstance(this);
        authManager = AuthManager.getInstance(this);
        handler = new Handler(Looper.getMainLooper());
        Log.d(TAG, "AutoSyncService created with aggressive sync");
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "AutoSyncService started");
        
        if (!isRunning && authManager.isUserAuthenticated()) {
            startPeriodicSync();
        } else if (!authManager.isUserAuthenticated()) {
            Log.d(TAG, "User not authenticated, stopping sync service");
            stopSelf();
        }
        
        return START_STICKY; // Restart if killed
    }
    
    private void startPeriodicSync() {
        isRunning = true;
        
        syncRunnable = new Runnable() {
            @Override
            public void run() {
                if (authManager.isUserAuthenticated()) {
                    performSync();
                    // Schedule next sync
                    handler.postDelayed(this, SYNC_INTERVAL);
                } else {
                    Log.d(TAG, "User no longer authenticated, stopping periodic sync");
                    stopPeriodicSync();
                    stopSelf();
                }
            }
        };
        
        // Start first sync immediately
        handler.post(syncRunnable);
        Log.d(TAG, "Started periodic sync every " + (SYNC_INTERVAL / 1000) + " seconds");
    }
    
    private void stopPeriodicSync() {
        if (syncRunnable != null) {
            handler.removeCallbacks(syncRunnable);
            syncRunnable = null;
        }
        isRunning = false;
        Log.d(TAG, "Stopped periodic sync");
    }
    
    private void performSync() {
        if (syncManager == null) return;
        
        syncManager.autoSyncIfNeeded(new SyncManager.SyncCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Periodic sync completed successfully");
            }
            
            @Override
            public void onFailure(String error) {
                Log.w(TAG, "Periodic sync failed: " + error);
                // Don't stop service on failure, just log and continue
            }
        });
    }
    
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    @Override
    public void onDestroy() {
        stopPeriodicSync();
        super.onDestroy();
        Log.d(TAG, "AutoSyncService destroyed");
    }
}