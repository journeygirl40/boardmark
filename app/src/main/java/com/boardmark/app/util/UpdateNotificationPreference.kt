package com.boardmark.app.util

import android.content.Context
import androidx.core.content.edit

private const val PREFS_NAME = "boardmark_prefs"
private const val KEY_LAST_NOTIFIED_VERSION_CODE = "last_notified_update_version_code"

/**
 * 「新しいバージョンがあります」通知の既読管理。バージョンコードごとに1回だけ通知する
 * (毎回の起動でしつこく出さない)。より新しいバージョンがPlayに公開された場合は、
 * そのバージョンについて改めて1回だけ通知する。
 */
object UpdateNotificationPreference {

    private fun lastNotifiedVersionCode(context: Context): Int =
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_LAST_NOTIFIED_VERSION_CODE, 0)

    fun markNotified(context: Context, versionCode: Int) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit { putInt(KEY_LAST_NOTIFIED_VERSION_CODE, versionCode) }
    }

    /** availableVersionCodeがまだ通知していない新しいバージョンであればtrue。 */
    fun shouldNotify(context: Context, availableVersionCode: Int): Boolean =
        availableVersionCode > lastNotifiedVersionCode(context)
}
