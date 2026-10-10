package com.androidfung.linkscrub.data.network

import com.androidfung.linkscrub.data.model.OpenGraphMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.jsoup.Jsoup
import java.util.concurrent.TimeUnit

interface OpenGraphScraper {
    suspend fun scrape(url: String): OpenGraphMetadata?
}

class JsoupOpenGraphScraper(
    private val client: OkHttpClient = defaultClient()
) : OpenGraphScraper {

    companion object {
        private const val MAX_HTML_BYTES = 1024L * 1024L // 1 MB to comfortably catch all head tags

        // Standard modern desktop browser User-Agent so major sites (YouTube, Twitter/X, GitHub, Reddit, News, etc.)
        // serve the full HTML with Open Graph tags rather than mobile redirects or stripped pages.
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"

        fun defaultClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .followRedirects(true)
                .followSslRedirects(true)
                .connectTimeout(12, TimeUnit.SECONDS)
                .readTimeout(12, TimeUnit.SECONDS)
                .build()
        }

        /**
         * Extracts a YouTube video ID from common YouTube URL formats:
         * - youtu.be/{id}
         * - youtube.com/watch?v={id}
         * - youtube.com/shorts/{id}
         * - youtube.com/embed/{id}
         * - youtube.com/live/{id}
         * - youtube.com/v/{id}
         */
        fun extractYouTubeVideoId(url: String): String? {
            return try {
                val httpUrl = url.trim().toHttpUrlOrNull() ?: return null
                val host = httpUrl.host.lowercase()
                val isYouTubeHost = host == "youtu.be" || host.endsWith(".youtu.be") ||
                        host == "youtube.com" || host.endsWith(".youtube.com")
                if (!isYouTubeHost) return null

                if (host == "youtu.be" || host.endsWith(".youtu.be")) {
                    val segments = httpUrl.pathSegments.filter { it.isNotBlank() }
                    return segments.firstOrNull()?.takeIf { isValidYouTubeId(it) }
                }

                // youtube.com variants
                val path = httpUrl.encodedPath
                if (path.startsWith("/watch")) {
                    val v = httpUrl.queryParameter("v")
                    return v?.takeIf { isValidYouTubeId(it) }
                }

                val segments = httpUrl.pathSegments.filter { it.isNotBlank() }
                if (segments.size >= 2) {
                    val first = segments[0].lowercase()
                    if (first in listOf("shorts", "embed", "live", "v")) {
                        return segments[1].takeIf { isValidYouTubeId(it) }
                    }
                }

                null
            } catch (_: Exception) {
                null
            }
        }

        private fun isValidYouTubeId(id: String): Boolean {
            // YouTube IDs are typically 11 alphanumeric and -_ characters
            return id.isNotBlank() && id.length in 8..16 && id.all { it.isLetterOrDigit() || it == '-' || it == '_' }
        }
    }

    override suspend fun scrape(url: String): OpenGraphMetadata? = withContext(Dispatchers.IO) {
        val youTubeVideoId = extractYouTubeVideoId(url)

        // For YouTube URLs, query YouTube's official oEmbed API first.
        // It provides author, title, and thumbnail without EU consent barriers or cookie challenges.
        if (youTubeVideoId != null) {
            val youTubeMetadata = fetchYouTubeMetadata(youTubeVideoId)
            if (youTubeMetadata != null && !youTubeMetadata.imageUrl.isNullOrBlank()) {
                return@withContext youTubeMetadata
            }
        }

        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.9")
                .header("Sec-Fetch-Mode", "navigate")
                .header("Sec-Fetch-Site", "none")
                .header("Sec-Fetch-Dest", "document")
                .build()

            val html: String
            val baseUri: String
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val contentType = response.header("Content-Type") ?: ""
                if (!contentType.contains("text/html", ignoreCase = true) &&
                    !contentType.contains("application/xhtml", ignoreCase = true)
                ) {
                    return@withContext null
                }
                baseUri = response.request.url.toString()
                val responseBody = response.body
                val source = responseBody.source()
                val charset = responseBody.contentType()?.charset(Charsets.UTF_8) ?: Charsets.UTF_8
                val buffer = okio.Buffer()
                source.read(buffer, MAX_HTML_BYTES)
                html = buffer.readString(charset)
            }

            val parsedMetadata = parseHtmlMetadata(html, baseUri)

            // If parsed metadata has missing image/title but it's a YouTube URL, complement it with YouTube video ID
            if (youTubeVideoId != null) {
                val deterministicThumbnail = getBestYouTubeThumbnail(youTubeVideoId)
                val finalImage = parsedMetadata.imageUrl ?: deterministicThumbnail
                val finalTitle = parsedMetadata.title ?: "YouTube Video"
                val finalSiteName = parsedMetadata.siteName ?: "YouTube"

                val mergedTags = linkedMapOf<String, String>().apply {
                    putAll(parsedMetadata.allTags)
                    if (!containsKey("og:image")) {
                        put("og:image", finalImage)
                    }
                    if (!containsKey("og:site_name")) {
                        put("og:site_name", finalSiteName)
                    }
                }

                return@withContext parsedMetadata.copy(
                    title = finalTitle,
                    imageUrl = finalImage,
                    siteName = finalSiteName,
                    allTags = mergedTags
                )
            }

            parsedMetadata
        } catch (_: Exception) {
            // As fallback for YouTube if network HTML scraping failed
            if (youTubeVideoId != null) {
                val thumbnail = getBestYouTubeThumbnail(youTubeVideoId)
                return@withContext OpenGraphMetadata(
                    title = "YouTube Video",
                    description = null,
                    imageUrl = thumbnail,
                    siteName = "YouTube",
                    allTags = linkedMapOf(
                        "og:site_name" to "YouTube",
                        "og:image" to thumbnail
                    )
                )
            }
            null
        }
    }

    /**
     * Queries YouTube's official oEmbed endpoint and verifies the best thumbnail resolution.
     */
    private fun fetchYouTubeMetadata(videoId: String): OpenGraphMetadata? {
        return try {
            val oEmbedRequest = Request.Builder()
                .url("https://www.youtube.com/oembed?url=https://www.youtube.com/watch?v=$videoId&format=json")
                .header("User-Agent", USER_AGENT)
                .build()

            client.newCall(oEmbedRequest).execute().use { response ->
                if (!response.isSuccessful) return null
                val bodyString = response.body.string()
                val json = JSONObject(bodyString)

                val title = json.optString("title").takeIf { it.isNotBlank() }
                val author = json.optString("author_name").takeIf { it.isNotBlank() }
                val provider = json.optString("provider_name").takeIf { it.isNotBlank() } ?: "YouTube"

                // Prefer maxresdefault or fall back to oembed thumbnail / hqdefault
                val bestThumbnail = getBestYouTubeThumbnail(videoId)
                    .ifBlank { json.optString("thumbnail_url") }
                    .takeIf { it.isNotBlank() }

                val tags = linkedMapOf<String, String>()
                tags["og:site_name"] = provider
                if (title != null) tags["og:title"] = title
                if (bestThumbnail != null) tags["og:image"] = bestThumbnail
                if (author != null) tags["author"] = author

                OpenGraphMetadata(
                    title = title,
                    description = author?.let { "Video by $it" },
                    imageUrl = bestThumbnail,
                    siteName = provider,
                    allTags = tags
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Checks if maxresdefault is available via HEAD request; falls back to hqdefault.jpg
     */
    private fun getBestYouTubeThumbnail(videoId: String): String {
        val maxResUrl = "https://i.ytimg.com/vi/$videoId/maxresdefault.jpg"
        val hqUrl = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"

        return try {
            val headRequest = Request.Builder()
                .url(maxResUrl)
                .head()
                .build()
            client.newCall(headRequest).execute().use { response ->
                if (response.isSuccessful) maxResUrl else hqUrl
            }
        } catch (_: Exception) {
            hqUrl
        }
    }

    fun parseHtmlMetadata(html: String, baseUri: String): OpenGraphMetadata {
        val doc = Jsoup.parse(html, baseUri)
        val allTags = linkedMapOf<String, String>()

        // Collect all <meta> tags having property or name starting with og:, twitter:, or standard meta
        val metaElements = doc.select("meta[property], meta[name]")
        for (meta in metaElements) {
            val key = when {
                meta.hasAttr("property") -> meta.attr("property").trim()
                meta.hasAttr("name") -> meta.attr("name").trim()
                else -> ""
            }
            val content = meta.attr("content").trim()
            if (key.isNotBlank() && content.isNotBlank()) {
                val lowerKey = key.lowercase()
                if (lowerKey.startsWith("og:") || lowerKey.startsWith("twitter:") ||
                    lowerKey == "description" || lowerKey == "author" || lowerKey == "keywords"
                ) {
                    // Resolve relative URLs for image / url metadata if applicable
                    val resolvedContent = if (
                        lowerKey.contains("image") || lowerKey.contains("url")
                    ) {
                        resolveImageUrl(baseUri, content)
                    } else {
                        content
                    }
                    // Keep first or update with resolved value
                    allTags[key] = resolvedContent
                }
            }
        }

        // Title
        val ogTitle = allTags["og:title"]
            ?: allTags["twitter:title"]
            ?: doc.title().takeIf { it.isNotBlank() }

        // Description
        val ogDescription = allTags["og:description"]
            ?: allTags["twitter:description"]
            ?: allTags["description"]

        // Image candidates from meta tags
        var ogImage = allTags["og:image"]
            ?: allTags["og:image:secure_url"]
            ?: allTags["twitter:image"]
            ?: allTags["twitter:image:src"]

        // Fallbacks for image: <link rel="image_src">, <link itemprop="thumbnailUrl">, <link rel="thumbnail">
        if (ogImage.isNullOrBlank()) {
            val linkImageSrc = doc.selectFirst("link[rel~=(?i)image_src]")?.attr("href")?.trim()
                ?: doc.selectFirst("link[itemprop~=(?i)thumbnailurl]")?.attr("href")?.trim()
                ?: doc.selectFirst("link[rel~=(?i)thumbnail]")?.attr("href")?.trim()

            if (!linkImageSrc.isNullOrBlank()) {
                ogImage = linkImageSrc
                allTags["link:image_src"] = linkImageSrc
            }
        }

        if (ogImage != null) {
            ogImage = resolveImageUrl(baseUri, ogImage)
        }

        // Site Name
        val ogSiteName = allTags["og:site_name"]
            ?: doc.selectFirst("meta[name=application-name]")?.attr("content")?.takeIf { it.isNotBlank() }

        return OpenGraphMetadata(
            title = ogTitle?.trim(),
            description = ogDescription?.trim(),
            imageUrl = ogImage?.trim(),
            siteName = ogSiteName?.trim(),
            allTags = allTags
        )
    }

    private fun resolveImageUrl(base: String, image: String): String {
        return try {
            val baseHttp = base.toHttpUrlOrNull() ?: return image
            baseHttp.resolve(image)?.toString() ?: image
        } catch (_: Exception) {
            image
        }
    }
}
