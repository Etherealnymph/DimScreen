package com.etherealnymph.dim;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

/** 设置面板：锁定方式、悬浮球、触感、撤销设备管理员。 */
final class SettingsSheet {

    private final Activity host;
    private final Runnable onChanged;
    private final Runnable openSetup;

    private Dialog dialog;
    private ImageView checkAuto;
    private ImageView checkAccessibility;
    private ImageView checkAdmin;
    private Switch floatingSwitch;
    private Switch hapticsSwitch;
    private TextView floatingSub;
    private View revokeRow;

    SettingsSheet(Activity host, Runnable onChanged, Runnable openSetup) {
        this.host = host;
        this.onChanged = onChanged;
        this.openSetup = openSetup;
    }

    boolean isShowing() {
        return dialog != null && dialog.isShowing();
    }

    void show() {
        if (isShowing()) {
            refresh();
            return;
        }
        dialog = new Dialog(host, R.style.Theme_Dim_Dialog);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        View content = LayoutInflater.from(host).inflate(R.layout.dialog_settings, null);
        dialog.setContentView(content);
        dialog.setCanceledOnTouchOutside(true);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setDimAmount(0.55f);
            window.setGravity(Gravity.BOTTOM);
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setWindowAnimations(R.style.SheetAnimation);
        }

        checkAuto = content.findViewById(R.id.check_auto);
        checkAccessibility = content.findViewById(R.id.check_acc);
        checkAdmin = content.findViewById(R.id.check_admin);
        floatingSwitch = content.findViewById(R.id.switch_float);
        hapticsSwitch = content.findViewById(R.id.switch_haptics);
        floatingSub = content.findViewById(R.id.float_sub);
        revokeRow = content.findViewById(R.id.revoke_row);

        content.findViewById(R.id.method_auto_row)
                .setOnClickListener(v -> selectMethod(Prefs.METHOD_AUTO));
        content.findViewById(R.id.method_acc_row)
                .setOnClickListener(v -> selectMethod(Prefs.METHOD_ACCESSIBILITY));
        content.findViewById(R.id.method_admin_row)
                .setOnClickListener(v -> selectMethod(Prefs.METHOD_ADMIN));

        content.findViewById(R.id.float_row).setOnClickListener(v -> toggleFloating());
        content.findViewById(R.id.haptics_row).setOnClickListener(v -> toggleHaptics());
        revokeRow.setOnClickListener(v -> confirmRevoke());

        refresh();
        dialog.show();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    void refresh() {
        if (checkAuto == null) {
            return;
        }
        String method = Prefs.method(host);
        checkAuto.setVisibility(Prefs.METHOD_AUTO.equals(method) ? View.VISIBLE : View.INVISIBLE);
        checkAccessibility.setVisibility(
                Prefs.METHOD_ACCESSIBILITY.equals(method) ? View.VISIBLE : View.INVISIBLE);
        checkAdmin.setVisibility(Prefs.METHOD_ADMIN.equals(method) ? View.VISIBLE : View.INVISIBLE);

        boolean accessibility = LockManager.isAccessibilityEnabled(host);
        floatingSwitch.setChecked(Prefs.floating(host) && accessibility);
        floatingSwitch.setEnabled(accessibility);
        floatingSub.setText(accessibility
                ? R.string.settings_floating_sub
                : R.string.row_floating_locked);
        hapticsSwitch.setChecked(Prefs.haptics(host));
        revokeRow.setVisibility(LockManager.isAdminActive(host) ? View.VISIBLE : View.GONE);
    }

    void dismiss() {
        if (dialog != null) {
            dialog.dismiss();
        }
    }

    private void selectMethod(String method) {
        Prefs.setMethod(host, method);
        refresh();
        if (onChanged != null) {
            onChanged.run();
        }
    }

    private void toggleFloating() {
        if (!LockManager.isAccessibilityEnabled(host)) {
            dismiss();
            if (openSetup != null) {
                openSetup.run();
            }
            return;
        }
        boolean next = !Prefs.floating(host);
        Prefs.setFloating(host, next);
        DimAccessibilityService service = DimAccessibilityService.peek();
        if (service != null) {
            service.syncBall();
        } else if (next) {
            Toast.makeText(host, R.string.toast_floating_need_service, Toast.LENGTH_LONG).show();
        }
        refresh();
        if (onChanged != null) {
            onChanged.run();
        }
    }

    private void toggleHaptics() {
        Prefs.setHaptics(host, !Prefs.haptics(host));
        refresh();
        Haptics.click(hapticsSwitch);
    }

    private void confirmRevoke() {
        new AlertDialog.Builder(host, R.style.Theme_Dim_Alert)
                .setTitle(R.string.revoke_title)
                .setMessage(R.string.revoke_message)
                .setPositiveButton(R.string.revoke_confirm, (d, which) -> {
                    if (LockManager.removeAdmin(host)) {
                        Toast.makeText(host, R.string.toast_admin_revoked, Toast.LENGTH_SHORT).show();
                        refresh();
                        if (onChanged != null) {
                            onChanged.run();
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}
