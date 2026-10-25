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
        val EXACT_TRACKING_PARAMS = setOf(
            // Google / Analytics / Ads / Merchant
            "gclid", "gclsrc", "dclid", "wbraid", "gbraid", "_ga", "_gl", "gad_source", "srsltid",
            // Facebook / Instagram / Meta / Threads
            "fbclid", "igsh", "igshid", "fb_action_ids", "fb_action_types", "fb_source", "fref", "xmt",
            // Twitter / X unambiguous
            "twclid", "ref_src", "ref_url",
            // YouTube / Spotify / Media shares
            "si", "feature",
            // TikTok / ByteDance
            "_t", "_r", "tt_from", "tt_medium", "tt_content", "u_code", "preview_pb",
            // Reddit
            "rdt", "share_id", "correlation_id", "ref_campaign", "ref_source",
            // LinkedIn
            "trk", "trkinfo", "lipi", "licu", "rcm", "upsellorderorigin",
            // Asian Social/Commerce (Bilibili, Xiaohongshu, Taobao, AliExpress, Shopee)
            "spm_id_from", "from_source", "vd_source", "scm", "aff_fcid", "aff_fsk", "aff_trace_key", "terminal_id", "pdp_npi",
            // Microsoft / Bing
            "msclkid",
            // Mailchimp / Email / Newsletters / Marketo
            "mc_cid", "mc_eid", "mailcamp", "mkt_tok", "oly_enc_id", "oly_anon_id",
            // Amazon unambiguous tracking
            "ref_", "pf_rd_r", "pf_rd_p", "pf_rd_m", "pf_rd_s", "pf_rd_t", "pf_rd_i",
            "pd_rd_r", "pd_rd_w", "pd_rd_wg", "ascsubtag", "dib", "dib_tag",
            // General click & affiliate identifiers
            "ncid", "yclid", "zanpid", "irclickid", "aff_id", "aff_sub", "aff_platform", "rb_clickid", "s_kwcid", "wickedid",
            // Hubspot
            "_hsenc", "_hsmi", "hsctstracking", "hsa_acc", "hsa_cam", "hsa_grp", "hsa_ad", "hsa_src", "hsa_tgt", "hsa_kw", "hsa_mt", "hsa_net", "hsa_ver",
            // Matomo / Piwik
            "pk_campaign", "pk_kwd", "pk_source", "pk_medium", "pk_content"
        )

        val TWITTER_SCOPED_PARAMS = setOf("s", "t", "cxt")
        val AMAZON_SCOPED_PARAMS = setOf("tag", "crid", "sprefix", "keywords", "qid", "sr", "linkcode", "linkid", "camp", "creative", "creativeasin")
        val YOUTUBE_SCOPED_PARAMS = setOf("pp", "ab_channel", "kw")
        val SPOTIFY_SCOPED_PARAMS = setOf("nd", "pt", "context")
        val SUBSTACK_SCOPED_PARAMS = setOf("r", "triedredirect")

        val TRACKING_PARAM_PREFIXES = listOf(
            "utm_",
            "itm_",
            "at_custom",
            "at_campaign",
            "at_medium",
            "vero_",
            "__cft__",
            "__tn__"
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
        if (isAmazonHost(parsed.host) &&
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

    fun isTrackingParam(paramName: String, host: String? = null): Boolean {
        val lower = paramName.lowercase()

        // User-defined allowlist always takes precedence
        if (lower in normalizedAllowlist) {
            return false
        }

        // User-defined blocklist
        if (lower in normalizedBlocklist || lower == AMAZON_PATH_PARAM_KEY) {
            return true
        }

        if (host != null) {
            val lowerHost = host.lowercase()

            // YouTube: preserve 't' timestamp, strip YouTube-specific tracking params
            if (isYouTubeHost(lowerHost)) {
                if (lower == "t") return false
                if (lower in YOUTUBE_SCOPED_PARAMS) return true
            }

            // Twitter / X: strip 's', 't', 'cxt'
            if (isTwitterHost(lowerHost) && lower in TWITTER_SCOPED_PARAMS) {
                return true
            }

            // Threads: strip 's'
            if (isThreadsHost(lowerHost) && lower == "s") {
                return true
            }

            // Amazon: strip retail/affiliate params ('tag', 'keywords', 'crid', 'sprefix', etc.)
            if (isAmazonHost(lowerHost) && lower in AMAZON_SCOPED_PARAMS) {
                return true
            }

            // Spotify: strip 'nd', 'pt', 'context'
            if (isSpotifyHost(lowerHost) && lower in SPOTIFY_SCOPED_PARAMS) {
                return true
            }

            // Substack: strip 'r', 'triedRedirect'
            if (isSubstackHost(lowerHost) && lower in SUBSTACK_SCOPED_PARAMS) {
                return true
            }
        } else {
            // Legacy fallback when called without a host
            if (lower in TWITTER_SCOPED_PARAMS || lower in AMAZON_SCOPED_PARAMS) {
                return true
            }
        }

        if (lower in EXACT_TRACKING_PARAMS) {
            return true
        }
        for (prefix in TRACKING_PARAM_PREFIXES) {
            if (lower.startsWith(prefix)) {
                return true
            }
        }
        return false
    }

    private fun isYouTubeHost(lowerHost: String): Boolean =
        lowerHost == "youtube.com" || lowerHost.endsWith(".youtube.com") ||
            lowerHost == "youtu.be" || lowerHost.endsWith(".youtu.be")

    private fun isTwitterHost(lowerHost: String): Boolean =
        lowerHost == "twitter.com" || lowerHost.endsWith(".twitter.com") ||
            lowerHost == "x.com" || lowerHost.endsWith(".x.com") ||
            lowerHost == "fxtwitter.com" || lowerHost.endsWith(".fxtwitter.com") ||
            lowerHost == "vxtwitter.com" || lowerHost.endsWith(".vxtwitter.com") ||
            lowerHost == "fixupx.com" || lowerHost.endsWith(".fixupx.com")

    private fun isThreadsHost(lowerHost: String): Boolean =
        lowerHost == "threads.net" || lowerHost.endsWith(".threads.net") ||
            lowerHost == "threads.com" || lowerHost.endsWith(".threads.com")

    private fun isAmazonHost(lowerHost: String): Boolean =
        lowerHost.contains("amazon.") || lowerHost == "amzn.to" || lowerHost.endsWith(".amzn.com")

    private fun isSpotifyHost(lowerHost: String): Boolean =
        lowerHost == "spotify.com" || lowerHost.endsWith(".spotify.com")

    private fun isSubstackHost(lowerHost: String): Boolean =
        lowerHost == "substack.com" || lowerHost.endsWith(".substack.com")

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
