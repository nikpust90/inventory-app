package com.example.inventory_app.data;

import android.content.Context;
import android.util.Log;
import androidx.room.Room;

public class AppDatabaseSingleton {

    private static volatile AppDatabase INSTANCE;
    private static final String TAG = "AppDatabaseSingleton";

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    try {
                        INSTANCE = Room.databaseBuilder(
                                        context.getApplicationContext(),
                                        AppDatabase.class,
                                        "local_queue_db"
                                )
                                .fallbackToDestructiveMigration() // Удаляет и пересоздает БД при изменении версии
                                .build();
                        Log.i(TAG, "Database created successfully");
                    } catch (Exception e) {
                        Log.e(TAG, "Error creating database", e);
                        throw new RuntimeException("Failed to create database", e);
                    }
                }
            }
        }
        return INSTANCE;
    }
}
