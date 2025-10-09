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
//        if (hasCameraPermission) {
//            barcodeView = binding.barcodeScanner;
//
//            // Настройки сканера
//            CameraSettings settings = new CameraSettings();
//            settings.setFocusMode(CameraSettings.FocusMode.AUTO);
//            settings.setAutoFocusEnabled(true);
//
//            barcodeView.getBarcodeView().setCameraSettings(settings);
//            //barcodeView.getBarcodeView().setStatusText("");
//            //barcodeView.setStatusText("Наведите камеру на штрихкод");
//
//            // Включите декодирование всех форматов
//            Collection<BarcodeFormat> formats = Arrays.asList(
//                    BarcodeFormat.QR_CODE,
//                    BarcodeFormat.CODE_128,
//                    BarcodeFormat.EAN_13,
//                    BarcodeFormat.UPC_A
//            );
//            barcodeView.getBarcodeView().setDecoderFactory(new DefaultDecoderFactory(formats));
//        }
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

    // УДАЛЕНО: Этот метод больше не нужен, т.к. мы не используем отдельную активность для сканирования
    /*
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null && result.getContents() != null) {
            processScan(result.getContents());
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
    */

    // ИЗМЕНЕНО: Теперь этот метод вызывается напрямую из BarcodeCallback
    private void processScan(String scannedBarcode) {
        if (currentDocument == null || currentDocument.getItems() == null || scannedBarcode == null || scannedBarcode.trim().isEmpty()) {
            Toast.makeText(this, "Документ не готов или штрихкод пуст", Toast.LENGTH_SHORT).show();
            return;
        }

        String valueToSearch;
        if (scannedBarcode.toLowerCase().startsWith("http")) {
            valueToSearch = scannedBarcode.length() >= 6 ? scannedBarcode.substring(scannedBarcode.length() - 6) : scannedBarcode;
        } else {
            valueToSearch = scannedBarcode;
        }

        int foundItemIndex = -1;
        List<InventoryItem> items = currentDocument.getItems();

        for (int i = 0; i < items.size(); i++) {
            InventoryItem item = items.get(i);
            Seriya seriya = item.getSeriya();
            if (seriya == null) continue;

            boolean isMatch = (seriya.getName() != null && valueToSearch.equals(seriya.getName())) ||
                    (seriya.getImei() != null && valueToSearch.equals(seriya.getImei()));

            if (isMatch) {
                foundItemIndex = i;
                break;
            }
        }

        if (foundItemIndex != -1) {
            InventoryItem item = items.get(foundItemIndex);
            //item.setKolichestvoFakt(item.getKolichestvoFakt() + 1);
            item.setKolichestvoFakt(1);
            item.setFound(true);
            adapter.notifyItemChanged(foundItemIndex);

            // ИЗМЕНЕНО: Вместо диалогового окна теперь Toast, звук и вибрация
            playSuccessSound();
            vibrateSuccess();
            Toast.makeText(this, "Найдено: " + item.getNomenklatura().getName(), Toast.LENGTH_SHORT).show();
        } else {
            // ИЗМЕНЕНО: Вместо диалогового окна теперь Toast, звук и вибрация
            playErrorSound();
            vibrateError();
            Toast.makeText(this, "Ошибка: '" + valueToSearch + "' не найдено", Toast.LENGTH_SHORT).show();
        }

        // НОВЫЙ БЛОК: Задержка перед считыванием следующего кода, чтобы избежать двойных срабатываний
        // и дать пользователю время среагировать на звук/вибрацию.
        new android.os.Handler().postDelayed(() -> lastScannedBarcode = "", 800); // 0.8 секунды
    }

    // УДАЛЕНО: Диалоговое окно блокирует UI и мешает непрерывному сканированию
    /*
    private void showResultDialog(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }
    */

    // --- Методы ниже остались без изменений ---

    private void setupRecyclerView() {
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
