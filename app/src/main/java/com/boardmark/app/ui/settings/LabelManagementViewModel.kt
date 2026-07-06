package com.boardmark.app.ui.settings

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boardmark.app.domain.model.Label
import com.boardmark.app.domain.repository.BookmarkRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
data class LabelWithCount(val label: Label, val bookmarkCount: Int)

@HiltViewModel
class LabelManagementViewModel @Inject constructor(
    private val repository: BookmarkRepository,
) : ViewModel() {

    private val query = MutableStateFlow("")

    // ラベル数が多くなっても検索で絞り込めるよう、一覧はここでフィルタする。
    private val labelsWithCounts = repository.observeLabels()
        .map { labels -> labels.map { LabelWithCount(it, repository.countBookmarksForLabel(it.id)) } }

    val uiState: StateFlow<List<LabelWithCount>> = combine(labelsWithCounts, query) { items, currentQuery ->
        if (currentQuery.isBlank()) {
            items
        } else {
            items.filter { it.label.name.contains(currentQuery, ignoreCase = true) }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    fun onQueryChange(newQuery: String) {
        query.value = newQuery
    }

    fun onRename(labelId: Long, newName: String) {
        viewModelScope.launch { repository.renameLabel(labelId, newName) }
    }

    fun onDelete(labelId: Long) {
        viewModelScope.launch { repository.deleteLabel(labelId) }
    }
}
