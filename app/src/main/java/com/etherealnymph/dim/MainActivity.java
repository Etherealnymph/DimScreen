package com.etherealnymph.dim;

import android.app.Activity;
import android.app.StatusBarManager;
import android.content.ComponentName;
import android.graphics.Color;
import android.graphics.Insets;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.etherealnymph.dim.ui.BreathingDotView;
import com.etherealnymph.dim.ui.LockDialView;
import com.etherealnymph.dim.ui.Ui;

public class MainActivity extends Activity {

    private LockDialView dial;
    private BreathingDotView statusDot;
    private TextView statusText;
    private TextView methodValue;
    private TextView floatingSub;
    private Switch floatingSwitch;

    private SetupSheet setupSheet;
    private SettingsSheet settingsSheet;
    private boolean setupShown;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        applyEdgeToEdge();

        dial = findViewById(R.id.dial);
        statusDot = findViewById(R.id.status_dot);
        statusText = findViewById(R.id.status_text);

        dial.setOnLockListener(this::lockNow);
        findViewById(R.id.btn_settings).setOnClickListener(v -> openSettings());
        findViewById(R.id.status_chip).setOnClickListener(v -> {
            if (LockManager.isConfigured(this)) {
                openSettings();
            } else {
                showSetup();
            }
        });

