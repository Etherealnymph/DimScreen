package com.etherealnymph.dim;

import android.content.Context;
import android.content.SharedPreferences;

/** 应用唯一的本地状态：锁定方式、悬浮球开关与位置、触感开关。全部留在本机。 */
public final class Prefs {

    public static final String METHOD_AUTO = "auto";
    public static final String METHOD_ACCESSIBILITY = "accessibility";
    public static final String METHOD_ADMIN = "admin";

    private static final String FILE = "dim";
    private static final int NO_POSITION = Integer.MIN_VALUE;

    private Prefs() {
    }

    private static SharedPreferences sp(Context context) {
        return context.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public static String method(Context context) {
        return sp(context).getString("method", METHOD_AUTO);
    }

    public static void setMethod(Context context, String method) {
        sp(context).edit().putString("method", method).apply();
    }

    public static boolean floating(Context context) {
        return sp(context).getBoolean("floating", false);
    }

    public static void setFloating(Context context, boolean enabled) {
        sp(context).edit().putBoolean("floating", enabled).apply();
    }

    public static boolean haptics(Context context) {
        return sp(context).getBoolean("haptics", true);
    }

    public static void setHaptics(Context context, boolean enabled) {
        sp(context).edit().putBoolean("haptics", enabled).apply();
    }

    public static boolean hasBallPosition(Context context) {
        return sp(context).getInt("ball_x", NO_POSITION) != NO_POSITION;
    }

    public static int ballX(Context context) {
        return sp(context).getInt("ball_x", NO_POSITION);
    }

    public static int ballY(Context context) {
        return sp(context).getInt("ball_y", NO_POSITION);
    }

    public static void setBallPosition(Context context, int x, int y) {
        sp(context).edit().putInt("ball_x", x).putInt("ball_y", y).apply();
    }
}
