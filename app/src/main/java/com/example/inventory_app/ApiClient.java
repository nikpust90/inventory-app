package com.example.inventory_app;

import com.google.gson.*;
import okhttp3.*;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.io.IOException;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

public class ApiClient {
    //
    private static final String BASE_URL = "";
    private static final String USERNAME = ""; // Замените на реальный логин
    private static final String PASSWORD = ""; // Замените на реальный пароль
   // private static final String BEARER_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJKV1Qgd2l0aCB1c2VyIGRldGFpbHMiLCJ1c2VybmFtZSI6InVzZXJfdGVzdCIsInJvbGUiOiJST0xFX0FETUlOIiwiaWF0IjoxNzM5NTQyOTUwLCJpc3MiOiJNYXhpbWEgU2Nob29sIiwiZXhwIjoxNzc1ODMwOTUwfQ.nEqtAFmH6ol8kWp-71Bqt8fUZF3Q8Y2lF3j8IO8X-VI"; // Ваш токен


    public static ApiService getApiService() {
        return getRetrofitInstance().create(ApiService.class);
    }
    private static Retrofit retrofit = null;

    public static Retrofit getRetrofitInstance() {
        if (retrofit == null) {

            // ✅ 1. Кастомный ДЕсериализатор для приема данных
            JsonDeserializer<InventoryItem> inventoryItemDeserializer = (JsonElement json, Type typeOfT, com.google.gson.JsonDeserializationContext context) -> {
                JsonObject obj = json.getAsJsonObject();
                InventoryItem item = new InventoryItem();

                // Собираем nomenklatura
                if (obj.has("nomenklaturaId") && obj.has("nomenklatura")) {
                    com.example.inventory_app.Nomenklatura n = new com.example.inventory_app.Nomenklatura();
                    n.setId(obj.get("nomenklaturaId").getAsString());
                    n.setName(obj.get("nomenklatura").getAsString());
                    item.setNomenklatura(n);
                }

                // Собираем seriya
                if (obj.has("seriyaId") && obj.has("seriya")) {
                    com.example.inventory_app.Seriya s = new com.example.inventory_app.Seriya();
                    s.setId(obj.get("seriyaId").getAsString());
                    s.setName(obj.get("seriya").getAsString());
                    if (obj.has("imei")) {
                        s.setImei(obj.get("imei").getAsString());
                    }
                    item.setSeriya(s);
                }

                // Остальные поля
                if (obj.has("kolichestvo"))
                    item.setKolichestvo(obj.get("kolichestvo").getAsInt());

                if (obj.has("kolichestvoFakt"))
                    item.setKolichestvoFakt(obj.get("kolichestvoFakt").getAsInt());

                if (obj.has("found"))
                    item.setFound(obj.get("found").getAsBoolean());

                return item;
            };

            // ✅ 2. Кастомный СЕриализатор для отправки данных
            JsonSerializer<InventoryItem> inventoryItemSerializer = (InventoryItem item, Type typeOfSrc, com.google.gson.JsonSerializationContext context) -> {
                JsonObject obj = new JsonObject();

                // Сериализуем nomenklatura
                if (item.getNomenklatura() != null) {
                    obj.addProperty("nomenklaturaId", item.getNomenklatura().getId());
                    obj.addProperty("nomenklatura", item.getNomenklatura().getName());
                }

                // Сериализуем seriya
                if (item.getSeriya() != null) {
                    obj.addProperty("seriyaId", item.getSeriya().getId());
                    obj.addProperty("seriya", item.getSeriya().getName());
                    if (item.getSeriya().getImei() != null) {
                        obj.addProperty("imei", item.getSeriya().getImei());
                    }
                }

                // Остальные поля
                obj.addProperty("kolichestvo", item.getKolichestvo());
                obj.addProperty("kolichestvoFakt", item.getKolichestvoFakt());
                obj.addProperty("found", item.isFound());

                return obj;
            };

            // ✅ 3. Собираем Gson с обоими адаптерами
            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(InventoryItem.class, inventoryItemDeserializer)
                    .registerTypeAdapter(InventoryItem.class, inventoryItemSerializer)
                    .create();

            // ✅ 4. Настраиваем OkHttpClient
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(new Interceptor() {
                        @Override
                        public Response intercept(Chain chain) throws IOException {
                            Request originalRequest = chain.request();

                            String credentials = Credentials.basic(USERNAME, PASSWORD, StandardCharsets.UTF_8);
                            Request newRequest = originalRequest.newBuilder()
                                    .addHeader("Authorization", credentials)
                                    .addHeader("Content-Type", "application/json")
                                    .addHeader("Accept", "application/json")
                                    .build();

                            return chain.proceed(newRequest);
                        }
                    })
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();

            // ✅ 5. Создаем Retrofit
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();
        }

        return retrofit;
    }
}