package com.example.debttracker.calendar;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;

import com.example.debttracker.database.Debt;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.TimeZone;

public class CalendarIntegration {
    private final Context context;

    public CalendarIntegration(Context context) {
        this.context = context;
    }

    public long addDebtToCalendar(Debt debt) {
        if (debt.getDueDate() == null) {
            return -1;
        }

        long calendarId = getDefaultCalendarId();
        if (calendarId == -1) {
            return -1;
        }

        ContentResolver contentResolver = context.getContentResolver();
        ContentValues values = new ContentValues();

        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("tr", "TR"));
        String formattedAmount = currencyFormat.format(debt.getAmount());

        String title;
        String description;
        if ("RECEIVABLE".equals(debt.getType())) {
            title = "Alacak: " + debt.getPersonName() + " - " + formattedAmount;
            description = "Alacak tahsili\n" +
                    "Kişi: " + debt.getPersonName() + "\n" +
                    "Tutar: " + formattedAmount;
        } else {
            title = "Borç: " + debt.getPersonName() + " - " + formattedAmount;
            description = "Borç ödemesi\n" +
                    "Kişi: " + debt.getPersonName() + "\n" +
                    "Tutar: " + formattedAmount;
        }

        if (debt.getDescription() != null && !debt.getDescription().isEmpty()) {
            description += "\n\nNotlar: " + debt.getDescription();
        }

        values.put(CalendarContract.Events.DTSTART, debt.getDueDate());
        values.put(CalendarContract.Events.DTEND, debt.getDueDate() + (60 * 60 * 1000));
        values.put(CalendarContract.Events.TITLE, title);
        values.put(CalendarContract.Events.DESCRIPTION, description);
        values.put(CalendarContract.Events.CALENDAR_ID, calendarId);
        values.put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().getID());
        values.put(CalendarContract.Events.HAS_ALARM, 1);

        Uri uri = contentResolver.insert(CalendarContract.Events.CONTENT_URI, values);
        if (uri != null) {
            long eventId = Long.parseLong(uri.getLastPathSegment());

            addReminderToEvent(eventId, 24 * 60);

            return eventId;
        }

        return -1;
    }

    private void addReminderToEvent(long eventId, int minutesBefore) {
        ContentResolver contentResolver = context.getContentResolver();
        ContentValues values = new ContentValues();
        values.put(CalendarContract.Reminders.EVENT_ID, eventId);
        values.put(CalendarContract.Reminders.MINUTES, minutesBefore);
        values.put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT);

        contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, values);
    }

    public boolean deleteEventFromCalendar(String eventId) {
        if (eventId == null || eventId.isEmpty()) {
            return false;
        }

        try {
            ContentResolver contentResolver = context.getContentResolver();
            Uri deleteUri = ContentResolver.withAppendedId(
                    CalendarContract.Events.CONTENT_URI,
                    Long.parseLong(eventId)
            );
            int rows = contentResolver.delete(deleteUri, null, null);
            return rows > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private long getDefaultCalendarId() {
        String[] projection = new String[]{
                CalendarContract.Calendars._ID,
                CalendarContract.Calendars.ACCOUNT_NAME,
                CalendarContract.Calendars.IS_PRIMARY
        };

        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                CalendarContract.Calendars.VISIBLE + " = 1",
                null,
                CalendarContract.Calendars.IS_PRIMARY + " DESC"
        );

        if (cursor != null && cursor.moveToFirst()) {
            long calendarId = cursor.getLong(0);
            cursor.close();
            return calendarId;
        }

        if (cursor != null) {
            cursor.close();
        }

        return -1;
    }

    public boolean hasCalendarPermission() {
        try {
            ContentResolver contentResolver = context.getContentResolver();
            Cursor cursor = contentResolver.query(
                    CalendarContract.Calendars.CONTENT_URI,
                    new String[]{CalendarContract.Calendars._ID},
                    null,
                    null,
                    null
            );
            if (cursor != null) {
                boolean hasPermission = cursor.getCount() > 0;
                cursor.close();
                return hasPermission;
            }
        } catch (SecurityException e) {
            return false;
        }
        return false;
    }
}
