package com.example.campustask.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 教务系统 xlsx 课表解析器。
 * 将 XlsxScheduleParser 解析出的行列矩阵转换为 Course 对象列表。
 * 教务系统课表格式：
 * - 第3行为表头：节次/星期, 星期一, ..., 星期日
 * - 第4行起为课程数据，第1列为节次名（如"第1节"），第2-8列为对应星期的课程信息
 * - 单元格内课程格式（两行一组）：
 *   课程名[课程代码]\n周次范围 教师名 第X节-第Y节 教室
 * - 同一格可包含多门课程
 * - 周次格式：8-11周、1-4周、12周、9-10周,12周
 */
public final class ScheduleExcelParser {

    private static final int[] COLORS = {
            0xFF3B82F6, 0xFF10B981, 0xFFF59E0B, 0xFFEC4899,
            0xFF8B5CF6, 0xFF14B8A6, 0xFFEF4444, 0xFF6366F1,
            0xFFF97316, 0xFF06B6D4
    };

    private static final Pattern WEEK_RANGE_PATTERN = Pattern.compile("(\\d+)\\s*-\\s*(\\d+)\\s*周");
    private static final Pattern WEEK_SINGLE_PATTERN = Pattern.compile("(\\d+)\\s*周");
    private static final Pattern SECTION_PATTERN = Pattern.compile("第\\s*(\\d+)\\s*节\\s*-\\s*第\\s*(\\d+)\\s*节");

    private ScheduleExcelParser() {
    }

    public static List<Course> parse(List<List<String>> rows, int colorOffset) {
        if (rows == null || rows.isEmpty()) {
            throw new IllegalArgumentException("课表数据为空");
        }

        int headerRow = findHeaderRow(rows);
        if (headerRow < 0) {
            throw new IllegalArgumentException("未找到课表表头行（需包含\"节次\"和\"星期\"）");
        }

        List<Course> courses = new ArrayList<>();
        Set<String> deduplication = new HashSet<>();
        int colorIndex = colorOffset;

        for (int r = headerRow + 1; r < rows.size(); r++) {
            List<String> row = rows.get(r);
            if (row.isEmpty() || row.get(0).isEmpty()) {
                continue;
            }
            String firstCol = row.get(0).trim();
            if (!firstCol.contains("第") || !firstCol.contains("节")) {
                continue;
            }
            for (int col = 1; col < row.size() && col <= 7; col++) {
                String cellContent = row.get(col);
                if (cellContent == null || cellContent.trim().isEmpty()) {
                    continue;
                }
                int weekday = col;
                List<Course> cellCourses = parseCell(cellContent, weekday, colorIndex);
                for (Course course : cellCourses) {
                    String key = course.name + "_" + course.weekday + "_"
                            + course.startSection + "_" + course.startWeek + "_" + course.endWeek;
                    if (!deduplication.contains(key)) {
                        deduplication.add(key);
                        courses.add(course);
                        colorIndex++;
                    }
                }
            }
        }

        if (courses.isEmpty()) {
            throw new IllegalArgumentException("未在课表中找到任何课程信息");
        }
        return courses;
    }

    private static int findHeaderRow(List<List<String>> rows) {
        for (int i = 0; i < Math.min(rows.size(), 10); i++) {
            List<String> row = rows.get(i);
            if (!row.isEmpty() && row.get(0).contains("节次") && row.size() > 1) {
                for (int j = 1; j < row.size(); j++) {
                    if (row.get(j).contains("星期")) {
                        return i;
                    }
                }
            }
        }
        return -1;
    }

    private static List<Course> parseCell(String cellContent, int weekday, int colorIndex) {
        List<Course> courses = new ArrayList<>();
        String[] lines = cellContent.split("\n");
        int i = 0;
        while (i < lines.length) {
            String line = lines[i].trim();
            if (line.isEmpty()) {
                i++;
                continue;
            }
            String courseName = extractCourseName(line);
            if (courseName != null && i + 1 < lines.length) {
                String detailLine = lines[i + 1].trim();
                Course course = parseCourseDetail(courseName, detailLine, weekday, colorIndex + courses.size());
                if (course != null) {
                    courses.add(course);
                }
                i += 2;
            } else {
                i++;
            }
        }
        return courses;
    }

