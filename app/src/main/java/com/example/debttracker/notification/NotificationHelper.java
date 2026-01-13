package com.example.debttracker.notification;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.example.debttracker.DebtDetailActivity;
import com.example.debttracker.R;
import com.example.debttracker.database.Debt;
import com.example.debttracker.database.DatabaseHelper;

import java.text.NumberFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class NotificationHelper {
    private static final String CHANNEL_ID = "debt_reminder_channel";
    private static final String CHANNEL_NAME = "Borç Hatırlatıcı";
    private static final String CHANNEL_DESC = "Borç ödeme hatırlatmaları";

    private final Context context;
    private final AlarmManager alarmManager;

    public NotificationHelper(Context context) {
        this.context = context;
        this.alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(CHANNEL_DESC);

            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    // Borç için alarm kur (vade gününden 1 gün önce ve vade günü)
    public void scheduleNotification(Debt debt) {
        if (debt.getDueDate() <= 0 || !debt.isNotificationEnabled()) {
            return;
        }

        // 1 gün önce hatırlatma
        Calendar oneDayBefore = Calendar.getInstance();
        oneDayBefore.setTimeInMillis(debt.getDueDate());
        oneDayBefore.add(Calendar.DAY_OF_MONTH, -1);
        oneDayBefore.set(Calendar.HOUR_OF_DAY, 10); // Sabah 10:00
        oneDayBefore.set(Calendar.MINUTE, 0);
        oneDayBefore.set(Calendar.SECOND, 0);

        if (oneDayBefore.getTimeInMillis() > System.currentTimeMillis()) {
            setAlarm(debt.getId(), oneDayBefore.getTimeInMillis(), true);
        }

        // Vade günü hatırlatma
        Calendar dueDay = Calendar.getInstance();
        dueDay.setTimeInMillis(debt.getDueDate());
        dueDay.set(Calendar.HOUR_OF_DAY, 9); // Sabah 09:00
        dueDay.set(Calendar.MINUTE, 0);
        dueDay.set(Calendar.SECOND, 0);

        if (dueDay.getTimeInMillis() > System.currentTimeMillis()) {
            setAlarm(debt.getId() + 10000, dueDay.getTimeInMillis(), false);
        }
    }

    private void setAlarm(int requestCode, long triggerTime, boolean isOneDayBefore) {
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra("REQUEST_CODE", requestCode);
        intent.putExtra("IS_ONE_DAY_BEFORE", isOneDayBefore);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
            }
        } catch (SecurityException e) {
            // Exact alarm izni yoksa normal alarm kur
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
        }
    }

    // Bildirimi göster
    public void showNotification(int debtId, boolean isOneDayBefore) {
        DatabaseHelper dbHelper = new DatabaseHelper(context);
        Debt debt = dbHelper.getDebtById(debtId > 10000 ? debtId - 10000 : debtId);

        if (debt == null || debt.isPaid()) {
            return;
        }

        // Bildirim izni kontrolü (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("tr", "TR"));
        String amountStr = currencyFormat.format(debt.getAmount());

        String title = isOneDayBefore ? "Yarın Ödeme Günü!" : "Bugün Ödeme Günü!";
        String message = debt.getType().equals("PAYABLE")
                ? debt.getPersonName() + "'e " + amountStr + " ödemeniz var."
                : debt.getPersonName() + "'den " + amountStr + " alacağınız var.";

        // Tıklandığında açılacak intent
        Intent intent = new Intent(context, DebtDetailActivity.class);
        intent.putExtra("DEBT_ID", debt.getId());
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                debt.getId(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(debtId, builder.build());
    }

    // Alarm iptal et
    public void cancelNotification(int debtId) {
        Intent intent = new Intent(context, AlarmReceiver.class);

        // 1 gün önce alarmını iptal et
        PendingIntent pendingIntent1 = PendingIntent.getBroadcast(
                context,
                debtId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        alarmManager.cancel(pendingIntent1);

        // Vade günü alarmını iptal et
        PendingIntent pendingIntent2 = PendingIntent.getBroadcast(
                context,
                debtId + 10000,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        alarmManager.cancel(pendingIntent2);
    }

    // Tüm bildirimleri yeniden kur (cihaz yeniden başlatıldığında)
    public void rescheduleAllNotifications() {
        DatabaseHelper dbHelper = new DatabaseHelper(context);
        List<Debt> debts = dbHelper.getDebtsWithNotification();

        for (Debt debt : debts) {
            scheduleNotification(debt);
        }
    }
}
