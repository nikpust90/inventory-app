package com.example.inventory_app.activity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.inventory_app.ApiClient;
import com.example.inventory_app.ApiService;
import com.example.inventory_app.R;
import com.example.inventory_app.adapters.StockReportAdapter;
import com.example.inventory_app.models.Seriya;
import com.example.inventory_app.models.StockItem;
import com.example.inventory_app.models.StockReport;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StockReportActivity extends AppCompatActivity {

    private RecyclerView recycler;
    private ProgressBar progressBar;
    private StockReportAdapter adapter;
    private List<StockItem> flatList = new ArrayList<>();
    private ApiService apiService;
    private List<String> warehouseIds;
    private boolean isAdmin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_report);

        initViews();
        loadUserData();
        setupRecyclerView();
        loadStockReport();
    }

    private void initViews() {
        recycler = findViewById(R.id.recyclerStockReport);
        progressBar = findViewById(R.id.progressBar);
        apiService = ApiClient.getApiService();
    }

    private void setupRecyclerView() {
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new StockReportAdapter(this, flatList);
        recycler.setAdapter(adapter);
    }

    private void loadUserData() {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        Set<String> warehouseSet = prefs.getStringSet("WAREHOUSE_ID_LIST", new HashSet<>());
        warehouseIds = new ArrayList<>(warehouseSet);
        isAdmin = prefs.getBoolean("IS_ADMIN", false);

        if (warehouseIds.isEmpty() && !isAdmin) {
            Toast.makeText(this, "Ошибка: Склады не назначены", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void loadStockReport() {
        if (warehouseIds.isEmpty()) return;

        showLoading(true);
        String warehousesParam = String.join(",", warehouseIds);

        apiService.getStockReport(warehousesParam).enqueue(new Callback<List<StockReport>>() {
            @Override
            public void onResponse(Call<List<StockReport>> call, Response<List<StockReport>> response) {
                showLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    processStockData(response.body());
                } else {
                    // Подробная информация об ошибке
                    String errorMessage = "Ошибка сервера: " + response.code();
                    try {
                        if (response.errorBody() != null) {
                            String errorBody = response.errorBody().string();
                            errorMessage += " - " + errorBody;
                            // Логируем для отладки
                            Log.e("StockReport", "Error response: " + errorBody);
                        }
                    } catch (Exception e) {
                        Log.e("StockReport", "Error reading error body", e);
                    }
                    Toast.makeText(StockReportActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<List<StockReport>> call, Throwable t) {
                showLoading(false);
                Log.e("StockReport", "Network error", t);
                Toast.makeText(StockReportActivity.this,
                        "Ошибка загрузки: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void processStockData(List<StockReport> stockReports) {
        flatList.clear();

        for (StockReport report : stockReports) {
            // Создаем элемент склада
            StockItem warehouseItem = StockItem.warehouse(report.warehouseId, report.warehouseName);

            // Добавляем номенклатуры как детей склада
            if (report.items != null) {
                for (StockItem item : report.items) {
                    // Создаем номенклатуру с сериями
                    StockItem nomenclatureItem = StockItem.nomenclature(
                            item.nomenclatureId,
                            item.nomenclatureName,
                            item.totalQuantity,
                            item.reserveQuantity,
                            item.freeQuantity,
                            item.series
                    );

                    // Добавляем серии как детей номенклатуры
                    if (item.series != null) {
                        for (Seriya seriya : item.series) {
                            StockItem seriesItem = StockItem.series(seriya);
                            nomenclatureItem.children.add(seriesItem);
                        }
                    }

                    warehouseItem.children.add(nomenclatureItem);
                }
            }

            flatList.add(warehouseItem);
        }

        adapter.notifyDataSetChanged();

        if (flatList.isEmpty()) {
            Toast.makeText(this, "Нет данных по остаткам", Toast.LENGTH_SHORT).show();
        }
    }






    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        recycler.setVisibility(show ? View.GONE : View.VISIBLE);
    }
}
