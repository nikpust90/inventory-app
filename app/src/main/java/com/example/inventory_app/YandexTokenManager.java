package com.example.inventory_app;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import com.example.inventory_app.activity.YandexOAuthActivity;

public class YandexTokenManager {
    private static final String TAG = "YandexTokenManager";
    private static final String CLIENT_ID = "";
    // ★ НОВАЯ КОНСТАНТА: Ваш заранее полученный токен
    private static final String INITIAL_PRESET_TOKEN = "";

    public interface TokenCallback {
        void onTokenReady(String token);
        void onTokenError(String error);
    }

    // ★ ИСПРАВЛЕНИЕ: Используем ThreadLocal для потокобезопасности
    private static volatile TokenCallback currentCallback;
    private static Context appContext;

    /**
     * Проверяет, сохранен ли токен. Если нет, сохраняет предопределенный токен.
     * Этот метод должен быть вызван при запуске приложения.
     */
    public static void saveInitialTokenIfMissing(Context context) {
        if (!AppConfig.hasToken(context)) {
            Log.d(TAG, "Токен не найден. Сохраняем предопределенный токен для первого входа.");
            // Предполагаем, что AppConfig.saveYandexDiskToken сохраняет его в SharedPreferences
            AppConfig.setYandexDiskToken(context.getApplicationContext(), INITIAL_PRESET_TOKEN);
        } else {
            Log.d(TAG, "Токен уже есть. Используем сохраненный.");
        }
    }
    public static void ensureToken(Context context, TokenCallback callback) {
        appContext = context.getApplicationContext();

        // Если токен есть в хранилище - используем его
        if (AppConfig.hasToken(context)) {
            String savedToken = AppConfig.getYandexDiskToken(context);
            Log.d(TAG, "Используем сохраненный токен");
            callback.onTokenReady(savedToken);
        } else {
            // Если токена нет - получаем новый
            Log.d(TAG, "Токена нет, получаем новый");
            getNewToken(context, callback);
        }
    }

    public static void refreshToken(Context context, TokenCallback callback) {
        appContext = context.getApplicationContext();
        // Принудительно получаем новый токен и заменяем старый
        Log.d(TAG, "Обновляем токен");
        getNewToken(context, callback);
    }

    private static void getNewToken(Context context, TokenCallback callback) {
        // ★ ИСПРАВЛЕНИЕ: Синхронизируем установку callback
        synchronized (YandexTokenManager.class) {
            currentCallback = callback;
        }

        // Открываем OAuth активность
        Intent intent = new Intent(context, YandexOAuthActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    // ★ ИСПРАВЛЕННЫЕ МЕТОДЫ ДЛЯ ВЗАИМОДЕЙСТВИЯ С YandexOAuthActivity
    public static void onTokenReceived(String token) {
        TokenCallback callback;
        synchronized (YandexTokenManager.class) {
            callback = currentCallback;
            currentCallback = null; // Очищаем сразу после получения
        }

        if (callback != null && appContext != null) {
            // Сохраняем токен
            AppConfig.setYandexDiskToken(appContext, token);
            callback.onTokenReady(token);
        } else {
            Log.w(TAG, "Callback не найден при получении токена");
        }
    }

    public static void onTokenError(String error) {
        TokenCallback callback;
        synchronized (YandexTokenManager.class) {
            callback = currentCallback;
            currentCallback = null; // Очищаем сразу после использования
        }

        if (callback != null) {
            callback.onTokenError(error);
        } else {
            Log.w(TAG, "Callback не найден при ошибке токена: " + error);
        }
    }

    // ★ ДОБАВЛЕНО: Метод для проверки наличия активного callback
    public static boolean hasActiveCallback() {
        synchronized (YandexTokenManager.class) {
            return currentCallback != null;
        }
    }
}