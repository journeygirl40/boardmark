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

    // 表示専用の広告枠。ViewModelが持つ実データ(uiState.gridItems)には含まれず、
    // 画面側で表示用リストを組み立てる際にだけ挿入する(選択・並び替えなどの
    // 業務ロジックは常に広告を含まない元のリストを見るため)。一覧の長さに応じて
    // 複数枠を繰り返し挿入できるよう、何番目の枠かをslotIndexで区別する。
    @Immutable
    data class NativeAdItem(val slotIndex: Int) : BookmarkGridItem {
        override val key get() = "native_ad_slot:$slotIndex"
    }
}
