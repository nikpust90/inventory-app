package com.example.inventory_app.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.inventory_app.ApiClient;
import com.example.inventory_app.ApiService;
import com.example.inventory_app.InventoryDocument;
import com.example.inventory_app.adapters.InventoryDocumentsAdapter;
import com.example.inventory_app.databinding.ActivityDocumentListBinding; // Создайте binding для layout списка
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class InventoryListActivity extends AppCompatActivity {

    private ActivityDocumentListBinding binding; // ViewBinding для доступа к элементам layout
    private ApiService apiService; // Клиент для работы с API
    private InventoryDocumentsAdapter adapter; // Адаптер для списка документов

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Инициализация ViewBinding
        binding = ActivityDocumentListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Создание экземпляра API сервиса через клиент
        apiService = ApiClient.getRetrofitInstance().create(ApiService.class);

        // Настройка RecyclerView и загрузка данных
        setupRecyclerView();
        loadDocuments();
    }

    /**
     * Настраивает RecyclerView для отображения списка документов
     */
    private void setupRecyclerView() {
        // Создаем адаптер с пустым списком и колбэком для обработки кликов
        adapter = new InventoryDocumentsAdapter(new ArrayList<>(), this::openDocument);
        // Устанавливаем LinearLayoutManager для вертикального списка
        binding.documentsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        // Привязываем адаптер к RecyclerView
        binding.documentsRecyclerView.setAdapter(adapter);
    }

    /**
     * Загружает список документов инвентаризации с сервера
     */
    private void loadDocuments() {
        // Выполняем асинхронный запрос к API
        apiService.getInventoryDocuments().enqueue(new Callback<List<InventoryDocument>>() {
            @Override
            public void onResponse(Call<List<InventoryDocument>> call, Response<List<InventoryDocument>> response) {
                // Проверяем успешность ответа и наличие данных
//                if (response.isSuccessful() && response.body() != null) {
//                    // Обновляем адаптер новыми данными
//                    adapter.updateDocuments(response.body());
//                } else {
//                    // Показываем сообщение об ошибке загрузки
//                    Toast.makeText(InventoryListActivity.this,
//                            "Не удалось загрузить список документов", Toast.LENGTH_SHORT).show();
//                }
                if (response.isSuccessful() && response.body() != null) {
                    adapter.updateDocuments(response.body());
                } else {
                    String errorMsg = "Не удалось загрузить: код " + response.code();
                    if (response.errorBody() != null) {
                        try {
                            errorMsg += " - " + response.errorBody().string();  // Показывает тело ошибки от сервера (e.g., "Unauthorized")
                        } catch (IOException e) {
                            errorMsg += " - Ошибка чтения тела";
                        }
                    }
                    Toast.makeText(InventoryListActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                }
            }



            @Override
            public void onFailure(Call<List<InventoryDocument>> call, Throwable t) {
                // Обрабатываем ошибки сети
                Toast.makeText(InventoryListActivity.this,
                        "Ошибка сети", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Открывает детали документа инвентаризации
     * @param documentId ID документа для открытия
     */
    private void openDocument(String documentId) {
        // Показываем сообщение о том, какой документ открываем
        Toast.makeText(this, "Открываем документ с ID: " + documentId, Toast.LENGTH_SHORT).show();

        // Создаем Intent для перехода к активности с деталями документа
        Intent intent = new Intent(this, InventoryActivity.class);
        // Передаем ID документа как параметр
        intent.putExtra("DOCUMENT_ID", documentId);
        // Запускаем активность
        startActivity(intent);
    }
}


