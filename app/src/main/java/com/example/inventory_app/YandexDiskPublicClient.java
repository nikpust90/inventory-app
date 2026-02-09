package com.example.inventory_app;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.util.concurrent.TimeUnit;

public class YandexDiskPublicClient {
    private static final String BASE_URL = "https://cloud-api.yandex.net/";

    private static YandexDiskService yandexDiskService;

    public static YandexDiskService getPublicService() {
        if (yandexDiskService == null) {

            RemoteLogger.info("YandexDiskPublicClient", "ServiceCreation",
                    "Создание YandexDiskService");

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(new OkHttpClient.Builder()
                            .connectTimeout(30, TimeUnit.SECONDS)
                            .readTimeout(30, TimeUnit.SECONDS)
                            .build())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            yandexDiskService = retrofit.create(YandexDiskService.class);
            RemoteLogger.info("YandexDiskPublicClient", "ServiceCreated",
                    "YandexDiskService создан, базовый URL: " + BASE_URL);
        }
        return yandexDiskService;
    }
}
