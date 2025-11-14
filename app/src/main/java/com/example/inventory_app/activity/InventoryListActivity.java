package com.example.inventory_app.activity;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.inventory_app.*;
import com.example.inventory_app.models.InventoryDocument;
import com.example.inventory_app.adapters.InventoryDocumentsAdapter;
import com.example.inventory_app.databinding.ActivityDocumentListBinding; // Создайте binding для layout списка
import com.example.inventory_app.models.UploadPhotoInfoRequest;
import org.json.JSONException;
import org.json.JSONObject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

public class InventoryListActivity extends AppCompatActivity {

    private ActivityDocumentListBinding binding; // ViewBinding для доступа к элементам layout
    private ApiService apiService; // Клиент для работы с API
    private InventoryDocumentsAdapter adapter; // Адаптер для списка документов
    //private String warehouseId; // Поле для хранения ID склада

    private List<String> warehouseIds; // Теперь это список
    private boolean isAdmin; // Будем хранить статус админа

    // ✅ НОВЫЕ ПОЛЯ: Лаунчеры для выбора фото и запроса разрешений
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ActivityResultLauncher<String> requestPermissionLauncher;

    private static final int PICK_IMAGES_REQUEST = 1001;
    private static final int YANDEX_OAUTH_REQUEST = 1002;

