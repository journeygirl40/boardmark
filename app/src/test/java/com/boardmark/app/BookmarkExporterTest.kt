package com.boardmark.app

import com.boardmark.app.domain.model.Bookmark
import com.boardmark.app.domain.model.FetchStatus
import com.boardmark.app.domain.model.Folder
import com.boardmark.app.util.BookmarkExporter
import java.time.Instant
import org.junit.Assert.assertTrue
import org.junit.Test

class BookmarkExporterTest {

    @Test
    fun `serializes bookmarks and folders as json`() {
        val folder = Folder(id = 1, name = "Travel", createdAt = Instant.ofEpochMilli(1_000))
        val bookmark = Bookmark(
            id = 10,
            url = "https://example.com",
            originalUrl = "https://example.com",
            title = "Example",
            ogImageUrl = null,
            faviconUrl = null,
            fetchStatus = FetchStatus.SUCCESS,
            addedAt = Instant.ofEpochMilli(2_000),
            folderId = 1,
            description = "A description",
            siteName = "Example Site",
        )

        val json = BookmarkExporter.toJson(listOf(bookmark), listOf(folder))

        assertTrue(json.contains(""""name":"Travel""""))
        assertTrue(json.contains(""""title":"Example""""))
        assertTrue(json.contains(""""description":"A description""""))
        assertTrue(json.contains(""""siteName":"Example Site""""))
        assertTrue(json.contains(""""folderId":1"""))
    }

    @Test
    fun `escapes quotes and backslashes in strings`() {
        val bookmark = Bookmark(
            id = 1,
            url = "https://example.com",
            originalUrl = "https://example.com",
            title = """He said "hi" \ bye""",
            ogImageUrl = null,
            faviconUrl = null,
            fetchStatus = FetchStatus.SUCCESS,
            addedAt = Instant.ofEpochMilli(1_000),
        )

        val json = BookmarkExporter.toJson(listOf(bookmark), emptyList())

        assertTrue(json.contains("""He said \"hi\" \\ bye"""))
    }

    @Test
    fun `renders null fields as json null`() {
        val bookmark = Bookmark(
            id = 1,
            url = "https://example.com",
            originalUrl = "https://example.com",
            title = null,
            ogImageUrl = null,
            faviconUrl = null,
            fetchStatus = FetchStatus.PENDING,
            addedAt = Instant.ofEpochMilli(1_000),
        )

        val json = BookmarkExporter.toJson(listOf(bookmark), emptyList())

        assertTrue(json.contains(""""title":null"""))
        assertTrue(json.contains(""""folderId":null"""))
    }
}
