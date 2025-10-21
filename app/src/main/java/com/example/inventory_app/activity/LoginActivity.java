package com.example.inventory_app.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.inventory_app.ApiClient;
import com.example.inventory_app.ApiService;
import com.example.inventory_app.LoginRequest;
import com.example.inventory_app.LoginResponse;
import com.example.inventory_app.databinding.ActivityLoginBinding; // Убедитесь, что путь правильный

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.IOException;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        apiService = ApiClient.getApiService();

        binding.loginButton.setOnClickListener(v -> {
            String pinCode = binding.pinEditText.getText().toString();
            if (pinCode.isEmpty()) {
                Toast.makeText(this, "Введите ПИН-код", Toast.LENGTH_SHORT).show();
                return;
            }
            performLogin(pinCode);
        });
    }

    private void performLogin(String pin) {
        setLoading(true);
        LoginRequest loginRequest = new LoginRequest(pin);

        apiService.loginByPin(loginRequest).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse loginData = response.body();

                    // Сохраняем данные пользователя и склада
                    saveUserData(loginData);

                    // Переходим на главный экран
                    startActivity(new Intent(LoginActivity.this, InventoryListActivity.class));
                    finish(); // Закрываем экран входа
                } else {
                    String errorMessage = "Произошла неизвестная ошибка";
                    if (response.code() == 400 ) {
                        errorMessage = "ПИН-код не передан";
                    } else if (response.code() == 404) {
                        errorMessage = "Пользователь с таким ПИН-кодом не найден.";
                    } else if (response.code() == 500) {
                        errorMessage = "Внутренняя ошибка сервера. Попробуйте позже.";
                    } else if (response.errorBody() != null) {
                        // Попытка прочитать сообщение об ошибке от сервера
                        try {
                            // Здесь нужно создать парсер для JSON-ошибки, если сервер ее присылает
                            errorMessage = response.errorBody().string();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                setLoading(false);
                Toast.makeText(LoginActivity.this, "Ошибка сети: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void saveUserData(LoginResponse data) {
        // Используем SharedPreferences для простого хранения данных сессии
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("USER_NAME", data.getUserName());
        editor.putString("WAREHOUSE_ID", data.getWarehouseId());
        editor.apply();
    }



    private void setLoading(boolean isLoading) {
        if (isLoading) {
            binding.loginButton.setVisibility(View.GONE);
            binding.loadingProgressBar.setVisibility(View.VISIBLE);
        } else {
            binding.loginButton.setVisibility(View.VISIBLE);
            binding.loadingProgressBar.setVisibility(View.GONE);
        }
    }
}
