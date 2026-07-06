package com.boardmark.app.ui.list

import androidx.compose.runtime.Immutable
import com.boardmark.app.domain.model.Bookmark
import com.boardmark.app.domain.model.FolderWithPreview

@Immutable
sealed interface BookmarkGridItem {
    val key: String

    @Immutable
    data class FolderItem(val data: FolderWithPreview) : BookmarkGridItem {
        override val key get() = "folder:${data.folder.id}"
    }

    @Immutable
    data class BookmarkItem(val bookmark: Bookmark) : BookmarkGridItem {
        override val key get() = "bookmark:${bookmark.id}"
    }
}
