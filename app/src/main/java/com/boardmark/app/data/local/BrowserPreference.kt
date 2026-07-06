package com.boardmark.app.data.local

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** 既定で開くブラウザ(アプリ全体の設定)を端末に永続化する。 */
class BrowserPreference @Inject constructor(@ApplicationContext context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun get(): String? = prefs.getString(KEY_DEFAULT_BROWSER, null)

    fun set(packageName: String?) {
        prefs.edit { putString(KEY_DEFAULT_BROWSER, packageName) }
    }

    private companion object {
        const val PREFS_NAME = "boardmark_prefs"
        const val KEY_DEFAULT_BROWSER = "default_browser_package"
    }
}
