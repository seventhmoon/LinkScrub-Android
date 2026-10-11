package com.androidfung.linkscrub.data.model

import androidx.compose.runtime.Immutable

@Immutable
data class OpenGraphMetadata(
    val title: String? = null,
    val description: String? = null,
    val imageUrl: String? = null,
    val siteName: String? = null,
    val allTags: Map<String, String> = emptyMap()
)
