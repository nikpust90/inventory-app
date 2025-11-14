package com.example.inventory_app.activity;

// ИЗМЕНЕНО: Добавлены импорты для View и встроенного сканера
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
// УДАЛЕНО: Импорты для старого сканера больше не нужны
// import com.google.zxing.integration.android.IntentIntegrator;
// import com.google.zxing.integration.android.IntentResult;
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

import com.example.inventory_app.data.LocalQueueManager;

public class InventoryActivity extends AppCompatActivity {

    private ActivityDocumentBinding binding;
    private ApiService apiService;
    private InventoryItemAdapter adapter;
    private InventoryDocument currentDocument;
    private DecoratedBarcodeView barcodeView;
    private String lastScannedBarcode = "";
    private long lastScanTime = 0;
    private static final long SCAN_DELAY_MS = 1500;
    private static final int CAMERA_PERMISSION_REQUEST = 1001;
    private boolean hasCameraPermission = false;
    private RecyclerView recyclerView;
    private LocalQueueManager queueManager;
    private static final int STORAGE_PERMISSION_CODE = 1002;


    // 🔹 ИЗМЕНЕНО: новый callback с ограничением частоты сканирования
    private final BarcodeCallback callback = result -> {
        if (result.getText() == null) return;

        long now = System.currentTimeMillis();

        // 🔸 ДОБАВЛЕНО: защита от слишком частого сканирования
        if (now - lastScanTime < SCAN_DELAY_MS) {
            return; // Пропускаем если с момента прошлого скана прошло < 2 секунд
        }

        // 🔸 ДОБАВЛЕНО: защита от дублирования того же штрихкода
        if (result.getText().equals(lastScannedBarcode)) {
            return;
        }

        // 🔸 ДОБАВЛЕНО: обновляем данные последнего сканирования
        lastScannedBarcode = result.getText();
        lastScanTime = now;

        // 🔸 Обработка скана в UI-потоке
        runOnUiThread(() -> processScan(lastScannedBarcode));
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDocumentBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Запрос разрешения камеры
        checkCameraPermission();

        // ИЗМЕНЕНО: Инициализируем наш встроенный сканер из макета
        barcodeView = binding.barcodeScanner;
        apiService = ApiClient.getRetrofitInstance().create(ApiService.class);

        queueManager = new LocalQueueManager(this, apiService);

        // Раз в onCreate
        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(
                PendingUploadWorker.class,
                15, TimeUnit.MINUTES
        ).build();
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "PendingUploadWork",
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                workRequest
        );

        String documentId = getIntent().getStringExtra("DOCUMENT_ID");

        setupRecyclerView();

        if (documentId != null && !documentId.isEmpty()) {
            loadDocumentDetails(documentId);
        } else {
            Toast.makeText(this, "ID документа отсутствует", Toast.LENGTH_SHORT).show();
        }

        // ИЗМЕНЕНО: Старый вызов сканера заменен на вызов метода-переключателя
        binding.fabScan.setOnClickListener(v -> toggleScanner());

        binding.sendButton.setOnClickListener(v -> sendDocument());

