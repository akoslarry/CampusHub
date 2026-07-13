package com.example.campustask.model;

public class CampusService {
    public final String id;
    public final String name;
    public final String description;
    public final String category;
    public final String iconText;
    public final int color;
    public final boolean pinned;

    public CampusService(
            String id,
            String name,
            String description,
            String category,
            String iconText,
            int color,
            boolean pinned
    ) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category;
        this.iconText = iconText;
        this.color = color;
        this.pinned = pinned;
    }
}
