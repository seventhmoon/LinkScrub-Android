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
}
