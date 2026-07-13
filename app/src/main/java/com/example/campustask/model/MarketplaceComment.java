package com.example.campustask.model;

public class MarketplaceComment {
    public final long id;
    public final long itemId;
    public final String content;
    public final String author;
    public final long createdAt;

    public MarketplaceComment(long id, long itemId, String content, String author, long createdAt) {
        this.id = id;
        this.itemId = itemId;
        this.content = content;
        this.author = author;
        this.createdAt = createdAt;
    }
}
