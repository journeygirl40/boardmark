package com.boardmark.app.ui.list

import com.boardmark.app.domain.model.Folder
import com.boardmark.app.domain.model.Label

enum class SortField { DATE, TITLE, VIEW_COUNT, DOMAIN }

enum class SortDirection { ASC, DESC }

data class SortCriterion(val field: SortField, val direction: SortDirection)

data class ThumbnailFetchProgress(val completed: Int, val total: Int)

// サムネイルサイズは「列数」を直接切り替える離散4段階。中間の大きさを動かしても
// 見た目が変わらない、ということが起きないようにするため、無段階の幅ではなく
// 列数そのものを4段階のレベルとして扱う(レベル1=1列(最大)〜レベル4=4列(最小))。
const val THUMBNAIL_SIZE_LEVEL_MIN = 1
const val THUMBNAIL_SIZE_LEVEL_MAX = 4
const val THUMBNAIL_SIZE_LEVEL_DEFAULT = 3

fun thumbnailColumnsForLevel(level: Int): Int =
    (THUMBNAIL_SIZE_LEVEL_MAX + THUMBNAIL_SIZE_LEVEL_MIN) - level

data class BookmarkListUiState(
    // 初期値はDB読み込み前を表すtrue。gridItemsが空==未登録、と誤解されないよう、
    // 一覧側はこのフラグが立っている間は空状態ではなくローディング表示を出す。
    val isLoading: Boolean = true,
    val gridItems: List<BookmarkGridItem> = emptyList(),
    val query: String = "",
    val sortCriteria: List<SortCriterion> = listOf(SortCriterion(SortField.DATE, SortDirection.DESC)),
    val isManualOrder: Boolean = false,
    val thumbnailSizeLevel: Int = THUMBNAIL_SIZE_LEVEL_DEFAULT,
    val currentFolderId: Long? = null,
    val currentFolderName: String? = null,
    val isSelectionMode: Boolean = false,
    val selectedIds: Set<Long> = emptySet(),
    val allFolders: List<Folder> = emptyList(),
    val allLabels: List<Label> = emptyList(),
    val activeLabelFilter: Set<Long> = emptySet(),
)
