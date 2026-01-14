package com.example.debttracker;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.debttracker.adapter.DebtAdapter;
import com.example.debttracker.database.DatabaseHelper;
import com.example.debttracker.database.Debt;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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
    private EditText etSearch;
    private ImageButton btnSort, btnFilter, btnExport;

    private DatabaseHelper dbHelper;
    private String currentType = "RECEIVABLE";
    private String currentSort = "DATE_DESC";
    private String currentFilter = "ALL";
    private String currentSearch = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);

        initViews();
        setupRecyclerView();
        setupTabLayout();
        setupFAB();
        setupSearchAndFilters();

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
        etSearch = findViewById(R.id.etSearch);
        btnSort = findViewById(R.id.btnSort);
        btnFilter = findViewById(R.id.btnFilter);
        btnExport = findViewById(R.id.btnExport);
    }

    private void setupSearchAndFilters() {
        // Arama
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearch = s.toString();
                loadDebts();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Sıralama
        btnSort.setOnClickListener(v -> showSortDialog());

        // Filtre
        btnFilter.setOnClickListener(v -> showFilterDialog());

        // Export
        btnExport.setOnClickListener(v -> showExportDialog());
    }

    private void showSortDialog() {
        String[] options = {
                "Tarihe Göre (Yeni)",
                "Tarihe Göre (Eski)",
                "Tutara Göre (Yüksek)",
                "Tutara Göre (Düşük)",
                "İsme Göre (A-Z)",
                "İsme Göre (Z-A)",
                "Vade Tarihine Göre (Yakın)",
                "Vade Tarihine Göre (Uzak)"
        };

        new AlertDialog.Builder(this)
                .setTitle("Sırala")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: currentSort = "DATE_DESC"; break;
                        case 1: currentSort = "DATE_ASC"; break;
                        case 2: currentSort = "AMOUNT_DESC"; break;
                        case 3: currentSort = "AMOUNT_ASC"; break;
                        case 4: currentSort = "NAME_ASC"; break;
                        case 5: currentSort = "NAME_DESC"; break;
                        case 6: currentSort = "DUE_DATE_ASC"; break;
                        case 7: currentSort = "DUE_DATE_DESC"; break;
                    }
                    loadDebts();
                })
                .show();
    }

    private void showFilterDialog() {
        String[] options = {"Tümü", "Ödenenler", "Ödenmeyenler"};

        new AlertDialog.Builder(this)
                .setTitle("Filtrele")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: currentFilter = "ALL"; break;
                        case 1: currentFilter = "PAID"; break;
                        case 2: currentFilter = "UNPAID"; break;
                    }
                    loadDebts();
                })
                .show();
    }

    private void showExportDialog() {
        String[] options = {"PDF Olarak", "CSV Olarak"};

        new AlertDialog.Builder(this)
                .setTitle("Dışa Aktar")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        exportToPdf();
                    } else {
                        exportToCsv();
                    }
                })
                .show();
    }

    private void exportToPdf() {
        List<Debt> debts = dbHelper.getAllDebts();

        if (debts.isEmpty()) {
            Toast.makeText(this, "Dışa aktarılacak veri yok", Toast.LENGTH_SHORT).show();
            return;
        }

        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);

        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();
        paint.setTextSize(12);

        Paint titlePaint = new Paint();
        titlePaint.setTextSize(18);
        titlePaint.setFakeBoldText(true);

        int y = 40;

        canvas.drawText("Borç Takip Raporu", 40, y, titlePaint);
        y += 30;

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy", new Locale("tr"));
        canvas.drawText("Oluşturulma: " + dateFormat.format(new Date()), 40, y, paint);
        y += 40;

        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("tr", "TR"));

        double totalReceivable = 0;
        double totalPayable = 0;

        for (Debt debt : debts) {
            if (y > 780) {
                document.finishPage(page);
                pageInfo = new PdfDocument.PageInfo.Builder(595, 842, document.getPages().size() + 1).create();
                page = document.startPage(pageInfo);
                canvas = page.getCanvas();
                y = 40;
            }

            String typeStr = debt.getType().equals("RECEIVABLE") ? "Alacak" : "Borç";
            String status = debt.isPaid() ? "(Ödendi)" : "";

            canvas.drawText(debt.getPersonName() + " - " + typeStr + " " + status, 40, y, paint);
            y += 18;
            canvas.drawText("   Tutar: " + currencyFormat.format(debt.getAmount()), 40, y, paint);
            y += 18;
            canvas.drawText("   Tarih: " + dateFormat.format(new Date(debt.getDate())), 40, y, paint);
            y += 25;

            if (debt.getType().equals("RECEIVABLE") && !debt.isPaid()) {
                totalReceivable += debt.getAmount();
            } else if (debt.getType().equals("PAYABLE") && !debt.isPaid()) {
                totalPayable += debt.getAmount();
            }
        }

        y += 20;
        canvas.drawText("Toplam Alacak: " + currencyFormat.format(totalReceivable), 40, y, titlePaint);
        y += 25;
        canvas.drawText("Toplam Borç: " + currencyFormat.format(totalPayable), 40, y, titlePaint);
        y += 25;
        canvas.drawText("Net: " + currencyFormat.format(totalReceivable - totalPayable), 40, y, titlePaint);

        document.finishPage(page);

        try {
            // Uygulama cache dizinine kaydet
            File cacheDir = new File(getCacheDir(), "exports");
            if (!cacheDir.exists()) {
                cacheDir.mkdirs();
            }
            String fileName = "borc_takip_" + System.currentTimeMillis() + ".pdf";
            File file = new File(cacheDir, fileName);
            FileOutputStream fos = new FileOutputStream(file);
            document.writeTo(fos);
            fos.close();
            document.close();

            // FileProvider ile paylaş
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("application/pdf");
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, "PDF'i Kaydet/Paylaş"));
        } catch (IOException e) {
            Toast.makeText(this, "PDF oluşturma hatası: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void exportToCsv() {
        List<Debt> debts = dbHelper.getAllDebts();

        if (debts.isEmpty()) {
            Toast.makeText(this, "Dışa aktarılacak veri yok", Toast.LENGTH_SHORT).show();
            return;
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy", new Locale("tr"));

        StringBuilder csv = new StringBuilder();
        csv.append("Kişi,Tutar,Tür,Tarih,Vade Tarihi,Durum,Açıklama\n");

        for (Debt debt : debts) {
            String typeStr = debt.getType().equals("RECEIVABLE") ? "Alacak" : "Borç";
            String status = debt.isPaid() ? "Ödendi" : "Ödenmedi";
            String dueDate = debt.getDueDate() > 0 ? dateFormat.format(new Date(debt.getDueDate())) : "";
            String description = debt.getDescription() != null ? debt.getDescription().replace(",", ";") : "";

            csv.append(String.format("\"%s\",%.2f,%s,%s,%s,%s,\"%s\"\n",
                    debt.getPersonName(),
                    debt.getAmount(),
                    typeStr,
                    dateFormat.format(new Date(debt.getDate())),
                    dueDate,
                    status,
                    description));
        }

        try {
            // Uygulama cache dizinine kaydet
            File cacheDir = new File(getCacheDir(), "exports");
            if (!cacheDir.exists()) {
                cacheDir.mkdirs();
            }
            String fileName = "borc_takip_" + System.currentTimeMillis() + ".csv";
            File file = new File(cacheDir, fileName);
            FileWriter writer = new FileWriter(file);
            writer.write(csv.toString());
            writer.close();

            // FileProvider ile paylaş
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/csv");
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, "CSV'yi Kaydet/Paylaş"));
        } catch (IOException e) {
            Toast.makeText(this, "CSV oluşturma hatası: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
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
        List<Debt> debts;

        // Arama varsa
        if (!currentSearch.isEmpty()) {
            debts = dbHelper.searchDebtsByType(currentType, currentSearch);
        }
        // Filtre varsa
        else if (!currentFilter.equals("ALL")) {
            debts = dbHelper.getDebtsByTypeWithFilter(currentType, currentFilter);
        }
        // Sıralama varsa
        else if (!currentSort.equals("DATE_DESC")) {
            debts = dbHelper.getDebtsByTypeWithSort(currentType, currentSort);
        }
        // Normal
        else {
            debts = dbHelper.getDebtsByType(currentType);
        }

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