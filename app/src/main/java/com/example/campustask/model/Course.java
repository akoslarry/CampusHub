package com.example.campustask.model;

public class Course {
    public final long id;
    public final String name;
    public final String teacher;
    public final String location;
    public final int color;
    public final int weekday;
    public final int startSection;
    public final int endSection;
    public final int startWeek;
    public final int endWeek;

    public Course(long id, String name, String teacher, String location, int color) {
        this(id, name, teacher, location, color, 1, 1, 2, 1, 16);
    }

    public Course(
            long id,
            String name,
            String teacher,
            String location,
            int color,
            int weekday,
            int startSection,
            int endSection,
            int startWeek,
            int endWeek
    ) {
        this.id = id;
        this.name = name;
        this.teacher = teacher;
        this.location = location;
        this.color = color;
        this.weekday = weekday;
        this.startSection = startSection;
        this.endSection = endSection;
        this.startWeek = startWeek;
        this.endWeek = endWeek;
    }
}
