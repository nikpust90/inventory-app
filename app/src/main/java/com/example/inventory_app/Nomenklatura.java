package com.example.inventory_app;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class Nomenklatura implements Serializable {
    @SerializedName("nomenklaturaId")
    private String id;

    @SerializedName("nomenklatura")
    private String name;

    // Геттеры и сеттеры
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
