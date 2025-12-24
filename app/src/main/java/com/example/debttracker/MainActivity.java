package com.example.debttracker;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.debttracker.adapter.DebtAdapter;
import com.example.debttracker.database.DatabaseHelper;
import com.example.debttracker.database.Debt;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import androidx.appcompat.widget.Toolbar;
import android.view.MenuItem;

public class MainActivity extends AppCompatActivity {
    private TabLayout tabLayout;
    private RecyclerView recyclerView;
    private DebtAdapter adapter;
    private FloatingActionButton fabAdd;
    private TextView tvTotalAmount;

    private DatabaseHelper dbHelper;
    private String currentType = "RECEIVABLE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);

        initViews();
        setupRecyclerView();
        setupTabLayout();
        setupFAB();

        // Dashboard'dan gelen tab bilgisini kontrol et
        int selectedTab = getIntent().getIntExtra("TAB", 0);
        tabLayout.selectTab(tabLayout.getTabAt(selectedTab));
        currentType = selectedTab == 0 ? "RECEIVABLE" : "PAYABLE";

        loadDebts();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbarMain);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        tabLayout = findViewById(R.id.tabLayout);
        recyclerView = findViewById(R.id.recyclerView);
        fabAdd = findViewById(R.id.fabAdd);
        tvTotalAmount = findViewById(R.id.tvTotalAmount);
    }

    private void setupRecyclerView() {
        adapter = new DebtAdapter(this, new ArrayList<>());
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setupTabLayout() {
        tabLayout.addTab(tabLayout.newTab().setText("Alacaklarım"));
        tabLayout.addTab(tabLayout.newTab().setText("Borçlarım"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentType = tab.getPosition() == 0 ? "RECEIVABLE" : "PAYABLE";
                loadDebts();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupFAB() {
        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddDebtActivity.class);
            intent.putExtra("TYPE", currentType);
            startActivity(intent);
        });
    }

    private void loadDebts() {
        List<Debt> debts = dbHelper.getDebtsByType(currentType);
        adapter.updateDebts(debts);

        double total = dbHelper.getTotalByType(currentType);

        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("tr", "TR"));
        tvTotalAmount.setText("Toplam: " + currencyFormat.format(total));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDebts();
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