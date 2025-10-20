package com.example.healthscanner;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

/**
 * Helper class for creating Material Design parallax + depth transition animations
 * Provides 3D-like depth transitions with scale and translation effects
 */
public class ParallaxDepthTransitionHelper {
    
    private static final String TAG = "ParallaxDepthHelper";
    private static final int ANIMATION_DURATION = 500; // 450-550ms range
    private static final float DEPTH_SCALE_FROM = 0.95f;
    private static final float DEPTH_SCALE_TO = 1.0f;
    private static final float PARALLAX_TRANSLATION = 50f; // dp
    
    private Context context;
    private boolean isAnimating = false;
    
    public ParallaxDepthTransitionHelper(Context context) {
        this.context = context;
    }
    
    /**
     * Create forward depth transition animation
     * @param outgoingView The view that's being navigated away from
     * @param incomingView The view that's being navigated to
     * @param listener Animation completion listener
     */
    public void createForwardDepthTransition(View outgoingView, View incomingView, 
                                           AnimationCompleteListener listener) {
        
        if (isAnimating) {
            Log.w(TAG, "Animation already in progress, skipping");
            return;
        }
        
        if (incomingView == null) {
            Log.w(TAG, "Incoming view is null, cannot create transition");
            if (listener != null) listener.onAnimationComplete();
            return;
        }
        
        isAnimating = true;
        
        // Prepare incoming view
        incomingView.setVisibility(View.VISIBLE);
        incomingView.setAlpha(0f);
        incomingView.setScaleX(DEPTH_SCALE_FROM);
        incomingView.setScaleY(DEPTH_SCALE_FROM);
        incomingView.setTranslationZ(0f);
        
        // Create incoming view animations (depth scale-up + fade in)
        ObjectAnimator incomingAlpha = ObjectAnimator.ofFloat(incomingView, "alpha", 0f, 1f);
        ObjectAnimator incomingScaleX = ObjectAnimator.ofFloat(incomingView, "scaleX", DEPTH_SCALE_FROM, DEPTH_SCALE_TO);
        ObjectAnimator incomingScaleY = ObjectAnimator.ofFloat(incomingView, "scaleY", DEPTH_SCALE_FROM, DEPTH_SCALE_TO);
        ObjectAnimator incomingTranslationZ = ObjectAnimator.ofFloat(incomingView, "translationZ", -PARALLAX_TRANSLATION, 0f);
        
        AnimatorSet incomingAnimatorSet = new AnimatorSet();
        incomingAnimatorSet.playTogether(incomingAlpha, incomingScaleX, incomingScaleY, incomingTranslationZ);
        incomingAnimatorSet.setDuration(ANIMATION_DURATION);
        incomingAnimatorSet.setInterpolator(new FastOutSlowInInterpolator());
        
        AnimatorSet completeAnimatorSet = new AnimatorSet();
        
        if (outgoingView != null && outgoingView.getVisibility() == View.VISIBLE) {
            // Create outgoing view animations (fade out + move back)
            ObjectAnimator outgoingAlpha = ObjectAnimator.ofFloat(outgoingView, "alpha", 1f, 0f);
            ObjectAnimator outgoingScaleX = ObjectAnimator.ofFloat(outgoingView, "scaleX", DEPTH_SCALE_TO, DEPTH_SCALE_FROM);
            ObjectAnimator outgoingScaleY = ObjectAnimator.ofFloat(outgoingView, "scaleY", DEPTH_SCALE_TO, DEPTH_SCALE_FROM);
            ObjectAnimator outgoingTranslationZ = ObjectAnimator.ofFloat(outgoingView, "translationZ", 0f, -PARALLAX_TRANSLATION);
            
            AnimatorSet outgoingAnimatorSet = new AnimatorSet();
            outgoingAnimatorSet.playTogether(outgoingAlpha, outgoingScaleX, outgoingScaleY, outgoingTranslationZ);
            outgoingAnimatorSet.setDuration(ANIMATION_DURATION);
            outgoingAnimatorSet.setInterpolator(new FastOutSlowInInterpolator());
            
            // Play both animations together
            completeAnimatorSet.playTogether(incomingAnimatorSet, outgoingAnimatorSet);
        } else {
            // Only incoming animation
            completeAnimatorSet.play(incomingAnimatorSet);
        }
        
        completeAnimatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                // Reset outgoing view
                if (outgoingView != null) {
                    outgoingView.setVisibility(View.GONE);
                    resetViewProperties(outgoingView);
                }
                
                // Ensure incoming view is properly set
                resetViewProperties(incomingView);
                incomingView.setVisibility(View.VISIBLE);
                
                isAnimating = false;
                
                if (listener != null) {
                    listener.onAnimationComplete();
                }
            }
        });
        
        completeAnimatorSet.start();
        
        Log.d(TAG, "Forward depth transition animation started");
    }
    
    /**
     * Create backward depth transition animation (reverse effect)
     * @param outgoingView The view that's being navigated away from
     * @param incomingView The view that's being navigated to
     * @param listener Animation completion listener
     */
    public void createBackwardDepthTransition(View outgoingView, View incomingView,
                                            AnimationCompleteListener listener) {
        
        if (isAnimating) {
            Log.w(TAG, "Animation already in progress, skipping");
            return;
        }
        
        if (incomingView == null) {
            Log.w(TAG, "Incoming view is null, cannot create transition");
            if (listener != null) listener.onAnimationComplete();
            return;
        }
        
        isAnimating = true;
        
        // Prepare incoming view (reverse effect - starts larger and moves forward)
        incomingView.setVisibility(View.VISIBLE);
        incomingView.setAlpha(0f);
        incomingView.setScaleX(1.05f); // Slightly larger for reverse effect
        incomingView.setScaleY(1.05f);
        incomingView.setTranslationZ(PARALLAX_TRANSLATION);
        
        // Create incoming view animations (reverse depth effect)
        ObjectAnimator incomingAlpha = ObjectAnimator.ofFloat(incomingView, "alpha", 0f, 1f);
        ObjectAnimator incomingScaleX = ObjectAnimator.ofFloat(incomingView, "scaleX", 1.05f, DEPTH_SCALE_TO);
        ObjectAnimator incomingScaleY = ObjectAnimator.ofFloat(incomingView, "scaleY", 1.05f, DEPTH_SCALE_TO);
        ObjectAnimator incomingTranslationZ = ObjectAnimator.ofFloat(incomingView, "translationZ", PARALLAX_TRANSLATION, 0f);
        
        AnimatorSet incomingAnimatorSet = new AnimatorSet();
        incomingAnimatorSet.playTogether(incomingAlpha, incomingScaleX, incomingScaleY, incomingTranslationZ);
        incomingAnimatorSet.setDuration(ANIMATION_DURATION);
        incomingAnimatorSet.setInterpolator(new FastOutSlowInInterpolator());
        
        AnimatorSet completeAnimatorSet = new AnimatorSet();
        
        if (outgoingView != null && outgoingView.getVisibility() == View.VISIBLE) {
            // Create outgoing view animations (scale down + move back)
            ObjectAnimator outgoingAlpha = ObjectAnimator.ofFloat(outgoingView, "alpha", 1f, 0f);
            ObjectAnimator outgoingScaleX = ObjectAnimator.ofFloat(outgoingView, "scaleX", DEPTH_SCALE_TO, 0.9f);
            ObjectAnimator outgoingScaleY = ObjectAnimator.ofFloat(outgoingView, "scaleY", DEPTH_SCALE_TO, 0.9f);
            ObjectAnimator outgoingTranslationZ = ObjectAnimator.ofFloat(outgoingView, "translationZ", 0f, -PARALLAX_TRANSLATION * 1.5f);
            
            AnimatorSet outgoingAnimatorSet = new AnimatorSet();
            outgoingAnimatorSet.playTogether(outgoingAlpha, outgoingScaleX, outgoingScaleY, outgoingTranslationZ);
            outgoingAnimatorSet.setDuration(ANIMATION_DURATION);
            outgoingAnimatorSet.setInterpolator(new FastOutSlowInInterpolator());
            
            // Play both animations together
            completeAnimatorSet.playTogether(incomingAnimatorSet, outgoingAnimatorSet);
        } else {
            // Only incoming animation
            completeAnimatorSet.play(incomingAnimatorSet);
        }
        
        completeAnimatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                // Reset outgoing view
                if (outgoingView != null) {
                    outgoingView.setVisibility(View.GONE);
                    resetViewProperties(outgoingView);
                }
                
                // Ensure incoming view is properly set
                resetViewProperties(incomingView);
                incomingView.setVisibility(View.VISIBLE);
                
                isAnimating = false;
                
                if (listener != null) {
                    listener.onAnimationComplete();
                }
            }
        });
        
        completeAnimatorSet.start();
        
        Log.d(TAG, "Backward depth transition animation started");
    }
    
    /**
     * Create subtle UI element entrance animation
     * @param view The view to animate
     * @param delay Delay before starting animation
     */
    public void createElementEntranceAnimation(View view, long delay) {
        if (view == null) return;
        
        view.setAlpha(0f);
        view.setScaleX(0.9f);
        view.setScaleY(0.9f);
        view.setTranslationY(20f);
        
        ObjectAnimator alpha = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f);
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", 0.9f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", 0.9f, 1f);
        ObjectAnimator translationY = ObjectAnimator.ofFloat(view, "translationY", 20f, 0f);
        
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(alpha, scaleX, scaleY, translationY);
        animatorSet.setDuration(300);
        animatorSet.setStartDelay(delay);
        animatorSet.setInterpolator(new FastOutSlowInInterpolator());
        
        animatorSet.start();
    }
    
    /**
     * Reset view properties to default state
     * @param view The view to reset
     */
    private void resetViewProperties(View view) {
        if (view == null) return;
        
        view.setAlpha(1f);
        view.setScaleX(1f);
        view.setScaleY(1f);
        view.setTranslationX(0f);
        view.setTranslationY(0f);
        view.setTranslationZ(0f);
    }
    
    /**
     * Check if animation is currently running
     * @return true if animating
     */
    public boolean isAnimating() {
        return isAnimating;
    }
    
    /**
     * Interface for animation completion callbacks
     */
    public interface AnimationCompleteListener {
        void onAnimationComplete();
    }
}