package com.example.inventory_app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.inventory_app.databinding.ActivityInventoryBinding;

public class InventoryActivity extends AppCompatActivity {

    private ActivityInventoryBinding binding;
    private BarcodeReceiver barcodeReceiver;

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Инициализация View Binding
        binding = ActivityInventoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Логирование для проверки инициализации
        Log.d("InventoryActivity", "onCreate called");

        // Инициализация BroadcastReceiver
        barcodeReceiver = new BarcodeReceiver(binding.scannedBarcodeText);

        // Регистрация BroadcastReceiver для получения данных от сканера
        IntentFilter filter = new IntentFilter("com.xcheng.scanner.action.BARCODE_DECODING_BROADCAST");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(barcodeReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(barcodeReceiver, filter);
        }

        Log.d("InventoryActivity", "BroadcastReceiver registered");

        // Установка начального текста
        binding.scannedBarcodeText.setText("Ожидание сканирования...");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Отмена регистрации BroadcastReceiver при уничтожении активности
        if (barcodeReceiver != null) {
            unregisterReceiver(barcodeReceiver);
            Log.d("InventoryActivity", "BroadcastReceiver unregistered");
        }
    }

    /**
     * Вложенный класс для обработки полученных штрих-кодов.
     */
    public static class BarcodeReceiver extends BroadcastReceiver {
        private final TextView textView;

        public BarcodeReceiver(TextView textView) {
            this.textView = textView;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            // Логирование для проверки получаемого Intent
            Log.d("BarcodeReceiver", "Received Intent: " + intent.toString());

            // Логируем все дополнительные данные (extras)
            Bundle extras = intent.getExtras();
            if (extras != null) {
                for (String key : extras.keySet()) {
                    Log.d("BarcodeReceiver", "Key: " + key + ", Value: " + extras.get(key));
                }
            }

            // Получаем штрих-код из Intent
            String barcode = intent.getStringExtra("EXTRA_BARCODE_DECODING_DATA");
            Log.d("BarcodeReceiver", "Received barcode: " + barcode);

            if (barcode != null && textView != null) {
                // Обновляем UI в основном потоке
                new Handler(Looper.getMainLooper()).post(() -> {
                    textView.setText("Отсканирован код: " + barcode);
                });
            } else {
                Log.d("BarcodeReceiver", "No barcode received or textView is null");
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (textView != null) {
                        textView.setText("Не удалось получить штрихкод.");
                    }
                });
            }
        }
    }
}