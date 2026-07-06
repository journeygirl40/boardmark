package com.boardmark.app.data.local

import android.content.Context
import androidx.core.content.edit
import com.boardmark.app.ui.list.SortCriterion
import com.boardmark.app.ui.list.SortDirection
import com.boardmark.app.ui.list.SortField
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** 選択中のソート条件(フィールド+方向のリスト)を端末に永続化する。 */
class SortCriteriaPreference @Inject constructor(@ApplicationContext context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun get(): List<SortCriterion> {
        val raw = prefs.getString(KEY_SORT_CRITERIA, null) ?: return DEFAULT
        val criteria = raw.split(",").mapNotNull { entry ->
            val parts = entry.split(":")
            if (parts.size != 2) return@mapNotNull null
            val field = parts[0].toSortFieldOrNull() ?: return@mapNotNull null
            val direction = parts[1].toSortDirectionOrNull() ?: return@mapNotNull null
            SortCriterion(field, direction)
        }
        return criteria.ifEmpty { DEFAULT }
    }

    fun set(criteria: List<SortCriterion>) {
        val raw = criteria.joinToString(",") { "${it.field.name}:${it.direction.name}" }
        prefs.edit { putString(KEY_SORT_CRITERIA, raw) }
    }

    private fun String.toSortFieldOrNull(): SortField? =
        SortField.entries.firstOrNull { it.name == this }

    private fun String.toSortDirectionOrNull(): SortDirection? =
        SortDirection.entries.firstOrNull { it.name == this }

    private companion object {
        const val PREFS_NAME = "boardmark_prefs"
        const val KEY_SORT_CRITERIA = "sort_criteria"
        val DEFAULT = listOf(SortCriterion(SortField.DATE, SortDirection.DESC))
    }
}
