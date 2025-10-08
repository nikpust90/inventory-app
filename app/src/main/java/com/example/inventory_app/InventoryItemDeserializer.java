package com.example.inventory_app;

import com.google.gson.*;

import java.lang.reflect.Type;

public class InventoryItemDeserializer implements JsonDeserializer<InventoryItem> {
    @Override
    public InventoryItem deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {

        JsonObject obj = json.getAsJsonObject();

        InventoryItem item = new InventoryItem();

        // Собираем Nomenklatura
        Nomenklatura n = new Nomenklatura();
        n.setId(obj.get("nomenklaturaId").getAsString());
        n.setName(obj.get("nomenklatura").getAsString());
        item.setNomenklatura(n);

        // Собираем Seriya
        Seriya s = new Seriya();
        s.setId(obj.get("seriyaId").getAsString());
        s.setName(obj.get("seriya").getAsString());
        item.setSeriya(s);

        // Остальные поля
        item.setKolichestvo(obj.get("kolichestvo").getAsInt());
        item.setKolichestvoFakt(obj.get("kolichestvoFakt").getAsInt());

        return item;
    }
}
