package com.example.campustask.model;

public class Dish {
    public final long id;
    public final long merchantId;
    public final String merchantName;
    public final String name;
    public final int price;

    public Dish(long id, long merchantId, String merchantName, String name, int price) {
        this.id = id;
        this.merchantId = merchantId;
        this.merchantName = merchantName;
        this.name = name;
        this.price = price;
    }
}
