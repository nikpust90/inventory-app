package com.example.inventory_app;


import com.google.gson.annotations.SerializedName;

public class OutgoingItem {
    @SerializedName("product")
    private Product product;

    @SerializedName("quantity")
    private int quantity;

    @SerializedName("bin")
    private Bin bin;

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public Bin getBin() {
        return bin;
    }

    public void setBin(Bin bin) {
        this.bin = bin;
    }
}

