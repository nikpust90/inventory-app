package com.example.inventory_app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.inventory_app.databinding.ActivityInventoryBinding;

/**
 * Активность для процесса инвентаризации.
 * Ожидает сканирование штрих-кода и отображает его.
 */
public class InventoryActivity extends AppCompatActivity {

    private ActivityInventoryBinding binding; // View Binding для удобства
    private BarcodeReceiver barcodeReceiver; // Приемник штрих-кодов

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityInventoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Создаем экземпляр ресивера и регистрируем его
        barcodeReceiver = new BarcodeReceiver(binding.scannedBarcodeText);
        registerReceiver(barcodeReceiver, new IntentFilter("com.android.tsd.SCAN"));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(barcodeReceiver); // Отменяем регистрацию при выходе из активности
    }

    /**
     * Вложенный класс для обработки полученных штрих-кодов.
     */
    public static class BarcodeReceiver extends BroadcastReceiver {
        private TextView textView;

        public BarcodeReceiver(TextView textView) {
            this.textView = textView;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            // Получаем данные о штрих-коде из Intent
            String barcode = intent.getStringExtra("barcode");
            if (barcode != null) {
                textView.setText("Отсканирован код: " + barcode);
            }
        }
    }
}