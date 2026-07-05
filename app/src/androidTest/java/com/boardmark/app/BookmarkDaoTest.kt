package com.boardmark.app

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.boardmark.app.data.local.AppDatabase
import com.boardmark.app.data.local.BookmarkDao
import com.boardmark.app.data.local.BookmarkEntity
import com.boardmark.app.data.local.FolderDao
import com.boardmark.app.data.local.FolderEntity
import com.boardmark.app.domain.model.FetchStatus
import java.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BookmarkDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: BookmarkDao
    private lateinit var folderDao: FolderDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.bookmarkDao()
        folderDao = db.folderDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun newBookmark(
        url: String,
        title: String?,
        folderId: Long? = null,
        fetchStatus: FetchStatus = FetchStatus.PENDING,
    ) = BookmarkEntity(
        url = url,
        originalUrl = url,
        title = title,
        ogImageUrl = null,
        faviconUrl = null,
        fetchStatus = fetchStatus,
        addedAt = Instant.now(),
        folderId = folderId,
    )

    @Test
    fun insertAndObserveAllRaw() = runTest {
        dao.insert(newBookmark("https://example.com", "Example"))

        val results = dao.observeAllRaw().first()

        assertEquals(1, results.size)
        assertEquals("Example", results[0].title)
    }

    @Test
    fun updateFetchResult_updatesFieldsAndKeepsUrlWhenFinalUrlNull() = runTest {
        val id = dao.insert(newBookmark("https://example.com", null))

        dao.updateFetchResult(
            id = id,
            status = FetchStatus.FAILED,
            finalUrl = null,
            title = null,
            ogImageUrl = null,
            faviconUrl = null,
            description = null,
            siteName = null,
        )

        val updated = dao.findById(id)
        assertEquals(FetchStatus.FAILED, updated?.fetchStatus)
        assertEquals("https://example.com", updated?.url)
    }

    @Test
    fun searchWithinFolderByTitleOrUrl() = runTest {
        val folderId = folderDao.insert(FolderEntity(name = "Pets", createdAt = Instant.now()))
        dao.insert(newBookmark("https://cats.example.com", "Cute Cats", folderId = folderId))
        dao.insert(newBookmark("https://dogs.example.com", "Good Dogs", folderId = folderId))
        dao.insert(newBookmark("https://other.example.com", "Cat food outside folder", folderId = null))

        val results = dao.observeByFolder(folderId, "Cat").first()

        assertEquals(1, results.size)
        assertEquals("Cute Cats", results[0].title)
    }

    @Test
    fun moveToFolder_movesBookmarksAndClearFolderResetsThem() = runTest {
        val folderId = folderDao.insert(FolderEntity(name = "Pets", createdAt = Instant.now()))
        val id1 = dao.insert(newBookmark("https://cats.example.com", "Cats"))
        val id2 = dao.insert(newBookmark("https://dogs.example.com", "Dogs"))

        dao.moveToFolder(listOf(id1, id2), folderId)
        assertEquals(folderId, dao.findById(id1)?.folderId)
        assertEquals(folderId, dao.findById(id2)?.folderId)

        dao.clearFolder(folderId)
        assertNull(dao.findById(id1)?.folderId)
        assertNull(dao.findById(id2)?.folderId)
    }

    @Test
    fun findByOriginalUrl_returnsMatchingBookmark() = runTest {
        dao.insert(newBookmark("https://example.com/article", "Article"))

        val found = dao.findByOriginalUrl("https://example.com/article")
        val notFound = dao.findByOriginalUrl("https://example.com/other")

        assertEquals("Article", found?.title)
        assertNull(notFound)
    }

    @Test
    fun updateAddedAt_updatesOnlyAddedAtField() = runTest {
        val original = Instant.ofEpochMilli(1_000)
        val id = dao.insert(newBookmark("https://example.com", "Example").copy(addedAt = original))

        val updated = Instant.ofEpochMilli(2_000)
        dao.updateAddedAt(id, updated)

        val result = dao.findById(id)
        assertEquals(updated, result?.addedAt)
        assertEquals("Example", result?.title)
    }

    @Test
    fun deleteByIds_removesOnlySpecifiedBookmarks() = runTest {
        val id1 = dao.insert(newBookmark("https://a.example.com", "A"))
        val id2 = dao.insert(newBookmark("https://b.example.com", "B"))

        dao.deleteByIds(listOf(id1))

        val remaining = dao.observeAllRaw().first()
        assertEquals(1, remaining.size)
        assertEquals(id2, remaining[0].id)
    }
}
