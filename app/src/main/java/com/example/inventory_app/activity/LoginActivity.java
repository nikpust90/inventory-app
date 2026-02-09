package com.example.inventory_app.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.inventory_app.ApiClient;
import com.example.inventory_app.ApiService;
import com.example.inventory_app.MainActivity;
import com.example.inventory_app.RemoteLogger;
import com.example.inventory_app.models.LoginRequest;
import com.example.inventory_app.models.LoginResponse;
import com.example.inventory_app.databinding.ActivityLoginBinding; // Убедитесь, что путь правильный

import java.util.HashSet;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.IOException;
import java.util.Set;

public class LoginActivity extends AppCompatActivity {

    private static final int MAX_ATTEMPTS = 3;
    private static final int PIN_LENGTH = 5;
    
    private ActivityLoginBinding binding;
    private ApiService apiService;
    private int loginAttempts = 0;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        RemoteLogger.init(this);
        RemoteLogger.info("LoginActivity", "ActivityStart", "Экран входа запущен");
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        apiService = ApiClient.getApiService();
        prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        
        // Восстанавливаем счетчик попыток
        loginAttempts = prefs.getInt("LOGIN_ATTEMPTS", 0);
        if (loginAttempts >= MAX_ATTEMPTS) {
            binding.pinEditText.setEnabled(false);
            binding.loginButton.setEnabled(false);
            Toast.makeText(this, "Превышено количество попыток входа. Перезапустите приложение.", Toast.LENGTH_LONG).show();
        }

