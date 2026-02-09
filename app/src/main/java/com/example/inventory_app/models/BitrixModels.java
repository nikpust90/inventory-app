package com.example.inventory_app.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class BitrixModels {

    // Ответ от crm.item.list
    public static class ItemListResponse {
        @SerializedName("result")
        public Result result;
    }

    public static class Result {
        @SerializedName("items")
        public List<Item> items;
    }

    public static class Item {
        @SerializedName("id")
        public int id;
    }

    // Ответ от crm.timeline.comment.add
    public static class CommentResponse {
        @SerializedName("result")
        public int result; // ID комментария
    }

    // Тело запроса для поиска ID (crm.item.list)
    public static class FilterRequest {
        public int entityTypeId;
        public Filter filter;
        public String[] select = {"id"};

        public FilterRequest(int entityTypeId, String orderNumber) {
            this.entityTypeId = entityTypeId;
            this.filter = new Filter(orderNumber);
        }

        static class Filter {
            public String parentId2; // Поле в котором лежит номер заказа
            public Filter(String parentId2) { this.parentId2 = parentId2; }
        }
    }

    // Тело запроса для комментария (crm.timeline.comment.add)
    public static class CommentRequest {
        public Fields fields;

        public CommentRequest(int entityId, String entityType, String comment) {
            this.fields = new Fields(entityId, entityType, comment);
        }

        static class Fields {
            @SerializedName("ENTITY_ID")
            public int entityId;
            @SerializedName("ENTITY_TYPE")
            public String entityType;
            @SerializedName("COMMENT")
            public String comment;

            public Fields(int entityId, String entityType, String comment) {
                this.entityId = entityId;
                this.entityType = entityType;
                this.comment = comment;
            }
        }
    }
}
