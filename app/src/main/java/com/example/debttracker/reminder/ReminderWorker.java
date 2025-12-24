package com.example.debttracker.reminder;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.debttracker.DebtDetailActivity;
import com.example.debttracker.R;
import com.example.debttracker.database.DatabaseHelper;
import com.example.debttracker.database.Debt;

import java.text.NumberFormat;
import java.util.Locale;

public class ReminderWorker extends Worker {
    private static final String CHANNEL_ID = "debt_reminder_channel";
    private static final String CHANNEL_NAME = "Borç Hatırlatmaları";
    private static final String CHANNEL_DESCRIPTION = "Borç ödeme hatırlatma bildirimleri";

    public ReminderWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        int debtId = getInputData().getInt("debt_id", -1);
        if (debtId == -1) {
            return Result.failure();
        }

        DatabaseHelper dbHelper = new DatabaseHelper(getApplicationContext());
        Debt debt = dbHelper.getDebtById(debtId);

        if (debt != null && !debt.isPaid()) {
            createNotificationChannel();
            showNotification(debt);
            return Result.success();
        }

        return Result.failure();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(CHANNEL_DESCRIPTION);

            NotificationManager notificationManager = getApplicationContext()
                    .getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private void showNotification(Debt debt) {
        Intent intent = new Intent(getApplicationContext(), DebtDetailActivity.class);
        intent.putExtra("debt_id", debt.getId());
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                getApplicationContext(),
                debt.getId(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("tr", "TR"));
        String formattedAmount = currencyFormat.format(debt.getAmount());

        String title;
        String content;
        if ("RECEIVABLE".equals(debt.getType())) {
            title = "Alacak Hatırlatması";
            content = debt.getPersonName() + " - " + formattedAmount + " tahsil edilecek";
        } else {
            title = "Borç Hatırlatması";
            content = debt.getPersonName() + " - " + formattedAmount + " ödenecek";
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(content)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(content))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManager notificationManager = (NotificationManager)
                getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager != null) {
            notificationManager.notify(debt.getId(), builder.build());
        }
    }
}
