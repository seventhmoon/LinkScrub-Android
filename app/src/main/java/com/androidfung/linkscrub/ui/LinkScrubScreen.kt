package com.androidfung.linkscrub.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imeNestedScroll
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDragHandle
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.layout.AdaptStrategy
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.layout.PaneExpansionAnchor
import androidx.compose.material3.adaptive.layout.PaneExpansionState
import androidx.compose.material3.adaptive.layout.SupportingPaneScaffold
import androidx.compose.material3.adaptive.layout.SupportingPaneScaffoldDefaults
import androidx.compose.material3.adaptive.layout.SupportingPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldScope
import androidx.compose.material3.adaptive.layout.rememberPaneExpansionState
import androidx.compose.material3.adaptive.navigation.rememberSupportingPaneScaffoldNavigator
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.core.net.toUri
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import com.androidfung.linkscrub.R
import com.androidfung.linkscrub.data.model.HistoryItem
import com.androidfung.linkscrub.data.model.OpenGraphMetadata
import com.androidfung.linkscrub.ui.components.HelpFaqBottomSheet
import com.androidfung.linkscrub.ui.components.HistoryPanel
import com.androidfung.linkscrub.ui.components.OpenGraphPreviewCard
import com.androidfung.linkscrub.ui.components.ParameterChipGroup
import com.androidfung.linkscrub.ui.components.WizardStepIndicator
import com.androidfung.linkscrub.ui.theme.LinkScrubTheme

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class, ExperimentalLayoutApi::class)
@Composable
fun LinkScrubScreen(
    viewModel: LinkScrubViewModel,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    val uiState by viewModel.uiState.collectAsState()
    val historyItems by viewModel.historyItems.collectAsState()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    // Supporting Pane Navigator: hide supporting pane on compact single-pane layout by default instead of reflowing underneath
    val adaptStrategies = SupportingPaneScaffoldDefaults.adaptStrategies(
        supportingPaneAdaptStrategy = AdaptStrategy.Hide
    )
    val navigator = rememberSupportingPaneScaffoldNavigator(
        adaptStrategies = adaptStrategies
    )
    val isDualPane = navigator.scaffoldValue[SupportingPaneScaffoldRole.Supporting] == PaneAdaptedValue.Expanded

    // Pane expansion state for expanded dual-pane layout with safe snapping anchors
    val paneExpansionState = rememberPaneExpansionState(
        keyProvider = navigator.scaffoldValue,
        anchors = listOf(
            PaneExpansionAnchor.Offset.fromStart(280.dp),
            PaneExpansionAnchor.Proportion(0.5f),
            PaneExpansionAnchor.Offset.fromEnd(280.dp)
        ),
        initialAnchoredIndex = 1
    )

    LaunchedEffect(Unit) {
        viewModel.checkClipboardForUrl()
    }

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissUserMessage()
        }
    }

    SupportingPaneScaffold(
        directive = navigator.scaffoldDirective,
        value = navigator.scaffoldValue,
        paneExpansionState = if (isDualPane) paneExpansionState else null,
        paneExpansionDragHandle = if (isDualPane) {
            { state ->
                val interactionSource = remember { MutableInteractionSource() }
                VerticalDragHandle(
                    modifier = Modifier.paneExpansionDraggable(
                        state = state,
                        minTouchTargetSize = LocalMinimumInteractiveComponentSize.current,
                        interactionSource = interactionSource,
                    ),
                    interactionSource = interactionSource,
                )
            }
        } else null,
        modifier = modifier.fillMaxSize(),
        mainPane = {
            AnimatedPane {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = ScaffoldDefaults.contentWindowInsets,
                    topBar = {
                        TopAppBar(
                            windowInsets = TopAppBarDefaults.windowInsets,
                            title = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.CleaningServices,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = stringResource(R.string.app_name),
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = stringResource(R.string.app_tagline),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            },
                            actions = {
                                // Help & FAQ action button
                                IconButton(onClick = { viewModel.toggleHelpSheet() }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.HelpOutline,
                                        contentDescription = stringResource(R.string.cd_help_faq),
                                        tint = if (uiState.isHelpSheetVisible) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                                // History action button
                                IconButton(onClick = { viewModel.toggleHistoryPane() }) {
                                    Icon(
                                        imageVector = Icons.Rounded.History,
                                        contentDescription = stringResource(R.string.cd_history),
                                        tint = if (uiState.isHistoryPaneVisible || isDualPane) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                                if (uiState.currentStep != WizardStep.INPUT) {
                                    IconButton(onClick = { viewModel.clearInput() }) {
                                        Icon(
                                            imageVector = Icons.Rounded.RestartAlt,
                                            contentDescription = stringResource(R.string.action_start_over),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    },
                    snackbarHost = { SnackbarHost(snackbarHostState) }
                ) { innerPadding ->
                    val scrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .consumeWindowInsets(innerPadding)
                            .imePadding()
                            .imeNestedScroll()
                            .verticalScroll(scrollState)
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Step Indicator Wizard
                        WizardStepIndicator(
                            currentStep = uiState.currentStep,
                            onStepClick = { step -> viewModel.goToStep(step) }
                        )

                        // Animated step content transitions
                        AnimatedContent(
                            targetState = uiState.currentStep,
                            transitionSpec = {
                                if (targetState.stepNumber > initialState.stepNumber) {
                                    (slideInHorizontally { width -> width } + fadeIn(tween(300)))
                                        .togetherWith(slideOutHorizontally { width -> -width } + fadeOut(tween(300)))
                                } else {
                                    (slideInHorizontally { width -> -width } + fadeIn(tween(300)))
                                        .togetherWith(slideOutHorizontally { width -> width } + fadeOut(tween(300)))
                                }
                            },
                            label = "stepAnimation"
                        ) { targetStep ->
                            when (targetStep) {
                                WizardStep.INPUT -> {
                                    Step1InputView(
                                        inputUrl = uiState.inputUrl,
                                        detectedClipboardUrl = uiState.detectedClipboardUrl,
                                        isProcessing = uiState.isProcessing,
                                        onUrlChange = { viewModel.onInputUrlChanged(it) },
                                        onPasteClipboard = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            viewModel.useClipboardUrl()
                                        },
                                        onClearUrl = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            viewModel.clearInput()
                                        },
                                        onQuickClean = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            viewModel.onQuickCleanClicked()
                                        },
                                        onTuneInspect = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            viewModel.onTuneInspectClicked()
                                        },
                                        onScanQrCode = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            com.androidfung.linkscrub.util.QrCodeScanner.startScan(
                                                context = context,
                                                onSuccess = { scannedUrl ->
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    viewModel.onQrCodeScanned(scannedUrl)
                                                },
                                                onFailure = { error ->
                                                    // Record error and inform analytics
                                                    com.androidfung.linkscrub.util.AnalyticsHelper.recordException(error)
                                                },
                                                onCanceled = {
                                                    // User canceled or dismissed, no action needed
                                                }
                                            )
                                        }
                                    )
                                }

                                WizardStep.INSPECT -> {
                                    Step2InspectView(
                                        inputUrl = uiState.inputUrl,
                                        unshortenedUrl = uiState.unshortenedUrl,
                                        isRedirected = uiState.isRedirected,
                                        redirectChain = uiState.redirectChain,
                                        isUnshortening = uiState.isUnshortening,
                                        parameters = uiState.detectedParameters,
                                        cleanedUrlCandidate = uiState.cleanedUrl,
                                        onToggleParam = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            viewModel.toggleParameterRemoval(it)
                                        },
                                        onScrubAllTracking = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            viewModel.selectAllTrackingParamsForRemoval()
                                        },
                                        onKeepAll = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            viewModel.resetAllParameters()
                                        },
                                        onBack = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            viewModel.goToStep(WizardStep.INPUT)
                                        },
                                        onProceedToPreview = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            viewModel.onProceedToPreview()
                                        }
                                    )
                                }

                                WizardStep.PREVIEW -> {
                                    Step3PreviewView(
                                        originalUrl = uiState.inputUrl,
                                        cleanedUrl = uiState.cleanedUrl,
                                        unshortenedUrl = uiState.unshortenedUrl,
                                        removedParamsCount = uiState.removedParamsCount,
                                        metadata = uiState.metadata,
                                        isLoadingMetadata = uiState.isLoadingMetadata,
                                        onCopy = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            viewModel.copyCleanedUrlToClipboard()
                                        },
                                        onShare = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            shareUrl(context, uiState.cleanedUrl)
                                        },
                                        onOpenInBrowser = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            openInBrowser(context, uiState.cleanedUrl)
                                        },
                                        onInspectAgain = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            viewModel.goToStep(WizardStep.INSPECT)
                                        },
                                        onScrubAnother = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            viewModel.clearInput()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        supportingPane = {
            AnimatedPane {
                HistoryPanel(
                    historyItems = historyItems,
                    searchQuery = uiState.historySearchQuery,
                    onSearchQueryChange = viewModel::onHistorySearchQueryChanged,
                    onSelectItem = viewModel::selectHistoryItem,
                    onCopyItem = viewModel::copyHistoryItem,
                    onDeleteItem = viewModel::deleteHistoryItem,
                    onClearAll = viewModel::clearAllHistory,
                    onClose = null,
                    modifier = Modifier.statusBarsPadding()
                )
            }
        }
    )

    // Help & FAQ bottom sheet
    if (uiState.isHelpSheetVisible) {
        HelpFaqBottomSheet(
            onDismissRequest = { viewModel.setHelpSheetVisible(false) }
        )
    }

    // Mobile / single-pane bottom sheet when history icon is tapped
    if (!isDualPane && uiState.isHistoryPaneVisible) {
        val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { viewModel.setHistoryPaneVisible(false) },
            sheetState = bottomSheetState
        ) {
            HistoryPanel(
                historyItems = historyItems,
                searchQuery = uiState.historySearchQuery,
                onSearchQueryChange = viewModel::onHistorySearchQueryChanged,
                onSelectItem = { item ->
                    viewModel.selectHistoryItem(item)
                    viewModel.setHistoryPaneVisible(false)
                },
                onCopyItem = viewModel::copyHistoryItem,
                onDeleteItem = viewModel::deleteHistoryItem,
                onClearAll = viewModel::clearAllHistory,
                onClose = { viewModel.setHistoryPaneVisible(false) }
            )
        }
    }
}

