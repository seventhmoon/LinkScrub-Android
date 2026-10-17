package com.androidfung.linkscrub.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "history_entries",
    indices = [
        Index("timestamp"),
        Index("cleanedUrl")
    ]
)
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val originalUrl: String,
    val unshortenedUrl: String? = null,
    val cleanedUrl: String,
    val removedParamsJson: String = "[]",
    val title: String? = null,
    val description: String? = null,
    val imageUrl: String? = null,
    val siteName: String? = null,
    val allTagsJson: String = "{}",
    val timestamp: Long = System.currentTimeMillis()
)
