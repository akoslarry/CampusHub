package com.example.campustask.model;

import java.util.List;

public final class FoodOrderRules {
    public static final String STATUS_WAITING = "待接单";
    public static final String STATUS_MAKING = "制作中";
    public static final String STATUS_READY = "待取餐";
    public static final String STATUS_COMPLETED = "已完成";
    public static final String STATUS_CANCELED = "已取消";

    private FoodOrderRules() {
    }

    public static int total(List<Dish> dishes) {
        int total = 0;
        if (dishes == null) {
            return total;
        }
        for (Dish dish : dishes) {
            total += dish.price;
        }
        return total;
    }

    public static boolean canSubmit(List<Dish> dishes) {
        return dishes != null && !dishes.isEmpty();
    }

    public static boolean removeAt(List<Dish> dishes, int index) {
        if (dishes == null || index < 0 || index >= dishes.size()) {
            return false;
        }
        dishes.remove(index);
        return true;
    }

    public static String nextStatus(String status) {
        if (STATUS_WAITING.equals(status)) {
            return STATUS_MAKING;
        }
        if (STATUS_MAKING.equals(status)) {
            return STATUS_READY;
        }
        if (STATUS_READY.equals(status)) {
            return STATUS_COMPLETED;
        }
        return status;
    }
}
