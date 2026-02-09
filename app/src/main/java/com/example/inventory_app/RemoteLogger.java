package com.example.inventory_app;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import com.example.inventory_app.models.LogRecord;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RemoteLogger {

    // ⚠️ УКАЖИТЕ ВАШ URL
    private static final String BASE_URL = "http://45.155.207.231:8000/";

    private static LoggerApi loggerApi;
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static Context appContext;

    // Кэшируем версию приложения, чтобы не запрашивать каждый раз
    private static String cachedAppVersion = null;

    public static void init(Context context) {
        if (appContext == null) {
            appContext = context.getApplicationContext();
        }
    }

    private static LoggerApi getApi() {
        if (loggerApi == null) {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            loggerApi = retrofit.create(LoggerApi.class);
        }
        return loggerApi;
    }

    // Вспомогательный метод для получения версии приложения
    private static String getAppVersion() {
        if (cachedAppVersion != null) return cachedAppVersion;
        try {
            if (appContext != null) {
                PackageInfo pInfo = appContext.getPackageManager().getPackageInfo(appContext.getPackageName(), 0);
                cachedAppVersion = pInfo.versionName + " (" + pInfo.versionCode + ")";
            }
        } catch (PackageManager.NameNotFoundException e) {
            cachedAppVersion = "unknown";
        }
        return cachedAppVersion;
    }

    public static void log(String level, String source, String event, String description, Map<String, Object> context) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US);
        String currentDate = sdf.format(new Date());

        if (context == null) {
            context = new HashMap<>();
        }

        // --- АВТОМАТИЧЕСКОЕ ЗАПОЛНЕНИЕ CONTEXT ---

        // 1. Данные пользователя (из SharedPreferences)
        if (appContext != null) {
            SharedPreferences prefs = appContext.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);

            // Имя пользователя
            context.put("user_name", prefs.getString("USER_NAME", "Guest"));

            // Является ли админом
            context.put("is_admin", prefs.getBoolean("IS_ADMIN", false));

            // Список складов (преобразуем Set в String для читаемости)
            Set<String> warehouses = prefs.getStringSet("WAREHOUSE_ID_LIST", null);
            if (warehouses != null) {
                context.put("warehouses", warehouses.toString());
            }
        }

        // 2. Технические данные устройства
        context.put("device_model", Build.MODEL);           // Например: Zebra TC21
        context.put("device_manuf", Build.MANUFACTURER);    // Например: Zebra
        context.put("android_ver", Build.VERSION.RELEASE);  // Например: 10
        context.put("sdk_int", Build.VERSION.SDK_INT);      // Например: 29

        // 3. Версия приложения (Важно для отслеживания обновлений!)
        context.put("app_version", getAppVersion());

        // -----------------------------------------

        LogRecord record = new LogRecord(currentDate, level, source, event, description, context);

        executor.execute(() -> {
            try {
                getApi().sendLog(record).enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        // Silent fail
                    }
                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        Log.e("RemoteLogger", "Error", t);
                    }
                });
            } catch (Exception e) {
                Log.e("RemoteLogger", "Wrapper Error", e);
            }
        });
    }

    public static void info(String source, String event, String description) {
        log("INFO", source, event, description, null);
    }

    public static void error(String source, String event, String description, Throwable t) {
        String fullDescription = description;
        if (t != null) {
            fullDescription += " | Exception: " + t.getMessage();
        }
        log("ERROR", source, event, fullDescription, null);
    }

    public static void warn(String source, String event, String description) {
        log("WARN", source, event, description, null);
    }

    public static void warn(String source, String event, String description, Map<String, Object> context) {
        log("WARN", source, event, description, context);
    }
}
