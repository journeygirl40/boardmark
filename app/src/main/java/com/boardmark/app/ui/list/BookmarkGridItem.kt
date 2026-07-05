package com.boardmark.app.ui.list

import com.boardmark.app.domain.model.Bookmark
import com.boardmark.app.domain.model.FolderWithPreview

sealed interface BookmarkGridItem {
    val key: String

    data class FolderItem(val data: FolderWithPreview) : BookmarkGridItem {
        override val key get() = "folder:${data.folder.id}"
    }

    data class BookmarkItem(val bookmark: Bookmark) : BookmarkGridItem {
        override val key get() = "bookmark:${bookmark.id}"
    }
}
