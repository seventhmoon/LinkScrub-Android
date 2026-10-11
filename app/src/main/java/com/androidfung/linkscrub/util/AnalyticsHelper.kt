package com.androidfung.linkscrub.util

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * Privacy-conscious analytics & crashlytics helper.
 * Strictly avoids logging URLs, domains, queries, or PII.
 */
object AnalyticsHelper {

    private var firebaseAnalytics: FirebaseAnalytics? = null
    private var crashlytics: FirebaseCrashlytics? = null

    fun initialize(context: Context) {
        try {
            firebaseAnalytics = FirebaseAnalytics.getInstance(context)
            crashlytics = FirebaseCrashlytics.getInstance()
        } catch (_: Exception) {
            // Firebase may fail to initialize in test environments or without Google Play Services
        }
    }

    /**
     * Logs privacy-safe URL scrubbing metrics.
     * Note: Never logs actual URLs or parameter keys/values.
     */
    fun logLinkScrubbed(paramCount: Int, unshortened: Boolean, isQuickClean: Boolean) {
        try {
            val bundle = Bundle().apply {
                putInt("param_count", paramCount)
                putBoolean("unshortened", unshortened)
                putBoolean("is_quick_clean", isQuickClean)
            }
            firebaseAnalytics?.logEvent("link_scrubbed", bundle)
        } catch (_: Exception) {
            // Safe guard against unmocked bundle in local unit test environments
        }
    }

    /**
     * Logs when user searches history.
     * Note: Never logs search query content.
     */
    fun logHistorySearched() {
        try {
            firebaseAnalytics?.logEvent("history_searched", null)
        } catch (_: Exception) {
            // Safe guard against local unit test environments
        }
    }

    /**
     * Logs AdMob banner impressions.
     */
    fun logAdImpression() {
        try {
            firebaseAnalytics?.logEvent("ad_impression", null)
        } catch (_: Exception) {
            // Safe guard against local unit test environments
        }
    }

    /**
     * Records a non-fatal exception to Crashlytics.
     */
    fun recordException(throwable: Throwable) {
        try {
            crashlytics?.recordException(throwable)
        } catch (_: Exception) {
            // Safe guard against local unit test environments
        }
    }

    /**
     * Records a privacy-safe breadcrumb log in Crashlytics.
     */
    fun recordBreadcrumb(message: String) {
        try {
            crashlytics?.log(message)
        } catch (_: Exception) {
            // Safe guard against local unit test environments
        }
    }
}
