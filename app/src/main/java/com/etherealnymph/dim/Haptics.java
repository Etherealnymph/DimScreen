package com.etherealnymph.dim;

import android.content.Context;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;

/** 轻触反馈。没有振动马达的设备会安静地跳过。 */
public final class Haptics {

    private Haptics() {
    }

    public static void click(View view) {
        if (view == null) {
            return;
        }
        Context context = view.getContext();
        if (!Prefs.haptics(context)) {
            return;
        }
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator == null || !vibrator.hasVibrator()) {
            return;
        }
        try {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK));
        } catch (Throwable ignored) {
            // 个别定制 ROM 会禁用预定义效果，忽略即可。
        }
    }
}
