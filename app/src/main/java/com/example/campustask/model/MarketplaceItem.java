package com.example.campustask.model;

public class MarketplaceItem {
    public final long id;
    public final String name;
    public final int price;
    public final String description;
    public final String contact;
    public final String seller;
    public final String imageUri;
    public final long createdAt;
    public final int currentPrice;
    public final int bidCount;
    public final boolean sold;
    public final int soldPrice;
    public final String buyer;
    public final long soldAt;
    public final int platformFee;
    public final int sellerIncome;

    public MarketplaceItem(long id, String name, int price, String description, String contact, String seller, String imageUri, long createdAt) {
        this(id, name, price, description, contact, seller, imageUri, createdAt, price, 0);
    }

    public MarketplaceItem(long id, String name, int price, String description, String contact, String seller, String imageUri, long createdAt, int currentPrice, int bidCount) {
        this(id, name, price, description, contact, seller, imageUri, createdAt, currentPrice, bidCount, false, 0, "", 0, 0, 0);
    }

    public MarketplaceItem(long id, String name, int price, String description, String contact, String seller, String imageUri, long createdAt,
                           int currentPrice, int bidCount, boolean sold, int soldPrice, String buyer, long soldAt,
                           int platformFee, int sellerIncome) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.description = description;
        this.contact = contact;
        this.seller = seller;
        this.imageUri = imageUri == null ? "" : imageUri;
        this.createdAt = createdAt;
        this.currentPrice = currentPrice;
        this.bidCount = bidCount;
        this.sold = sold;
        this.soldPrice = soldPrice;
        this.buyer = buyer;
        this.soldAt = soldAt;
        this.platformFee = platformFee;
        this.sellerIncome = sellerIncome;
    }
}
