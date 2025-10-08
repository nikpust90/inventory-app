package com.example.inventory_app.activity;

import android.app.AlertDialog;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.inventory_app.*;
import com.example.inventory_app.adapters.InventoryItemAdapter;
import com.example.inventory_app.databinding.ActivityDocumentBinding;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import org.jetbrains.annotations.Nullable;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class InventoryActivity extends AppCompatActivity {

    private ActivityDocumentBinding binding;
    private ApiService apiService;
    private InventoryItemAdapter adapter;
    private InventoryDocument currentDocument;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDocumentBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        apiService = ApiClient.getRetrofitInstance().create(ApiService.class);

        // Получаем ID документа из Intent
        String documentId = getIntent().getStringExtra("DOCUMENT_ID");

        setupRecyclerView();

        // Загружаем данные документа
        if (documentId != null && !documentId.isEmpty()) {
            loadDocumentDetails(documentId);
        } else {
            Toast.makeText(this, "ID документа отсутствует", Toast.LENGTH_SHORT).show();
        }

        binding.fabScan.setOnClickListener(v -> new IntentIntegrator(this).initiateScan());

        binding.sendButton.setOnClickListener(v -> {
            if (currentDocument != null) {
                sendDocumentToServer();
            } else {
                Toast.makeText(this, "Документ не загружен", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupRecyclerView() {
        adapter = new InventoryItemAdapter(new ArrayList<>());
        binding.itemsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.itemsRecyclerView.setAdapter(adapter);
    }

    private void loadDocumentDetails(String documentId) {
        apiService.getInventoryDocumentById(documentId).enqueue(new Callback<InventoryDocument>() {
            @Override
            public void onResponse(Call<InventoryDocument> call, Response<InventoryDocument> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentDocument = response.body();
                    List<InventoryItem> items = currentDocument.getItems();
                    if (items == null) items = new ArrayList<>();
                    adapter.updateItems(items);
                } else {
                    String errorMsg = "Не удалось загрузить документ";
                    try {
                        if (response.errorBody() != null) errorMsg += ": " + response.errorBody().string();
                    } catch (IOException e) {
                        errorMsg += " (ошибка чтения ответа)";
                    }
                    Toast.makeText(InventoryActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<InventoryDocument> call, Throwable t) {
                Toast.makeText(InventoryActivity.this, "Ошибка сети: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null && result.getContents() != null) {
            processScan(result.getContents());
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void processScan(String scannedBarcode) {
        if (currentDocument == null || currentDocument.getItems() == null || scannedBarcode == null) {
            Toast.makeText(this, "Документ не готов для сканирования", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean found = false;
        List<InventoryItem> items = currentDocument.getItems();

        for (int i = 0; i < items.size(); i++) {
            InventoryItem item = items.get(i);
            if (scannedBarcode.equals(item.getSeriya())) {
                item.setKolichestvoFakt(item.getKolichestvoFakt() + 1);
                item.setFound(true);
                adapter.notifyItemChanged(i);

                showResultDialog("Серия найдена!", "Товар: " + item.getNomenklatura());
                playSuccessSound();
                vibrateSuccess();

                found = true;
                break;
            }
        }

        if (!found) {
            showResultDialog("Ошибка", "Серия " + scannedBarcode + " не найдена.");
            playErrorSound();
            vibrateError();
        }
    }

    private void vibrateSuccess() {
        try {
            Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(100);
                }
            }
        } catch (Exception e) {
            Log.e("Vibration", "Ошибка вибрации успеха", e);
        }
    }

    private void vibrateError() {
        try {
            Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(new long[]{0, 100, 100, 100}, new int[]{0, 255, 0, 255}, -1));
                } else {
                    vibrator.vibrate(new long[]{0, 100, 100, 100}, -1);
                }
            }
        } catch (Exception e) {
            Log.e("Vibration", "Ошибка вибрации ошибки", e);
        }
    }

    private void playSuccessSound() {
        try {
            MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.scan_success);
            if (mediaPlayer != null) {
                mediaPlayer.setOnCompletionListener(MediaPlayer::release);
                mediaPlayer.start();
            }
        } catch (Exception e) {
            Log.e("Sound", "Ошибка воспроизведения звука успеха", e);
        }
    }

    private void playErrorSound() {
        try {
            MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.scan_error);
            if (mediaPlayer != null) {
                mediaPlayer.setOnCompletionListener(MediaPlayer::release);
                mediaPlayer.start();
            }
        } catch (Exception e) {
            Log.e("Sound", "Ошибка воспроизведения звука ошибки", e);
        }
    }

    private void sendDocumentToServer() {
        if (currentDocument == null) return;

        apiService.updateInventoryDocument(currentDocument).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(InventoryActivity.this, "Документ успешно отправлен!", Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    Toast.makeText(InventoryActivity.this, "Ошибка отправки документа", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(InventoryActivity.this, "Ошибка сети при отправке: " + t.getMessage(), Toast.LENGTH_SHORT).show();
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
