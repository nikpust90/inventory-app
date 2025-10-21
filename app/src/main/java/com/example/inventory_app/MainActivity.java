package com.example.inventory_app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import com.example.inventory_app.activity.InventoryListActivity;
import com.example.inventory_app.activity.LoginActivity;
import com.example.inventory_app.databinding.ActivityMainBinding;

/**
 * Главная активность приложения, откуда начинается инвентаризация.
 */
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        binding = ActivityMainBinding.inflate(getLayoutInflater());
//        setContentView(binding.getRoot());
//
//
//        // Обработчик кнопки для просмотра списка документов инвентаризации
//        // Предполагаем, что в layout activity_main.xml есть кнопка с id="viewInventoryListButton"
//        // Если кнопки нет, добавьте в XML: <Button android:id="@+id/viewInventoryListButton" android:text="Просмотреть список документов" ... />
//        binding.viewInventoryListButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                // Переход в активность со списком документов инвентаризации
//                startActivity(new Intent(MainActivity.this, InventoryListActivity.class));
//            }
//        });
//
//
//    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Сразу же перенаправляем на экран входа
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(intent);

        // Закрываем текущую активность, чтобы она не оставалась в стеке
        finish();
    }
}