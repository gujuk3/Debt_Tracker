package com.example.debttracker;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.debttracker.database.DatabaseHelper;
import com.example.debttracker.database.Debt;
import com.example.debttracker.notification.NotificationHelper;
import com.example.debttracker.service.CurrencyService;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DebtDetailActivity extends AppCompatActivity {
    private TextView tvPersonName, tvAmount, tvAmountUsd, tvType, tvDate, tvDueDate, tvDescription;
    private TextView tvDueDateLabel;
    private Button btnMarkPaid, btnDelete, btnShare;

    private DatabaseHelper dbHelper;
    private Debt debt;
    private int debtId;
    private double currentExchangeRate = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debt_detail);

        dbHelper = new DatabaseHelper(this);
        debtId = getIntent().getIntExtra("DEBT_ID", -1);

        initViews();
        loadDebtDetails();
        setupButtons();
        loadExchangeRate();
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
        tvAmountUsd = findViewById(R.id.tvAmountUsd);
        tvType = findViewById(R.id.tvType);
        tvDate = findViewById(R.id.tvDate);
        tvDueDate = findViewById(R.id.tvDueDate);
        tvDueDateLabel = findViewById(R.id.tvDueDateLabel);
        tvDescription = findViewById(R.id.tvDescription);
        btnMarkPaid = findViewById(R.id.btnMarkPaid);
        btnDelete = findViewById(R.id.btnDelete);
        btnShare = findViewById(R.id.btnShare);
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

            // Vade tarihi
            if (debt.getDueDate() > 0) {
                tvDueDate.setText(dateFormat.format(new Date(debt.getDueDate())));
                tvDueDate.setVisibility(View.VISIBLE);
                tvDueDateLabel.setVisibility(View.VISIBLE);
            } else {
                tvDueDate.setVisibility(View.GONE);
                tvDueDateLabel.setVisibility(View.GONE);
            }

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

    private void loadExchangeRate() {
        CurrencyService currencyService = CurrencyService.getInstance(this);
        currencyService.fetchExchangeRate(new CurrencyService.CurrencyCallback() {
            @Override
            public void onSuccess(double usdToTryRate) {
                currentExchangeRate = usdToTryRate;
                runOnUiThread(() -> updateUsdAmount(usdToTryRate));
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> tvAmountUsd.setVisibility(View.GONE));
            }
        });
    }

    private void updateUsdAmount(double usdToTryRate) {
        if (debt != null && usdToTryRate > 0) {
            double usdAmount = debt.getAmount() / usdToTryRate;
            NumberFormat usdFormat = NumberFormat.getCurrencyInstance(Locale.US);
            tvAmountUsd.setText("(~" + usdFormat.format(usdAmount) + ")");
            tvAmountUsd.setVisibility(View.VISIBLE);
        }
    }

    private void setupButtons() {
        btnMarkPaid.setOnClickListener(v -> {
            dbHelper.markAsPaid(debtId);
            // Bildirimi iptal et
            NotificationHelper notificationHelper = new NotificationHelper(this);
            notificationHelper.cancelNotification(debtId);
            Toast.makeText(this, "Ödendi olarak işaretlendi", Toast.LENGTH_SHORT).show();
            finish();
        });

        btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Sil")
                    .setMessage("Bu kaydı silmek istediğinize emin misiniz?")
                    .setPositiveButton("Sil", (dialog, which) -> {
                        // Bildirimi iptal et
                        NotificationHelper notificationHelper = new NotificationHelper(this);
                        notificationHelper.cancelNotification(debtId);
                        dbHelper.deleteDebt(debtId);
                        Toast.makeText(this, "Silindi", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .setNegativeButton("İptal", null)
                    .show();
        });

        btnShare.setOnClickListener(v -> shareDebt());
    }

    private void shareDebt() {
        if (debt == null) return;

        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("tr", "TR"));
        String amountStr = currencyFormat.format(debt.getAmount());

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy", new Locale("tr"));

        StringBuilder message = new StringBuilder();
        message.append("💰 Borç Bilgisi\n\n");
        message.append("Kişi: ").append(debt.getPersonName()).append("\n");
        message.append("Tutar: ").append(amountStr);

        // USD karşılığı
        if (currentExchangeRate > 0) {
            double usdAmount = debt.getAmount() / currentExchangeRate;
            NumberFormat usdFormat = NumberFormat.getCurrencyInstance(Locale.US);
            message.append(" (~").append(usdFormat.format(usdAmount)).append(")");
        }
        message.append("\n");

        message.append("Tür: ").append(debt.getType().equals("RECEIVABLE") ? "Alacak" : "Borç").append("\n");
        message.append("Tarih: ").append(dateFormat.format(new Date(debt.getDate()))).append("\n");

        // Vade tarihi
        if (debt.getDueDate() > 0) {
            message.append("Vade Tarihi: ").append(dateFormat.format(new Date(debt.getDueDate()))).append("\n");
        }

        // Açıklama
        if (debt.getDescription() != null && !debt.getDescription().isEmpty()) {
            message.append("Açıklama: ").append(debt.getDescription()).append("\n");
        }

        message.append("\n- Debt Tracker");

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, message.toString());

        startActivity(Intent.createChooser(shareIntent, "Paylaş"));
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
