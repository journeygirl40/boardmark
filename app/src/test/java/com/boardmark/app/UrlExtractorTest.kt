package com.boardmark.app

import com.boardmark.app.util.UrlExtractor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UrlExtractorTest {

    @Test
    fun `extracts url from plain text`() {
        assertEquals("https://example.com/path", UrlExtractor.extract("https://example.com/path"))
    }

    @Test
    fun `extracts url from text with title prefix`() {
        assertEquals(
            "https://example.com/article",
            UrlExtractor.extract("面白い記事だった https://example.com/article チェック"),
        )
    }

    @Test
    fun `returns null when no url present`() {
        assertNull(UrlExtractor.extract("これはURLではありません"))
    }

    @Test
    fun `returns null for null input`() {
        assertNull(UrlExtractor.extract(null))
    }
}
