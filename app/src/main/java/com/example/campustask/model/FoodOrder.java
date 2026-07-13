package com.example.campustask.model;

public class FoodOrder {
    public final long id;
    public final String merchantName;
    public final String itemsSummary;
    public final int totalPrice;
    public final String status;
    public final long createdAtMillis;

    public FoodOrder(long id, String merchantName, String itemsSummary, int totalPrice, String status, long createdAtMillis) {
        this.id = id;
        this.merchantName = merchantName;
        this.itemsSummary = itemsSummary;
        this.totalPrice = totalPrice;
        this.status = status;
        this.createdAtMillis = createdAtMillis;
    }
}
