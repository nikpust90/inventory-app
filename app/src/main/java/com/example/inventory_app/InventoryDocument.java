package com.example.inventory_app;

import java.io.Serializable;
import java.util.List;

public class InventoryDocument implements Serializable {
    private String id;
    private String date;
    private List<InventoryItem> items; // Список позиций в документе

    // Геттеры и сеттеры для всех полей
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public List<InventoryItem> getItems() { return items; }
    public void setItems(List<InventoryItem> items) { this.items = items; }
}
