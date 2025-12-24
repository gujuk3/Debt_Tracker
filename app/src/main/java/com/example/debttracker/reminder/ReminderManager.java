package com.example.debttracker.reminder;

import android.content.Context;

import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

public class ReminderManager {
    private final Context context;
    private final WorkManager workManager;

    public ReminderManager(Context context) {
        this.context = context;
        this.workManager = WorkManager.getInstance(context);
    }

    public void scheduleReminder(int debtId, long reminderTimeMillis) {
        long currentTime = System.currentTimeMillis();
        long delay = reminderTimeMillis - currentTime;

        if (delay <= 0) {
            return;
        }

        Data inputData = new Data.Builder()
                .putInt("debt_id", debtId)
                .build();

        OneTimeWorkRequest reminderWork = new OneTimeWorkRequest.Builder(ReminderWorker.class)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(inputData)
                .addTag("reminder_" + debtId)
                .build();

        workManager.enqueue(reminderWork);
    }

    public void cancelReminder(int debtId) {
        workManager.cancelAllWorkByTag("reminder_" + debtId);
    }

    public void rescheduleReminder(int debtId, long newReminderTimeMillis) {
        cancelReminder(debtId);
        scheduleReminder(debtId, newReminderTimeMillis);
    }
}
