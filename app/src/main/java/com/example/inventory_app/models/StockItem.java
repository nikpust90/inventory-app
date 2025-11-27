package com.example.inventory_app.models;

import java.util.List;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class StockItem {
    public static final int TYPE_WAREHOUSE = 0;
    public static final int TYPE_NOMENCLATURE = 1;
    public static final int TYPE_SERIES = 2;

    public int type;

    // Для склада
    public String warehouseId;
    public String warehouseName;

    // Для номенклатуры
    @SerializedName("nomenclatureId")
    public String nomenclatureId;

    @SerializedName("nomenclatureName")
    public String nomenclatureName;

    @SerializedName("totalQuantity")
    public int totalQuantity;

    @SerializedName("reserveQuantity")
    public int reserveQuantity;

    @SerializedName("freeQuantity")
    public int freeQuantity;

    @SerializedName("inTransitQuantity")
    public int inTransitQuantity;

    // Для серии
    @SerializedName("seriyaId")
    public String seriyaId;

    @SerializedName("seriya")
    public String seriya;

    @SerializedName("imei")
    public String imei;

    // Серии для номенклатуры (только для TYPE_NOMENCLATURE)
    @SerializedName("series")
    public List<Seriya> series;

    public List<StockItem> children;
    public boolean expanded = false;

    // Конструктор склада
    public static StockItem warehouse(String id, String name) {
        StockItem item = new StockItem();
        item.type = TYPE_WAREHOUSE;
        item.warehouseId = id;
        item.warehouseName = name;
        item.children = new java.util.ArrayList<>();
        return item;
    }

    // Конструктор номенклатуры
    public static StockItem nomenclature(String id, String name, int quantity, int reserveQuantity, int freeQuantity, int inTransitQuantity, List<Seriya> series) {
        StockItem item = new StockItem();
        item.type = TYPE_NOMENCLATURE;
        item.nomenclatureId = id;
        item.nomenclatureName = name;
        item.totalQuantity = quantity;
        item.reserveQuantity = reserveQuantity;
        item.freeQuantity = freeQuantity;
        item.inTransitQuantity = inTransitQuantity;
        item.series = series;
        item.children = new java.util.ArrayList<>();
        return item;
    }

    // Конструктор серии
    public static StockItem series(Seriya seriya) {
        StockItem item = new StockItem();
        item.type = TYPE_SERIES;
        item.seriyaId = seriya.getId();
        item.seriya = seriya.getName();
        item.imei = seriya.getImei();
        return item;
    }

//    public String getSeriesText() {
//        if (series == null || series.isEmpty()) {
//            return "Серии: нет";
//        }
//
//        StringBuilder sb = new StringBuilder("Серии: ");
//        for (int i = 0; i < Math.min(series.size(), 3); i++) {
//            if (i > 0) sb.append(", ");
//            sb.append(series.get(i).getName());
//        }
//        if (series.size() > 3) {
//            sb.append("... (+").append(series.size() - 3).append(")");
//        }
//        return sb.toString();
//    }
public String getSeriesText() {
    if (series == null || series.isEmpty()) {
        return "Серии: нет";
    }

    StringBuilder sb = new StringBuilder("Серии: ");
    for (int i = 0; i < Math.min(series.size(), 3); i++) {
        if (i > 0) sb.append(", ");
        sb.append(series.get(i).getName());
    }
    if (series.size() > 3) {
        sb.append("... (+").append(series.size() - 3).append(")");
    }
    return sb.toString();
}

    public String getQuantityText() {
        // Используем перенос строки (\n), чтобы "В пути" было заметно
        if (inTransitQuantity > 0) {
            return String.format("Всего: %d | Рез: %d | Своб: %d\nВ пути: %d",
                    totalQuantity, reserveQuantity, freeQuantity, inTransitQuantity);
        } else {
            return String.format("Всего: %d | Рез: %d | Своб: %d",
                    totalQuantity, reserveQuantity, freeQuantity);
        }
    }
}
