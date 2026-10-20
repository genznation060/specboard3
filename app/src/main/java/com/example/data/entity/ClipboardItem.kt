package com.example.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "clipboard_items",
    indices = [
        Index(value = ["createdAt"]),
        Index(value = ["isFavorite"]),
        Index(value = ["isTsv"])
    ]
)
data class ClipboardItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val content: String,
    val isTsv: Boolean = false,
    val detectedPhonesCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val isPinned: Boolean = false
)
