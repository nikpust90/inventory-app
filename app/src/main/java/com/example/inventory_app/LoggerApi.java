package com.example.inventory_app;

import com.example.inventory_app.models.LogRecord;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface LoggerApi {
    @POST("android/log") // Соответствует @app.post("/log") в Python
    Call<Void> sendLog(@Body LogRecord record);
}
