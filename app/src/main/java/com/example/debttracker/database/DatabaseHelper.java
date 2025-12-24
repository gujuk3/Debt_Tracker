package com.example.debttracker.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "DebtTracker.db";
    private static final int DATABASE_VERSION = 2;

    private static final String TABLE_DEBTS = "debts";
    private static final String COL_ID = "id";
    private static final String COL_PERSON_NAME = "person_name";
    private static final String COL_AMOUNT = "amount";
    private static final String COL_TYPE = "type";
    private static final String COL_DESCRIPTION = "description";
    private static final String COL_DATE = "date";
    private static final String COL_IS_PAID = "is_paid";
    private static final String COL_DUE_DATE = "due_date";
    private static final String COL_REMINDER_TIME = "reminder_time";
    private static final String COL_CALENDAR_EVENT_ID = "calendar_event_id";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_DEBTS + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_PERSON_NAME + " TEXT, " +
                COL_AMOUNT + " REAL, " +
                COL_TYPE + " TEXT, " +
                COL_DESCRIPTION + " TEXT, " +
                COL_DATE + " INTEGER, " +
                COL_IS_PAID + " INTEGER DEFAULT 0, " +
                COL_DUE_DATE + " INTEGER, " +
                COL_REMINDER_TIME + " INTEGER, " +
                COL_CALENDAR_EVENT_ID + " TEXT)";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            // Add new columns for version 2
            db.execSQL("ALTER TABLE " + TABLE_DEBTS + " ADD COLUMN " + COL_DUE_DATE + " INTEGER");
            db.execSQL("ALTER TABLE " + TABLE_DEBTS + " ADD COLUMN " + COL_REMINDER_TIME + " INTEGER");
            db.execSQL("ALTER TABLE " + TABLE_DEBTS + " ADD COLUMN " + COL_CALENDAR_EVENT_ID + " TEXT");
        }
    }

    public long addDebt(String personName, double amount, String type, String description, long date) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_PERSON_NAME, personName);
        values.put(COL_AMOUNT, amount);
        values.put(COL_TYPE, type);
        values.put(COL_DESCRIPTION, description);
        values.put(COL_DATE, date);
        values.put(COL_IS_PAID, 0);

        long id = db.insert(TABLE_DEBTS, null, values);
        db.close();
        return id;
    }

    public long addDebt(String personName, double amount, String type, String description, long date,
                       Long dueDate, Long reminderTime, String calendarEventId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_PERSON_NAME, personName);
        values.put(COL_AMOUNT, amount);
        values.put(COL_TYPE, type);
        values.put(COL_DESCRIPTION, description);
        values.put(COL_DATE, date);
        values.put(COL_IS_PAID, 0);
        if (dueDate != null) values.put(COL_DUE_DATE, dueDate);
        if (reminderTime != null) values.put(COL_REMINDER_TIME, reminderTime);
        if (calendarEventId != null) values.put(COL_CALENDAR_EVENT_ID, calendarEventId);

        long id = db.insert(TABLE_DEBTS, null, values);
        db.close();
        return id;
    }

    public List<Debt> getDebtsByType(String type) {
        List<Debt> debtList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_DEBTS, null,
                COL_TYPE + "=?", new String[]{type},
                null, null, COL_DATE + " DESC");

        if (cursor.moveToFirst()) {
            do {
                Debt debt = new Debt();
                debt.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COL_ID)));
                debt.setPersonName(cursor.getString(cursor.getColumnIndexOrThrow(COL_PERSON_NAME)));
                debt.setAmount(cursor.getDouble(cursor.getColumnIndexOrThrow(COL_AMOUNT)));
                debt.setType(cursor.getString(cursor.getColumnIndexOrThrow(COL_TYPE)));
                debt.setDescription(cursor.getString(cursor.getColumnIndexOrThrow(COL_DESCRIPTION)));
                debt.setDate(cursor.getLong(cursor.getColumnIndexOrThrow(COL_DATE)));
                debt.setPaid(cursor.getInt(cursor.getColumnIndexOrThrow(COL_IS_PAID)) == 1);

                int dueDateIndex = cursor.getColumnIndex(COL_DUE_DATE);
                if (dueDateIndex != -1 && !cursor.isNull(dueDateIndex)) {
                    debt.setDueDate(cursor.getLong(dueDateIndex));
                }

                int reminderIndex = cursor.getColumnIndex(COL_REMINDER_TIME);
                if (reminderIndex != -1 && !cursor.isNull(reminderIndex)) {
                    debt.setReminderTime(cursor.getLong(reminderIndex));
                }

                int calendarIndex = cursor.getColumnIndex(COL_CALENDAR_EVENT_ID);
                if (calendarIndex != -1 && !cursor.isNull(calendarIndex)) {
                    debt.setCalendarEventId(cursor.getString(calendarIndex));
                }

                debtList.add(debt);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return debtList;
    }

    public Debt getDebtById(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_DEBTS, null,
                COL_ID + "=?", new String[]{String.valueOf(id)},
                null, null, null);

        Debt debt = null;
        if (cursor.moveToFirst()) {
            debt = new Debt();
            debt.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COL_ID)));
            debt.setPersonName(cursor.getString(cursor.getColumnIndexOrThrow(COL_PERSON_NAME)));
            debt.setAmount(cursor.getDouble(cursor.getColumnIndexOrThrow(COL_AMOUNT)));
            debt.setType(cursor.getString(cursor.getColumnIndexOrThrow(COL_TYPE)));
            debt.setDescription(cursor.getString(cursor.getColumnIndexOrThrow(COL_DESCRIPTION)));
            debt.setDate(cursor.getLong(cursor.getColumnIndexOrThrow(COL_DATE)));
            debt.setPaid(cursor.getInt(cursor.getColumnIndexOrThrow(COL_IS_PAID)) == 1);

            int dueDateIndex = cursor.getColumnIndex(COL_DUE_DATE);
            if (dueDateIndex != -1 && !cursor.isNull(dueDateIndex)) {
                debt.setDueDate(cursor.getLong(dueDateIndex));
            }

            int reminderIndex = cursor.getColumnIndex(COL_REMINDER_TIME);
            if (reminderIndex != -1 && !cursor.isNull(reminderIndex)) {
                debt.setReminderTime(cursor.getLong(reminderIndex));
            }

            int calendarIndex = cursor.getColumnIndex(COL_CALENDAR_EVENT_ID);
            if (calendarIndex != -1 && !cursor.isNull(calendarIndex)) {
                debt.setCalendarEventId(cursor.getString(calendarIndex));
            }
        }
        cursor.close();
        db.close();
        return debt;
    }

    public double getTotalByType(String type) {
        SQLiteDatabase db = this.getReadableDatabase();
        double total = 0;

        Cursor cursor = db.rawQuery(
                "SELECT SUM(" + COL_AMOUNT + ") FROM " + TABLE_DEBTS +
                        " WHERE " + COL_TYPE + "=? AND " + COL_IS_PAID + "=0",
                new String[]{type});

        if (cursor.moveToFirst()) {
            total = cursor.getDouble(0);
        }
        cursor.close();
        db.close();
        return total;
    }

    public void markAsPaid(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_IS_PAID, 1);

        db.update(TABLE_DEBTS, values, COL_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
    }

    public void deleteDebt(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_DEBTS, COL_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
    }

    public void updateReminderTime(int id, long reminderTime) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_REMINDER_TIME, reminderTime);
        db.update(TABLE_DEBTS, values, COL_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
    }

    public void updateCalendarEventId(int id, String calendarEventId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_CALENDAR_EVENT_ID, calendarEventId);
        db.update(TABLE_DEBTS, values, COL_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
    }

    public List<Debt> getUpcomingReminders(long fromTime, long toTime) {
        List<Debt> debtList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_DEBTS, null,
                COL_REMINDER_TIME + ">=? AND " + COL_REMINDER_TIME + "<=? AND " + COL_IS_PAID + "=0",
                new String[]{String.valueOf(fromTime), String.valueOf(toTime)},
                null, null, COL_REMINDER_TIME + " ASC");

        if (cursor.moveToFirst()) {
            do {
                Debt debt = new Debt();
                debt.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COL_ID)));
                debt.setPersonName(cursor.getString(cursor.getColumnIndexOrThrow(COL_PERSON_NAME)));
                debt.setAmount(cursor.getDouble(cursor.getColumnIndexOrThrow(COL_AMOUNT)));
                debt.setType(cursor.getString(cursor.getColumnIndexOrThrow(COL_TYPE)));
                debt.setDescription(cursor.getString(cursor.getColumnIndexOrThrow(COL_DESCRIPTION)));
                debt.setDate(cursor.getLong(cursor.getColumnIndexOrThrow(COL_DATE)));
                debt.setPaid(cursor.getInt(cursor.getColumnIndexOrThrow(COL_IS_PAID)) == 1);

                int dueDateIndex = cursor.getColumnIndex(COL_DUE_DATE);
                if (dueDateIndex != -1 && !cursor.isNull(dueDateIndex)) {
                    debt.setDueDate(cursor.getLong(dueDateIndex));
                }

                int reminderIndex = cursor.getColumnIndex(COL_REMINDER_TIME);
                if (reminderIndex != -1 && !cursor.isNull(reminderIndex)) {
                    debt.setReminderTime(cursor.getLong(reminderIndex));
                }

                int calendarIndex = cursor.getColumnIndex(COL_CALENDAR_EVENT_ID);
                if (calendarIndex != -1 && !cursor.isNull(calendarIndex)) {
                    debt.setCalendarEventId(cursor.getString(calendarIndex));
                }

                debtList.add(debt);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return debtList;
    }
}