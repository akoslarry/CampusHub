package com.example.campustask.model;

public final class CommunityRules {
    private CommunityRules() {
    }

    public static boolean canAddService(String name, String category, String description) {
        return hasText(name) && hasText(category) && hasText(description);
    }

    public static boolean canPublishPost(String title, String content) {
        return hasText(title) && hasText(content);
    }

    public static boolean canPublishItem(String name, int price, String description, String contact) {
        return hasText(name) && price > 0 && hasText(description) && hasText(contact);
    }

    public static boolean canPlaceBid(int bidPrice, int currentPrice) {
        return bidPrice > 0 && bidPrice > currentPrice;
    }

    public static boolean isOwner(String owner, String currentUser) {
        return hasText(owner) && hasText(currentUser) && owner.trim().equals(currentUser.trim());
    }

    public static boolean canBidOnItem(String seller, String bidder, boolean sold, int bidPrice, int currentPrice) {
        return !sold && !isOwner(seller, bidder) && canPlaceBid(bidPrice, currentPrice);
    }

    public static boolean canAffordBid(int walletBalance, int bidPrice) {
        return bidPrice > 0 && walletBalance >= bidPrice;
    }

    public static boolean canRechargeWallet(int amount) {
        return amount > 0;
    }

    public static boolean canWithdrawWallet(int walletBalance, int amount) {
        return amount > 0 && walletBalance >= amount;
    }

    public static boolean canCompleteMarketplacePayment(int walletBalance, int finalPrice) {
        return finalPrice > 0 && walletBalance >= finalPrice;
    }

    public static boolean canSettleSale(String seller, String currentUser, boolean sold, int bidCount) {
        return !sold && bidCount > 0 && isOwner(seller, currentUser);
    }

    public static int platformFee(int price) {
        if (price <= 0) {
            return 0;
        }
        return (int) Math.ceil(price * 0.02d);
    }

    public static int sellerIncome(int price) {
        return Math.max(0, price - platformFee(price));
    }

    public static boolean isCustomServiceId(String id) {
        if (id == null || !id.startsWith("custom_")) {
            return false;
        }
        String number = id.substring("custom_".length());
        if (number.isEmpty()) {
            return false;
        }
        for (int i = 0; i < number.length(); i++) {
            if (!Character.isDigit(number.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
