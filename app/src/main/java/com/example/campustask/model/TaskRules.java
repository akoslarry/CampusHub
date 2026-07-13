package com.example.campustask.model;

import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Locale;
import java.util.TimeZone;

public final class TaskRules {
    public static final String CAMPUS_TIME_ZONE_ID = "Asia/Shanghai";

    private TaskRules() {
    }

    public static boolean isValidTitle(String title) {
        return title != null && !title.trim().isEmpty();
    }

    public static boolean isOverdue(TaskItem task, long nowMillis) {
        return task != null && !task.completed && task.dueAtMillis > 0 && nowMillis > task.dueAtMillis;
    }

    public static int normalizeRepeatMinutes(int minutes) {
        return Math.max(0, minutes);
    }

    public static long nextRepeatReminderAt(long currentReminderAt, int repeatMinutes) {
        int cleanMinutes = normalizeRepeatMinutes(repeatMinutes);
        if (cleanMinutes <= 0) {
            return 0L;
        }
        return currentReminderAt + cleanMinutes * 60_000L;
    }

    public static boolean canScheduleReminder(long reminderAtMillis, long nowMillis) {
        return reminderAtMillis > nowMillis;
    }

    public static SimpleDateFormat campusDateFormat() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA);
        format.setTimeZone(TimeZone.getTimeZone(CAMPUS_TIME_ZONE_ID));
        return format;
    }

    public static Comparator<TaskItem> dashboardComparator() {
        return (left, right) -> {
            int completedCompare = Boolean.compare(left.completed, right.completed);
            if (completedCompare != 0) {
                return completedCompare;
            }
            int priorityCompare = Integer.compare(right.priority, left.priority);
            if (priorityCompare != 0) {
                return priorityCompare;
            }
            return Long.compare(left.dueAtMillis, right.dueAtMillis);
        };
    }
}