/**
 * Step 1: Input URL and clipboard paste detection with clear action buttons (Quick Clean vs Tune / Inspect).
 */
@Composable
fun Step1InputView(
    inputUrl: String,
    detectedClipboardUrl: String?,
    isProcessing: Boolean,
    onUrlChange: (String) -> Unit,
    onPasteClipboard: () -> Unit,
    onClearUrl: () -> Unit,
    onQuickClean: () -> Unit,
    onTuneInspect: () -> Unit,
    onScanQrCode: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Clipboard detection alert pill
        AnimatedVisibility(
            visible = !detectedClipboardUrl.isNullOrBlank() && detectedClipboardUrl != inputUrl
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPasteClipboard() }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ContentPaste,
                        contentDescription = stringResource(R.string.cd_clipboard_found),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.input_link_detected_clipboard),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = detectedClipboardUrl.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    FilledTonalButton(
                        onClick = onPasteClipboard,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.action_paste),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }

        // URL input text field with Scan QR Code action
        OutlinedTextField(
            value = inputUrl,
            onValueChange = onUrlChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("url_input_field"),
            label = { Text(stringResource(R.string.input_url_label)) },
            placeholder = { Text(stringResource(R.string.input_url_placeholder)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Link,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onScanQrCode,
                        modifier = Modifier.testTag("scan_qr_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.QrCodeScanner,
                            contentDescription = stringResource(R.string.action_scan_qr),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (inputUrl.isNotBlank()) {
                        IconButton(onClick = onClearUrl) {
                            Icon(
                                imageVector = Icons.Rounded.Clear,
                                contentDescription = stringResource(R.string.action_clear_input),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            shape = RoundedCornerShape(18.dp),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Go
            ),
            keyboardActions = KeyboardActions(
                onGo = {
                    if (inputUrl.isNotBlank() && !isProcessing) {
                        onQuickClean()
                    }
                }
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest
            ),
            singleLine = false,
            maxLines = 4
        )

        // Action Buttons: Quick Clean (Prominent primary) & Tune / Inspect (Tonal / Detailed)
        val hasUrl = inputUrl.isNotBlank()
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Quick Clean: Instant scrub and copy to clipboard
            Button(
                onClick = onQuickClean,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = hasUrl && !isProcessing,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.5.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.action_scrubbing_link),
                        maxLines = 1,
                        softWrap = false
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Bolt,
                        contentDescription = stringResource(R.string.cd_quick_clean_icon),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.action_quick_clean_copy),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Tune / Inspect: Step-by-step query parameter selection
            FilledTonalButton(
                onClick = onTuneInspect,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = hasUrl && !isProcessing,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.Tune,
                    contentDescription = stringResource(R.string.cd_tune_inspect_icon),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.action_tune_inspect),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Step 2: Parameter inspection, unshortening info, and interactive chip removal
 */
@Composable
fun Step2InspectView(
    inputUrl: String,
    unshortenedUrl: String?,
    isRedirected: Boolean,
    redirectChain: List<String>,
    isUnshortening: Boolean,
    parameters: List<ParameterChipItem>,
    cleanedUrlCandidate: String,
    onToggleParam: (String) -> Unit,
    onScrubAllTracking: () -> Unit,
    onKeepAll: () -> Unit,
    onBack: () -> Unit,
    onProceedToPreview: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Redirect & Unshortening notification card
        if (isUnshortening) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.inspect_unshortening),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else if (isRedirected && !unshortenedUrl.isNullOrBlank()) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Route,
                            contentDescription = stringResource(R.string.cd_redirect_traced),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.inspect_redirect_resolved, redirectChain.size),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.inspect_destination, unshortenedUrl),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Parameter Chip Group
        ParameterChipGroup(
            parameters = parameters,
            onToggleParameter = onToggleParam,
            onSelectAllTracking = onScrubAllTracking,
            onResetAll = onKeepAll
        )

        // Real-time Preview of Candidate Clean URL
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = stringResource(R.string.inspect_candidate_preview_title),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = cleanedUrlCandidate.ifBlank { inputUrl },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Navigation controls (Back & Forward)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f).height(50.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.action_back)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.action_back),
                    maxLines = 1,
                    softWrap = false
                )
            }

            Button(
                onClick = onProceedToPreview,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(2f).height(50.dp)
            ) {
                Text(
                    text = stringResource(R.string.action_confirm_preview),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = stringResource(R.string.cd_preview)
                )
            }
        }
    }
}

