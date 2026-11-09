package com.etherealnymph.dim;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 * 桌面长按图标的「立即熄屏」入口：没有界面，锁屏完成就退出。
 * 没有授权时退回到主界面，让用户先完成设置。
 */
public class QuickLockActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LockManager.Result result = LockManager.lock(this);
        if (result == LockManager.Result.LOCKED) {
            finish();
            overridePendingTransition(0, 0);
            return;
        }
        startActivity(new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP));
        finish();
        overridePendingTransition(0, 0);
    }
}
