package com.example.campustask;

import com.example.campustask.model.Dish;
import com.example.campustask.model.FoodOrderRules;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FoodOrderRulesTest {
    @Test
    public void sumsCartDishPrices() {
        int total = FoodOrderRules.total(Arrays.asList(
                new Dish(1, 1, "一食堂轻食", "鸡胸肉沙拉", 18),
                new Dish(2, 1, "一食堂轻食", "牛肉饭", 22)
        ));

        assertEquals(40, total);
    }

    @Test
    public void emptyCartCannotSubmitOrder() {
        assertFalse(FoodOrderRules.canSubmit(Collections.emptyList()));
    }

    @Test
    public void nonEmptyCartCanSubmitOrder() {
        assertTrue(FoodOrderRules.canSubmit(Collections.singletonList(
                new Dish(1, 1, "咖啡角", "拿铁", 15)
        )));
    }

    @Test
    public void removesDishFromCartByIndex() {
        java.util.List<Dish> cart = new java.util.ArrayList<>(Arrays.asList(
                new Dish(1, 1, "一食堂轻食", "鸡胸肉沙拉", 18),
                new Dish(2, 1, "一食堂轻食", "牛肉饭", 22)
        ));

        boolean removed = FoodOrderRules.removeAt(cart, 0);

        assertTrue(removed);
        assertEquals(1, cart.size());
        assertEquals("牛肉饭", cart.get(0).name);
    }

    @Test
    public void merchantOrderStatusMovesForward() {
        assertEquals(FoodOrderRules.STATUS_MAKING, FoodOrderRules.nextStatus(FoodOrderRules.STATUS_WAITING));
        assertEquals(FoodOrderRules.STATUS_READY, FoodOrderRules.nextStatus(FoodOrderRules.STATUS_MAKING));
        assertEquals(FoodOrderRules.STATUS_COMPLETED, FoodOrderRules.nextStatus(FoodOrderRules.STATUS_READY));
        assertEquals(FoodOrderRules.STATUS_COMPLETED, FoodOrderRules.nextStatus(FoodOrderRules.STATUS_COMPLETED));
    }
}
