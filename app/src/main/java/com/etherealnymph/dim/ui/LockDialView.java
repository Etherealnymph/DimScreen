package com.etherealnymph.dim.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;

import com.etherealnymph.dim.R;

/**
 * 主界面的「熄屏圆盘」。
 *
 * <p>视觉由三层组成：一圈缓慢旋转的极光弧、一枚内发光圆盘、中心的电源符号。
 * 触摸按下时圆盘轻微收缩，松开后触发一条扩散的冲击波。
 */
public class LockDialView extends View {

    public interface OnLockListener {
        void onLock();
    }

    private static final int DEFAULT_SIZE_DP = 244;

    private final Paint haloPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint discPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glyphPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shockPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF arcRect = new RectF();

    private OnLockListener listener;
    private ValueAnimator spinAnimator;
    private ValueAnimator pulseAnimator;
    private ValueAnimator pressAnimator;
    private ValueAnimator shockAnimator;

    private float spin;
    private float pulse;
    private float pressScale = 1f;
    private float shockProgress = -1f;
    private boolean pressed;

    public LockDialView(Context context) {
        this(context, null);
    }

    public LockDialView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LockDialView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setClickable(true);
        setFocusable(true);
        setContentDescription(context.getString(R.string.dial_description));

        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeWidth(Ui.dp(context, 1f));
        ringPaint.setColor(0x1FFFFFFF);

        arcPaint.setStyle(Paint.Style.STROKE);
        arcPaint.setStrokeWidth(Ui.dp(context, 2.4f));
        arcPaint.setStrokeCap(Paint.Cap.ROUND);

        glyphPaint.setStyle(Paint.Style.STROKE);
        glyphPaint.setStrokeWidth(Ui.dp(context, 3.2f));
        glyphPaint.setStrokeCap(Paint.Cap.ROUND);
        glyphPaint.setColor(0xFFEAF7F2);

