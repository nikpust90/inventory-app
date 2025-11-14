package com.example.inventory_app.models;

import com.google.gson.annotations.SerializedName;

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

    // Геттеры и сеттеры
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
}
