package com.example.inventory_app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import com.example.inventory_app.databinding.ActivityMainBinding;

/**
 * Главная активность приложения, откуда начинается инвентаризация.
 */
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Обработчик кнопки "Начать инвентаризацию"
        binding.startInventoryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Переход в активность инвентаризации
                startActivity(new Intent(MainActivity.this, InventoryActivity.class));
            }
        });
    }
}