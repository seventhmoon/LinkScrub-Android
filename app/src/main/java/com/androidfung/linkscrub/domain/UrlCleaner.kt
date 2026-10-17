package com.androidfung.linkscrub.domain

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

class UrlCleaner(
    private val customBlocklist: Set<String> = emptySet(),
) {

    companion object {
        val EXACT_TRACKING_PARAMS = setOf(
            // Google / Analytics / Ads / Merchant
            "gclid", "gclsrc", "dclid", "wbraid", "gbraid", "_ga", "_gl", "gad_source", "srsltid",
            // Facebook / Instagram / Meta
            "fbclid", "igsh", "igshid", "fb_action_ids", "fb_action_types", "fb_source", "fref",
            // Twitter / X
            "twclid", "t", "s",
            // YouTube / Spotify / Media shares
            "si", "feature",
            // TikTok / ByteDance
            "_t", "_r", "tt_from", "tt_medium", "tt_content",
            // Asian Social/Commerce (Bilibili, Xiaohongshu, Taobao)
            "spm_id_from", "from_source", "vd_source", "scm",
            // Microsoft / Bing
            "msclkid",
            // Mailchimp / Email / Newsletters
            "mc_cid", "mc_eid", "mailcamp",
            // Amazon tracking / ref & Retail
            "tag", "ref_", "pf_rd_r", "pf_rd_p", "pf_rd_m", "pf_rd_s", "pf_rd_t", "pf_rd_i",
            "pd_rd_r", "pd_rd_w", "pd_rd_wg", "crid", "sprefix", "keywords",
            // General click & affiliate identifiers
            "ncid", "yclid", "zanpid", "irclickid", "aff_id", "aff_sub", "aff_platform",
            // Hubspot
            "_hsenc", "_hsmi", "hsCtsTracking",
            // Matomo / Piwik
            "pk_campaign", "pk_kwd", "pk_source", "pk_medium", "pk_content"
        )

        val TRACKING_PARAM_PREFIXES = listOf(
            "utm_",
            "itm_",
            "at_custom",
            "at_campaign",
            "at_medium",
            "vero_"
        )

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
        if (parsed.host.contains("amazon.", ignoreCase = true) && AMAZON_REF_PATH_REGEX.containsMatchIn(cleanedUrlString)) {
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
            val foundUrl = trimTrailingPunctuation(matchResult.value)
            val cleaningResult = cleanUrl(foundUrl)
            results.add(cleaningResult)
            cleaningResult.cleanedUrl
        }
        return Pair(replacedText, results)
    }

    fun isTrackingParam(paramName: String, host: String? = null): Boolean {
        val lower = paramName.lowercase()

        // Domain-Aware Preservation Rules
        if (host != null) {
            val lowerHost = host.lowercase()
            val isYouTube = lowerHost == "youtube.com" || lowerHost.endsWith(".youtube.com") ||
                    lowerHost == "youtu.be" || lowerHost.endsWith(".youtu.be")
            if (isYouTube && lower == "t") {
                // Preserve 't' (timestamp parameter) on YouTube
                return false
            }
        }

        if (lower in EXACT_TRACKING_PARAMS || lower in customBlocklist || lower == AMAZON_PATH_PARAM_KEY) {
            return true
        }
        for (prefix in TRACKING_PARAM_PREFIXES) {
            if (lower.startsWith(prefix)) {
                return true
            }
        }
        return false
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
