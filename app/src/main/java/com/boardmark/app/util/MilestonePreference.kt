package com.boardmark.app.util

import android.content.Context
import androidx.core.content.edit

private const val PREFS_NAME = "boardmark_prefs"
private const val KEY_LAST_CELEBRATED_MILESTONE = "last_celebrated_milestone"

/**
 * ブックマーク件数の節目をお祝いする演出の既読管理。「これまでにお祝い済みの
 * 最大マイルストーン」を1つだけ保持する。インポート等で件数が一気に増えても
 * 最新の1つだけを祝う自然な挙動になり、多重発火も防げる。
 */
object MilestonePreference {

    val MILESTONES = listOf(1, 10, 50, 100, 250, 500, 1000)

    private fun lastCelebrated(context: Context): Int =
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_LAST_CELEBRATED_MILESTONE, 0)

    fun markCelebrated(context: Context, milestone: Int) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit { putInt(KEY_LAST_CELEBRATED_MILESTONE, milestone) }
    }

    /** count時点で新たに到達した(まだお祝いしていない)マイルストーンがあれば返す。 */
    fun newlyReached(context: Context, count: Int): Int? {
        val reached = MILESTONES.filter { it <= count }.maxOrNull() ?: return null
        return reached.takeIf { it > lastCelebrated(context) }
    }
}
