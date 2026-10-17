package com.androidfung.linkscrub.ui

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
import com.androidfung.linkscrub.ui.components.WizardStepIndicator
import com.androidfung.linkscrub.ui.steps.Step1Actions
import com.androidfung.linkscrub.ui.steps.Step1InputView
import com.androidfung.linkscrub.ui.steps.Step2Actions
import com.androidfung.linkscrub.ui.steps.Step2InspectView
import com.androidfung.linkscrub.ui.steps.Step3Actions
import com.androidfung.linkscrub.ui.steps.Step3PreviewView
import com.androidfung.linkscrub.ui.theme.LinkScrubTheme
import com.androidfung.linkscrub.util.AnalyticsHelper
import com.androidfung.linkscrub.util.IntentUtils
import com.androidfung.linkscrub.util.QrCodeScanner

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

    // Predictive back handling: navigate back within wizard or dismiss sheet instead of exiting app
    androidx.activity.compose.BackHandler(
        enabled = uiState.isHelpSheetVisible || (!isDualPane && uiState.isHistoryPaneVisible) || uiState.currentStep != WizardStep.INPUT
    ) {
        when {
            uiState.isHelpSheetVisible -> viewModel.setHelpSheetVisible(false)
            !isDualPane && uiState.isHistoryPaneVisible -> viewModel.setHistoryPaneVisible(false)
            uiState.currentStep == WizardStep.PREVIEW -> {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                viewModel.goToStep(WizardStep.INSPECT)
            }
            uiState.currentStep == WizardStep.INSPECT -> {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                viewModel.goToStep(WizardStep.INPUT)
            }
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
                                            painter = androidx.compose.ui.res.painterResource(R.drawable.ic_linkscrub_logo),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(22.dp)
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
                                    val step1Actions = remember(viewModel, context) {
                                        Step1Actions(
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
                                                QrCodeScanner.startScan(
                                                    context = context,
                                                    onSuccess = { scannedUrl ->
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        viewModel.onQrCodeScanned(scannedUrl)
                                                    },
                                                    onFailure = { error ->
                                                        AnalyticsHelper.recordException(error)
                                                    },
                                                    onCanceled = {
                                                        // User canceled or dismissed
                                                    }
                                                )
                                            }
                                        )
                                    }
                                    Step1InputView(
                                        inputUrl = uiState.inputUrl,
                                        detectedClipboardUrl = uiState.detectedClipboardUrl,
                                        isProcessing = uiState.isProcessing,
                                        actions = step1Actions
                                    )
                                }

                                WizardStep.INSPECT -> {
                                    val step2Actions = remember(viewModel) {
                                        Step2Actions(
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
                                    Step2InspectView(
                                        inputUrl = uiState.inputUrl,
                                        unshortenedUrl = uiState.unshortenedUrl,
                                        isRedirected = uiState.isRedirected,
                                        redirectChain = uiState.redirectChain,
                                        isUnshortening = uiState.isUnshortening,
                                        parameters = uiState.detectedParameters,
                                        cleanedUrlCandidate = uiState.cleanedUrl,
                                        actions = step2Actions
                                    )
                                }

                                WizardStep.PREVIEW -> {
                                    val step3Actions = remember(viewModel, context, uiState.cleanedUrl) {
                                        Step3Actions(
                                            onCopy = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                viewModel.copyCleanedUrlToClipboard()
                                            },
                                            onShare = {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                IntentUtils.shareUrl(context, uiState.cleanedUrl)
                                            },
                                            onOpenInBrowser = {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                IntentUtils.openInBrowser(context, uiState.cleanedUrl)
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
                                    Step3PreviewView(
                                        cleanedUrl = uiState.cleanedUrl,
                                        removedParamsCount = uiState.removedParamsCount,
                                        metadata = uiState.metadata,
                                        isLoadingMetadata = uiState.isLoadingMetadata,
                                        actions = step3Actions
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

@Preview(showBackground = true)
@Composable
fun Step1InputPreview() {
    LinkScrubTheme {
        Step1InputView(
            inputUrl = "https://example.com?utm_source=twitter&gclid=123",
            detectedClipboardUrl = "https://news.ycombinator.com",
            isProcessing = false,
            actions = Step1Actions(
                onUrlChange = {},
                onPasteClipboard = {},
                onClearUrl = {},
                onQuickClean = {},
                onTuneInspect = {},
                onScanQrCode = {}
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun Step3Preview() {
    LinkScrubTheme {
        Step3PreviewView(
            cleanedUrl = "https://example.com",
            removedParamsCount = 2,
            metadata = OpenGraphMetadata(
                title = "Example Domain",
                description = "Example Domain illustration for LinkScrub",
                imageUrl = null,
                siteName = "example.com"
            ),
            isLoadingMetadata = false,
            actions = Step3Actions(
                onCopy = {},
                onShare = {},
                onOpenInBrowser = {},
                onInspectAgain = {},
                onScrubAnother = {}
            )
        )
    }
}

