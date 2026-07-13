package com.example.campustask.reminder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.campustask.data.CampusTaskRepository;
import com.example.campustask.model.TaskItem;

public class BootReceiver extends BroadcastReceiver {
    private static final String PREFS_NAME = "campus_hub_auth";
    private static final String PREF_USERNAME = "username";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            return;
        }
        CampusTaskRepository repository = new CampusTaskRepository(context);
        String username = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(PREF_USERNAME, "");
        repository.useAccount(username == null || username.isEmpty() ? "guest" : username);
        ReminderScheduler scheduler = new ReminderScheduler(context);
        for (TaskItem task : repository.getTasks()) {
            scheduler.schedule(task);
        }
    }
}
