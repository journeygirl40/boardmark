package com.boardmark.app.data.local

import android.content.Context
import androidx.core.content.edit
import com.boardmark.app.ui.list.THUMBNAIL_SIZE_LEVEL_DEFAULT
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** サムネイルサイズの段階(THUMBNAIL_SIZE_LEVEL_MIN〜MAX)を端末に永続化する。 */
class ThumbnailSizePreference @Inject constructor(@ApplicationContext context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun get(): Int = prefs.getInt(KEY_THUMBNAIL_SIZE_LEVEL, THUMBNAIL_SIZE_LEVEL_DEFAULT)

    fun set(level: Int) {
        prefs.edit { putInt(KEY_THUMBNAIL_SIZE_LEVEL, level) }
    }

    private companion object {
        const val PREFS_NAME = "boardmark_prefs"
        const val KEY_THUMBNAIL_SIZE_LEVEL = "thumbnail_size_level"
    }
}
