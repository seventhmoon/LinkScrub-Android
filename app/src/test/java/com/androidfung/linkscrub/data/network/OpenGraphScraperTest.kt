package com.androidfung.linkscrub.data.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenGraphScraperTest {

    private val scraper = JsoupOpenGraphScraper()

    @Test
    fun `parseHtmlMetadata extracts og tags accurately`() {
        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta property="og:title" content="Android Developers" />
                <meta property="og:description" content="Modern tools and resources for Android development." />
                <meta property="og:image" content="https://developer.android.com/images/hero.png" />
                <meta property="og:site_name" content="Google Developers" />
                <meta property="og:type" content="website" />
            </head>
            <body></body>
            </html>
        """.trimIndent()

        val metadata = scraper.parseHtmlMetadata(html, "https://developer.android.com")

        assertNotNull(metadata)
        assertEquals("Android Developers", metadata.title)
        assertEquals("Modern tools and resources for Android development.", metadata.description)
        assertEquals("https://developer.android.com/images/hero.png", metadata.imageUrl)
        assertEquals("Google Developers", metadata.siteName)
        assertEquals("website", metadata.allTags["og:type"])
        assertEquals("Android Developers", metadata.allTags["og:title"])
        assertEquals(5, metadata.allTags.size)
    }

    @Test
    fun `parseHtmlMetadata resolves relative image urls`() {
        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta property="og:title" content="Relative Image Test" />
                <meta property="og:image" content="/assets/cover.jpg" />
            </head>
            <body></body>
            </html>
        """.trimIndent()

        val metadata = scraper.parseHtmlMetadata(html, "https://example.org/news/index.html")

        assertNotNull(metadata)
        assertEquals("Relative Image Test", metadata.title)
        assertEquals("https://example.org/assets/cover.jpg", metadata.imageUrl)
        assertEquals("https://example.org/assets/cover.jpg", metadata.allTags["og:image"])
    }

    @Test
    fun `parseHtmlMetadata falls back to twitter card and standard title when og missing`() {
        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Page Standard Title</title>
                <meta name="description" content="Meta standard description" />
                <meta name="twitter:image" content="https://example.com/tw_image.png" />
            </head>
            <body></body>
            </html>
        """.trimIndent()

        val metadata = scraper.parseHtmlMetadata(html, "https://example.com")

        assertNotNull(metadata)
        assertEquals("Page Standard Title", metadata.title)
        assertEquals("Meta standard description", metadata.description)
        assertEquals("https://example.com/tw_image.png", metadata.imageUrl)
        assertNull(metadata.siteName)
        assertTrue(metadata.allTags.containsKey("twitter:image"))
        assertTrue(metadata.allTags.containsKey("description"))
    }

    @Test
    fun `parseHtmlMetadata extracts link image_src fallback when og image is absent`() {
        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Custom Thumbnail Page</title>
                <link rel="image_src" href="https://example.com/images/thumb.jpg" />
            </head>
            <body></body>
            </html>
        """.trimIndent()

        val metadata = scraper.parseHtmlMetadata(html, "https://example.com")

        assertNotNull(metadata)
        assertEquals("Custom Thumbnail Page", metadata.title)
        assertEquals("https://example.com/images/thumb.jpg", metadata.imageUrl)
        assertEquals("https://example.com/images/thumb.jpg", metadata.allTags["link:image_src"])
    }

    @Test
    fun `extractYouTubeVideoId extracts video ID across multiple YouTube URL formats`() {
        assertEquals("dQw4w9WgXcQ", JsoupOpenGraphScraper.extractYouTubeVideoId("https://youtu.be/dQw4w9WgXcQ"))
        assertEquals("dQw4w9WgXcQ", JsoupOpenGraphScraper.extractYouTubeVideoId("https://youtu.be/dQw4w9WgXcQ?si=abcdef12345"))
        assertEquals("dQw4w9WgXcQ", JsoupOpenGraphScraper.extractYouTubeVideoId("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
        assertEquals("dQw4w9WgXcQ", JsoupOpenGraphScraper.extractYouTubeVideoId("https://www.youtube.com/watch?v=dQw4w9WgXcQ&si=tracking"))
        assertEquals("dQw4w9WgXcQ", JsoupOpenGraphScraper.extractYouTubeVideoId("https://m.youtube.com/watch?v=dQw4w9WgXcQ"))
        assertEquals("dQw4w9WgXcQ", JsoupOpenGraphScraper.extractYouTubeVideoId("https://www.youtube.com/shorts/dQw4w9WgXcQ"))
        assertEquals("dQw4w9WgXcQ", JsoupOpenGraphScraper.extractYouTubeVideoId("https://youtube.com/shorts/dQw4w9WgXcQ?si=track"))
        assertEquals("dQw4w9WgXcQ", JsoupOpenGraphScraper.extractYouTubeVideoId("https://www.youtube.com/embed/dQw4w9WgXcQ"))
        assertEquals("dQw4w9WgXcQ", JsoupOpenGraphScraper.extractYouTubeVideoId("https://www.youtube.com/live/dQw4w9WgXcQ?feature=share"))

        // Invalid / Non-youtube
        assertNull(JsoupOpenGraphScraper.extractYouTubeVideoId("https://example.com/watch?v=dQw4w9WgXcQ"))
        assertNull(JsoupOpenGraphScraper.extractYouTubeVideoId("https://youtube.com/feed/trending"))
    }
}
