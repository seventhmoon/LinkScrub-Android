package com.androidfung.linkscrub.data.model

import androidx.compose.runtime.Immutable

@Immutable
data class CleanedUrlResult(
    val originalUrl: String,
    val unshortenedUrl: String?,
    val cleanedUrl: String,
    val removedParams: List<String> = emptyList(),
    val metadata: OpenGraphMetadata? = null,
    val redirectChain: List<String> = emptyList()
)
