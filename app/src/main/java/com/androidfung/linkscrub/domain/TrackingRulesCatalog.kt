package com.androidfung.linkscrub.domain

import androidx.annotation.StringRes
import com.androidfung.linkscrub.R

/**
 * Categorizes URL parameters so the UI can explain why a parameter is flagged or preserved.
 */
enum class ParameterCategory(@StringRes val labelRes: Int) {
    UTM_CAMPAIGN(R.string.category_utm_campaign),
    ADS_CLICK_ID(R.string.category_ads_click_id),
    SOCIAL_SHARE(R.string.category_social_share),
    ECOMMERCE_AFFILIATE(R.string.category_ecommerce_affiliate),
    EMAIL_MARKETING(R.string.category_email_marketing),
    ANALYTICS_TELEMETRY(R.string.category_analytics_telemetry),
    CUSTOM_BLOCKED(R.string.category_custom_blocked),
    CUSTOM_ALLOWED(R.string.category_custom_allowed),
    ESSENTIAL_SAFE(R.string.category_essential_safe)
}

/**
 * Centralized catalog of global and domain-scoped tracking parameters and classification logic.
 */
object TrackingRulesCatalog {

    val GOOGLE_ADS_PARAMS = setOf(
        "gclid", "gclsrc", "dclid", "wbraid", "gbraid", "_ga", "_gl", "gad_source", "srsltid"
    )

    val SOCIAL_SHARE_PARAMS = setOf(
        // Meta / Facebook / Instagram / Threads
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
        "trk", "trkinfo", "lipi", "licu", "rcm", "upsellorderorigin"
    )

    val ECOMMERCE_PARAMS = setOf(
        // Asian Social/Commerce (Bilibili, Xiaohongshu, Taobao, AliExpress, Shopee)
        "spm_id_from", "from_source", "vd_source", "scm",
        "aff_fcid", "aff_fsk", "aff_trace_key", "terminal_id", "pdp_npi",
        // Amazon unambiguous tracking
        "ref_", "pf_rd_r", "pf_rd_p", "pf_rd_m", "pf_rd_s", "pf_rd_t", "pf_rd_i",
        "pd_rd_r", "pd_rd_w", "pd_rd_wg", "ascsubtag", "dib", "dib_tag",
        // General affiliate identifiers
        "zanpid", "irclickid", "aff_id", "aff_sub", "aff_platform"
    )

    val ADS_AND_CLICK_PARAMS = setOf(
        "msclkid", "ncid", "yclid", "rb_clickid", "s_kwcid", "wickedid"
    )

    val EMAIL_AND_CRM_PARAMS = setOf(
        "mc_cid", "mc_eid", "mailcamp", "mkt_tok", "oly_enc_id", "oly_anon_id",
        "_hsenc", "_hsmi", "hsctstracking",
        "hsa_acc", "hsa_cam", "hsa_grp", "hsa_ad", "hsa_src", "hsa_tgt", "hsa_kw", "hsa_mt", "hsa_net", "hsa_ver"
    )

    val ANALYTICS_PARAMS = setOf(
        "pk_campaign", "pk_kwd", "pk_source", "pk_medium", "pk_content"
    )

    val EXACT_TRACKING_PARAMS: Set<String> =
        GOOGLE_ADS_PARAMS +
            SOCIAL_SHARE_PARAMS +
            ECOMMERCE_PARAMS +
            ADS_AND_CLICK_PARAMS +
            EMAIL_AND_CRM_PARAMS +
            ANALYTICS_PARAMS

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

    fun isYouTubeHost(lowerHost: String): Boolean =
        lowerHost == "youtube.com" || lowerHost.endsWith(".youtube.com") ||
            lowerHost == "youtu.be" || lowerHost.endsWith(".youtu.be")

    fun isTwitterHost(lowerHost: String): Boolean =
        lowerHost == "twitter.com" || lowerHost.endsWith(".twitter.com") ||
            lowerHost == "x.com" || lowerHost.endsWith(".x.com") ||
            lowerHost == "fxtwitter.com" || lowerHost.endsWith(".fxtwitter.com") ||
            lowerHost == "vxtwitter.com" || lowerHost.endsWith(".vxtwitter.com") ||
            lowerHost == "fixupx.com" || lowerHost.endsWith(".fixupx.com")

