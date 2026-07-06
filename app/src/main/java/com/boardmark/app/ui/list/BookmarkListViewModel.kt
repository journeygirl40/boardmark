package com.boardmark.app.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boardmark.app.data.local.BrowserPreference
import com.boardmark.app.data.local.ManualOrderPreference
import com.boardmark.app.data.local.SortCriteriaPreference
import com.boardmark.app.data.local.ThumbnailSizePreference
import com.boardmark.app.domain.model.Bookmark
import com.boardmark.app.domain.repository.BookmarkRepository
import com.boardmark.app.domain.repository.TopLevelListing
import com.boardmark.app.util.domainOf
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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
    private val browserPreference: BrowserPreference,
    private val manualOrderPreference: ManualOrderPreference,
    private val sortCriteriaPreference: SortCriteriaPreference,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val sortCriteria = MutableStateFlow(sortCriteriaPreference.get())
    private val isManualOrder = MutableStateFlow(manualOrderPreference.get())
    private val activeLabelFilter = MutableStateFlow<Set<Long>>(emptySet())
    private val thumbnailSize = MutableStateFlow(thumbnailSizePreference.get())
    private val currentFolderId = MutableStateFlow<Long?>(null)
    private val selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    private val isSelectionMode = MutableStateFlow(false)

    // ドラッグ選択中に同じアイテムへ連続ヒットしてもトグルを繰り返さないための内部状態
    private var lastDragHitId: Long? = null

    // アプリを開いたときに一度だけ検出する、未解決(「すべて残す」で無視されていない)の重複URLグループ
    private val duplicateGroupsFlow = MutableStateFlow<List<List<Bookmark>>>(emptyList())
    val duplicateGroups: StateFlow<List<List<Bookmark>>> = duplicateGroupsFlow

    // 複数選択時の画像自動取得の進行状況。null=非実行中。UI操作を妨げないバーで表示するため、
    // 選択解除やモーダル遷移とは独立にStateFlowとして公開する。
    private val thumbnailFetchProgressFlow = MutableStateFlow<ThumbnailFetchProgress?>(null)
    val thumbnailFetchProgress: StateFlow<ThumbnailFetchProgress?> = thumbnailFetchProgressFlow

    init {
        viewModelScope.launch {
            duplicateGroupsFlow.value = repository.getUnresolvedDuplicateGroups()
        }
    }

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

    private data class SortFilterState(
        val criteria: List<SortCriterion>,
        val manual: Boolean,
        val labelFilter: Set<Long>,
    )
    private val sortFilterState = combine(sortCriteria, isManualOrder, activeLabelFilter, ::SortFilterState)

    private fun bookmarkMatchesLabels(bookmark: Bookmark, labelIds: Set<Long>): Boolean =
        labelIds.isEmpty() || bookmark.labels.map { it.id }.toSet().containsAll(labelIds)

    private fun applyLabelFilter(list: List<Bookmark>, labelIds: Set<Long>): List<Bookmark> {
        if (labelIds.isEmpty()) return list
        return list.filter { bookmark -> bookmarkMatchesLabels(bookmark, labelIds) }
    }

    private fun sortedBookmarks(list: List<Bookmark>, criteria: List<SortCriterion>, manual: Boolean): List<Bookmark> {
        if (manual) return list.sortedBy { it.manualOrder }
        if (criteria.isEmpty()) return list
        val comparator = criteria
            .map { criterion ->
                val base: Comparator<Bookmark> = when (criterion.field) {
                    SortField.DATE -> compareBy { it.addedAt }
                    SortField.TITLE -> compareBy { (it.title ?: it.url).lowercase() }
                    SortField.VIEW_COUNT -> compareBy { it.viewCount }
                    SortField.DOMAIN -> compareBy { domainOf(it.url).lowercase() }
                }
                if (criterion.direction == SortDirection.DESC) base.reversed() else base
            }
            .reduce { acc, next -> acc.then(next) }
        return list.sortedWith(comparator)
    }

    private val basePartial = combine(
        screenContent, query, sortFilterState, thumbnailSize, selectionState,
    ) { content, currentQuery, sortFilter, currentSize, selection ->
        fun sorted(list: List<Bookmark>) = sortedBookmarks(
            applyLabelFilter(list, sortFilter.labelFilter),
            sortFilter.criteria,
            sortFilter.manual,
        )

        val gridItems: List<BookmarkGridItem> = when (content) {
            is ScreenContent.TopLevel -> {
                val visibleFolders = content.listing.folders.filter { folderData ->
                    sortFilter.labelFilter.isEmpty() ||
                        folderData.contents.any { bookmarkMatchesLabels(it, sortFilter.labelFilter) }
                }
                val sortedFolders = if (sortFilter.criteria.firstOrNull()?.field == SortField.TITLE) {
                    visibleFolders.sortedBy { it.folder.name.lowercase() }
                } else {
                    visibleFolders.sortedByDescending { it.folder.createdAt }
                }
                sortedFolders.map(BookmarkGridItem::FolderItem) +
                    sorted(content.listing.ungroupedBookmarks).map(BookmarkGridItem::BookmarkItem)
            }
            is ScreenContent.FolderDetail ->
                sorted(content.bookmarks).map(BookmarkGridItem::BookmarkItem)
        }

        BookmarkListUiState(
            gridItems = gridItems,
            query = currentQuery,
            sortCriteria = sortFilter.criteria,
            isManualOrder = sortFilter.manual,
            activeLabelFilter = sortFilter.labelFilter,
            thumbnailSizeLevel = currentSize,
            currentFolderId = (content as? ScreenContent.FolderDetail)?.folderId,
            isSelectionMode = selection.active,
            selectedIds = selection.ids,
        )
    }

    val uiState: StateFlow<BookmarkListUiState> =
        combine(basePartial, repository.observeFolders(), repository.observeLabels()) { partial, folders, labels ->
            partial.copy(
                allFolders = folders,
                allLabels = labels,
                currentFolderName = partial.currentFolderId?.let { fid -> folders.find { it.id == fid }?.name },
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = BookmarkListUiState(thumbnailSizeLevel = thumbnailSizePreference.get()),
        )

    fun onQueryChange(newQuery: String) {
        query.value = newQuery
    }

    fun onSortCriteriaChange(newCriteria: List<SortCriterion>) {
        sortCriteria.value = newCriteria
        sortCriteriaPreference.set(newCriteria)
        isManualOrder.value = false
        manualOrderPreference.set(false)
    }

    fun onToggleLabelFilter(labelId: Long) {
        activeLabelFilter.update { if (labelId in it) it - labelId else it + labelId }
    }

    fun onBookmarkOpened(bookmarkId: Long) {
        viewModelScope.launch { repository.incrementViewCount(bookmarkId) }
    }

    fun onThumbnailSizeChange(newLevel: Int) {
        thumbnailSize.value = newLevel
        thumbnailSizePreference.set(newLevel)
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

    fun onSelectAll() {
        val ids = uiState.value.gridItems
            .filterIsInstance<BookmarkGridItem.BookmarkItem>()
            .map { it.bookmark.id }
            .toSet()
        selectedIds.value = ids
        isSelectionMode.value = ids.isNotEmpty()
    }

    /**
     * 選択中の各ブックマークに、指定したラベルを追加/削除する。
     * addNames/removeNamesに含まれないラベルは、各ブックマーク個別の設定のまま変更しない
     * (一律上書きすると、他の選択アイテムに付いていた個別のラベルが消えてしまうため)。
     */
    fun onSetLabelsForSelection(ids: Set<Long>, addNames: Set<String>, removeNames: Set<String>) {
        val bookmarks = uiState.value.gridItems
            .filterIsInstance<BookmarkGridItem.BookmarkItem>()
            .map { it.bookmark }
            .filter { it.id in ids }
        viewModelScope.launch {
            bookmarks.forEach { bookmark ->
                val currentNames = bookmark.labels.map { it.name }.toSet()
                val newNames = (currentNames - removeNames) + addNames
                repository.setBookmarkLabels(bookmark.id, newNames)
            }
        }
    }

    /** 選択中の各ブックマークについて、候補画像の1件目を自動でサムネイルに設定する。 */
    fun onAutoSetFirstThumbnailForSelection(ids: Set<Long>) {
        val bookmarks = uiState.value.gridItems
            .filterIsInstance<BookmarkGridItem.BookmarkItem>()
            .map { it.bookmark }
            .filter { it.id in ids }
        if (bookmarks.isEmpty()) return
        viewModelScope.launch {
            thumbnailFetchProgressFlow.value = ThumbnailFetchProgress(completed = 0, total = bookmarks.size)
            bookmarks.forEachIndexed { index, bookmark ->
                val imageUrl = repository.fetchCandidateImages(bookmark).firstOrNull()
                if (imageUrl != null) repository.setManualThumbnail(bookmark, imageUrl)
                thumbnailFetchProgressFlow.value = ThumbnailFetchProgress(completed = index + 1, total = bookmarks.size)
            }
            thumbnailFetchProgressFlow.value = null
        }
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

    fun onRenameBookmark(bookmark: Bookmark, title: String) {
        viewModelScope.launch { repository.renameBookmark(bookmark, title) }
    }

    /**
     * ドラッグ操作で targetId の直前に draggedIds(1件以上、選択中の全件)をまとめて移動する。
     * 現在の表示順を manualOrder として正規化してから挿入するため、直前のソート順
     * (日付/名前)が何であっても見た目どおりの位置に反映される。draggedIds同士の
     * 相対順序は移動前の並びを保つ。
     */
    fun onReorderBookmarks(draggedIds: Set<Long>, targetId: Long) {
        if (draggedIds.isEmpty() || targetId in draggedIds) return
        val bookmarks = uiState.value.gridItems
            .filterIsInstance<BookmarkGridItem.BookmarkItem>()
            .map { it.bookmark }
        val draggedInOrder = bookmarks.filter { it.id in draggedIds }
        val withoutDragged = bookmarks.filterNot { it.id in draggedIds }
        val targetIndex = withoutDragged.indexOfFirst { it.id == targetId }
        if (targetIndex < 0) return

        viewModelScope.launch {
            withoutDragged.forEachIndexed { index, bookmark ->
                if (bookmark.manualOrder != index.toDouble()) {
                    repository.reorderBookmark(bookmark.id, index.toDouble())
                }
            }
            val step = 1.0 / (draggedInOrder.size + 1)
            draggedInOrder.forEachIndexed { i, bookmark ->
                repository.reorderBookmark(bookmark.id, targetIndex - 1.0 + step * (i + 1))
            }
            isManualOrder.value = true
            manualOrderPreference.set(true)
        }
    }

    /** フォルダの既定ブラウザ、なければアプリ全体の既定ブラウザを返す。 */
    fun resolveEffectiveBrowser(): String? {
        val state = uiState.value
        val folderDefault = state.currentFolderId?.let { folderId ->
            state.allFolders.find { it.id == folderId }?.defaultBrowserPackage
        }
        return folderDefault ?: browserPreference.get()
    }

    fun onSetGlobalDefaultBrowser(packageName: String?) {
        browserPreference.set(packageName)
    }

    fun onSetFolderDefaultBrowser(folderId: Long, packageName: String?) {
        viewModelScope.launch { repository.setFolderDefaultBrowser(folderId, packageName) }
    }

    /** 「すべて残す」: このURLグループを以後の重複検知の対象外にする。 */
    fun onKeepAllDuplicates(url: String) {
        viewModelScope.launch { repository.ignoreDuplicateGroup(url) }
        duplicateGroupsFlow.update { groups -> groups.filterNot { it.first().url == url } }
    }

    /** 「残す方を選ぶ」: keepId以外をグループから削除する。 */
    fun onResolveDuplicateKeepOne(keepId: Long, group: List<Bookmark>) {
        val deleteIds = group.filterNot { it.id == keepId }.map { it.id }.toSet()
        viewModelScope.launch { repository.deleteByIds(deleteIds) }
        duplicateGroupsFlow.update { groups -> groups.filterNot { it === group } }
    }

    /** ダイアログを閉じるだけ(判断を保留)。DBには何も残さず、次回起動時に再度検知される。 */
    fun onDismissDuplicateGroup(group: List<Bookmark>) {
        duplicateGroupsFlow.update { groups -> groups.filterNot { it === group } }
    }
}