    private static String extractCourseName(String line) {
        if (line == null || line.trim().isEmpty()) {
            return null;
        }
        // 课程名行包含[课程代码]且不含"周"和"第X节"
        if (line.contains("[") && line.contains("]") && !line.contains("周") && !line.contains("第")) {
            int bracketIdx = line.indexOf("[");
            String name = line.substring(0, bracketIdx).trim();
            return name.isEmpty() ? null : name;
        }
        // 不含[课程代码]但也不含周次和节次信息（纯课程名行）
        if (!line.contains("周") && !line.contains("第") && !line.contains("节") && !line.matches(".*\\d.*")) {
            return line.trim();
        }
        return null;
    }

    private static Course parseCourseDetail(String courseName, String detailLine, int weekday, int colorIndex) {
        if (detailLine == null || detailLine.isEmpty()) {
            return null;
        }

        int startWeek = 1, endWeek = 16;
        Matcher weekRangeMatcher = WEEK_RANGE_PATTERN.matcher(detailLine);
        if (weekRangeMatcher.find()) {
            startWeek = Integer.parseInt(weekRangeMatcher.group(1));
            endWeek = Integer.parseInt(weekRangeMatcher.group(2));
        } else {
            Matcher weekSingleMatcher = WEEK_SINGLE_PATTERN.matcher(detailLine);
            if (weekSingleMatcher.find()) {
                startWeek = Integer.parseInt(weekSingleMatcher.group(1));
                endWeek = startWeek;
            }
        }

        int startSection = 1, endSection = 2;
        Matcher sectionMatcher = SECTION_PATTERN.matcher(detailLine);
        if (sectionMatcher.find()) {
            startSection = Integer.parseInt(sectionMatcher.group(1));
            endSection = Integer.parseInt(sectionMatcher.group(2));
        }

        String teacher = extractTeacher(detailLine);
        String location = extractLocation(detailLine);
        int color = COLORS[Math.abs(colorIndex) % COLORS.length];

        return new Course(0, courseName, teacher, location, color, weekday,
                startSection, endSection, startWeek, endWeek);
    }

    /**
     * 从详情行提取教师名。
     * 格式：8-11周 焦立博 第2节-第5节 XXB-602
     * 或：9-10周,12周 陈雷 第2节-第5节 WLA-107
     * 教师名位于所有周次信息之后、第X节之前。
     */
    private static String extractTeacher(String detailLine) {
        // 找到所有周次信息的结束位置（可能有逗号分隔的多段周次）
        int lastWeekEnd = 0;
        Matcher weekRangeMatcher = WEEK_RANGE_PATTERN.matcher(detailLine);
        while (weekRangeMatcher.find()) {
            lastWeekEnd = weekRangeMatcher.end();
        }
        Matcher weekSingleMatcher = WEEK_SINGLE_PATTERN.matcher(detailLine);
        while (weekSingleMatcher.find()) {
            if (weekSingleMatcher.end() > lastWeekEnd) {
                lastWeekEnd = weekSingleMatcher.end();
            }
        }

        // 跳过周次后的逗号和空格
        while (lastWeekEnd < detailLine.length()
                && (detailLine.charAt(lastWeekEnd) == ',' || detailLine.charAt(lastWeekEnd) == '，'
                || Character.isWhitespace(detailLine.charAt(lastWeekEnd)))) {
            lastWeekEnd++;
        }

        // 找到"第X节"之前的位置
        int sectionStart = detailLine.indexOf("第", lastWeekEnd);
        if (sectionStart < 0) {
            sectionStart = detailLine.length();
        }

        String teacher = detailLine.substring(lastWeekEnd, sectionStart).trim();
        teacher = teacher.replaceAll("^[,，\\s]+", "").replaceAll("[,，\\s]+$", "");
        return teacher;
    }

    /**
     * 从详情行提取教室。
     * 格式：8-11周 焦立博 第2节-第5节 XXB-602
     * 教室位于节次信息之后。
     */
    private static String extractLocation(String detailLine) {
        Matcher sectionMatcher = SECTION_PATTERN.matcher(detailLine);
        if (sectionMatcher.find()) {
            String afterSection = detailLine.substring(sectionMatcher.end()).trim();
            // 去除可能的前导空格和标点
            afterSection = afterSection.replaceAll("^[,，\\s]+", "");
            if (!afterSection.isEmpty()) {
                return afterSection;
            }
        }
        return "";
    }
}
