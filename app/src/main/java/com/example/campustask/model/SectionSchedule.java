package com.example.campustask.model;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * 北信科课程节次时间映射。
 * 每个节次对应一个开始时间和结束时间（24小时制）。
 * 数据来源：北信科教务系统标准课表时间。
 */
public final class SectionSchedule {
    private SectionSchedule() {
    }

    /**
     * 第N节的开始时间（小时,分钟），索引从1开始。
     * 返回 int[]{hour, minute}
     */
    public static int[] startTime(int section) {
        switch (section) {
            case 1: return new int[]{8, 0};
            case 2: return new int[]{8, 55};
            case 3: return new int[]{9, 50};
            case 4: return new int[]{10, 45};
            case 5: return new int[]{11, 40};
            case 6: return new int[]{13, 30};
            case 7: return new int[]{14, 25};
            case 8: return new int[]{15, 20};
            case 9: return new int[]{16, 15};
            case 10: return new int[]{18, 0};
            case 11: return new int[]{18, 55};
            case 12: return new int[]{19, 50};
            case 13: return new int[]{20, 45};
            case 14: return new int[]{21, 40};
            default: return new int[]{8, 0};
        }
    }

    /**
     * 第N节的结束时间（小时,分钟），索引从1开始。
     */
    public static int[] endTime(int section) {
        switch (section) {
            case 1: return new int[]{8, 45};
            case 2: return new int[]{9, 40};
            case 3: return new int[]{10, 35};
            case 4: return new int[]{11, 30};
            case 5: return new int[]{12, 15};
            case 6: return new int[]{14, 15};
            case 7: return new int[]{15, 10};
            case 8: return new int[]{16, 5};
            case 9: return new int[]{17, 0};
            case 10: return new int[]{18, 45};
            case 11: return new int[]{19, 40};
            case 12: return new int[]{20, 35};
            case 13: return new int[]{21, 30};
            case 14: return new int[]{22, 15};
            default: return new int[]{8, 45};
        }
    }

    /**
     * 获取当前是第几周（开学日为参照，简化处理：以1月1日后的第N周计算）。
     * 实际应用中应从教务系统获取学期开始日期。
     * 这里使用SharedPreferences中存储的开学日期。
     */
    public static int currentWeek(long semesterStartMillis) {
        if (semesterStartMillis <= 0) {
            return 1;
        }
        long now = System.currentTimeMillis();
        long diff = now - semesterStartMillis;
        if (diff < 0) {
            return 1;
        }
        return (int) (diff / (7 * 24 * 60 * 60 * 1000L)) + 1;
    }

    /**
     * 获取今天是星期几（1=周一, 7=周日）。
     */
    public static int todayWeekday() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai"));
        int day = cal.get(Calendar.DAY_OF_WEEK);
        // Calendar: 1=周日, 2=周一, ..., 7=周六
        // 转为: 1=周一, ..., 7=周日
        return day == Calendar.SUNDAY ? 7 : day - 1;
    }

    /**
     * 计算指定 weekday、section、提前分钟数对应的下次提醒时间戳。
     * 只调度本周内还未过去的提醒。
     * @return 提醒时间戳，如果本周已过则返回0
     */
    public static long nextReminderTime(int weekday, int section, int advanceMinutes, int week, int currentWeek) {
        if (week < currentWeek) {
            return 0L;
        }
        int[] start = startTime(section);
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai"));
        int todayDay = cal.get(Calendar.DAY_OF_WEEK);
        int todayWeekday = todayDay == Calendar.SUNDAY ? 7 : todayDay - 1;
        int daysUntil = weekday - todayWeekday;
        if (daysUntil < 0) {
            return 0L;
        }
        cal.add(Calendar.DAY_OF_MONTH, daysUntil);
        cal.set(Calendar.HOUR_OF_DAY, start[0]);
        cal.set(Calendar.MINUTE, start[1]);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long courseStart = cal.getTimeInMillis();
        long reminderTime = courseStart - advanceMinutes * 60_000L;
        if (reminderTime <= System.currentTimeMillis()) {
            return 0L;
        }
        return reminderTime;
    }
}
