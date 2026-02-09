package com.example.inventory_app;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.util.concurrent.TimeUnit;

public class CarApiClient {

    // Твой IP адрес Python-сервера
    private static final String BASE_URL = "http://45.155.207.231:8000/";

    private static Retrofit retrofit = null;

    public static CarApiService getService() {
        if (retrofit == null) {
            // 1. Логирование (чтобы видеть в Logcat, что отправляется и приходит)
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            // 2. Настраиваем OkHttp
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(logging) // Добавляем логгер
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build();

            // 3. Создаем Retrofit
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create()) // Стандартный GSON
                    .build();
        }
        return retrofit.create(CarApiService.class);
    }
}