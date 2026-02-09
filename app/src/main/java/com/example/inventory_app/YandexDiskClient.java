package com.example.inventory_app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.util.concurrent.TimeUnit;

public class YandexDiskClient {
    private static final String BASE_URL = "https://cloud-api.yandex.net/";
    private static final String CLIENT_ID = "";
    private static final String CLIENT_SECRET = "";
    private static final String REDIRECT_URI = "https://oauth.yandex.ru/verification_code";

    private static YandexDiskService yandexDiskService;
    private static String authToken;

    // Метод для инициализации OAuth авторизации
    public static void initializeOAuth(AppCompatActivity activity, OAuthCallback callback) {
        RemoteLogger.init(activity);
        RemoteLogger.info("YandexDiskClient", "InitializeOAuth",
                "Инициализация OAuth авторизации");

        SharedPreferences prefs = activity.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        String savedToken = prefs.getString("YANDEX_DISK_TOKEN", null);

        if (savedToken != null && !savedToken.isEmpty()) {
            // Токен уже есть, используем его
            setAuthToken(savedToken);
            callback.onSuccess();
        } else {
            // Нужна авторизация
            startOAuthFlow(activity, callback);
        }
    }

    private static void startOAuthFlow(AppCompatActivity activity, OAuthCallback callback) {
        RemoteLogger.info("YandexDiskClient", "StartOAuthFlow",
                "Запуск OAuth потока, client_id: " + CLIENT_ID);

        String authUrl = "https://oauth.yandex.ru/authorize?" +
                "response_type=token" +
                "&client_id=" + CLIENT_ID +
                "&redirect_uri=" + REDIRECT_URI;

        RemoteLogger.info("YandexDiskClient", "OAuthUrl",
                "OAuth URL: " + authUrl);

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(authUrl));
        RemoteLogger.info("YandexDiskClient", "StartOAuthActivity",
                "Запуск браузера для OAuth");
        activity.startActivity(intent);

        // Здесь нужно обработать возврат с токеном
        // На практике лучше использовать WebView или глубокие ссылки
    }

    public static void setAuthToken(String token) {
        if (token == null) {
            RemoteLogger.error("YandexDiskClient", "SetAuthTokenNull",
                    "Попытка установить null токен", null);
            return;
        }

        authToken = token;
        RemoteLogger.info("YandexDiskClient", "AuthTokenSet",
                "Токен установлен, длина: " + token.length());

        // При установке нового токена сбрасываем сервис
        yandexDiskService = null;
        RemoteLogger.info("YandexDiskClient", "ServiceReset",
                "Сервис сброшен после установки нового токена");
    }

    public static String getAuthToken() {
        return authToken;
    }

    public static YandexDiskService getYandexDiskService() {
        RemoteLogger.info("YandexDiskClient", "GetService",
                "Запрос YandexDiskService, текущий сервис: " +
                        (yandexDiskService != null ? "существует" : "null"));

        if (yandexDiskService == null) {
            RemoteLogger.info("YandexDiskClient", "ServiceCreation",
                    "Создание нового сервиса");
            if (authToken == null || authToken.isEmpty()) {
                RemoteLogger.error("YandexDiskClient", "NoAuthToken",
                        "Токен не установлен. Необходимо вызвать setAuthToken()", null);
                throw new IllegalStateException("Yandex.Disk token not set. Call setAuthToken() first.");
            }

            long startTime = System.currentTimeMillis();

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(chain -> {
                        Request original = chain.request();
                        Request.Builder requestBuilder = original.newBuilder()
                                .header("Authorization", "OAuth " + authToken)
                                .header("Content-Type", "application/json")
                                .method(original.method(), original.body());

                        Request request = requestBuilder.build();

                        RemoteLogger.info("YandexDiskClient", "NetworkRequest",
                                "Запрос: " + request.method() + " " + request.url());

                        long requestTime = System.currentTimeMillis();
                        Response response = chain.proceed(request);
                        long responseTime = System.currentTimeMillis() - requestTime;

                        RemoteLogger.info("YandexDiskClient", "NetworkResponse",
                                "Ответ: " + response.code() + " " + response.message() +
                                        ", время: " + responseTime + "мс");

                        return response;
                    })
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            yandexDiskService = retrofit.create(YandexDiskService.class);

            long creationTime = System.currentTimeMillis() - startTime;
            RemoteLogger.info("YandexDiskClient", "ServiceCreated",
                    "YandexDiskService создан за " + creationTime + "мс, базовый URL: " + BASE_URL);
        } else {
            RemoteLogger.info("YandexDiskClient", "ServiceReuse",
                    "Используется существующий сервис");

        }
        return yandexDiskService;
    }

    public interface OAuthCallback {
        void onSuccess();
        void onError(String error);
    }
}
