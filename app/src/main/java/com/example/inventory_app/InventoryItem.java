package com.example.inventory_app;

/**
 * Модель данных для элемента инвентаризации.
 */
public class InventoryItem {
    private String id;
    private String name;
    private int quantity;

    public InventoryItem(String id, String name, int quantity) {
        this.id = id;
        this.name = name;
        this.quantity = quantity;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getQuantity() {
        return quantity;
    }
}
