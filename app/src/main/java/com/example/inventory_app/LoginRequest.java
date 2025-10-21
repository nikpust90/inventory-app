package com.example.inventory_app;

import com.google.gson.annotations.SerializedName;

public class LoginRequest {

    @SerializedName("pin") // Явно указываем имя поля в JSON
    private String pin;

    public LoginRequest(String pin) {
        this.pin = pin;
    }

    // Геттер не обязателен, если есть аннотация, но это хороший стиль
    public String getPin() {
        return pin;
    }
}
