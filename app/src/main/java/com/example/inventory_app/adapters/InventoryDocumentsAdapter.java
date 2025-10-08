package com.example.inventory_app.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.inventory_app.InventoryDocument;
import com.example.inventory_app.databinding.ListItemDocumentBinding; // Убедитесь, что этот binding генерируется из list_item_document.xml
import java.util.List;

public class InventoryDocumentsAdapter extends RecyclerView.Adapter<InventoryDocumentsAdapter.ViewHolder> {
    private List<InventoryDocument> documents;
    private final OnDocumentClickListener clickListener;

    public interface OnDocumentClickListener {
        void onClick(String documentId);
    }

    public InventoryDocumentsAdapter(List<InventoryDocument> documents, OnDocumentClickListener clickListener) {
        this.documents = documents;
        this.clickListener = clickListener;
    }

    public void updateDocuments(List<InventoryDocument> newDocuments) {
        this.documents.clear();
        this.documents.addAll(newDocuments);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Создаем View для элемента списка
        ListItemDocumentBinding binding = ListItemDocumentBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // Привязываем данные к View
        InventoryDocument currentDocument = documents.get(position);
        holder.bind(currentDocument);

        // Обработчик клика на элемент
        holder.itemView.setOnClickListener(v -> {
            clickListener.onClick(currentDocument.getId()); // Предполагаем, что есть метод getId() в InventoryDocument
        });
    }

    @Override
    public int getItemCount() {
        return documents != null ? documents.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ListItemDocumentBinding binding;

        public ViewHolder(ListItemDocumentBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(InventoryDocument document) {
            // Заголовок
            String title = "Инв.";
            if (document.getId() != null && !document.getId().isEmpty()) {
                title += " #" + document.getId().substring(0, Math.min(6, document.getId().length()));
            }
            binding.textViewTitle.setText(title);

            // Номер документа
            if (document.getDocumentNumber() != null && !document.getDocumentNumber().isEmpty()) {
                binding.textViewDocumentNumber.setText("№ " + document.getDocumentNumber());
            } else {
                binding.textViewDocumentNumber.setText("№ не указан");
            }

            // Склад
            if (document.getWarehouse() != null && !document.getWarehouse().isEmpty()) {
                binding.textViewWarehouse.setText("Склад: " + document.getWarehouse());
            } else {
                binding.textViewWarehouse.setText("Склад не указан");
            }

            // Дата + количество
            StringBuilder subtitle = new StringBuilder();
            if (document.getDate() != null && !document.getDate().isEmpty()) {
                try {
                    String formattedDate = formatDate(document.getDate());
                    subtitle.append(formattedDate);
                } catch (Exception e) {
                    subtitle.append(document.getDate());
                }
            } else {
                subtitle.append("Без даты");
            }

            int itemsCount = document.getItems() != null ? document.getItems().size() : 0;
            subtitle.append(" • ").append(itemsCount).append(" шт.");

            binding.textViewDate.setText(subtitle.toString());
        }

        // Вспомогательный метод для форматирования даты
        private String formatDate(String dateString) {
            // Если дата уже в нормальном формате, просто возвращаем
            if (dateString.length() <= 20) {
                return dateString;
            }

            // Если дата в формате ISO (2024-01-15T10:30:00), обрезаем время
            if (dateString.contains("T")) {
                return dateString.split("T")[0];
            }

            return dateString;
        }
    }



}

