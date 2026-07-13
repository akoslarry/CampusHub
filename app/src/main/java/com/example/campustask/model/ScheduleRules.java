package com.example.campustask.model;

public final class ScheduleRules {
    private static final String[] WEEKDAY_LABELS = {"", "周一", "周二", "周三", "周四", "周五", "周六", "周日"};

    private ScheduleRules() {
    }

    public static boolean isVisibleInWeek(Course course, int week) {
        return course != null && week >= course.startWeek && week <= course.endWeek;
    }

    public static int sectionSpan(Course course) {
        if (course == null) {
            return 0;
        }
        return Math.max(1, course.endSection - course.startSection + 1);
    }

    public static String weekdayLabel(int weekday) {
        if (weekday < 1 || weekday >= WEEKDAY_LABELS.length) {
            return "";
        }
        return WEEKDAY_LABELS[weekday];
    }
}
