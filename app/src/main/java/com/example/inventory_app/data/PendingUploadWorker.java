package com.example.inventory_app.data;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import androidx.room.Room;

import com.example.inventory_app.ApiClient;
import com.example.inventory_app.ApiService;
import com.example.inventory_app.data.AppDatabase;
import com.example.inventory_app.data.PendingDocument;
import com.example.inventory_app.models.InventoryDocument;
import com.google.gson.Gson;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PendingUploadWorker extends Worker {

    private static final String TAG = "PendingUploadWorker";
    private final ApiService apiService;
    private final AppDatabase db;
    private final Gson gson = new Gson();

    public PendingUploadWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        apiService = ApiClient.getRetrofitInstance().create(ApiService.class);
        db = Room.databaseBuilder(context, AppDatabase.class, "local_queue_db").build();
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            List<PendingDocument> pendingDocs = db.pendingDocumentDao().getAll();
            Log.i(TAG, "WorkManager: найдено " + pendingDocs.size() + " документов");

            for (PendingDocument pd : pendingDocs) {
                InventoryDocument doc = gson.fromJson(pd.documentJson, InventoryDocument.class);
                if (doc == null) {
                    Log.w(TAG, "Некорректный JSON, удаляем id=" + pd.id);
                    db.pendingDocumentDao().deleteById(pd.id);
                    continue;
                }

                apiService.updateInventoryDocument(doc).enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (response.isSuccessful()) {
                            Log.i(TAG, "Документ успешно отправлен id=" + pd.id);
                            new Thread(() -> db.pendingDocumentDao().deleteById(pd.id)).start();
                        } else {
                            Log.w(TAG, "Ошибка сервера при отправке id=" + pd.id + " code=" + response.code());
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        Log.w(TAG, "Ошибка сети при отправке id=" + pd.id + " : " + t.getMessage());
                    }
                });
            }

            // Возвращаем успешный результат — WorkManager сам решит, когда запустить снова
            return Result.success();

        } catch (Exception e) {
            Log.e(TAG, "Ошибка фоновой отправки", e);
            return Result.retry();
        }
    }
}
