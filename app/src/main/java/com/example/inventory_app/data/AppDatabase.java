package com.example.inventory_app.data;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {PendingDocument.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract PendingDocumentDao pendingDocumentDao();
}
