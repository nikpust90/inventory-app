package com.example.inventory_app.activity;

// ИЗМЕНЕНО: Добавлены импорты для View и встроенного сканера
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.inventory_app.*;
import com.example.inventory_app.adapters.InventoryItemAdapter;
import com.example.inventory_app.databinding.ActivityDocumentBinding;
import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
// УДАЛЕНО: Импорты для старого сканера больше не нужны
// import com.google.zxing.integration.android.IntentIntegrator;
// import com.google.zxing.integration.android.IntentResult;
import com.journeyapps.barcodescanner.DefaultDecoderFactory;
import com.journeyapps.barcodescanner.camera.CameraSettings;
import org.jetbrains.annotations.Nullable;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import androidx.appcompat.app.AlertDialog;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class InventoryActivity extends AppCompatActivity {

    private ActivityDocumentBinding binding;
    private ApiService apiService;
    private InventoryItemAdapter adapter;
    private InventoryDocument currentDocument;

    // НОВЫЕ ПОЛЯ: для управления встроенным сканером
    private DecoratedBarcodeView barcodeView;
    private String lastScannedBarcode = "";

    private static final int CAMERA_PERMISSION_REQUEST = 1001;
    private boolean hasCameraPermission = false;

    private RecyclerView recyclerView;


    // НОВЫЙ ОБЪЕКТ: Callback, который будет получать результат сканирования непрерывно
    private final BarcodeCallback callback = result -> {
        // Проверяем, что результат есть и он не такой же, как предыдущий
        if (result.getText() == null || result.getText().equals(lastScannedBarcode)) {
            return;
        }
        lastScannedBarcode = result.getText();

        // Вызываем обработку в основном потоке для безопасности работы с UI
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

        String documentId = getIntent().getStringExtra("DOCUMENT_ID");

        setupRecyclerView();

        if (documentId != null && !documentId.isEmpty()) {
            loadDocumentDetails(documentId);
        } else {
            Toast.makeText(this, "ID документа отсутствует", Toast.LENGTH_SHORT).show();
        }

        // ИЗМЕНЕНО: Старый вызов сканера заменен на вызов метода-переключателя
        binding.fabScan.setOnClickListener(v -> toggleScanner());

        binding.sendButton.setOnClickListener(v -> {
            if (currentDocument != null) {
                sendDocumentToServer();
            } else {
                Toast.makeText(this, "Документ не загружен", Toast.LENGTH_SHORT).show();
            }
        });
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
        new android.os.Handler().postDelayed(() -> lastScannedBarcode = "", 800);
    }

    /**
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

        item.setKolichestvoFakt(1); // Устанавливаем факт = 1
        item.setFound(true);      // Помечаем как найденную
        adapter.notifyItemChanged(itemIndex);

        playSuccessSound();
        vibrateSuccess();
        Toast.makeText(this, "Найдено: " + item.getNomenklatura().getName(), Toast.LENGTH_SHORT).show();

        // Прокрутка к найденной позиции
        recyclerView.scrollToPosition(itemIndex);
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

    private void sendDocumentToServer() {
        if (currentDocument == null) return;

        apiService.updateInventoryDocument(currentDocument).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(InventoryActivity.this, "Документ успешно отправлен!", Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    Toast.makeText(InventoryActivity.this, "Ошибка отправки документа", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(InventoryActivity.this, "Ошибка сети при отправке: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
