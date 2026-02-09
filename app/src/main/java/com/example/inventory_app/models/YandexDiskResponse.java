package com.example.inventory_app.models;

import androidx.room.Embedded;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class YandexDiskResponse {


    @SerializedName("href")
    private String href;

    @SerializedName("method")
    private String method;

    @SerializedName("templated")
    private boolean templated;

    @SerializedName("public_url")
    private String publicUrl;

    @SerializedName("public_key")
    private String publicKey;

    // --- Поля для публичных ресурсов и скачивания (НОВЫЕ) ---

    // Ссылка на скачивание файла (приходит внутри элемента списка)
    @SerializedName("file")
    private String file;

    // Имя файла (app-release.apk)
    @SerializedName("name")
    private String name;

    // Вложенная структура для папок (содержит список файлов)
    @SerializedName("_embedded")
    private Embedded embedded;

    @SerializedName("type")
    private String type;

    // Геттеры и сеттеры
    // Геттеры и сеттеры для новых полей
    public String getFile() { return file; }
    public void setFile(String file) { this.file = file; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Embedded getEmbedded() { return embedded; }
    public void setEmbedded(Embedded embedded) { this.embedded = embedded; }
    public String getHref() { return href; }
    public void setHref(String href) { this.href = href; }

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    public boolean isTemplated() { return templated; }
    public void setTemplated(boolean templated) { this.templated = templated; }

    public String getPublicUrl() { return publicUrl; }
    public void setPublicUrl(String publicUrl) { this.publicUrl = publicUrl; }

    public String getPublicKey() { return publicKey; }
    public void setPublicKey(String publicKey) { this.publicKey = publicKey; }


    /**
     * Внутренний класс для обработки структуры папки (_embedded)
     */
    public static class Embedded {
        @SerializedName("items")
        private List<YandexDiskResponse> items;

        public List<YandexDiskResponse> getItems() { return items; }
        public void setItems(List<YandexDiskResponse> items) { this.items = items; }
    }
}
