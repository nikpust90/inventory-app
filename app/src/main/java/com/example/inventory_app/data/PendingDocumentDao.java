package com.example.inventory_app.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface PendingDocumentDao {
    @Insert
    void insert(PendingDocument doc);

    @Query("SELECT * FROM pending_documents")
    List<PendingDocument> getAll();

    @Query("DELETE FROM pending_documents WHERE id = :id")
    void deleteById(int id);
}
