package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.entity.ClipboardItem
import kotlinx.coroutines.flow.Flow

@Dao
interface ClipboardDao {

    @Query("SELECT * FROM clipboard_items ORDER BY isPinned DESC, createdAt DESC")
    fun getAllItems(): Flow<List<ClipboardItem>>

    @Query("SELECT * FROM clipboard_items WHERE isTsv = 1 ORDER BY isPinned DESC, createdAt DESC")
    fun getTsvItems(): Flow<List<ClipboardItem>>

    @Query("SELECT * FROM clipboard_items WHERE isFavorite = 1 ORDER BY createdAt DESC")
    fun getFavoriteItems(): Flow<List<ClipboardItem>>

    @Query("SELECT * FROM clipboard_items ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestItem(): ClipboardItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ClipboardItem): Long

    @Delete
    suspend fun deleteItem(item: ClipboardItem)

    @Query("DELETE FROM clipboard_items WHERE id = :id")
    suspend fun deleteItemById(id: Long)

    @Query("UPDATE clipboard_items SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: Long, isFavorite: Boolean)

    @Query("UPDATE clipboard_items SET isPinned = :isPinned WHERE id = :id")
    suspend fun setPinned(id: Long, isPinned: Boolean)

    @Query("DELETE FROM clipboard_items")
    suspend fun clearAll()
}
