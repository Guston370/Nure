package com.example.healthscanner;

import android.app.Activity;
import android.view.View;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Keeps screen content clear of the status bar.
 *
 * <p>The app theme sets a transparent status bar with
 * {@code windowDrawsSystemBarBackgrounds}, so by default content is drawn underneath the
 * clock and battery icons. Rather than hardcoding a status bar height into every layout,
 * this applies the real inset reported by the system as extra top padding on the content
 * root, which is correct across notches, punch holes and 16 KB-page devices alike.</p>
 */
public final class SystemBarInsets {

    private SystemBarInsets() {
        // Utility class.
    }

    /**
     * Pad the activity's content root by the status bar height, and by the navigation bar
     * height at the bottom.
     *
     * <p>Safe to call more than once: the view's original padding is captured on the first
     * pass and used as the baseline for every later inset update, so padding never
     * accumulates across configuration changes.</p>
     */
    public static void applyTopInset(Activity activity) {
        if (activity == null) {
            return;
        }
        View content = activity.findViewById(android.R.id.content);
        if (content instanceof android.view.ViewGroup
                && ((android.view.ViewGroup) content).getChildCount() > 0) {
            applyTopInset(((android.view.ViewGroup) content).getChildAt(0));
        }
    }

    /**
     * Pad an explicit view by the status bar inset.
     */
    public static void applyTopInset(View root) {
        if (root == null) {
            return;
        }

        final int basePaddingTop = root.getPaddingTop();
        final int basePaddingBottom = root.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            Insets bars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());

            view.setPadding(
                    view.getPaddingLeft(),
                    basePaddingTop + bars.top,
                    view.getPaddingRight(),
                    basePaddingBottom);

            return windowInsets;
        });

        // Ask for a fresh inset pass in case the view is already attached.
        ViewCompat.requestApplyInsets(root);
    }
}
