package com.example.debttracker;

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.debttracker.database.DatabaseHelper;
import com.example.debttracker.database.Debt;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import androidx.appcompat.widget.Toolbar;
import android.view.MenuItem;

public class DebtDetailActivity extends AppCompatActivity {
    private TextView tvPersonName, tvAmount, tvType, tvDate, tvDescription;
    private Button btnMarkPaid, btnDelete;

    private DatabaseHelper dbHelper;
    private Debt debt;
    private int debtId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debt_detail);

        dbHelper = new DatabaseHelper(this);
        debtId = getIntent().getIntExtra("DEBT_ID", -1);

        initViews();
        loadDebtDetails();
        setupButtons();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbarDetail);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Detaylar");
        }

        tvPersonName = findViewById(R.id.tvPersonName);
        tvAmount = findViewById(R.id.tvAmount);
        tvType = findViewById(R.id.tvType);
        tvDate = findViewById(R.id.tvDate);
        tvDescription = findViewById(R.id.tvDescription);
        btnMarkPaid = findViewById(R.id.btnMarkPaid);
        btnDelete = findViewById(R.id.btnDelete);
    }

    private void loadDebtDetails() {
        debt = dbHelper.getDebtById(debtId);

        if (debt != null) {
            tvPersonName.setText(debt.getPersonName());

            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("tr", "TR"));
            tvAmount.setText(currencyFormat.format(debt.getAmount()));

            tvType.setText(debt.getType().equals("RECEIVABLE") ? "Alacak" : "Borç");

            SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy", new Locale("tr"));
            tvDate.setText(dateFormat.format(new Date(debt.getDate())));

            if (debt.getDescription() != null && !debt.getDescription().isEmpty()) {
                tvDescription.setText(debt.getDescription());
            } else {
                tvDescription.setText("Açıklama yok");
            }

            if (debt.isPaid()) {
                btnMarkPaid.setText("Ödendi olarak işaretlendi");
                btnMarkPaid.setEnabled(false);
            }
        }
    }

    private void setupButtons() {
        btnMarkPaid.setOnClickListener(v -> {
            dbHelper.markAsPaid(debtId);
            Toast.makeText(this, "Ödendi olarak işaretlendi", Toast.LENGTH_SHORT).show();
            finish();
        });

        btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Sil")
                    .setMessage("Bu kaydı silmek istediğinize emin misiniz?")
                    .setPositiveButton("Sil", (dialog, which) -> {
                        dbHelper.deleteDebt(debtId);
                        Toast.makeText(this, "Silindi", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .setNegativeButton("İptal", null)
                    .show();
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