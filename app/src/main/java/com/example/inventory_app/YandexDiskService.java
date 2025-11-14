package com.example.inventory_app;

import com.example.inventory_app.models.YandexDiskResponse;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.*;

public interface YandexDiskService {

    @PUT("/v1/disk/resources")
    Call<Void> createFolder(@Query("path") String path);

    @GET("/v1/disk/resources/upload")
    Call<YandexDiskResponse> getUploadLink(@Query("path") String path);

    @PUT
    Call<ResponseBody> uploadFile(@Url String url, @Body RequestBody file);

    // ★ ПРАВИЛЬНЫЙ МЕТОД ДЛЯ ПУБЛИКАЦИИ РЕСУРСА
    @PUT("/v1/disk/resources/publish")
    Call<Void> publishResource(@Query("path") String path);

    // ★ ПРАВИЛЬНЫЙ МЕТОД ДЛЯ ПОЛУЧЕНИЯ ИНФОРМАЦИИ О РЕСУРСЕ
    @GET("/v1/disk/resources")
    Call<YandexDiskResponse> getResource(@Query("path") String path, @Query("fields") String fields);

    // ★ ПЕРЕГРУЗКА ДЛЯ ОБРАТНОЙ СОВМЕСТИМОСТИ
    @GET("/v1/disk/resources")
    Call<YandexDiskResponse> getResource(@Query("path") String path);

}
