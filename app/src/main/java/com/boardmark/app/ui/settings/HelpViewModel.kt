package com.boardmark.app.ui.settings

import androidx.lifecycle.ViewModel
import com.boardmark.app.domain.repository.BookmarkRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class HelpViewModel @Inject constructor(
    private val repository: BookmarkRepository,
) : ViewModel() {

    /** ヘルプ画面の「URL直接入力」ボタンから、その場でブックマークを追加する。 */
    suspend fun addBookmarkFromUrl(url: String): Boolean = repository.addBookmark(url)
}
