package com.etherealnymph.dim;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.text.TextUtils;

/**
 * 熄屏能力的唯一入口。
 *
 * <p>Android 14 上没有第三方应用可以直接调用 PowerManager.goToSleep()（那是
 * signature 权限），普通应用能用的只有两条路：
 *
 * <ol>
 *   <li>无障碍服务的 GLOBAL_ACTION_LOCK_SCREEN（API 28+），不需要设备管理员；</li>
 *   <li>设备管理员的 DevicePolicyManager.lockNow()，只需要 force-lock 一项策略。</li>
 * </ol>
 *
 * <p>这里把两条路收敛成一次调用，并按照用户偏好自动回退。
 */
public final class LockManager {

    public enum Method {
        NONE,
        ACCESSIBILITY,
        DEVICE_ADMIN
    }

    public enum Result {
        LOCKED,
        NO_METHOD,
        FAILED
    }

    private LockManager() {
    }

    public static ComponentName adminComponent(Context context) {
        return new ComponentName(context, LockAdminReceiver.class);
    }

    public static boolean isAdminActive(Context context) {
        DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        if (dpm == null) {
            return false;
        }
        try {
            return dpm.isAdminActive(adminComponent(context));
        } catch (Throwable ignored) {
            return false;
        }
    }

    /** 用户是否在系统设置里勾选了本应用的无障碍服务。 */
    public static boolean isAccessibilityEnabled(Context context) {
        String flat = Settings.Secure.getString(
                context.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (TextUtils.isEmpty(flat)) {
            return false;
        }
        ComponentName me = new ComponentName(context, DimAccessibilityService.class);
        String full = me.flattenToString();
        String shortName = me.flattenToShortString();
        for (String entry : flat.split(":")) {
            if (entry.equalsIgnoreCase(full) || entry.equalsIgnoreCase(shortName)) {
                return true;
            }
        }
        return false;
    }

    /** 无障碍服务进程是否真的连上了（勾选之后被系统杀掉时会是 false）。 */
    public static boolean isAccessibilityConnected() {
        return DimAccessibilityService.peek() != null;
    }

    /** 是否已经完成过任意一种授权。用于决定启动时要不要弹引导。 */
    public static boolean isConfigured(Context context) {
        return isAccessibilityEnabled(context) || isAdminActive(context);
    }

    public static Method effective(Context context) {
        String pref = Prefs.method(context);
        boolean accessibility = isAccessibilityConnected();
        boolean admin = isAdminActive(context);

        if (Prefs.METHOD_ACCESSIBILITY.equals(pref)) {
            if (accessibility) {
                return Method.ACCESSIBILITY;
            }
            return admin ? Method.DEVICE_ADMIN : Method.NONE;
        }
        if (Prefs.METHOD_ADMIN.equals(pref)) {
            if (admin) {
                return Method.DEVICE_ADMIN;
            }
            return accessibility ? Method.ACCESSIBILITY : Method.NONE;
        }
        if (accessibility) {
            return Method.ACCESSIBILITY;
        }
        return admin ? Method.DEVICE_ADMIN : Method.NONE;
    }

    /** 立即熄屏。可从 Activity、TileService、无障碍服务里调用。 */
    public static Result lock(Context context) {
        Method method = effective(context);
        if (method == Method.ACCESSIBILITY) {
            DimAccessibilityService service = DimAccessibilityService.peek();
            if (service != null && service.lockScreen()) {
                return Result.LOCKED;
            }
            return lockWithAdmin(context) ? Result.LOCKED : Result.FAILED;
        }
        if (method == Method.DEVICE_ADMIN) {
            return lockWithAdmin(context) ? Result.LOCKED : Result.FAILED;
        }
        return Result.NO_METHOD;
    }

    @SuppressWarnings("deprecation")
    private static boolean lockWithAdmin(Context context) {
        if (!isAdminActive(context)) {
            return false;
        }
        DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        if (dpm == null) {
            return false;
        }
        try {
            dpm.lockNow();
            return true;
        } catch (SecurityException | IllegalStateException e) {
            return false;
        }
    }

    public static void openAccessibilitySettings(Activity activity) {
        try {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            activity.startActivity(intent);
        } catch (Throwable ignored) {
            // 极少数 ROM 没有这个入口，保持静默。
        }
    }

    public static void requestAdmin(Activity activity) {
        try {
            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent(activity));
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    activity.getString(R.string.admin_explanation));
            activity.startActivity(intent);
        } catch (Throwable ignored) {
            // 部分定制 ROM 会移除设备管理员入口。
        }
    }

    public static boolean removeAdmin(Context context) {
        DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        if (dpm == null) {
            return false;
        }
        ComponentName component = adminComponent(context);
        try {
            if (!dpm.isAdminActive(component)) {
                return false;
            }
            dpm.removeActiveAdmin(component);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
