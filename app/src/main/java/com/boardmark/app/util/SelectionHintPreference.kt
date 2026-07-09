package com.boardmark.app.util

import android.content.Context
import androidx.core.content.edit

private const val PREFS_NAME = "boardmark_prefs"
private const val KEY_SELECTION_HINT_SHOWN = "selection_hint_shown"

/**
 * 選択モードのアイコンを長押しすると説明が出ることを、最初の1回だけ知らせるための
 * 既読フラグ。毎回出すと邪魔になるため、一度見せたら二度と出さない。
 */
object SelectionHintPreference {

    fun hasShownHint(context: Context): Boolean =
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_SELECTION_HINT_SHOWN, false)

    fun markHintShown(context: Context) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit { putBoolean(KEY_SELECTION_HINT_SHOWN, true) }
    }
}