        binding.btnExportDb.setOnClickListener(v -> exportDatabase());

    }

    private void checkCameraPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST);
            } else {
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
                hasCameraPermission = true;
                initializeScanner();
            } else {
                Toast.makeText(this, "Для сканирования нужны разрешения камеры", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void initializeScanner() {

        if (hasCameraPermission) {
            barcodeView = binding.barcodeScanner;

            // Настройки камеры
            CameraSettings settings = new CameraSettings();
            settings.setFocusMode(CameraSettings.FocusMode.AUTO);
            settings.setAutoFocusEnabled(true);
            settings.setContinuousFocusEnabled(true); // Включите непрерывный фокус
            settings.setExposureEnabled(true); // Включите экспозицию

            barcodeView.getBarcodeView().setCameraSettings(settings);

            // Расширьте список поддерживаемых форматов
            Collection<BarcodeFormat> formats = Arrays.asList(
                    BarcodeFormat.CODE_128,    // Основной формат вашего штрихкода
                    BarcodeFormat.CODE_39,
                    BarcodeFormat.CODE_93,
                    BarcodeFormat.CODABAR,
                    BarcodeFormat.EAN_13,
                    BarcodeFormat.EAN_8,
                    BarcodeFormat.UPC_A,
                    BarcodeFormat.UPC_E,
                    BarcodeFormat.ITF,
                    BarcodeFormat.RSS_14,
                    BarcodeFormat.RSS_EXPANDED,
                    BarcodeFormat.QR_CODE,
                    BarcodeFormat.DATA_MATRIX,
                    BarcodeFormat.AZTEC,
                    BarcodeFormat.PDF_417
            );
            barcodeView.getBarcodeView().setDecoderFactory(new DefaultDecoderFactory(formats));

            // Включите отладочную информацию
            barcodeView.setStatusText("Наведите на штрихкод");
        }
    }

    // НОВЫЙ МЕТОД: Включает и выключает сканер по нажатию на кнопку
    private void toggleScanner() {
        if (barcodeView.getVisibility() == View.VISIBLE) {
            // Если сканер виден - скрываем его
            barcodeView.setVisibility(View.GONE);
            barcodeView.pause();
            binding.fabScan.setImageResource(android.R.drawable.ic_menu_camera);
        } else {
            // Проверяем разрешение перед открытием сканера
            if (!hasCameraPermission) {
                checkCameraPermission();
                return;
            }

            // Если сканер скрыт - показываем его
            barcodeView.setVisibility(View.VISIBLE);
            barcodeView.resume();
            barcodeView.decodeContinuous(callback);
            binding.fabScan.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);

            // Фокус на сканер
            barcodeView.getBarcodeView().setFocusableInTouchMode(true);
            barcodeView.getBarcodeView().requestFocus();
        }
    }

    // ИЗМЕНЕНО: Управление жизненным циклом активити для корректной работы камеры
    @Override
    protected void onResume() {
        super.onResume();
        queueManager.trySendAll();

        // Возобновляем работу камеры только если сканер был видимым
        if (barcodeView.getVisibility() == View.VISIBLE) {
            barcodeView.resume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Всегда останавливаем камеру, когда активность уходит в фон
        if (barcodeView.isActivated()) {
            barcodeView.pause();
        }
    }

    // НОВЫЙ МЕТОД: Обработка системной кнопки "Назад"
    @Override
    public void onBackPressed() {
        // Если сканер открыт, кнопка "Назад" сначала закроет его
        if (barcodeView.getVisibility() == View.VISIBLE) {
            toggleScanner();
        } else {
            // Если сканер закрыт, выходим из активности
            super.onBackPressed();
        }
    }

    // ИЗМЕНЕНО: Теперь этот метод вызывается напрямую из BarcodeCallback
    // ИЗМЕНЕНО: Новая, более гибкая логика обработки сканирования
    private void processScan(String scannedBarcode) {
        if (currentDocument == null || currentDocument.getItems() == null || scannedBarcode == null || scannedBarcode.trim().isEmpty()) {
            Toast.makeText(this, "Документ не готов или штрихкод пуст", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1. Подготовка значения для поиска (как и раньше)
        String valueToSearch;
        if (scannedBarcode.toLowerCase().startsWith("http")) {
            valueToSearch = scannedBarcode.length() >= 12 ? scannedBarcode.substring(scannedBarcode.length() - 12) : scannedBarcode;
        } else {
            valueToSearch = scannedBarcode;
        }

        // 2. Поиск ВСЕХ совпадений, а не первого
        List<InventoryItem> items = currentDocument.getItems();
        List<Integer> matchingIndices = new ArrayList<>(); // Список для хранения индексов найденных позиций

        for (int i = 0; i < items.size(); i++) {
            InventoryItem item = items.get(i);
            Seriya seriya = item.getSeriya();
            if (seriya == null) continue;

            boolean isMatch = (seriya.getName() != null && valueToSearch.equals(seriya.getName())) ||
                    (seriya.getImei() != null && valueToSearch.equals(seriya.getImei()));

            if (isMatch) {
                matchingIndices.add(i); // Добавляем индекс в список, не прерываем цикл
            }
        }

        // 3. Анализ результатов поиска
        if (matchingIndices.isEmpty()) {
            // СОВПАДЕНИЙ НЕ НАЙДЕНО
            playErrorSound();
            vibrateError();
            //Toast.makeText(this, "Ошибка: '" + valueToSearch + "' не найдено", Toast.LENGTH_SHORT).show();

        } else if (matchingIndices.size() == 1) {
            // НАЙДЕНО РОВНО ОДНО СОВПАДЕНИЕ (стандартный сценарий)
            processFoundItem(matchingIndices.get(0));

        } else {
            // НАЙДЕНО НЕСКОЛЬКО СОВПАДЕНИЙ
            showNomenclatureChoiceDialog(matchingIndices, valueToSearch);
        }

        // Задержка перед следующим сканированием
        //new android.os.Handler().postDelayed(() -> lastScannedBarcode = "", 800);
    }


    /**
     * 🔥 ИЗМЕНЕНО: Теперь показывает диалоговое окно для подтверждения найденной позиции.
     * Обрабатывает найденную позицию: обновляет данные, адаптер и подает сигнал пользователю.
     * @param itemIndex Индекс найденной позиции в списке currentDocument.getItems()
     */
    private void processFoundItem(int itemIndex) {
        InventoryItem item = currentDocument.getItems().get(itemIndex);

        // Если товар уже был найден, можно просто подать сигнал, не изменяя количество
        if (item.isFound()) {
            Toast.makeText(this, "Повторное сканирование: " + item.getNomenklatura().getName(), Toast.LENGTH_SHORT).show();
            playSuccessSound(); // Просто звук успеха
            return;
        }

        // 🔥 НОВЫЙ КОД: Создаем и показываем диалоговое окно
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("✅ Найдена позиция");

        // Формируем сообщение с деталями
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

        // Кнопка "ОК"
        builder.setPositiveButton("ОК", (dialog, which) -> {
            // Вся логика обработки выполняется только после нажатия "ОК"
            item.setKolichestvoFakt(1); // Устанавливаем факт = 1
            item.setFound(true);      // Помечаем как найденную
            adapter.notifyItemChanged(itemIndex);

            playSuccessSound();
            vibrateSuccess();
            Toast.makeText(this, "Подтверждено: " + item.getNomenklatura().getName(), Toast.LENGTH_SHORT).show();

            // Прокрутка к найденной позиции
            recyclerView.scrollToPosition(itemIndex);

            dialog.dismiss();
        });

        // Кнопка "Отмена"
        builder.setNegativeButton("Отмена", (dialog, which) -> {
            dialog.dismiss();
        });

        // Показываем диалог
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Показывает диалог выбора номенклатуры, когда по одному штрихкоду найдено несколько позиций.
     * @param indices Список индексов найденных позиций.
     * @param scannedValue Отсканированное значение для отображения в заголовке.
     */
    private void showNomenclatureChoiceDialog(List<Integer> indices, String scannedValue) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Найдено несколько позиций для '" + scannedValue + "'. Выберите нужную:");

        // Создаем массив строк с названиями номенклатур для отображения в диалоге
        String[] nomenclatureNames = new String[indices.size()];
        for (int i = 0; i < indices.size(); i++) {
            InventoryItem item = currentDocument.getItems().get(indices.get(i));
            String name = item.getNomenklatura().getName() != null ? item.getNomenklatura().getName() : "Без имени";
            nomenclatureNames[i] = name;
        }

        // Устанавливаем список и обработчик клика
        builder.setItems(nomenclatureNames, (dialog, which) -> {
            // 'which' - это индекс нажатого элемента в диалоговом окне.
            // Он соответствует индексу в нашем списке 'indices'.
            int chosenItemIndex = indices.get(which);

            // Теперь, когда пользователь сделал выбор, обрабатываем эту позицию
            processFoundItem(chosenItemIndex);

            // ЯВНО ЗАКРЫВАЕМ ДИАЛОГ
            dialog.dismiss();
        });

        // Добавляем кнопку "Отмена"
        builder.setNegativeButton("Отмена", (dialog, id) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

        // Также подадим сигнал, что что-то произошло
        playErrorSound(); // Можно использовать звук ошибки/внимания
        vibrateError();   // И вибрацию
    }

    private void setupRecyclerView() {
        // Мы сохраняем ссылку на RecyclerView из binding в наше поле класса.
        recyclerView = binding.itemsRecyclerView;
        adapter = new InventoryItemAdapter(new ArrayList<>());
        binding.itemsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.itemsRecyclerView.setAdapter(adapter);
    }

    private void loadDocumentDetails(String documentId) {
        apiService.getInventoryDocumentById(documentId).enqueue(new Callback<InventoryDocument>() {
            @Override
            public void onResponse(Call<InventoryDocument> call, Response<InventoryDocument> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentDocument = response.body();
                    List<InventoryItem> items = currentDocument.getItems();
                    if (items == null) items = new ArrayList<>();
                    adapter.updateItems(items);
                } else {
                    String errorMsg = "Не удалось загрузить документ";
                    try {
                        if (response.errorBody() != null) errorMsg += ": " + response.errorBody().string();
                    } catch (IOException e) {
                        errorMsg += " (ошибка чтения ответа)";
                    }
                    Toast.makeText(InventoryActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<InventoryDocument> call, Throwable t) {
                Toast.makeText(InventoryActivity.this, "Ошибка сети: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

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


    private void sendDocument() {
        if (currentDocument == null) {
            Toast.makeText(this, "Документ не заполнен", Toast.LENGTH_SHORT).show();
            return;
        }

        queueManager.sendOrQueue(currentDocument);
    }

    // ДАЛЕЕ КЛАССЫ ПРОСТО ДЛЯ ВЫГРУЗКИ БД
    private void exportDatabase() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Для Android 10+ не нужны разрешения для Downloads
            performExport();
        } else {
            // Для старых версий запрашиваем разрешения
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

//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
//                                           @NonNull int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        if (requestCode == STORAGE_PERMISSION_CODE) {
//            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                performExport();
//            } else {
//                Toast.makeText(this, "Нужны разрешения для экспорта БД", Toast.LENGTH_LONG).show();
//            }
//        }
//    }

    private void performExport() {
        new Thread(() -> {
            try {
                // 1. Получаем путь к РЕАЛЬНОЙ базе данных Room
                File dbFile = getDatabasePath("local_queue_db");

                // 2. Room создает несколько файлов - нужно копировать основной
                Log.d("Export", "Main DB path: " + dbFile.getAbsolutePath());
                Log.d("Export", "Main DB exists: " + dbFile.exists());

                if (!dbFile.exists()) {
                    runOnUiThread(() ->
                            Toast.makeText(this, "Основная БД не найдена: " + dbFile.getAbsolutePath(), Toast.LENGTH_LONG).show());
                    return;
                }

                // 3. Проверяем размер БД
                long dbSize = dbFile.length();
                Log.d("Export", "DB size: " + dbSize + " bytes");

                if (dbSize == 0) {
                    runOnUiThread(() ->
                            Toast.makeText(this, "БД пуста или не создана", Toast.LENGTH_LONG).show());
                    return;
                }

                // 4. ВЫВОДИМ ПЕРВУЮ СТРОКУ ИЗ БД ДЛЯ ПРОВЕРКИ
                showFirstRecordForDebug();

                // 5. Создаем папку для экспорта
                File exportDir;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Для Android 10+ используем MediaStore
                    exportUsingMediaStore(dbFile);
                    return;
                } else {
                    // Для старых версий - прямо в Downloads
                    exportDir = new File(
                            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                            "InventoryAppDB"
                    );

                    if (!exportDir.exists() && !exportDir.mkdirs()) {
                        runOnUiThread(() ->
                                Toast.makeText(this, "Не удалось создать папку: " + exportDir.getAbsolutePath(), Toast.LENGTH_LONG).show());
                        return;
                    }
                }

                // 6. Копируем файл БД
                File destFile = new File(exportDir, "local_queue_db.db");

                try (InputStream in = new FileInputStream(dbFile);
                     OutputStream out = new FileOutputStream(destFile)) {

                    byte[] buffer = new byte[8192]; // Увеличиваем буфер для скорости
                    int length;
                    long totalCopied = 0;

                    while ((length = in.read(buffer)) > 0) {
                        out.write(buffer, 0, length);
                        totalCopied += length;
                    }

                    Log.d("Export", "Copied " + totalCopied + " bytes to " + destFile.getAbsolutePath());
                }

                // 7. Проверяем что скопировалось
                if (destFile.exists() && destFile.length() > 0) {
                    final String successMessage = "БД успешно экспортирована!\n" +
                            "Размер: " + destFile.length() + " байт\n" +
                            "Путь: Download/InventoryAppDB/local_queue_db.db";

                    runOnUiThread(() -> {
                        Toast.makeText(this, successMessage, Toast.LENGTH_LONG).show();

                        // Показываем подробности
                        new AlertDialog.Builder(this)
                                .setTitle("✅ Экспорт завершен")
                                .setMessage(successMessage)
                                .setPositiveButton("OK", null)
                                .show();
                    });

                } else {
                    runOnUiThread(() ->
                            Toast.makeText(this, "Ошибка: файл не скопировался", Toast.LENGTH_LONG).show());
                }

            } catch (Exception e) {
                Log.e("Export", "Export error", e);
                runOnUiThread(() ->
                        Toast.makeText(this, "Ошибка экспорта: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    // МЕТОД ДЛЯ ВЫВОДА ПЕРВОЙ СТРОКИ БД
    private void showFirstRecordForDebug() {
        try {
            AppDatabase db = AppDatabaseSingleton.getInstance(this);
            List<PendingDocument> docs = db.pendingDocumentDao().getAll();

            if (docs == null || docs.isEmpty()) {
                Log.d("Export", "БД ПУСТА - нет записей для экспорта");
                runOnUiThread(() ->
                        Toast.makeText(this, "БД пуста - нет записей", Toast.LENGTH_LONG).show());
                return;
            }

            // Берем первую запись
            PendingDocument firstDoc = docs.get(0);

            String debugInfo = "=== ДЕБАГ ИНФО БД ===\n" +
                    "Всего записей: " + docs.size() + "\n" +
                    "Первая запись:\n" +
                    "ID: " + firstDoc.id + "\n" +
                    "Timestamp: " + firstDoc.timestamp + "\n" +
                    "JSON длина: " + (firstDoc.documentJson != null ? firstDoc.documentJson.length() : 0) + " символов\n" +
                    "JSON начало: " + (firstDoc.documentJson != null ?
                    firstDoc.documentJson.substring(0, Math.min(80, firstDoc.documentJson.length())) : "null");

            Log.d("Export", debugInfo);

            // Показываем пользователю только основную информацию
            final String userMessage = "Найдено записей: " + docs.size() +
                    "\nПервая запись ID: " + firstDoc.id;

            runOnUiThread(() -> {
                Toast.makeText(this, userMessage, Toast.LENGTH_LONG).show();
            });

        } catch (Exception e) {
            Log.e("Export", "Ошибка при чтении БД для дебага", e);
        }
    }

    // РЕАЛИЗАЦИЯ МЕТОДА exportUsingMediaStore
    @TargetApi(Build.VERSION_CODES.Q)
    private void exportUsingMediaStore(File dbFile) {
        try {
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.Downloads.DISPLAY_NAME, "local_queue_db.db");
            contentValues.put(MediaStore.Downloads.MIME_TYPE, "application/octet-stream");
            contentValues.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/InventoryAppDB");

            Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues);

            if (uri != null) {
                try (OutputStream out = getContentResolver().openOutputStream(uri);
                     InputStream in = new FileInputStream(dbFile)) {

                    byte[] buffer = new byte[8192];
                    int length;
                    long totalCopied = 0;

                    while ((length = in.read(buffer)) > 0) {
                        out.write(buffer, 0, length);
                        totalCopied += length;
                    }

                    Log.d("Export", "Copied " + totalCopied + " bytes via MediaStore");
                }

                final String successMessage = "БД экспортирована через MediaStore!\n" +
                        "Путь: Download/InventoryAppDB/local_queue_db.db\n" +
                        "Записей в БД: " + getRecordCount();

                runOnUiThread(() -> {
                    Toast.makeText(this, successMessage, Toast.LENGTH_LONG).show();

                    new AlertDialog.Builder(this)
                            .setTitle("✅ Экспорт завершен")
                            .setMessage(successMessage)
                            .setPositiveButton("OK", null)
                            .show();
                });
            } else {
                runOnUiThread(() ->
                        Toast.makeText(this, "Ошибка: не удалось создать файл через MediaStore", Toast.LENGTH_LONG).show());
            }

        } catch (Exception e) {
            Log.e("Export", "MediaStore export error", e);
            runOnUiThread(() ->
                    Toast.makeText(this, "Ошибка MediaStore: " + e.getMessage(), Toast.LENGTH_LONG).show());
        }
    }

    // Вспомогательный метод для получения количества записей
    private int getRecordCount() {
        try {
            AppDatabase db = AppDatabaseSingleton.getInstance(this);
            List<PendingDocument> docs = db.pendingDocumentDao().getAll();
            return docs != null ? docs.size() : 0;
        } catch (Exception e) {
            return -1;
        }
    }

    private void showExportSuccessDialog(String filePath) {
        new AlertDialog.Builder(this)
                .setTitle("✅ База данных экспортирована")
                .setMessage("Файл: " + filePath + "\n\nТеперь откройте его в SQLite Viewer")
                .setPositiveButton("OK", null)
                .setNeutralButton("Открыть папку", (dialog, which) -> openDownloadsFolder())
                .show();
    }

    private void openDownloadsFolder() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri uri = Uri.parse(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS) + "/InventoryAppDB");
            intent.setDataAndType(uri, "resource/folder");
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Не удалось открыть папку", Toast.LENGTH_SHORT).show();
        }
    }
}
