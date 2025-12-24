package com.example.debttracker;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.debttracker.database.DatabaseHelper;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.NumberFormat;
import java.util.Locale;

public class DashboardActivity extends AppCompatActivity {
    private TextView tvTotalBalance, tvTotalReceivable, tvTotalPayable;
    private CardView cardReceivable, cardPayable;
    private FloatingActionButton fabSettings;
    private DatabaseHelper dbHelper;

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
    }

    private void initViews() {
        tvTotalBalance = findViewById(R.id.tvTotalBalance);
        tvTotalReceivable = findViewById(R.id.tvTotalReceivable);
        tvTotalPayable = findViewById(R.id.tvTotalPayable);
        cardReceivable = findViewById(R.id.cardReceivable);
        cardPayable = findViewById(R.id.cardPayable);
        fabSettings = findViewById(R.id.fabSettings);
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

        fabSettings.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, SettingsActivity.class);
            startActivity(intent);
        });
    }

    private void loadTotals() {
        double totalReceivable = dbHelper.getTotalByType("RECEIVABLE");
        double totalPayable = dbHelper.getTotalByType("PAYABLE");
        double balance = totalReceivable - totalPayable;

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
}