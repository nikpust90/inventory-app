package com.example.inventory_app.data;

import android.content.Context;
import android.util.Log;
import androidx.room.Room;
import com.example.inventory_app.RemoteLogger;

public class AppDatabaseSingleton {

    private static volatile AppDatabase INSTANCE;
    private static final String LOG_TAG = "Database";
    private static final String TAG = "AppDatabaseSingleton";

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    try {
                        RemoteLogger.info(LOG_TAG, "Init", "Инициализация локальной базы данных (Room)...");
                        INSTANCE = Room.databaseBuilder(
                                        context.getApplicationContext(),
                                        AppDatabase.class,
                                        "local_queue_db"
                                )
                                .fallbackToDestructiveMigration() // Удаляет и пересоздает БД при изменении версии
                                .build();
                        RemoteLogger.info(LOG_TAG, "Init", "База данных успешно создана/открыта.");
                        Log.i(TAG, "Database created successfully");
                    } catch (Exception e) {
                        RemoteLogger.error(LOG_TAG, "InitError", "Критическая ошибка создания БД: " + e.getMessage(), null);
                        Log.e(TAG, "Error creating database", e);
                        throw new RuntimeException("Failed to create database", e);
                    }
                }
            }
        }
        return INSTANCE;
    }
}
