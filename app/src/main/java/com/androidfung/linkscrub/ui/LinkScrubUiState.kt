package com.androidfung.linkscrub.ui

import androidx.compose.runtime.Immutable
import com.androidfung.linkscrub.data.model.OpenGraphMetadata

/**
 * Represents each parameter found in a URL, its default removal state,
 * and whether the user chooses to remove it or keep it.
 */
@Immutable
data class ParameterChipItem(
    val key: String,
    val value: String,
    val isTracking: Boolean,
    val isMarkedForRemoval: Boolean
)

/**
 * Steps in the guided 3-step sanitization wizard.
 */
enum class WizardStep(val stepNumber: Int, val title: String) {
    INPUT(1, "Input Link"),
    INSPECT(2, "Inspect & Filter"),
    PREVIEW(3, "Preview & Action")
}

/**
 * State holding all UI attributes for LinkScrub screen.
 */
@Immutable
data class LinkScrubUiState(
    val currentStep: WizardStep = WizardStep.INPUT,
    val inputUrl: String = "",
    val detectedClipboardUrl: String? = null,
    val isQuickCleanEnabled: Boolean = false,

    // Step 2 state
    val detectedParameters: List<ParameterChipItem> = emptyList(),
    val isUnshortening: Boolean = false,
    val unshortenedUrl: String? = null,
    val isRedirected: Boolean = false,
    val redirectChain: List<String> = emptyList(),

    // Step 3 / Result state
    val cleanedUrl: String = "",
    val removedParamsCount: Int = 0,
    val metadata: OpenGraphMetadata? = null,
    val isLoadingMetadata: Boolean = false,

    // General status
    val isProcessing: Boolean = false,
    val userMessage: String? = null,
    val isQuickCleanCopiedNotification: Boolean = false,

    // History & Supporting Pane state
    val isHistoryPaneVisible: Boolean = false,
    val historySearchQuery: String = "",

    // Help & FAQ sheet state
    val isHelpSheetVisible: Boolean = false
)
