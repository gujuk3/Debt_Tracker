package com.example.debttracker;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.debttracker.calendar.CalendarIntegration;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.slider.Slider;
import com.google.android.material.textview.MaterialTextView;

public class SettingsActivity extends AppCompatActivity {
    private static final String PREFS_NAME = "DebtTrackerPrefs";
    private static final String KEY_ENABLE_REMINDERS = "enable_reminders";
    private static final String KEY_REMINDER_HOURS_BEFORE = "reminder_hours_before";
    private static final String KEY_ENABLE_CALENDAR = "enable_calendar";

    private static final int PERMISSION_REQUEST_CALENDAR = 100;
    private static final int PERMISSION_REQUEST_NOTIFICATION = 101;

    private SharedPreferences preferences;
    private SwitchCompat switchReminders;
    private SwitchCompat switchCalendar;
    private Slider sliderReminderHours;
    private MaterialTextView textReminderHours;
    private CalendarIntegration calendarIntegration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        preferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        calendarIntegration = new CalendarIntegration(this);

        initializeViews();
        setupToolbar();
        loadSettings();
        setupListeners();
    }

    private void initializeViews() {
        switchReminders = findViewById(R.id.switchReminders);
        switchCalendar = findViewById(R.id.switchCalendar);
        sliderReminderHours = findViewById(R.id.sliderReminderHours);
        textReminderHours = findViewById(R.id.textReminderHours);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Ayarlar");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void loadSettings() {
        boolean remindersEnabled = preferences.getBoolean(KEY_ENABLE_REMINDERS, true);
        int reminderHours = preferences.getInt(KEY_REMINDER_HOURS_BEFORE, 24);
        boolean calendarEnabled = preferences.getBoolean(KEY_ENABLE_CALENDAR, false);

        switchReminders.setChecked(remindersEnabled);
        switchCalendar.setChecked(calendarEnabled);
        sliderReminderHours.setValue(reminderHours);
        updateReminderHoursText(reminderHours);
    }

    private void setupListeners() {
        switchReminders.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && !hasNotificationPermission()) {
                requestNotificationPermission();
            } else {
                saveRemindersSetting(isChecked);
            }
        });

        switchCalendar.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && !hasCalendarPermission()) {
                requestCalendarPermission();
            } else {
                saveCalendarSetting(isChecked);
            }
        });

        sliderReminderHours.addOnChangeListener((slider, value, fromUser) -> {
            int hours = (int) value;
            updateReminderHoursText(hours);
            saveReminderHoursSetting(hours);
        });
    }

    private void updateReminderHoursText(int hours) {
        if (hours == 0) {
            textReminderHours.setText("Vade tarihinde");
        } else if (hours == 1) {
            textReminderHours.setText("1 saat önce");
        } else if (hours < 24) {
            textReminderHours.setText(hours + " saat önce");
        } else {
            int days = hours / 24;
            textReminderHours.setText(days + " gün önce");
        }
    }

    private void saveRemindersSetting(boolean enabled) {
        preferences.edit().putBoolean(KEY_ENABLE_REMINDERS, enabled).apply();
        Toast.makeText(this,
                enabled ? "Hatırlatmalar açıldı" : "Hatırlatmalar kapatıldı",
                Toast.LENGTH_SHORT).show();
    }

    private void saveCalendarSetting(boolean enabled) {
        preferences.edit().putBoolean(KEY_ENABLE_CALENDAR, enabled).apply();
        Toast.makeText(this,
                enabled ? "Takvim entegrasyonu açıldı" : "Takvim entegrasyonu kapatıldı",
                Toast.LENGTH_SHORT).show();
    }

    private void saveReminderHoursSetting(int hours) {
        preferences.edit().putInt(KEY_REMINDER_HOURS_BEFORE, hours).apply();
    }

    private boolean hasNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this,
                    Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private boolean hasCalendarPermission() {
        return ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED &&
               ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    PERMISSION_REQUEST_NOTIFICATION);
        }
    }

    private void requestCalendarPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{
                        Manifest.permission.READ_CALENDAR,
                        Manifest.permission.WRITE_CALENDAR
                },
                PERMISSION_REQUEST_CALENDAR);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                          @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_NOTIFICATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                saveRemindersSetting(true);
            } else {
                switchReminders.setChecked(false);
                Toast.makeText(this,
                        "Hatırlatmalar için bildirim izni gerekli",
                        Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == PERMISSION_REQUEST_CALENDAR) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                saveCalendarSetting(true);
            } else {
                switchCalendar.setChecked(false);
                Toast.makeText(this,
                        "Takvim entegrasyonu için takvim izni gerekli",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    public static boolean isRemindersEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_ENABLE_REMINDERS, true);
    }

    public static int getReminderHoursBefore(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_REMINDER_HOURS_BEFORE, 24);
    }

    public static boolean isCalendarEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_ENABLE_CALENDAR, false);
    }
}
