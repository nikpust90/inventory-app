package com.example.inventory_app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Глобальный ресивер, который ловит штрих-коды, отправляемые сканером ТСД.
 */


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Класс для обработки полученных штрих-кодов.
 */
public class BarcodeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // Получаем отсканированный штрих-код
        String barcode = intent.getStringExtra("barcode");

        // Выводим в лог
        Log.d("BarcodeReceiver", "Сканирован код: " + barcode);
    }
}
