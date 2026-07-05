package com.boardmark.app

import com.boardmark.app.data.remote.OgpFetcher
import com.boardmark.app.data.remote.OgpResult
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OgpFetcherTest {

    private lateinit var server: MockWebServer
    private lateinit var fetcher: OgpFetcher

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        fetcher = OgpFetcher(OkHttpClient())
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `parses og image and title from html`() = runTest {
        val baseUrl = server.url("/").toString().removeSuffix("/")
        server.enqueue(
            MockResponse().setBody(
                """
                <html><head>
                <meta property="og:title" content="Sample Page" />
                <meta property="og:image" content="$baseUrl/thumb.png" />
                </head><body></body></html>
                """.trimIndent()
            )
        )

        val result = fetcher.fetch(server.url("/article").toString())

        assertTrue(result is OgpResult.Success)
        val success = result as OgpResult.Success
        assertEquals("Sample Page", success.title)
        assertEquals("$baseUrl/thumb.png", success.imageUrl)
    }

    @Test
    fun `falls back to favicon when og image missing`() = runTest {
        val baseUrl = server.url("/").toString().removeSuffix("/")
        server.enqueue(
            MockResponse().setBody(
                """
                <html><head>
                <title>No OGP Page</title>
                <link rel="icon" href="$baseUrl/favicon.ico" />
                </head><body></body></html>
                """.trimIndent()
            )
        )

        val result = fetcher.fetch(server.url("/no-ogp").toString())

        assertTrue(result is OgpResult.Success)
        val success = result as OgpResult.Success
        assertEquals("No OGP Page", success.title)
        assertEquals(null, success.imageUrl)
        assertEquals("$baseUrl/favicon.ico", success.faviconUrl)
    }

    @Test
    fun `parses description and site name`() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """
                <html><head>
                <meta property="og:title" content="Sample Page" />
                <meta property="og:description" content="A sample description" />
                <meta property="og:site_name" content="Sample Site" />
                </head><body></body></html>
                """.trimIndent()
            )
        )

        val result = fetcher.fetch(server.url("/article").toString())

        assertTrue(result is OgpResult.Success)
        val success = result as OgpResult.Success
        assertEquals("A sample description", success.description)
        assertEquals("Sample Site", success.siteName)
    }

    @Test
    fun `falls back to meta description when og description missing`() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """
                <html><head>
                <title>No OGP Page</title>
                <meta name="description" content="Fallback description" />
                </head><body></body></html>
                """.trimIndent()
            )
        )

        val result = fetcher.fetch(server.url("/no-ogp").toString())

        assertTrue(result is OgpResult.Success)
        val success = result as OgpResult.Success
        assertEquals("Fallback description", success.description)
        assertEquals(null, success.siteName)
    }

    @Test
    fun `returns failure on http error`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500))

        val result = fetcher.fetch(server.url("/error").toString())

        assertTrue(result is OgpResult.Failure)
    }

    @Test
    fun `collects candidate images from img tags`() = runTest {
        val baseUrl = server.url("/").toString().removeSuffix("/")
        server.enqueue(
            MockResponse().setBody(
                """
                <html><body>
                <img src="$baseUrl/a.png" />
                <img src="$baseUrl/b.png" />
                </body></html>
                """.trimIndent()
            )
        )

        val candidates = fetcher.fetchCandidateImages(server.url("/article").toString())

        assertEquals(listOf("$baseUrl/a.png", "$baseUrl/b.png"), candidates)
    }

    @Test
    fun `collects candidates from json-ld, image_src link and poster when no img tags exist`() = runTest {
        val baseUrl = server.url("/").toString().removeSuffix("/")
        server.enqueue(
            MockResponse().setBody(
                """
                <html><head>
                <link rel="image_src" href="$baseUrl/link-thumb.jpg" />
                <script type="application/ld+json">{"thumbnailUrl":"$baseUrl/jsonld-thumb.jpg"}</script>
                </head><body>
                <video poster="$baseUrl/poster.jpg"></video>
                </body></html>
                """.trimIndent()
            )
        )

        val candidates = fetcher.fetchCandidateImages(server.url("/spa-page").toString())

        assertEquals(
            listOf("$baseUrl/link-thumb.jpg", "$baseUrl/poster.jpg", "$baseUrl/jsonld-thumb.jpg"),
            candidates,
        )
    }
}
