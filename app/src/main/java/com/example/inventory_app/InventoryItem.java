package com.example.inventory_app;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Модель данных для элемента инвентаризации.
 */
public class InventoryItem implements Serializable {
    private String nomenklatura; // Название товара
    private String seriya;       // Серия, которую мы будем сканировать
    private int kolichestvo;     // План
    private int kolichestvoFakt; // Факт (изначально 0)

    public <E> InventoryItem(ArrayList<E> es) {
    }

    // Геттеры и сеттеры
    public String getNomenklatura() { return nomenklatura; }
    public void setNomenklatura(String nomenklatura) { this.nomenklatura = nomenklatura; }
    public String getSeriya() { return seriya; }
    public void setSeriya(String seriya) { this.seriya = seriya; }
    public int getKolichestvo() { return kolichestvo; }
    public void setKolichestvo(int kolichestvo) { this.kolichestvo = kolichestvo; }
    public int getKolichestvoFakt() { return kolichestvoFakt; }
    public void setKolichestvoFakt(int kolichestvoFakt) { this.kolichestvoFakt = kolichestvoFakt; }
}
