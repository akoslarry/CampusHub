package com.example.campustask.model;

public class ForumPost {
    public final long id;
    public final String title;
    public final String content;
    public final String author;
    public final long createdAt;

    public ForumPost(long id, String title, String content, String author, long createdAt) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.author = author;
        this.createdAt = createdAt;
    }
}
