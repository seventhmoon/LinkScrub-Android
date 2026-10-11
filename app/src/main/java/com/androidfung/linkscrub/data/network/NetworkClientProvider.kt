package com.androidfung.linkscrub.data.network

import okhttp3.ConnectionPool
import okhttp3.Dispatcher
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
}
