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
        String authUrl = "https://oauth.yandex.ru/authorize?" +
                "response_type=token" +
                "&client_id=" + CLIENT_ID +
                "&redirect_uri=" + REDIRECT_URI;

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(authUrl));
        activity.startActivity(intent);

        // Здесь нужно обработать возврат с токеном
        // На практике лучше использовать WebView или глубокие ссылки
    }

    public static void setAuthToken(String token) {
        authToken = token;
    }

    public static String getAuthToken() {
        return authToken;
    }

    public static YandexDiskService getYandexDiskService() {
        if (yandexDiskService == null) {
            if (authToken == null || authToken.isEmpty()) {
                throw new IllegalStateException("Yandex.Disk token not set. Call setAuthToken() first.");
            }

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(chain -> {
                        Request original = chain.request();
                        Request.Builder requestBuilder = original.newBuilder()
                                .header("Authorization", "OAuth " + authToken)
                                .header("Content-Type", "application/json")
                                .method(original.method(), original.body());

                        Request request = requestBuilder.build();
                        return chain.proceed(request);
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
        }
        return yandexDiskService;
    }

    public interface OAuthCallback {
        void onSuccess();
        void onError(String error);
    }
}
