package com.example.campustask.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.example.campustask.service.MarketSettlementService;

/**
 * 成员D负责的BroadcastReceiver组件。
 * 接收闲置交易结算核对定时广播，启动MarketSettlementService执行已售商品结算完整性检查。
 */
public class MarketSettlementReceiver extends BroadcastReceiver {
    public static final String ACTION_MARKET_SETTLEMENT = "com.example.campustask.ACTION_MARKET_SETTLEMENT";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent != null ? intent.getAction() : null;
        if (!ACTION_MARKET_SETTLEMENT.equals(action)) {
            return;
        }
        Intent serviceIntent = new Intent(context, MarketSettlementService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
    }
}
