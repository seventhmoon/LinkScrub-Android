package com.androidfung.linkscrub.data.network

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class RedirectUnshortenerTest {

    @Test
    fun `unshorten detects cyclic redirect loop and halts immediately`() = runBlocking {
        val urlA = "https://example.com/loop-a"
        val urlB = "https://example.com/loop-b"

        val interceptor = Interceptor { chain ->
            val requestUrl = chain.request().url.toString()
            when (requestUrl) {
                urlA -> Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(302)
                    .message("Found")
                    .header("Location", urlB)
                    .body("".toResponseBody("text/plain".toMediaType()))
                    .build()
                urlB -> Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(302)
                    .message("Found")
                    .header("Location", urlA)
                    .body("".toResponseBody("text/plain".toMediaType()))
                    .build()
                else -> Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(404)
                    .message("Not Found")
                    .body("".toResponseBody("text/plain".toMediaType()))
                    .build()
            }
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .followRedirects(false)
            .followSslRedirects(false)
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build()

        val unshortener = OkHttpRedirectUnshortener(client)
        val result = unshortener.unshorten(urlA, maxHops = 10)

        // It visits urlA, redirects to urlB, next redirect location is urlA (already in visitedUrls), stops loop immediately
        assertTrue(result.isRedirected)
        assertEquals(listOf(urlB), result.redirectChain)
        assertEquals(urlB, result.finalUrl)
    }

    @Test
    fun `unshorten follows normal redirect chain to destination`() = runBlocking {
        val initialUrl = "https://example.com/start"
        val midUrl = "https://example.com/mid"
        val finalUrl = "https://example.com/destination"

        val interceptor = Interceptor { chain ->
            val requestUrl = chain.request().url.toString()
            when (requestUrl) {
                initialUrl -> Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(301)
                    .message("Moved Permanently")
                    .header("Location", midUrl)
                    .body("".toResponseBody("text/plain".toMediaType()))
                    .build()
                midUrl -> Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(302)
                    .message("Found")
                    .header("Location", finalUrl)
                    .body("".toResponseBody("text/plain".toMediaType()))
                    .build()
                finalUrl -> Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("content".toResponseBody("text/plain".toMediaType()))
                    .build()
                else -> Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(404)
                    .message("Not Found")
                    .body("".toResponseBody("text/plain".toMediaType()))
                    .build()
            }
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .followRedirects(false)
            .followSslRedirects(false)
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build()

        val unshortener = OkHttpRedirectUnshortener(client)
        val result = unshortener.unshorten(initialUrl, maxHops = 5)

        assertTrue(result.isRedirected)
        assertEquals(2, result.redirectChain.size)
        assertEquals(midUrl, result.redirectChain[0])
        assertEquals(finalUrl, result.redirectChain[1])
        assertEquals(finalUrl, result.finalUrl)
    }
}
