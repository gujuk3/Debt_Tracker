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
    private static final int DATABASE_VERSION = 2; // Versiyon artırıldı

    private static final String TABLE_DEBTS = "debts";
    private static final String COL_ID = "id";
    private static final String COL_PERSON_NAME = "person_name";
    private static final String COL_AMOUNT = "amount";
    private static final String COL_TYPE = "type";
    private static final String COL_DESCRIPTION = "description";
    private static final String COL_DATE = "date";
    private static final String COL_IS_PAID = "is_paid";
    private static final String COL_DUE_DATE = "due_date";
    private static final String COL_NOTIFICATION_ENABLED = "notification_enabled";

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
                COL_DUE_DATE + " INTEGER DEFAULT 0, " +
                COL_NOTIFICATION_ENABLED + " INTEGER DEFAULT 0)";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            // Yeni sütunları ekle
            db.execSQL("ALTER TABLE " + TABLE_DEBTS + " ADD COLUMN " + COL_DUE_DATE + " INTEGER DEFAULT 0");
            db.execSQL("ALTER TABLE " + TABLE_DEBTS + " ADD COLUMN " + COL_NOTIFICATION_ENABLED + " INTEGER DEFAULT 0");
        }
    }

    public long addDebt(String personName, double amount, String type, String description, long date) {
        return addDebt(personName, amount, type, description, date, 0, false);
    }

    public long addDebt(String personName, double amount, String type, String description, long date, long dueDate, boolean notificationEnabled) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_PERSON_NAME, personName);
        values.put(COL_AMOUNT, amount);
        values.put(COL_TYPE, type);
        values.put(COL_DESCRIPTION, description);
        values.put(COL_DATE, date);
        values.put(COL_IS_PAID, 0);
        values.put(COL_DUE_DATE, dueDate);
        values.put(COL_NOTIFICATION_ENABLED, notificationEnabled ? 1 : 0);

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
                Debt debt = cursorToDebt(cursor);
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
            debt = cursorToDebt(cursor);
        }
        cursor.close();
        db.close();
        return debt;
    }

    public List<Debt> getDebtsWithNotification() {
        List<Debt> debtList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_DEBTS, null,
                COL_NOTIFICATION_ENABLED + "=1 AND " + COL_IS_PAID + "=0 AND " + COL_DUE_DATE + ">0",
                null, null, null, COL_DUE_DATE + " ASC");

        if (cursor.moveToFirst()) {
            do {
                Debt debt = cursorToDebt(cursor);
                debtList.add(debt);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return debtList;
    }

    private Debt cursorToDebt(Cursor cursor) {
        Debt debt = new Debt();
        debt.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COL_ID)));
        debt.setPersonName(cursor.getString(cursor.getColumnIndexOrThrow(COL_PERSON_NAME)));
        debt.setAmount(cursor.getDouble(cursor.getColumnIndexOrThrow(COL_AMOUNT)));
        debt.setType(cursor.getString(cursor.getColumnIndexOrThrow(COL_TYPE)));
        debt.setDescription(cursor.getString(cursor.getColumnIndexOrThrow(COL_DESCRIPTION)));
        debt.setDate(cursor.getLong(cursor.getColumnIndexOrThrow(COL_DATE)));
        debt.setPaid(cursor.getInt(cursor.getColumnIndexOrThrow(COL_IS_PAID)) == 1);
        debt.setDueDate(cursor.getLong(cursor.getColumnIndexOrThrow(COL_DUE_DATE)));
        debt.setNotificationEnabled(cursor.getInt(cursor.getColumnIndexOrThrow(COL_NOTIFICATION_ENABLED)) == 1);
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
}
