package com.boardmark.app.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * LocalHapticFeedbackを操作の意味ごとに名前付けしてラップする。呼び出し側で毎回
 * HapticFeedbackTypeの選定基準を考えずに済むようにするための薄いヘルパー。
 */
class Haptics(private val hapticFeedback: HapticFeedback) {
    /** 選択トグル(選択/解除)。 */
    fun selectionToggle() = hapticFeedback.performHapticFeedback(HapticFeedbackType.ToggleOn)

    /** 長押しで選択モード/フォルダ操作メニューを開始。 */
    fun longPress() = hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)

    /** 並べ替えドラッグ中、挿入先候補が切り替わった瞬間。 */
    fun dragOverNewTarget() = hapticFeedback.performHapticFeedback(HapticFeedbackType.SegmentTick)

    /** 並べ替えドラッグの確定(指を離した)。 */
    fun reorderCommit() = hapticFeedback.performHapticFeedback(HapticFeedbackType.GestureEnd)

    /** 削除確定など、破壊的操作の確定。 */
    fun confirm() = hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
}

@Composable
fun rememberHaptics(): Haptics {
    val hapticFeedback = LocalHapticFeedback.current
    return remember(hapticFeedback) { Haptics(hapticFeedback) }
}
