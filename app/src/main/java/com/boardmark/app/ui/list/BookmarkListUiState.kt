package com.boardmark.app.ui.list

import com.boardmark.app.domain.model.Folder

enum class SortOrder { DATE_DESC, NAME_ASC }

enum class ThumbnailSize(val minWidthDp: Int) {
    SMALL(100),
    MEDIUM(140),
    LARGE(200),
}

data class BookmarkListUiState(
    val gridItems: List<BookmarkGridItem> = emptyList(),
    val query: String = "",
    val sortOrder: SortOrder = SortOrder.DATE_DESC,
    val thumbnailSize: ThumbnailSize = ThumbnailSize.MEDIUM,
    val currentFolderId: Long? = null,
    val currentFolderName: String? = null,
    val isSelectionMode: Boolean = false,
    val selectedIds: Set<Long> = emptySet(),
    val allFolders: List<Folder> = emptyList(),
)
