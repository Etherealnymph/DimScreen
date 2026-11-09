package com.etherealnymph.dim;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

/** 只声明 force-lock 的设备管理员，用于 DevicePolicyManager.lockNow()。 */
public class LockAdminReceiver extends DeviceAdminReceiver {

    @Override
    public void onEnabled(Context context, Intent intent) {
        Toast.makeText(context, R.string.toast_admin_enabled, Toast.LENGTH_SHORT).show();
    }

    @Override
    public CharSequence onDisableRequested(Context context, Intent intent) {
        return context.getString(R.string.admin_disable_warning);
    }
}