    fun isThreadsHost(lowerHost: String): Boolean =
        lowerHost == "threads.net" || lowerHost.endsWith(".threads.net") ||
            lowerHost == "threads.com" || lowerHost.endsWith(".threads.com")

    fun isAmazonHost(lowerHost: String): Boolean =
        lowerHost.contains("amazon.") || lowerHost == "amzn.to" || lowerHost.endsWith(".amzn.com")

    fun isSpotifyHost(lowerHost: String): Boolean =
        lowerHost == "spotify.com" || lowerHost.endsWith(".spotify.com")

    fun isSubstackHost(lowerHost: String): Boolean =
        lowerHost == "substack.com" || lowerHost.endsWith(".substack.com")

    fun classifyParameter(
        paramName: String,
        host: String? = null,
        customBlocklist: Set<String> = emptySet(),
        customAllowlist: Set<String> = emptySet()
    ): ParameterCategory {
        val lower = paramName.lowercase()

        if (lower in customAllowlist) {
            return ParameterCategory.CUSTOM_ALLOWED
        }
        if (lower in customBlocklist) {
            return ParameterCategory.CUSTOM_BLOCKED
        }
        if (lower == UrlCleaner.AMAZON_PATH_PARAM_KEY) {
            return ParameterCategory.ECOMMERCE_AFFILIATE
        }

        val lowerHost = host?.lowercase()
        if (lowerHost != null) {
            if (isYouTubeHost(lowerHost)) {
                if (lower == "t") return ParameterCategory.ESSENTIAL_SAFE
                if (lower in YOUTUBE_SCOPED_PARAMS) return ParameterCategory.SOCIAL_SHARE
            }
            if (isTwitterHost(lowerHost) && lower in TWITTER_SCOPED_PARAMS) {
                return ParameterCategory.SOCIAL_SHARE
            }
            if (isThreadsHost(lowerHost) && lower == "s") {
                return ParameterCategory.SOCIAL_SHARE
            }
            if (isAmazonHost(lowerHost) && lower in AMAZON_SCOPED_PARAMS) {
                return ParameterCategory.ECOMMERCE_AFFILIATE
            }
            if (isSpotifyHost(lowerHost) && lower in SPOTIFY_SCOPED_PARAMS) {
                return ParameterCategory.SOCIAL_SHARE
            }
            if (isSubstackHost(lowerHost) && lower in SUBSTACK_SCOPED_PARAMS) {
                return ParameterCategory.EMAIL_MARKETING
            }
        } else {
            if (lower in TWITTER_SCOPED_PARAMS) return ParameterCategory.SOCIAL_SHARE
            if (lower in AMAZON_SCOPED_PARAMS) return ParameterCategory.ECOMMERCE_AFFILIATE
        }

        if (lower.startsWith("utm_") || lower.startsWith("itm_")) {
            return ParameterCategory.UTM_CAMPAIGN
        }
        if (lower.startsWith("__cft__") || lower.startsWith("__tn__")) {
            return ParameterCategory.SOCIAL_SHARE
        }
        if (lower.startsWith("at_") || lower.startsWith("vero_")) {
            return ParameterCategory.ANALYTICS_TELEMETRY
        }

        return when (lower) {
            in GOOGLE_ADS_PARAMS, in ADS_AND_CLICK_PARAMS -> ParameterCategory.ADS_CLICK_ID
            in SOCIAL_SHARE_PARAMS -> ParameterCategory.SOCIAL_SHARE
            in ECOMMERCE_PARAMS -> ParameterCategory.ECOMMERCE_AFFILIATE
            in EMAIL_AND_CRM_PARAMS -> ParameterCategory.EMAIL_MARKETING
            in ANALYTICS_PARAMS -> ParameterCategory.ANALYTICS_TELEMETRY
            else -> ParameterCategory.ESSENTIAL_SAFE
        }
    }
}
