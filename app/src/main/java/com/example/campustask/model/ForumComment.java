package com.example.campustask.model;

public class ForumComment {
    public final long id;
    public final long postId;
    public final String content;
    public final String author;
    public final long createdAt;

    public ForumComment(long id, long postId, String content, String author, long createdAt) {
        this.id = id;
        this.postId = postId;
        this.content = content;
        this.author = author;
        this.createdAt = createdAt;
    }
}
