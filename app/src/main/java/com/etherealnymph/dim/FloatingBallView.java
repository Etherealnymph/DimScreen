package com.etherealnymph.dim;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import com.etherealnymph.dim.ui.Ui;

/**
 * 无障碍服务里的悬浮球：点一下锁屏，按住拖动换位置。
 * 它挂在 TYPE_ACCESSIBILITY_OVERLAY 上，不需要「显示在其他应用上层」权限。
 */
class FloatingBallView extends View {

    interface Listener {
        void onTap();

        void onMoved(int x, int y);

        void onDrop(int x, int y);
    }

    private final Paint haloPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint discPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glyphPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF glyphRect = new RectF();
    private final int touchSlop;

    private Listener listener;
    private int positionX;
    private int positionY;
    private float downRawX;
    private float downRawY;
    private int startX;
    private int startY;
    private boolean dragging;

    FloatingBallView(Context context) {
        super(context);
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        setClickable(true);
        setFocusable(true);
        setContentDescription(context.getString(R.string.floating_description));

        discPaint.setColor(0xF2141C27);
        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeWidth(Ui.dp(context, 1.2f));
        ringPaint.setColor(0x9A74E8C8);
        glyphPaint.setStyle(Paint.Style.STROKE);
        glyphPaint.setStrokeWidth(Ui.dp(context, 2.3f));
        glyphPaint.setStrokeCap(Paint.Cap.ROUND);
        glyphPaint.setColor(0xFF74E8C8);
        setElevation(Ui.dp(context, 6));
    }

    void setListener(Listener listener) {
        this.listener = listener;
    }

    void setPosition(int x, int y) {
        positionX = x;
        positionY = y;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float cx = w / 2f;
        float cy = h / 2f;
        float radius = Math.min(w, h) / 2f - Ui.dp(getContext(), 7);
        haloPaint.setShader(new RadialGradient(
                cx, cy, Math.max(1f, radius * 1.32f),
                new int[]{0x5574E8C8, 0x2274E8C8, 0x0074E8C8},
                new float[]{0f, 0.55f, 1f},
                Shader.TileMode.CLAMP));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float radius = Math.min(getWidth(), getHeight()) / 2f - Ui.dp(getContext(), 7);

        canvas.drawCircle(cx, cy, radius * 1.3f, haloPaint);
        canvas.drawCircle(cx, cy, radius, discPaint);
        canvas.drawCircle(cx, cy, radius, ringPaint);

        float glyphRadius = radius * 0.40f;
        glyphRect.set(cx - glyphRadius, cy - glyphRadius, cx + glyphRadius, cy + glyphRadius);
        canvas.drawArc(glyphRect, -58f, 296f, false, glyphPaint);
        canvas.drawLine(cx, cy - glyphRadius * 1.16f, cx, cy - glyphRadius * 0.10f, glyphPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downRawX = event.getRawX();
                downRawY = event.getRawY();
                startX = positionX;
                startY = positionY;
                dragging = false;
                animate().scaleX(0.94f).scaleY(0.94f).setDuration(90).start();
                return true;
            case MotionEvent.ACTION_MOVE: {
                float dx = event.getRawX() - downRawX;
                float dy = event.getRawY() - downRawY;
                if (!dragging && Math.hypot(dx, dy) > touchSlop) {
                    dragging = true;
                }
                if (dragging && listener != null) {
                    listener.onMoved(Math.round(startX + dx), Math.round(startY + dy));
                }
                return true;
            }
            case MotionEvent.ACTION_UP:
                animate().scaleX(1f).scaleY(1f).setDuration(120).start();
                if (dragging) {
                    if (listener != null) {
                        listener.onDrop(positionX, positionY);
                    }
                } else {
                    performClick();
                }
                return true;
            case MotionEvent.ACTION_CANCEL:
                animate().scaleX(1f).scaleY(1f).setDuration(120).start();
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }

    @Override
    public boolean performClick() {
        super.performClick();
        if (listener != null) {
            listener.onTap();
        }
        return true;
    }
}
