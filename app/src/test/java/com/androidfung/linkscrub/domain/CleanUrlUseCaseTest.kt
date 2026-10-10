package com.androidfung.linkscrub.domain

import com.androidfung.linkscrub.data.model.OpenGraphMetadata
import com.androidfung.linkscrub.data.network.OpenGraphScraper
import com.androidfung.linkscrub.data.network.RedirectUnshortener
import com.androidfung.linkscrub.data.network.UnshortenResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CleanUrlUseCaseTest {

    private val urlCleaner = UrlCleaner()

    @Test
    fun `use case unshortens url and strips parameters from destination`() = runTest {
        val fakeUnshortener = object : RedirectUnshortener {
            override suspend fun unshorten(url: String, maxHops: Int): UnshortenResult {
                return if (url == "https://bit.ly/3sample") {
                    UnshortenResult(
                        initialUrl = url,
                        finalUrl = "https://target.com/page?utm_source=shortener&utm_campaign=promo&ref_id=999",
                        redirectChain = listOf("https://target.com/page?utm_source=shortener&utm_campaign=promo&ref_id=999"),
                        isRedirected = true
                    )
                } else {
                    UnshortenResult(initialUrl = url, finalUrl = url)
                }
            }
        }

        val fakeScraper = object : OpenGraphScraper {
            override suspend fun scrape(url: String): OpenGraphMetadata {
                return OpenGraphMetadata(
                    title = "Target Page Title",
                    description = "Destination description",
                    imageUrl = "https://target.com/image.png",
                    siteName = "Target"
                )
            }
        }

        val useCase = CleanUrlUseCase(
            urlCleaner = urlCleaner,
            redirectUnshortener = fakeUnshortener,
            openGraphScraper = fakeScraper
        )

        val result = useCase.invoke("https://bit.ly/3sample")

        assertEquals("https://bit.ly/3sample", result.originalUrl)
        assertEquals("https://target.com/page?ref_id=999", result.cleanedUrl)
        assertTrue(result.removedParams.contains("utm_source"))
        assertTrue(result.removedParams.contains("utm_campaign"))
        assertEquals("Target Page Title", result.metadata?.title)
    }

    @Test
    fun `use case handles unshortening disabled`() = runTest {
        val fakeUnshortener = object : RedirectUnshortener {
            override suspend fun unshorten(url: String, maxHops: Int): UnshortenResult {
                throw AssertionError("Should not be called")
            }
        }

        val fakeScraper = object : OpenGraphScraper {
            override suspend fun scrape(url: String): OpenGraphMetadata? = null
        }

        val useCase = CleanUrlUseCase(
            urlCleaner = urlCleaner,
            redirectUnshortener = fakeUnshortener,
            openGraphScraper = fakeScraper
        )

        val result = useCase.invoke(
            inputUrl = "https://example.com/item?id=42&si=track123",
            unshorten = false,
            fetchMetadata = false,
            saveToHistory = false
        )

        assertEquals("https://example.com/item?id=42", result.cleanedUrl)
        assertTrue(result.removedParams.contains("si"))
    }
}
