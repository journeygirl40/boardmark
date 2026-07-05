package com.boardmark.app.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boardmark.app.data.local.ThumbnailSizePreference
import com.boardmark.app.domain.model.Bookmark
import com.boardmark.app.domain.repository.BookmarkRepository
import com.boardmark.app.domain.repository.TopLevelListing
import com.boardmark.app.util.BookmarkExporter
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BookmarkListViewModel @Inject constructor(
    private val repository: BookmarkRepository,
    private val thumbnailSizePreference: ThumbnailSizePreference,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val sortOrder = MutableStateFlow(SortOrder.DATE_DESC)
    private val thumbnailSize = MutableStateFlow(thumbnailSizePreference.get())
    private val currentFolderId = MutableStateFlow<Long?>(null)
    private val selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    private val isSelectionMode = MutableStateFlow(false)

    // ドラッグ選択中に同じアイテムへ連続ヒットしてもトグルを繰り返さないための内部状態
    private var lastDragHitId: Long? = null

    private sealed interface ScreenContent {
        data class TopLevel(val listing: TopLevelListing) : ScreenContent
        data class FolderDetail(val folderId: Long, val bookmarks: List<Bookmark>) : ScreenContent
    }

    private val screenContent: Flow<ScreenContent> =
        currentFolderId.flatMapLatest { folderId ->
            if (folderId == null) {
                query.flatMapLatest { repository.observeTopLevel(it) }
                    .map { listing -> ScreenContent.TopLevel(listing) as ScreenContent }
            } else {
                query.flatMapLatest { repository.observeFolderContents(folderId, it) }
                    .map { bookmarks -> ScreenContent.FolderDetail(folderId, bookmarks) as ScreenContent }
            }
        }

    private data class SelectionState(val ids: Set<Long>, val active: Boolean)
    private val selectionState = combine(selectedIds, isSelectionMode, ::SelectionState)

    private val basePartial = combine(
        screenContent, query, sortOrder, thumbnailSize, selectionState,
    ) { content, currentQuery, currentSort, currentSize, selection ->
        fun sortedBookmarks(list: List<Bookmark>) = when (currentSort) {
            SortOrder.DATE_DESC -> list.sortedByDescending { it.addedAt }
            SortOrder.NAME_ASC -> list.sortedBy { (it.title ?: it.url).lowercase() }
        }

        val gridItems: List<BookmarkGridItem> = when (content) {
            is ScreenContent.TopLevel -> {
                val sortedFolders = when (currentSort) {
                    SortOrder.DATE_DESC -> content.listing.folders.sortedByDescending { it.folder.createdAt }
                    SortOrder.NAME_ASC -> content.listing.folders.sortedBy { it.folder.name.lowercase() }
                }
                sortedFolders.map(BookmarkGridItem::FolderItem) +
                    sortedBookmarks(content.listing.ungroupedBookmarks).map(BookmarkGridItem::BookmarkItem)
            }
            is ScreenContent.FolderDetail ->
                sortedBookmarks(content.bookmarks).map(BookmarkGridItem::BookmarkItem)
        }

        BookmarkListUiState(
            gridItems = gridItems,
            query = currentQuery,
            sortOrder = currentSort,
            thumbnailSize = currentSize,
            currentFolderId = (content as? ScreenContent.FolderDetail)?.folderId,
            isSelectionMode = selection.active,
            selectedIds = selection.ids,
        )
    }

    val uiState: StateFlow<BookmarkListUiState> =
        combine(basePartial, repository.observeFolders()) { partial, folders ->
            partial.copy(
                allFolders = folders,
                currentFolderName = partial.currentFolderId?.let { fid -> folders.find { it.id == fid }?.name },
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = BookmarkListUiState(thumbnailSize = thumbnailSizePreference.get()),
        )

    fun onQueryChange(newQuery: String) {
        query.value = newQuery
    }

    fun onSortOrderChange(newSortOrder: SortOrder) {
        sortOrder.value = newSortOrder
    }

    fun onThumbnailSizeChange(newSize: ThumbnailSize) {
        thumbnailSize.value = newSize
        thumbnailSizePreference.set(newSize)
    }

    fun onEnterFolder(folderId: Long) {
        clearSelection()
        query.value = ""
        currentFolderId.value = folderId
    }

    fun onExitFolder() {
        clearSelection()
        query.value = ""
        currentFolderId.value = null
    }

    fun onToggleSelection(bookmarkId: Long) {
        selectedIds.update { if (bookmarkId in it) it - bookmarkId else it + bookmarkId }
        isSelectionMode.value = selectedIds.value.isNotEmpty()
    }

    fun onSelectionLongPressStart(bookmarkId: Long) {
        isSelectionMode.value = true
        selectedIds.update { it + bookmarkId }
        lastDragHitId = bookmarkId
    }

    fun onSelectionDragOver(bookmarkId: Long) {
        if (!isSelectionMode.value || bookmarkId == lastDragHitId) return
        selectedIds.update { it + bookmarkId }
        lastDragHitId = bookmarkId
    }

    fun onSelectionDragEnd() {
        lastDragHitId = null
    }

    fun clearSelection() {
        selectedIds.value = emptySet()
        isSelectionMode.value = false
        lastDragHitId = null
    }

    fun onMoveSelectionTo(folderId: Long?) {
        val ids = selectedIds.value
        viewModelScope.launch {
            repository.moveBookmarksToFolder(ids, folderId)
            clearSelection()
        }
    }

    fun onCreateFolderAndMoveSelection(name: String) {
        val ids = selectedIds.value
        viewModelScope.launch {
            val newFolderId = repository.createFolder(name)
            repository.moveBookmarksToFolder(ids, newFolderId)
            clearSelection()
        }
    }

    fun onDeleteSelection() {
        val ids = selectedIds.value
        viewModelScope.launch {
            repository.deleteByIds(ids)
            clearSelection()
        }
    }

    fun onDeleteFolder(folderId: Long) {
        viewModelScope.launch {
            repository.deleteFolder(folderId)
            if (currentFolderId.value == folderId) onExitFolder()
        }
    }

    fun onRenameFolder(folderId: Long, name: String) {
        viewModelScope.launch { repository.renameFolder(folderId, name) }
    }

    suspend fun fetchCandidateImages(bookmark: Bookmark): List<String> =
        repository.fetchCandidateImages(bookmark)

    fun onSetManualThumbnail(bookmark: Bookmark, imageUrl: String) {
        viewModelScope.launch { repository.setManualThumbnail(bookmark, imageUrl) }
    }

    suspend fun exportBookmarksJson(): String {
        val (bookmarks, folders) = repository.getAllForExport()
        return BookmarkExporter.toJson(bookmarks, folders)
    }

    suspend fun getFolderThumbnailUrls(folderId: Long, limit: Int): List<String> {
        val bookmarks = repository.observeFolderContents(folderId, "").first()
        return bookmarks.mapNotNull { it.ogImageUrl ?: it.faviconUrl }.take(limit)
    }
}
