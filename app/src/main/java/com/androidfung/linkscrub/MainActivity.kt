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

        // Initialize Analytics & Crashlytics helper
        AnalyticsHelper.initialize(this)

        // Initialize Google Play In-App Updates and trigger check
        inAppUpdateManager = InAppUpdateManager(this)
        inAppUpdateManager.checkForUpdate()

        // Initialize Mobile Ads asynchronously on IO dispatcher to avoid blocking main thread startup
        lifecycleScope.launch(Dispatchers.IO) {
            MobileAds.initialize(this@MainActivity) {}
        }

        // Handle shared link on fresh launch
        handleIncomingIntent(intent)

        setContent {
            LinkScrubTheme {
                val isUpdateDownloaded by inAppUpdateManager.isUpdateDownloaded.collectAsState()
                val snackbarHostState = remember { SnackbarHostState() }

                LaunchedEffect(isUpdateDownloaded) {
                    if (isUpdateDownloaded) {
                        val result = snackbarHostState.showSnackbar(
                            message = "An update has just been downloaded. Restart to install",
                            actionLabel = "Restart",
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
                    if (!sharedText.isNullOrBlank()) {
                        viewModel.handleSharedUrl(sharedText)
                    }
                }
            }
            Intent.ACTION_VIEW == action -> {
                val dataUri = intent.dataString
                if (!dataUri.isNullOrBlank()) {
                    viewModel.handleSharedUrl(dataUri)
                }
            }
        }
    }
}


