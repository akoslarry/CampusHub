package com.example.campustask.model;

public class TaskItem {
    public final long id;
    public final long courseId;
    public final String title;
    public final String description;
    public final long dueAtMillis;
    public final long remindAtMillis;
    public final int repeatMinutes;
    public final int priority;
    public final boolean completed;

    public TaskItem(
            long id,
            long courseId,
            String title,
            String description,
            long dueAtMillis,
            long remindAtMillis,
            int priority,
            boolean completed
    ) {
        this(id, courseId, title, description, dueAtMillis, remindAtMillis, 0, priority, completed);
    }

    public TaskItem(
            long id,
            long courseId,
            String title,
            String description,
            long dueAtMillis,
            long remindAtMillis,
            int repeatMinutes,
            int priority,
            boolean completed
    ) {
        this.id = id;
        this.courseId = courseId;
        this.title = title;
        this.description = description;
        this.dueAtMillis = dueAtMillis;
        this.remindAtMillis = remindAtMillis;
        this.repeatMinutes = TaskRules.normalizeRepeatMinutes(repeatMinutes);
        this.priority = priority;
        this.completed = completed;
    }

    public TaskItem markCompleted(boolean value) {
        return new TaskItem(id, courseId, title, description, dueAtMillis, remindAtMillis, repeatMinutes, priority, value);
    }
}
