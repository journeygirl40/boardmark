package com.boardmark.app.ads

import android.content.Context
import androidx.core.content.edit

private const val PREFS_NAME = "boardmark_prefs"
private const val KEY_AD_FREE_PURCHASED = "ad_free_purchased"

/**
 * 広告非表示(買い切り)の購入状態を保持する。BillingManagerが購入確認時に書き込み、
 * InterstitialAdManager/BannerAd/BoardmarkApplicationはDIなしでここを直接参照して
 * 広告表示の可否を判定する。
 */
object AdFreeAccess {

    fun isAdFree(context: Context): Boolean =
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_AD_FREE_PURCHASED, false)

    fun setAdFree(context: Context, adFree: Boolean) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit { putBoolean(KEY_AD_FREE_PURCHASED, adFree) }
    }
}
