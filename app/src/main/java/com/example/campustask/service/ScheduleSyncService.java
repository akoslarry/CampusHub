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
import com.example.campustask.model.Course;

import java.util.List;

/**
 * 成员A负责的Service组件。
 * 课表同步检查前台服务：定时扫描课程表数据，检查课程数据完整性，
 * 如果课程数量为零则发送通知提醒用户导入教务系统课表。
 */
public class ScheduleSyncService extends Service {
    public static final String CHANNEL_ID = "schedule_sync_channel";
    public static final int NOTIFICATION_ID = 2001;
    public static final long CHECK_INTERVAL_MS = 24 * 60 * 60 * 1000L;
    private static final String PREFS_NAME = "campus_hub_auth";
    private static final String PREF_USERNAME = "username";

    @Override
    public void onCreate() {
        super.onCreate();
        ensureChannel();
        startForeground(NOTIFICATION_ID, buildNotification("课表同步服务运行中"));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        checkScheduleIntegrity();
        scheduleNextCheck();
        stopSelf();
        return START_NOT_STICKY;
    }

    private void checkScheduleIntegrity() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String username = prefs.getString(PREF_USERNAME, "");
        if (username == null || username.isEmpty()) {
            return;
        }
        CampusTaskRepository repository = new CampusTaskRepository(this);
        repository.useAccount(username);
        List<Course> courses = repository.getCourses();
        if (courses.isEmpty()) {
            notifyScheduleEmpty();
        }
    }

    private void notifyScheduleEmpty() {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID + 1, buildNotification("你的课表为空，点击导入教务系统课表"));
        }
    }

    private void scheduleNextCheck() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }
        Intent intent = new Intent(this, com.example.campustask.receiver.ScheduleSyncReceiver.class);
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
                .setContentTitle("课表同步")
                .setContentText(text)
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .setPriority(Notification.PRIORITY_LOW)
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
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "课表同步", NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("课表数据完整性检查");
        manager.createNotificationChannel(channel);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
