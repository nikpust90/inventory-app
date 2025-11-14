package com.example.inventory_app.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;
public class UploadPhotoInfoRequest {
    @SerializedName("userName")
    private String userName;

    @SerializedName("photoFolderUrl")
    private String photoFolderUrl;

    @SerializedName("documentIds")
    private List<String> documentIds;

    @SerializedName("uploadTime")
    private String uploadTime;

    // Конструктор
    public UploadPhotoInfoRequest(String userName, String photoFolderUrl, List<String> documentIds, String uploadTime) {
        this.userName = userName;
        this.photoFolderUrl = photoFolderUrl;
        this.documentIds = documentIds;
        this.uploadTime = uploadTime;
    }

    // Геттеры (Retrofit/Gson их использует для сериализации)
    public String getUserName() {
        return userName;
    }

    public String getPhotoFolderUrl() {
        return photoFolderUrl;
    }

    public List<String> getDocumentIds() {
        return documentIds;
    }

    public String getUploadTime() {
        return uploadTime;
    }
}
