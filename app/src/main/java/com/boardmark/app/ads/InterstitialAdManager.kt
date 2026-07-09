package com.boardmark.app.ads

import android.app.Activity
import android.content.Context
import androidx.core.content.edit
import com.boardmark.app.BuildConfig
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import kotlin.random.Random

// デバッグビルドはGoogle公式のテスト用ID、リリースビルドは本番IDを使う。
private val INTERSTITIAL_AD_UNIT_ID = if (BuildConfig.DEBUG) {
    "ca-app-pub-3940256099942544/1033173712"
} else {
    "ca-app-pub-3334691626809528/9008469354"
}

private const val PREFS_NAME = "boardmark_prefs"
private const val KEY_LAST_SHOWN_AT = "interstitial_last_shown_at"

/** 前回表示から最低これだけ間隔を空ける(トリガー種別に関わらず共通のクールダウン)。 */
private const val COOLDOWN_MILLIS = 3 * 60 * 1000L

/** アプリ起動時のみ、この分の1の確率でしか表示しない(5回に1回程度)。 */
private const val APP_OPEN_FREQUENCY = 5

/**
 * サムネイル更新の「重み」の母数。1件更新なら10回に1回、itemCount件分まとめて更新した
 * 場合はitemCount/この値の確率で表示する(itemCountがこの値以上なら実質確定表示になる)。
 */
private const val THUMBNAIL_UPDATE_FREQUENCY = 10

/** インポートは実際の取得件数によらず、サムネイル更新5件分に相当する重みとして扱う。 */
private const val IMPORT_EQUIVALENT_ITEM_COUNT = 5

/**
 * インタースティシャル(全画面)広告を、自然な区切り(アプリ起動・インポート・エクスポート・
 * サムネイル更新)でのみ表示する。ブックマークを開く操作など、ユーザーの主要な操作の
 * 妨げにはならない箇所でのみ呼び出すこと。
 */
object InterstitialAdManager {

    enum class Trigger { APP_OPEN, IMPORT, EXPORT, THUMBNAIL_UPDATE }

    private var loadedAd: InterstitialAd? = null
    private var isLoading = false

    fun preload(context: Context) {
        if (AdFreeAccess.isAdFree(context) || isLoading || loadedAd != null) return
        isLoading = true
        InterstitialAd.load(
            context.applicationContext,
            INTERSTITIAL_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    isLoading = false
                    loadedAd = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    isLoading = false
                    loadedAd = null
                }
            },
        )
    }

    /**
     * クールダウンと、トリガーごとの確率判定を満たしていれば表示する。
     * itemCountはTHUMBNAIL_UPDATEでのみ意味を持ち、まとめて更新した件数を渡す
     * (例: 複数選択で10件更新した場合はitemCount=10。10件以上ならほぼ確定表示になる)。
     * EXPORTは確率判定なしの必須表示、IMPORTは実際の件数によらず常に
     * THUMBNAIL_UPDATEの5件分相当の重みで判定する。
     * 広告の読み込みが間に合っていない場合は何もしない(ユーザーの操作を待たせない)。
     */
    fun maybeShow(activity: Activity, trigger: Trigger, itemCount: Int = 1) {
        if (AdFreeAccess.isAdFree(activity)) return
        val prefs = activity.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        if (now - prefs.getLong(KEY_LAST_SHOWN_AT, 0L) < COOLDOWN_MILLIS) return
        when (trigger) {
            Trigger.APP_OPEN -> if (Random.nextInt(APP_OPEN_FREQUENCY) != 0) return
            Trigger.THUMBNAIL_UPDATE -> if (Random.nextInt(THUMBNAIL_UPDATE_FREQUENCY) >= itemCount) return
            Trigger.IMPORT -> if (Random.nextInt(THUMBNAIL_UPDATE_FREQUENCY) >= IMPORT_EQUIVALENT_ITEM_COUNT) return
            Trigger.EXPORT -> Unit // 必須表示: 確率判定なし
        }

        val ad = loadedAd ?: return
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                loadedAd = null
                prefs.edit { putLong(KEY_LAST_SHOWN_AT, System.currentTimeMillis()) }
                preload(activity)
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                loadedAd = null
                preload(activity)
            }
        }
        ad.show(activity)
    }
}
