package com.boardmark.app.ads

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val PREFS_NAME = "boardmark_prefs"
private const val KEY_AD_FREE_PURCHASED = "ad_free_purchased"
private const val KEY_REWARDED_AD_FREE_UNTIL = "rewarded_ad_free_until"

/**
 * 広告非表示の状態を保持する。理由は2つあり、どちらか一方でも有効なら広告を出さない。
 * - 買い切りの購入(BillingManagerが購入確認時に書き込む)
 * - リワード広告の視聴による期間限定の非表示(RewardedAdManagerが書き込む)
 *
 * 各広告マネージャ/BannerAdはDIなしでここを直接参照して広告表示の可否を判定する。
 * 画面側はisAdFreeFlowを購読し、状態が変わったら(購入・リワード獲得・期限切れ)
 * その場で広告欄を出し入れする。
 */
object AdFreeAccess {

    private val _isAdFreeFlow = MutableStateFlow(false)
    val isAdFreeFlow: StateFlow<Boolean> = _isAdFreeFlow.asStateFlow()

    private val mainHandler = Handler(Looper.getMainLooper())
    private var expiryRunnable: Runnable? = null

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isAdFree(context: Context): Boolean = isPurchased(context) || isRewardActive(context)

    fun isPurchased(context: Context): Boolean = prefs(context).getBoolean(KEY_AD_FREE_PURCHASED, false)

    fun rewardedAdFreeUntil(context: Context): Long = prefs(context).getLong(KEY_REWARDED_AD_FREE_UNTIL, 0L)

    private fun isRewardActive(context: Context): Boolean =
        System.currentTimeMillis() < rewardedAdFreeUntil(context)

    fun setAdFree(context: Context, adFree: Boolean) {
        prefs(context).edit { putBoolean(KEY_AD_FREE_PURCHASED, adFree) }
        refresh(context)
    }

    /** リワード広告の特典として、今からdurationMillisの間すべての広告を非表示にする。 */
    fun grantRewardedAdFree(context: Context, durationMillis: Long) {
        prefs(context).edit { putLong(KEY_REWARDED_AD_FREE_UNTIL, System.currentTimeMillis() + durationMillis) }
        refresh(context)
    }

    /**
     * 保存済みの状態からisAdFreeFlowを再計算し、リワードの期限切れに合わせて
     * 自動で再計算されるようタイマーを張り直す。起動時(Application.onCreate)と
     * 状態を書き換えたときに呼ぶ。
     */
    fun refresh(context: Context) {
        val appContext = context.applicationContext
        val wasAdFree = _isAdFreeFlow.value
        val adFree = isAdFree(appContext)
        _isAdFreeFlow.value = adFree

        expiryRunnable?.let { mainHandler.removeCallbacks(it) }
        expiryRunnable = null
        if (!isPurchased(appContext) && isRewardActive(appContext)) {
            val runnable = Runnable { refresh(appContext) }
            expiryRunnable = runnable
            mainHandler.postDelayed(runnable, rewardedAdFreeUntil(appContext) - System.currentTimeMillis())
        }

        // 非表示期間が終わった瞬間から広告を出せるよう、在庫を読み込み直しておく。
        if (wasAdFree && !adFree) ConsentManager.preloadAll(appContext)
    }
}
