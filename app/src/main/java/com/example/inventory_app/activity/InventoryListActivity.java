package com.example.inventory_app.activity;

import android.content.Intent;
import android.content.SharedPreferences;
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
    private String warehouseId; // Поле для хранения ID склада

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Инициализация ViewBinding
        binding = ActivityDocumentListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // ✅ Получаем ID склада из SharedPreferences
        loadUserData();

        // Создание экземпляра API сервиса через клиент
        apiService = ApiClient.getRetrofitInstance().create(ApiService.class);

        // Настройка RecyclerView и загрузка данных
        setupRecyclerView();
        loadDocuments();
    }

    private void loadUserData() {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        warehouseId = prefs.getString("WAREHOUSE_ID", null);
        String userName = prefs.getString("USER_NAME", "Пользователь");

        // Можно отобразить имя пользователя в заголовке
        setTitle("Документы (" + userName + ")");

        if (warehouseId == null) {
            // Если данных нет, что-то пошло не так. Возвращаемся на экран входа.
            Toast.makeText(this, "Ошибка авторизации. Пожалуйста, войдите снова.", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
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
        apiService.getInventoryDocuments(warehouseId, "ВРаботе").enqueue(new Callback<List<InventoryDocument>>() {
            @Override
            public void onResponse(Call<List<InventoryDocument>> call, Response<List<InventoryDocument>> response) {
                // Проверяем, успешен ли запрос и есть ли тело ответа
                if (response.isSuccessful() && response.body() != null) {
                    // Запрос успешен, обновляем данные в адаптере
                    adapter.updateDocuments(response.body());
                } else {
                    // Запрос неуспешен (код ответа не 2xx), обрабатываем ошибку сервера
                    String errorMsg;
                    try {
                        // Пытаемся получить тело ошибки из ответа 1С
                        if (response.errorBody() != null) {
                            // Читаем текст ошибки, который передала 1С (например, "Склад не найден")
                            errorMsg = "Ошибка сервера: " + response.code() + " - " + response.errorBody().string();
                        } else {
                            // Если тела ошибки нет, показываем только код
                            errorMsg = "Ошибка сервера: Код " + response.code();
                        }
                    } catch (IOException e) {
                        // На случай, если возникла ошибка при чтении тела ответа
                        errorMsg = "Не удалось загрузить данные. Ошибка чтения тела ответа.";
                    }

                    // В зависимости от кода, можно показать более специфичное сообщение
                    if (response.code() == 400) {
                        // Например, для 400-го кода можно дать более понятное сообщение
                        errorMsg = "Ошибка запроса: " + (errorMsg.contains("Не указан идентификатор склада") ? "Не указан идентификатор склада." : "Некорректные параметры.");
                    } else if (response.code() == 404) {
                        errorMsg = "Данные не найдены: " + (errorMsg.contains("Склад с указанным ID не найден") ? "Склад не найден." : "Ресурс не найден.");
                    }

                    // Показываем пользователю всплывающее уведомление
                    Toast.makeText(InventoryListActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                }
            }



            @Override
            public void onFailure(Call<List<InventoryDocument>> call, Throwable t) {
                // Обрабатываем ошибки, не связанные с HTTP-ответом (сеть, парсинг)
                String errorMsg;
                // Проверяем, является ли ошибка проблемой сети
                if (t instanceof IOException) {
                    errorMsg = "Ошибка сети. Проверьте подключение к интернету.";
                } else {
                    // Это может быть ошибка парсинга JSON или другая системная ошибка
                    errorMsg = "Непредвиденная ошибка: " + t.getMessage();
                }

                // Показываем пользователю сообщение об ошибке
                Toast.makeText(InventoryListActivity.this, errorMsg, Toast.LENGTH_LONG).show();
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


