package com.etherealnymph.dim.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

/** 状态点：缓慢呼吸，用来表示「已就绪」。 */
public class BreathingDotView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private ValueAnimator animator;
    private float alpha = 1f;

    public BreathingDotView(Context context) {
        this(context, null);
    }

    public BreathingDotView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BreathingDotView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        paint.setColor(0xFF74E8C8);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    public void setColor(int color) {
        paint.setColor(color);
        invalidate();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (animator == null) {
            animator = ValueAnimator.ofFloat(0.35f, 1f);
            animator.setDuration(1600L);
            animator.setRepeatMode(ValueAnimator.REVERSE);
            animator.setRepeatCount(ValueAnimator.INFINITE);
            animator.setInterpolator(new AccelerateDecelerateInterpolator());
            animator.addUpdateListener(animation -> {
                alpha = (float) animation.getAnimatedValue();
                invalidate();
            });
        }
        animator.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        if (animator != null) {
            animator.cancel();
        }
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float radius = Math.min(getWidth(), getHeight()) / 2f;
        paint.setAlpha((int) (255 * alpha));
        canvas.drawCircle(cx, cy, radius * 0.44f, paint);
        paint.setAlpha((int) (46 * alpha));
        canvas.drawCircle(cx, cy, radius * 0.95f, paint);
    }
}
