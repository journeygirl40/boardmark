package com.boardmark.app.data.local

import android.content.Context
import androidx.core.content.edit
import com.boardmark.app.ui.list.ThumbnailSize
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** サムネイルの表示サイズ(小/中/大)を端末に永続化する。 */
class ThumbnailSizePreference @Inject constructor(@ApplicationContext context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun get(): ThumbnailSize {
        val name = prefs.getString(KEY_THUMBNAIL_SIZE, null) ?: return ThumbnailSize.MEDIUM
        return runCatching { ThumbnailSize.valueOf(name) }.getOrDefault(ThumbnailSize.MEDIUM)
    }

    fun set(size: ThumbnailSize) {
        prefs.edit { putString(KEY_THUMBNAIL_SIZE, size.name) }
    }

    private companion object {
        const val PREFS_NAME = "boardmark_prefs"
        const val KEY_THUMBNAIL_SIZE = "thumbnail_size"
    }
}
