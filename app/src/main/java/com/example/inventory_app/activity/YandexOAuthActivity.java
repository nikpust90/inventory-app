package com.example.inventory_app.activity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.webkit.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.example.inventory_app.R;
import com.example.inventory_app.RemoteLogger;
import com.example.inventory_app.YandexTokenManager;

import java.util.Arrays;

public class YandexOAuthActivity extends AppCompatActivity {
    private WebView webView;
    private static final String CLIENT_ID = "3534a9277f294208be07c87561aecc7f";
    private boolean tokenProcessed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        RemoteLogger.init(this);
        RemoteLogger.info("YandexOAuth", "ActivityStart",
                "Активити авторизации Яндекс OAuth запущено");


        // Устанавливаем прозрачную тему
        setTheme(R.style.Theme_AppCompat_Translucent);

        setContentView(R.layout.activity_yandex_oauth); // Создайте layout если нужно

        setupWebView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        RemoteLogger.info("YandexOAuth", "ActivityResume",
                "Активити авторизации возобновлено");
    }

    @Override
    protected void onPause() {
        super.onPause();
        RemoteLogger.info("YandexOAuth", "ActivityPause",
                "Активити авторизации приостановлено");
    }

    private void setupWebView() {
        webView = new WebView(this);
        setContentView(webView);

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);

        RemoteLogger.info("YandexOAuth", "WebViewSetup",
                "WebView настроен: JavaScript включен, DOM Storage включен");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                RemoteLogger.info("YandexOAuth", "WebViewRequest",
                        "shouldOverrideUrlLoading (WebResourceRequest): " + url);
                return handleUrl(url);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                RemoteLogger.info("YandexOAuth", "WebViewRequest",
                        "shouldOverrideUrlLoading (String): " + url);
                return handleUrl(url);
            }

            private boolean handleUrl(String url) {
                RemoteLogger.info("YandexOAuth", "HandleUrl",
                        "Обработка URL: " + url);

                if (url.contains("access_token=")) {
                    RemoteLogger.info("YandexOAuth", "TokenInUrl",
                            "Найден access_token в URL");
                    String token = extractTokenFromUrl(url);
                    if (token != null && !tokenProcessed) {
                        tokenProcessed = true;

                        // Маскируем токен для безопасности (показываем только первые 10 символов)
                        String maskedToken = token.length() > 10 ?
                                token.substring(0, 10) + "..." : "***";
                        RemoteLogger.info("YandexOAuth", "TokenExtracted",
                                "Токен извлечен успешно: " + maskedToken +
                                        ", длина: " + token.length());

                        YandexTokenManager.onTokenReceived(token);
                        finish();
                        return true;

                    } else if (tokenProcessed) {
                        RemoteLogger.warn("YandexOAuth", "TokenAlreadyProcessed",
                                "Токен уже был обработан ранее");
                    } else {
                        RemoteLogger.error("YandexOAuth", "TokenExtractFailed",
                                "Не удалось извлечь токен из URL", null);
                    }
                } else if (url.contains("error=") && !tokenProcessed) {
                    RemoteLogger.warn("YandexOAuth", "AuthErrorInUrl",
                            "Ошибка авторизации в URL: " + url);
                    tokenProcessed = true;
                    YandexTokenManager.onTokenError("Ошибка авторизации");
                    finish();
                    return true;
                }
                return false;
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                RemoteLogger.error("YandexOAuth", "WebViewError",
                        "Ошибка WebView: " + error.getErrorCode() + " - " + error.getDescription(), null);
                if (!tokenProcessed) {
                    tokenProcessed = true;
                    String errorMsg = "Ошибка загрузки: " + error.getDescription() + " (код: " + error.getErrorCode() + ")";
                    RemoteLogger.error("YandexOAuth", "AuthFailedWebError", errorMsg, null);
                    YandexTokenManager.onTokenError("Ошибка загрузки: " + error.getDescription());
                    finish();
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                Log.d("YandexOAuth", "Page finished: " + url);
            }
        });

        // Загружаем OAuth страницу
        String authUrl = "https://oauth.yandex.ru/authorize?" +
                "response_type=token" +
                "&client_id=" + CLIENT_ID +
                "&redirect_uri=https://oauth.yandex.ru/verification_code";

        RemoteLogger.info("YandexOAuth", "AuthUrl",
                "Загрузка OAuth URL: " + authUrl);

        Log.d("YandexOAuth", "Loading URL: " + authUrl);
        webView.loadUrl(authUrl);
    }

    private String extractTokenFromUrl(String url) {
        try {
            Uri uri = Uri.parse(url);
            String fragment = uri.getFragment();
            if (fragment != null) {
                String[] params = fragment.split("&");

                // Логируем все параметры фрагмента (без значений для безопасности)
                RemoteLogger.info("YandexOAuth", "FragmentParams",
                        "Параметры фрагмента: " + Arrays.toString(
                                Arrays.stream(params)
                                        .map(p -> p.split("=")[0])
                                        .toArray()
                        ));

                for (String param : params) {
                    if (param.startsWith("access_token=")) {
                        RemoteLogger.info("YandexOAuth", "TokenExpires",
                                "Токен истекает через: " + param.substring("expires_in=".length()) + " сек");
                        return param.substring("access_token=".length());
                    }
                }
            }
        } catch (Exception e) {
            RemoteLogger.error("YandexOAuth", "TokenExtractException",
                    "Исключение при извлечении токена", e);
            Log.e("YandexOAuth", "Error extracting token", e);
        }
        return null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        RemoteLogger.info("YandexOAuth", "ActivityDestroy",
                "Активити авторизации уничтожено, tokenProcessed: " + tokenProcessed);
        if (!tokenProcessed) {
            RemoteLogger.warn("YandexOAuth", "TokenNotProcessed",
                    "Активити уничтожено без обработки токена");
            Log.w("YandexOAuth", "Activity destroyed without token processing");
            YandexTokenManager.onTokenError("Авторизация отменена");
        }

        if (webView != null) {
            RemoteLogger.info("YandexOAuth", "WebViewDestroy", "WebView уничтожен");
            webView.destroy();
        }
    }

    @Override
    public void onBackPressed() {

        RemoteLogger.info("YandexOAuth", "BackPressed",
                "Нажата кнопка назад, tokenProcessed: " + tokenProcessed);

        if (!tokenProcessed) {
            tokenProcessed = true;
            RemoteLogger.warn("YandexOAuth", "AuthCancelledByUser",
                    "Авторизация отменена пользователем (кнопка назад)");
            YandexTokenManager.onTokenError("Авторизация отменена пользователем");
        }
        super.onBackPressed();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        RemoteLogger.info("YandexOAuth", "SaveInstanceState",
                "Сохранение состояния, tokenProcessed: " + tokenProcessed);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        RemoteLogger.info("YandexOAuth", "RestoreInstanceState",
                "Восстановление состояния");
    }
}
