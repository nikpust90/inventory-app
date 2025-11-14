package com.example.inventory_app;


import java.util.List;

import com.example.inventory_app.models.*;
import retrofit2.Call;
import retrofit2.http.*;

/**
 * Интерфейс для запросов к серверу.
 */
public interface ApiService {

    // Авторизация по ПИН-коду
    @POST("inventory_documents/login") // Endpoint будет: /hs/inventory_documents/login
    Call<LoginResponse> loginByPin(@Body LoginRequest request);

    // Получение списка всех документов инвентаризации
    @GET("inventory_documents/")
    Call<List<InventoryDocument>> getInventoryDocuments(
            @Query("warehouse") String warehouseIds,
            @Query("status") String status
    );

    // Получение одного документа по ID
    @GET("inventory_documents/{id}")
    Call<InventoryDocument> getInventoryDocumentById(@Path("id") String documentId);

    // Отправка обновленного документа
    @POST("inventory_documents/update")
    Call<Void> updateInventoryDocument(@Body InventoryDocument document);

    @GET("inventory_documents/stock_report")
    Call<List<StockReport>> getStockReport(
            @Query("warehouse") String warehouseIds
    );

    /**
     * Отправляет в 1С информацию о загруженных фото
     * (ссылка на папку, ID документов и имя пользователя)
     */
    @POST("inventory_documents/upload_photo_info")
    Call<Void> sendPhotoInfo(@Body UploadPhotoInfoRequest requestBody);

}