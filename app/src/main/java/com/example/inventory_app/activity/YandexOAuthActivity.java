package com.example.inventory_app.activity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.webkit.*;
import androidx.appcompat.app.AppCompatActivity;
import com.example.inventory_app.R;
import com.example.inventory_app.YandexTokenManager;

public class YandexOAuthActivity extends AppCompatActivity {
    private WebView webView;
    private static final String CLIENT_ID = "3534a9277f294208be07c87561aecc7f";
    private boolean tokenProcessed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Устанавливаем прозрачную тему
        setTheme(R.style.Theme_AppCompat_Translucent);

        setContentView(R.layout.activity_yandex_oauth); // Создайте layout если нужно

        setupWebView();
    }

    private void setupWebView() {
        webView = new WebView(this);
        setContentView(webView);

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                return handleUrl(url);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleUrl(url);
            }

            private boolean handleUrl(String url) {
                if (url.contains("access_token=")) {
                    String token = extractTokenFromUrl(url);
                    if (token != null && !tokenProcessed) {
                        tokenProcessed = true;
                        YandexTokenManager.onTokenReceived(token);
                        finish();
                        return true;
                    }
                } else if (url.contains("error=") && !tokenProcessed) {
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
                if (!tokenProcessed) {
                    tokenProcessed = true;
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

        Log.d("YandexOAuth", "Loading URL: " + authUrl);
        webView.loadUrl(authUrl);
    }

    private String extractTokenFromUrl(String url) {
        try {
            Uri uri = Uri.parse(url);
            String fragment = uri.getFragment();
            if (fragment != null) {
                String[] params = fragment.split("&");
                for (String param : params) {
                    if (param.startsWith("access_token=")) {
                        return param.substring("access_token=".length());
                    }
                }
            }
        } catch (Exception e) {
            Log.e("YandexOAuth", "Error extracting token", e);
        }
        return null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!tokenProcessed) {
            Log.w("YandexOAuth", "Activity destroyed without token processing");
            YandexTokenManager.onTokenError("Авторизация отменена");
        }

        if (webView != null) {
            webView.destroy();
        }
    }

    @Override
    public void onBackPressed() {
        if (!tokenProcessed) {
            tokenProcessed = true;
            YandexTokenManager.onTokenError("Авторизация отменена пользователем");
        }
        super.onBackPressed();
    }
}
