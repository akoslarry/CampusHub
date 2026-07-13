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

/**
 * 上课提醒广播接收器。
 * 接收AlarmManager闹钟广播，发送"还有15分钟上课"通知。
 */
public class CourseReminderReceiver extends BroadcastReceiver {
    public static final String EXTRA_COURSE_NAME = "course_name";
    public static final String EXTRA_COURSE_LOCATION = "course_location";
    public static final String EXTRA_COURSE_WEEKDAY = "course_weekday";
    public static final String EXTRA_COURSE_SECTION = "course_section";
    private static final String CHANNEL_ID = "campus_course_reminders";

    @Override
    public void onReceive(Context context, Intent intent) {
        String courseName = intent.getStringExtra(EXTRA_COURSE_NAME);
        String location = intent.getStringExtra(EXTRA_COURSE_LOCATION);
        int weekday = intent.getIntExtra(EXTRA_COURSE_WEEKDAY, 1);
        int section = intent.getIntExtra(EXTRA_COURSE_SECTION, 1);

        if (!canPostNotifications(context)) {
            return;
        }

        ensureChannel(context);

        Intent openIntent = new Intent(context, MainActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, openIntent, flags);

        String title = courseName == null ? "\u4e0a\u8bfe\u63d0\u9192" : courseName;
        String body = "\u8fd8\u670915\u5206\u949f\u4e0a\u8bfe";
        if (location != null && !location.isEmpty()) {
            body += "\uff0c\u5730\u70b9\uff1a" + location;
        }
        body += "\uff08\u7b2c" + section + "\u8282\uff09";

        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(context, CHANNEL_ID)
                : new Notification.Builder(context);
        builder.setSmallIcon(R.drawable.ic_stat_task)
                .setContentTitle(title)
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
        if (manager != null) {
            int notificationId = 60000 + weekday * 100 + section;
            manager.notify(notificationId, builder.build());
        }
    }

    private static boolean canPostNotifications(Context context) {
        return Build.VERSION.SDK_INT < 33
                || context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }

    private static void ensureChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return;
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "\u4e0a\u8bfe\u63d0\u9192",
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription("\u8bfe\u7a0b\u5f00\u59cb\u524d15\u5206\u949f\u63d0\u9192");
        channel.enableVibration(true);
        manager.createNotificationChannel(channel);
    }
}
