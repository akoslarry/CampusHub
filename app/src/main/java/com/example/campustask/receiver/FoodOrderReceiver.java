package com.example.campustask.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.example.campustask.service.FoodOrderService;

/**
 * 成员B负责的BroadcastReceiver组件。
 * 接收外卖订单状态流转定时广播，启动FoodOrderService执行订单状态推进。
 */
public class FoodOrderReceiver extends BroadcastReceiver {
    public static final String ACTION_FOOD_ORDER_CHECK = "com.example.campustask.ACTION_FOOD_ORDER_CHECK";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent != null ? intent.getAction() : null;
        if (!ACTION_FOOD_ORDER_CHECK.equals(action)) {
            return;
        }
        Intent serviceIntent = new Intent(context, FoodOrderService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
    }
}