/**
 * Step 3: Open Graph preview card with Coil, and Share, Copy, Open in Browser actions
 */
@Composable
fun Step3PreviewView(
    originalUrl: String,
    cleanedUrl: String,
    unshortenedUrl: String?,
    removedParamsCount: Int,
    metadata: OpenGraphMetadata?,
    isLoadingMetadata: Boolean,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onOpenInBrowser: () -> Unit,
    onInspectAgain: () -> Unit,
    onScrubAnother: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Success Badge Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = stringResource(R.string.cd_success),
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = stringResource(R.string.preview_success_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = stringResource(R.string.preview_params_eliminated, removedParamsCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // Open Graph Thumbnail Preview Card (using Coil)
        OpenGraphPreviewCard(
            metadata = metadata,
            isLoading = isLoadingMetadata,
            url = cleanedUrl
        )

        // Cleaned URL display card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.preview_clean_url_title),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = cleanedUrl,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Primary Action Buttons: Copy, Share, Open in Browser
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Copy Clean URL Button
                Button(
                    onClick = onCopy,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ContentCopy,
                        contentDescription = stringResource(R.string.cd_copy_clean_url)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.action_copy),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                // Share Clean URL Button
                FilledTonalButton(
                    onClick = onShare,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Share,
                        contentDescription = stringResource(R.string.cd_share_clean_url)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.action_share),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            // Open in Browser Button
            OutlinedButton(
                onClick = onOpenInBrowser,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                    contentDescription = stringResource(R.string.action_open_in_browser),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.action_open_in_browser),
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        // Auxiliary actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onInspectAgain,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).height(48.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Tune,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.action_adjust_parameters),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }

            FilledTonalButton(
                onClick = onScrubAnother,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).height(48.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.CleaningServices,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.action_scrub_another),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Placement 1: AdMob Adaptive Banner at the bottom of Step 3
        Spacer(modifier = Modifier.height(8.dp))
        com.androidfung.linkscrub.ui.components.AdMobBanner(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * System Share Intent helper
 */
fun shareUrl(context: Context, url: String) {
    if (url.isBlank()) return
    val chooserTitle = context.getString(R.string.share_chooser_title)
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, url)
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, chooserTitle)
    context.startActivity(shareIntent)
}

/**
 * System Open in Browser Intent helper
 */
fun openInBrowser(context: Context, url: String) {
    if (url.isBlank()) return
    try {
        val safeUrl = if (!url.startsWith("http://", ignoreCase = true) && !url.startsWith("https://", ignoreCase = true)) {
            "https://$url"
        } else {
            url
        }
        val intent = Intent(Intent.ACTION_VIEW, safeUrl.toUri())
        context.startActivity(intent)
    } catch (_: Exception) {
    }
}

@Preview(showBackground = true)
@Composable
fun Step1InputPreview() {
    LinkScrubTheme {
        Step1InputView(
            inputUrl = "https://example.com?utm_source=twitter&gclid=123",
            detectedClipboardUrl = "https://news.ycombinator.com",
            isProcessing = false,
            onUrlChange = {},
            onPasteClipboard = {},
            onClearUrl = {},
            onQuickClean = {},
            onTuneInspect = {},
            onScanQrCode = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun Step3Preview() {
    LinkScrubTheme {
        Step3PreviewView(
            originalUrl = "https://example.com?utm_source=twitter&fbclid=abc",
            cleanedUrl = "https://example.com",
            unshortenedUrl = null,
            removedParamsCount = 2,
            metadata = OpenGraphMetadata(
                title = "Example Domain",
                description = "Example Domain illustration for LinkScrub",
                imageUrl = null,
                siteName = "example.com"
            ),
            isLoadingMetadata = false,
            onCopy = {},
            onShare = {},
            onOpenInBrowser = {},
            onInspectAgain = {},
            onScrubAnother = {}
        )
    }
}
