package com.etherealnymph.dim.ui;

import android.content.Context;

public final class Ui {

    private Ui() {
    }

    public static int dp(Context context, float value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
