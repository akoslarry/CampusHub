package com.example.campustask.model;

public class WalletTransaction {
    public final long id;
    public final int amount;
    public final String description;
    public final long createdAt;

    public WalletTransaction(long id, int amount, String description, long createdAt) {
        this.id = id;
        this.amount = amount;
        this.description = description;
        this.createdAt = createdAt;
    }
}
