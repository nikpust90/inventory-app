package com.example.inventory_app.data;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.widget.Toast;

import androidx.room.Room;

import com.example.inventory_app.ApiService;

import com.example.inventory_app.models.InventoryDocument;
import com.google.gson.Gson;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LocalQueueManager {

    private static final String TAG = "LocalQueueManager";

    private final Context context;
    private final AppDatabase db;
    private final ApiService apiService;
    private final Gson gson = new Gson();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public LocalQueueManager(Context context, ApiService apiService) {
        this.context = context.getApplicationContext();
        this.apiService = apiService;
        this.db = AppDatabaseSingleton.getInstance(context); // безопасно, Singleton
        // Создаем базу напрямую
//        this.db = Room.databaseBuilder(context.getApplicationContext(),
//                        AppDatabase.class, "local_queue_db")
//                .fallbackToDestructiveMigration()
//                .build();

    }

    // Проверка сети (простая)
    private boolean isNetworkAvailable() {
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return false;
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnected();
        } catch (Exception e) {
            Log.e(TAG, "isNetworkAvailable error", e);
            return false;
        }
    }

    // Сохранение документа в очередь (Room)
    public void saveDocument(InventoryDocument document) {
        executor.submit(() -> {
            try {
                PendingDocument pd = new PendingDocument();
                pd.documentJson = gson.toJson(document);
                pd.timestamp = System.currentTimeMillis();
                db.pendingDocumentDao().insert(pd);
                Log.i(TAG, "Document saved offline, id: " + pd.id);
            } catch (Exception e) {
                Log.e(TAG, "saveDocument error", e);
            }
        });
    }

    // Отправка одного документа из очереди (десериализуем в InventoryDocument)
    private void sendDocument(PendingDocument pendingDoc) {
        try {
            final InventoryDocument parsedDoc = gson.fromJson(pendingDoc.documentJson, InventoryDocument.class);
            if (parsedDoc == null) {
                Log.e(TAG, "Parsed document is null, skipping id=" + pendingDoc.id);
                // при некорректном JSON — удаляем запись, чтобы не застревала навсегда
                executor.submit(() -> db.pendingDocumentDao().deleteById(pendingDoc.id));
                return;
            }

            apiService.updateInventoryDocument(parsedDoc).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) {
                        // Удаляем запись из БД в фоновом потоке
                        executor.submit(() -> {
                            try {
                                db.pendingDocumentDao().deleteById(pendingDoc.id);
                                Log.i(TAG, "Pending doc sent and removed, id=" + pendingDoc.id);
                            } catch (Exception e) {
                                Log.e(TAG, "Error deleting pending doc id=" + pendingDoc.id, e);
                            }
                        });
                    } else {
                        Log.w(TAG, "Server returned error for pending doc id=" + pendingDoc.id + " code=" + response.code());
                        // Оставляем в очереди — повторим позже
                    }
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    Log.w(TAG, "Failed to send pending doc id=" + pendingDoc.id + " : " + t.getMessage());
                    // Оставляем в очереди — повторим позже
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "sendDocument error for id=" + pendingDoc.id, e);
        }
    }

    // Попытка отправить все документы из очереди
    public void trySendAll() {
        if (!isNetworkAvailable()) return;

        executor.submit(() -> {
            try {
                List<PendingDocument> docs = db.pendingDocumentDao().getAll();
                Log.i(TAG, "Found " + (docs == null ? 0 : docs.size()) + " pending docs");
                if (docs != null) {
                    for (PendingDocument pd : docs) {
                        sendDocument(pd);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "trySendAll error", e);
            }
        });
    }

    // Пытаемся отправить документ сразу, иначе сохраняем в очередь
    public void sendOrQueue(InventoryDocument document) {
        if (document == null) return;

        if (!isNetworkAvailable()) {
            saveDocument(document);
            Toast.makeText(context, "Интернета нет — документ сохранён локально", Toast.LENGTH_LONG).show();
            return;
        }

        apiService.updateInventoryDocument(document).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(context, "Документ отправлен", Toast.LENGTH_SHORT).show();
                } else {
                    // Сохраняем в очередь при ошибке
                    saveDocument(document);
                    Toast.makeText(context, "Ошибка сервера — документ сохранён локально", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                saveDocument(document);
                Toast.makeText(context, "Ошибка сети — документ сохранён локально", Toast.LENGTH_LONG).show();
            }
        });
    }

    // Закрытие executor (по желанию)
    public void shutdown() {
        try {
            executor.shutdownNow();
        } catch (Exception e) {
            Log.e(TAG, "shutdown error", e);
        }
    }
}
