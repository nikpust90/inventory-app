package com.example.inventory_app;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.io.IOException;
import java.time.LocalDateTime;

public class ApiClient {

    private static final String BASE_URL = "http://192.168.1.76:8080";
    private static final String BEARER_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJKV1Qgd2l0aCB1c2VyIGRldGFpbHMiLCJ1c2VybmFtZSI6InVzZXJfdGVzdCIsInJvbGUiOiJST0xFX0FETUlOIiwiaWF0IjoxNzM5NTQyOTUwLCJpc3MiOiJNYXhpbWEgU2Nob29sIiwiZXhwIjoxNzc1ODMwOTUwfQ.nEqtAFmH6ol8kWp-71Bqt8fUZF3Q8Y2lF3j8IO8X-VI"; // Ваш токен

    private static Retrofit retrofit = null;

    public static Retrofit getClient() {
        if (retrofit == null) {
            // Создаем OkHttpClient с интерсептором для добавления токена
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(new Interceptor() {
                        @Override
                        public Response intercept(Chain chain) throws IOException {
                            Request newRequest = chain.request().newBuilder()
                                    .addHeader("Authorization", "Bearer " + BEARER_TOKEN) // Добавляем заголовок с токеном
                                    .build();
                            return chain.proceed(newRequest);
                        }
                    })
                    .build();



            // Создаем Retrofit с OkHttpClient и Gson
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client) // Указываем custom OkHttpClient
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }
}