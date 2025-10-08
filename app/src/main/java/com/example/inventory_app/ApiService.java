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


    // Получение списка всех документов инвентаризации
    // Будет вызывать: GET http://89.108.72.168/Stavtrack_UNF/hs/inventory_documents/
    @GET("inventory_documents/")
    Call<List<InventoryDocument>> getInventoryDocuments();

    // Получение одного документа по ID
    // Будет вызывать: GET http://89.108.72.168/Stavtrack_UNF/hs/inventory_documents/{id}
    @GET("inventory_documents/{id}")
    Call<InventoryDocument> getInventoryDocumentById(@Path("id") String documentId);

    // Отправка обновленного документа
    // Будет вызывать: POST http://89.108.72.168/Stavtrack_UNF/hs/inventory_documents/update
    @POST("inventory_documents/update")
    Call<Void> updateInventoryDocument(@Body InventoryDocument document);

}