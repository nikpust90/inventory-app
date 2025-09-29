package com.example.inventory_app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.example.inventory_app.databinding.ActivityInventoryBinding;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class InventoryActivityTest extends AppCompatActivity {

    private ActivityInventoryBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Инициализация View Binding
        binding = ActivityInventoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Установка начального текста
        binding.scannedBarcodeText.setText("Нажмите кнопку для сканирования");

        // Находим кнопку и устанавливаем обработчик нажатия
        FloatingActionButton fabScan = binding.fabScan; // Лучше брать view из binding
        fabScan.setOnClickListener(view -> {
            // Инициализируем сканер
            startBarcodeScanner();
        });
    }

    /**
     * Метод для запуска сканера штрихкодов через камеру.
     */
    private void startBarcodeScanner() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES); // Сканировать все типы ШК
        integrator.setPrompt("Наведите камеру на штрихкод");
        integrator.setCameraId(0);  // Использовать заднюю камеру
        integrator.setBeepEnabled(true); // Звуковой сигнал при успехе
        integrator.setBarcodeImageEnabled(false); // Не сохранять изображение ШК, чтобы было быстрее
        integrator.setOrientationLocked(false); // Не блокировать ориентацию
        integrator.initiateScan();
    }

    /**
     * Этот метод вызывается, когда сканер возвращает результат.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        // Получаем результат сканирования
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() == null) {
                // Пользователь отменил сканирование (нажал "назад")
                Toast.makeText(this, "Сканирование отменено", Toast.LENGTH_LONG).show();
            } else {
                // Штрихкод успешно получен!
                String scannedBarcode = result.getContents();
                binding.scannedBarcodeText.setText("Отсканирован код: " + scannedBarcode);

                // TODO: Здесь вызываем метод для загрузки номенклатуры с сервера по штрихкоду
                // loadItemByBarcode(scannedBarcode);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}