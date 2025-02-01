package com.example.inventory_app;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class InventoryAdapter extends RecyclerView.Adapter<InventoryAdapter.ViewHolder> {
    private final List<Item> items;

    public InventoryAdapter(List<Item> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_inventory, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Item item = items.get(position);
        holder.name.setText(item.getName());
        holder.quantity.setText(String.valueOf(item.getQuantity()));

        // Удаляем предыдущий TextWatcher, если он есть
        if (holder.quantity.getTag() instanceof TextWatcher) {
            holder.quantity.removeTextChangedListener((TextWatcher) holder.quantity.getTag());
        }

        // Новый TextWatcher для EditText
        // Создаём новый TextWatcher
        TextWatcher watcher = new SimpleTextWatcher() {
            @Override
            public void onTextChanged(String text) {
                int qty = text.isEmpty() ? 0 : Integer.parseInt(text);
                item.setQuantity(qty);
            }
        };

        holder.quantity.addTextChangedListener(watcher);
        holder.quantity.setTag(watcher); // Запоминаем TextWatcher, чтобы потом удалить
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public List<Item> getUpdatedItems() {
        return items;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name;
        EditText quantity;

        ViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.name);
            quantity = itemView.findViewById(R.id.quantity);
        }
    }
}
