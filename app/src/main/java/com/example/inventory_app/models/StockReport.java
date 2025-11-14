package com.example.inventory_app.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class StockReport {

    @SerializedName("warehouseId")
    public String warehouseId;

    @SerializedName("warehouseName")
    public String warehouseName;

    @SerializedName("items")
    public List<StockItem> items;
}
