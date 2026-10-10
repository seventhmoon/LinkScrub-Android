package com.androidfung.linkscrub

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.androidfung.linkscrub.ui.LinkScrubScreen
import com.androidfung.linkscrub.ui.LinkScrubViewModel
import com.androidfung.linkscrub.ui.theme.LinkScrubTheme

class MainActivity : ComponentActivity() {

    private val viewModel: LinkScrubViewModel by viewModels {
        LinkScrubViewModel.provideFactory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Mobile Ads asynchronously on IO dispatcher to avoid blocking main thread startup
        lifecycleScope.launch(Dispatchers.IO) {
            MobileAds.initialize(this@MainActivity) {}
        }

        // Handle shared link on fresh launch
        handleIncomingIntent(intent)

        setContent {
            LinkScrubTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LinkScrubScreen(viewModel = viewModel)
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

