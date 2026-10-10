package com.androidfung.linkscrub.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

interface RedirectUnshortener {
    suspend fun unshorten(url: String, maxHops: Int = 10): UnshortenResult
}

data class UnshortenResult(
    val initialUrl: String,
    val finalUrl: String,
    val redirectChain: List<String> = emptyList(),
    val isRedirected: Boolean = false,
    val error: String? = null
)

class OkHttpRedirectUnshortener(
    private val client: OkHttpClient = defaultClient()
) : RedirectUnshortener {

    companion object {
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"

        fun defaultClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .followRedirects(false)
                .followSslRedirects(false)
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build()
        }
    }

    override suspend fun unshorten(url: String, maxHops: Int): UnshortenResult = withContext(Dispatchers.IO) {
        var currentUrl = url
        val chain = mutableListOf<String>()
        val visitedUrls = mutableSetOf<String>(currentUrl)
        var hops = 0

        try {
            while (hops < maxHops) {
                // Try HEAD request first to avoid downloading body
                var targetLocation: String? = null
                val headRequest = Request.Builder()
                    .url(currentUrl)
                    .head()
                    .header("User-Agent", USER_AGENT)
                    .build()

                try {
                    client.newCall(headRequest).execute().use { response ->
                        if (response.isRedirect) {
                            targetLocation = response.header("Location")
                        } else if (response.code in 400..405) {
                            // Method not allowed or other client issue on HEAD, fallback to GET
                            targetLocation = null
                        } else {
                            // Non-redirect response, we've reached the destination
                            return@withContext UnshortenResult(
                                initialUrl = url,
                                finalUrl = currentUrl,
                                redirectChain = chain,
                                isRedirected = chain.isNotEmpty()
                            )
                        }
                    }
                } catch (_: Exception) {
                    targetLocation = null
                }

                // If HEAD didn't yield a redirect, try GET (some servers don't handle HEAD properly)
                if (targetLocation == null) {
                    val getRequest = Request.Builder()
                        .url(currentUrl)
                        .get()
                        .header("User-Agent", USER_AGENT)
                        .build()

                    client.newCall(getRequest).execute().use { response ->
                        if (response.isRedirect) {
                            targetLocation = response.header("Location")
                        } else {
                            return@withContext UnshortenResult(
                                initialUrl = url,
                                finalUrl = currentUrl,
                                redirectChain = chain,
                                isRedirected = chain.isNotEmpty()
                            )
                        }
                    }
                }

                if (targetLocation != null) {
                    // Resolve relative redirect locations if necessary
                    val resolvedUrl = resolveUrl(currentUrl, targetLocation!!)
                    if (resolvedUrl in visitedUrls) {
                        // Cyclic redirect loop detected: break immediately to avoid infinite redirect cycle
                        break
                    }
                    chain.add(resolvedUrl)
                    visitedUrls.add(resolvedUrl)
                    currentUrl = resolvedUrl
                    hops++
                } else {
                    break
                }
            }

            UnshortenResult(
                initialUrl = url,
                finalUrl = currentUrl,
                redirectChain = chain,
                isRedirected = chain.isNotEmpty()
            )
        } catch (e: Exception) {
            UnshortenResult(
                initialUrl = url,
                finalUrl = currentUrl,
                redirectChain = chain,
                isRedirected = chain.isNotEmpty(),
                error = e.localizedMessage ?: "Unknown network error"
            )
        }
    }

    private fun resolveUrl(base: String, location: String): String {
        return try {
            val baseHttp = base.toHttpUrlOrNull() ?: return location
            baseHttp.resolve(location)?.toString() ?: location
        } catch (_: Exception) {
            location
        }
    }
}
