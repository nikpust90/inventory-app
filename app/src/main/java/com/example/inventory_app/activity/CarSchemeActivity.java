package com.example.inventory_app.activity;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.inventory_app.BitrixManager;
import com.example.inventory_app.R;
import com.example.inventory_app.RemoteLogger;
import com.example.inventory_app.CarApiClient;
import com.example.inventory_app.models.CarResponse;
import com.example.inventory_app.adapters.SimpleTextAdapter; // Адаптер создадим ниже

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import android.view.inputmethod.EditorInfo;

public class CarSchemeActivity extends AppCompatActivity {

    private EditText etSearch;
    private Button btnSearch;
    private TextView tvTitle, tvBreadcrumbs;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private LinearLayout layoutFinalStep;
    private TextView tvFinalLink;
    private Button btnBack;

    private Button btnOpenLink;
    private Button btnSendBitrix;

    private Button btnStarline;
    private Button btnSoldAir;

    // Состояние выбора
    private String selectedCategory = null;
    private String selectedBrand = null;
    private String selectedModel = null;

    // Текущий шаг (0=Cat, 1=Brand, 2=Model, 3=Gen, 4=Result)
    private int currentStep = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_car_scheme); // Создадим этот layout ниже

        initViews();
        setupSearch(); // <--- Настройка поиска
        setupExtraButtons();
        loadCategories(); // Стартуем с загрузки категорий
    }

    private void initViews() {
        tvTitle = findViewById(R.id.tvTitle);
        tvBreadcrumbs = findViewById(R.id.tvBreadcrumbs);
        recyclerView = findViewById(R.id.recyclerMenu);
        progressBar = findViewById(R.id.progressBar);
        layoutFinalStep = findViewById(R.id.layoutFinalStep);
        tvFinalLink = findViewById(R.id.tvFinalLink);
        btnBack = findViewById(R.id.btnBack);
        // Инициализируем кнопку
        etSearch = findViewById(R.id.etSearch);
        btnSearch = findViewById(R.id.btnSearch);
        btnOpenLink = findViewById(R.id.btnOpenLink);
        btnSendBitrix = findViewById(R.id.btnSendBitrix);
        // --- НОВЫЕ КНОПКИ ---
        btnStarline = findViewById(R.id.btnStarline);
        btnSoldAir = findViewById(R.id.btnSoldAir);

        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));

        btnBack.setOnClickListener(v -> handleBackPress());
    }

    // --- ЛОГИКА НОВЫХ КНОПОК ---
    private void setupExtraButtons() {
        // 1. Кнопка Starline - просто открываем ссылку
        btnStarline.setOnClickListener(v -> {
            try {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://install.starline.ru"));
                startActivity(browserIntent);
            } catch (Exception e) {
                Toast.makeText(this, "Не удалось открыть сайт", Toast.LENGTH_SHORT).show();
            }
        });

        // 2. Кнопка "Продал воздух" - цепочка подтверждений
        btnSoldAir.setOnClickListener(v -> {
            showAirConfirmationDialog();
        });
    }

    // Шаг 1: Диалог "Точно? Ты согласовал?"
    private void showAirConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Подтверждение");
        builder.setMessage("точно? ты согласовал?");

        // Кнопка "обещаю" ведет к вводу номера заказа
        builder.setPositiveButton("обещаю", (dialog, which) -> {
            showBitrixDialogForAir();
        });

        builder.setNegativeButton("Нет", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    // Шаг 2: Диалог ввода номера для "Воздуха"
    private void showBitrixDialogForAir() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Отправка (Схемы нет)");

        // Используем тот же макет dialog_bitrix_simple
        View view = getLayoutInflater().inflate(R.layout.dialog_bitrix_simple, null);
        final EditText inputOrder = view.findViewById(R.id.etOrderNumber);
        final CheckBox cbService = view.findViewById(R.id.cbServiceOrder);
        inputOrder.requestFocus();

        builder.setView(view);

        builder.setPositiveButton("Отправить", (dialog, which) -> {
            String inputId = inputOrder.getText().toString().trim();
            boolean isService = cbService.isChecked();

            if (!inputId.isEmpty()) {
                // Формируем СПЕЦИАЛЬНЫЙ текст
                String message = "Запрос схемы из базы CAN-LOG выполнен.\n" +
                        "CAN схема расшифровки не найдена.\n" +
                        "Будет использоваться универсальный файл расшифровки,только через согласование с лидером МПП или ТО.\n" +
                        "Точка подключения не найдена ,ориентироваться на модель старее либо на сайт https://install.starline.ru";

                // Отправляем
                BitrixManager manager = new BitrixManager(this);
                manager.sendDirectMessage(inputId, message, isService);
            } else {
                Toast.makeText(this, "Введите номер (ID)", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Отмена", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    // --- ЛОГИКА ЗАГРУЗКИ ДАННЫХ ---
    private void setupSearch() {
        // Обработка нажатия кнопки "Найти"
        btnSearch.setOnClickListener(v -> performSearch());

        // Обработка нажатия "Enter" (Лупы) на клавиатуре
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch();
                return true;
            }
            return false;
        });
    }

    private void performSearch() {
        String query = etSearch.getText().toString().trim();
        if (query.length() < 2) {
            Toast.makeText(this, "Введите минимум 2 буквы", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading("Поиск...");
        // Скрываем хлебные крошки при поиске, чтобы не путать
        tvBreadcrumbs.setText("Результаты поиска: " + query);

        CarApiClient.getService().searchCars(query).enqueue(new Callback<CarResponse>() {
            @Override
            public void onResponse(Call<CarResponse> call, Response<CarResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<CarResponse.SearchResult> results = response.body().results;

                    if (results == null || results.isEmpty()) {
                        Toast.makeText(CarSchemeActivity.this, "Ничего не найдено", Toast.LENGTH_SHORT).show();
                        loadCategories(); // Возврат в начало
                        return;
                    }

                    // 1. Подготавливаем список названий для адаптера
                    List<String> displayNames = new ArrayList<>();
                    for (CarResponse.SearchResult res : results) {
                        displayNames.add(res.display_name);
                    }

                    // 2. Показываем список
                    showList(displayNames, clickedName -> {
                        // 3. Когда нажали на результат - ищем оригинальный объект
                        for (CarResponse.SearchResult res : results) {
                            if (res.display_name.equals(clickedName)) {
                                applySearchResult(res);
                                break;
                            }
                        }
                    });
                } else showError();
            }

            @Override
            public void onFailure(Call<CarResponse> call, Throwable t) {
                showError();
            }
        });
    }

    // Метод для перехода сразу к поколениям выбранной машины
    private void applySearchResult(CarResponse.SearchResult res) {
        // Устанавливаем все переменные состояния сразу
        selectedCategory = res.category;
        selectedBrand = res.brand;
        selectedModel = res.model;

        // Очищаем строку поиска
        etSearch.setText("");

        // Переходим сразу на шаг 3 (загрузка поколений)
        loadGenerations();
    }

    private void loadCategories() {
        showLoading("Выберите раздел");
        currentStep = 0;
        updateBreadcrumbs();

        CarApiClient.getService().getCategories().enqueue(new Callback<CarResponse>() {
            @Override
            public void onResponse(Call<CarResponse> call, Response<CarResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    showList(response.body().categories, item -> {
                        selectedCategory = item;
                        loadBrands();
                    });
                } else showError();
            }
            @Override
            public void onFailure(Call<CarResponse> call, Throwable t) { showError(); }
        });
    }

    private void loadBrands() {
        showLoading("Выберите марку");
        currentStep = 1;
        updateBreadcrumbs();

        CarApiClient.getService().getBrands(selectedCategory).enqueue(new Callback<CarResponse>() {
            @Override
            public void onResponse(Call<CarResponse> call, Response<CarResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    showList(response.body().brands, item -> {
                        selectedBrand = item;
                        loadModels();
                    });
                } else showError();
            }
            @Override
            public void onFailure(Call<CarResponse> call, Throwable t) { showError(); }
        });
    }

    private void loadModels() {
        showLoading("Выберите модель");
        currentStep = 2;
        updateBreadcrumbs();

        CarApiClient.getService().getModels(selectedCategory, selectedBrand).enqueue(new Callback<CarResponse>() {
            @Override
            public void onResponse(Call<CarResponse> call, Response<CarResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    showList(response.body().models, item -> {
                        selectedModel = item;
                        loadGenerations();
                    });
                } else showError();
            }
            @Override
            public void onFailure(Call<CarResponse> call, Throwable t) { showError(); }
        });
    }

    private void loadGenerations() {
        showLoading("Выберите поколение");
        currentStep = 3;
        updateBreadcrumbs();

        CarApiClient.getService().getGenerations(selectedCategory, selectedBrand, selectedModel)
                .enqueue(new Callback<CarResponse>() {
                    @Override
                    public void onResponse(Call<CarResponse> call, Response<CarResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            List<CarResponse.GenerationItem> genItems = response.body().generations;
                            List<String> names = new ArrayList<>();

                            for(CarResponse.GenerationItem g : genItems) {
                                names.add(g.name);
                            }

                            showList(names, name -> {
                                // Ищем выбранный объект, чтобы достать ссылку и инфо
                                for(CarResponse.GenerationItem g : genItems) {
                                    if(g.name.equals(name)) {
                                        showFinalResult(g);
                                        break;
                                    }
                                }
                            });
                        } else showError();
                    }
                    @Override
                    public void onFailure(Call<CarResponse> call, Throwable t) { showError(); }
                });
    }

    // --- UI HELPER METHODS ---

    private void showList(List<String> items, OnItemClick listener) {
        progressBar.setVisibility(View.GONE);
        layoutFinalStep.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);

        if (items == null || items.isEmpty()) {
            Toast.makeText(this, "Список пуст", Toast.LENGTH_SHORT).show();
            return;
        }

        SimpleTextAdapter adapter = new SimpleTextAdapter(items, listener);
        recyclerView.setAdapter(adapter);
    }

    // ПОЛНОСТЬЮ ОБНОВЛЕННЫЙ МЕТОД showFinalResult
    private void showFinalResult(CarResponse.GenerationItem item) {
        currentStep = 4;
        updateBreadcrumbs();

        recyclerView.setVisibility(View.GONE);
        layoutFinalStep.setVisibility(View.VISIBLE);

        // 1. Заголовок
        tvTitle.setText(selectedBrand + " " + selectedModel + " " + item.name);

        // 2. Текст с информацией
        String infoText = "";
        if (item.info != null && !item.info.isEmpty() && !item.info.equals("None")) {
            infoText += "Инфо: " + item.info;
        }
        tvFinalLink.setText(infoText.isEmpty() ? "Дополнительной информации нет" : infoText);

        // 3. Логика КНОПКИ ССЫЛКИ
        if (item.link != null && (item.link.startsWith("http://") || item.link.startsWith("https://"))) {
            btnOpenLink.setVisibility(View.VISIBLE);

            btnOpenLink.setOnClickListener(v -> {
                try {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(item.link));
                    startActivity(browserIntent);
                    RemoteLogger.info("CarScheme", "OpenLink", "Открыта ссылка: " + item.link);
                } catch (Exception e) {
                    Toast.makeText(CarSchemeActivity.this, "Не удалось открыть ссылку", Toast.LENGTH_SHORT).show();
                    RemoteLogger.error("CarScheme", "OpenLinkError", "Ошибка открытия ссылки", e);
                }
            });
        } else {
            // Если ссылки нет или она кривая - скрываем кнопку
            btnOpenLink.setVisibility(View.GONE);
        }

        // 4. Логика КНОПКИ БИТРИКС (ВСЕГДА ПОКАЗЫВАЕМ, если дошли до финала)
        btnSendBitrix.setVisibility(View.VISIBLE);
        btnSendBitrix.setOnClickListener(v -> {
            showBitrixDialog(item);
        });
    }

    // --- ДИАЛОГ ВВОДА НОМЕРА ЗАКАЗА (УПРОЩЕННЫЙ) ---
    private void showBitrixDialog(CarResponse.GenerationItem item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Отправить схему в Битрикс");

        // 1. Используем НОВЫЙ упрощенный макет
        View view = getLayoutInflater().inflate(R.layout.dialog_bitrix_simple, null);

        // 2. Инициализируем поля из нового макета
        final EditText inputOrder = view.findViewById(R.id.etOrderNumber);
        final CheckBox cbService = view.findViewById(R.id.cbServiceOrder);

        // Сразу ставим фокус на ввод номера и открываем клавиатуру (для удобства)
        inputOrder.requestFocus();

        builder.setView(view);

        builder.setPositiveButton("Отправить", (dialog, which) -> {
            String inputId = inputOrder.getText().toString().trim(); // Теперь это ID документа
            boolean isService = cbService.isChecked();

            if (!inputId.isEmpty()) {
                String message = buildSchemeMessage(item);

                BitrixManager manager = new BitrixManager(this);

                // ИСПОЛЬЗУЕМ НОВЫЙ МЕТОД
                // manager.sendCustomMessage(...) <-- Старый поиск убираем
                manager.sendDirectMessage(inputId, message, isService);

            } else {
                Toast.makeText(this, "Введите номер (ID)", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Отмена", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    // --- ФОРМИРОВАНИЕ ТЕКСТА ---
    private String buildSchemeMessage(CarResponse.GenerationItem item) {
        StringBuilder sb = new StringBuilder();

        // Строка 1
        sb.append("Запрос схемы из базы CAN-LOG выполнен.\n");

        // Строка 2: CAN-(категория)
        sb.append("CAN-").append(selectedCategory).append("\n");

        // Строка 3: Объект
        String fullDescription = selectedBrand + " " + selectedModel + " " + item.name;
        if (item.info != null && !item.info.isEmpty() && !item.info.equals("None")) {
            fullDescription += " " + item.info;
        }
        sb.append("Объект: ").append(fullDescription).append("\n");

        // Строка 4: Статус
        sb.append("Статус: Успешно\n\n");

        // Строка 5: Ссылка
        if (item.link != null && !item.link.isEmpty()) {
            sb.append("Прикрепленные ресурсы:\n");
            sb.append("• Схема: ").append(item.link);
        } else {
            sb.append("Прикрепленные ресурсы: отсутствуют");
        }

        return sb.toString();
    }

    private void showLoading(String title) {
        tvTitle.setText(title);
        recyclerView.setVisibility(View.GONE);
        layoutFinalStep.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
    }

    private void showError() {
        progressBar.setVisibility(View.GONE);
        Toast.makeText(this, "Ошибка загрузки данных", Toast.LENGTH_SHORT).show();
        RemoteLogger.error("CarScheme", "ApiError", "Ошибка запроса", null);
    }

    private void updateBreadcrumbs() {
        StringBuilder sb = new StringBuilder();
        if (selectedCategory != null) sb.append(selectedCategory);
        if (selectedBrand != null) sb.append(" > ").append(selectedBrand);
        if (selectedModel != null) sb.append(" > ").append(selectedModel);
        tvBreadcrumbs.setText(sb.toString());
    }

    private void handleBackPress() {
        if (currentStep <= 0) {
            finish(); // Закрыть активность
        } else if (currentStep == 1) {
            selectedCategory = null;
            loadCategories();
        } else if (currentStep == 2) {
            selectedBrand = null;
            loadBrands();
        } else if (currentStep == 3) {
            selectedModel = null;
            loadModels();
        } else if (currentStep == 4) {
            loadGenerations(); // Вернуться к выбору поколения
        }
    }

    // Интерфейс для кликов
    public interface OnItemClick {
        void onClick(String item);
    }
}
