package com.example.campustask;

import com.example.campustask.model.Course;
import com.example.campustask.model.CourseImportParser;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class CourseImportParserTest {
    @Test
    public void parsesStandardAcademicCsv() {
        String text = "课程名称,教师,地点,星期,开始节次,结束节次,开始周,结束周\n"
                + "移动应用系统,张老师,A301,2,3,4,1,16\n"
                + "大学英语,李老师,B205,周一,1,2,1,18";

        List<Course> courses = CourseImportParser.parse(text, 0);

        assertEquals(2, courses.size());
        assertEquals("移动应用系统", courses.get(0).name);
        assertEquals(2, courses.get(0).weekday);
        assertEquals(3, courses.get(0).startSection);
        assertEquals(4, courses.get(0).endSection);
        assertEquals(1, courses.get(1).weekday);
    }

    @Test
    public void parsesCompactRangeFormat() {
        String text = "课程,教师,地点,星期,节次,周次\n"
                + "数据结构,王老师,实验楼B402,星期三,15-16,1-12";

        List<Course> courses = CourseImportParser.parse(text, 3);

        assertEquals(1, courses.size());
        assertEquals("数据结构", courses.get(0).name);
        assertEquals(3, courses.get(0).weekday);
        assertEquals(15, courses.get(0).startSection);
        assertEquals(16, courses.get(0).endSection);
        assertEquals(12, courses.get(0).endWeek);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsInvalidScheduleRange() {
        CourseImportParser.parse("课程A,教师,地点,8,1,2,1,16", 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsSectionAfterSixteen() {
        CourseImportParser.parse("课程A,教师,地点,2,15,17,1,16", 0);
    }
}
