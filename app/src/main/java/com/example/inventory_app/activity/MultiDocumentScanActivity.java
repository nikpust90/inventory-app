package com.example.inventory_app.activity;

// ИМПОРТЫ СКОПИРОВАНЫ ИЗ ВАШЕГО INVENTORY_ACTIVITY
import android.Manifest;
import android.annotation.TargetApi;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.*;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import com.example.inventory_app.*;
import com.example.inventory_app.adapters.InventoryItemAdapter;
import com.example.inventory_app.data.*;
import com.example.inventory_app.databinding.ActivityDocumentBinding;
import com.example.inventory_app.models.InventoryDocument;
import com.example.inventory_app.models.InventoryItem;
import com.example.inventory_app.models.Seriya;
import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import com.journeyapps.barcodescanner.DefaultDecoderFactory;
import com.journeyapps.barcodescanner.camera.CameraSettings;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import androidx.appcompat.app.AlertDialog;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger; // ✅ ДОБАВЛЕН НОВЫЙ ИМПОРТ

import com.example.inventory_app.data.LocalQueueManager;

public class MultiDocumentScanActivity extends AppCompatActivity {

    private ActivityDocumentBinding binding;
    private ApiService apiService;
    private InventoryItemAdapter adapter;

    private static final String LOG_TAG = "MultiScan";
    // ❌ private InventoryDocument currentDocument; // УДАЛЕНО

    // ✅ ДОБАВЛЕНО:
    private List<InventoryDocument> loadedDocuments = new ArrayList<>();
    private List<InventoryItem> flatItemList = new ArrayList<>(); // Единый список для адаптера

    private DecoratedBarcodeView barcodeView;
    private String lastScannedBarcode = "";
    private long lastScanTime = 0;
    private static final long SCAN_DELAY_MS = 1500;
    private static final int CAMERA_PERMISSION_REQUEST = 1001;
    private boolean hasCameraPermission = false;
    private RecyclerView recyclerView;
    private LocalQueueManager queueManager;
    private static final int STORAGE_PERMISSION_CODE = 1002;


    // 🔹 Callback остается БЕЗ ИЗМЕНЕНИЙ
    private final BarcodeCallback callback = result -> {
        if (result.getText() == null) return;

        long now = System.currentTimeMillis();

        if (now - lastScanTime < SCAN_DELAY_MS) {
            return;
        }

        if (result.getText().equals(lastScannedBarcode)) {
            return;
        }

        lastScannedBarcode = result.getText();
        lastScanTime = now;
        // 📝 ЛОГ: Сырой скан
        RemoteLogger.info(LOG_TAG, "ScanEvent",
                "Считан штрихкод: " + lastScannedBarcode);

        runOnUiThread(() -> processScan(lastScannedBarcode));
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDocumentBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        RemoteLogger.info(LOG_TAG, "ActivityLifecycle",
                "Запуск активности мульти-сканирования");

        // Запрос разрешения камеры (без изменений)
        checkCameraPermission();

        // Инициализация (без изменений)
        barcodeView = binding.barcodeScanner;
        apiService = ApiClient.getRetrofitInstance().create(ApiService.class);
        queueManager = new LocalQueueManager(this, apiService);

        // Настройка WorkManager (без изменений)
        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(
                PendingUploadWorker.class,
                15, TimeUnit.MINUTES
        ).build();
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "PendingUploadWork",
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                workRequest
        );

        RemoteLogger.info(LOG_TAG, "WorkManager",
                "Запланирована фоновая задача PendingUploadWorker");

        // ⚠️ НАЧАЛО ИЗМЕНЕНИЙ: Загрузка нескольких ID
        ArrayList<String> documentIds = getIntent().getStringArrayListExtra("DOCUMENT_IDS_LIST");

        setupRecyclerView(); // Настраиваем RecyclerView (метод без изменений)

