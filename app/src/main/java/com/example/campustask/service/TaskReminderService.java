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
import com.example.campustask.model.TaskItem;
import com.example.campustask.reminder.ReminderScheduler;

import java.util.List;

/**
 * 成员C负责的Service组件。
 * 任务提醒调度管理前台服务：启动时扫描数据库中所有未完成且有提醒时间的任务，
 * 通过ReminderScheduler统一注册AlarmManager闹钟。
 * 提供任务变更后的集中重新调度入口。
 */
public class TaskReminderService extends Service {
    public static final String CHANNEL_ID = "task_reminder_service_channel";
    public static final int NOTIFICATION_ID = 4001;
    public static final String ACTION_RESCHEDULE = "com.example.campustask.ACTION_RESCHEDULE";
    private static final String PREFS_NAME = "campus_hub_auth";
    private static final String PREF_USERNAME = "username";

    @Override
    public void onCreate() {
        super.onCreate();
        ensureChannel();
        startForeground(NOTIFICATION_ID, buildNotification("任务提醒服务运行中"));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        rescheduleAllTasks();
        stopSelf();
        return START_NOT_STICKY;
    }

    private void rescheduleAllTasks() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String username = prefs.getString(PREF_USERNAME, "");
        if (username == null || username.isEmpty()) {
            return;
        }
        CampusTaskRepository repository = new CampusTaskRepository(this);
        repository.useAccount(username);
        ReminderScheduler scheduler = new ReminderScheduler(this);
        List<TaskItem> tasks = repository.getTasks();
        for (TaskItem task : tasks) {
            scheduler.schedule(task);
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
                .setContentTitle("待办提醒")
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
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "任务提醒服务", NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("任务提醒调度管理服务");
        manager.createNotificationChannel(channel);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
