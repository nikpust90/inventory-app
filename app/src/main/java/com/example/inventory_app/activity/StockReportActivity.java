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
import com.example.inventory_app.RemoteLogger;
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

    private static final String LOG_TAG = "StockReport";
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
        RemoteLogger.info(LOG_TAG, "ActivityLifecycle",
                "Запуск активности отчета по остаткам (onCreate)");
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

        RemoteLogger.info(LOG_TAG, "UserData",
                "Загружены данные пользователя. Складов: " + warehouseIds.size() +
                        ", Админ: " + isAdmin + ", Список ID: " + warehouseIds.toString());

        if (warehouseIds.isEmpty() && !isAdmin) {
            RemoteLogger.error(LOG_TAG, "UserDataError",
                    "Ошибка: Склады не назначены, пользователь не админ. Закрытие окна.", null);
            Toast.makeText(this, "Ошибка: Склады не назначены", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private static final int MAX_WAREHOUSES_PER_REQUEST = 50; // Максимум складов в одном запросе
    
    private void loadStockReport() {
        if (warehouseIds.isEmpty()) {
            RemoteLogger.warn(LOG_TAG, "APIRequest",
                    "Список складов пуст, запрос отменен.");
            return;
        }

        showLoading(true);
        
        // Если складов немного, делаем один запрос
        if (warehouseIds.size() <= MAX_WAREHOUSES_PER_REQUEST) {
            String warehousesParam = String.join(",", warehouseIds);
            RemoteLogger.info(LOG_TAG, "APIRequest",
                    "Отправка запроса getStockReport. Складов: " + warehouseIds.size());
            makeStockReportRequest(warehousesParam);
        } else {
            // Если складов много, разбиваем на несколько запросов
            RemoteLogger.info(LOG_TAG, "APIRequest",
                    "Разбиваем запрос на части. Всего складов: " + warehouseIds.size() + 
                    ", по " + MAX_WAREHOUSES_PER_REQUEST + " в запросе");
            loadStockReportInChunks();
        }
    }
    
    /**
     * Загрузка остатков частями (по MAX_WAREHOUSES_PER_REQUEST складов)
     */
    private void loadStockReportInChunks() {
        List<StockReport> allReports = new ArrayList<>();
        int totalChunks = (int) Math.ceil((double) warehouseIds.size() / MAX_WAREHOUSES_PER_REQUEST);
        final int[] completedChunks = {0};
        final boolean[] hasError = {false};
        
        for (int i = 0; i < warehouseIds.size(); i += MAX_WAREHOUSES_PER_REQUEST) {
            int endIndex = Math.min(i + MAX_WAREHOUSES_PER_REQUEST, warehouseIds.size());
            List<String> chunk = warehouseIds.subList(i, endIndex);
            String warehousesParam = String.join(",", chunk);
            
            int chunkNumber = (i / MAX_WAREHOUSES_PER_REQUEST) + 1;
            RemoteLogger.info(LOG_TAG, "APIRequest",
                    "Запрос части " + chunkNumber + " из " + totalChunks + 
                    " (складов: " + chunk.size() + ")");
            
            apiService.getStockReport(warehousesParam).enqueue(new Callback<List<StockReport>>() {
                @Override
                public void onResponse(Call<List<StockReport>> call, Response<List<StockReport>> response) {
                    completedChunks[0]++;
                    
                    if (response.isSuccessful() && response.body() != null) {
                        allReports.addAll(response.body());
                        RemoteLogger.info(LOG_TAG, "APISuccess",
                                "Часть " + chunkNumber + " получена. Отчетов: " + response.body().size());
                    } else {
                        hasError[0] = true;
                        String errorMessage = "Ошибка при загрузке части " + chunkNumber + ": " + response.code();
                        RemoteLogger.error(LOG_TAG, "APIError",
                                "Неудачный ответ сервера для части " + chunkNumber, null);
                        Log.e("StockReport", "Error response for chunk " + chunkNumber + ": " + response.code());
                    }
                    
                    // Когда все части загружены
                    if (completedChunks[0] == totalChunks) {
                        showLoading(false);
                        if (hasError[0]) {
                            Toast.makeText(StockReportActivity.this,
                                    "Ошибка при загрузке некоторых данных", Toast.LENGTH_LONG).show();
                        }
                        if (!allReports.isEmpty()) {
                            RemoteLogger.info(LOG_TAG, "APISuccess",
                                    "Все части получены. Всего отчетов: " + allReports.size());
                            processStockData(allReports);
                        } else {
                            Toast.makeText(StockReportActivity.this,
                                    "Не удалось загрузить данные", Toast.LENGTH_LONG).show();
                        }
                    }
                }

                @Override
                public void onFailure(Call<List<StockReport>> call, Throwable t) {
                    completedChunks[0]++;
                    hasError[0] = true;
                    RemoteLogger.error(LOG_TAG, "APINetworkFailure",
                            "Сетевая ошибка при загрузке части " + chunkNumber + ": " + t.getMessage(), t);
                    Log.e("StockReport", "Network error for chunk " + chunkNumber, t);
                    
                    // Когда все части обработаны
                    if (completedChunks[0] == totalChunks) {
                        showLoading(false);
                        if (!allReports.isEmpty()) {
                            Toast.makeText(StockReportActivity.this,
                                    "Частично загружено. Некоторые данные недоступны.", Toast.LENGTH_LONG).show();
                            processStockData(allReports);
                        } else {
                            Toast.makeText(StockReportActivity.this,
                                    "Ошибка загрузки: " + t.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                }
            });
        }
    }
    
    /**
     * Выполнение одного запроса остатков
     */
    private void makeStockReportRequest(String warehousesParam) {
        apiService.getStockReport(warehousesParam).enqueue(new Callback<List<StockReport>>() {
            @Override
            public void onResponse(Call<List<StockReport>> call, Response<List<StockReport>> response) {
                showLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    RemoteLogger.info(LOG_TAG, "APISuccess",
                            "Данные получены успешно. Количество отчетов (складов): " + response.body().size());
                    processStockData(response.body());
                } else {
                    // Подробная информация об ошибке
                    String errorMessage = "Ошибка сервера: " + response.code();
                    try {
                        if (response.errorBody() != null) {
                            String errorBody = response.errorBody().string();
                            errorMessage += " - " + errorBody;
                            RemoteLogger.error(LOG_TAG, "APIError",
                                    "Неудачный ответ сервера. " + errorMessage, null);
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
                RemoteLogger.error(LOG_TAG, "APINetworkFailure",
                        "Сетевая ошибка при загрузке отчета: " + t.getMessage(), null);
                Log.e("StockReport", "Network error", t);
                Toast.makeText(StockReportActivity.this,
                        "Ошибка загрузки: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void processStockData(List<StockReport> stockReports) {
        RemoteLogger.info(LOG_TAG, "DataProcessing",
                "Начало обработки данных для отображения. Входящих объектов: " + stockReports.size());
        flatList.clear();

        int totalNomenclature = 0;
        int totalSeries = 0;

        for (StockReport report : stockReports) {
            // Создаем элемент склада
            StockItem warehouseItem = StockItem.warehouse(report.warehouseId, report.warehouseName);

            // Добавляем номенклатуры как детей склада
            if (report.items != null) {
                totalNomenclature += report.items.size();
                for (StockItem item : report.items) {
                    // Создаем номенклатуру с сериями
                    StockItem nomenclatureItem = StockItem.nomenclature(
                            item.nomenclatureId,
                            item.nomenclatureName,
                            item.totalQuantity,
                            item.reserveQuantity,
                            item.freeQuantity,
                            item.inTransitQuantity,
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

        RemoteLogger.info(LOG_TAG, "DataProcessing",
                "Обработка завершена. Сформировано складов: " + flatList.size() +
                        ", Номенклатур: " + totalNomenclature + ", Серий: " + totalSeries);

        adapter.notifyDataSetChanged();

        if (flatList.isEmpty()) {
            RemoteLogger.warn(LOG_TAG, "DataProcessing",
                    "Результирующий список пуст.Нет данных по остаткам");
            Toast.makeText(this, "Нет данных по остаткам", Toast.LENGTH_SHORT).show();
        }
    }






    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        recycler.setVisibility(show ? View.GONE : View.VISIBLE);
    }
}
