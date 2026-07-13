package com.example.campustask.reminder;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.example.campustask.model.TaskItem;
import com.example.campustask.model.TaskRules;

public class ReminderScheduler {
    private final Context context;
    private final AlarmManager alarmManager;

    public ReminderScheduler(Context context) {
        this.context = context.getApplicationContext();
        alarmManager = (AlarmManager) this.context.getSystemService(Context.ALARM_SERVICE);
    }

    public void schedule(TaskItem task) {
        if (task == null || task.completed || !TaskRules.canScheduleReminder(task.remindAtMillis, System.currentTimeMillis())) {
            cancel(task == null ? -1 : task.id);
            return;
        }
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.putExtra(ReminderReceiver.EXTRA_TASK_ID, task.id);
        intent.putExtra(ReminderReceiver.EXTRA_TITLE, task.title);
        intent.putExtra(ReminderReceiver.EXTRA_DESCRIPTION, task.description);
        intent.putExtra(ReminderReceiver.EXTRA_REPEAT_MINUTES, task.repeatMinutes);
        intent.putExtra(ReminderReceiver.EXTRA_REMIND_AT, task.remindAtMillis);
        PendingIntent pendingIntent = pendingIntent(task.id, intent);
        scheduleAt(task.id, task.remindAtMillis, pendingIntent);
    }

    public void scheduleRepeat(long taskId, String title, String description, int repeatMinutes, long remindAtMillis) {
        if (taskId <= 0 || repeatMinutes <= 0 || !TaskRules.canScheduleReminder(remindAtMillis, System.currentTimeMillis())) {
            return;
        }
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.putExtra(ReminderReceiver.EXTRA_TASK_ID, taskId);
        intent.putExtra(ReminderReceiver.EXTRA_TITLE, title);
        intent.putExtra(ReminderReceiver.EXTRA_DESCRIPTION, description);
        intent.putExtra(ReminderReceiver.EXTRA_REPEAT_MINUTES, repeatMinutes);
        intent.putExtra(ReminderReceiver.EXTRA_REMIND_AT, remindAtMillis);
        scheduleAt(taskId, remindAtMillis, pendingIntent(taskId, intent));
    }

    private void scheduleAt(long taskId, long remindAtMillis, PendingIntent pendingIntent) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, remindAtMillis, pendingIntent);
                return;
            }
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, remindAtMillis, pendingIntent);
        } catch (SecurityException e) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, remindAtMillis, pendingIntent);
        }
    }

    public void cancel(long taskId) {
        if (taskId <= 0) {
            return;
        }
        Intent intent = new Intent(context, ReminderReceiver.class);
        alarmManager.cancel(pendingIntent(taskId, intent));
    }

    private PendingIntent pendingIntent(long taskId, Intent intent) {
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getBroadcast(context, (int) taskId, intent, flags);
    }
}
