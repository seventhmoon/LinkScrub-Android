package com.androidfung.linkscrub.data.repository

import com.androidfung.linkscrub.data.local.HistoryDao
import com.androidfung.linkscrub.data.local.HistoryEntity
import com.androidfung.linkscrub.data.model.HistoryItem
import com.androidfung.linkscrub.data.model.OpenGraphMetadata
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray

interface HistoryRepository {
    fun getAllHistory(): Flow<List<HistoryItem>>
    suspend fun getHistoryById(id: Long): HistoryItem?
    fun searchHistory(query: String): Flow<List<HistoryItem>>
    suspend fun saveCleanedUrl(
        originalUrl: String,
        unshortenedUrl: String?,
        cleanedUrl: String,
        removedParams: List<String>,
        metadata: OpenGraphMetadata? = null
    ): Long
    suspend fun delete(id: Long)
    suspend fun clearAll()
}

class RoomHistoryRepository(
    private val historyDao: HistoryDao
) : HistoryRepository {

    override fun getAllHistory(): Flow<List<HistoryItem>> {
        return historyDao.getAllHistory().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getHistoryById(id: Long): HistoryItem? {
        return historyDao.getHistoryById(id)?.toDomain()
    }

    override fun searchHistory(query: String): Flow<List<HistoryItem>> {
        return historyDao.searchHistory(query).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun saveCleanedUrl(
        originalUrl: String,
        unshortenedUrl: String?,
        cleanedUrl: String,
        removedParams: List<String>,
        metadata: OpenGraphMetadata?
    ): Long {
        val entity = HistoryEntity(
            originalUrl = originalUrl,
            unshortenedUrl = unshortenedUrl,
            cleanedUrl = cleanedUrl,
            removedParamsJson = serializeParams(removedParams),
            title = metadata?.title,
            description = metadata?.description,
            imageUrl = metadata?.imageUrl,
            siteName = metadata?.siteName,
            timestamp = System.currentTimeMillis()
        )
        return historyDao.insert(entity)
    }

    override suspend fun delete(id: Long) {
        historyDao.deleteById(id)
    }

    override suspend fun clearAll() {
        historyDao.clearAll()
    }

    private fun HistoryEntity.toDomain(): HistoryItem {
        val metadata = if (title != null || description != null || imageUrl != null || siteName != null) {
            OpenGraphMetadata(
                title = title,
                description = description,
                imageUrl = imageUrl,
                siteName = siteName
            )
        } else {
            null
        }

        return HistoryItem(
            id = id,
            originalUrl = originalUrl,
            unshortenedUrl = unshortenedUrl,
            cleanedUrl = cleanedUrl,
            removedParams = deserializeParams(removedParamsJson),
            metadata = metadata,
            timestamp = timestamp
        )
    }

    private fun serializeParams(params: List<String>): String {
        val jsonArray = JSONArray()
        params.forEach { jsonArray.put(it) }
        return jsonArray.toString()
    }

    private fun deserializeParams(jsonString: String): List<String> {
        return try {
            val jsonArray = JSONArray(jsonString)
            val list = mutableListOf<String>()
            for (i in 0 until jsonArray.length()) {
                list.add(jsonArray.getString(i))
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }
}
