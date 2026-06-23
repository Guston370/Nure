package com.example.healthscanner;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewTreeObserver;

public class ScannerOverlayView extends View {
    private Paint eraserPaint;
    private Paint borderPaint;
    private RectF frameRect = new RectF();
    private float cornerRadius = 0f;
    private View targetFrameView;
    private ViewTreeObserver.OnGlobalLayoutListener layoutListener;

    public ScannerOverlayView(Context context) {
        super(context);
        init();
    }

    public ScannerOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ScannerOverlayView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // Required for PorterDuff CLEAR mode to work correctly on all API levels
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        eraserPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        eraserPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(dpToPx(1.5f));
        borderPaint.setColor(0x1FFFFFFF); // Subtle white glass border outline matching R.color.glass_border
    }

    public void setTargetFrameView(View view, float cornerRadiusDp) {
        // Remove previous listener if any
        if (this.targetFrameView != null && layoutListener != null) {
            try {
                this.targetFrameView.getViewTreeObserver().removeOnGlobalLayoutListener(layoutListener);
            } catch (Exception e) {}
        }

        this.targetFrameView = view;
        this.cornerRadius = dpToPx(cornerRadiusDp);

        if (view != null) {
            layoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    invalidate();
                }
            };
            view.getViewTreeObserver().addOnGlobalLayoutListener(layoutListener);
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // 1. Draw a dark navy semi-transparent background overlay (aligned with Nure's theme)
        canvas.drawColor(0xB3050816); // 70% opacity dark navy

        // 2. Draw clear cutout if target view is bound and laid out
        if (targetFrameView != null && targetFrameView.getWidth() > 0) {
            int[] overlayLocation = new int[2];
            getLocationOnScreen(overlayLocation);

            int[] frameLocation = new int[2];
            targetFrameView.getLocationOnScreen(frameLocation);

            float left = frameLocation[0] - overlayLocation[0];
            float top = frameLocation[1] - overlayLocation[1];
            float right = left + targetFrameView.getWidth();
            float bottom = top + targetFrameView.getHeight();

            frameRect.set(left, top, right, bottom);

            // Eraser paint punches a transparent hole in the overlay background
            canvas.drawRoundRect(frameRect, cornerRadius, cornerRadius, eraserPaint);

            // Draw a subtle border outline
            canvas.drawRoundRect(frameRect, cornerRadius, cornerRadius, borderPaint);
        }
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (targetFrameView != null && layoutListener != null) {
            try {
                targetFrameView.getViewTreeObserver().removeOnGlobalLayoutListener(layoutListener);
            } catch (Exception e) {}
        }
    }
}
