package com.picsearch.app.data.repository

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.workDataOf
import com.picsearch.app.data.local.BookmarkDao
import com.picsearch.app.data.local.BookmarkEntity
import com.picsearch.app.data.local.FolderDao
import com.picsearch.app.data.local.FolderEntity
import com.picsearch.app.data.remote.OgpFetcher
import com.picsearch.app.domain.model.Bookmark
import com.picsearch.app.domain.model.FetchStatus
import com.picsearch.app.domain.model.Folder
import com.picsearch.app.domain.model.FolderWithPreview
import com.picsearch.app.domain.repository.BookmarkRepository
import com.picsearch.app.domain.repository.TopLevelListing
import com.picsearch.app.worker.OgpFetchWorker
import java.time.Instant
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class BookmarkRepositoryImpl @Inject constructor(
    private val bookmarkDao: BookmarkDao,
    private val folderDao: FolderDao,
    private val workManager: WorkManager,
    private val ogpFetcher: OgpFetcher,
) : BookmarkRepository {

    override fun observeTopLevel(query: String): Flow<TopLevelListing> =
        combine(folderDao.observeAll(), bookmarkDao.observeAllRaw()) { folderEntities, bookmarkEntities ->
            val allBookmarks = bookmarkEntities.map { it.toDomain() }
            val byFolder = allBookmarks.groupBy { it.folderId }

            val folders = folderEntities
                .map { fe ->
                    val contents = byFolder[fe.id].orEmpty().sortedByDescending { it.addedAt }
                    FolderWithPreview(
                        folder = fe.toDomain(),
                        previewBookmarks = contents.take(4),
                        itemCount = contents.size,
                    )
                }
                .filter { query.isBlank() || it.folder.name.contains(query, ignoreCase = true) }

            val ungrouped = byFolder[null].orEmpty().filter { b ->
                query.isBlank() ||
                    b.title?.contains(query, ignoreCase = true) == true ||
                    b.url.contains(query, ignoreCase = true)
            }

            TopLevelListing(folders = folders, ungroupedBookmarks = ungrouped)
        }

    override fun observeFolderContents(folderId: Long, query: String): Flow<List<Bookmark>> =
        bookmarkDao.observeByFolder(folderId, query).map { it.map(BookmarkEntity::toDomain) }

    override fun observeFolders(): Flow<List<Folder>> =
        folderDao.observeAll().map { it.map(FolderEntity::toDomain) }

    override suspend fun addBookmark(url: String): Boolean {
        val existing = bookmarkDao.findByOriginalUrl(url)
        if (existing != null) {
            bookmarkDao.updateAddedAt(existing.id, Instant.now())
            if (existing.fetchStatus == FetchStatus.FAILED) {
                enqueueOgpFetch(existing.id, existing.originalUrl)
            }
            return false
        }

        val entity = BookmarkEntity(
            url = url,
            originalUrl = url,
            title = null,
            ogImageUrl = null,
            faviconUrl = null,
            fetchStatus = FetchStatus.PENDING,
            addedAt = Instant.now(),
        )
        val id = bookmarkDao.insert(entity)
        enqueueOgpFetch(id, url)
        return true
    }

    private fun enqueueOgpFetch(bookmarkId: Long, url: String) {
        val request = OneTimeWorkRequestBuilder<OgpFetchWorker>()
            .setInputData(
                workDataOf(
                    OgpFetchWorker.KEY_BOOKMARK_ID to bookmarkId,
                    OgpFetchWorker.KEY_URL to url,
                )
            )
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            )
            .setBackoffCriteria(BackoffPolicy.LINEAR, WorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
            .build()

        workManager.enqueue(request)
    }

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
        bookmarkDao.updateFetchResult(
            id = bookmarkId,
            status = status,
            finalUrl = finalUrl,
            title = title,
            ogImageUrl = ogImageUrl,
            faviconUrl = faviconUrl,
            description = description,
            siteName = siteName,
        )
    }

    override suspend fun deleteByIds(ids: Set<Long>) {
        if (ids.isEmpty()) return
        bookmarkDao.deleteByIds(ids.toList())
    }

    override suspend fun fetchCandidateImages(bookmark: Bookmark): List<String> =
        ogpFetcher.fetchCandidateImages(bookmark.originalUrl)

    override suspend fun setManualThumbnail(bookmark: Bookmark, imageUrl: String) {
        bookmarkDao.updateThumbnail(bookmark.id, imageUrl, FetchStatus.SUCCESS)
    }

    override suspend fun createFolder(name: String): Long =
        folderDao.insert(FolderEntity(name = name, createdAt = Instant.now()))

    override suspend fun deleteFolder(folderId: Long) {
        bookmarkDao.clearFolder(folderId)
        folderDao.delete(folderId)
    }

    override suspend fun renameFolder(folderId: Long, name: String) {
        folderDao.rename(folderId, name)
    }

    override suspend fun moveBookmarksToFolder(bookmarkIds: Set<Long>, folderId: Long?) {
        if (bookmarkIds.isEmpty()) return
        bookmarkDao.moveToFolder(bookmarkIds.toList(), folderId)
    }

    override suspend fun getAllForExport(): Pair<List<Bookmark>, List<Folder>> {
        val bookmarks = bookmarkDao.observeAllRaw().first().map { it.toDomain() }
        val folders = folderDao.observeAll().first().map { it.toDomain() }
        return bookmarks to folders
    }
}
