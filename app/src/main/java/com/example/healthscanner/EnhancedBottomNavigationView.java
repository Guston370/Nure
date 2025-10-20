package com.example.healthscanner;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * Enhanced Bottom Navigation View with gradient effects and smooth animations
 * Provides a modern, health-themed navigation experience
 */
public class EnhancedBottomNavigationView extends BottomNavigationView {
    
    private Paint gradientPaint;
    private LinearGradient backgroundGradient;
    private ValueAnimator glowAnimator;
    private float glowIntensity = 0f;
    
    // Gradient colors matching the health theme
    private int[] gradientColors = {
        0xFF3CCF91, // health_mint_green
        0xFF0FB8AD, // health_teal
        0xFF4DB6E3  // health_soft_blue
    };
    
    public EnhancedBottomNavigationView(@NonNull Context context) {
        super(context);
        init();
    }
    
    public EnhancedBottomNavigationView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    
    public EnhancedBottomNavigationView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    
    private void init() {
        // Initialize gradient paint
        gradientPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        
        // Enable hardware acceleration for smooth animations
        setLayerType(View.LAYER_TYPE_HARDWARE, null);
    }
    
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        
        // Create gradient based on the view size
        backgroundGradient = new LinearGradient(
            0, 0, w, 0,
            gradientColors,
            new float[]{0f, 0.5f, 1f},
            Shader.TileMode.CLAMP
        );
        gradientPaint.setShader(backgroundGradient);
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        // Draw subtle gradient glow effect when items are selected
        if (glowIntensity > 0) {
            gradientPaint.setAlpha((int) (glowIntensity * 30)); // Subtle glow
            canvas.drawRect(0, 0, getWidth(), 4, gradientPaint); // Top border glow
        }
    }
    
    private OnItemSelectedListener originalListener;
    
    @Override
    public void setOnItemSelectedListener(@Nullable OnItemSelectedListener listener) {
        this.originalListener = listener;
        super.setOnItemSelectedListener(this::onEnhancedItemSelected);
    }
    
    /**
     * Enhanced item selection with animations
     */
    private boolean onEnhancedItemSelected(@NonNull MenuItem item) {
        // Animate the selected item
        animateItemSelection(item);
        
        // Trigger glow effect
        animateGlowEffect();
        
        // Call the original listener if set
        if (originalListener != null) {
            return originalListener.onNavigationItemSelected(item);
        }
        
        return true;
    }
    
    /**
     * Animate the selected navigation item with enhanced effects
     */
    private void animateItemSelection(@NonNull MenuItem item) {
        try {
            // Find the view for the menu item
            View itemView = findViewById(item.getItemId());
            if (itemView != null) {
                // Scale bounce animation for the icon
                itemView.startAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.scale_bounce));
                
                // Add subtle elevation animation
                itemView.animate()
                    .translationY(-2f)
                    .setDuration(150)
                    .withEndAction(() -> {
                        itemView.animate()
                            .translationY(0f)
                            .setDuration(150)
                            .start();
                    })
                    .start();
                
                // Animate text label fade-in (since labelVisibilityMode is "selected")
                animateTextLabel(itemView);
            }
        } catch (Exception e) {
            // Fail silently to prevent crashes
        }
    }
    
    /**
     * Animate the text label fade-in for selected items
     */
    private void animateTextLabel(View itemView) {
        // The text will automatically appear due to labelVisibilityMode="selected"
        // We can add a subtle slide-up animation to enhance the effect
        itemView.postDelayed(() -> {
            try {
                // Find text views within the item and animate them
                if (itemView instanceof android.view.ViewGroup) {
                    android.view.ViewGroup group = (android.view.ViewGroup) itemView;
                    for (int i = 0; i < group.getChildCount(); i++) {
                        View child = group.getChildAt(i);
                        if (child instanceof android.widget.TextView) {
                            child.setAlpha(0f);
                            child.setTranslationY(8f);
                            child.animate()
                                .alpha(1f)
                                .translationY(0f)
                                .setDuration(200)
                                .start();
                        }
                    }
                }
            } catch (Exception e) {
                // Fail silently
            }
        }, 100);
    }
    
    /**
     * Animate the glow effect on item selection
     */
    private void animateGlowEffect() {
        if (glowAnimator != null && glowAnimator.isRunning()) {
            glowAnimator.cancel();
        }
        
        glowAnimator = ValueAnimator.ofFloat(0f, 1f, 0f);
        glowAnimator.setDuration(600);
        glowAnimator.addUpdateListener(animation -> {
            glowIntensity = (float) animation.getAnimatedValue();
            invalidate(); // Trigger redraw
        });
        glowAnimator.start();
    }
    
    /**
     * Set custom gradient colors
     */
    public void setGradientColors(int[] colors) {
        this.gradientColors = colors;
        if (getWidth() > 0 && getHeight() > 0) {
            backgroundGradient = new LinearGradient(
                0, 0, getWidth(), 0,
                gradientColors,
                new float[]{0f, 0.5f, 1f},
                Shader.TileMode.CLAMP
            );
            gradientPaint.setShader(backgroundGradient);
            invalidate();
        }
    }
    
    /**
     * Animate the entire navigation bar on activity transitions
     */
    public void animateTransition() {
        animate()
            .alpha(0.8f)
            .scaleY(0.95f)
            .setDuration(200)
            .withEndAction(() -> {
                animate()
                    .alpha(1f)
                    .scaleY(1f)
                    .setDuration(200)
                    .start();
            })
            .start();
    }
    
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        // Clean up animations
        if (glowAnimator != null && glowAnimator.isRunning()) {
            glowAnimator.cancel();
        }
    }
}