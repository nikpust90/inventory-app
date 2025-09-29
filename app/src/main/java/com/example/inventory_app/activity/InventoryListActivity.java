package com.example.inventory_app.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.inventory_app.ApiClient;
import com.example.inventory_app.ApiService;
import com.example.inventory_app.InventoryDocument;
import com.example.inventory_app.adapters.InventoryDocumentsAdapter;
import com.example.inventory_app.databinding.ActivityDocumentListBinding; // Создайте binding для layout списка
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.ArrayList;
import java.util.List;

public class InventoryListActivity extends AppCompatActivity {

    private ActivityDocumentListBinding binding;
    private ApiService apiService;
    private InventoryDocumentsAdapter adapter; // Адаптер для списка документов

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDocumentListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        apiService = ApiClient.getRetrofitInstance().create(ApiService.class);

        setupRecyclerView();
        loadDocuments();
    }

    private void setupRecyclerView() {
        adapter = new InventoryDocumentsAdapter(new ArrayList<>(), this::openDocument);
        binding.documentsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.documentsRecyclerView.setAdapter(adapter);
    }

    private void loadDocuments() {
        apiService.getInventoryDocuments().enqueue(new Callback<List<InventoryDocument>>() { // Предполагаем метод в ApiService: getAllInventoryDocuments()
            @Override
            public void onResponse(Call<List<InventoryDocument>> call, Response<List<InventoryDocument>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    adapter.updateDocuments(response.body());
                } else {
                    Toast.makeText(InventoryListActivity.this, "Не удалось загрузить список документов", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<InventoryDocument>> call, Throwable t) {
                Toast.makeText(InventoryListActivity.this, "Ошибка сети", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openDocument(String documentId) {
        Intent intent = new Intent(this, InventoryActivity.class);
        intent.putExtra("DOCUMENT_ID", documentId);
        startActivity(intent);
    }
}


