package com.example.campustask;

import com.example.campustask.model.Course;
import com.example.campustask.model.ScheduleRules;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ScheduleRulesTest {
    @Test
    public void courseIsVisibleOnlyInsideWeekRange() {
        Course course = new Course(1, "移动应用系统", "张老师", "实验楼 A301", -13388315, 2, 3, 4, 1, 16);

        assertTrue(ScheduleRules.isVisibleInWeek(course, 1));
        assertTrue(ScheduleRules.isVisibleInWeek(course, 10));
        assertTrue(ScheduleRules.isVisibleInWeek(course, 16));
        assertFalse(ScheduleRules.isVisibleInWeek(course, 17));
    }

    @Test
    public void sectionSpanIncludesStartAndEndSection() {
        Course course = new Course(1, "移动应用系统", "张老师", "实验楼 A301", -13388315, 2, 3, 4, 1, 16);

        assertEquals(2, ScheduleRules.sectionSpan(course));
    }

    @Test
    public void weekdayLabelsMatchChineseScheduleHeader() {
        assertEquals("周一", ScheduleRules.weekdayLabel(1));
        assertEquals("周日", ScheduleRules.weekdayLabel(7));
        assertEquals("", ScheduleRules.weekdayLabel(0));
    }
}
