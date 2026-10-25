package com.androidfung.linkscrub

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.androidfung.linkscrub.ui.LinkScrubScreen
import com.androidfung.linkscrub.ui.LinkScrubViewModel
import com.androidfung.linkscrub.ui.theme.LinkScrubTheme
import com.androidfung.linkscrub.util.AnalyticsHelper
import android.os.Build
import com.androidfung.linkscrub.util.InAppUpdateManager
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: LinkScrubViewModel by viewModels {
        LinkScrubViewModel.provideFactory(application)
    }

    private lateinit var inAppUpdateManager: InAppUpdateManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Disable navigation bar contrast enforcement for true edge-to-edge on supported Android versions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        // Initialize Analytics & Crashlytics helper
        AnalyticsHelper.initialize(this)

        // Initialize Google Play In-App Updates and trigger check
        inAppUpdateManager = InAppUpdateManager(this)
        inAppUpdateManager.checkForUpdate()

        // Initialize Mobile Ads asynchronously on IO dispatcher to avoid blocking main thread startup
        lifecycleScope.launch(Dispatchers.IO) {
            MobileAds.initialize(this@MainActivity) {}
        }

        // Handle shared link on fresh launch only (prevent re-triggering on configuration change/rotation)
        if (savedInstanceState == null) {
            handleIncomingIntent(intent)
        }

        setContent {
            LinkScrubTheme {
                val isUpdateDownloaded by inAppUpdateManager.isUpdateDownloaded.collectAsState()
                val snackbarHostState = remember { SnackbarHostState() }
                val updateMessage = androidx.compose.ui.res.stringResource(R.string.update_downloaded_message)
                val restartAction = androidx.compose.ui.res.stringResource(R.string.update_restart_action)

                LaunchedEffect(isUpdateDownloaded) {
                    if (isUpdateDownloaded) {
                        val result = snackbarHostState.showSnackbar(
                            message = updateMessage,
                            actionLabel = restartAction,
                            duration = SnackbarDuration.Indefinite
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            inAppUpdateManager.completeUpdate()
                        }
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LinkScrubScreen(
                        viewModel = viewModel,
                        snackbarHostState = snackbarHostState
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        // Automatically check clipboard when returning to the app
        viewModel.checkClipboardForUrl()
        // Check for downloaded updates or updates in progress
        if (::inAppUpdateManager.isInitialized) {
            inAppUpdateManager.onResumeCheck()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::inAppUpdateManager.isInitialized) {
            inAppUpdateManager.onDestroy()
        }
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return
        val action = intent.action
        val type = intent.type

        when {
            Intent.ACTION_SEND == action && type != null -> {
                if ("text/plain" == type || type.startsWith("text/")) {
                    val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                        ?: intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()
                    // Guard against excessively large payloads to avoid memory spikes / DoS
                    if (!sharedText.isNullOrBlank() && sharedText.length <= MAX_INTENT_TEXT_LENGTH) {
                        viewModel.handleSharedUrl(sharedText)
                    }
                }
            }
            Intent.ACTION_VIEW == action -> {
                val dataUri = intent.dataString
                if (!dataUri.isNullOrBlank() && dataUri.length <= MAX_INTENT_TEXT_LENGTH) {
                    viewModel.handleSharedUrl(dataUri)
                }
            }
        }
    }

    companion object {
        private const val MAX_INTENT_TEXT_LENGTH = 100_000 // 100 KB limit for intent payloads
    }
}


