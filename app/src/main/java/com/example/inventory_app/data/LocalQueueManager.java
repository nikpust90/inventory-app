package com.example.inventory_app.data;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.widget.Toast;

import androidx.room.Room;

import com.example.inventory_app.ApiService;

import com.example.inventory_app.RemoteLogger;
import com.example.inventory_app.models.InventoryDocument;
import com.google.gson.Gson;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LocalQueueManager {

    private static final String TAG = "1QueueManager";
    private static final String LOG_TAG = "QueueManager";

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
            RemoteLogger.error(LOG_TAG, "NetworkCheck", "Ошибка проверки сети: " + e.getMessage(), null);
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
                RemoteLogger.warn(LOG_TAG, "OfflineSave",
                        "Документ сохранен в локальную БД (офлайн). ID документа: " + document.getId());
            } catch (Exception e) {
                RemoteLogger.error(LOG_TAG, "OfflineSaveError", "Ошибка сохранения в БД: " + e.getMessage(), null);
                Log.e(TAG, "saveDocument error", e);
            }
        });
    }

    // Отправка одного документа из очереди (десериализуем в InventoryDocument)
    private void sendDocument(PendingDocument pendingDoc) {
        try {
            final InventoryDocument parsedDoc = gson.fromJson(pendingDoc.documentJson, InventoryDocument.class);
            if (parsedDoc == null) {
                RemoteLogger.error(LOG_TAG, "QueueProcessing", "Ошибка: JSON пустой или битый. Удаляем запись ID=" + pendingDoc.id, null);
                Log.e(TAG, "Parsed document is null, skipping id=" + pendingDoc.id);
                // при некорректном JSON — удаляем запись, чтобы не застревала навсегда
                executor.submit(() -> db.pendingDocumentDao().deleteById(pendingDoc.id));
                return;
            }

            RemoteLogger.info(LOG_TAG, "QueueProcessing", "Попытка отправить отложенный документ: " + parsedDoc.getId());

            apiService.updateInventoryDocument(parsedDoc).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) {
                        // Удаляем запись из БД в фоновом потоке
                        executor.submit(() -> {
                            try {
                                db.pendingDocumentDao().deleteById(pendingDoc.id);
                                RemoteLogger.info(LOG_TAG, "QueueSuccess",
                                        "Отложенный документ успешно отправлен и удален из БД. ID=" + pendingDoc.id);
                                Log.i(TAG, "Pending doc sent and removed, id=" + pendingDoc.id);
                            } catch (Exception e) {
                                RemoteLogger.error(LOG_TAG, "QueueCleanupError", "Ошибка удаления записи: " + e.getMessage(), null);
                                Log.e(TAG, "Error deleting pending doc id=" + pendingDoc.id, e);
                            }
                        });
                    } else {
                        RemoteLogger.warn(LOG_TAG, "QueueFailure",
                                "Сервер вернул ошибку " + response.code() + ". Документ оставлен в очереди.");
                        Log.w(TAG, "Server returned error for pending doc id=" + pendingDoc.id + " code=" + response.code());
                        // Оставляем в очереди — повторим позже
                    }

                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    RemoteLogger.warn(LOG_TAG, "QueueFailure",
                            "Ошибка сети при отправке из очереди: " + t.getMessage());
                    Log.w(TAG, "Failed to send pending doc id=" + pendingDoc.id + " : " + t.getMessage());
                    // Оставляем в очереди — повторим позже
                }
            });
        } catch (Exception e) {
            RemoteLogger.error(LOG_TAG, "QueueError", "Критическая ошибка отправки: " + e.getMessage(), null);
            Log.e(TAG, "sendDocument error for id=" + pendingDoc.id, e);
        }
    }

    // Попытка отправить все документы из очереди
    public void trySendAll() {
        if (!isNetworkAvailable()) {
            RemoteLogger.info(LOG_TAG, "SyncSkipped", "Нет сети, пропускаем синхронизацию очереди.");
            return;
        }

        executor.submit(() -> {
            try {
                List<PendingDocument> docs = db.pendingDocumentDao().getAll();
                Log.i(TAG, "Found " + (docs == null ? 0 : docs.size()) + " pending docs");
//                if (docs != null) {
//                    for (PendingDocument pd : docs) {
//                        sendDocument(pd);
//                    }
//                }
                int count = (docs == null) ? 0 : docs.size();

                if (count > 0) {
                    RemoteLogger.info(LOG_TAG, "SyncStart", "Найдено документов в очереди: " + count + ". Начинаем отправку.");
                    for (PendingDocument pd : docs) {
                        sendDocument(pd);
                    }
                } else {
                    // Можно закомментировать, чтобы не спамило
                    // RemoteLogger.info(LOG_TAG, "SyncEmpty", "Очередь пуста.");
                }
            } catch (Exception e) {
                RemoteLogger.error(LOG_TAG, "SyncError", "Ошибка чтения очереди: " + e.getMessage(), null);
                Log.e(TAG, "trySendAll error", e);
            }
        });
    }

    // Пытаемся отправить документ сразу, иначе сохраняем в очередь
    public void sendOrQueue(InventoryDocument document) {
        if (document == null) return;
        String docId = document.getId();

        if (!isNetworkAvailable()) {
            RemoteLogger.warn(LOG_TAG, "DirectSend", "Интернета нет. Сохраняем документ " + docId + " локально.");
            saveDocument(document);
            Toast.makeText(context, "Интернета нет — документ сохранён локально", Toast.LENGTH_LONG).show();
            return;
        }

        RemoteLogger.info(LOG_TAG, "DirectSend", "Интернет есть. Пробуем отправить документ " + docId + " напрямую.");

        apiService.updateInventoryDocument(document).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    RemoteLogger.info(LOG_TAG, "DirectSendSuccess", "Документ " + docId + " успешно отправлен на сервер.");
                    Toast.makeText(context, "Документ отправлен", Toast.LENGTH_SHORT).show();
                } else {
                    // Сохраняем в очередь при ошибке
                    RemoteLogger.warn(LOG_TAG, "DirectSendFail",
                            "Ошибка сервера (" + response.code() + "). Сохраняем " + docId + " в очередь.");
                    saveDocument(document);
                    Toast.makeText(context, "Ошибка сервера — документ сохранён локально", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                RemoteLogger.warn(LOG_TAG, "DirectSendFail",
                        "Сбой сети (" + t.getMessage() + "). Сохраняем " + docId + " в очередь.");
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
