package com.example.inventory_app.data;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "pending_documents")
public class PendingDocument {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "document_json")
    public String documentJson;

    @ColumnInfo(name = "timestamp")
    public long timestamp;
}
