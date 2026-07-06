package com.boardmark.app

import com.boardmark.app.domain.model.Bookmark
import com.boardmark.app.domain.model.FetchStatus
import com.boardmark.app.domain.model.Folder
import com.boardmark.app.util.BookmarkExporter
import java.time.Instant
import org.junit.Assert.assertTrue
import org.junit.Test

class BookmarkExporterTest {

    private fun bookmark(
        id: Long,
        url: String = "https://example.com",
        title: String? = "Example",
        folderId: Long? = null,
    ) = Bookmark(
        id = id,
        url = url,
        originalUrl = url,
        title = title,
        ogImageUrl = null,
        faviconUrl = null,
        fetchStatus = FetchStatus.SUCCESS,
        addedAt = Instant.ofEpochMilli(2_000),
        folderId = folderId,
    )

    @Test
    fun `renders ungrouped and folder bookmarks as netscape html`() {
        val folder = Folder(id = 1, name = "Travel", createdAt = Instant.ofEpochMilli(1_000))
        val ungrouped = bookmark(id = 1, url = "https://example.com", title = "Example")
        val grouped = bookmark(id = 2, url = "https://travel.example.com", title = "Trip", folderId = 1)

        val html = BookmarkExporter.toNetscapeHtml(listOf(ungrouped, grouped), listOf(folder))

        assertTrue(html.contains("<H3 ADD_DATE=\"1\">Travel</H3>"))
        assertTrue(html.contains("HREF=\"https://example.com\""))
        assertTrue(html.contains(">Example</A>"))
        assertTrue(html.contains("HREF=\"https://travel.example.com\""))
        assertTrue(html.contains(">Trip</A>"))
    }

    @Test
    fun `escapes html special characters in title and url`() {
        val bookmark = bookmark(id = 1, url = "https://example.com?a=1&b=2", title = """He said "hi" <bye>""")

        val html = BookmarkExporter.toNetscapeHtml(listOf(bookmark), emptyList())

        assertTrue(html.contains("https://example.com?a=1&amp;b=2"))
        assertTrue(html.contains("He said &quot;hi&quot; &lt;bye&gt;"))
    }

    @Test
    fun `falls back to url when title is null`() {
        val bookmark = bookmark(id = 1, url = "https://example.com", title = null)

        val html = BookmarkExporter.toNetscapeHtml(listOf(bookmark), emptyList())

        assertTrue(html.contains(">https://example.com</A>"))
    }
}
