package com.example.debttracker;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.debttracker.database.DatabaseHelper;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class AddDebtActivity extends AppCompatActivity {
    private EditText etPersonName, etAmount, etDescription;
    private TextView tvDate, tvTitle;
    private Button btnSave, btnSelectDate;
    private Toolbar toolbar;

    private DatabaseHelper dbHelper;
    private String type;
    private long selectedDate;
    private Calendar calendar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_debt);

        dbHelper = new DatabaseHelper(this);
        type = getIntent().getStringExtra("TYPE");

        initViews();
        setupToolbar();
        setupDatePicker();
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

    private void updateDateDisplay() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy", new Locale("tr"));
        tvDate.setText(dateFormat.format(selectedDate));
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

            dbHelper.addDebt(personName, amount, type, description, selectedDate);

            Toast.makeText(this, "Kaydedildi", Toast.LENGTH_SHORT).show();
            finish();
        });
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