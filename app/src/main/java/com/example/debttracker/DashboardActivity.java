package com.example.debttracker;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.debttracker.database.DatabaseHelper;
import com.example.debttracker.service.CurrencyService;

import java.text.NumberFormat;
import java.util.Locale;

public class DashboardActivity extends AppCompatActivity {
    private TextView tvTotalBalance, tvTotalBalanceUsd;
    private TextView tvTotalReceivable, tvTotalReceivableUsd;
    private TextView tvTotalPayable, tvTotalPayableUsd;
    private CardView cardReceivable, cardPayable;
    private DatabaseHelper dbHelper;

    private double totalReceivable = 0;
    private double totalPayable = 0;
    private double balance = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        dbHelper = new DatabaseHelper(this);

        initViews();
        setupCardClicks();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTotals();
        loadExchangeRate();
    }

    private void initViews() {
        tvTotalBalance = findViewById(R.id.tvTotalBalance);
        tvTotalBalanceUsd = findViewById(R.id.tvTotalBalanceUsd);
        tvTotalReceivable = findViewById(R.id.tvTotalReceivable);
        tvTotalReceivableUsd = findViewById(R.id.tvTotalReceivableUsd);
        tvTotalPayable = findViewById(R.id.tvTotalPayable);
        tvTotalPayableUsd = findViewById(R.id.tvTotalPayableUsd);
        cardReceivable = findViewById(R.id.cardReceivable);
        cardPayable = findViewById(R.id.cardPayable);
    }

    private void setupCardClicks() {
        cardReceivable.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, MainActivity.class);
            intent.putExtra("TAB", 0); // Alacaklar sekmesi
            startActivity(intent);
        });

        cardPayable.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, MainActivity.class);
            intent.putExtra("TAB", 1); // Borçlar sekmesi
            startActivity(intent);
        });
    }

    private void loadTotals() {
        totalReceivable = dbHelper.getTotalByType("RECEIVABLE");
        totalPayable = dbHelper.getTotalByType("PAYABLE");
        balance = totalReceivable - totalPayable;

        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("tr", "TR"));

        // Toplam bakiye
        tvTotalBalance.setText(currencyFormat.format(Math.abs(balance)));
        if (balance >= 0) {
            tvTotalBalance.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
        } else {
            tvTotalBalance.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        }

        // Alacaklar
        tvTotalReceivable.setText(currencyFormat.format(totalReceivable));

        // Borçlar
        tvTotalPayable.setText(currencyFormat.format(totalPayable));
    }

    private void loadExchangeRate() {
        CurrencyService currencyService = CurrencyService.getInstance(this);
        currencyService.fetchExchangeRate(new CurrencyService.CurrencyCallback() {
            @Override
            public void onSuccess(double usdToTryRate) {
                runOnUiThread(() -> updateUsdAmounts(usdToTryRate));
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> hideUsdAmounts());
            }
        });
    }

    private void updateUsdAmounts(double usdToTryRate) {
        if (usdToTryRate <= 0) {
            hideUsdAmounts();
            return;
        }

        NumberFormat usdFormat = NumberFormat.getCurrencyInstance(Locale.US);

        // Bakiye USD
        double balanceUsd = Math.abs(balance) / usdToTryRate;
        tvTotalBalanceUsd.setText("(~" + usdFormat.format(balanceUsd) + ")");
        tvTotalBalanceUsd.setVisibility(View.VISIBLE);

        // Alacak USD
        double receivableUsd = totalReceivable / usdToTryRate;
        tvTotalReceivableUsd.setText("(~" + usdFormat.format(receivableUsd) + ")");
        tvTotalReceivableUsd.setVisibility(View.VISIBLE);

        // Borç USD
        double payableUsd = totalPayable / usdToTryRate;
        tvTotalPayableUsd.setText("(~" + usdFormat.format(payableUsd) + ")");
        tvTotalPayableUsd.setVisibility(View.VISIBLE);
    }

    private void hideUsdAmounts() {
        tvTotalBalanceUsd.setVisibility(View.GONE);
        tvTotalReceivableUsd.setVisibility(View.GONE);
        tvTotalPayableUsd.setVisibility(View.GONE);
    }
}
