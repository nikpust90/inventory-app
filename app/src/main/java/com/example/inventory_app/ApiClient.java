package com.example.inventory_app;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Клиент для работы с сервером через Retrofit.
 */
public class ApiClient {
    private static final String BASE_URL = "http://yourserver.com/api/"; // URL сервера
    private static Retrofit retrofit = null;

    /**
     * Возвращает экземпляр API для взаимодействия с сервером.
     */
    public static ApiService getInstance() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(ApiService.class);
    }
}