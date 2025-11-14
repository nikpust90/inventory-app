package com.example.inventory_app.models;


import com.google.gson.annotations.SerializedName;
import java.util.List; // <-- Важно: добавляем импорт List

public class LoginResponse {

    @SerializedName("userName")
    private String userName;

    // ИСПРАВЛЕНО: Должно быть List<String>
    @SerializedName("warehouseIds")
    private List<String> warehouseIds;

    @SerializedName("admin")
    private Boolean admin; // Используем Boolean, но геттер может быть boolean

    // Getters
    public String getUserName() {
        return userName;
    }

    // ИСПРАВЛЕНО: Геттер должен возвращать List<String>
    public List<String> getWarehouseIds() {
        return warehouseIds;
    }

    public boolean isAdmin() {
        // Защита от null, если вдруг сервер вернет null
        return admin != null && admin;
    }
}
