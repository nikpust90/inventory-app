package com.example.inventory_app;

import com.example.inventory_app.models.BitrixModels;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface BitrixApi {
    // URL из вашего 1С кода: <bitrix-host>/rest/<user>/<token>/

    @POST("crm.item.list")
    Call<BitrixModels.ItemListResponse> getItemId(@Body BitrixModels.FilterRequest body);

    @POST("crm.timeline.comment.add")
    Call<BitrixModels.CommentResponse> addComment(@Body BitrixModels.CommentRequest body);
}
