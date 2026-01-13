package com.example.debttracker.notification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        int requestCode = intent.getIntExtra("REQUEST_CODE", -1);
        boolean isOneDayBefore = intent.getBooleanExtra("IS_ONE_DAY_BEFORE", false);

        if (requestCode != -1) {
            NotificationHelper notificationHelper = new NotificationHelper(context);
            notificationHelper.showNotification(requestCode, isOneDayBefore);
        }
    }
}