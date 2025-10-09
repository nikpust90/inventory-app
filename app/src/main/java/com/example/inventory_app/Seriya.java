package com.example.inventory_app;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class Seriya implements Serializable {
    @SerializedName("seriyaId")
    private String id;
    @SerializedName("seriya")
    private String name;

    private String imei;

    // Геттеры и сеттеры
    public String getImei() {
        return imei;
    }
    public void setImei(String imei) { this.imei = imei; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
