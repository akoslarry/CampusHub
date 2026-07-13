package com.example.campustask.model;

public class MarketBid {
    public final long id;
    public final long itemId;
    public final int price;
    public final String bidder;
    public final long createdAt;

    public MarketBid(long id, long itemId, int price, String bidder, long createdAt) {
        this.id = id;
        this.itemId = itemId;
        this.price = price;
        this.bidder = bidder;
        this.createdAt = createdAt;
    }
}
