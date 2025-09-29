package com.example.inventory_app;


import com.google.gson.annotations.SerializedName;

import java.util.List;



public class OutgoingDocument {
    @SerializedName("documentNumber")
    private String documentNumber;

    @SerializedName("createdAt")
    private String createdAt;

    @SerializedName("items")
    private List<OutgoingItem> items;

    @SerializedName("warehouseId")
    private String warehouseId;

    public String getDocumentNumber() {
        return documentNumber;
    }

    public void setDocumentNumber(String documentNumber) {
        this.documentNumber = documentNumber;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public List<OutgoingItem> getItems() {
        return items;
    }

    public void setItems(List<OutgoingItem> items) {
        this.items = items;
    }

    public String getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(String warehouseId) {
        this.warehouseId = warehouseId;
    }
}
