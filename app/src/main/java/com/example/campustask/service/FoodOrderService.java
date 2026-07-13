package com.example.campustask.service;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;

import com.example.campustask.MainActivity;
import com.example.campustask.R;
import com.example.campustask.data.CampusTaskRepository;
import com.example.campustask.model.FoodOrder;
import com.example.campustask.model.FoodOrderRules;

import java.util.List;

/**
 * 成员B负责的Service组件。
 * 外卖订单状态流转前台服务：定时扫描所有"待接单"和"制作中"的订单，
 * 自动推进订单状态（待接单→制作中→待取餐→已完成），模拟真实外卖流程。
 */
public class FoodOrderService extends Service {
    public static final String CHANNEL_ID = "food_order_channel";
    public static final int NOTIFICATION_ID = 3001;
    public static final long CHECK_INTERVAL_MS = 30 * 1000L;
    private static final String PREFS_NAME = "campus_hub_auth";
    private static final String PREF_USERNAME = "username";

    @Override
    public void onCreate() {
        super.onCreate();
        ensureChannel();
        startForeground(NOTIFICATION_ID, buildNotification("外卖订单服务运行中"));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        advanceOrders();
        scheduleNextCheck();
        stopSelf();
        return START_NOT_STICKY;
    }

    private void advanceOrders() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String username = prefs.getString(PREF_USERNAME, "");
        if (username == null || username.isEmpty()) {
            return;
        }
        CampusTaskRepository repository = new CampusTaskRepository(this);
        repository.useAccount(username);
        List<FoodOrder> orders = repository.getFoodOrders();
        for (FoodOrder order : orders) {
            if (FoodOrderRules.STATUS_COMPLETED.equals(order.status)
                    || FoodOrderRules.STATUS_CANCELED.equals(order.status)) {
                continue;
            }
            String next = FoodOrderRules.nextStatus(order.status);
            if (!next.equals(order.status)) {
                repository.updateFoodOrderStatus(order.id, next);
                notifyOrderAdvanced(order, next);
            }
        }
    }

    private void notifyOrderAdvanced(FoodOrder order, String newStatus) {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            String text = order.merchantName + " 订单状态更新：" + newStatus;
            manager.notify((int) (NOTIFICATION_ID + order.id), buildNotification(text));
        }
    }

    private void scheduleNextCheck() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }
        Intent intent = new Intent(this, com.example.campustask.receiver.FoodOrderReceiver.class);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, flags);
        long triggerAt = System.currentTimeMillis() + CHECK_INTERVAL_MS;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent);
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent);
            }
        } catch (SecurityException e) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent);
        }
    }

    private Notification buildNotification(String text) {
        Intent openIntent = new Intent(this, MainActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, openIntent, flags);
        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);
        return builder
                .setSmallIcon(R.drawable.ic_stat_task)
                .setContentTitle("校园外卖")
                .setContentText(text)
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .setPriority(Notification.PRIORITY_DEFAULT)
                .build();
    }

    private void ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) {
            return;
        }
        if (manager.getNotificationChannel(CHANNEL_ID) != null) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "外卖订单", NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription("外卖订单状态流转通知");
        manager.createNotificationChannel(channel);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