        buildRows();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
        if (setupSheet != null && setupSheet.isShowing()) {
            setupSheet.refresh();
        }
        if (!LockManager.isConfigured(this) && !setupShown) {
            setupShown = true;
            dial.postDelayed(this::showSetup, 520L);
        }
    }

    @Override
    protected void onDestroy() {
        if (setupSheet != null) {
            setupSheet.dismiss();
        }
        if (settingsSheet != null) {
            settingsSheet.dismiss();
        }
        super.onDestroy();
    }

    private void applyEdgeToEdge() {
        Window window = getWindow();
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);
        window.setNavigationBarContrastEnforced(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.getAttributes().layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false);
        } else {
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }

        View content = findViewById(R.id.content);
        int baseTop = content.getPaddingTop();
        int baseBottom = content.getPaddingBottom();
        content.setOnApplyWindowInsetsListener((view, insets) -> {
            int top;
            int bottom;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Insets bars = insets.getInsets(
                        WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                top = bars.top;
                bottom = bars.bottom;
            } else {
                top = insets.getSystemWindowInsetTop();
                bottom = insets.getSystemWindowInsetBottom();
            }
            view.setPadding(view.getPaddingLeft(), baseTop + top,
                    view.getPaddingRight(), baseBottom + bottom);
            return insets;
        });
        content.requestApplyInsets();
    }

    private void buildRows() {
        LinearLayout card = findViewById(R.id.card);

        View methodRow = addRow(card, R.drawable.ic_method,
                R.string.row_method, R.string.row_method_sub);
        methodValue = new TextView(this);
        methodValue.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
        methodValue.setTextColor(getColor(R.color.text_secondary));
        methodValue.setCompoundDrawablePadding(Ui.dp(this, 4));
        methodValue.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.ic_chevron, 0);
        ((ViewGroup) methodRow.findViewById(R.id.row_action)).addView(methodValue);
        methodRow.setOnClickListener(v -> openSettings());

        addDivider(card);

        View tileRow = addRow(card, R.drawable.ic_tile_row,
                R.string.row_tile, R.string.row_tile_sub);
        TextView tileAction = new TextView(this);
        tileAction.setText(R.string.row_tile_action);
        tileAction.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
        tileAction.setTextColor(getColor(R.color.accent));
        tileAction.setPadding(Ui.dp(this, 8), Ui.dp(this, 6),
                Ui.dp(this, 2), Ui.dp(this, 6));
        ((ViewGroup) tileRow.findViewById(R.id.row_action)).addView(tileAction);
        tileRow.setOnClickListener(v -> addTile());

        addDivider(card);

        View floatingRow = addRow(card, R.drawable.ic_float,
                R.string.row_floating, R.string.row_floating_sub);
        floatingSub = floatingRow.findViewById(R.id.row_sub);
        floatingSwitch = new Switch(this);
        floatingSwitch.setClickable(false);
        floatingSwitch.setFocusable(false);
        ((ViewGroup) floatingRow.findViewById(R.id.row_action)).addView(floatingSwitch);
        floatingRow.setOnClickListener(v -> toggleFloating());
    }

    private View addRow(LinearLayout card, int icon, int title, int subtitle) {
        View row = getLayoutInflater().inflate(R.layout.item_row, card, false);
        ((ImageView) row.findViewById(R.id.row_icon)).setImageResource(icon);
        ((TextView) row.findViewById(R.id.row_title)).setText(title);
        ((TextView) row.findViewById(R.id.row_sub)).setText(subtitle);
        card.addView(row);
        return row;
    }

    private void addDivider(LinearLayout card) {
        View divider = new View(this);
        divider.setBackgroundColor(0x0FFFFFFF);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 1));
        params.setMarginStart(Ui.dp(this, 58));
        card.addView(divider, params);
    }

    private void refresh() {
        boolean accessibilityEnabled = LockManager.isAccessibilityEnabled(this);
        boolean accessibilityLive = LockManager.isAccessibilityConnected();
        LockManager.Method method = LockManager.effective(this);

        if (method == LockManager.Method.NONE) {
            statusDot.setColor(getColor(R.color.warn));
            statusText.setText(accessibilityEnabled && !accessibilityLive
                    ? R.string.status_stale
                    : R.string.status_missing);
        } else {
            statusDot.setColor(getColor(R.color.accent));
            statusText.setText(method == LockManager.Method.ACCESSIBILITY
                    ? R.string.status_ready_accessibility
                    : R.string.status_ready_admin);
        }

        methodValue.setText(methodSummary());
        floatingSwitch.setEnabled(accessibilityEnabled);
        floatingSwitch.setChecked(Prefs.floating(this) && accessibilityEnabled);
        floatingSub.setText(accessibilityEnabled
                ? R.string.row_floating_sub
                : R.string.row_floating_locked);
    }

    private int methodSummary() {
        String pref = Prefs.method(this);
        LockManager.Method method = LockManager.effective(this);
        if (method == LockManager.Method.NONE) {
            return R.string.method_value_none;
        }
        if (Prefs.METHOD_ACCESSIBILITY.equals(pref)) {
            return method == LockManager.Method.ACCESSIBILITY
                    ? R.string.method_value_accessibility
                    : R.string.method_value_accessibility_fallback;
        }
        if (Prefs.METHOD_ADMIN.equals(pref)) {
            return method == LockManager.Method.DEVICE_ADMIN
                    ? R.string.method_value_admin
                    : R.string.method_value_admin_fallback;
        }
        return method == LockManager.Method.ACCESSIBILITY
                ? R.string.method_value_auto_accessibility
                : R.string.method_value_auto_admin;
    }

    private void lockNow() {
        Haptics.click(dial);
        LockManager.Result result = LockManager.lock(this);
        if (result == LockManager.Result.LOCKED) {
            overridePendingTransition(0, 0);
            finishAndRemoveTask();
            return;
        }
        if (result == LockManager.Result.NO_METHOD) {
            showSetup();
        } else {
            Toast.makeText(this, R.string.toast_lock_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void addTile() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Toast.makeText(this, R.string.toast_tile_unsupported, Toast.LENGTH_LONG).show();
            return;
        }
        StatusBarManager statusBar = getSystemService(StatusBarManager.class);
        if (statusBar == null) {
            Toast.makeText(this, R.string.toast_tile_unsupported, Toast.LENGTH_LONG).show();
            return;
        }
        try {
            statusBar.requestAddTileService(
                    new ComponentName(this, LockTileService.class),
                    getString(R.string.tile_label),
                    Icon.createWithResource(this, R.drawable.ic_tile),
                    getMainExecutor(),
                    result -> {
                        int message;
                        if (result == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED) {
                            message = R.string.toast_tile_added;
                        } else if (result
                                == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED) {
                            message = R.string.toast_tile_exists;
                        } else {
                            message = R.string.toast_tile_failed;
                        }
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                    });
        } catch (Throwable t) {
            Toast.makeText(this, R.string.toast_tile_unsupported, Toast.LENGTH_LONG).show();
        }
    }

    private void toggleFloating() {
        if (!LockManager.isAccessibilityEnabled(this)) {
            showSetup();
            return;
        }
        boolean next = !Prefs.floating(this);
        Prefs.setFloating(this, next);
        DimAccessibilityService service = DimAccessibilityService.peek();
        if (service != null) {
            service.syncBall();
        } else if (next) {
            Toast.makeText(this, R.string.toast_floating_need_service, Toast.LENGTH_LONG).show();
        }
        refresh();
    }

    private void showSetup() {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        if (settingsSheet != null && settingsSheet.isShowing()) {
            settingsSheet.dismiss();
        }
        if (setupSheet == null) {
            setupSheet = new SetupSheet(this, this::refresh);
        }
        setupSheet.show();
    }

    private void openSettings() {
        if (setupSheet != null && setupSheet.isShowing()) {
            setupSheet.dismiss();
        }
        if (settingsSheet == null) {
            settingsSheet = new SettingsSheet(this, this::refresh, this::showSetup);
        }
        settingsSheet.show();
    }
}
