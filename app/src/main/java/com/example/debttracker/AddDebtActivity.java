package com.example.debttracker;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;

import com.example.debttracker.calendar.CalendarIntegration;
import com.example.debttracker.database.DatabaseHelper;
import com.example.debttracker.reminder.ReminderManager;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class AddDebtActivity extends AppCompatActivity {
    private EditText etPersonName, etAmount, etDescription;
    private TextView tvDate, tvTitle, tvDueDate;
    private Button btnSave, btnSelectDate, btnSelectDueDate;
    private SwitchCompat switchReminder;
    private Toolbar toolbar;

    private DatabaseHelper dbHelper;
    private ReminderManager reminderManager;
    private CalendarIntegration calendarIntegration;
    private String type;
    private long selectedDate;
    private Long selectedDueDate = null;
    private Calendar calendar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_debt);

        dbHelper = new DatabaseHelper(this);
        reminderManager = new ReminderManager(this);
        calendarIntegration = new CalendarIntegration(this);
        type = getIntent().getStringExtra("TYPE");

        initViews();
        setupToolbar();
        setupDatePicker();
        setupDueDatePicker();
        setupSaveButton();
        setupKeyboardActions();

        tvTitle.setText(type.equals("RECEIVABLE") ? "Yeni Alacak Ekle" : "Yeni Borç Ekle");
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        tvTitle = findViewById(R.id.tvTitle);
        etPersonName = findViewById(R.id.etPersonName);
        etAmount = findViewById(R.id.etAmount);
        etDescription = findViewById(R.id.etDescription);
        tvDate = findViewById(R.id.tvDate);
        btnSelectDate = findViewById(R.id.btnSelectDate);
        tvDueDate = findViewById(R.id.tvDueDate);
        btnSelectDueDate = findViewById(R.id.btnSelectDueDate);
        switchReminder = findViewById(R.id.switchReminder);
        btnSave = findViewById(R.id.btnSave);

        calendar = Calendar.getInstance();
        selectedDate = calendar.getTimeInMillis();
        updateDateDisplay();
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("");
        }
    }

    private void setupKeyboardActions() {
        // Kişi adı için - Enter'a basınca bir sonraki alana geç
        etPersonName.setImeOptions(EditorInfo.IME_ACTION_NEXT);

        // Tutar için - Enter'a basınca bir sonraki alana geç
        etAmount.setImeOptions(EditorInfo.IME_ACTION_NEXT);

        // Açıklama için - Enter'a basınca klavyeyi kapat
        etDescription.setImeOptions(EditorInfo.IME_ACTION_DONE);
        etDescription.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                // Klavyeyi kapat
                android.view.inputmethod.InputMethodManager imm =
                        (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(etDescription.getWindowToken(), 0);
                etDescription.clearFocus();
                return true;
            }
            return false;
        });
    }

    private void setupDatePicker() {
        btnSelectDate.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    this,
                    (view, year, month, dayOfMonth) -> {
                        calendar.set(year, month, dayOfMonth);
                        selectedDate = calendar.getTimeInMillis();
                        updateDateDisplay();
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();
        });
    }

    private void setupDueDatePicker() {
        btnSelectDueDate.setOnClickListener(v -> {
            Calendar dueDateCalendar = Calendar.getInstance();
            if (selectedDueDate != null) {
                dueDateCalendar.setTimeInMillis(selectedDueDate);
            }

            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    this,
                    (view, year, month, dayOfMonth) -> {
                        dueDateCalendar.set(year, month, dayOfMonth);
                        selectedDueDate = dueDateCalendar.getTimeInMillis();
                        updateDueDateDisplay();
                        switchReminder.setEnabled(true);
                    },
                    dueDateCalendar.get(Calendar.YEAR),
                    dueDateCalendar.get(Calendar.MONTH),
                    dueDateCalendar.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());
            datePickerDialog.show();
        });
    }

    private void updateDateDisplay() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy", new Locale("tr"));
        tvDate.setText(dateFormat.format(selectedDate));
    }

    private void updateDueDateDisplay() {
        if (selectedDueDate != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy", new Locale("tr"));
            tvDueDate.setText(dateFormat.format(selectedDueDate));
        } else {
            tvDueDate.setText("Seçilmedi");
        }
    }

    private void setupSaveButton() {
        btnSave.setOnClickListener(v -> {
            String personName = etPersonName.getText().toString().trim();
            String amountStr = etAmount.getText().toString().trim();
            String description = etDescription.getText().toString().trim();

            if (personName.isEmpty()) {
                etPersonName.setError("İsim gerekli");
                etPersonName.requestFocus();
                return;
            }

            if (amountStr.isEmpty()) {
                etAmount.setError("Tutar gerekli");
                etAmount.requestFocus();
                return;
            }

            double amount = Double.parseDouble(amountStr);

            Long reminderTime = null;
            if (selectedDueDate != null && switchReminder.isChecked() && SettingsActivity.isRemindersEnabled(this)) {
                int hoursBefore = SettingsActivity.getReminderHoursBefore(this);
                reminderTime = selectedDueDate - (hoursBefore * 60 * 60 * 1000);
            }

            String calendarEventId = null;
            if (selectedDueDate != null && SettingsActivity.isCalendarEnabled(this)) {
                long eventId = calendarIntegration.addDebtToCalendar(
                        createDebtFromInputs(personName, amount, description)
                );
                if (eventId != -1) {
                    calendarEventId = String.valueOf(eventId);
                }
            }

            long debtId = dbHelper.addDebt(personName, amount, type, description, selectedDate,
                    selectedDueDate, reminderTime, calendarEventId);

            if (reminderTime != null && debtId != -1) {
                reminderManager.scheduleReminder((int) debtId, reminderTime);
            }

            Toast.makeText(this, "Kaydedildi", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private com.example.debttracker.database.Debt createDebtFromInputs(String personName, double amount, String description) {
        com.example.debttracker.database.Debt debt = new com.example.debttracker.database.Debt();
        debt.setPersonName(personName);
        debt.setAmount(amount);
        debt.setType(type);
        debt.setDescription(description);
        debt.setDate(selectedDate);
        debt.setDueDate(selectedDueDate);
        return debt;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}