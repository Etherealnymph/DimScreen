package com.etherealnymph.dim;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

/** 底部弹出的授权引导。两种方式并列，已开启的一项会点亮。 */
final class SetupSheet {

    private final Activity host;
    private final Runnable onChanged;

    private Dialog dialog;
    private View accCard;
    private View adminCard;
    private TextView accState;
    private TextView adminState;
    private TextView accAction;
    private TextView adminAction;

    SetupSheet(Activity host, Runnable onChanged) {
        this.host = host;
        this.onChanged = onChanged;
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
        View content = LayoutInflater.from(host).inflate(R.layout.dialog_setup, null);
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

        accCard = content.findViewById(R.id.setup_acc_card);
        adminCard = content.findViewById(R.id.setup_admin_card);
        accState = content.findViewById(R.id.setup_acc_state);
        adminState = content.findViewById(R.id.setup_admin_state);
        accAction = content.findViewById(R.id.setup_acc_action);
        adminAction = content.findViewById(R.id.setup_admin_action);

        content.findViewById(R.id.setup_close).setOnClickListener(v -> dismiss());
        accCard.setOnClickListener(v -> openAccessibility());
        adminCard.setOnClickListener(v -> {
            if (LockManager.isAdminActive(host)) {
                Toast.makeText(host, R.string.setup_already_on, Toast.LENGTH_SHORT).show();
            } else {
                LockManager.requestAdmin(host);
            }
        });
        dialog.setOnDismissListener(d -> {
            if (onChanged != null) {
                onChanged.run();
            }
        });

        refresh();
        dialog.show();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    void refresh() {
        if (accCard == null) {
            return;
        }
        boolean accessibility = LockManager.isAccessibilityEnabled(host);
        boolean admin = LockManager.isAdminActive(host);

        accState.setText(accessibility ? R.string.setup_state_on : R.string.setup_state_off);
        accState.setTextColor(host.getColor(accessibility ? R.color.accent : R.color.text_tertiary));
        accAction.setText(accessibility ? R.string.setup_action_done : R.string.setup_action_open);
        accAction.setTextColor(host.getColor(accessibility ? R.color.text_tertiary : R.color.accent));
        accCard.setBackgroundResource(accessibility ? R.drawable.bg_option_on : R.drawable.bg_option);

        adminState.setText(admin ? R.string.setup_state_on : R.string.setup_state_off);
        adminState.setTextColor(host.getColor(admin ? R.color.accent : R.color.text_tertiary));
        adminAction.setText(admin ? R.string.setup_action_done : R.string.setup_action_open);
        adminAction.setTextColor(host.getColor(admin ? R.color.text_tertiary : R.color.accent));
        adminCard.setBackgroundResource(admin ? R.drawable.bg_option_on : R.drawable.bg_option);
    }

    void dismiss() {
        if (dialog != null) {
            dialog.dismiss();
        }
    }

    private void openAccessibility() {
        Toast.makeText(host, R.string.setup_accessibility_hint, Toast.LENGTH_LONG).show();
        LockManager.openAccessibilitySettings(host);
    }
}
