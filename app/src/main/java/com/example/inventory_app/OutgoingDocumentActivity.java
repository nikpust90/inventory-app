package com.example.inventory_app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OutgoingDocumentActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private OutgoingItemAdapter adapter;
    private BarcodeReceiver barcodeReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_outgoing_document);

        Long documentId = getIntent().getLongExtra("DOCUMENT_ID", 1L);
        Log.d("OutgoingDocumentActivity", "Received Document ID: " + documentId);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        barcodeReceiver = new BarcodeReceiver();

        IntentFilter filter = new IntentFilter("com.xcheng.scanner.action.BARCODE_DECODING_BROADCAST");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(barcodeReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(barcodeReceiver, filter);
        }

        fetchOutgoingDocument(documentId);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(barcodeReceiver);
    }

    private void fetchOutgoingDocument(Long documentId) {
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        Call<OutgoingDocument> call = apiService.getOutgoingDocumentById(documentId);

        call.enqueue(new Callback<OutgoingDocument>() {
            @Override
            public void onResponse(Call<OutgoingDocument> call, Response<OutgoingDocument> response) {
                if (response.isSuccessful() && response.body() != null) {
                    OutgoingDocument document = response.body();
                    adapter = new OutgoingItemAdapter(document.getItems());
                    recyclerView.setAdapter(adapter);
                } else {
                    Log.e("OutgoingDocumentActivity", "Failed to fetch document: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<OutgoingDocument> call, Throwable t) {
                Log.e("OutgoingDocumentActivity", "API call failed: " + t.getMessage());
            }
        });
    }

    private class BarcodeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String barcode = intent.getStringExtra("EXTRA_BARCODE_DECODING_DATA");
            if (barcode != null && !barcode.isEmpty() && adapter != null) {
                adapter.updateQuantityByBarcode(barcode);
            }
        }
    }
}

