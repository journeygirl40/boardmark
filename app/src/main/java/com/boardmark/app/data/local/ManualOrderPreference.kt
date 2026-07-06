package com.boardmark.app.data.local

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** 手動並び替え(ドラッグ順)が有効かどうかを端末に永続化する。 */
class ManualOrderPreference @Inject constructor(@ApplicationContext context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun get(): Boolean = prefs.getBoolean(KEY_MANUAL_ORDER, false)

    fun set(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_MANUAL_ORDER, enabled) }
    }

    private companion object {
        const val PREFS_NAME = "boardmark_prefs"
        const val KEY_MANUAL_ORDER = "manual_order_enabled"
    }
}
