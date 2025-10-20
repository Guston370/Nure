// Manual data clearing script for Health Scanner App
// This simulates what the DataResetManager does

import android.content.Context;
import android.content.SharedPreferences;

public class ClearAppData {
    
    public static void clearAllLocalData(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("HealthScannerPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        
        // Clear all data
        editor.clear();
        
        // Apply changes
        editor.apply();
        
        System.out.println("✅ All local storage data cleared successfully!");
    }
    
    // Keys that would be cleared:
    /*
    - Authentication data: current_user_name, current_user_id, auth_provider
    - Google account data: google_account_type, google_id_token, is_google_account
    - App data: recent_scans, scan_history, health_concerns, dietary_preferences
    - Statistics: total_scans, healthy_choices, average_health_score
    - Settings: notifications_enabled, dark_mode_enabled
    - Sync data: last_sync_timestamp, is_first_launch_after_signin
    */
}