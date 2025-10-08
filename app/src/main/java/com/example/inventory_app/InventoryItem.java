package com.example.inventory_app;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Модель данных для элемента инвентаризации.
 */
public class InventoryItem implements Serializable {
    private Nomenklatura nomenklatura; // Объект номенклатуры с id и name
    private Seriya seriya;             // Объект серии с id и name
    private int kolichestvo;           // План
    private int kolichestvoFakt;       // Факт (изначально 0)

    // Новый флаг
    private boolean found;

    public boolean isFound() {
        return found;
    }

    public void setFound(boolean found) {
        this.found = found;
    }

    // Геттеры и сеттеры
    public Nomenklatura getNomenklatura() { return nomenklatura; }
    public void setNomenklatura(Nomenklatura nomenklatura) { this.nomenklatura = nomenklatura; }

    public Seriya getSeriya() { return seriya; }
    public void setSeriya(Seriya seriya) { this.seriya = seriya; }

    public int getKolichestvo() { return kolichestvo; }
    public void setKolichestvo(int kolichestvo) { this.kolichestvo = kolichestvo; }

    public int getKolichestvoFakt() { return kolichestvoFakt; }
    public void setKolichestvoFakt(int kolichestvoFakt) { this.kolichestvoFakt = kolichestvoFakt; }
}
