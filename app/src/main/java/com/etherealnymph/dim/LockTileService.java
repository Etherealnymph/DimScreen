package com.etherealnymph.dim;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

/** 下拉控制中心里的「熄屏」磁贴。 */
public class LockTileService extends TileService {

    @Override
    public void onStartListening() {
        super.onStartListening();
        syncTile();
    }

    @Override
    public void onTileAdded() {
        super.onTileAdded();
        syncTile();
    }

    @Override
    public void onClick() {
        super.onClick();
        if (isLocked()) {
            // 锁屏界面可操作（未设置安全锁）时先解锁再执行，系统会恢复原状。
            unlockAndRun(this::attemptLock);
        } else {
            attemptLock();
        }
    }

    private void attemptLock() {
        LockManager.Result result = LockManager.lock(this);
        if (result == LockManager.Result.LOCKED) {
            return;
        }
        openApp();
    }

    private void openApp() {
        Intent intent = new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                PendingIntent pendingIntent = PendingIntent.getActivity(
                        this, 0, intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                startActivityAndCollapse(pendingIntent);
            } else {
                startActivityAndCollapse(intent);
            }
        } catch (Throwable t) {
            try {
                startActivity(intent);
            } catch (Throwable ignored) {
                // 系统拒绝启动界面时保持静默。
            }
        }
    }

    private void syncTile() {
        Tile tile = getQsTile();
        if (tile == null) {
            return;
        }
        boolean ready = LockManager.effective(this) != LockManager.Method.NONE;
        tile.setState(ready ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        tile.setLabel(getString(R.string.tile_label));
        tile.setContentDescription(getString(R.string.tile_description));
        try {
            tile.updateTile();
        } catch (Throwable ignored) {
            // 磁贴尚未绑定。
        }
    }
}
