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
        RemoteLogger.init(context);
        if (!AppConfig.hasToken(context)) {
            RemoteLogger.info("YandexTokenManager", "SaveInitialToken",
                    "Токен не найден. Сохраняем предопределенный токен для первого входа.");
            Log.d(TAG, "Токен не найден. Сохраняем предопределенный токен для первого входа.");
            // Предполагаем, что AppConfig.saveYandexDiskToken сохраняет его в SharedPreferences
            AppConfig.setYandexDiskToken(context.getApplicationContext(), INITIAL_PRESET_TOKEN);
            RemoteLogger.info("YandexTokenManager", "TokenSaved",
                    "Предопределенный токен сохранен");
        } else {
            RemoteLogger.info("YandexTokenManager", "TokenExists",
                    "Токен уже есть. Используем сохраненный.");
            Log.d(TAG, "Токен уже есть. Используем сохраненный.");
        }
    }
    public static void ensureToken(Context context, TokenCallback callback) {
        appContext = context.getApplicationContext();

        RemoteLogger.info("YandexTokenManager", "EnsureToken",
                "Проверка наличия токена");

        // Если токен есть в хранилище - используем его
        if (AppConfig.hasToken(context)) {
            String savedToken = AppConfig.getYandexDiskToken(context);
            RemoteLogger.info("YandexTokenManager", "TokenFound",
                    "Используем сохраненный токен");
            Log.d(TAG, "Используем сохраненный токен");
            callback.onTokenReady(savedToken);
        } else {
            // Если токена нет - получаем новый
            RemoteLogger.warn("YandexTokenManager", "TokenNotFound",
                    "Токена нет, получаем новый");
            Log.d(TAG, "Токена нет, получаем новый");
            getNewToken(context, callback);
        }
    }

    public static void refreshToken(Context context, TokenCallback callback) {
        RemoteLogger.init(context);
        appContext = context.getApplicationContext();
        RemoteLogger.info("YandexTokenManager", "RefreshToken",
                "Обновляем токен");
        // Принудительно получаем новый токен и заменяем старый
        Log.d(TAG, "Обновляем токен");
        getNewToken(context, callback);
    }

    private static void getNewToken(Context context, TokenCallback callback) {
        RemoteLogger.info("YandexTokenManager", "GetNewToken",
                "Получение нового токена");

        // ★ ИСПРАВЛЕНИЕ: Синхронизируем установку callback
        synchronized (YandexTokenManager.class) {
            currentCallback = callback;
            RemoteLogger.info("YandexTokenManager", "CallbackSet",
                    "Callback установлен");
        }

        // Открываем OAuth активность
        Intent intent = new Intent(context, YandexOAuthActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        RemoteLogger.info("YandexTokenManager", "StartOAuth",
                "Запуск YandexOAuthActivity");
        context.startActivity(intent);
    }

    // ★ ИСПРАВЛЕННЫЕ МЕТОДЫ ДЛЯ ВЗАИМОДЕЙСТВИЯ С YandexOAuthActivity
    public static void onTokenReceived(String token) {
        RemoteLogger.info("YandexTokenManager", "TokenReceived",
                "Токен получен от OAuthActivity");

        TokenCallback callback;
        synchronized (YandexTokenManager.class) {
            callback = currentCallback;
            currentCallback = null; // Очищаем сразу после получения
            RemoteLogger.info("YandexTokenManager", "CallbackCleared",
                    "Callback очищен после получения токена");
        }

        if (callback != null && appContext != null) {
            // Сохраняем токен
            RemoteLogger.info("YandexTokenManager", "SavingToken",
                    "Сохранение токена в AppConfig");
            AppConfig.setYandexDiskToken(appContext, token);
            RemoteLogger.info("YandexTokenManager", "TokenSavedSuccess",
                    "Токен успешно сохранен");
            callback.onTokenReady(token);
        } else {
            RemoteLogger.warn("YandexTokenManager", "NoCallbackOnToken",
                    "Callback не найден при получении токена");
            Log.w(TAG, "Callback не найден при получении токена");
        }
    }

    public static void onTokenError(String error) {
        RemoteLogger.error("YandexTokenManager", "TokenErrorReceived",
                "Ошибка получения токена от OAuthActivity: " + error, null);

        TokenCallback callback;
        synchronized (YandexTokenManager.class) {
            callback = currentCallback;
            currentCallback = null; // Очищаем сразу после использования
            RemoteLogger.warn("YandexTokenManager", "CallbackClearedError",
                    "Callback очищен после ошибки");
        }

        if (callback != null) {
            RemoteLogger.info("YandexTokenManager", "CallbackError",
                    "Передача ошибки в callback: " + error);
            callback.onTokenError(error);
        } else {
            RemoteLogger.warn("YandexTokenManager", "NoCallbackOnError",
                    "Callback не найден при ошибке токена: " + error);
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