package com.example.inventory_app.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.inventory_app.R;
import com.example.inventory_app.models.Seriya;
import com.example.inventory_app.models.StockItem;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.inventory_app.R;
import com.example.inventory_app.models.StockItem;

import java.util.List;

public class StockReportAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<StockItem> items;
    private final LayoutInflater inflater;
    private final Context context;

    public StockReportAdapter(Context context, List<StockItem> items) {
        this.context = context;
        this.items = items;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).type;
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        switch (viewType) {
            case StockItem.TYPE_WAREHOUSE:
                View warehouseView = inflater.inflate(R.layout.item_warehouse, parent, false);
                return new WarehouseViewHolder(warehouseView);

            case StockItem.TYPE_NOMENCLATURE:
                View nomenclatureView = inflater.inflate(R.layout.item_nomenclature, parent, false);
                return new NomenclatureViewHolder(nomenclatureView);

            case StockItem.TYPE_SERIES:
                View seriesView = inflater.inflate(R.layout.item_series, parent, false);
                return new SeriesViewHolder(seriesView);

            default:
                throw new IllegalArgumentException("Unknown view type: " + viewType);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        StockItem item = items.get(position);

        switch (holder.getItemViewType()) {
            case StockItem.TYPE_WAREHOUSE:
                bindWarehouseViewHolder((WarehouseViewHolder) holder, item, position);
                break;

            case StockItem.TYPE_NOMENCLATURE:
                bindNomenclatureViewHolder((NomenclatureViewHolder) holder, item, position);
                break;

            case StockItem.TYPE_SERIES:
                bindSeriesViewHolder((SeriesViewHolder) holder, item);
                break;
        }
    }

    private void bindWarehouseViewHolder(WarehouseViewHolder holder, StockItem item, int position) {
        holder.textWarehouseName.setText(
                (item.expanded ? "▼ " : "▶ ") + item.warehouseName
        );

        // Подсветка складов
        holder.itemView.setBackgroundColor(ContextCompat.getColor(context,
                item.expanded ? R.color.warehouse_expanded : R.color.warehouse_collapsed));

        holder.itemView.setOnClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;

            StockItem clicked = items.get(pos);

            if (clicked.expanded) {
                collapseItem(pos, clicked);
            } else {
                expandItem(pos, clicked);
            }

            clicked.expanded = !clicked.expanded;
            notifyItemChanged(pos);
        });
    }

    private void bindNomenclatureViewHolder(NomenclatureViewHolder holder, StockItem item, int position) {
        holder.textNomenclature.setText(item.nomenclatureName);
        holder.textQuantity.setText(item.getQuantityText()); // Используем новый метод
        holder.textSeries.setText(item.getSeriesText());

        // Отступ для вложенности
        holder.itemView.setPadding(
                dpToPx(16),
                holder.itemView.getPaddingTop(),
                holder.itemView.getPaddingRight(),
                holder.itemView.getPaddingBottom()
        );

        // Подсветка в зависимости от наличия свободного количества
        if (item.freeQuantity == 0) {
            holder.itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.no_stock));
        } else if (item.freeQuantity < item.totalQuantity) {
            holder.itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.partial_stock));
        } else {
            holder.itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.full_stock));
        }

        holder.itemView.setOnClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;

            StockItem clicked = items.get(pos);

            if (clicked.expanded) {
                collapseItem(pos, clicked);
            } else {
                expandItem(pos, clicked);
            }

            clicked.expanded = !clicked.expanded;
            notifyItemChanged(pos);
        });
    }

    private void bindSeriesViewHolder(SeriesViewHolder holder, StockItem item) {
        holder.textSeriesName.setText("• " + item.seriya);

        // Показываем IMEI если есть
        if (item.imei != null && !item.imei.isEmpty()) {
            holder.textImei.setText("IMEI: " + item.imei);
            holder.textImei.setVisibility(View.VISIBLE);
        } else {
            holder.textImei.setVisibility(View.GONE);
        }

        // Больший отступ для серий
        holder.itemView.setPadding(
                dpToPx(32),
                holder.itemView.getPaddingTop(),
                holder.itemView.getPaddingRight(),
                holder.itemView.getPaddingBottom()
        );
    }

    private void expandItem(int position, StockItem item) {
        if (item.children == null || item.children.isEmpty()) return;

        int insertPos = position + 1;
        items.addAll(insertPos, item.children);

        notifyItemRangeInserted(insertPos, item.children.size());
    }

    private void collapseAllWarehousesExcept(int exceptPosition) {

        for (int i = 0; i < items.size(); i++) {
            StockItem warehouse = items.get(i);

            if (warehouse.type == StockItem.TYPE_WAREHOUSE &&
                    i != exceptPosition &&
                    warehouse.expanded) {

                collapseItem(i, warehouse);  // ← ВАЖНО!
                warehouse.expanded = false;
            }
        }

        notifyDataSetChanged();
    }

    /**
     * Рекурсивно сбрасывает состояние expanded у элемента и всех его дочерних элементов.
     */
    private void resetExpandedState(StockItem item) {
        if (item.children == null || item.children.isEmpty()) return;

        for (StockItem child : item.children) {
            if (child.expanded) {
                child.expanded = false;
            }
            // Если дочерний элемент сам может иметь детей (например, Номенклатура имеет Серии)
            if (child.type == StockItem.TYPE_NOMENCLATURE) {
                // Если номенклатура была развернута, нужно рекурсивно свернуть ее серии
                resetExpandedState(child);
            }
        }
    }

    private void collapseItem(int position, StockItem item) {
        if (item.children == null || item.children.isEmpty()) return;

        List<StockItem> itemsToRemove = new ArrayList<>();
        collectVisibleDescendants(item, itemsToRemove);

        if (itemsToRemove.isEmpty()) return;

        int count = itemsToRemove.size();
        int removePos = position + 1;

        // Сбрасываем состояние всех удаляемых элементов
        for (StockItem child : itemsToRemove) {
            child.expanded = false;
        }

        // Удаляем элементы
        for (int i = 0; i < count; i++) {
            items.remove(removePos);
        }

        notifyItemRangeRemoved(removePos, count);
    }

    private void collectVisibleDescendants(StockItem item, List<StockItem> collector) {
        if (!item.expanded || item.children == null) return;

        for (StockItem child : item.children) {
            collector.add(child);
            if (child.expanded) {
                collectVisibleDescendants(child, collector);
            }
        }
    }

    private int dpToPx(int dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density);
    }

    // ViewHolders
    static class WarehouseViewHolder extends RecyclerView.ViewHolder {
        TextView textWarehouseName;
        WarehouseViewHolder(View v) {
            super(v);
            textWarehouseName = v.findViewById(R.id.textWarehouseName);
        }
    }

    static class NomenclatureViewHolder extends RecyclerView.ViewHolder {
        TextView textNomenclature, textQuantity, textSeries;
        NomenclatureViewHolder(View v) {
            super(v);
            textNomenclature = v.findViewById(R.id.textNomenclature);
            textQuantity = v.findViewById(R.id.textQuantity);
            textSeries = v.findViewById(R.id.textSeries);
        }
    }

    static class SeriesViewHolder extends RecyclerView.ViewHolder {
        TextView textSeriesName, textImei;
        SeriesViewHolder(View v) {
            super(v);
            textSeriesName = v.findViewById(R.id.textSeriesName);
            textImei = v.findViewById(R.id.textImei);
        }
    }
}

