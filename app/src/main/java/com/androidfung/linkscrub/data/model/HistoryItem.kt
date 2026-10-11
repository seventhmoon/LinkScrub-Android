package com.androidfung.linkscrub.data.model

import androidx.compose.runtime.Immutable

@Immutable
data class HistoryItem(
    val id: Long = 0,
    val originalUrl: String,
    val unshortenedUrl: String?,
    val cleanedUrl: String,
    val removedParams: List<String> = emptyList(),
    val metadata: OpenGraphMetadata? = null,
    val timestamp: Long = System.currentTimeMillis()
)
