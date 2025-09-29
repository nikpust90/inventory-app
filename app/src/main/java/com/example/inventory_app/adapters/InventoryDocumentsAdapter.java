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
            // Устанавливаем текст в TextView (предполагаем, что есть getTitle(), getDate() или другие поля)
            binding.textViewTitle.setText(document.getTitle()); // Например, "Документ #1"
            // Если есть другие поля, добавьте аналогично, напр.:
            binding.textViewDate.setText("Дата: " + document.getDate());
        }
    }
}
