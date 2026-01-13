package com.example.debttracker;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.example.debttracker.database.DatabaseHelper;
import com.example.debttracker.database.Debt;
import com.example.debttracker.notification.NotificationHelper;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class AddDebtActivity extends AppCompatActivity {
    private EditText etPersonName, etAmount, etDescription;
    private TextView tvDate, tvTitle, tvDueDate;
    private Button btnSave, btnSelectDate, btnSelectDueDate;
    private CheckBox cbAddToCalendar, cbEnableNotification;
    private Toolbar toolbar;

    private DatabaseHelper dbHelper;
    private String type;
    private long selectedDate;
    private long selectedDueDate = 0;
    private Calendar calendar;
    private Calendar dueDateCalendar;

    private ActivityResultLauncher<String> notificationPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_debt);

        dbHelper = new DatabaseHelper(this);
        type = getIntent().getStringExtra("TYPE");

        initViews();
        setupToolbar();
        setupDatePicker();
        setupDueDatePicker();
        setupSaveButton();
        setupKeyboardActions();
        setupPermissionLauncher();

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
        btnSave = findViewById(R.id.btnSave);

        // Yeni alanlar
        tvDueDate = findViewById(R.id.tvDueDate);
        btnSelectDueDate = findViewById(R.id.btnSelectDueDate);
        cbAddToCalendar = findViewById(R.id.cbAddToCalendar);
        cbEnableNotification = findViewById(R.id.cbEnableNotification);

        calendar = Calendar.getInstance();
        dueDateCalendar = Calendar.getInstance();
        selectedDate = calendar.getTimeInMillis();
        updateDateDisplay();

        // Vade tarihi seçilmediğinde checkbox'ları gizle
        updateDueDateDependentViews();
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("");
        }
    }

    private void setupPermissionLauncher() {
        notificationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (!isGranted) {
                        cbEnableNotification.setChecked(false);
                        Toast.makeText(this, "Bildirim izni verilmedi", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void setupKeyboardActions() {
        etPersonName.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        etAmount.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        etDescription.setImeOptions(EditorInfo.IME_ACTION_DONE);
        etDescription.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
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
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    this,
                    (view, year, month, dayOfMonth) -> {
                        dueDateCalendar.set(year, month, dayOfMonth);
                        selectedDueDate = dueDateCalendar.getTimeInMillis();
                        updateDueDateDisplay();
                        updateDueDateDependentViews();
                    },
                    dueDateCalendar.get(Calendar.YEAR),
                    dueDateCalendar.get(Calendar.MONTH),
                    dueDateCalendar.get(Calendar.DAY_OF_MONTH)
            );
            // Bugünden önceki tarihleri seçmeyi engelle
            datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());
            datePickerDialog.show();
        });

        // Bildirim checkbox'ına tıklandığında izin kontrolü
        cbEnableNotification.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                requestNotificationPermission();
            }
        });
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    private void updateDateDisplay() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy", new Locale("tr"));
        tvDate.setText(dateFormat.format(selectedDate));
    }

    private void updateDueDateDisplay() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy", new Locale("tr"));
        tvDueDate.setText(dateFormat.format(selectedDueDate));
    }

    private void updateDueDateDependentViews() {
        boolean hasDueDate = selectedDueDate > 0;
        cbAddToCalendar.setVisibility(hasDueDate ? View.VISIBLE : View.GONE);
        cbEnableNotification.setVisibility(hasDueDate ? View.VISIBLE : View.GONE);

        if (!hasDueDate) {
            tvDueDate.setText("Seçilmedi");
            cbAddToCalendar.setChecked(false);
            cbEnableNotification.setChecked(false);
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
            boolean notificationEnabled = cbEnableNotification.isChecked();

            // Veritabanına kaydet
            long debtId = dbHelper.addDebt(personName, amount, type, description, selectedDate, selectedDueDate, notificationEnabled);

            // Takvime ekle
            if (cbAddToCalendar.isChecked() && selectedDueDate > 0) {
                addToCalendar(personName, amount, description);
            }

            // Bildirim kurulumu
            if (notificationEnabled && selectedDueDate > 0) {
                Debt debt = dbHelper.getDebtById((int) debtId);
                if (debt != null) {
                    NotificationHelper notificationHelper = new NotificationHelper(this);
                    notificationHelper.scheduleNotification(debt);
                }
            }

            Toast.makeText(this, "Kaydedildi", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void addToCalendar(String personName, double amount, String description) {
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("tr", "TR"));
        String amountStr = currencyFormat.format(amount);

        String title = type.equals("RECEIVABLE")
                ? "Alacak: " + personName
                : "Borç Ödemesi: " + personName;

        String desc = "Tutar: " + amountStr;
        if (description != null && !description.isEmpty()) {
            desc += "\nAçıklama: " + description;
        }

        Intent intent = new Intent(Intent.ACTION_INSERT)
                .setData(CalendarContract.Events.CONTENT_URI)
                .putExtra(CalendarContract.Events.TITLE, title)
                .putExtra(CalendarContract.Events.DESCRIPTION, desc)
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, selectedDueDate)
                .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, selectedDueDate + (60 * 60 * 1000)) // 1 saat
                .putExtra(CalendarContract.Events.ALL_DAY, false)
                .putExtra(CalendarContract.Events.HAS_ALARM, true);

        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Toast.makeText(this, "Takvim uygulaması bulunamadı", Toast.LENGTH_SHORT).show();
        }
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
