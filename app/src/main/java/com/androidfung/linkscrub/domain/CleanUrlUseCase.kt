package com.androidfung.linkscrub.domain

import com.androidfung.linkscrub.data.model.CleanedUrlResult
import com.androidfung.linkscrub.data.network.OpenGraphScraper
import com.androidfung.linkscrub.data.network.RedirectUnshortener
import com.androidfung.linkscrub.data.repository.HistoryRepository

class CleanUrlUseCase(
    private val urlCleaner: UrlCleaner,
    private val redirectUnshortener: RedirectUnshortener,
    private val openGraphScraper: OpenGraphScraper,
    private val historyRepository: HistoryRepository? = null
) {

    suspend operator fun invoke(
        inputUrl: String,
        unshorten: Boolean = true,
        fetchMetadata: Boolean = true,
        saveToHistory: Boolean = true,
        cleaner: UrlCleaner = urlCleaner
    ): CleanedUrlResult {
        // Step 1: Pre-clean the initial input URL
        val initialClean = cleaner.cleanUrl(inputUrl)
        var urlToProcess = initialClean.cleanedUrl
        var unshortenedUrl: String? = null
        var redirectChain: List<String> = emptyList()

        // Step 2: If unshorten requested, trace redirects
        if (unshorten) {
            val unshortenResult = redirectUnshortener.unshorten(urlToProcess)
            if (unshortenResult.isRedirected) {
                unshortenedUrl = unshortenResult.finalUrl
                urlToProcess = unshortenResult.finalUrl
                redirectChain = unshortenResult.redirectChain
            }
        }

        // Step 3: Clean the final / unshortened URL (removes any tracking params added by intermediate redirects)
        val finalClean = cleaner.cleanUrl(urlToProcess)
        val totalRemovedParams = (initialClean.removedParams + finalClean.removedParams).distinct()

        // Step 4: Fetch Open Graph metadata if requested
        val metadata = if (fetchMetadata) {
            openGraphScraper.scrape(finalClean.cleanedUrl)
        } else {
            null
        }

        // Step 5: Save to repository history if enabled
        if (saveToHistory && historyRepository != null) {
            historyRepository.saveCleanedUrl(
                originalUrl = inputUrl,
                unshortenedUrl = unshortenedUrl,
                cleanedUrl = finalClean.cleanedUrl,
                removedParams = totalRemovedParams,
                metadata = metadata
            )
        }

        return CleanedUrlResult(
            originalUrl = inputUrl,
            unshortenedUrl = unshortenedUrl,
            cleanedUrl = finalClean.cleanedUrl,
            removedParams = totalRemovedParams,
            metadata = metadata,
            redirectChain = redirectChain
        )
    }
}
