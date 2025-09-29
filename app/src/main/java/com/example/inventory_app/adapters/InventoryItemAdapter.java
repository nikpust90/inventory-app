package com.example.inventory_app.adapters;


import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.inventory_app.InventoryItem;
import com.example.inventory_app.databinding.ListItemInventoryBinding; // Убедитесь, что этот layout-файл существует
import java.util.List;

public class InventoryItemAdapter extends RecyclerView.Adapter<InventoryItemAdapter.InventoryItemViewHolder> {

    private final List<InventoryItem> items;

    public InventoryItemAdapter(List<InventoryItem> items) {
        this.items = items;
    }

    /**
     * Метод для обновления списка элементов и перерисовки RecyclerView.
     */
    public void updateItems(List<InventoryItem> newItems) {
        this.items.clear();
        this.items.addAll(newItems);
        notifyDataSetChanged(); // Сообщаем адаптеру, что данные изменились
    }

    @NonNull
    @Override
    public InventoryItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Создаем View для элемента списка
        ListItemInventoryBinding binding = ListItemInventoryBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new InventoryItemViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull InventoryItemViewHolder holder, int position) {
        // Привязываем данные к View
        InventoryItem currentItem = items.get(position);
        holder.bind(currentItem);
    }

    @Override
    public int getItemCount() {
        // Возвращаем количество элементов в списке
        return items != null ? items.size() : 0;
    }

    /**
     * ViewHolder хранит ссылки на View-компоненты для одного элемента списка.
     */
    static class InventoryItemViewHolder extends RecyclerView.ViewHolder {
        private final ListItemInventoryBinding binding;

        public InventoryItemViewHolder(ListItemInventoryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(InventoryItem item) {
            // Устанавливаем текст в TextView
            binding.textViewNomenclature.setText(item.getNomenklatura());
            binding.textViewSerial.setText("Серия: " + item.getSeriya());
            binding.textViewQuantity.setText(String.valueOf(item.getKolichestvo())); // План
            binding.textViewQuantityFact.setText(String.valueOf(item.getKolichestvoFakt())); // Факт
        }
    }
}
