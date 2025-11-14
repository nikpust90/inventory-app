package com.example.inventory_app;

import android.content.Context;
import android.content.SharedPreferences;

public class AppConfig {
    private static final String PREF_NAME = "AppConfig";
    private static final String KEY_YANDEX_TOKEN = "yandex_disk_token";

    public static String getYandexDiskToken(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_YANDEX_TOKEN, null); // null если токена нет
    }

    public static void setYandexDiskToken(Context context, String token) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_YANDEX_TOKEN, token).apply();
    }

    public static boolean hasToken(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String token = prefs.getString(KEY_YANDEX_TOKEN, null);
        return token != null && !token.isEmpty();
    }

    public static void clearToken(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().remove(KEY_YANDEX_TOKEN).apply();
    }
}
