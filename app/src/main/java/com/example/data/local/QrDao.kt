package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.QrCodeItem
import kotlinx.coroutines.flow.Flow

@Dao
interface QrDao {
    @Query("SELECT * FROM qr_items ORDER BY createdAt DESC")
    fun getAllItems(): Flow<List<QrCodeItem>>

    @Query("SELECT * FROM qr_items WHERE id = :id")
    suspend fun getItemById(id: Long): QrCodeItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: QrCodeItem): Long

    @Query("DELETE FROM qr_items WHERE id = :id")
    suspend fun deleteItemById(id: Long)

    @Query("DELETE FROM qr_items")
    suspend fun deleteAll()

    @Query("SELECT * FROM qr_items WHERE isSynced = 0")
    suspend fun getUnsyncedItems(): List<QrCodeItem>

    @Query("UPDATE qr_items SET firestoreId = :firestoreId, isSynced = 1 WHERE id = :id")
    suspend fun markSynced(id: Long, firestoreId: String)
}
