package com.boardmark.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import com.boardmark.app.data.remote.OgpFetcher
import com.boardmark.app.domain.model.Bookmark
import com.boardmark.app.domain.model.FetchStatus
import com.boardmark.app.domain.model.Label
import com.boardmark.app.domain.repository.BookmarkRepository
import com.boardmark.app.domain.repository.TopLevelListing
import com.boardmark.app.domain.model.Folder
import com.boardmark.app.worker.OgpFetchWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OgpFetchWorkerTest {

    private lateinit var server: MockWebServer
    private lateinit var ogpFetcher: OgpFetcher
    private lateinit var repository: RecordingBookmarkRepository
    private lateinit var context: Context

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        ogpFetcher = OgpFetcher(OkHttpClient())
        repository = RecordingBookmarkRepository()
        context = ApplicationProvider.getApplicationContext()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun doWork_retriesOnFailureBeforeMaxAttempts() = runTest {
        server.enqueue(MockResponse().setResponseCode(500))
        val worker = buildWorker(url = server.url("/error").toString(), runAttemptCount = 0)

        val result = worker.doWork()

        assertTrue(result is ListenableWorker.Result.Retry)
        assertEquals(0, repository.updateCallCount)
    }

    @Test
    fun doWork_marksFailedOnceMaxAttemptsReached() = runTest {
        server.enqueue(MockResponse().setResponseCode(500))
        val worker = buildWorker(
            url = server.url("/error").toString(),
            runAttemptCount = OgpFetchWorker.MAX_ATTEMPTS - 1,
        )

        val result = worker.doWork()

        assertTrue(result is ListenableWorker.Result.Success)
        assertEquals(1, repository.updateCallCount)
        assertEquals(FetchStatus.FAILED, repository.lastStatus)
    }

    @Test
    fun doWork_reportsSuccessOnSuccessfulFetch() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """<html><head><meta property="og:title" content="Sample" /></head></html>"""
            )
        )
        val worker = buildWorker(url = server.url("/article").toString(), runAttemptCount = 0)

        val result = worker.doWork()

        assertTrue(result is ListenableWorker.Result.Success)
        assertEquals(1, repository.updateCallCount)
        assertEquals(FetchStatus.SUCCESS, repository.lastStatus)
    }

    private fun buildWorker(url: String, runAttemptCount: Int): OgpFetchWorker {
        val inputData = workDataOf(
            OgpFetchWorker.KEY_BOOKMARK_ID to 1L,
            OgpFetchWorker.KEY_URL to url,
        )
        return TestListenableWorkerBuilder<OgpFetchWorker>(context)
            .setInputData(inputData)
            .setRunAttemptCount(runAttemptCount)
            .setWorkerFactory(object : WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters,
                ): ListenableWorker = OgpFetchWorker(appContext, workerParameters, ogpFetcher, repository)
            })
            .build()
    }

    private class RecordingBookmarkRepository : BookmarkRepository {
        var updateCallCount = 0
        var lastStatus: FetchStatus? = null

        override fun observeTopLevel(query: String): Flow<TopLevelListing> =
            throw UnsupportedOperationException()

        override fun observeFolderContents(folderId: Long, query: String): Flow<List<Bookmark>> =
            throw UnsupportedOperationException()

        override fun observeFolders(): Flow<List<Folder>> = throw UnsupportedOperationException()

        override suspend fun addBookmark(url: String): Boolean = throw UnsupportedOperationException()

        override suspend fun updateFetchResult(
            bookmarkId: Long,
            status: FetchStatus,
            finalUrl: String?,
            title: String?,
            ogImageUrl: String?,
            faviconUrl: String?,
            description: String?,
            siteName: String?,
        ) {
            updateCallCount++
            lastStatus = status
        }

        override suspend fun deleteByIds(ids: Set<Long>): Unit = throw UnsupportedOperationException()

        override suspend fun fetchCandidateImages(bookmark: Bookmark): List<String> =
            throw UnsupportedOperationException()

        override suspend fun setManualThumbnail(bookmark: Bookmark, imageUrl: String): Unit =
            throw UnsupportedOperationException()

        override suspend fun createFolder(name: String): Long = throw UnsupportedOperationException()

        override suspend fun deleteFolder(folderId: Long): Unit = throw UnsupportedOperationException()

        override suspend fun renameFolder(folderId: Long, name: String): Unit =
            throw UnsupportedOperationException()

        override suspend fun moveBookmarksToFolder(bookmarkIds: Set<Long>, folderId: Long?): Unit =
            throw UnsupportedOperationException()

        override suspend fun getAllForExport(): Pair<List<Bookmark>, List<Folder>> =
            throw UnsupportedOperationException()

        override suspend fun renameBookmark(bookmark: Bookmark, title: String): Unit =
            throw UnsupportedOperationException()

        override suspend fun reorderBookmark(bookmarkId: Long, order: Double): Unit =
            throw UnsupportedOperationException()

        override suspend fun setFolderDefaultBrowser(folderId: Long, packageName: String?): Unit =
            throw UnsupportedOperationException()

        override suspend fun importBookmarksFromHtml(html: String): Int =
            throw UnsupportedOperationException()

        override fun observeLabels(): Flow<List<Label>> = throw UnsupportedOperationException()

        override suspend fun setBookmarkLabels(bookmarkId: Long, labelNames: Set<String>): Unit =
            throw UnsupportedOperationException()

        override suspend fun countBookmarksForLabel(labelId: Long): Int =
            throw UnsupportedOperationException()

        override suspend fun renameLabel(labelId: Long, newName: String): Unit =
            throw UnsupportedOperationException()

        override suspend fun deleteLabel(labelId: Long): Unit =
            throw UnsupportedOperationException()

        override suspend fun incrementViewCount(bookmarkId: Long): Unit =
            throw UnsupportedOperationException()

        override suspend fun getUnresolvedDuplicateGroups(): List<List<Bookmark>> =
            throw UnsupportedOperationException()

        override suspend fun ignoreDuplicateGroup(url: String): Unit =
            throw UnsupportedOperationException()
    }
}