        if (documentIds != null && !documentIds.isEmpty()) {
            RemoteLogger.info(LOG_TAG, "Initialization",
                    "Получен список ID документов: " + documentIds.size());
            loadAllDocuments(documentIds); // ⚠️ Вызываем новый метод
        } else {
            RemoteLogger.error(LOG_TAG, "Initialization",
                    "ОШИБКА: Список ID документов пуст или null", null);
            Toast.makeText(this, "ID документов отсутствуют", Toast.LENGTH_SHORT).show();
            finish(); // Закрываем, если нет ID
        }
        // ⚠️ КОНЕЦ ИЗМЕНЕНИЙ

        // Обработчики кнопок (fabScan без изменений)
        binding.fabScan.setOnClickListener(v -> toggleScanner());

        // ⚠️ ИЗМЕНЕНИЕ: Кнопка "Отправить" теперь вызывает sendAllDocuments()
        binding.sendButton.setOnClickListener(v -> sendAllDocuments());

        binding.btnExportDb.setOnClickListener(v -> exportDatabase());
    }

    //
    // ----------------------------------------------------------------------------------
    // МЕТОДЫ УПРАВЛЕНИЯ КАМЕРОЙ И ЖИЗНЕННЫМ ЦИКЛОМ (БЕЗ ИЗМЕНЕНИЙ)
    // ----------------------------------------------------------------------------------
    //

    private void checkCameraPermission() {
        RemoteLogger.info(LOG_TAG, "PermissionCheck",
                "Проверка разрешений камеры");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                RemoteLogger.warn(LOG_TAG, "PermissionCheck",
                        "Разрешение на камеру отсутствует, запрашиваем");
                requestPermissions(new String[]{android.Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST);
            } else {
                RemoteLogger.info(LOG_TAG, "PermissionCheck",
                        "Разрешение на камеру уже предоставлено");
                hasCameraPermission = true;
                initializeScanner();
            }
        } else {
            hasCameraPermission = true;
            initializeScanner();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                RemoteLogger.info(LOG_TAG, "PermissionResult",
                        "Пользователь дал разрешение на камеру");
                hasCameraPermission = true;
                initializeScanner();
            } else {
                RemoteLogger.error(LOG_TAG, "PermissionResult",
                        "Пользователь ОТКЛОНИЛ разрешение на камеру",null);
                Toast.makeText(this, "Для сканирования нужны разрешения камеры", Toast.LENGTH_LONG).show();
            }
        }
        // Добавляем обработку для STORAGE_PERMISSION_CODE, если она была в onRequestPermissionsResult
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                RemoteLogger.info(LOG_TAG, "PermissionResult",
                        "Пользователь дал разрешение на хранилище для экспорта");
                performExport();
            } else {
                RemoteLogger.warn(LOG_TAG, "PermissionResult",
                        "Пользователь ОТКЛОНИЛ разрешение на хранилище");
                Toast.makeText(this, "Нужны разрешения для экспорта БД", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void initializeScanner() {
        if (hasCameraPermission) {
            RemoteLogger.info(LOG_TAG, "ScannerConfig",
                    "Инициализация сканера ZXing с поддержкой штрихкодов");
            barcodeView = binding.barcodeScanner;
            CameraSettings settings = new CameraSettings();
            settings.setFocusMode(CameraSettings.FocusMode.AUTO);
            settings.setAutoFocusEnabled(true);
            settings.setContinuousFocusEnabled(true);
            settings.setExposureEnabled(true);

            barcodeView.getBarcodeView().setCameraSettings(settings);

            Collection<BarcodeFormat> formats = Arrays.asList(
                    BarcodeFormat.CODE_128, BarcodeFormat.CODE_39, BarcodeFormat.CODE_93,
                    BarcodeFormat.CODABAR, BarcodeFormat.EAN_13, BarcodeFormat.EAN_8,
                    BarcodeFormat.UPC_A, BarcodeFormat.UPC_E, BarcodeFormat.ITF,
                    BarcodeFormat.RSS_14, BarcodeFormat.RSS_EXPANDED, BarcodeFormat.QR_CODE,
                    BarcodeFormat.DATA_MATRIX, BarcodeFormat.AZTEC, BarcodeFormat.PDF_417
            );
            barcodeView.getBarcodeView().setDecoderFactory(new DefaultDecoderFactory(formats));
            barcodeView.setStatusText("Наведите на штрихкод");
        }
    }

    private void toggleScanner() {
        if (barcodeView.getVisibility() == View.VISIBLE) {
            RemoteLogger.info(LOG_TAG, "ScannerControl",
                    "Остановка и скрытие сканера");
            barcodeView.setVisibility(View.GONE);
            barcodeView.pause();
            binding.fabScan.setImageResource(android.R.drawable.ic_menu_camera);
        } else {
            if (!hasCameraPermission) {
                checkCameraPermission();
                return;
            }
            RemoteLogger.info(LOG_TAG, "ScannerControl",
                    "Запуск и отображение сканера. Начало декодирования.");
            barcodeView.setVisibility(View.VISIBLE);
            barcodeView.resume();
            barcodeView.decodeContinuous(callback);
            binding.fabScan.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
            barcodeView.getBarcodeView().setFocusableInTouchMode(true);
            barcodeView.getBarcodeView().requestFocus();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        RemoteLogger.info(LOG_TAG, "ActivityLifecycle", "onResume: Попытка отправить отложенные данные");
        queueManager.trySendAll();
        if (barcodeView.getVisibility() == View.VISIBLE) {
            barcodeView.resume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        RemoteLogger.info(LOG_TAG, "ActivityLifecycle", "onPause");
        if (barcodeView.isActivated()) {
            barcodeView.pause();
        }
    }

    @Override
    public void onBackPressed() {
        if (barcodeView.getVisibility() == View.VISIBLE) {
            RemoteLogger.info(LOG_TAG, "UserAction", "Закрытие сканера по кнопке Назад");
            toggleScanner();
        } else {
            super.onBackPressed();
        }
    }

    //
    // ----------------------------------------------------------------------------------
    // ЛОГИКА СКАНИРОВАНИЯ И ЗАГРУЗКИ (С ИЗМЕНЕНИЯМИ)
    // ----------------------------------------------------------------------------------
    //

    // ⚠️ НАЧАЛО ИЗМЕНЕНИЙ: Модифицируем processScan
    private void processScan(String scannedBarcode) {
        RemoteLogger.info(LOG_TAG, "ScanProcess",
                "Начало обработки скана: [" + scannedBarcode + "]");

        // ⚠️ Проверяем новый список
        if (flatItemList.isEmpty() || scannedBarcode == null || scannedBarcode.trim().isEmpty()) {
            RemoteLogger.warn(LOG_TAG, "ScanProcess",
                    "Список товаров пуст. Отмена сканирования.");
            Toast.makeText(this, "Документы не готовы или штрихкод пуст", Toast.LENGTH_SHORT).show();
            return;
        }

        // Логика valueToSearch (без изменений)
        String valueToSearch;
        if (scannedBarcode.toLowerCase().startsWith("http")) {
            valueToSearch = scannedBarcode.length() >= 12 ? scannedBarcode.substring(scannedBarcode.length() - 12) : scannedBarcode;
            RemoteLogger.info(LOG_TAG, "ScanProcess",
                    "Штрихкод был URL, обрезан до: " + valueToSearch);
        } else {
            valueToSearch = scannedBarcode;
        }

        // ⚠️ Ищем в ЕДИНОМ плоском списке
        List<InventoryItem> items = this.flatItemList;
        List<Integer> matchingIndices = new ArrayList<>();

        for (int i = 0; i < items.size(); i++) {
            InventoryItem item = items.get(i);
            // Логика поиска совпадения (без изменений)
            Seriya seriya = item.getSeriya();
            if (seriya == null) continue;

            boolean isMatch = (seriya.getName() != null && valueToSearch.equals(seriya.getName())) ||
                    (seriya.getImei() != null && valueToSearch.equals(seriya.getImei()));

            if (isMatch) {
                RemoteLogger.info(LOG_TAG, "ScanMatch",
                        "Совпадение найдено. Номенклатура: " + item.getNomenklatura().getName() + ", Индекс: " + i);
                matchingIndices.add(i);
            }
        }

        // Анализ результатов поиска (без изменений)
        if (matchingIndices.isEmpty()) {
            RemoteLogger.warn(LOG_TAG, "ScanResult",
                    "Ничего не найдено для: " + valueToSearch);
            playErrorSound();
            vibrateError();
        } else if (matchingIndices.size() == 1) {
            RemoteLogger.info(LOG_TAG, "ScanResult",
                    "Найдена 1 уникальная позиция.");
            processFoundItem(matchingIndices.get(0));
        } else {
            RemoteLogger.warn(LOG_TAG, "ScanResult",
                    "Найдено " + matchingIndices.size() + " дубликатов. Требуется выбор.");
            showNomenclatureChoiceDialog(matchingIndices, valueToSearch);
        }
    }


    /**
     * 🔥 Метод processFoundItem (БЕЗ ИЗМЕНЕНИЙ)
     * Он по-прежнему работает, т.к. изменяет item по индексу в flatItemList.
     * Этот item является ссылкой на объект в одном из документов в loadedDocuments.
     */
    private void processFoundItem(int itemIndex) {
        // InventoryItem item = currentDocument.getItems().get(itemIndex);
        // ⚠️ Заменяем currentDocument.getItems() на flatItemList
        InventoryItem item = flatItemList.get(itemIndex);

        String itemName = item.getNomenklatura().getName();

        RemoteLogger.info(LOG_TAG, "ItemAction",
                "Обработка товара: " + itemName + " (Индекс: " + itemIndex + ")");

        if (item.isFound()) {
            RemoteLogger.warn(LOG_TAG, "ItemAction",
                    "Повторное сканирование товара: " + itemName);
            Toast.makeText(this, "Повторное сканирование: " + item.getNomenklatura().getName(), Toast.LENGTH_SHORT).show();
            playSuccessSound();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("✅ Найдена позиция");

        String message = "Номенклатура: " + item.getNomenklatura().getName();
        if (item.getSeriya() != null) {
            if (item.getSeriya().getName() != null && !item.getSeriya().getName().isEmpty()) {
                message += "\nСерия: " + item.getSeriya().getName();
            }
            if (item.getSeriya().getImei() != null && !item.getSeriya().getImei().isEmpty()) {
                message += "\nIMEI: " + item.getSeriya().getImei();
            }
        }
        builder.setMessage(message);

        builder.setPositiveButton("ОК", (dialog, which) -> {
            RemoteLogger.info(LOG_TAG, "ItemConfirmation",
                    "Пользователь подтвердил товар. Обновление статуса: " + itemName);
            item.setKolichestvoFakt(1);
            item.setFound(true);
            adapter.notifyItemChanged(itemIndex);

            playSuccessSound();
            vibrateSuccess();
            Toast.makeText(this, "Подтверждено: " + item.getNomenklatura().getName(), Toast.LENGTH_SHORT).show();

            recyclerView.scrollToPosition(itemIndex);

            dialog.dismiss();
        });

        builder.setNegativeButton("Отмена", (dialog, which) -> {
            RemoteLogger.info(LOG_TAG, "ItemConfirmation",
                    "Пользователь отменил подтверждение товара: " + itemName);
            dialog.dismiss();
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Метод showNomenclatureChoiceDialog (БЕЗ ИЗМЕНЕНИЙ)
     * Он также работает с flatItemList через currentDocument.getItems().
     */
    private void showNomenclatureChoiceDialog(List<Integer> indices, String scannedValue) {
        RemoteLogger.warn(LOG_TAG, "DialogShow",
                "Показан диалог выбора для " + indices.size() + " дубликатов");
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Найдено несколько позиций для '" + scannedValue + "'. Выберите нужную:");

        String[] nomenclatureNames = new String[indices.size()];
        for (int i = 0; i < indices.size(); i++) {
            // ⚠️ Заменяем currentDocument.getItems() на flatItemList
            InventoryItem item = flatItemList.get(indices.get(i));
            String name = item.getNomenklatura().getName() != null ? item.getNomenklatura().getName() : "Без имени";
            nomenclatureNames[i] = name;
        }

        builder.setItems(nomenclatureNames, (dialog, which) -> {
            int chosenItemIndex = indices.get(which);
            RemoteLogger.info(LOG_TAG, "DialogSelection",
                    "Пользователь выбрал позицию под индексом: " + chosenItemIndex);
            processFoundItem(chosenItemIndex);
            dialog.dismiss();
        });

        builder.setNegativeButton("Отмена", (dialog, id) -> dialog.dismiss());
        AlertDialog dialog = builder.create();
        dialog.show();
        playErrorSound();
        vibrateError();
    }

    /**
     * Метод setupRecyclerView (БЕЗ ИЗМЕНЕНИЙ)
     */
    private void setupRecyclerView() {
        recyclerView = binding.itemsRecyclerView;
        adapter = new InventoryItemAdapter(new ArrayList<>());
        binding.itemsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.itemsRecyclerView.setAdapter(adapter);
    }

    // ❌ СТАРЫЙ МЕТОД loadDocumentDetails(String documentId) УДАЛЕН

    /**
     * ✅ НОВЫЙ МЕТОД
     * Загружает все документы по списку ID и объединяет их товары в один список.
     */
    private void loadAllDocuments(List<String> documentIds) {
        RemoteLogger.info(LOG_TAG, "APICall",
                "Начало асинхронной загрузки " + documentIds.size() + " документов");
        // TODO: Показать индикатор загрузки (если нужно)
        // binding.progressBar.setVisibility(View.VISIBLE);

        loadedDocuments.clear();
        flatItemList.clear();

        int requestsToMake = documentIds.size();
        AtomicInteger requestsCompleted = new AtomicInteger(0);

        for (String docId : documentIds) {
            apiService.getInventoryDocumentById(docId).enqueue(new Callback<InventoryDocument>() {
                @Override
                public void onResponse(Call<InventoryDocument> call, Response<InventoryDocument> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        InventoryDocument doc = response.body();
                        loadedDocuments.add(doc); // Сохраняем весь документ
                        String warehouseName = doc.getWarehouse();
                        int itemsInDoc = (doc.getItems() != null) ? doc.getItems().size() : 0;

                        RemoteLogger.info(LOG_TAG, "APISuccess",
                                "Успешно загружен док " + docId + ". Склад: " + warehouseName + ". Позиций: " + itemsInDoc);

                        if (doc.getItems() != null) {
                            // ⚠️ ПРЕДПОЛАГАЕМ, что у документа есть имя склада
                            // (Если метод называется иначе, исправьте)
                            //String warehouseName = doc.getWarehouse();

                            // ❌ НЕ ДЕЛАЙТЕ ТАК:
                            // flatItemList.addAll(doc.getItems());

                            // ✅ ДЕЛАЙТЕ ТАК:
                            for (InventoryItem item : doc.getItems()) {
                                item.setWarehouseName(warehouseName); // Устанавливаем склад для каждого товара
                                flatItemList.add(item); // Добавляем в общий список
                            }
                            // Добавляем все товары из этого документа в ЕДИНЫЙ список
                            //flatItemList.addAll(doc.getItems());
                        }
                    } else {
                        // Ошибка загрузки одного из документов
                        RemoteLogger.error(LOG_TAG, "APIFailure",
                                "Ошибка ответа сервера для " + docId + ": " + response.code(),null);
                        Toast.makeText(MultiDocumentScanActivity.this, "Ошибка загрузки док-та " + docId, Toast.LENGTH_SHORT).show();
                    }

                    // Проверяем, завершились ли все запросы
                    if (requestsCompleted.incrementAndGet() == requestsToMake) {
                        RemoteLogger.info(LOG_TAG, "APISummary",
                                "Все запросы загрузки документов завершены");
                        onAllDocumentsLoaded();
                    }
                }

                @Override
                public void onFailure(Call<InventoryDocument> call, Throwable t) {
                    RemoteLogger.error(LOG_TAG, "APINetworkError",
                            "Ошибка сети для " + docId + ": " + t.getMessage(),null);
                    Toast.makeText(MultiDocumentScanActivity.this, "Ошибка сети (док-т " + docId + ")", Toast.LENGTH_SHORT).show();
                    // Проверяем, завершились ли все запросы (даже с ошибкой)
                    if (requestsCompleted.incrementAndGet() == requestsToMake) {
                        onAllDocumentsLoaded();
                    }
                }
            });
        }
    }

    /**
     * ✅ НОВЫЙ МЕТОД
     * Вызывается, когда все документы загружены (или не загружены).
     * Обновляет адаптер единым списком.
     */
    private void onAllDocumentsLoaded() {
        RemoteLogger.info(LOG_TAG, "DataReady",
                "Данные готовы. Документов: " + loadedDocuments.size() + ", Товаров: " + flatItemList.size());
        // TODO: Скрыть индикатор загрузки
        // binding.progressBar.setVisibility(View.GONE);

        if (flatItemList.isEmpty()) {
            RemoteLogger.warn(LOG_TAG, "DataReady",
                    "Финальный список товаров пуст");
            Toast.makeText(this, "Не удалось загрузить товары", Toast.LENGTH_LONG).show();
        } else {
            adapter.updateItems(flatItemList);
            setTitle("Сканирование (" + flatItemList.size() + " позиций)");
        }
    }

    //
    // ----------------------------------------------------------------------------------
    // УТИЛИТЫ (ЗВУК, ВИБРАЦИЯ, ОТПРАВКА, ЭКСПОРТ)
    // ----------------------------------------------------------------------------------
    //

    // Методы vibrateSuccess, vibrateError, playSuccessSound, playErrorSound (БЕЗ ИЗМЕНЕНИЙ)
    private void vibrateSuccess() {
        try {
            Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(100);
                }
            }
        } catch (Exception e) {
            Log.e("Vibration", "Ошибка вибрации успеха", e);
        }
    }

    private void vibrateError() {
        try {
            Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(new long[]{0, 100, 100, 100}, -1));
                } else {
                    vibrator.vibrate(new long[]{0, 100, 100, 100}, -1);
                }
            }
        } catch (Exception e) {
            Log.e("Vibration", "Ошибка вибрации ошибки", e);
        }
    }

    private void playSuccessSound() {
        try {
            MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.scan_success);
            if (mediaPlayer != null) {
                mediaPlayer.setOnCompletionListener(MediaPlayer::release);
                mediaPlayer.start();
            }
        } catch (Exception e) {
            Log.e("Sound", "Ошибка воспроизведения звука успеха", e);
        }
    }

    private void playErrorSound() {
        try {
            MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.scan_error);
            if (mediaPlayer != null) {
                mediaPlayer.setOnCompletionListener(MediaPlayer::release);
                mediaPlayer.start();
            }
        } catch (Exception e) {
            Log.e("Sound", "Ошибка воспроизведения звука ошибки", e);
        }
    }

    // ❌ СТАРЫЙ МЕТОД sendDocument() ЗАМЕНЕН
    /**
     * ✅ МОДИФИЦИРОВАННЫЙ МЕТОД
     * Отправляет ВСЕ загруженные документы в очередь на отправку.
     */
    private void sendAllDocuments() {
        RemoteLogger.info(LOG_TAG, "UserAction",
                "Нажата кнопка ОТПРАВИТЬ ВСЕ");
        if (loadedDocuments.isEmpty()) {
            RemoteLogger.warn(LOG_TAG, "Upload",
                    "Нет загруженных документов для отправки");
            Toast.makeText(this, "Документы не загружены", Toast.LENGTH_SHORT).show();
            return;
        }

        RemoteLogger.info(LOG_TAG, "Upload",
                "Отправка " + loadedDocuments.size() + " документов в очередь LocalQueueManager...");

        for (InventoryDocument doc : loadedDocuments) {
            RemoteLogger.info(LOG_TAG, "UploadQueue",
                    "Добавление документа ID: " + doc.getId() + " в очередь");
            // Так как мы изменяли InventoryItem по ссылке,
            // все изменения (setKolichestvoFakt(1)) уже находятся внутри
            // объектов 'doc' в списке 'loadedDocuments'.
            queueManager.sendOrQueue(doc);
        }

        Toast.makeText(this, "Отправлено в очередь " + loadedDocuments.size() + " док-тов", Toast.LENGTH_LONG).show();
        finish(); // Закрываем активность и возвращаемся к списку
    }

    // Методы экспорта БД (БЕЗ ИЗМЕНЕНИЙ)
    private void exportDatabase() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            performExport();
        } else {
            if (checkStoragePermission()) {
                performExport();
            } else {
                requestStoragePermission();
            }
        }
    }

    private boolean checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(
                    new String[]{
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE
                    },
                    STORAGE_PERMISSION_CODE
            );
        }
    }

    private void performExport() {
        new Thread(() -> {
            try {
                File dbFile = getDatabasePath("local_queue_db");
                Log.d("Export", "Main DB path: " + dbFile.getAbsolutePath());
                Log.d("Export", "Main DB exists: " + dbFile.exists());

                if (!dbFile.exists()) {
                    runOnUiThread(() ->
                            Toast.makeText(this, "Основная БД не найдена: " + dbFile.getAbsolutePath(), Toast.LENGTH_LONG).show());
                    return;
                }

                long dbSize = dbFile.length();
                Log.d("Export", "DB size: " + dbSize + " bytes");

                if (dbSize == 0) {
                    runOnUiThread(() ->
                            Toast.makeText(this, "БД пуста или не создана", Toast.LENGTH_LONG).show());
                    return;
                }

                // showFirstRecordForDebug(); // Вы можете раскомментировать это, если нужно

                // ... (Код для копирования в Downloads, если он у вас был в InventoryActivity) ...
                // Так как в вашем коде InventoryActivity метод performExport не был завершен,
                // я оставляю его как есть. Логика копирования файла должна быть здесь.
                // Примерная логика:
                /*
                File exportDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                if (!exportDir.exists()) {
                    exportDir.mkdirs();
                }
                File exportFile = new File(exportDir, "inventory_db_export.db");

                try (InputStream in = new FileInputStream(dbFile);
                     OutputStream out = new FileOutputStream(exportFile)) {

                    byte[] buffer = new byte[1024];
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                    }
                    out.flush();
                    runOnUiThread(() ->
                            Toast.makeText(this, "БД экспортирована в Downloads", Toast.LENGTH_LONG).show());
                } catch (IOException e) {
                    Log.e("Export", "Ошибка копирования файла", e);
                    runOnUiThread(() ->
                            Toast.makeText(this, "Ошибка экспорта: " + e.getMessage(), Toast.LENGTH_LONG).show());
                }
                */

            } catch (Exception e) {
                Log.e("Export", "Ошибка экспорта БД", e);
                runOnUiThread(() ->
                        Toast.makeText(this, "Ошибка экспорта: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    // Этот метод был в InventoryActivity, но не был вызван в performExport,
    // оставляю его на всякий случай, если он вам нужен.
    private void showFirstRecordForDebug() {
        // Логика для извлечения первой записи из БД (если она у вас была)
    }
}
