package com.example.campustask.reminder;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import com.example.campustask.MainActivity;
import com.example.campustask.R;
import com.example.campustask.model.TaskRules;

public class ReminderReceiver extends BroadcastReceiver {
    public static final String EXTRA_TASK_ID = "task_id";
    public static final String EXTRA_TITLE = "task_title";
    public static final String EXTRA_DESCRIPTION = "task_description";
    public static final String EXTRA_REPEAT_MINUTES = "repeat_minutes";
    public static final String EXTRA_REMIND_AT = "remind_at";
    private static final String CHANNEL_ID = "campus_task_reminders";

    @Override
    public void onReceive(Context context, Intent intent) {
        long taskId = intent.getLongExtra(EXTRA_TASK_ID, 0L);
        String title = intent.getStringExtra(EXTRA_TITLE);
        String description = intent.getStringExtra(EXTRA_DESCRIPTION);
        int repeatMinutes = intent.getIntExtra(EXTRA_REPEAT_MINUTES, 0);

        if (!canPostNotifications(context)) {
            rescheduleNextRepeat(context, taskId, title, description, repeatMinutes);
            return;
        }

        ensureChannel(context);
        Intent openIntent = new Intent(context, MainActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(
                context,
                (int) taskId,
                openIntent,
                pendingFlags()
        );
        String safeTitle = title == null || title.trim().isEmpty() ? "\u4efb\u52a1\u63d0\u9192" : title;
        String body = description == null || description.trim().isEmpty()
                ? "\u6709\u4e00\u9879\u6821\u56ed\u4efb\u52a1\u9700\u8981\u5904\u7406"
                : description;
        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(context, CHANNEL_ID)
                : new Notification.Builder(context);
        builder.setSmallIcon(R.drawable.ic_stat_task)
                .setContentTitle(safeTitle)
                .setContentText(body)
                .setStyle(new Notification.BigTextStyle().bigText(body))
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .setCategory(Notification.CATEGORY_REMINDER)
                .setPriority(Notification.PRIORITY_HIGH)
                .setDefaults(Notification.DEFAULT_ALL)
                .setWhen(System.currentTimeMillis())
                .setShowWhen(true);
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify((int) taskId, builder.build());
        rescheduleNextRepeat(context, taskId, title, description, repeatMinutes);
    }

    private static boolean canPostNotifications(Context context) {
        return Build.VERSION.SDK_INT < 33
                || context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }

    private static void rescheduleNextRepeat(Context context, long taskId, String title, String description, int repeatMinutes) {
        long nextReminderAt = TaskRules.nextRepeatReminderAt(System.currentTimeMillis(), repeatMinutes);
        if (nextReminderAt > 0) {
            new ReminderScheduler(context).scheduleRepeat(taskId, title, description, repeatMinutes, nextReminderAt);
        }
    }

    private static void ensureChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel existing = manager.getNotificationChannel(CHANNEL_ID);
        if (existing != null) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "\u4efb\u52a1\u63d0\u9192",
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription("\u6821\u56ed\u4efb\u52a1\u622a\u6b62\u65f6\u95f4\u63d0\u9192");
        channel.enableVibration(true);
        manager.createNotificationChannel(channel);
    }

    private static int pendingFlags() {
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return flags;
    }
}