        // Добавляем фильтр для ввода только букв и цифр
        binding.pinEditText.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Фильтруем: оставляем только буквы (латиница и кириллица) и цифры
                String filtered = s.toString().replaceAll("[^a-zA-Zа-яА-Я0-9]", "");
                if (!s.toString().equals(filtered)) {
                    binding.pinEditText.setText(filtered);
                    binding.pinEditText.setSelection(filtered.length());
                }
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        binding.loginButton.setOnClickListener(v -> {
            String pinCode = binding.pinEditText.getText().toString().trim();
            
            // Проверка на блокировку
            if (loginAttempts >= MAX_ATTEMPTS) {
                Toast.makeText(this, "Превышено количество попыток входа. Перезапустите приложение.", Toast.LENGTH_LONG).show();
                return;
            }
            
            if (pinCode.isEmpty()) {
                RemoteLogger.warn("LoginActivity", "EmptyPIN", "Попытка входа с пустым PIN-кодом");
                Toast.makeText(this, "Введите ПИН-код", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (pinCode.length() < PIN_LENGTH) {
                RemoteLogger.warn("LoginActivity", "InvalidPINLength", "PIN-код должен содержать " + PIN_LENGTH + " символов");
                Toast.makeText(this, "PIN-код должен содержать " + PIN_LENGTH + " символов", Toast.LENGTH_SHORT).show();
                return;
            }
            
            String maskedPIN = pinCode.length() > 2 ?
                    pinCode.substring(0, 2) + "***" : "***";
            RemoteLogger.info("LoginActivity", "LoginAttempt", 
                    "Попытка входа с PIN: " + maskedPIN + " (попытка " + (loginAttempts + 1) + " из " + MAX_ATTEMPTS + ")");
            performLogin(pinCode);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        RemoteLogger.info("LoginActivity", "ActivityResume", "Экран входа возобновлен");
    }

    @Override
    protected void onPause() {
        super.onPause();
        RemoteLogger.info("LoginActivity", "ActivityPause", "Экран входа приостановлен");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        RemoteLogger.info("LoginActivity", "ActivityDestroy", "Экран входа уничтожен");
    }

    private void performLogin(String pin) {
        RemoteLogger.info("LoginActivity", "LoginRequest", "Отправка запроса на вход");
        setLoading(true);
        LoginRequest loginRequest = new LoginRequest(pin);

        apiService.loginByPin(loginRequest).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse loginData = response.body();

                    // Успешная авторизация - сбрасываем счетчик попыток
                    loginAttempts = 0;
                    prefs.edit().putInt("LOGIN_ATTEMPTS", 0).commit();

                    // Логируем успешный ответ без конфиденциальных данных
                    RemoteLogger.info("LoginActivity", "LoginSuccess",
                            "Вход успешен. Пользователь: " + loginData.getUserName() +
                                    ", Админ: " + loginData.isAdmin());

                    // Сохраняем данные пользователя и склада
                    saveUserData(loginData);

                    // Переходим на главный экран
                    RemoteLogger.info("LoginActivity", "Navigation", "Переход на MainActivity");
                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    finish(); // Закрываем экран входа
                } else {
                    // Увеличиваем счетчик попыток
                    loginAttempts++;
                    prefs.edit().putInt("LOGIN_ATTEMPTS", loginAttempts).commit();
                    
                    String errorMessage = "Произошла неизвестная ошибка";
                    if (response.code() == 400 ) {
                        errorMessage = "ПИН-код не передан";
                        RemoteLogger.warn("LoginActivity", "LoginError",
                                "Код 400: " + errorMessage);
                    } else if (response.code() == 404) {
                        int remaining = MAX_ATTEMPTS - loginAttempts;
                        if (remaining > 0) {
                            errorMessage = "Пользователь с таким ПИН-кодом не найден. Осталось попыток: " + remaining;
                        } else {
                            errorMessage = "Превышено количество попыток входа (" + MAX_ATTEMPTS + "). Перезапустите приложение.";
                            binding.pinEditText.setEnabled(false);
                            binding.loginButton.setEnabled(false);
                        }
                        RemoteLogger.warn("LoginActivity", "LoginError",
                                "Код 404: " + errorMessage + " (попытка " + loginAttempts + " из " + MAX_ATTEMPTS + ")");
                    } else if (response.code() == 500) {
                        errorMessage = "Внутренняя ошибка сервера. Попробуйте позже.";
                        RemoteLogger.error("LoginActivity", "LoginError",
                                "Код 500: " + errorMessage, null);
                    } else if (response.errorBody() != null) {
                        // Попытка прочитать сообщение об ошибке от сервера
                        try {
                            // Здесь нужно создать парсер для JSON-ошибки, если сервер ее присылает
                            errorMessage = response.errorBody().string();
                            RemoteLogger.info("LoginActivity", "ServerErrorBody",
                                    "Тело ошибки: " + errorMessage);
                        } catch (IOException e) {
                            e.printStackTrace();
                            RemoteLogger.error("LoginActivity", "ErrorBodyParse",
                                    "Ошибка чтения тела ошибки", e);
                        }
                    }
                    Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    binding.pinEditText.setText(""); // Очищаем поле при ошибке
                    RemoteLogger.info("LoginActivity", "ServerErrorBody",
                            "Тело ошибки: " + errorMessage);
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                setLoading(false);
                // Увеличиваем счетчик попыток при сетевой ошибке
                loginAttempts++;
                prefs.edit().putInt("LOGIN_ATTEMPTS", loginAttempts).commit();
                
                int remaining = MAX_ATTEMPTS - loginAttempts;
                String errorMsg = "Ошибка сети: " + t.getMessage();
                if (remaining > 0) {
                    errorMsg += " (осталось попыток: " + remaining + ")";
                } else {
                    errorMsg = "Превышено количество попыток входа (" + MAX_ATTEMPTS + "). Перезапустите приложение.";
                    binding.pinEditText.setEnabled(false);
                    binding.loginButton.setEnabled(false);
                }
                
                Toast.makeText(LoginActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                RemoteLogger.error("LoginActivity", "LoginNetworkError",
                        "Сетевая ошибка при входе (попытка " + loginAttempts + " из " + MAX_ATTEMPTS + ")", t);
            }
        });
    }

    private void saveUserData(LoginResponse data) {
        // Используем SharedPreferences для простого хранения данных сессии
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("USER_NAME", data.getUserName());
        // Сохраняем флаг администратора
        editor.putBoolean("IS_ADMIN", data.isAdmin());
        // Сохраняем ВЕСЬ список складов.
        if (data.getWarehouseIds() != null) {

            editor.putStringSet("WAREHOUSE_ID_LIST", new HashSet<>(data.getWarehouseIds()));
            // Логируем сохраненные склады
            RemoteLogger.info("LoginActivity", "WarehousesSaved",
                    "Сохранено складов");
        } else {
            editor.putStringSet("WAREHOUSE_ID_LIST", new HashSet<>());
        }

        // --- КЛЮЧЕВОЕ ИЗМЕНЕНИЕ: Используем commit() для синхронного сохранения ---
        editor.commit(); // <-- Замените editor.apply() на editor.commit()
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
