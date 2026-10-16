package com.androidfung.linkscrub.util

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.androidfung.linkscrub.R

/**
 * Reusable Intent launch helpers for sharing links and launching browsers.
 */
object IntentUtils {

    /**
     * System Share Intent helper to dispatch a cleaned URL to other apps.
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
     * System Open in Browser Intent helper with safe HTTP/HTTPS scheme fallback.
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
            // Gracefully ignore ActivityNotFoundException if no browser is installed
        }
    }
}
