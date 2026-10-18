package com.boardmark.app.ads

import android.content.Context
import androidx.core.content.edit

private const val PREFS_NAME = "boardmark_prefs"
private const val KEY_LAST_SHOWN_AT = "interstitial_last_shown_at"

/**
 * 全画面広告(アプリ起動広告・インタースティシャル)どうしが連続しないよう、前回表示から
 * 最低これだけ間隔を空ける。リワード広告はユーザー自身が選んで見るものなので対象外。
 */
private const val COOLDOWN_MILLIS = 3 * 60 * 1000L

/**
 * 全画面広告の種類をまたいだ共通のルール(同時に2つ出さない・連続させない)を管理する。
 */
internal object FullScreenAdGate {

    /** いずれかの全画面広告(リワード含む)が表示中かどうか。 */
    var isShowing = false

    fun canShow(context: Context): Boolean {
        if (isShowing) return false
        val lastShownAt = prefs(context).getLong(KEY_LAST_SHOWN_AT, 0L)
        return System.currentTimeMillis() - lastShownAt >= COOLDOWN_MILLIS
    }

    /** 全画面広告が最後まで表示され閉じられたときに呼ぶ(クールダウンの起点)。 */
    fun markShown(context: Context) {
        prefs(context).edit { putLong(KEY_LAST_SHOWN_AT, System.currentTimeMillis()) }
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
