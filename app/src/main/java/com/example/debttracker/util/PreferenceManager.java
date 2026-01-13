package com.example.debttracker.util;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferenceManager {
    private static final String PREF_NAME = "DebtTrackerPrefs";
    private static final String KEY_EXCHANGE_RATE = "exchange_rate_usd_try";
    private static final String KEY_EXCHANGE_RATE_TIME = "exchange_rate_time";

    private static PreferenceManager instance;
    private final SharedPreferences prefs;

    private PreferenceManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized PreferenceManager getInstance(Context context) {
        if (instance == null) {
            instance = new PreferenceManager(context.getApplicationContext());
        }
        return instance;
    }

    // Döviz kuru kaydet
    public void saveExchangeRate(double rate) {
        prefs.edit()
                .putFloat(KEY_EXCHANGE_RATE, (float) rate)
                .putLong(KEY_EXCHANGE_RATE_TIME, System.currentTimeMillis())
                .apply();
    }

    // Kaydedilmiş döviz kurunu al
    public double getCachedExchangeRate() {
        return prefs.getFloat(KEY_EXCHANGE_RATE, 0f);
    }

    // Döviz kuru cache zamanını al
    public long getExchangeRateCacheTime() {
        return prefs.getLong(KEY_EXCHANGE_RATE_TIME, 0);
    }
}