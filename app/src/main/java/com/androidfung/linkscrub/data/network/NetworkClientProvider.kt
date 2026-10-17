package com.androidfung.linkscrub.data.network

import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Shared network configuration provider for LinkScrub.
 * Reuses a single shared OkHttpClient with shared connection pool and dispatcher
 * across RedirectUnshortener and OpenGraphScraper to optimize socket reuse and memory footprint.
 */
object NetworkClientProvider {

    private val sharedDispatcher by lazy {
        Dispatcher().apply {
            maxRequests = 32
            maxRequestsPerHost = 8
        }
    }

    private val sharedConnectionPool by lazy {
        ConnectionPool(5, 5, TimeUnit.MINUTES)
    }

    val sharedOkHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .dispatcher(sharedDispatcher)
            .connectionPool(sharedConnectionPool)
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Protects against SSRF by checking whether the URL host is safe and not pointing
     * to loopback, link-local, or private/intranet addresses.
     */
    fun isSafePublicUrl(url: String): Boolean {
        val httpUrl = url.toHttpUrlOrNull() ?: return false
        val scheme = httpUrl.scheme.lowercase()
        if (scheme != "http" && scheme != "https") return false
        return isSafePublicHost(httpUrl.host)
    }

    fun isSafePublicHost(host: String): Boolean {
        val lower = host.trim().lowercase()
        if (lower == "localhost" || lower.endsWith(".localhost") || lower.endsWith(".local") || lower.endsWith(".internal")) {
            return false
        }
        if (lower == "127.0.0.1" || lower == "0.0.0.0" || lower == "::1") {
            return false
        }
        return try {
            val address = java.net.InetAddress.getByName(lower)
            !address.isLoopbackAddress && !address.isSiteLocalAddress && !address.isLinkLocalAddress && !address.isAnyLocalAddress
        } catch (_: Exception) {
            true
        }
    }
}
