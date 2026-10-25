package com.androidfung.linkscrub.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UrlCleanerTest {

    private lateinit var urlCleaner: UrlCleaner

    @Before
    fun setUp() {
        urlCleaner = UrlCleaner()
    }

    @Test
    fun `cleanUrl strips utm parameters but preserves query params`() {
        val input = "https://example.com/article?utm_source=twitter&utm_medium=social&utm_campaign=spring_sale&id=12345"
        val result = urlCleaner.cleanUrl(input)

        assertEquals("https://example.com/article?id=12345", result.cleanedUrl)
        assertTrue(result.removedParams.contains("utm_source"))
        assertTrue(result.removedParams.contains("utm_medium"))
        assertTrue(result.removedParams.contains("utm_campaign"))
        assertFalse(result.removedParams.contains("id"))
    }

    @Test
    fun `cleanUrl strips fbclid and gclid`() {
        val input = "https://example.com/product?fbclid=IwAR123456789&gclid=Cj0KCQj&category=shoes&page=2"
        val result = urlCleaner.cleanUrl(input)

        assertEquals("https://example.com/product?category=shoes&page=2", result.cleanedUrl)
        assertTrue(result.removedParams.contains("fbclid"))
        assertTrue(result.removedParams.contains("gclid"))
    }

    @Test
    fun `cleanUrl strips instagram igsh and igshid`() {
        val input = "https://www.instagram.com/reel/C123456/?igsh=MWx1Y2p0&igshid=YmMyMTA2M2Y="
        val result = urlCleaner.cleanUrl(input)

        assertEquals("https://www.instagram.com/reel/C123456/", result.cleanedUrl)
        assertTrue(result.removedParams.contains("igsh"))
        assertTrue(result.removedParams.contains("igshid"))
    }

    @Test
    fun `cleanUrl strips youtube si and feature tracking`() {
        val input = "https://youtu.be/dQw4w9WgXcQ?si=abcdef123456&feature=shared"
        val result = urlCleaner.cleanUrl(input)

        assertEquals("https://youtu.be/dQw4w9WgXcQ", result.cleanedUrl)
        assertTrue(result.removedParams.contains("si"))
        assertTrue(result.removedParams.contains("feature"))
    }

    @Test
    fun `cleanUrl strips tiktok tracking params`() {
        val input = "https://www.tiktok.com/@creator/video/12345?_r=1&tt_medium=ios&tt_content=share"
        val result = urlCleaner.cleanUrl(input)

        assertEquals("https://www.tiktok.com/@creator/video/12345", result.cleanedUrl)
        assertTrue(result.removedParams.contains("_r"))
        assertTrue(result.removedParams.contains("tt_medium"))
        assertTrue(result.removedParams.contains("tt_content"))
    }

    @Test
    fun `cleanUrl leaves clean url unchanged`() {
        val input = "https://en.wikipedia.org/wiki/Kotlin_(programming_language)"
        val result = urlCleaner.cleanUrl(input)

        assertEquals(input, result.cleanedUrl)
        assertTrue(result.removedParams.isEmpty())
    }

    @Test
    fun `cleanUrl handles custom blocklist parameter`() {
        val customCleaner = UrlCleaner(customBlocklist = setOf("my_custom_ref"))
        val input = "https://example.com/search?q=test&my_custom_ref=secret_id"
        val result = customCleaner.cleanUrl(input)

        assertEquals("https://example.com/search?q=test", result.cleanedUrl)
        assertTrue(result.removedParams.contains("my_custom_ref"))
    }

    @Test
    fun `cleanUrl strips Amazon ref path and tag query param`() {
        val input = "https://www.amazon.com/dp/B08N5WRWNW/ref=cm_sw_r_cp_api_glt_fabc?tag=affiliate-20&psc=1"
        val result = urlCleaner.cleanUrl(input)

        assertEquals("https://www.amazon.com/dp/B08N5WRWNW?psc=1", result.cleanedUrl)
        assertTrue(result.removedParams.contains("tag"))
        assertTrue(result.removedParams.contains(UrlCleaner.AMAZON_PATH_PARAM_KEY))
    }

    @Test
    fun `trimTrailingPunctuation removes punctuation but preserves valid URL ends`() {
        assertEquals("https://example.com", UrlCleaner.trimTrailingPunctuation("https://example.com."))
        assertEquals("https://example.com/path", UrlCleaner.trimTrailingPunctuation("https://example.com/path),"))
        assertEquals("https://en.wikipedia.org/wiki/Kotlin_(programming_language)", UrlCleaner.trimTrailingPunctuation("https://en.wikipedia.org/wiki/Kotlin_(programming_language)"))
    }

    @Test
    fun `findFirstUrl extracts cleanly trimmed url from surrounding text`() {
        val text = "Check out this link (https://example.com/test?utm_source=tw), thanks!"
        assertEquals("https://example.com/test?utm_source=tw", UrlCleaner.findFirstUrl(text))
    }

    @Test
    fun `computeCleanedUrlWithExcludedParams handles Amazon path tracking`() {
        val input = "https://www.amazon.com/dp/B08N5WRWNW/ref=cm_sw?tag=affiliate-20&psc=1"
        val cleaned = urlCleaner.computeCleanedUrlWithExcludedParams(
            rawUrl = input,
            paramsToRemove = setOf(UrlCleaner.AMAZON_PATH_PARAM_KEY, "tag")
        )
        assertEquals("https://www.amazon.com/dp/B08N5WRWNW?psc=1", cleaned)

        val kept = urlCleaner.computeCleanedUrlWithExcludedParams(
            rawUrl = input,
            paramsToRemove = setOf("tag")
        )
        assertEquals("https://www.amazon.com/dp/B08N5WRWNW/ref=cm_sw?psc=1", kept)
    }

    @Test
    fun `cleanUrl strips Google and Merchant tracking params`() {
        val input = "https://example.com/product?gad_source=1&gbraid=0AAAAAD&wbraid=test123&srsltid=AfmBOoo&dclid=CPxyz&id=42"
        val result = urlCleaner.cleanUrl(input)

        assertEquals("https://example.com/product?id=42", result.cleanedUrl)
        assertTrue(result.removedParams.contains("gad_source"))
        assertTrue(result.removedParams.contains("gbraid"))
        assertTrue(result.removedParams.contains("wbraid"))
        assertTrue(result.removedParams.contains("srsltid"))
        assertTrue(result.removedParams.contains("dclid"))
        assertFalse(result.removedParams.contains("id"))
    }

    @Test
    fun `cleanUrl strips TikTok and ByteDance tracking params`() {
        val input = "https://www.tiktok.com/@creator/video/123?_t=8abcd&_r=1&tt_from=copy&tt_medium=ios"
        val result = urlCleaner.cleanUrl(input)

        assertEquals("https://www.tiktok.com/@creator/video/123", result.cleanedUrl)
        assertTrue(result.removedParams.contains("_t"))
        assertTrue(result.removedParams.contains("_r"))
        assertTrue(result.removedParams.contains("tt_from"))
        assertTrue(result.removedParams.contains("tt_medium"))
    }

    @Test
    fun `cleanUrl strips Twitter and X share token and click tracking t and s`() {
        val twitterUrl = "https://twitter.com/user/status/123456?s=20&t=abcdef"
        val twitterResult = urlCleaner.cleanUrl(twitterUrl)

        assertEquals("https://twitter.com/user/status/123456", twitterResult.cleanedUrl)
        assertTrue(twitterResult.removedParams.contains("s"))
        assertTrue(twitterResult.removedParams.contains("t"))

        val xUrl = "https://x.com/user/status/789101?s=46&t=xyz789"
        val xResult = urlCleaner.cleanUrl(xUrl)

        assertEquals("https://x.com/user/status/789101", xResult.cleanedUrl)
        assertTrue(xResult.removedParams.contains("s"))
        assertTrue(xResult.removedParams.contains("t"))
    }

    @Test
    fun `cleanUrl domain-aware rule preserves t timestamp on YouTube and youtu be`() {
        val ytInput = "https://www.youtube.com/watch?v=dQw4w9WgXcQ&t=120s&si=track123"
        val ytResult = urlCleaner.cleanUrl(ytInput)

        assertEquals("https://www.youtube.com/watch?v=dQw4w9WgXcQ&t=120s", ytResult.cleanedUrl)
        assertTrue(ytResult.removedParams.contains("si"))
        assertFalse(ytResult.removedParams.contains("t"))

        val youtuBeInput = "https://youtu.be/dQw4w9WgXcQ?t=2m30s&feature=shared"
        val youtuBeResult = urlCleaner.cleanUrl(youtuBeInput)

        assertEquals("https://youtu.be/dQw4w9WgXcQ?t=2m30s", youtuBeResult.cleanedUrl)
        assertTrue(youtuBeResult.removedParams.contains("feature"))
        assertFalse(youtuBeResult.removedParams.contains("t"))
    }

    @Test
    fun `cleanUrl strips Asian social and commerce tracking params`() {
        val bilibiliInput = "https://www.bilibili.com/video/BV1xx411c7mD?spm_id_from=333.999.0.0&vd_source=abc123456"
        val bilibiliResult = urlCleaner.cleanUrl(bilibiliInput)

        assertEquals("https://www.bilibili.com/video/BV1xx411c7mD", bilibiliResult.cleanedUrl)
        assertTrue(bilibiliResult.removedParams.contains("spm_id_from"))
        assertTrue(bilibiliResult.removedParams.contains("vd_source"))

        val ecommerceInput = "https://item.taobao.com/item.htm?id=987654&from_source=banner&scm=1007.123"
        val ecommerceResult = urlCleaner.cleanUrl(ecommerceInput)

        assertEquals("https://item.taobao.com/item.htm?id=987654", ecommerceResult.cleanedUrl)
        assertTrue(ecommerceResult.removedParams.contains("from_source"))
        assertTrue(ecommerceResult.removedParams.contains("scm"))
    }

    @Test
    fun `cleanUrl strips retail and Amazon expanded tracking params`() {
        val input = "https://www.amazon.com/dp/B08N5WRWNW?pf_rd_p=abc&pf_rd_r=def&pd_rd_r=123&pd_rd_w=456&pd_rd_wg=789&crid=XYZ&sprefix=item&keywords=gadget"
        val result = urlCleaner.cleanUrl(input)

        assertEquals("https://www.amazon.com/dp/B08N5WRWNW", result.cleanedUrl)
        assertTrue(result.removedParams.contains("pf_rd_p"))
        assertTrue(result.removedParams.contains("pf_rd_r"))
        assertTrue(result.removedParams.contains("pd_rd_r"))
        assertTrue(result.removedParams.contains("pd_rd_w"))
        assertTrue(result.removedParams.contains("pd_rd_wg"))
        assertTrue(result.removedParams.contains("crid"))
        assertTrue(result.removedParams.contains("sprefix"))
        assertTrue(result.removedParams.contains("keywords"))
    }

    @Test
    fun `cleanUrl strips general tracking parameters ncid yclid mailcamp`() {
        val input = "https://news.example.com/article?ncid=news_share&yclid=123456789&mailcamp=spring_newsletter&item=55"
        val result = urlCleaner.cleanUrl(input)

        assertEquals("https://news.example.com/article?item=55", result.cleanedUrl)
        assertTrue(result.removedParams.contains("ncid"))
        assertTrue(result.removedParams.contains("yclid"))
        assertTrue(result.removedParams.contains("mailcamp"))
        assertFalse(result.removedParams.contains("item"))
    }

    @Test
    fun `extractAndCleanUrlsInText replaces multiple URLs in surrounding text`() {
        val text = "Check this out https://youtu.be/123?si=track1 and also https://example.com/deal?utm_source=mail&code=SAVE20 for deals!"
        val (cleanedText, results) = urlCleaner.extractAndCleanUrlsInText(text)

        val expected = "Check this out https://youtu.be/123 and also https://example.com/deal?code=SAVE20 for deals!"
        assertEquals(expected, cleanedText)
        assertEquals(2, results.size)
        assertEquals("https://youtu.be/123", results[0].cleanedUrl)
        assertEquals("https://example.com/deal?code=SAVE20", results[1].cleanedUrl)
    }

    @Test
    fun `extractAndCleanUrlsInText preserves trailing punctuation around URLs`() {
        val text = "Read this (https://example.com/post?utm_source=rss), and this: https://x.com/user/status/99?s=20&t=abc!"
        val (cleanedText, results) = urlCleaner.extractAndCleanUrlsInText(text)

        assertEquals("Read this (https://example.com/post), and this: https://x.com/user/status/99!", cleanedText)
        assertEquals(2, results.size)
    }

    @Test
    fun `cleanUrl preserves s and t and keywords on non-tracking domains`() {
        // WordPress / generic site search using ?s=
        val searchUrl = "https://blog.example.com/?s=kotlin+coroutines&utm_source=newsletter"
        val searchResult = urlCleaner.cleanUrl(searchUrl)
        assertEquals("https://blog.example.com/?s=kotlin+coroutines", searchResult.cleanedUrl)
        assertFalse(searchResult.removedParams.contains("s"))

        // Twitch / Vimeo timestamp using ?t=
        val twitchUrl = "https://www.twitch.tv/videos/123456789?t=01h15m30s&tt_medium=clips"
        val twitchResult = urlCleaner.cleanUrl(twitchUrl)
        assertEquals("https://www.twitch.tv/videos/123456789?t=01h15m30s", twitchResult.cleanedUrl)
        assertFalse(twitchResult.removedParams.contains("t"))

        // Job board search using ?keywords= and ?tag=
        val jobsUrl = "https://jobs.example.com/search?keywords=android&tag=remote&gclid=123"
        val jobsResult = urlCleaner.cleanUrl(jobsUrl)
        assertEquals("https://jobs.example.com/search?keywords=android&tag=remote", jobsResult.cleanedUrl)
        assertFalse(jobsResult.removedParams.contains("keywords"))
        assertFalse(jobsResult.removedParams.contains("tag"))
    }

    @Test
    fun `cleanUrl strips Reddit LinkedIn Threads Spotify Substack and AliExpress tracking params`() {
        val redditUrl = "https://www.reddit.com/r/androiddev/comments/abc/title/?rdt=12345&share_id=xyz&context=3"
        val redditResult = urlCleaner.cleanUrl(redditUrl)
        assertEquals("https://www.reddit.com/r/androiddev/comments/abc/title/?context=3", redditResult.cleanedUrl)
        assertTrue(redditResult.removedParams.containsAll(listOf("rdt", "share_id")))

        val linkedInUrl = "https://www.linkedin.com/posts/activity-123?trk=public_profile&lipi=urn%3Ali%3Apage&rcm=abc"
        val linkedInResult = urlCleaner.cleanUrl(linkedInUrl)
        assertEquals("https://www.linkedin.com/posts/activity-123", linkedInResult.cleanedUrl)
        assertTrue(linkedInResult.removedParams.containsAll(listOf("trk", "lipi", "rcm")))

        val threadsUrl = "https://www.threads.net/@user/post/C123?xmt=AQGz123&s=09"
        val threadsResult = urlCleaner.cleanUrl(threadsUrl)
        assertEquals("https://www.threads.net/@user/post/C123", threadsResult.cleanedUrl)
        assertTrue(threadsResult.removedParams.containsAll(listOf("xmt", "s")))

        val spotifyUrl = "https://open.spotify.com/track/4cOdK2wGLETKBW3PvgPWqT?si=12345&nd=1&pt=abc"
        val spotifyResult = urlCleaner.cleanUrl(spotifyUrl)
        assertEquals("https://open.spotify.com/track/4cOdK2wGLETKBW3PvgPWqT", spotifyResult.cleanedUrl)
        assertTrue(spotifyResult.removedParams.containsAll(listOf("si", "nd", "pt")))

        val substackUrl = "https://example.substack.com/p/my-article?r=1a2b3c&triedRedirect=true"
        val substackResult = urlCleaner.cleanUrl(substackUrl)
        assertEquals("https://example.substack.com/p/my-article", substackResult.cleanedUrl)
        assertTrue(substackResult.removedParams.containsAll(listOf("r", "triedRedirect")))

        val aliExpressUrl = "https://www.aliexpress.com/item/100500123.html?aff_fcid=123&aff_fsk=456&pdp_npi=4%40dis&sku_id=999"
        val aliResult = urlCleaner.cleanUrl(aliExpressUrl)
        assertEquals("https://www.aliexpress.com/item/100500123.html?sku_id=999", aliResult.cleanedUrl)
        assertTrue(aliResult.removedParams.containsAll(listOf("aff_fcid", "aff_fsk", "pdp_npi")))
    }

    @Test
    fun `cleanUrl respects customAllowlist over built-in tracking params`() {
        val customCleaner = UrlCleaner(
            customBlocklist = setOf("custom_track"),
            customAllowlist = setOf("utm_source", "tag")
        )
        val input = "https://www.amazon.com/dp/B08N5WRWNW?utm_source=partner&utm_medium=banner&tag=keep-20&custom_track=999"
        val result = customCleaner.cleanUrl(input)

        assertEquals("https://www.amazon.com/dp/B08N5WRWNW?utm_source=partner&tag=keep-20", result.cleanedUrl)
        assertFalse(result.removedParams.contains("utm_source"))
        assertFalse(result.removedParams.contains("tag"))
        assertTrue(result.removedParams.contains("utm_medium"))
        assertTrue(result.removedParams.contains("custom_track"))
    }
}
