package com.example.inventory_app.models;

import com.google.gson.annotations.SerializedName;
import java.util.Map;

public class LogRecord {
    @SerializedName("date")
    private String date; // ISO формат

    @SerializedName("level")
    private String level; // "INFO", "ERROR", "DEBUG"

    @SerializedName("source")
    private String source; // Например, "MainActivity"

    @SerializedName("event")
    private String event; // Короткое название события

    @SerializedName("description")
    private String description; // Подробности

    @SerializedName("context")
    private Map<String, Object> context; // Дополнительные данные

    public LogRecord(String date, String level, String source, String event, String description, Map<String, Object> context) {
        this.date = date;
        this.level = level;
        this.source = source;
        this.event = event;
        this.description = description;
        this.context = context;
    }
}