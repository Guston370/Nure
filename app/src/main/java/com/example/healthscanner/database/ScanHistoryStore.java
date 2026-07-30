package com.example.healthscanner.database;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.healthscanner.AuthManager;
import com.example.healthscanner.ScanAnalyzer;
import com.example.healthscanner.models.Scan;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Single entry point for reading and writing scan history.
 *
 * <p>Before this class existed each screen hand-rolled its own {@code recent_scans} JSON
 * handling, which is why records were written with different field sets and why the
 * Firestore {@code scans} collection that analytics reads was never populated. Every write
 * now goes through {@link #addScan(Scan)}, which:</p>
 *
 * <ol>
 * <li>stores the full record in local {@code SharedPreferences} (offline source of truth),</li>
 * <li>refreshes the cached aggregate counters used by the home and profile screens,</li>
 * <li>writes the scan to the Firestore {@code scans} collection, and</li>
 * <li>asks {@link SyncManager} to push the history blob to the user document.</li>
 * </ol>
 */
public class ScanHistoryStore {

    private static final String TAG = "ScanHistoryStore";
    private static final String PREFS_NAME = "HealthScannerPrefs";
    private static final String KEY_RECENT_SCANS = "recent_scans";

    /** Maximum number of scans retained locally. */
    public static final int MAX_LOCAL_SCANS = 50;

    private static ScanHistoryStore instance;

    private final Context context;
    private final SharedPreferences preferences;

    private ScanHistoryStore(Context context) {
        this.context = context.getApplicationContext();
        this.preferences = this.context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized ScanHistoryStore getInstance(Context context) {
        if (instance == null) {
            instance = new ScanHistoryStore(context);
        }
        return instance;
    }

    /**
     * Read the local scan history, newest first. Malformed entries are skipped rather than
     * failing the whole read, so one bad record can't empty a user's history.
     */
    public List<Scan> getScans() {
        List<Scan> scans = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(preferences.getString(KEY_RECENT_SCANS, "[]"));
            for (int i = 0; i < array.length(); i++) {
                JSONObject json = array.optJSONObject(i);
                if (json == null) {
                    continue;
                }
                Scan scan = Scan.fromJson(json);
                if (scan != null) {
                    scans.add(scan);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to read scan history: " + e.getMessage(), e);
        }
        return scans;
    }

    /** Only the scans the user marked as favourite, newest first. */
    public List<Scan> getFavorites() {
        List<Scan> favorites = new ArrayList<>();
        for (Scan scan : getScans()) {
            if (scan.isFavorite()) {
                favorites.add(scan);
            }
        }
        return favorites;
    }

    /**
     * Persist a scan locally and push it to Firebase.
     *
     * <p>Re-scanning the same barcode replaces the previous entry instead of adding a
     * duplicate, which keeps the history and the category breakdown meaningful.</p>
     */
    public void addScan(Scan scan) {
        if (scan == null) {
            return;
        }

        try {
            if (scan.getUserId() == null || scan.getUserId().isEmpty()) {
                scan.setUserId(AuthManager.getInstance(context).getCurrentUserId());
            }
            if (scan.getScanDate() == null) {
                scan.setScanDate(new java.util.Date());
            }
            if (scan.getScanId() == null || scan.getScanId().isEmpty()) {
                scan.setScanId("scan_" + System.currentTimeMillis() + "_"
                        + Math.abs(String.valueOf(scan.getBarcode()).hashCode() % 1000));
            }

            List<Scan> existing = getScans();
            JSONArray updated = new JSONArray();
            updated.put(scan.toJson());

            String barcode = scan.getBarcode();
            for (Scan previous : existing) {
                if (updated.length() >= MAX_LOCAL_SCANS) {
                    break;
                }
                boolean sameProduct = barcode != null && !barcode.isEmpty()
                        && barcode.equals(previous.getBarcode());
                if (sameProduct) {
                    // Carry the favourite flag forward so re-scanning doesn't un-favourite.
                    if (previous.isFavorite()) {
                        scan.setFavorite(true);
                        updated.put(0, scan.toJson());
                    }
                    continue;
                }
                updated.put(previous.toJson());
            }

            preferences.edit().putString(KEY_RECENT_SCANS, updated.toString()).apply();
            Log.d(TAG, "Saved scan locally: " + scan.getProductName() + " (" + updated.length() + " total)");

            refreshCachedStatistics();
            saveToFirestore(scan);
            syncHistory();

        } catch (Exception e) {
            Log.e(TAG, "Failed to save scan: " + e.getMessage(), e);
        }
    }

    /**
     * Toggle the favourite flag for a scan, matched by barcode.
     *
     * @return the new favourite state, or {@code false} if the scan wasn't found
     */
    public boolean toggleFavorite(String barcode) {
        if (barcode == null || barcode.isEmpty()) {
            return false;
        }

        List<Scan> scans = getScans();
        boolean newState = false;
        boolean found = false;

        for (Scan scan : scans) {
            if (barcode.equals(scan.getBarcode())) {
                newState = !scan.isFavorite();
                scan.setFavorite(newState);
                found = true;
                break;
            }
        }

        if (!found) {
            return false;
        }

        writeAll(scans);
        return newState;
    }

    /** Whether the product with this barcode is currently favourited. */
    public boolean isFavorite(String barcode) {
        if (barcode == null || barcode.isEmpty()) {
            return false;
        }
        for (Scan scan : getScans()) {
            if (barcode.equals(scan.getBarcode())) {
                return scan.isFavorite();
            }
        }
        return false;
    }

    /** Replace the whole history, used by favourite toggles. */
    private void writeAll(List<Scan> scans) {
        try {
            JSONArray array = new JSONArray();
            for (Scan scan : scans) {
                array.put(scan.toJson());
            }
            preferences.edit().putString(KEY_RECENT_SCANS, array.toString()).apply();
            syncHistory();
        } catch (Exception e) {
            Log.e(TAG, "Failed to write scan history: " + e.getMessage(), e);
        }
    }

    /**
     * Recompute the aggregate counters the home and profile screens read directly, and
     * mirror them onto the Firestore user document.
     */
    private void refreshCachedStatistics() {
        ScanAnalyzer.Stats stats = ScanAnalyzer.analyze(getScans());

        preferences.edit()
                .putInt("total_scans", stats.totalScans)
                .putInt("healthy_choices", stats.healthyChoices)
                .putFloat("average_health_score", (float) stats.averageHealthScore)
                .putLong("last_scan_timestamp", System.currentTimeMillis())
                .apply();

        String userId = AuthManager.getInstance(context).getCurrentUserId();
        if (userId == null || userId.isEmpty()) {
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("totalScans", stats.totalScans);
        updates.put("healthyChoices", stats.healthyChoices);
        updates.put("averageHealthScore", stats.averageHealthScore);
        updates.put("lastScanTimestamp", System.currentTimeMillis());

        FirebaseManager.getInstance().updateUserPreferences(userId, updates,
                new FirebaseManager.OperationCallback() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "User statistics updated in Firebase");
                    }

                    @Override
                    public void onFailure(String error) {
                        Log.w(TAG, "Could not update user statistics: " + error);
                    }
                });
    }

    /**
     * Write the scan to the top-level {@code scans} collection that the analytics screen
     * queries. Failures are logged only: the local copy is authoritative when offline.
     */
    private void saveToFirestore(Scan scan) {
        if (scan.getUserId() == null || scan.getUserId().isEmpty()) {
            Log.d(TAG, "No signed-in user, keeping scan local only");
            return;
        }

        FirebaseScanManager.getInstance().saveScan(scan, new FirebaseScanManager.OperationCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Scan mirrored to Firestore");
            }

            @Override
            public void onFailure(String error) {
                Log.w(TAG, "Could not mirror scan to Firestore: " + error);
            }
        });
    }

    private void syncHistory() {
        try {
            SyncManager.getInstance(context).syncOnScanHistoryChange(new SyncManager.SyncCallback() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "Scan history synced");
                }

                @Override
                public void onFailure(String error) {
                    Log.w(TAG, "Scan history sync deferred: " + error);
                }
            });
        } catch (Exception e) {
            Log.w(TAG, "Sync unavailable: " + e.getMessage());
        }
    }
}