        shockPaint.setStyle(Paint.Style.STROKE);
        shockPaint.setStrokeWidth(Ui.dp(context, 1.4f));
        shockPaint.setColor(0xFF74E8C8);
    }

    public void setOnLockListener(OnLockListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desired = Ui.dp(getContext(), DEFAULT_SIZE_DP);
        int width = resolveSize(desired, widthMeasureSpec);
        int height = resolveSize(desired, heightMeasureSpec);
        int size = Math.min(width, height);
        setMeasuredDimension(size, size);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float cx = w / 2f;
        float cy = h / 2f;
        float radius = Math.min(w, h) / 2f - Ui.dp(getContext(), 10);

        haloPaint.setShader(new RadialGradient(
                cx, cy, Math.max(1f, radius * 1.22f),
                new int[]{0x5574E8C8, 0x1E74E8C8, 0x0074E8C8},
                new float[]{0f, 0.55f, 1f},
                Shader.TileMode.CLAMP));

        discPaint.setShader(new RadialGradient(
                cx, cy - radius * 0.30f, Math.max(1f, radius * 1.05f),
                new int[]{0x2BFFFFFF, 0x10FFFFFF, 0x05FFFFFF},
                new float[]{0f, 0.55f, 1f},
                Shader.TileMode.CLAMP));

        arcPaint.setShader(new SweepGradient(
                cx, cy,
                new int[]{0x0074E8C8, 0x0074E8C8, 0xD974E8C8, 0x0074E8C8},
                new float[]{0f, 0.55f, 0.78f, 1f}));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float radius = Math.min(getWidth(), getHeight()) / 2f - Ui.dp(getContext(), 10);
        float ringRadius = radius * 0.92f;

        canvas.save();
        canvas.scale(pressScale, pressScale, cx, cy);

        haloPaint.setAlpha((int) (120 + 70 * pulse));
        canvas.drawCircle(cx, cy, radius * 1.18f, haloPaint);

        canvas.drawCircle(cx, cy, ringRadius, ringPaint);

        canvas.save();
        canvas.rotate(spin, cx, cy);
        arcRect.set(cx - ringRadius, cy - ringRadius, cx + ringRadius, cy + ringRadius);
        canvas.drawArc(arcRect, -95f, 210f, false, arcPaint);
        canvas.restore();

        float discRadius = radius * 0.72f;
        canvas.drawCircle(cx, cy, discRadius, discPaint);
        canvas.drawCircle(cx, cy, discRadius, ringPaint);

        float glyphRadius = discRadius * 0.42f;
        arcRect.set(cx - glyphRadius, cy - glyphRadius, cx + glyphRadius, cy + glyphRadius);
        canvas.drawArc(arcRect, -58f, 296f, false, glyphPaint);
        canvas.drawLine(cx, cy - glyphRadius * 1.22f, cx, cy - glyphRadius * 0.08f, glyphPaint);

        if (shockProgress >= 0f) {
            shockPaint.setAlpha((int) (140 * (1f - shockProgress)));
            canvas.drawCircle(cx, cy, discRadius * (0.95f + 0.6f * shockProgress), shockPaint);
        }

        canvas.restore();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float dx = event.getX() - getWidth() / 2f;
        float dy = event.getY() - getHeight() / 2f;
        boolean inside = Math.hypot(dx, dy) <= getWidth() / 2f;

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (!inside) {
                    return false;
                }
                pressed = true;
                animatePress(0.965f);
                return true;
            case MotionEvent.ACTION_UP:
                if (!pressed) {
                    return false;
                }
                pressed = false;
                animatePress(1f);
                if (inside) {
                    performClick();
                }
                return true;
            case MotionEvent.ACTION_CANCEL:
                pressed = false;
                animatePress(1f);
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }

    @Override
    public boolean performClick() {
        super.performClick();
        playShock();
        if (listener != null) {
            listener.onLock();
        }
        return true;
    }

    private void animatePress(float target) {
        if (pressAnimator != null) {
            pressAnimator.cancel();
        }
        pressAnimator = ValueAnimator.ofFloat(pressScale, target);
        pressAnimator.setDuration(160L);
        pressAnimator.setInterpolator(new DecelerateInterpolator());
        pressAnimator.addUpdateListener(animation -> {
            pressScale = (float) animation.getAnimatedValue();
            invalidate();
        });
        pressAnimator.start();
    }

    private void playShock() {
        if (shockAnimator != null) {
            shockAnimator.cancel();
        }
        shockProgress = 0f;
        shockAnimator = ValueAnimator.ofFloat(0f, 1f);
        shockAnimator.setDuration(620L);
        shockAnimator.setInterpolator(new DecelerateInterpolator());
        shockAnimator.addUpdateListener(animation -> {
            shockProgress = (float) animation.getAnimatedValue();
            invalidate();
        });
        shockAnimator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                shockProgress = -1f;
                invalidate();
            }
        });
        shockAnimator.start();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (spinAnimator == null) {
            spinAnimator = ValueAnimator.ofFloat(0f, 360f);
            spinAnimator.setDuration(36000L);
            spinAnimator.setRepeatCount(ValueAnimator.INFINITE);
            spinAnimator.setInterpolator(new LinearInterpolator());
            spinAnimator.addUpdateListener(animation -> {
                spin = (float) animation.getAnimatedValue();
                invalidate();
            });
        }
        spinAnimator.start();

        if (pulseAnimator == null) {
            pulseAnimator = ValueAnimator.ofFloat(0f, 1f);
            pulseAnimator.setDuration(2800L);
            pulseAnimator.setRepeatMode(ValueAnimator.REVERSE);
            pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
            pulseAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
            pulseAnimator.addUpdateListener(animation -> {
                pulse = (float) animation.getAnimatedValue();
                invalidate();
            });
        }
        pulseAnimator.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        if (spinAnimator != null) {
            spinAnimator.cancel();
        }
        if (pulseAnimator != null) {
            pulseAnimator.cancel();
        }
        if (shockAnimator != null) {
            shockAnimator.cancel();
        }
        if (pressAnimator != null) {
            pressAnimator.cancel();
        }
        super.onDetachedFromWindow();
    }
}
