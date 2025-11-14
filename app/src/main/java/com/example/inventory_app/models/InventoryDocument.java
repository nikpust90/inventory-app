package com.example.inventory_app.models;

import java.io.Serializable;
import java.util.List;

public class InventoryDocument implements Serializable {
    private String id;
    private String date;
    private String warehouse;   // Новое поле - склад
    private String documentNumber; // Новое поле - номер документа
    private List<InventoryItem> items; // Список позиций в документе

    // Геттеры и сеттеры
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getWarehouse() { return warehouse; }
    public void setWarehouse(String warehouse) { this.warehouse = warehouse; }

    public String getDocumentNumber() { return documentNumber; }
    public void setDocumentNumber(String documentNumber) { this.documentNumber = documentNumber; }

    public List<InventoryItem> getItems() { return items; }
    public void setItems(List<InventoryItem> items) { this.items = items; }


}
