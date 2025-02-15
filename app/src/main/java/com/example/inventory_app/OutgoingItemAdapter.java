package com.example.inventory_app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.inventory_app.OutgoingItem;
import com.example.inventory_app.R;

import java.util.List;

public class OutgoingItemAdapter extends RecyclerView.Adapter<OutgoingItemAdapter.ViewHolder> {

    private final List<OutgoingItem> items; // Список элементов для отображения

    // Конструктор адаптера
    public OutgoingItemAdapter(List<OutgoingItem> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Создаем View для каждого элемента списка
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_outgoing, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // Привязываем данные к ViewHolder
        OutgoingItem item = items.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        // Возвращаем количество элементов в списке
        return items.size();
    }

    // Метод для обновления количества товара по штрих-коду
    public void updateQuantityByBarcode(String barcode) {
        if (barcode == null) {
            return; // Выход из метода, если штрих-код равен null
        }

        for (int i = 0; i < items.size(); i++) {
            OutgoingItem item = items.get(i);
            if (item.getProduct() != null && barcode.equals(item.getProduct().getBarcode())) {
                item.setQuantity(item.getQuantity() + 1); // Увеличиваем количество на 1
                notifyItemChanged(i); // Уведомляем RecyclerView об изменении данных
                break;
            }
        }
    }

    // ViewHolder для отображения элементов списка
    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final EditText productNameEditText; // Поле для названия продукта
        private final EditText quantityEditText;    // Поле для количества
        private final EditText barcodeEditText;     // Поле для штрих-кода
        private final Button saveButton;            // Кнопка сохранения

        public ViewHolder(View view) {
            super(view);
            // Инициализация View-элементов
            productNameEditText = view.findViewById(R.id.productNameEditText);
            quantityEditText = view.findViewById(R.id.quantityEditText);
            barcodeEditText = view.findViewById(R.id.barcodeEditText);
            saveButton = view.findViewById(R.id.saveButton);
        }

        // Метод для привязки данных к View-элементам
        public void bind(OutgoingItem item) {
            if (item.getProduct() != null) {
                productNameEditText.setText(item.getProduct().getName());
                barcodeEditText.setText(item.getProduct().getBarcode());
            } else {
                productNameEditText.setText("N/A");
                barcodeEditText.setText("N/A");
            }
            quantityEditText.setText(String.valueOf(item.getQuantity()));

            // Обработчик нажатия на кнопку "Save"
            saveButton.setOnClickListener(v -> {
                // Обновляем данные в объекте OutgoingItem
                item.getProduct().setName(productNameEditText.getText().toString());
                item.getProduct().setBarcode(barcodeEditText.getText().toString());
                try {
                    item.setQuantity(Integer.parseInt(quantityEditText.getText().toString()));
                } catch (NumberFormatException e) {
                    // Обработка ошибки, если введено не число
                    item.setQuantity(0);
                }

                // Здесь можно добавить логику для отправки данных на устройство (например, ТСД АТОЛ)
                // Например, вызвать метод API для обновления данных на устройстве
            });
        }
    }
}