package com.androidfung.linkscrub.domain

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

class UrlCleaner(
    private val customBlocklist: Set<String> = emptySet(),
    private val customAllowlist: Set<String> = emptySet(),
) {

    private val normalizedBlocklist: Set<String> = customBlocklist.map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet()
    private val normalizedAllowlist: Set<String> = customAllowlist.map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet()

    fun withCustomRules(
        blocklist: Set<String> = customBlocklist,
        allowlist: Set<String> = customAllowlist
    ): UrlCleaner = UrlCleaner(customBlocklist = blocklist, customAllowlist = allowlist)

    companion object {
        val EXACT_TRACKING_PARAMS: Set<String> = TrackingRulesCatalog.EXACT_TRACKING_PARAMS
        val TWITTER_SCOPED_PARAMS: Set<String> = TrackingRulesCatalog.TWITTER_SCOPED_PARAMS
        val AMAZON_SCOPED_PARAMS: Set<String> = TrackingRulesCatalog.AMAZON_SCOPED_PARAMS
        val YOUTUBE_SCOPED_PARAMS: Set<String> = TrackingRulesCatalog.YOUTUBE_SCOPED_PARAMS
        val SPOTIFY_SCOPED_PARAMS: Set<String> = TrackingRulesCatalog.SPOTIFY_SCOPED_PARAMS
        val SUBSTACK_SCOPED_PARAMS: Set<String> = TrackingRulesCatalog.SUBSTACK_SCOPED_PARAMS
        val TRACKING_PARAM_PREFIXES: List<String> = TrackingRulesCatalog.TRACKING_PARAM_PREFIXES

        val URL_REGEX = Regex("""https?://[^\s<>"'{}|\\^`]+""", RegexOption.IGNORE_CASE)
        val AMAZON_REF_PATH_REGEX = Regex("""/ref=[^/?#]+""")
        const val AMAZON_PATH_PARAM_KEY = "ref (path)"

        /**
         * Trims trailing punctuation like '.', ',', ';', ')', ']' from extracted URLs in conversational text.
         */
        fun trimTrailingPunctuation(url: String): String {
            var result = url
            while (result.isNotEmpty()) {
                val lastChar = result.last()
                if (lastChar in ".,;:!?\"'>`") {
                    result = result.dropLast(1)
                } else if (lastChar == ')' && result.count { it == '(' } < result.count { it == ')' }) {
                    result = result.dropLast(1)
                } else if (lastChar == ']' && result.count { it == '[' } < result.count { it == ']' }) {
                    result = result.dropLast(1)
                } else {
                    break
                }
            }
            return result
        }

        fun findFirstUrl(text: String): String? {
            val match = URL_REGEX.find(text) ?: return null
            return trimTrailingPunctuation(match.value)
        }

        fun findAllUrls(text: String): List<String> {
            return URL_REGEX.findAll(text)
                .map { trimTrailingPunctuation(it.value) }
                .filter { it.isNotBlank() }
                .toList()
        }
    }

    fun cleanUrl(rawUrl: String): CleaningResult {
        val trimmed = trimTrailingPunctuation(rawUrl.trim())
        val parsed = trimmed.toHttpUrlOrNull() ?: return CleaningResult(
            originalUrl = trimmed,
            cleanedUrl = trimmed,
            removedParams = emptyList()
        )

        val removedParams = mutableListOf<String>()
        val builder = parsed.newBuilder()

        // Clear existing query params and rebuild preserving non-tracking ones
        val remainingNames = parsed.queryParameterNames.toList()
        for (name in remainingNames) {
            if (isTrackingParam(name, parsed.host)) {
                removedParams.add(name)
                builder.removeAllQueryParameters(name)
            }
        }

        // Handle path-based tracking for Amazon URLs e.g., /ref=...
        var cleanedUrlString = builder.build().toString()
        if (TrackingRulesCatalog.isAmazonHost(parsed.host.lowercase()) &&
            AMAZON_REF_PATH_REGEX.containsMatchIn(cleanedUrlString) &&
            AMAZON_PATH_PARAM_KEY.lowercase() !in normalizedAllowlist
        ) {
            cleanedUrlString = cleanAmazonPath(cleanedUrlString)
            removedParams.add(AMAZON_PATH_PARAM_KEY)
        }

        return CleaningResult(
            originalUrl = trimmed,
            cleanedUrl = cleanedUrlString,
            removedParams = removedParams
        )
    }

    fun extractAndCleanUrlsInText(text: String): Pair<String, List<CleaningResult>> {
        val results = mutableListOf<CleaningResult>()
        val replacedText = URL_REGEX.replace(text) { matchResult ->
            val rawMatch = matchResult.value
            val foundUrl = trimTrailingPunctuation(rawMatch)
            val trailingSuffix = rawMatch.substring(foundUrl.length)
            val cleaningResult = cleanUrl(foundUrl)
            results.add(cleaningResult)
            cleaningResult.cleanedUrl + trailingSuffix
        }
        return Pair(replacedText, results)
    }

    fun classifyParameter(paramName: String, host: String? = null): ParameterCategory {
        return TrackingRulesCatalog.classifyParameter(
            paramName = paramName,
            host = host,
            customBlocklist = normalizedBlocklist,
            customAllowlist = normalizedAllowlist
        )
    }

    fun isTrackingParam(paramName: String, host: String? = null): Boolean {
        val category = classifyParameter(paramName, host)
        return category != ParameterCategory.ESSENTIAL_SAFE && category != ParameterCategory.CUSTOM_ALLOWED
    }

    fun computeCleanedUrlWithExcludedParams(rawUrl: String, paramsToRemove: Set<String>): String {
        val parsed = rawUrl.toHttpUrlOrNull() ?: return rawUrl
        val builder = parsed.newBuilder()

        for (param in paramsToRemove) {
            if (param != AMAZON_PATH_PARAM_KEY) {
                builder.removeAllQueryParameters(param)
            }
        }

        var result = builder.build().toString()
        if (parsed.host.contains("amazon.", ignoreCase = true) && paramsToRemove.contains(AMAZON_PATH_PARAM_KEY)) {
            result = cleanAmazonPath(result)
        }
        return result
    }

    fun cleanAmazonPath(url: String): String {
        return url.replace(AMAZON_REF_PATH_REGEX, "")
    }

    data class CleaningResult(
        val originalUrl: String,
        val cleanedUrl: String,
        val removedParams: List<String>
    )
}
