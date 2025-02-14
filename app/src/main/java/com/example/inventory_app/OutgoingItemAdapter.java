package com.example.inventory_app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class OutgoingItemAdapter extends RecyclerView.Adapter<OutgoingItemAdapter.ViewHolder> {

    private List<OutgoingItem> items;

    public OutgoingItemAdapter(List<OutgoingItem> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_outgoing, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        OutgoingItem item = items.get(position);
        holder.productNameTextView.setText(item.getProduct().getName());
        holder.quantityTextView.setText(String.valueOf(item.getQuantity()));
        holder.barcodeTextView.setText(item.getProduct().getBarcode());
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void updateQuantityByBarcode(String barcode) {
        if (barcode == null) {
            return; // Выход из метода, если barcode равен null
        }

        for (int i = 0; i < items.size(); i++) {
            OutgoingItem item = items.get(i);
            // Проверяем, что barcode товара не равен null и совпадает с переданным значением
            if (item.getProduct().getBarcode() != null && item.getProduct().getBarcode().equals(barcode)) {
                item.setQuantity(item.getQuantity() + 1);
                notifyItemChanged(i); // Уведомляем RecyclerView об изменении данных
                break;
            }
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView productNameTextView;
        public TextView quantityTextView;
        public TextView barcodeTextView;

        public ViewHolder(View view) {
            super(view);
            productNameTextView = view.findViewById(R.id.productNameTextView);
            quantityTextView = view.findViewById(R.id.quantityTextView);
            barcodeTextView = view.findViewById(R.id.barcodeTextView);
        }
    }
}