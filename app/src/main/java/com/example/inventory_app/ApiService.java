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

    @GET("/api/inventory/documents/{id}") // Замените на ваш endpoint
    Call<InventoryDocument> getOutgoingDocumentById(@Path("id") Long documentId);


    // 1. Получение списка всех документов инвентаризации
    @GET("inventory_documents") // Замените на ваш URL
    Call<List<InventoryDocument>> getInventoryDocuments();

    // 2. Получение одного документа со всеми его позициями по ID
    @GET("/api/inventory/documents/{id}") // Замените на ваш URL
    Call<InventoryDocument> getInventoryDocumentById(@Path("id") String documentId);

    // 3. Отправка обновленного документа на сервер
    @POST("/api/inventory/documents/update") // Замените на ваш URL
    Call<Void> updateInventoryDocument(@Body InventoryDocument document); // Отправляем весь объект в теле запроса

}