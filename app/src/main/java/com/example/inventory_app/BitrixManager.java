package com.example.inventory_app;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;
import com.example.inventory_app.models.BitrixModels;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class BitrixManager {

    private static final String BASE_URL = "https://<bitrix-host>/rest/<user>/<token>/";
    private final BitrixApi api;
    private final Context context;

    public BitrixManager(Context context) {
        this.context = context;
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        api = retrofit.create(BitrixApi.class);
    }

    // Аналог: ОтправитьНаДоработкуНаСервере
    public void sendToRevision(String orderNumber, String userComment, boolean isServiceOrder) {

        // 1. Определяем TypeId (как в 1С)
        int typeId = isServiceOrder ? 133 : 144;

        // 2. Ищем ID в Битрикс (Аналог ПолучитьИдЗаказНаряда)
        BitrixModels.FilterRequest request = new BitrixModels.FilterRequest(typeId, orderNumber);

        api.getItemId(request).enqueue(new Callback<BitrixModels.ItemListResponse>() {
            @Override
            public void onResponse(Call<BitrixModels.ItemListResponse> call, Response<BitrixModels.ItemListResponse> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().result.items != null
                        && !response.body().result.items.isEmpty()) {

                    int bitrixId = response.body().result.items.get(0).id;

                    // 3. Формируем текст и отправляем комментарий
                    String fullComment = buildCommentText(orderNumber, userComment, bitrixId, typeId);
                    sendTimelineComment(bitrixId, typeId, fullComment);

                } else {
                    Toast.makeText(context, "Заказ не найден в Битрикс!", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<BitrixModels.ItemListResponse> call, Throwable t) {
                Toast.makeText(context, "Ошибка сети: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Формирование текста (Аналог блока формирования ИтоговыйКомментарий)
    private String buildCommentText(String orderNumber, String userReason, int bitrixId, int typeId) {
        String emoTool = "\u2699\uFE0F"; // ⚙️
        String emoDoc = "\u2709\uFE0F";  // ✉️
        String emoClock = "\u23F0";      // ⏰
        String emoNote = "\u2712\uFE0F"; // ✒️

        StringBuilder sb = new StringBuilder();

        // Заголовок
        sb.append(emoTool).append(" *ЗАКАЗ ОТПРАВЛЕН НА ДОРАБОТКУ* ").append(emoTool).append("\n\n");

        // Инфо
        sb.append(emoDoc).append(" *Номер заказа:* ").append(orderNumber).append("\n");

        // Дедлайн (+24 часа)
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR, 24);
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy в HH:mm", Locale.getDefault());
        sb.append(emoClock).append(" *Необходимо исправить до:* ").append(sdf.format(calendar.getTime())).append("\n");

        // Причина
        if (userReason != null && !userReason.isEmpty()) {
            sb.append("\n").append(emoNote).append(" *Причина доработки:* ").append(userReason).append("\n");
        }

        // Ссылка (Аналог блока формирования URL)
        String link = "https://portal.stavtrack.ru/page/zakaznaryady/zakaznaryady/type/"
                + typeId + "/details/" + bitrixId + "/";
        sb.append("\n").append(link);

        return sb.toString();
    }

    // Отправка комментария (Аналог ОтправитьКомментарийКЗаказуБ24...)
    private void sendTimelineComment(int bitrixId, int typeId, String text) {
        String entityType = "dynamic_" + typeId;

        BitrixModels.CommentRequest request = new BitrixModels.CommentRequest(bitrixId, entityType, text);

        api.addComment(request).enqueue(new Callback<BitrixModels.CommentResponse>() {
            @Override
            public void onResponse(Call<BitrixModels.CommentResponse> call, Response<BitrixModels.CommentResponse> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(context, "Успешно отправлено в Битрикс!", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(context, "Ошибка отправки комментария", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<BitrixModels.CommentResponse> call, Throwable t) {
                Toast.makeText(context, "Ошибка сети", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // НОВЫЙ МЕТОД: Отправка сразу по ID (без поиска)
    public void sendDirectMessage(String documentIdString, String messageText, boolean isServiceOrder) {
        try {
            // Преобразуем введенную строку в число (ID)
            int bitrixId = Integer.parseInt(documentIdString);

            // Определяем TypeId (133 или 144)
            int typeId = isServiceOrder ? 133 : 144;

            // Сразу отправляем комментарий
            sendTimelineComment(bitrixId, typeId, messageText);

        } catch (NumberFormatException e) {
            Toast.makeText(context, "ID должен быть числом!", Toast.LENGTH_SHORT).show();
        }
    }

    // Универсальный метод для отправки ЛЮБОГО комментария к сделке/заказ-наряду
    public void sendCustomMessage(String orderNumber, String messageText, boolean isServiceOrder) {
        // 1. Определяем TypeId
        int typeId = isServiceOrder ? 133 : 144;

        // 2. Ищем ID
        BitrixModels.FilterRequest request = new BitrixModels.FilterRequest(typeId, orderNumber);

        api.getItemId(request).enqueue(new Callback<BitrixModels.ItemListResponse>() {
            @Override
            public void onResponse(Call<BitrixModels.ItemListResponse> call, Response<BitrixModels.ItemListResponse> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().result.items != null
                        && !response.body().result.items.isEmpty()) {

                    int bitrixId = response.body().result.items.get(0).id;

                    // 3. Отправляем переданный текст
                    sendTimelineComment(bitrixId, typeId, messageText);

                } else {
                    Toast.makeText(context, "Заказ " + orderNumber + " не найден в Битрикс!", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<BitrixModels.ItemListResponse> call, Throwable t) {
                Toast.makeText(context, "Ошибка сети: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
