package com.androidfung.linkscrub.data.model

data class CleanedUrlResult(
    val originalUrl: String,
    val unshortenedUrl: String?,
    val cleanedUrl: String,
    val removedParams: List<String> = emptyList(),
    val metadata: OpenGraphMetadata? = null
)
