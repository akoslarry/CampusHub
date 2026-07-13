package com.example.campustask.reminder;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import com.example.campustask.data.CampusTaskRepository;
import com.example.campustask.model.Course;
import com.example.campustask.model.SectionSchedule;

import java.util.ArrayList;
import java.util.List;

/**
 * 上课提醒调度器。
 * 扫描所有课程，计算本周各课程的上课时间，提前15分钟调度通知。
 * 连续课程（前一节课的endSection紧接后一节课的startSection-1）只通知第一节课。
 */
public class CourseReminderScheduler {
    private static final String PREFS_NAME = "campus_course_reminders";
    private static final String KEY_SEMESTER_START = "semester_start";
    private static final String KEY_CURRENT_WEEK = "current_week";
    private static final String KEY_LAST_SCHEDULED = "last_scheduled";
    private static final int ADVANCE_MINUTES = 15;
    private static final int REQUEST_CODE_BASE = 50000;

    private final Context context;
    private final AlarmManager alarmManager;

    public CourseReminderScheduler(Context context) {
        this.context = context.getApplicationContext();
        alarmManager = (AlarmManager) this.context.getSystemService(Context.ALARM_SERVICE);
    }

    public void setSemesterStart(long millis) {
        getPrefs().edit().putLong(KEY_SEMESTER_START, millis).apply();
    }

    /**
     * 设置用户当前查看的周次，作为上课提醒的当前周次。
     */
    public void setCurrentWeek(int week) {
        getPrefs().edit().putInt(KEY_CURRENT_WEEK, week).apply();
    }

    private SharedPreferences getPrefs() {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * 扫描所有课程，调度本周内还未过去的上课提醒。
     * 连续课程只通知第一节课。
     */
    public void scheduleAll(String username) {
        if (username == null || username.isEmpty()) {
            return;
        }
        CampusTaskRepository repository = new CampusTaskRepository(context);
        repository.useAccount(username);
        List<Course> courses = repository.getCourses();
        if (courses.isEmpty()) {
            return;
        }

        int currentWeek = getPrefs().getInt(KEY_CURRENT_WEEK, 1);

        List<Course> sorted = new ArrayList<>(courses);
        sorted.sort((a, b) -> {
            if (a.weekday != b.weekday) return a.weekday - b.weekday;
            return a.startSection - b.startSection;
        });

        List<Course> toSchedule = new ArrayList<>();
        for (Course course : sorted) {
            if (course.startWeek > currentWeek || course.endWeek < currentWeek) {
                continue;
            }
            boolean isContinuation = false;
            for (Course other : sorted) {
                if (other.weekday == course.weekday
                        && other.endSection == course.startSection - 1
                        && other.startWeek <= currentWeek
                        && other.endWeek >= currentWeek
                        && !other.name.equals(course.name)) {
                    isContinuation = true;
                    break;
                }
            }
            if (isContinuation) {
                continue;
            }
            toSchedule.add(course);
        }

        cancelAll(courses);

        for (Course course : toSchedule) {
            long reminderTime = SectionSchedule.nextReminderTime(
                    course.weekday, course.startSection, ADVANCE_MINUTES, currentWeek, currentWeek);
            if (reminderTime > 0) {
                scheduleCourseReminder(course, reminderTime);
            }
        }

        getPrefs().edit().putLong(KEY_LAST_SCHEDULED, System.currentTimeMillis()).apply();
    }

    private void scheduleCourseReminder(Course course, long reminderTime) {
        Intent intent = new Intent(context, CourseReminderReceiver.class);
        int requestCode = REQUEST_CODE_BASE + (int) course.id;
        intent.putExtra(CourseReminderReceiver.EXTRA_COURSE_NAME, course.name);
        intent.putExtra(CourseReminderReceiver.EXTRA_COURSE_LOCATION, course.location);
        intent.putExtra(CourseReminderReceiver.EXTRA_COURSE_WEEKDAY, course.weekday);
        intent.putExtra(CourseReminderReceiver.EXTRA_COURSE_SECTION, course.startSection);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, flags);

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent);
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent);
            }
        } catch (SecurityException e) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent);
        }
    }

    public void cancelAll(List<Course> courses) {
        if (courses == null) return;
        for (Course course : courses) {
            cancelCourse(course.id);
        }
    }

    private void cancelCourse(long courseId) {
        Intent intent = new Intent(context, CourseReminderReceiver.class);
        int flags = PendingIntent.FLAG_NO_CREATE;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context,
                REQUEST_CODE_BASE + (int) courseId, intent, flags);
        if (pendingIntent != null && alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }
    }
}
