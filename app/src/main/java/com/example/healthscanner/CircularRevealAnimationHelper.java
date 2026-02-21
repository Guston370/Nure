package com.example.healthscanner;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.view.ViewAnimationUtils;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import android.widget.FrameLayout;

import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

/**
 * Helper class for creating Material Design circular reveal animations
 * Handles both reveal and reverse reveal animations with theme color overlays
 */
public class CircularRevealAnimationHelper {

    private static final String TAG = "CircularRevealHelper";
    private static final int ANIMATION_DURATION = 500;
    private static final int BACK_ANIMATION_DURATION = 500; // 450-550ms range for back navigation
    private static final int FADE_DURATION = 300;

    private Context context;
    private FrameLayout overlayContainer;
    private View gradientOverlay;

    public CircularRevealAnimationHelper(Context context) {
        this.context = context;
    }

    /**
     * Create circular reveal animation from a specific point
     * 
     * @param targetView The view to reveal
     * @param centerX    X coordinate of the reveal center
     * @param centerY    Y coordinate of the reveal center
     * @param listener   Animation completion listener
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void createCircularReveal(View targetView, int centerX, int centerY,
            AnimationCompleteListener listener) {

        if (targetView == null) {
            Log.w(TAG, "Target view is null, cannot create reveal animation");
            if (listener != null)
                listener.onAnimationComplete();
            return;
        }

        // Calculate the maximum radius for the reveal
        int width = targetView.getWidth();
        int height = targetView.getHeight();
        float maxRadius = (float) Math.hypot(Math.max(centerX, width - centerX),
                Math.max(centerY, height - centerY));

        // Create the circular reveal animator
        Animator revealAnimator = ViewAnimationUtils.createCircularReveal(
                targetView, centerX, centerY, 0, maxRadius);

        // Set up the animation
        revealAnimator.setDuration(ANIMATION_DURATION);
        revealAnimator.setInterpolator(new FastOutSlowInInterpolator());

        // Create gradient overlay animation
        AnimatorSet animatorSet = new AnimatorSet();

        if (overlayContainer != null) {
            createGradientOverlay();
            Animator overlayAnimator = createOverlayAnimation(true);
            animatorSet.playTogether(revealAnimator, overlayAnimator);
        } else {
            animatorSet.play(revealAnimator);
        }

        // Set up completion listener
        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                targetView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                cleanupOverlay();
                if (listener != null) {
                    listener.onAnimationComplete();
                }
            }
        });

        // Start the animation
        animatorSet.start();

        Log.d(TAG, "Circular reveal animation started");
    }

    /**
     * Create reverse circular reveal animation (for back navigation)
     * 
     * @param targetView The view to hide
     * @param centerX    X coordinate of the reveal center
     * @param centerY    Y coordinate of the reveal center
     * @param listener   Animation completion listener
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void createReverseCircularReveal(View targetView, int centerX, int centerY,
            AnimationCompleteListener listener) {

        if (targetView == null) {
            Log.w(TAG, "Target view is null, cannot create reverse reveal animation");
            if (listener != null)
                listener.onAnimationComplete();
            return;
        }

        // Calculate the maximum radius
        int width = targetView.getWidth();
        int height = targetView.getHeight();
        float maxRadius = (float) Math.hypot(Math.max(centerX, width - centerX),
                Math.max(centerY, height - centerY));

        // Create the reverse circular reveal animator
        Animator revealAnimator = ViewAnimationUtils.createCircularReveal(
                targetView, centerX, centerY, maxRadius, 0);

        revealAnimator.setDuration(ANIMATION_DURATION);
        revealAnimator.setInterpolator(new FastOutSlowInInterpolator());

        // Create gradient overlay animation
        AnimatorSet animatorSet = new AnimatorSet();

        if (overlayContainer != null) {
            createGradientOverlay();
            Animator overlayAnimator = createOverlayAnimation(false);
            animatorSet.playTogether(revealAnimator, overlayAnimator);
        } else {
            animatorSet.play(revealAnimator);
        }

        // Set up completion listener
        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                targetView.setVisibility(View.INVISIBLE);
                cleanupOverlay();
                if (listener != null) {
                    listener.onAnimationComplete();
                }
            }
        });

        // Start the animation
        animatorSet.start();

        Log.d(TAG, "Reverse circular reveal animation started");
    }

    /**
     * Create back navigation circular reveal animation with optimized timing
     * 
     * @param targetView The view to contract
     * @param centerX    X coordinate of the reveal center
     * @param centerY    Y coordinate of the reveal center
     * @param listener   Animation completion listener
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void createBackNavigationReveal(View targetView, int centerX, int centerY,
            AnimationCompleteListener listener) {

        if (targetView == null) {
            Log.w(TAG, "Target view is null, cannot create back navigation reveal animation");
            if (listener != null)
                listener.onAnimationComplete();
            return;
        }

        // Calculate the maximum radius
        int width = targetView.getWidth();
        int height = targetView.getHeight();
        float maxRadius = (float) Math.hypot(Math.max(centerX, width - centerX),
                Math.max(centerY, height - centerY));

        // Create the back navigation circular reveal animator (contracting)
        Animator revealAnimator = ViewAnimationUtils.createCircularReveal(
                targetView, centerX, centerY, maxRadius, 0);

        // Use back navigation specific duration (450-550ms range)
        revealAnimator.setDuration(BACK_ANIMATION_DURATION);
        revealAnimator.setInterpolator(new FastOutSlowInInterpolator());

        // Create enhanced gradient overlay with fade-out blending
        AnimatorSet animatorSet = new AnimatorSet();

        if (overlayContainer != null) {
            createBackNavigationOverlay();
            Animator overlayAnimator = createBackNavigationOverlayAnimation();
            animatorSet.playTogether(revealAnimator, overlayAnimator);
        } else {
            animatorSet.play(revealAnimator);
        }

        // Set up completion listener
        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                targetView.setVisibility(View.INVISIBLE);
                cleanupOverlay();
                if (listener != null) {
                    listener.onAnimationComplete();
                }
            }
        });

        // Start the animation
        animatorSet.start();

        Log.d(TAG, "Back navigation circular reveal animation started");
    }

    /**
     * Create fallback fade animation for API < 21
     * 
     * @param targetView The view to animate
     * @param fadeIn     True for fade in, false for fade out
     * @param listener   Animation completion listener
     */
    public void createFallbackAnimation(View targetView, boolean fadeIn,
            AnimationCompleteListener listener) {

        if (targetView == null) {
            Log.w(TAG, "Target view is null, cannot create fallback animation");
            if (listener != null)
                listener.onAnimationComplete();
            return;
        }

        float startAlpha = fadeIn ? 0f : 1f;
        float endAlpha = fadeIn ? 1f : 0f;

        ObjectAnimator fadeAnimator = ObjectAnimator.ofFloat(targetView, "alpha", startAlpha, endAlpha);
        fadeAnimator.setDuration(FADE_DURATION);
        fadeAnimator.setInterpolator(new FastOutSlowInInterpolator());

        fadeAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                if (fadeIn) {
                    targetView.setVisibility(View.VISIBLE);
                    targetView.setAlpha(0f);
                }
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (!fadeIn) {
                    targetView.setVisibility(View.INVISIBLE);
                }
                if (listener != null) {
                    listener.onAnimationComplete();
                }
            }
        });

        fadeAnimator.start();

        Log.d(TAG, "Fallback fade animation started");
    }

    /**
     * Create fallback fade animation for back navigation on API < 21
     * 
     * @param targetView The view to animate
     * @param listener   Animation completion listener
     */
    public void createBackNavigationFallbackAnimation(View targetView, AnimationCompleteListener listener) {

        if (targetView == null) {
            Log.w(TAG, "Target view is null, cannot create back navigation fallback animation");
            if (listener != null)
                listener.onAnimationComplete();
            return;
        }

        // Enhanced fade-out animation for back navigation fallback
        ObjectAnimator fadeAnimator = ObjectAnimator.ofFloat(targetView, "alpha", 1f, 0f);
        fadeAnimator.setDuration(BACK_ANIMATION_DURATION);
        fadeAnimator.setInterpolator(new FastOutSlowInInterpolator());

        fadeAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                targetView.setVisibility(View.INVISIBLE);
                if (listener != null) {
                    listener.onAnimationComplete();
                }
            }
        });

        fadeAnimator.start();

        Log.d(TAG, "Back navigation fallback fade animation started");
    }

    /**
     * Set the overlay container for gradient effects
     * 
     * @param container FrameLayout that will contain the gradient overlay
     */
    public void setOverlayContainer(FrameLayout container) {
        this.overlayContainer = container;
    }

    /**
     * Create gradient overlay view with theme colors
     */
    private void createGradientOverlay() {
        if (overlayContainer == null)
            return;

        // Remove any existing overlay
        cleanupOverlay();

        // Create gradient overlay view
        gradientOverlay = new View(context);

        // Create gradient drawable with theme colors
        GradientDrawable gradient = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[] {
                        ContextCompat.getColor(context, R.color.health_teal),
                        ContextCompat.getColor(context, R.color.health_mint_green)
                });

        gradientOverlay.setBackground(gradient);
        gradientOverlay.setAlpha(0f);

        // Add to overlay container
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT);
        overlayContainer.addView(gradientOverlay, params);
    }

    /**
     * Create enhanced gradient overlay for back navigation with fade-out blending
     */
    private void createBackNavigationOverlay() {
        if (overlayContainer == null)
            return;

        // Remove any existing overlay
        cleanupOverlay();

        // Create gradient overlay view with enhanced fade-out effect
        gradientOverlay = new View(context);

        // Create gradient drawable with theme colors and enhanced blending
        GradientDrawable gradient = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[] {
                        ContextCompat.getColor(context, R.color.health_teal),
                        ContextCompat.getColor(context, R.color.health_mint_green),
                        ContextCompat.getColor(context, R.color.background_gradient_end)
                });

        gradientOverlay.setBackground(gradient);
        gradientOverlay.setAlpha(0.4f); // Start with higher opacity for back navigation

        // Add to overlay container
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT);
        overlayContainer.addView(gradientOverlay, params);
    }

    /**
     * Create overlay animation (fade in/out)
     * 
     * @param fadeIn True for fade in, false for fade out
     * @return Animator for the overlay
     */
    private Animator createOverlayAnimation(boolean fadeIn) {
        if (gradientOverlay == null) {
            return ValueAnimator.ofFloat(0f, 1f).setDuration(1); // Dummy animator
        }

        float startAlpha = fadeIn ? 0f : 0.3f;
        float endAlpha = fadeIn ? 0.3f : 0f;

        ObjectAnimator overlayAnimator = ObjectAnimator.ofFloat(gradientOverlay, "alpha", startAlpha, endAlpha);
        overlayAnimator.setDuration(ANIMATION_DURATION);
        overlayAnimator.setInterpolator(new FastOutSlowInInterpolator());

        return overlayAnimator;
    }

    /**
     * Create back navigation overlay animation with enhanced fade-out blending
     * 
     * @return Animator for the back navigation overlay
     */
    private Animator createBackNavigationOverlayAnimation() {
        if (gradientOverlay == null) {
            return ValueAnimator.ofFloat(0f, 1f).setDuration(1); // Dummy animator
        }

        // Enhanced fade-out animation for back navigation
        ObjectAnimator overlayAnimator = ObjectAnimator.ofFloat(gradientOverlay, "alpha", 0.4f, 0f);
        overlayAnimator.setDuration(BACK_ANIMATION_DURATION);
        overlayAnimator.setInterpolator(new FastOutSlowInInterpolator());

        return overlayAnimator;
    }

    /**
     * Clean up overlay view
     */
    private void cleanupOverlay() {
        if (overlayContainer != null && gradientOverlay != null) {
            overlayContainer.removeView(gradientOverlay);
            gradientOverlay = null;
        }
    }

    /**
     * Calculate center point from bottom navigation item
     * 
     * @param bottomNavView The bottom navigation view
     * @param itemId        The menu item ID
     * @return int array with [x, y] coordinates, or null if not found
     */
    public static int[] getBottomNavItemCenter(View bottomNavView, int itemId) {
        if (bottomNavView == null)
            return null;

        try {
            // Get the bottom navigation view bounds
            int navWidth = bottomNavView.getWidth();
            int navHeight = bottomNavView.getHeight();

            // Calculate approximate item position based on item count and index
            // This is an approximation since we can't directly access item views
            int itemCount = 5; // Based on the menu (home, scan, stats, history, profile)
            int itemIndex = getItemIndex(itemId);

            if (itemIndex == -1)
                return null;

            int itemWidth = navWidth / itemCount;
            int centerX = (itemIndex * itemWidth) + (itemWidth / 2);
            int centerY = navHeight / 2;

            // Get the bottom navigation's position on screen
            int[] location = new int[2];
            bottomNavView.getLocationOnScreen(location);

            return new int[] { location[0] + centerX, location[1] + centerY };

        } catch (Exception e) {
            Log.w(TAG, "Error calculating bottom nav item center: " + e.getMessage());
            return null;
        }
    }

    /**
     * Get item index based on menu item ID
     */
    private static int getItemIndex(int itemId) {
        if (itemId == R.id.nav_home)
            return 0;
        if (itemId == R.id.nav_scan)
            return 1;
        if (itemId == R.id.nav_stats)
            return 2;
        if (itemId == R.id.nav_history)
            return 3;
        if (itemId == R.id.nav_profile)
            return 4;
        return -1;
    }

    /**
     * Interface for animation completion callbacks
     */
    public interface AnimationCompleteListener {
        void onAnimationComplete();
    }
}