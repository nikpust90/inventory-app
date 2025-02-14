package com.example.inventory_app;

import com.google.gson.annotations.SerializedName;

public class Bin {
    @SerializedName("binId")
    private String binId;

    @SerializedName("binName")
    private String binName;

    @SerializedName("shelfId")
    private String shelfId;

    @SerializedName("qrCode")
    private String qrCode;

    public String getBinId() {
        return binId;
    }

    public void setBinId(String binId) {
        this.binId = binId;
    }

    public String getBinName() {
        return binName;
    }

    public void setBinName(String binName) {
        this.binName = binName;
    }

    public String getShelfId() {
        return shelfId;
    }

    public void setShelfId(String shelfId) {
        this.shelfId = shelfId;
    }

    public String getQrCode() {
        return qrCode;
    }

    public void setQrCode(String qrCode) {
        this.qrCode = qrCode;
    }
}
