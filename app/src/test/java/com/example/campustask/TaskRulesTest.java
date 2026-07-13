package com.example.campustask;

import com.example.campustask.model.TaskItem;
import com.example.campustask.model.TaskRules;

import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TaskRulesTest {
    @Test
    public void titleIsInvalidWhenBlankAfterTrimming() {
        assertFalse(TaskRules.isValidTitle("   "));
        assertTrue(TaskRules.isValidTitle("实验报告"));
    }

    @Test
    public void unfinishedTaskIsOverdueAfterDueTime() {
        TaskItem task = new TaskItem(1, 1, "交实验", "", 1000L, 900L, 2, false);

        assertTrue(TaskRules.isOverdue(task, 1001L));
        assertFalse(TaskRules.isOverdue(task, 999L));
        assertFalse(TaskRules.isOverdue(task.markCompleted(true), 1001L));
    }

    @Test
    public void dashboardSortsUnfinishedFirstThenPriorityThenDueTime() {
        List<TaskItem> tasks = new ArrayList<>();
        tasks.add(new TaskItem(1, 1, "低优先级", "", 3000L, 0L, 1, false));
        tasks.add(new TaskItem(2, 1, "已完成", "", 1000L, 0L, 3, true));
        tasks.add(new TaskItem(3, 1, "高优先级", "", 2000L, 0L, 3, false));

        Collections.sort(tasks, TaskRules.dashboardComparator());

        assertEquals("高优先级", tasks.get(0).title);
        assertEquals("低优先级", tasks.get(1).title);
        assertEquals("已完成", tasks.get(2).title);
    }

    @Test
    public void repeatIntervalCannotBeNegative() {
        assertEquals(0, TaskRules.normalizeRepeatMinutes(-5));
        assertEquals(0, TaskRules.normalizeRepeatMinutes(0));
        assertEquals(15, TaskRules.normalizeRepeatMinutes(15));
    }

    @Test
    public void repeatReminderUsesMinuteInterval() {
        assertEquals(0L, TaskRules.nextRepeatReminderAt(1000L, 0));
        assertEquals(0L, TaskRules.nextRepeatReminderAt(1000L, -2));
        assertEquals(301000L, TaskRules.nextRepeatReminderAt(1000L, 5));
    }

    @Test
    public void reminderCanOnlyBeScheduledInFuture() {
        assertFalse(TaskRules.canScheduleReminder(999L, 1000L));
        assertFalse(TaskRules.canScheduleReminder(1000L, 1000L));
        assertTrue(TaskRules.canScheduleReminder(1001L, 1000L));
    }

    @Test
    public void campusDateFormatUsesBeijingTime() throws Exception {
        TimeZone original = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("GMT"));

            SimpleDateFormat format = TaskRules.campusDateFormat();

            assertEquals("Asia/Shanghai", format.getTimeZone().getID());
            assertEquals(1782965280000L, format.parse("2026-07-02 12:08").getTime());
        } finally {
            TimeZone.setDefault(original);
        }
    }
}
