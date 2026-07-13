package com.example.campustask.model;

import java.util.ArrayList;
import java.util.List;

public final class CourseImportParser {
    private static final int[] COLORS = {
            0xFF3B82F6,
            0xFF10B981,
            0xFFF59E0B,
            0xFFEC4899,
            0xFF8B5CF6,
            0xFF14B8A6,
            0xFFEF4444
    };

    private CourseImportParser() {
    }

    public static List<Course> parse(String rawText, int colorOffset) {
        if (rawText == null || rawText.trim().isEmpty()) {
            throw new IllegalArgumentException("请粘贴教务系统课表文本");
        }
        List<Course> courses = new ArrayList<>();
        String[] lines = rawText.replace("\r", "").split("\n");
        for (String line : lines) {
            String normalized = line == null ? "" : line.trim();
            if (normalized.isEmpty() || isHeader(normalized)) {
                continue;
            }
            String[] columns = splitColumns(normalized);
            Course course = parseCourse(columns, courses.size() + colorOffset);
            courses.add(course);
        }
        if (courses.isEmpty()) {
            throw new IllegalArgumentException("没有识别到可导入的课程");
        }
        return courses;
    }

    private static Course parseCourse(String[] columns, int colorIndex) {
        if (columns.length >= 8) {
            String name = require(columns[0], "课程名称");
            String teacher = safe(columns[1]);
            String location = safe(columns[2]);
            int weekday = parseWeekday(columns[3]);
            int startSection = parseNumber(columns[4], "开始节次");
            int endSection = parseNumber(columns[5], "结束节次");
            int startWeek = parseNumber(columns[6], "开始周");
            int endWeek = parseNumber(columns[7], "结束周");
            return buildCourse(name, teacher, location, weekday, startSection, endSection, startWeek, endWeek, colorIndex);
        }
        if (columns.length >= 6) {
            String name = require(columns[0], "课程名称");
            String teacher = safe(columns[1]);
            String location = safe(columns[2]);
            int weekday = parseWeekday(columns[3]);
            int[] sections = parseRange(columns[4], "节次");
            int[] weeks = parseRange(columns[5], "周次");
            return buildCourse(name, teacher, location, weekday, sections[0], sections[1], weeks[0], weeks[1], colorIndex);
        }
        throw new IllegalArgumentException("每行至少需要 6 列：课程、教师、地点、星期、节次、周次");
    }

    private static Course buildCourse(
            String name,
            String teacher,
            String location,
            int weekday,
            int startSection,
            int endSection,
            int startWeek,
            int endWeek,
            int colorIndex
    ) {
        if (weekday < 1 || weekday > 7) {
            throw new IllegalArgumentException("星期必须在 1-7 之间");
        }
        if (startSection < 1 || endSection < startSection || endSection > 16) {
            throw new IllegalArgumentException("节次范围必须在 1-16 之间");
        }
        if (startWeek < 1 || endWeek < startWeek || endWeek > 20) {
            throw new IllegalArgumentException("周次范围必须在 1-20 之间");
        }
        return new Course(0, name, teacher, location, COLORS[Math.abs(colorIndex) % COLORS.length], weekday, startSection, endSection, startWeek, endWeek);
    }

    private static String[] splitColumns(String line) {
        String delimiter = ",";
        if (line.contains("\t")) {
            delimiter = "\t";
        } else if (line.contains("|")) {
            delimiter = "\\|";
        } else if (line.contains("，")) {
            delimiter = "，";
        } else if (line.contains(";")) {
            delimiter = ";";
        } else if (line.contains("；")) {
            delimiter = "；";
        }
        String[] values = line.split(delimiter);
        for (int i = 0; i < values.length; i++) {
            values[i] = values[i].trim();
        }
        return values;
    }

    private static boolean isHeader(String line) {
        return line.contains("课程") && line.contains("星期") && (line.contains("节") || line.contains("周"));
    }

    private static String require(String value, String field) {
        String cleaned = safe(value);
        if (cleaned.isEmpty()) {
            throw new IllegalArgumentException(field + "不能为空");
        }
        return cleaned;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static int parseWeekday(String value) {
        String cleaned = safe(value);
        if (cleaned.contains("一")) {
            return 1;
        } else if (cleaned.contains("二")) {
            return 2;
        } else if (cleaned.contains("三")) {
            return 3;
        } else if (cleaned.contains("四")) {
            return 4;
        } else if (cleaned.contains("五")) {
            return 5;
        } else if (cleaned.contains("六")) {
            return 6;
        } else if (cleaned.contains("日") || cleaned.contains("天")) {
            return 7;
        }
        return parseNumber(cleaned, "星期");
    }

    private static int[] parseRange(String value, String field) {
        String[] numbers = safe(value).replace("到", "-").replace("至", "-").split("-");
        if (numbers.length == 1) {
            int single = parseNumber(numbers[0], field);
            return new int[]{single, single};
        }
        int start = parseNumber(numbers[0], field + "开始");
        int end = parseNumber(numbers[1], field + "结束");
        return new int[]{start, end};
    }

    private static int parseNumber(String value, String field) {
        String digits = safe(value).replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            throw new IllegalArgumentException(field + "必须是数字");
        }
        return Integer.parseInt(digits);
    }
}
