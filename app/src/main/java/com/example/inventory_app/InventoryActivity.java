package com.example.inventory_app;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.inventory_app.databinding.ActivityDocumentBinding;
import com.google.zxing.integration.android.IntentIntegrator;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.ArrayList;

public class InventoryActivity extends AppCompatActivity {

    private ActivityDocumentBinding binding;
    private ApiService apiService;
    private InventoryItem adapter; // Адаптер для списка позиций
    private InventoryDocument currentDocument; // Текущий документ, с которым работаем

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDocumentBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);

        // Получаем ID документа из Intent
        String documentId = getIntent().getStringExtra("DOCUMENT_ID");

        setupRecyclerView();

        // Загружаем данные документа
        if (documentId != null) {
            loadDocumentDetails(documentId);
        }

        binding.fabScan.setOnClickListener(v -> {
            new IntentIntegrator(this).initiateScan();
        });

        binding.sendButton.setOnClickListener(v -> {
            if (currentDocument != null) {
                sendDocumentToServer();
            }
        });
    }

    private void setupRecyclerView() {
        adapter = new InventoryItem(new ArrayList<>());
        binding.itemsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.itemsRecyclerView.setAdapter(adapter);
    }

    private void loadDocumentDetails(String documentId) {
        apiService.getInventoryDocumentById(documentId).enqueue(new Callback<InventoryDocument>() {
            @Override
            public void onResponse(Call<InventoryDocument> call, Response<InventoryDocument> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentDocument = response.body();
                    // Передаем список позиций в адаптер
                    adapter.updateItems(currentDocument.getItems());
                } else {
                    Toast.makeText(InventoryActivity.this, "Не удалось загрузить документ", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<InventoryDocument> call, Throwable t) {
                Toast.makeText(InventoryActivity.this, "Ошибка сети", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null && result.getContents() != null) {
            String scannedBarcode = result.getContents();
            processScan(scannedBarcode);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void processScan(String scannedBarcode) {
        if (currentDocument == null || currentDocument.getItems() == null) return;

        boolean found = false;
        for (int i = 0; i < currentDocument.getItems().size(); i++) {
            InventoryItem item = currentDocument.getItems().get(i);

            // Ищем совпадение по полю "серия"
            if (item.getSeriya().equals(scannedBarcode)) {
                // Нашли! Увеличиваем фактическое количество.
                item.setKolichestvoFakt(item.getKolichestvoFakt() + 1);

                // Обновляем только измененный элемент в списке для лучшей производительности
                adapter.notifyItemChanged(i);

                showResultDialog("Серия найдена!", "Товар: " + item.getNomenklatura());
                found = true;
                break; // Выходим из цикла, т.к. нашли совпадение
            }
        }

        if (!found) {
            // Если после цикла ничего не нашли
            showResultDialog("Ошибка", "Серия " + scannedBarcode + " не найдена в этом документе.");
        }
    }

    private void sendDocumentToServer() {
        apiService.updateInventoryDocument(currentDocument).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(InventoryActivity.this, "Документ успешно отправлен!", Toast.LENGTH_LONG).show();
                    finish(); // Закрываем активити после успешной отправки
                } else {
                    Toast.makeText(InventoryActivity.this, "Ошибка отправки", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(InventoryActivity.this, "Ошибка сети при отправке", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showResultDialog(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }
}
