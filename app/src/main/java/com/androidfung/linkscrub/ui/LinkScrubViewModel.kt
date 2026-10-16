package com.androidfung.linkscrub.ui

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.androidfung.linkscrub.R
import com.androidfung.linkscrub.data.local.LinkScrubDatabase
import com.androidfung.linkscrub.data.network.JsoupOpenGraphScraper
import com.androidfung.linkscrub.data.network.OkHttpRedirectUnshortener
import com.androidfung.linkscrub.data.network.OpenGraphScraper
import com.androidfung.linkscrub.data.network.RedirectUnshortener
import com.androidfung.linkscrub.data.repository.HistoryRepository
import com.androidfung.linkscrub.data.repository.RoomHistoryRepository
import com.androidfung.linkscrub.domain.CleanUrlUseCase
import com.androidfung.linkscrub.domain.UrlCleaner
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class LinkScrubViewModel(
    application: Application,
    private val urlCleaner: UrlCleaner = UrlCleaner(),
    private val redirectUnshortener: RedirectUnshortener = OkHttpRedirectUnshortener(),
    private val openGraphScraper: OpenGraphScraper = JsoupOpenGraphScraper(),
    private val historyRepository: HistoryRepository = RoomHistoryRepository(
        LinkScrubDatabase.getInstance(application).historyDao()
    ),
    private val cleanUrlUseCase: CleanUrlUseCase = CleanUrlUseCase(
        urlCleaner = urlCleaner,
        redirectUnshortener = redirectUnshortener,
        openGraphScraper = openGraphScraper,
        historyRepository = historyRepository
    )
) : AndroidViewModel(application) {

    private var cleanJob: Job? = null
    private var inspectJob: Job? = null
    private var metadataJob: Job? = null

    private fun getString(resId: Int, vararg formatArgs: Any): String {
        return try {
            getApplication<Application>().getString(resId, *formatArgs)
        } catch (_: Exception) {
            // Fallback for tests using mock/uninitialized Application
            when (resId) {
                R.string.msg_enter_valid_url -> "Please enter or paste a valid URL."
                R.string.msg_enter_valid_http_url -> "Please enter a valid HTTP or HTTPS URL."
                R.string.msg_cleaned_copied -> "Cleaned URL copied to clipboard!"
                R.string.msg_cleaned_copied_with_savings -> "Cleaned URL copied to clipboard! (saved ${formatArgs.firstOrNull()} chars)"
                R.string.msg_failed_to_clean_url -> "Failed to clean URL: ${formatArgs.firstOrNull()}"
                R.string.msg_unknown_error -> "Unknown error"
                R.string.msg_link_sanitized_params_removed -> "Link sanitized: ${formatArgs.getOrNull(0)} params removed (${formatArgs.getOrNull(1)} chars saved)"
                R.string.msg_link_cleaned -> "Link cleaned"
                R.string.msg_no_valid_url_in_shared_text -> "No valid URL found in shared text."
                R.string.msg_scanned_qr_code_url -> "Scanned QR code URL"
                R.string.msg_loaded_link_from_history -> "Loaded link from history"
                R.string.msg_item_removed_from_history -> "Item removed from history"
                R.string.msg_all_history_cleared -> "All history cleared"
                else -> ""
            }
        }
    }

    private val _uiState = MutableStateFlow(LinkScrubUiState())
    val uiState: StateFlow<LinkScrubUiState> = _uiState.asStateFlow()

    private val _historySearchQuery = MutableStateFlow("")
    val historyItems: StateFlow<List<com.androidfung.linkscrub.data.model.HistoryItem>> =
        _historySearchQuery
            .flatMapLatest { query ->
                if (query.isBlank()) {
                    historyRepository.getAllHistory()
                } else {
                    historyRepository.searchHistory(query.trim())
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    init {
        checkClipboardForUrl()
    }

    private fun cancelInFlightJobs() {
        cleanJob?.cancel()
        cleanJob = null
        inspectJob?.cancel()
        inspectJob = null
        metadataJob?.cancel()
        metadataJob = null
    }

    /**
     * Checks device clipboard for any valid URL and surfaces it to the user.
     */
    fun checkClipboardForUrl() {
        try {
            val clipboard = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            if (clipboard != null && clipboard.hasPrimaryClip()) {
                val clipData = clipboard.primaryClip
                if (clipData != null && clipData.itemCount > 0) {
                    val text = clipData.getItemAt(0)?.text?.toString()?.trim()
                    if (!text.isNullOrBlank()) {
                        val match = UrlCleaner.URL_REGEX.find(text)
                        val extractedUrl = match?.value ?: if (text.startsWith("http://", ignoreCase = true) || text.startsWith("https://", ignoreCase = true)) text else null
                        if (extractedUrl != null && extractedUrl != _uiState.value.inputUrl) {
                            _uiState.update { it.copy(detectedClipboardUrl = extractedUrl) }
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Clipboard access might throw SecurityException or fail on certain OEM/Android policies
        }
    }

    fun onInputUrlChanged(url: String) {
        cancelInFlightJobs()
        _uiState.update { it.copy(inputUrl = url, userMessage = null) }
    }

    fun useClipboardUrl() {
        val detected = _uiState.value.detectedClipboardUrl ?: return
        cancelInFlightJobs()
        _uiState.update { it.copy(inputUrl = detected) }
        processInputUrl(detected)
    }

    fun setQuickCleanEnabled(enabled: Boolean) {
        _uiState.update { it.copy(isQuickCleanEnabled = enabled) }
    }

    fun clearInput() {
        cancelInFlightJobs()
        _uiState.update {
            it.copy(
                inputUrl = "",
                currentStep = WizardStep.INPUT,
                detectedParameters = emptyList(),
                unshortenedUrl = null,
                isRedirected = false,
                redirectChain = emptyList(),
                cleanedUrl = "",
                metadata = null,
                userMessage = null,
                isQuickCleanCopiedNotification = false
            )
        }
    }

    /**
     * Handles Proceeding from Step 1.
     * If Quick-Clean is enabled, immediately cleans, unshortens, copies to clipboard, and goes to Step 3.
     * Otherwise, inspects query parameters and proceeds to Step 2.
     */
    fun onProceedFromInput() {
        val input = _uiState.value.inputUrl.trim()
        if (input.isBlank()) {
            _uiState.update { it.copy(userMessage = getString(R.string.msg_enter_valid_url)) }
            return
        }

        val parsed = input.toHttpUrlOrNull()
        if (parsed == null) {
            val withScheme = "https://$input"
            if (withScheme.toHttpUrlOrNull() != null) {
                cancelInFlightJobs()
                _uiState.update { it.copy(inputUrl = withScheme) }
                processInputUrl(withScheme)
                return
            } else {
                _uiState.update { it.copy(userMessage = getString(R.string.msg_enter_valid_http_url)) }
                return
            }
        }

        cancelInFlightJobs()
        processInputUrl(input)
    }

    /**
     * Triggered directly by the "Quick Clean" action button.
     * Always performs instant scrubbing, clipboard copy, and preview.
     */
    fun onQuickCleanClicked() {
        val input = _uiState.value.inputUrl.trim()
        if (input.isBlank()) {
            _uiState.update { it.copy(userMessage = getString(R.string.msg_enter_valid_url)) }
            return
        }
        val effectiveUrl = if (input.toHttpUrlOrNull() == null && "https://$input".toHttpUrlOrNull() != null) {
            val withScheme = "https://$input"
            _uiState.update { it.copy(inputUrl = withScheme) }
            withScheme
        } else {
            input
        }
        cancelInFlightJobs()
        executeQuickClean(effectiveUrl)
    }

    /**
     * Triggered directly by the "Tune / Inspect" action button.
     * Always opens step 2 parameter inspection.
     */
    fun onTuneInspectClicked() {
        val input = _uiState.value.inputUrl.trim()
        if (input.isBlank()) {
            _uiState.update { it.copy(userMessage = getString(R.string.msg_enter_valid_url)) }
            return
        }
        val effectiveUrl = if (input.toHttpUrlOrNull() == null && "https://$input".toHttpUrlOrNull() != null) {
            val withScheme = "https://$input"
            _uiState.update { it.copy(inputUrl = withScheme) }
            withScheme
        } else {
            input
        }
        cancelInFlightJobs()
        prepareStep2Inspection(effectiveUrl)
    }

    private fun processInputUrl(url: String) {
        if (_uiState.value.isQuickCleanEnabled) {
            executeQuickClean(url)
        } else {
            prepareStep2Inspection(url)
        }
    }

    /**
     * Direct Quick-Clean mode: scrubs tracking parameters, unshortens,
     * copies clean URL to clipboard, and saves to history.
     */
    fun executeQuickClean(rawUrl: String) {
        cancelInFlightJobs()
        cleanJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isProcessing = true,
                    userMessage = null,
                    isQuickCleanCopiedNotification = false
                )
            }
            try {
                val result = cleanUrlUseCase(
                    inputUrl = rawUrl,
                    unshorten = true,
                    fetchMetadata = true,
                    saveToHistory = true
                )

                // Log privacy-safe analytics event
                com.androidfung.linkscrub.util.AnalyticsHelper.logLinkScrubbed(
                    paramCount = result.removedParams.size,
                    unshortened = result.unshortenedUrl != null,
                    isQuickClean = true
                )

                // Copy to system clipboard
                copyToClipboard(result.cleanedUrl)

                val charsSaved = (rawUrl.length - result.cleanedUrl.length).coerceAtLeast(0)
                val feedback = if (charsSaved > 0) {
                    getString(R.string.msg_cleaned_copied_with_savings, charsSaved)
                } else {
                    getString(R.string.msg_cleaned_copied)
                }

                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        cleanedUrl = result.cleanedUrl,
                        unshortenedUrl = result.unshortenedUrl,
                        isRedirected = result.unshortenedUrl != null,
                        removedParamsCount = result.removedParams.size,
                        metadata = result.metadata,
                        currentStep = WizardStep.PREVIEW,
                        isQuickCleanCopiedNotification = true,
                        userMessage = feedback
                    )
                }
            } catch (e: Exception) {
                com.androidfung.linkscrub.util.AnalyticsHelper.recordException(e)
                val errorMsg = e.localizedMessage ?: getString(R.string.msg_unknown_error)
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        userMessage = getString(R.string.msg_failed_to_clean_url, errorMsg)
                    )
                }
            }
        }
    }

    /**
     * Step 2 Preparation: Parse URL parameters and check redirects.
     */
    private fun prepareStep2Inspection(rawUrl: String) {
        cancelInFlightJobs()
        inspectJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isProcessing = true,
                    isUnshortening = true,
                    currentStep = WizardStep.INSPECT,
                    userMessage = null
                )
            }

            // Check unshortening first so user sees resolved URL & parameters if redirected
            val unshortenResult = redirectUnshortener.unshorten(rawUrl)
            val effectiveUrl = if (unshortenResult.isRedirected) unshortenResult.finalUrl else rawUrl

            val parsed = effectiveUrl.toHttpUrlOrNull()
            val chips = mutableListOf<ParameterChipItem>()

            if (parsed != null) {
                for (name in parsed.queryParameterNames) {
                    val values = parsed.queryParameterValues(name)
                    val valueStr = values.filterNotNull().joinToString(",")
                    val isTracking = urlCleaner.isTrackingParam(name, parsed.host)
                    chips.add(
                        ParameterChipItem(
                            key = name,
                            value = valueStr,
                            isTracking = isTracking,
                            // By default, tracking parameters are marked for removal
                            isMarkedForRemoval = isTracking
                        )
                    )
                }
            }

            // Recalculate candidate clean URL
            val candidateClean = computeCleanedUrlFromChips(effectiveUrl, chips)

            _uiState.update {
                it.copy(
                    isProcessing = false,
                    isUnshortening = false,
                    unshortenedUrl = if (unshortenResult.isRedirected) unshortenResult.finalUrl else null,
                    isRedirected = unshortenResult.isRedirected,
                    redirectChain = unshortenResult.redirectChain,
                    detectedParameters = chips,
                    cleanedUrl = candidateClean,
                    removedParamsCount = chips.count { chip -> chip.isMarkedForRemoval }
                )
            }
        }
    }

    fun toggleParameterRemoval(paramKey: String) {
        val currentChips = _uiState.value.detectedParameters
        val updatedChips = currentChips.map { chip ->
            if (chip.key == paramKey) {
                chip.copy(isMarkedForRemoval = !chip.isMarkedForRemoval)
            } else {
                chip
            }
        }

        val effectiveUrl = _uiState.value.unshortenedUrl ?: _uiState.value.inputUrl
        val candidateClean = computeCleanedUrlFromChips(effectiveUrl, updatedChips)

        _uiState.update {
            it.copy(
                detectedParameters = updatedChips,
                cleanedUrl = candidateClean,
                removedParamsCount = updatedChips.count { chip -> chip.isMarkedForRemoval }
            )
        }
    }

    fun selectAllTrackingParamsForRemoval() {
        val currentChips = _uiState.value.detectedParameters
        val updatedChips = currentChips.map { chip ->
            if (chip.isTracking) chip.copy(isMarkedForRemoval = true) else chip
        }
        val effectiveUrl = _uiState.value.unshortenedUrl ?: _uiState.value.inputUrl
        val candidateClean = computeCleanedUrlFromChips(effectiveUrl, updatedChips)

        _uiState.update {
            it.copy(
                detectedParameters = updatedChips,
                cleanedUrl = candidateClean,
                removedParamsCount = updatedChips.count { chip -> chip.isMarkedForRemoval }
            )
        }
    }

    fun resetAllParameters() {
        val currentChips = _uiState.value.detectedParameters
        val updatedChips = currentChips.map { chip -> chip.copy(isMarkedForRemoval = false) }
        val effectiveUrl = _uiState.value.unshortenedUrl ?: _uiState.value.inputUrl
        val candidateClean = computeCleanedUrlFromChips(effectiveUrl, updatedChips)

        _uiState.update {
            it.copy(
                detectedParameters = updatedChips,
                cleanedUrl = candidateClean,
                removedParamsCount = 0
            )
        }
    }

    /**
     * Completes Step 2 and proceeds to Step 3 (Preview & Actions).
     * Fetches Open Graph Metadata and saves to History.
     */
    fun onProceedToPreview() {
        cancelInFlightJobs()
        metadataJob = viewModelScope.launch {
            val finalCleanUrl = _uiState.value.cleanedUrl.ifBlank {
                _uiState.value.unshortenedUrl ?: _uiState.value.inputUrl
            }

            _uiState.update {
                it.copy(
                    currentStep = WizardStep.PREVIEW,
                    isLoadingMetadata = true,
                    userMessage = null
                )
            }

            val metadata = try {
                openGraphScraper.scrape(finalCleanUrl)
            } catch (e: Exception) {
                com.androidfung.linkscrub.util.AnalyticsHelper.recordException(e)
                null
            }

            val removedParams = _uiState.value.detectedParameters
                .filter { it.isMarkedForRemoval }
                .map { it.key }

            // Log privacy-safe analytics event
            com.androidfung.linkscrub.util.AnalyticsHelper.logLinkScrubbed(
                paramCount = removedParams.size,
                unshortened = _uiState.value.unshortenedUrl != null,
                isQuickClean = false
            )

            // Save to history repository
            try {
                historyRepository.saveCleanedUrl(
                    originalUrl = _uiState.value.inputUrl,
                    unshortenedUrl = _uiState.value.unshortenedUrl,
                    cleanedUrl = finalCleanUrl,
                    removedParams = removedParams,
                    metadata = metadata
                )
            } catch (e: Exception) {
                com.androidfung.linkscrub.util.AnalyticsHelper.recordException(e)
            }

            val originalUrl = _uiState.value.inputUrl
            val charsSaved = (originalUrl.length - finalCleanUrl.length).coerceAtLeast(0)
            val cleanFeedback = if (removedParams.isNotEmpty()) {
                getString(R.string.msg_link_sanitized_params_removed, removedParams.size, charsSaved)
            } else {
                getString(R.string.msg_link_cleaned)
            }

            _uiState.update {
                it.copy(
                    isLoadingMetadata = false,
                    metadata = metadata,
                    cleanedUrl = finalCleanUrl,
                    removedParamsCount = removedParams.size,
                    userMessage = cleanFeedback
                )
            }
        }
    }

    fun goToStep(step: WizardStep) {
        _uiState.update { it.copy(currentStep = step, userMessage = null) }
    }

    fun copyCleanedUrlToClipboard() {
        val url = _uiState.value.cleanedUrl
        if (url.isNotBlank()) {
            copyToClipboard(url)
            val originalUrl = _uiState.value.inputUrl
            val charsSaved = (originalUrl.length - url.length).coerceAtLeast(0)
            val feedback = if (charsSaved > 0) {
                getString(R.string.msg_cleaned_copied_with_savings, charsSaved)
            } else {
                getString(R.string.msg_cleaned_copied)
            }
            _uiState.update { it.copy(userMessage = feedback) }
        }
    }

    fun dismissUserMessage() {
        _uiState.update { it.copy(userMessage = null, isQuickCleanCopiedNotification = false) }
    }

    /**
     * Handles incoming URL shared from external apps (e.g. Android Share Sheet or View intent).
     * If Quick-Clean is enabled, immediately cleans, copies, and shows result.
     * Otherwise, places URL into input and starts inspection.
     */
    fun handleSharedUrl(sharedText: String) {
        cancelInFlightJobs()
        val match = UrlCleaner.URL_REGEX.find(sharedText)
        val extractedUrl = match?.value ?: if (
            sharedText.startsWith("http://", ignoreCase = true) ||
            sharedText.startsWith("https://", ignoreCase = true)
        ) sharedText.trim() else null

        if (extractedUrl != null) {
            _uiState.update {
                it.copy(
                    inputUrl = extractedUrl,
                    currentStep = WizardStep.INPUT,
                    userMessage = null
                )
            }
            processInputUrl(extractedUrl)
        } else {
            _uiState.update {
                it.copy(userMessage = getString(R.string.msg_no_valid_url_in_shared_text))
            }
        }
    }

    /**
     * Handles URL extracted from scanned QR code.
     * Updates inputUrl, extracts URLs if present, and provides user feedback.
     */
    fun onQrCodeScanned(scannedText: String) {
        cancelInFlightJobs()
        val trimmed = scannedText.trim()
        val match = UrlCleaner.URL_REGEX.find(trimmed)
        val extractedUrl = match?.value ?: if (
            trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)
        ) trimmed else null

        val finalUrl = extractedUrl ?: trimmed
        _uiState.update {
            it.copy(
                inputUrl = finalUrl,
                userMessage = getString(R.string.msg_scanned_qr_code_url)
            )
        }
        if (extractedUrl != null) {
            processInputUrl(extractedUrl)
        }
    }

    // --- History & Supporting Pane Controls ---

    fun onHistorySearchQueryChanged(query: String) {
        if (query.isNotBlank() && _historySearchQuery.value.isBlank()) {
            com.androidfung.linkscrub.util.AnalyticsHelper.logHistorySearched()
        }
        _historySearchQuery.value = query
        _uiState.update { it.copy(historySearchQuery = query) }
    }

    fun toggleHistoryPane() {
        _uiState.update { it.copy(isHistoryPaneVisible = !it.isHistoryPaneVisible) }
    }

    fun setHistoryPaneVisible(visible: Boolean) {
        _uiState.update { it.copy(isHistoryPaneVisible = visible) }
    }

    fun toggleHelpSheet() {
        _uiState.update { it.copy(isHelpSheetVisible = !it.isHelpSheetVisible) }
    }

    fun setHelpSheetVisible(visible: Boolean) {
        _uiState.update { it.copy(isHelpSheetVisible = visible) }
    }

    fun selectHistoryItem(item: com.androidfung.linkscrub.data.model.HistoryItem) {
        _uiState.update {
            it.copy(
                inputUrl = item.originalUrl,
                cleanedUrl = item.cleanedUrl,
                unshortenedUrl = item.unshortenedUrl,
                isRedirected = item.unshortenedUrl != null,
                removedParamsCount = item.removedParams.size,
                metadata = item.metadata,
                currentStep = WizardStep.PREVIEW,
                userMessage = getString(R.string.msg_loaded_link_from_history)
            )
        }
    }

    fun copyHistoryItem(item: com.androidfung.linkscrub.data.model.HistoryItem) {
        copyToClipboard(item.cleanedUrl)
        _uiState.update { it.copy(userMessage = getString(R.string.msg_cleaned_copied)) }
    }

    fun deleteHistoryItem(id: Long) {
        viewModelScope.launch {
            historyRepository.delete(id)
            _uiState.update { it.copy(userMessage = getString(R.string.msg_item_removed_from_history)) }
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            historyRepository.clearAll()
            _uiState.update { it.copy(userMessage = getString(R.string.msg_all_history_cleared)) }
        }
    }

    private fun copyToClipboard(text: String) {
        try {
            val clipboard = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clip = ClipData.newPlainText("Cleaned URL", text)
            clipboard?.setPrimaryClip(clip)
        } catch (_: Exception) {
        }
    }

    private fun computeCleanedUrlFromChips(
        rawUrl: String,
        chips: List<ParameterChipItem>
    ): String {
        val paramsToRemove = chips.filter { it.isMarkedForRemoval }.map { it.key }.toSet()
        return urlCleaner.computeCleanedUrlWithExcludedParams(rawUrl, paramsToRemove)
    }

    companion object {
        fun provideFactory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return LinkScrubViewModel(application) as T
                }
            }
    }
}
