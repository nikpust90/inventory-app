package com.example.inventory_app;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.example.inventory_app.activity.CarSchemeActivity;
import com.example.inventory_app.activity.InventoryListActivity;
import com.example.inventory_app.activity.StockReportActivity;
import com.example.inventory_app.databinding.ActivityMainBinding;
import com.example.inventory_app.models.YandexDiskResponse;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

import com.example.inventory_app.BuildConfig;


import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;



public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private static final int REQUEST_INSTALL_PERMISSION = 1001;
    private long downloadId = -1;
    private boolean isReceiverRegistered = false;
    // Новые переменные для обновления
    private View updateNotificationCard;
    private Button btnUpdateNow;

    private static final String PUBLIC_KEY = "https://disk.yandex.ru/d/1mjJd-bZoF1nUw";
    private static final String YANDEX_DISK_LINK = "https://disk.yandex.ru/d/1mjJd-bZoF1nUw";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // --- ВСТАВИТЬ ЭТОТ БЛОК ---
        // Инициализация уведомления
        updateNotificationCard = findViewById(R.id.updateNotificationCard);
        btnUpdateNow = findViewById(R.id.btnUpdateNow);

        // Кнопка в уведомлении запускает ту же проверку обновлений, что и основная кнопка
        btnUpdateNow.setOnClickListener(v -> checkForUpdates());

        // Запускаем тихую проверку версии при старте
        checkAppVersion();
        // --------------------------

        // --- ДОБАВИТЬ ЭТУ СТРОКУ ---
        RemoteLogger.init(this);
        // ---------------------------

        // Теперь можно логировать, имя пользователя подтянется само
        RemoteLogger.info("MainActivity", "AppStart", "Приложение запущено");

        // Регистрируем receiver сразу при создании
        registerDownloadReceiver();

        binding.viewInventoryListButton.setOnClickListener(v -> {
            RemoteLogger.info("MainActivity", "ButtonClick", "Переход к списку инвентаризаций");
            startActivity(new Intent(MainActivity.this, InventoryListActivity.class));
        });

        binding.viewStockButton.setOnClickListener(v -> {
            RemoteLogger.info("MainActivity", "ButtonClick", "Переход к отчету остатков");
            startActivity(new Intent(MainActivity.this, StockReportActivity.class));
        });

        binding.viewSchemesButton.setOnClickListener(v -> {
            RemoteLogger.info("MainActivity", "ButtonClick", "Переход к поиску схем");
            // Запускаем новую активность (её код ниже)
            startActivity(new Intent(MainActivity.this, CarSchemeActivity.class));
        });

        binding.updateAppButton.setOnClickListener(v -> {
            RemoteLogger.info("MainActivity", "ButtonClick", "Нажали проверку обновлений");
            checkForUpdates();
        });

        // Новая кнопка для открытия Яндекс.Диска
        binding.openYandexDiskButton.setOnClickListener(v -> {
            RemoteLogger.info("MainActivity", "ButtonClick", "Нажали на кнопку Открытие Яндекс.Диска");
            openYandexDiskLink();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        RemoteLogger.info("MainActivity", "onResume", "MainActivity возобновлено");
        registerDownloadReceiver();
    }

    private void registerDownloadReceiver() {
        if (!isReceiverRegistered) {
            try {
                IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
                filter.addAction(DownloadManager.ACTION_NOTIFICATION_CLICKED);

                // Используем ContextCompat для правильной регистрации на всех версиях Android
                ContextCompat.registerReceiver(
                        this,
                        onDownloadComplete,
                        filter,
                        ContextCompat.RECEIVER_NOT_EXPORTED
                );

                isReceiverRegistered = true;
                Log.d("MainActivity", "BroadcastReceiver registered successfully");
                RemoteLogger.info("MainActivity", "ReceiverRegistered", "BroadcastReceiver зарегистрирован");
            } catch (Exception e) {
                Log.e("MainActivity", "Error registering receiver", e);
                RemoteLogger.error("MainActivity", "ReceiverError", "Ошибка регистрации BroadcastReceiver", e);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        RemoteLogger.info("MainActivity", "ActivityPause", "MainActivity приостановлено");
        unregisterDownloadReceiver();
        downloadHandler.removeCallbacks(downloadStatusChecker);
    }

    // Метод для открытия ссылки на Яндекс.Диск
    private void openYandexDiskLink() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(YANDEX_DISK_LINK));
            startActivity(intent);
            RemoteLogger.info("MainActivity", "YandexDiskOpen", "Ссылка на Яндекс.Диск открыта");
        } catch (Exception e) {
            Toast.makeText(this, "Не удалось открыть ссылку", Toast.LENGTH_SHORT).show();
            Log.e("MainActivity", "Error opening Yandex Disk link", e);
            RemoteLogger.error("MainActivity", "YandexDiskError", "Ошибка открытия Яндекс.Диска", e);
        }
    }

    private void unregisterDownloadReceiver() {
        if (isReceiverRegistered) {
            try {
                unregisterReceiver(onDownloadComplete);
                isReceiverRegistered = false;
                Log.d("MainActivity", "BroadcastReceiver unregistered");
                RemoteLogger.info("MainActivity", "ReceiverUnregistered", "BroadcastReceiver отрегистрирован");
            } catch (Exception e) {
                Log.e("MainActivity", "Error unregistering receiver", e);
                RemoteLogger.error("MainActivity", "ReceiverUnregisterError", "Ошибка отрегистрации BroadcastReceiver", e);
            }
        }
    }

    private void checkForUpdates() {
        RemoteLogger.info("MainActivity", "UpdateCheck", "Начало проверки обновлений");
        Toast.makeText(this, "Проверка обновлений...", Toast.LENGTH_SHORT).show();

        YandexDiskPublicClient.getPublicService()
                .getPublicResource(PUBLIC_KEY, "_embedded.items.name,_embedded.items.file,_embedded.items.type")
                .enqueue(new Callback<YandexDiskResponse>() {
                    @Override
                    public void onResponse(Call<YandexDiskResponse> call, Response<YandexDiskResponse> response) {
                        if (!response.isSuccessful() || response.body() == null) {
                            RemoteLogger.error("MainActivity", "checkForUpdates", "Ошибка API Яндекс.Диска: код " + response.code(), null);
                            Toast.makeText(MainActivity.this, "Ошибка API Яндекс.Диска", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        YandexDiskResponse data = response.body();

                        if (data.getEmbedded() == null || data.getEmbedded().getItems() == null) {
                            RemoteLogger.info("MainActivity", "checkForUpdates", "Папка Яндекс.Диска пуста");
                            Toast.makeText(MainActivity.this, "Папка пуста", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        for (YandexDiskResponse item : data.getEmbedded().getItems()) {
                            if (item.getName() != null && item.getName().endsWith(".apk")) {
                                if (item.getFile() != null) {
                                    RemoteLogger.info("MainActivity", "checkForUpdates", "Найден APK: " + item.getName());
                                    downloadApk(item.getFile(), item.getName());
                                    return;
                                }
                            }
                        }

                        RemoteLogger.info("MainActivity", "checkForUpdates", "APK не найден в папке");
                        Toast.makeText(MainActivity.this, "APK не найден", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(Call<YandexDiskResponse> call, Throwable t) {
                        RemoteLogger.error("MainActivity", "checkForUpdates", "Сетевая ошибка при проверке обновлений", t);
                        Toast.makeText(MainActivity.this, "Ошибка сети: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void downloadApk(String url, String fileName) {
        RemoteLogger.info("MainActivity", "downloadApk", "Скачивание APK: " + fileName);
        Toast.makeText(this, "Скачивание обновления...", Toast.LENGTH_LONG).show();

        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName);
        if (file.exists()) file.delete();

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setTitle("Обновление Inventory App");
        request.setDescription("Скачивание " + fileName);
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
        request.setMimeType("application/vnd.android.package-archive");

        DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        downloadId = dm.enqueue(request);

        RemoteLogger.info("MainActivity", "DownloadQueued", "Скачивание поставлено в очередь, ID: " + downloadId);
        Log.d("Download", "Download started with ID: " + downloadId);

        // Запускаем периодическую проверку статуса как fallback
        startDownloadStatusChecker();
    }

    private Handler downloadHandler = new Handler();
    private Runnable downloadStatusChecker = new Runnable() {
        @Override
        public void run() {
            checkDownloadStatus();
        }
    };

    private void startDownloadStatusChecker() {
        // Проверяем статус каждые 3 секунды
        downloadHandler.postDelayed(downloadStatusChecker, 3000);
        RemoteLogger.info("MainActivity", "DownloadCheckerStart", "Запущен проверщик статуса скачивания");
    }

    private void checkDownloadStatus() {
        if (downloadId == -1) return;

        DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(downloadId);

        try (Cursor cursor = dm.query(query)) {
            if (cursor != null && cursor.moveToFirst()) {
                int status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
                Log.d("DownloadCheck", "Download status: " + status);
                RemoteLogger.info("MainActivity", "startDownloadStatusChecker", "Статус скачивания: " + status);

                switch (status) {
                    case DownloadManager.STATUS_SUCCESSFUL:
                        RemoteLogger.info("MainActivity", "DownloadSuccess", "Скачивание успешно завершено");
                        Log.d("DownloadCheck", "Download completed successfully - installing");
                        downloadHandler.removeCallbacks(downloadStatusChecker);
                        installApk();
                        break;
                    case DownloadManager.STATUS_FAILED:
                        int reason = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON));
                        Log.e("DownloadCheck", "Download failed: " + reason);
                        String errorMsg = getDownloadErrorReason(reason);
                        RemoteLogger.error("MainActivity", "DownloadFailed", "Ошибка скачивания: " + errorMsg + " (код: " + reason + ")", null);
                        Toast.makeText(this, "Ошибка скачивания: " + getDownloadErrorReason(reason), Toast.LENGTH_SHORT).show();
                        downloadHandler.removeCallbacks(downloadStatusChecker);
                        break;
                    case DownloadManager.STATUS_RUNNING:
                        Log.d("DownloadCheck", "Download still running - checking again in 3 seconds");
                        downloadHandler.postDelayed(downloadStatusChecker, 3000);
                        break;
                    case DownloadManager.STATUS_PENDING:
                        Log.d("DownloadCheck", "Download pending - checking again in 3 seconds");
                        downloadHandler.postDelayed(downloadStatusChecker, 3000);
                        break;
                }
            } else {
                Log.d("DownloadCheck", "Download not found in manager - checking again in 3 seconds");
                downloadHandler.postDelayed(downloadStatusChecker, 3000);
            }
        } catch (Exception e) {
            RemoteLogger.error("MainActivity", "DownloadCheckError", "Ошибка проверки статуса скачивания", e);
            Log.e("DownloadCheck", "Error checking download status", e);
            downloadHandler.postDelayed(downloadStatusChecker, 3000);
        }
    }

    private String getDownloadErrorReason(int reason) {
        switch (reason) {
            case DownloadManager.ERROR_CANNOT_RESUME:
                return "Невозможно возобновить загрузку";
            case DownloadManager.ERROR_DEVICE_NOT_FOUND:
                return "Устройство хранения не найдено";
            case DownloadManager.ERROR_FILE_ALREADY_EXISTS:
                return "Файл уже существует";
            case DownloadManager.ERROR_FILE_ERROR:
                return "Ошибка файловой системы";
            case DownloadManager.ERROR_HTTP_DATA_ERROR:
                return "Ошибка HTTP данных";
            case DownloadManager.ERROR_INSUFFICIENT_SPACE:
                return "Недостаточно места";
            case DownloadManager.ERROR_TOO_MANY_REDIRECTS:
                return "Слишком много перенаправлений";
            case DownloadManager.ERROR_UNHANDLED_HTTP_CODE:
                return "Необработанный HTTP код";
            default:
                return "Неизвестная ошибка: " + reason;
        }
    }

    private final BroadcastReceiver onDownloadComplete = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("DownloadReceiver", "onReceive called");
            RemoteLogger.info("MainActivity", "DownloadReceiver", "BroadcastReceiver получил событие");

            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            Log.d("DownloadReceiver", "Received download ID: " + id + ", expected: " + downloadId);

            if (id == downloadId) {
                RemoteLogger.info("MainActivity", "DownloadReceiver", "Совпадение ID, начало установки");
                Log.d("DownloadReceiver", "Starting installation...");
                installApk();
            } else {
                RemoteLogger.info("MainActivity", "DownloadReceiver", "ID не совпадает, получено: " + id + ", ожидалось: " + downloadId);
                Log.d("DownloadReceiver", "Download ID doesn't match");
            }
        }
    };

    private void installApk() {
        RemoteLogger.info("MainActivity", "InstallStart", "Начало установки APK");
        // Проверяем разрешение на установку из неизвестных источников
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!getPackageManager().canRequestPackageInstalls()) {
                RemoteLogger.info("MainActivity", "InstallPermission", "Запрос разрешения на установку из неизвестных источников");
                Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                        .setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, REQUEST_INSTALL_PERMISSION);
                return;
            }
        }
        DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);

        // Получаем информацию о скачанном файле через DownloadManager
        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(downloadId);

        try (Cursor cursor = dm.query(query)) {
            if (cursor != null && cursor.moveToFirst()) {
                int status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));

                if (status == DownloadManager.STATUS_SUCCESSFUL) {
                    // Получаем путь к файлу
                    String filePath = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI));

                    if (filePath != null) {
                        Uri fileUri = Uri.parse(filePath);
                        installApkFromUri(fileUri);
                    } else {
                        RemoteLogger.error("MainActivity", "InstallError", "Путь к файлу не найден", null);
                        Toast.makeText(this, "Ошибка: путь к файлу не найден", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "Скачивание не завершено", Toast.LENGTH_SHORT).show();
                    RemoteLogger.error("MainActivity", "InstallError", "Скачивание не завершено, статус: " + status, null);
                }
            } else {
                Toast.makeText(this, "Файл не найден в DownloadManager", Toast.LENGTH_SHORT).show();
                RemoteLogger.error("MainActivity", "InstallError", "Файл не найден в DownloadManager", null);
            }
        } catch (Exception e) {
            Log.e("MainActivity", "Ошибка установки APK", e);
            RemoteLogger.error("MainActivity", "InstallError", "Ошибка установки APK", e);
            Toast.makeText(this, "Ошибка установки: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void installApkFromUri(Uri uri) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                // Для Android 7+ используем FileProvider
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                // Создаем файл из URI
                String filePath = getPathFromUri(uri);
                if (filePath != null) {
                    File file = new File(filePath);
                    Uri contentUri = FileProvider.getUriForFile(
                            this,
                            getPackageName() + ".provider",
                            file
                    );
                    intent.setDataAndType(contentUri, "application/vnd.android.package-archive");
                } else {
                    // Альтернативный способ
                    intent.setDataAndType(uri, "application/vnd.android.package-archive");
                }
            } else {
                // Для старых версий Android
                intent.setDataAndType(uri, "application/vnd.android.package-archive");
            }

            // Проверяем, есть ли activity для установки
            if (intent.resolveActivity(getPackageManager()) != null) {
                RemoteLogger.info("MainActivity", "InstallLaunch", "Запуск установки APK");
                startActivity(intent);
            } else {
                RemoteLogger.error("MainActivity", "InstallError", "Не найдено приложение для установки APK", null);
                Toast.makeText(this, "Не найдено приложение для установки APK", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e("MainActivity", "Ошибка запуска установки", e);
            RemoteLogger.error("MainActivity", "InstallError", "Ошибка запуска установки", e);
            Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private String getPathFromUri(Uri uri) {
        if (uri == null) return null;

        String scheme = uri.getScheme();
        if (scheme == null) {
            return uri.getPath();
        }

        if (scheme.equals("file")) {
            return uri.getPath();
        } else if (scheme.equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri,
                    new String[]{MediaStore.Downloads.DATA}, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Downloads.DATA);
                    return cursor.getString(columnIndex);
                }
            } catch (Exception e) {
                RemoteLogger.error("MainActivity", "PathError", "Ошибка получения пути из URI", e);
                Log.e("MainActivity", "Ошибка получения пути", e);
            }
        }

        return null;
    }

    // ---------------------------------------------------
    // ЛОГИКА ПРОВЕРКИ ВЕРСИИ (VERSION.TXT)
    // ---------------------------------------------------

    private void checkAppVersion() {
        RemoteLogger.info("MainActivity", "VersionCheck", "Проверяем файл версии...");

        // 1. Получаем список файлов, чтобы найти прямую ссылку на version.txt
        YandexDiskPublicClient.getPublicService()
                .getPublicResource(PUBLIC_KEY, "_embedded.items.name,_embedded.items.file")
                .enqueue(new Callback<YandexDiskResponse>() {
                    @Override
                    public void onResponse(Call<YandexDiskResponse> call, Response<YandexDiskResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            findAndReadVersionFile(response.body());
                        }
                    }
                    @Override
                    public void onFailure(Call<YandexDiskResponse> call, Throwable t) {
                        // Ошибки игнорируем, чтобы не спамить пользователю при старте
                    }
                });
    }

    private void findAndReadVersionFile(YandexDiskResponse data) {
        if (data.getEmbedded() == null || data.getEmbedded().getItems() == null) return;

        String versionFileUrl = null;
        // Ищем файл с точным именем "version.txt"
        for (YandexDiskResponse item : data.getEmbedded().getItems()) {
            if ("version.txt".equals(item.getName())) {
                versionFileUrl = item.getFile();
                break;
            }
        }

        if (versionFileUrl != null) {
            downloadAndCompareVersion(versionFileUrl);
        }
    }

    private void downloadAndCompareVersion(String url) {
        // Скачиваем сам текстовый файл
        YandexDiskPublicClient.getPublicService().downloadFile(url).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        // Читаем первую строку из файла
                        BufferedReader reader = new BufferedReader(new InputStreamReader(response.body().byteStream()));
                        String versionStr = reader.readLine();

                        if (versionStr != null && !versionStr.isEmpty()) {
                            // Парсим версию с сервера
                            int serverVersion = Integer.parseInt(versionStr.trim());
                            // Получаем текущую версию приложения (из build.gradle)
                            int currentVersion = BuildConfig.VERSION_CODE;

                            RemoteLogger.info("MainActivity", "VersionCheck",
                                    "Server: " + serverVersion + " / App: " + currentVersion);

                            // Если на сервере версия больше -> показываем карточку
                            if (serverVersion > currentVersion) {
                                runOnUiThread(() -> updateNotificationCard.setVisibility(View.VISIBLE));
                            }
                        }
                    } catch (Exception e) {
                        RemoteLogger.error("MainActivity", "VersionError", "Ошибка чтения version.txt", e);
                    }
                }
            }
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {}
        });
    }
}
