package com.example.campustask.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.example.campustask.service.ScheduleSyncService;

/**
 * 成员A负责的BroadcastReceiver组件。
 * 接收课表同步定时广播，启动ScheduleSyncService执行课表数据完整性检查。
 */
public class ScheduleSyncReceiver extends BroadcastReceiver {
    public static final String ACTION_SCHEDULE_SYNC = "com.example.campustask.ACTION_SCHEDULE_SYNC";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent != null ? intent.getAction() : null;
        if (!ACTION_SCHEDULE_SYNC.equals(action)) {
            return;
        }
        Intent serviceIntent = new Intent(context, ScheduleSyncService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
    }
}
