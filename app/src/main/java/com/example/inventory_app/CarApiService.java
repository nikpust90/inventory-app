package com.example.inventory_app;

import com.example.inventory_app.models.CarResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface CarApiService {
    // 0. Категории
    @GET("/api/cars/categories")
    Call<CarResponse> getCategories();

    // 1. Марки
    @GET("/api/cars/brands")
    Call<CarResponse> getBrands(@Query("category") String category);

    // 2. Модели
    @GET("/api/cars/models")
    Call<CarResponse> getModels(
            @Query("category") String category,
            @Query("brand") String brand
    );

    // 3. Поколения
    @GET("/api/cars/generations")
    Call<CarResponse> getGenerations(
            @Query("category") String category,
            @Query("brand") String brand,
            @Query("model") String model
    );

    // ПОИСК
    @GET("/api/cars/search")
    Call<CarResponse> searchCars(@Query("q") String query);
}
