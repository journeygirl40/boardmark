package com.boardmark.app.data.repository

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.workDataOf
import com.boardmark.app.data.local.BookmarkDao
import com.boardmark.app.data.local.BookmarkEntity
import com.boardmark.app.data.local.BookmarkLabelCrossRef
import com.boardmark.app.data.local.BookmarkWithLabels
import com.boardmark.app.data.local.FolderDao
import com.boardmark.app.data.local.FolderEntity
import com.boardmark.app.data.local.LabelDao
import com.boardmark.app.data.local.LabelEntity
import com.boardmark.app.data.remote.OgpFetcher
import com.boardmark.app.domain.model.Bookmark
import com.boardmark.app.domain.model.FetchStatus
import com.boardmark.app.domain.model.Folder
import com.boardmark.app.domain.model.FolderWithPreview
import com.boardmark.app.domain.model.Label
import com.boardmark.app.domain.repository.BookmarkRepository
import com.boardmark.app.domain.repository.TopLevelListing
import com.boardmark.app.util.BookmarkImporter
import com.boardmark.app.worker.OgpFetchWorker
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
    private val labelDao: LabelDao,
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
                        contents = contents,
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
        bookmarkDao.observeByFolder(folderId, query).map { it.map(BookmarkWithLabels::toDomain) }

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

        val now = Instant.now()
        val entity = BookmarkEntity(
            url = url,
            originalUrl = url,
            title = null,
            ogImageUrl = null,
            faviconUrl = null,
            fetchStatus = FetchStatus.PENDING,
            addedAt = now,
            manualOrder = now.toEpochMilli().toDouble(),
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

    override suspend fun renameBookmark(bookmark: Bookmark, title: String) {
        bookmarkDao.renameBookmark(bookmark.id, title)
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

    override suspend fun reorderBookmark(bookmarkId: Long, order: Double) {
        bookmarkDao.updateManualOrder(bookmarkId, order)
    }

    override suspend fun setFolderDefaultBrowser(folderId: Long, packageName: String?) {
        folderDao.setDefaultBrowser(folderId, packageName)
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

    override suspend fun importBookmarksFromHtml(html: String): Int {
        val parsed = BookmarkImporter.parseNetscapeHtml(html)
        val folderIdCache = mutableMapOf<String, Long>()
        var insertedCount = 0

        for (item in parsed) {
            if (bookmarkDao.findByOriginalUrl(item.url) != null) continue

            val folderId = if (item.folderPath.isEmpty()) {
                null
            } else {
                val folderName = item.folderPath.joinToString(" / ")
                folderIdCache.getOrPut(folderName) {
                    folderDao.findByName(folderName)?.id
                        ?: folderDao.insert(FolderEntity(name = folderName, createdAt = Instant.now()))
                }
            }

            val now = Instant.now()
            bookmarkDao.insert(
                BookmarkEntity(
                    url = item.url,
                    originalUrl = item.url,
                    title = item.title,
                    ogImageUrl = null,
                    faviconUrl = null,
                    fetchStatus = FetchStatus.SUCCESS,
                    addedAt = now,
                    folderId = folderId,
                    manualOrder = now.toEpochMilli().toDouble(),
                )
            )
            insertedCount++
        }

        return insertedCount
    }

    override fun observeLabels(): Flow<List<Label>> =
        labelDao.observeAll().map { it.map(LabelEntity::toDomain) }

    override suspend fun setBookmarkLabels(bookmarkId: Long, labelNames: Set<String>) {
        val labelIds = labelNames
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { name ->
                labelDao.findByName(name)?.id ?: labelDao.insert(LabelEntity(name = name))
            }
        labelDao.clearLabelsForBookmark(bookmarkId)
        labelDao.insertCrossRefs(labelIds.map { BookmarkLabelCrossRef(bookmarkId = bookmarkId, labelId = it) })
    }

    override suspend fun countBookmarksForLabel(labelId: Long): Int =
        labelDao.countBookmarksForLabel(labelId)

    override suspend fun createLabel(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        if (labelDao.findByName(trimmed) == null) {
            labelDao.insert(LabelEntity(name = trimmed))
        }
    }

    override suspend fun renameLabel(labelId: Long, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return
        val existing = labelDao.findByName(trimmed)
        if (existing != null && existing.id != labelId) {
            // 既に同名のラベルがある場合は、そちらへ統合する。
            labelDao.reassignCrossRefs(labelId, existing.id)
            labelDao.clearCrossRefsForLabel(labelId)
            labelDao.deleteById(labelId)
        } else {
            labelDao.renameRaw(labelId, trimmed)
        }
    }

    override suspend fun deleteLabel(labelId: Long) {
        labelDao.clearCrossRefsForLabel(labelId)
        labelDao.deleteById(labelId)
    }

    override suspend fun incrementViewCount(bookmarkId: Long) {
        bookmarkDao.incrementViewCount(bookmarkId)
    }

    override suspend fun getUnresolvedDuplicateGroups(): List<List<Bookmark>> {
        val candidates = bookmarkDao.findDuplicateCandidates().map { it.toDomain() }
        return candidates
            .groupBy { it.url }
            .values
            .filter { group -> group.size > 1 && group.any { !it.duplicateIgnored } }
    }

    override suspend fun ignoreDuplicateGroup(url: String) {
        bookmarkDao.ignoreDuplicatesForUrl(url)
    }
}
