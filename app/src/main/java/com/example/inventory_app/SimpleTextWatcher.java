package com.example.inventory_app;

import android.text.Editable;
import android.text.TextWatcher;

public abstract class SimpleTextWatcher implements TextWatcher {
    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        // Необязательно реализовывать
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        // Необязательно реализовывать
    }

    @Override
    public void afterTextChanged(Editable s) {
        onTextChanged(s.toString());
    }

    public abstract void onTextChanged(String text);
}