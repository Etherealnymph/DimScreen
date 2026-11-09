package com.etherealnymph.dim;

import android.accessibilityservice.AccessibilityService;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Insets;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.WindowMetrics;
import android.view.accessibility.AccessibilityEvent;

import com.etherealnymph.dim.ui.Ui;

/**
 * 只做两件事：执行系统全局「锁屏」动作；可选地挂一个悬浮球。
 * 不读取窗口内容（canRetrieveWindowContent=false），也不申请任何敏感权限。
 */
public class DimAccessibilityService extends AccessibilityService {

    private static volatile DimAccessibilityService instance;

    private WindowManager windowManager;
    private FloatingBallView ball;
    private WindowManager.LayoutParams ballParams;
    private BroadcastReceiver screenReceiver;

    public static DimAccessibilityService peek() {
        return instance;
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        registerScreenReceiver();
        syncBall();
    }

    public boolean lockScreen() {
        try {
            return performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN);
        } catch (Throwable t) {
            return false;
        }
    }

    /** 设置里的悬浮球开关变化后立即生效。 */
    public void syncBall() {
        if (Prefs.floating(this)) {
            showBall();
        } else {
            hideBall();
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // 应用不需要监听任何界面事件。
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (ball != null && ballParams != null) {
            moveBall(ballParams.x, ballParams.y);
        }
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    public boolean onUnbind(Intent intent) {
        teardown();
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        teardown();
        super.onDestroy();
    }

    private void teardown() {
        unregisterScreenReceiver();
        hideBall();
        if (instance == this) {
            instance = null;
        }
    }

    private void showBall() {
        if (ball != null || windowManager == null) {
            return;
        }
        int[] size = usableSize();
        int ballSize = Ui.dp(this, 62);
        int x;
        int y;
        if (Prefs.hasBallPosition(this)) {
            x = Prefs.ballX(this);
            y = Prefs.ballY(this);
        } else {
            x = size[0] - ballSize - Ui.dp(this, 4);
            y = Math.round(size[1] * 0.58f);
        }
        x = clamp(x, 0, Math.max(0, size[0] - ballSize));
        y = clamp(y, 0, Math.max(0, size[1] - ballSize));

        ball = new FloatingBallView(this);
        ball.setPosition(x, y);
        ball.setListener(new FloatingBallView.Listener() {
            @Override
            public void onTap() {
                Haptics.click(ball);
                lockScreen();
            }

            @Override
            public void onMoved(int newX, int newY) {
                moveBall(newX, newY);
            }

            @Override
            public void onDrop(int newX, int newY) {
                if (ballParams != null) {
                    Prefs.setBallPosition(DimAccessibilityService.this, ballParams.x, ballParams.y);
                }
            }
        });

        ballParams = new WindowManager.LayoutParams(
                ballSize,
                ballSize,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
        ballParams.gravity = Gravity.TOP | Gravity.START;
        ballParams.x = x;
        ballParams.y = y;
        try {
            windowManager.addView(ball, ballParams);
        } catch (Throwable t) {
            ball = null;
            ballParams = null;
        }
    }

    private void hideBall() {
        if (ball != null && windowManager != null) {
            try {
                windowManager.removeView(ball);
            } catch (Throwable ignored) {
                // 视图可能已经随窗口一起销毁。
            }
        }
        ball = null;
        ballParams = null;
    }

    private void moveBall(int x, int y) {
        if (ball == null || ballParams == null || windowManager == null) {
            return;
        }
        int[] size = usableSize();
        int clampedX = clamp(x, 0, Math.max(0, size[0] - ballParams.width));
        int clampedY = clamp(y, 0, Math.max(0, size[1] - ballParams.height));
        ballParams.x = clampedX;
        ballParams.y = clampedY;
        ball.setPosition(clampedX, clampedY);
        try {
            windowManager.updateViewLayout(ball, ballParams);
        } catch (Throwable ignored) {
            // 忽略瞬时窗口销毁。
        }
    }

    private int[] usableSize() {
        if (windowManager == null) {
            return new int[]{1080, 1920};
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowMetrics metrics = windowManager.getCurrentWindowMetrics();
            Rect bounds = metrics.getBounds();
            Insets insets = metrics.getWindowInsets().getInsetsIgnoringVisibility(
                    WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            return new int[]{
                    Math.max(1, bounds.width() - insets.left - insets.right),
                    Math.max(1, bounds.height() - insets.top - insets.bottom)};
        }
        Point point = new Point();
        windowManager.getDefaultDisplay().getSize(point);
        return new int[]{Math.max(1, point.x), Math.max(1, point.y)};
    }

    private void registerScreenReceiver() {
        if (screenReceiver != null) {
            return;
        }
        screenReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (ball == null || intent.getAction() == null) {
                    return;
                }
                switch (intent.getAction()) {
                    case Intent.ACTION_SCREEN_OFF:
                        ball.setVisibility(View.GONE);
                        break;
                    case Intent.ACTION_USER_PRESENT:
                        ball.setVisibility(View.VISIBLE);
                        break;
                    case Intent.ACTION_SCREEN_ON:
                        KeyguardManager keyguard =
                                (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
                        boolean locked = keyguard != null && keyguard.isKeyguardLocked();
                        ball.setVisibility(locked ? View.GONE : View.VISIBLE);
                        break;
                    default:
                        break;
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(screenReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
            } else {
                registerReceiver(screenReceiver, filter);
            }
        } catch (Throwable t) {
            screenReceiver = null;
        }
    }

    private void unregisterScreenReceiver() {
        if (screenReceiver == null) {
            return;
        }
        try {
            unregisterReceiver(screenReceiver);
        } catch (Throwable ignored) {
            // 已注销。
        }
        screenReceiver = null;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }
}
