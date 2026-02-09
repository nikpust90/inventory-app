package com.example.inventory_app;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.example.inventory_app.models.YandexDiskResponse;
import com.example.inventory_app.models.InventoryDocument;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class YandexDiskHelper {
    private static final String TAG = "YandexDiskHelper";

    public interface UploadCallback {
        void onSuccess(String folderUrl);
        void onError(String error);
        void onProgress(int progress);
    }

    public static void uploadPhotosToYandexDisk(Context context, Uri[] photoUris,
                                                String userName, List<InventoryDocument> documents, // <-- ИЗМЕНЕНО
                                                UploadCallback callback) {

        RemoteLogger.init(context);
        RemoteLogger.info("YandexDiskHelper", "UploadStart",
                "Начало загрузки " + (photoUris != null ? photoUris.length : 0) +
                        " фото, пользователь: " + userName +
                        ", документов: " + (documents != null ? documents.size() : 0));

        // Получаем токен (автоматически если нужно)
        YandexTokenManager.ensureToken(context, new YandexTokenManager.TokenCallback() {
            @Override
            public void onTokenReady(String token) {
                RemoteLogger.info("YandexDiskHelper", "TokenReady",
                        "Токен получен успешно, начинаем загрузку");
                // Устанавливаем токен и начинаем загрузку
                YandexDiskClient.setAuthToken(token);
                proceedWithUpload(context, photoUris, userName, documents, callback, token, 1); // <-- ИЗМЕНЕНО
            }

            @Override
            public void onTokenError(String error) {
                RemoteLogger.error("YandexDiskHelper", "TokenError",
                        "Не удалось получить токен: " + error, null);
                callback.onError("Не удалось получить токен: " + error);
            }
        });
    }

    private static void proceedWithUpload(Context context, Uri[] photoUris,
                                          String userName, List<InventoryDocument> documents, // <-- ИЗМЕНЕНО
                                          UploadCallback callback,
                                          String token, int attempt) {

        // --- НАЧАЛО: Новая логика генерации имени папки ---
        RemoteLogger.info("YandexDiskHelper", "ProceedUpload",
                "Продолжение загрузки, попытка: " + attempt +
                        ", токен доступен: " + (token != null && !token.isEmpty()));


        String documentsInfo;
        if (documents == null || documents.isEmpty()) {
            documentsInfo = "no_docs";
            RemoteLogger.info("YandexDiskHelper", "FolderNameGen",
                    "Документы отсутствуют, используем 'no_docs'");
        } else {
            StringBuilder sb = new StringBuilder();
            int count = documents.size();

            RemoteLogger.info("YandexDiskHelper", "FolderNameGen",
                    "Генерация имени папки из " + count + " документов");

            if (count <= 3) {
                // Формат: "№1 от 2024-10-20_№2 от 2024-10-21"
                RemoteLogger.info("YandexDiskHelper", "FolderNameFormat",
                        "Используем формат для <=3 документов");

                for (int i = 0; i < count; i++) {
                    InventoryDocument doc = documents.get(i);

                    String docNumber = doc.getDocumentNumber();
                    if (docNumber == null) docNumber = ""; // Делаем его безопасным

                    // УБРАТЬ ПРЕФИКС ИЗ НОМЕРА (все до "-")
                    if (docNumber.contains("-")) {
                        docNumber = docNumber.substring(docNumber.indexOf("-") + 1);
                        RemoteLogger.info("YandexDiskHelper", "DocNumberProcess",
                                "Очищен номер документа: " + docNumber);
                    }

                    // Очищаем дату (убираем время, если оно есть)
                    String datePart = doc.getDate();
                    if (datePart != null && datePart.contains("T")) {
                        datePart = datePart.split("T")[0];
                        RemoteLogger.info("YandexDiskHelper", "DateProcess",
                                "Очищена дата документа: " + datePart);
                    }

                    sb.append(docNumber); // "000040" вместо "НФНФ-000040"
                    sb.append("от");
                    sb.append(datePart);             // "2024-11-13"

                    if (i < count - 1) {
                        sb.append("_"); // Разделитель
                    }
                }
            } else {
                // Формат (больше 3-х док-ов): "№1_№2_№3_№4"
                RemoteLogger.info("YandexDiskHelper", "FolderNameFormat",
                        "Используем упрощенный формат для >3 документов");
                for (int i = 0; i < count; i++) {
                    InventoryDocument doc = documents.get(i);
                    String docNumber = doc.getDocumentNumber();

                    // УБРАТЬ ПРЕФИКС ИЗ НОМЕРА (все до "-")
                    if (docNumber != null && docNumber.contains("-")) {
                        docNumber = docNumber.substring(docNumber.indexOf("-") + 1);
                    }

                    sb.append(docNumber);
                    if (i < count - 1) {
                        sb.append("_");
                    }
                }
            }

            documentsInfo = sb.toString();
            RemoteLogger.info("YandexDiskHelper", "DocumentsInfo",
                    "Сформированная информация о документах: " + documentsInfo);
        }

        // Получаем текущую дату в формате ГГГГ-ММ-ДД
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String currentDate = sdf.format(new Date());

        // Собираем финальное имя: "inv_ИмяПользователя_ИнформацияОДокументах_ТекущаяДата"
        String folderName = ("inv_" + userName + "_" + documentsInfo + "_tek_date_" + currentDate)
                .replaceAll("[\\\\/:*?\"<>|]", "_");

        RemoteLogger.info("YandexDiskHelper", "FolderNameFinal",
                "Финальное имя папки: " + folderName);

        // Очистка имени от символов, запрещенных в Яндекс.Диске
        //folderName = folderName.replaceAll("[\\\\/:*?\"<>|]", "_");

        // --- КОНЕЦ: Новая логика ---

        String folderPath = "/" + folderName;
        YandexDiskService service = YandexDiskClient.getYandexDiskService();

        RemoteLogger.info("YandexDiskHelper", "CreateFolderRequest",
                "Создание папки на Яндекс.Диске: " + folderPath);

        service.createFolder(folderPath).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful() || response.code() == 409) {
                    RemoteLogger.warn("YandexDiskHelper", "FolderExists",
                            "Папка уже существует (код 409): " + folderPath);
                    // 409 (Conflict) означает, что папка с таким именем уже существует. Это нормально.
                    uploadPhotosToFolder(context, photoUris, folderPath, 0, callback, folderName);
                } else if (response.code() == 401 && attempt == 1) {
                    // ★ Токен невалидный - получаем новый и пробуем снова
                    RemoteLogger.warn("YandexDiskHelper", "TokenInvalid",
                            "Токен невалидный (код 401), получаем новый...");
                    Log.w(TAG, "Токен невалидный, получаем новый...");
                    YandexTokenManager.refreshToken(context, new YandexTokenManager.TokenCallback() {
                        @Override
                        public void onTokenReady(String newToken) {
                            RemoteLogger.info("YandexDiskHelper", "TokenRefreshed",
                                    "Токен успешно обновлен");
                            YandexDiskClient.setAuthToken(newToken);
                            // Передаем новые данные в рекурсивный вызов
                            proceedWithUpload(context, photoUris, userName, documents, callback, newToken, attempt + 1); // <-- ИЗМЕНЕНО
                        }

                        @Override
                        public void onTokenError(String error) {
                            RemoteLogger.error("YandexDiskHelper", "TokenRefreshError",
                                    "Не удалось обновить токен: " + error, null);
                            callback.onError("Не удалось обновить токен: " + error);
                        }
                    });
                } else {
                    String errorBody = "Неизвестно";
                    try {
                        if(response.errorBody() != null) errorBody = response.errorBody().string();
                    } catch (Exception e) {
                        RemoteLogger.error("YandexDiskHelper", "ErrorBodyRead",
                                "Ошибка чтения тела ошибки", e);
                    }

                    RemoteLogger.error("YandexDiskHelper", "CreateFolderError",
                            "Ошибка создания папки: код " + response.code() + ", тело: " + errorBody, null);
                    Log.e(TAG, "Ошибка создания папки: " + response.code() + " | " + errorBody);
                    callback.onError("Ошибка создания папки: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                RemoteLogger.error("YandexDiskHelper", "CreateFolderNetworkError",
                        "Ошибка сети при создании папки", t);
                callback.onError("Ошибка сети (создание папки): " + t.getMessage());
            }
        });
    }

    private static void uploadPhotosToFolder(Context context, Uri[] photoUris,
                                             String folderPath, int index,
                                             UploadCallback callback, String folderName) {
        if (index >= photoUris.length) {
            // ★ ВСЕ ФОТО ЗАГРУЖЕНЫ - ТЕПЕРЬ ВЫЗЫВАЕМ ПУБЛИКАЦИЮ
            RemoteLogger.info("YandexDiskHelper", "AllPhotosUploaded",
                    "Все " + photoUris.length + " фото загружены, начинаем публикацию папки");
            Log.d(TAG, "Все фото загружены, начинаем публикацию папки");
            publishFolderAndGetUrl(folderPath, callback);
            return;
        }

        Uri photoUri = photoUris[index];
//        String fileName = "photo_" + (index + 1) + ".jpg";
//        String filePath = folderPath + "/" + fileName;
        // ==========================================================
        // ★ НАЧАЛО: ИСПОЛЬЗОВАНИЕ УНИКАЛЬНОГО ИМЕНИ ФАЙЛА
        // ==========================================================

        // 1. Создаем метку времени: YYYYMMDD_HHMMSS
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());

        // 2. Используем уникальное имя: photo_20251114_080020_1.jpg
        String fileName = "photo_" + timestamp + "_" + (index + 1) + ".jpg";
        String filePath = folderPath + "/" + fileName;

        RemoteLogger.info("YandexDiskHelper", "PhotoUploadStart",
                "Загрузка фото " + (index + 1) + "/" + photoUris.length +
                        ", имя файла: " + fileName);

        // ==========================================================
        // ★ КОНЕЦ: ИСПОЛЬЗОВАНИЕ УНИКАЛЬНОГО ИМЕНИ ФАЙЛА
        // ==========================================================

        try {
            InputStream inputStream = context.getContentResolver().openInputStream(photoUri);
            if (inputStream == null) {
                RemoteLogger.error("YandexDiskHelper", "FileOpenError",
                        "Не удалось открыть файл: " + photoUri, null);
                callback.onError("Не удалось открыть файл: " + photoUri);
                return;
            }

            // Создаем временный файл
            File tempFile = File.createTempFile("upload", ".jpg", context.getCacheDir());
            RemoteLogger.info("YandexDiskHelper", "TempFileCreated",
                    "Временный файл создан: " + tempFile.getAbsolutePath());
            FileOutputStream outputStream = new FileOutputStream(tempFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            int totalBytes = 0;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
                totalBytes += bytesRead;
            }

            inputStream.close();
            outputStream.close();

            RemoteLogger.info("YandexDiskHelper", "TempFileSize",
                    "Временный файл подготовлен, размер: " + totalBytes + " байт");

            // Получаем ссылку для загрузки
            YandexDiskService service = YandexDiskClient.getYandexDiskService();
            RemoteLogger.info("YandexDiskHelper", "GetUploadLink",
                    "Запрос ссылки для загрузки: " + filePath);
            service.getUploadLink(filePath).enqueue(new Callback<YandexDiskResponse>() {
                @Override
                public void onResponse(Call<YandexDiskResponse> call, Response<YandexDiskResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        String uploadUrl = response.body().getHref();
                        RemoteLogger.info("YandexDiskHelper", "UploadLinkReceived",
                                "Ссылка для загрузки получена успешно");

                        // Загружаем файл
                        RequestBody requestBody = RequestBody.create(
                                MediaType.parse("image/jpeg"),
                                tempFile
                        );

                        RemoteLogger.info("YandexDiskHelper", "FileUploadStart",
                                "Начало загрузки файла на Яндекс.Диск");

                        service.uploadFile(uploadUrl, requestBody).enqueue(new Callback<ResponseBody>() {
                            @Override
                            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                                tempFile.delete();
                                RemoteLogger.info("YandexDiskHelper", "TempFileDeleted",
                                        "Временный файл удален");

                                if (response.isSuccessful()) {
                                    // Обновляем прогресс
                                    int progress = (int) ((float) (index + 1) / photoUris.length * 100);
                                    RemoteLogger.info("YandexDiskHelper", "PhotoUploadSuccess",
                                            "Фото " + (index + 1) + " успешно загружено, прогресс: " + progress + "%");
                                    callback.onProgress(progress);

                                    // Загружаем следующее фото
                                    uploadPhotosToFolder(context, photoUris, folderPath, index + 1, callback, folderName);
                                } else {
                                    tempFile.delete();
                                    callback.onError("Ошибка загрузки файла: " + response.code());
                                }
                            }

                            @Override
                            public void onFailure(Call<ResponseBody> call, Throwable t) {
                                tempFile.delete();
                                RemoteLogger.error("YandexDiskHelper", "FileUploadError",
                                        "Ошибка загрузки файла: код " + response.code(), null);
                                callback.onError("Ошибка сети при загрузке: " + t.getMessage());
                            }
                        });
                    } else {
                        tempFile.delete();
                        RemoteLogger.error("YandexDiskHelper", "FileUploadNetworkError",
                                "Не удалось получить ссылку для загрузки: " + response.code(), null);
                        callback.onError("Не удалось получить ссылку для загрузки: " + response.code());
                    }
                }

                @Override
                public void onFailure(Call<YandexDiskResponse> call, Throwable t) {
                    tempFile.delete();
                    RemoteLogger.error("YandexDiskHelper", "FileUploadNetworkError",
                            "Ошибка сети при получении ссылки: " + t.getMessage(), t);
                    callback.onError("Ошибка сети при получении ссылки: " + t.getMessage());
                }
            });

        } catch (Exception e) {
            RemoteLogger.error("YandexDiskHelper", "UploadLinkError",
                    "Ошибка обработки файла: " + e.getMessage(), null);
            callback.onError("Ошибка обработки файла: " + e.getMessage());
        }
    }

    private static void publishFolderAndGetUrl(String folderPath, UploadCallback callback) {
        YandexDiskService service = YandexDiskClient.getYandexDiskService();

        RemoteLogger.info("YandexDiskHelper", "PublishFolder",
                "Публикация папки: " + folderPath);
        // ★ ПРАВИЛЬНЫЙ МЕТОД ДЛЯ ПУБЛИКАЦИИ ПАПКИ
        service.publishResource(folderPath).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    RemoteLogger.info("YandexDiskHelper", "FolderPublished",
                            "Папка успешно опубликована: " + folderPath);
                    Log.d(TAG, "Папка опубликована: " + folderPath);

                    // ★ ЖДЕМ 2 СЕКУНДЫ И ПОЛУЧАЕМ ПУБЛИЧНУЮ ССЫЛКУ
                    RemoteLogger.info("YandexDiskHelper", "WaitForPublicUrl",
                            "Ожидание 2 секунды перед получением публичной ссылки");

                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        getPublishedFolderUrl(folderPath, callback);
                    }, 2000);
                } else {
                    String errorMsg = "Ошибка публикации папки: " + response.code();
                    try {
                        if (response.errorBody() != null) {
                            errorMsg += " - " + response.errorBody().string();
                            RemoteLogger.error("YandexDiskHelper", "PublishError",
                                    errorMsg, null);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        RemoteLogger.error("YandexDiskHelper", "PublishErrorBodyRead",
                                "Ошибка чтения тела ошибки публикации", e);
                    }
                    Log.e(TAG, errorMsg);

                    // ★ ПРОБУЕМ ПОЛУЧИТЬ ССЫЛКУ ДАЖЕ ЕСЛИ ПУБЛИКАЦИЯ НЕ УДАЛАСЬ
                    RemoteLogger.warn("YandexDiskHelper", "TryGetUrlAnyway",
                            "Пробуем получить ссылку даже при ошибке публикации");
                    getPublishedFolderUrl(folderPath, callback);
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                String errorMsg = "Ошибка сети при публикации: " + t.getMessage();
                RemoteLogger.error("YandexDiskHelper", "PublishNetworkError", errorMsg, t);
                Log.e(TAG, errorMsg);

                // ★ ПРОБУЕМ ПОЛУЧИТЬ ССЫЛКУ ДАЖЕ ПРИ ОШИБКЕ СЕТИ
                RemoteLogger.warn("YandexDiskHelper", "TryGetUrlAfterNetworkError",
                        "Пробуем получить ссылку после сетевой ошибки");
                getPublishedFolderUrl(folderPath, callback);
            }
        });
    }

    // ★ ПРАВИЛЬНЫЙ МЕТОД ДЛЯ ПОЛУЧЕНИЯ ПУБЛИЧНОЙ ССЫЛКИ
    private static void getPublishedFolderUrl(String folderPath, UploadCallback callback) {
        YandexDiskService service = YandexDiskClient.getYandexDiskService();

        RemoteLogger.info("YandexDiskHelper", "GetPublicUrl",
                "Запрос публичной ссылки для папки: " + folderPath);

        // ★ ЗАПРАШИВАЕМ ИНФОРМАЦИЮ О РЕСУРСЕ С ПОЛЕМ public_url
        service.getResource(folderPath, "public_url").enqueue(new Callback<YandexDiskResponse>() {
            @Override
            public void onResponse(Call<YandexDiskResponse> call, Response<YandexDiskResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    YandexDiskResponse resource = response.body();
                    String publicUrl = resource.getPublicUrl();

                    if (publicUrl != null && !publicUrl.isEmpty()) {
                        RemoteLogger.info("YandexDiskHelper", "PublicUrlReceived",
                                "Публичная ссылка получена: " + publicUrl);
                        Log.d(TAG, "Публичная ссылка получена: " + publicUrl);
                        callback.onSuccess(publicUrl);
                    } else {
                        // ★ ЕСЛИ ПУБЛИЧНОЙ ССЫЛКИ НЕТ, ПРОБУЕМ ЕЩЕ РАЗ ЧЕРЕЗ 3 СЕКУНДЫ
                        RemoteLogger.warn("YandexDiskHelper", "PublicUrlNotReady",
                                "Публичная ссылка еще не готова, пробуем снова...");
                        Log.w(TAG, "Публичная ссылка еще не готова, пробуем снова...");
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            retryGetPublicUrl(folderPath, callback, 1);
                        }, 3000);
                    }
                } else {
                    String errorMsg = "Ошибка получения публичной ссылки: " + response.code();
                    RemoteLogger.error("YandexDiskHelper", "GetPublicUrlError", errorMsg, null);
                    Log.e(TAG, errorMsg);
                    createAlternativeLink(folderPath, callback);
                }
            }

            @Override
            public void onFailure(Call<YandexDiskResponse> call, Throwable t) {
                String errorMsg = "Ошибка сети при получении ссылки: " + t.getMessage();
                RemoteLogger.error("YandexDiskHelper", "GetPublicUrlNetworkError", errorMsg, t);
                Log.e(TAG, errorMsg);
                createAlternativeLink(folderPath, callback);
            }
        });
    }

    // ★ МЕТОД ПОВТОРНОЙ ПОПЫТКИ ПОЛУЧЕНИЯ ССЫЛКИ
    private static void retryGetPublicUrl(String folderPath, UploadCallback callback, int attempt) {
        RemoteLogger.info("YandexDiskHelper", "RetryGetPublicUrl",
                "Повторная попытка получения ссылки: " + attempt);

        if (attempt > 3) {
            RemoteLogger.warn("YandexDiskHelper", "MaxRetriesExceeded",
                    "Превышено количество попыток (" + attempt + "), создаем альтернативную ссылку");
            Log.w(TAG, "Превышено количество попыток, создаем альтернативную ссылку");
            createAlternativeLink(folderPath, callback);
            return;
        }

        YandexDiskService service = YandexDiskClient.getYandexDiskService();
        service.getResource(folderPath, "public_url").enqueue(new Callback<YandexDiskResponse>() {
            @Override
            public void onResponse(Call<YandexDiskResponse> call, Response<YandexDiskResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    YandexDiskResponse resource = response.body();
                    String publicUrl = resource.getPublicUrl();

                    if (publicUrl != null && !publicUrl.isEmpty()) {
                        RemoteLogger.info("YandexDiskHelper", "PublicUrlAfterRetry",
                                "Публичная ссылка получена после " + attempt + " попытки: " + publicUrl);
                        Log.d(TAG, "Публичная ссылка получена после " + attempt + " попытки: " + publicUrl);
                        callback.onSuccess(publicUrl);
                    } else {
                        RemoteLogger.warn("YandexDiskHelper", "PublicUrlStillNotReady",
                                "Публичная ссылка еще не готова, попытка " + (attempt + 1));
                        Log.w(TAG, "Публичная ссылка еще не готова, попытка " + (attempt + 1));
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            retryGetPublicUrl(folderPath, callback, attempt + 1);
                        }, 3000);
                    }
                } else {
                    RemoteLogger.error("YandexDiskHelper", "RetryGetUrlError",
                            "Ошибка при повторной попытке получения ссылки: код " + response.code(), null);
                    createAlternativeLink(folderPath, callback);
                }
            }

            @Override
            public void onFailure(Call<YandexDiskResponse> call, Throwable t) {
                RemoteLogger.error("YandexDiskHelper", "RetryGetUrlNetworkError",
                        "Ошибка сети при повторной попытке получения ссылки", t);
                createAlternativeLink(folderPath, callback);
            }
        });
    }

    // ★ СОЗДАНИЕ АЛЬТЕРНАТИВНОЙ ССЫЛКИ
    private static void createAlternativeLink(String folderPath, UploadCallback callback) {
        String folderName = folderPath.substring(folderPath.lastIndexOf("/") + 1);

        // ★ ВОЗВРАЩАЕМ ТОЛЬКО ЧИСТУЮ ССЫЛКУ
        String alternativeUrl = "https://disk.yandex.ru/client/disk/" + folderName;

        RemoteLogger.info("YandexDiskHelper", "AlternativeLinkCreated",
                "Создана альтернативная ссылка: " + alternativeUrl);
        Log.d(TAG, "Альтернативная ссылка: " + alternativeUrl);
        callback.onSuccess(alternativeUrl);
    }
}
