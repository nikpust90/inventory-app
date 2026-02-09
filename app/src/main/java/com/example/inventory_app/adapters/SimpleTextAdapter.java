package com.example.inventory_app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.recyclerview.widget.RecyclerView;
import com.example.inventory_app.R;
import com.example.inventory_app.activity.CarSchemeActivity;
import java.util.List;

public class SimpleTextAdapter extends RecyclerView.Adapter<SimpleTextAdapter.ViewHolder> {
    private List<String> data;
    private CarSchemeActivity.OnItemClick listener;

    public SimpleTextAdapter(List<String> data, CarSchemeActivity.OnItemClick listener) {
        this.data = data;
        this.listener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_menu_button, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        String text = data.get(position);
        holder.btn.setText(text);
        holder.btn.setOnClickListener(v -> listener.onClick(text));
    }

    @Override
    public int getItemCount() { return data.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        Button btn;
        ViewHolder(View itemView) {
            super(itemView);
            btn = itemView.findViewById(R.id.menuBtn);
        }
    }
}