    private Uri[] selectedPhotoUris;
    private String currentDocumentId;
    private ProgressDialog uploadProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Инициализация ViewBinding
        binding = ActivityDocumentListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());


        // ★ НОВЫЙ ВЫЗОВ: При первом запуске сохранит токен,
        // в дальнейшем просто проверит его наличие.
        YandexTokenManager.saveInitialTokenIfMissing(this);
        // ✅ Инициализируем лаунчеры
        //registerActivityLaunchers();

        // Инициализация Яндекс.Диска
        initializeYandexDisk();

        // ✅ Получаем ID склада из SharedPreferences
        loadUserData();

        // Создание экземпляра API сервиса через клиент
        apiService = ApiClient.getRetrofitInstance().create(ApiService.class);

        // Настройка RecyclerView и загрузка данных
        setupRecyclerView();
        loadDocuments();


        // ✅ НАЧАЛО ИЗМЕНЕНИЙ: Добавляем обработчик для новой кнопки
        binding.btnScanAllDocuments.setOnClickListener(v -> {
            openMultiDocumentScan();
        });


        binding.btnAttachPhotos.setOnClickListener(v -> checkYandexAuthAndPickPhotos());
    }

    /**
     * ✅ НОВЫЙ МЕТОД
     * Собирает ID всех документов из адаптера и запускает новую активность
     * для сканирования по всем ним.
     */
    private void openMultiDocumentScan() {
        if (adapter == null || adapter.getDocuments() == null || adapter.getDocuments().isEmpty()) {
            Toast.makeText(this, "Список документов пуст", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1. Собираем все ID документов из адаптера
        ArrayList<String> documentIds = new ArrayList<>();
        for (InventoryDocument doc : adapter.getDocuments()) {
            documentIds.add(doc.getId()); // Убедитесь, что у InventoryDocument есть метод getId()
        }

        // 2. Создаем Intent для новой активности
        Intent intent = new Intent(this, MultiDocumentScanActivity.class); // Назовем ее так

        // 3. Передаем список ID
        intent.putStringArrayListExtra("DOCUMENT_IDS_LIST", documentIds);

        // 4. Запускаем
        startActivity(intent);
    }

    private void loadUserData() {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
//        warehouseId = prefs.getString("WAREHOUSE_ID", null);
//        String userName = prefs.getString("USER_NAME", "Пользователь");
        // Загружаем Set<String> и конвертируем его обратно в List<String>
        Set<String> warehouseSet = prefs.getStringSet("WAREHOUSE_ID_LIST", new HashSet<>());
        warehouseIds = new ArrayList<>(warehouseSet);

        isAdmin = prefs.getBoolean("IS_ADMIN", false);
        String userName = prefs.getString("USER_NAME", "Пользователь");
        // --- КОНЕЦ ИЗМЕНЕНИЯ ---

        // Можно отобразить имя пользователя в заголовке
        setTitle("Документы (" + userName + ")");

        // Проверяем, есть ли у пользователя склады
        if (warehouseIds.isEmpty() && !isAdmin) {
            Toast.makeText(this, "Ошибка: Склады не назначены. Пожалуйста, обратитесь к администратору.", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        } else if (warehouseIds.isEmpty()) {
            Toast.makeText(this, "Вы вошли как администратор, но склады не назначены.", Toast.LENGTH_LONG).show();
        }
//        if (warehouseId == null) {
//            // Если данных нет, что-то пошло не так. Возвращаемся на экран входа.
//            Toast.makeText(this, "Ошибка авторизации. Пожалуйста, войдите снова.", Toast.LENGTH_LONG).show();
//            startActivity(new Intent(this, LoginActivity.class));
//            finish();
//        }
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

        if (warehouseIds.isEmpty()) {
            Toast.makeText(this, "Склады для загрузки не найдены.", Toast.LENGTH_SHORT).show();
            return;
        }

        // ✅ Преобразуем список в строку через запятую
        String warehousesParam = String.join(",", warehouseIds);
        // Выполняем асинхронный запрос к API
        apiService.getInventoryDocuments(warehousesParam, "ВРаботе").enqueue(new Callback<List<InventoryDocument>>() {
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

    private void initializeYandexDisk() {
        String token = AppConfig.getYandexDiskToken(this);
        if (token != null && !token.isEmpty()) {
            YandexDiskClient.setAuthToken(token);
        }
    }

    private void checkYandexAuthAndPickPhotos() {

        openPhotoPicker();
    }



    private void openPhotoPicker() {


        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(Intent.createChooser(intent, "Выберите фото"), PICK_IMAGES_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGES_REQUEST && resultCode == RESULT_OK) {
            handleSelectedPhotos(data);
        }
    }

    private void handleSelectedPhotos(Intent data) {
        if (data != null) {
            if (data.getClipData() != null) {
                int count = data.getClipData().getItemCount();
                selectedPhotoUris = new Uri[count];
                for (int i = 0; i < count; i++) {
                    selectedPhotoUris[i] = data.getClipData().getItemAt(i).getUri();
                }
            } else if (data.getData() != null) {
                selectedPhotoUris = new Uri[]{data.getData()};
            }

            if (selectedPhotoUris != null && selectedPhotoUris.length > 0) {
                uploadPhotosToYandexDisk();
            }
        }
    }

    private void uploadPhotosToYandexDisk() {
        uploadProgressDialog = new ProgressDialog(this);
        uploadProgressDialog.setMessage("Загрузка фото на Яндекс.Диск...");
        uploadProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        uploadProgressDialog.setMax(100);
        uploadProgressDialog.setCancelable(false);
        uploadProgressDialog.show();

        // --- НАЧАЛО ИЗМЕНЕНИЙ (Сбор данных для YandexDiskHelper) ---

        // 1. Получаем имя пользователя из SharedPreferences
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        String userName = prefs.getString("USER_NAME", "Неизвестный");

        // 2. Получаем ПОЛНЫЙ список документов из адаптера
        List<InventoryDocument> documentsList;
        if (adapter != null && adapter.getDocuments() != null) {
            documentsList = adapter.getDocuments();
        } else {
            documentsList = new ArrayList<>(); // Отправляем пустой список, если адаптер пуст
        }

        YandexDiskHelper.uploadPhotosToYandexDisk(this, selectedPhotoUris, userName, documentsList,
                new YandexDiskHelper.UploadCallback() {
                    @Override
                    public void onSuccess(String folderUrl) {
                        uploadProgressDialog.dismiss();
                        Toast.makeText(InventoryListActivity.this,
                                "Фото успешно загружены!\nСсылка: " + folderUrl, Toast.LENGTH_LONG).show();

                        // Отправляем ссылку в 1С
                        // --- НАЧАЛО ИЗМЕНЕНИЙ ---

                        // 1. Собираем все ID документов из адаптера
                        ArrayList<String> documentIds = new ArrayList<>();
                        for (InventoryDocument doc : documentsList) { // Используем список, что уже получили
                            documentIds.add(doc.getId());
                        }

                        // 2. Получаем имя пользователя из SharedPreferences
                        //SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                        //String userName = prefs.getString("USER_NAME", "Неизвестный"); // Используем "Неизвестный" как запасной вариант

                        // 3. Отправляем ссылку, список ID и пользователя в 1С
                        sendLinkTo1C(folderUrl, documentIds, userName);
                        // --- КОНЕЦ ИЗМЕНЕНИЙ ---
                        //sendLinkTo1C(folderUrl);
                    }

                    @Override
                    public void onError(String error) {
                        uploadProgressDialog.dismiss();
                        Toast.makeText(InventoryListActivity.this,
                                "Ошибка загрузки: " + error, Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onProgress(int progress) {
                        uploadProgressDialog.setProgress(progress);
                    }
                });
    }

    private void sendLinkTo1C(String folderUrl, List<String> documentIds, String userName) {

        // 1. Получаем текущее время
        String uploadTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

        // 2. Создаем объект-тело запроса (вместо JSONObject)
        UploadPhotoInfoRequest requestBody = new UploadPhotoInfoRequest(userName, folderUrl, documentIds, uploadTime);

        Log.d("ApiService", "Отправка данных в 1С: " + folderUrl + " от " + userName);

        // 3. Вызываем новый метод вашего apiService
        apiService.sendPhotoInfo(requestBody).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    // Успех (сервер 1С вернул код 2xx)
                    Toast.makeText(InventoryListActivity.this, "Ссылка успешно отправлена в 1С", Toast.LENGTH_LONG).show();
                    Log.d("ApiService", "Отправка в 1С: Успех");
                } else {
                    // Ошибка (сервер 1С вернул 4xx, 5xx)
                    String errorMsg = "Ошибка 1С: " + response.code();
                    try {
                        // Пытаемся прочитать тело ошибки, если 1С его прислала
                        if (response.errorBody() != null) {
                            errorMsg += " - " + response.errorBody().string();
                        }
                    } catch (IOException e) {
                        Log.e("ApiService", "Ошибка чтения errorBody", e);
                    }
                    Toast.makeText(InventoryListActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                    Log.e("ApiService", "Отправка в 1С: " + errorMsg);
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                // Ошибка сети (нет интернета, таймаут, неверный адрес)
                Toast.makeText(InventoryListActivity.this, "Ошибка сети: " + t.getMessage(), Toast.LENGTH_LONG).show();
                Log.e("ApiService", "Отправка в 1С: Сбой сети", t);
            }
        });
    }






    // Обновите метод для сохранения currentDocumentId
//    private void openDocument(String documentId) {
//        this.currentDocumentId = documentId;
//        Toast.makeText(this, "Документ выбран: " + documentId, Toast.LENGTH_SHORT).show();
//
//        // Можно сразу открыть детали или просто выбрать документ для фото
//        Intent intent = new Intent(this, InventoryActivity.class);
//        intent.putExtra("DOCUMENT_ID", documentId);
//        startActivity(intent);
//    }

//    /**
//     * ✅ НОВЫЙ МЕТОД
//     * Инициализирует ActivityResultLauncher'ы
//     */
//    private void registerActivityLaunchers() {
//        // Лаунчер для запроса разрешения
//        requestPermissionLauncher = registerForActivityResult(
//                new ActivityResultContracts.RequestPermission(),
//                isGranted -> {
//                    if (isGranted) {
//                        launchPickerIntent(); // Если разрешение дали, запускаем выбор
//                    } else {
//                        Toast.makeText(this, "Разрешение на чтение галереи не дано", Toast.LENGTH_SHORT).show();
//                    }
//                });
//
//        // Лаунчер для выбора фото
//        imagePickerLauncher = registerForActivityResult(
//                new ActivityResultContracts.StartActivityForResult(),
//                result -> {
//                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
//                        handleSelectedImages(result.getData());
//                    }
//                });
//    }
//
//    /**
//     * ✅ НОВЫЙ МЕТОД
//     * Проверяет разрешения и запускает лаунчер
//     */
//    private void openImagePicker() {
//        String permission;
//        // Проверяем версию Android для правильного разрешения
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//            permission = Manifest.permission.READ_MEDIA_IMAGES;
//        } else {
//            permission = Manifest.permission.READ_EXTERNAL_STORAGE;
//        }
//
//        // Проверяем, есть ли у нас уже разрешение
//        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
//            launchPickerIntent();
//        } else {
//            // Если нет, запрашиваем его
//            requestPermissionLauncher.launch(permission);
//        }
//    }
//
//    /**
//     * ✅ НОВЫЙ МЕТОД
//     * Создает и запускает Intent для выбора фото
//     */
//    private void launchPickerIntent() {
//        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
//        intent.setType("image/*");
//        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true); // Разрешаем выбор нескольких фото
//        imagePickerLauncher.launch(Intent.createChooser(intent, "Выберите фото"));
//    }
//
//    /**
//     * ✅ НОВЫЙ МЕТОД
//     * Обрабатывает результат выбора фото
//     */
//    private void handleSelectedImages(Intent data) {
//        List<Uri> imageUris = new ArrayList<>();
//        if (data.getClipData() != null) {
//            // Выбрано несколько фото
//            ClipData clipData = data.getClipData();
//            for (int i = 0; i < clipData.getItemCount(); i++) {
//                imageUris.add(clipData.getItemAt(i).getUri());
//            }
//        } else if (data.getData() != null) {
//            // Выбрано одно фото
//            imageUris.add(data.getData());
//        }
//
//        if (!imageUris.isEmpty()) {
//            Toast.makeText(this, "Выбрано " + imageUris.size() + " фото. Начинаем загрузку...", Toast.LENGTH_LONG).show();
//
//            // ⚠️ ВАЖНО: Запускаем сложный процесс загрузки в облако
//            uploadImagesToMailRuCloud(imageUris);
//        }
//    }
//
//    /**
//     * ✅ НОВЫЙ МЕТОД (ЗАГЛУШКА)
//     * Здесь должна быть ОЧЕНЬ сложная логика загрузки в Облако Mail.ru.
//     * См. Раздел 2 этого ответа.
//     */
//    private void uploadImagesToMailRuCloud(List<Uri> uris) {
//        //
//        // ЭТО ПРОСТО ПРИМЕР! РЕАЛИЗАЦИЯ БУДЕТ СЛОЖНОЙ!
//        //
//        Log.d("Upload", "Начало загрузки " + uris.size() + " файлов.");
//
//        // ---------------------------------------------------------------
//        // ⚠️ Здесь должна быть ваша логика работы с API Mail.ru
//        // 1. (Если нужно) Получить/обновить OAuth 2.0 токен
//        // 2. Создать НОВЫЙ ApiClient для Mail.ru (с другим BaseUrl и Interceptor'ом)
//        // 3. Вызвать метод API Mail.ru для создания папки (напр. /api/v2/folder/add)
//        // 4. Получить имя новой папки (напр. "Фотоотчет 2025-11-12 09:00")
//        // 5. Для КАЖДОГО URI из списка:
//        //    a. Преобразовать Uri в File или InputStream
//        //    b. Вызвать метод API Mail.ru для получения URL для загрузки (напр. /api/v2/file/upload/url)
//        //    c. Загрузить файл (PUT или POST) по полученному URL
//        // 6. После загрузки ВСЕХ файлов, вызвать метод API Mail.ru для получения публичной ссылки на папку (напр. /api/v2/folder/public/link)
//        // 7. Получить ссылку, например: "https://cloud.mail.ru/public/..."
//        // ---------------------------------------------------------------
//
//        // (Имитируем получение ссылки после долгой загрузки)
//        String fakePublicLink = "https://cloud.mail.ru/public/XYZ123ABC";
//
//        // После того, как ссылка на папку получена, отправляем ее в 1С
//        //sendLinkTo1C(fakePublicLink);
//    }
//
//    // ... (остальной код класса InventoryListActivity)
//
//    // ... (в конце класса добавим метод отправки в 1С)
//
//    /**
//     * ✅ НОВЫЙ МЕТОД
//     * Отправляет полученную ссылку на папку в 1С.
//     * Использует ваш СУЩЕСТВУЮЩИЙ ApiService для 1С.
//     */
////    private void sendLinkTo1C(String folderUrl) {
////        // (Предполагаем, что у вас есть модель PhotoLinkRequest, см. Шаг 3.1)
////        PhotoLinkRequest request = new PhotoLinkRequest(folderUrl);
////
////        // Используем ваш существующий apiService для 1С
////        apiService.sendPhotoLink(request).enqueue(new Callback<Void>() {
////            @Override
////            public void onResponse(Call<Void> call, Response<Void> response) {
////                if (response.isSuccessful()) {
////                    Toast.makeText(InventoryListActivity.this, "Ссылка на фото успешно отправлена в 1С", Toast.LENGTH_LONG).show();
////                } else {
////                    Toast.makeText(InventoryListActivity.this, "Ошибка отправки ссылки в 1С: " + response.code(), Toast.LENGTH_LONG).show();
////                }
////            }
////
////            @Override
////            public void onFailure(Call<Void> call, Throwable t) {
////                Toast.makeText(InventoryListActivity.this, "Сетевая ошибка 1С: " + t.getMessage(), Toast.LENGTH_LONG).show();
////            }
////        });
////    }

}


