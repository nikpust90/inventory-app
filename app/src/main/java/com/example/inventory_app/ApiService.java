package com.example.inventory_app;


import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

/**
 * Интерфейс для запросов к серверу.
 */
public interface ApiService {
//    @GET("inventory")
//    Call<List<InventoryItem>> getInventoryItems(); // Получение списка товаров
//
//    @POST("inventory")
//    Call<Void> sendInventory(@Body List<InventoryItem> items); // Отправка данных о товарах

    @GET("/api/outgoing/documents/{id}") // Замените на ваш endpoint
    Call<OutgoingDocument> getOutgoingDocumentById(@Path("id") Long documentId);
}