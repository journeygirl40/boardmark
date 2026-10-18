package com.boardmark.app.ads

import android.app.Activity
import android.content.Context
import com.boardmark.app.BuildConfig
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// デバッグビルドはGoogle公式のテスト用ID、リリースビルドは本番IDを使う。
// IDが空の場合は、リワード広告の導線自体を出さない。
private val REWARDED_AD_UNIT_ID = if (BuildConfig.DEBUG) {
    "ca-app-pub-3940256099942544/5224354917"
} else {
    "ca-app-pub-3334691626809528/5737445707"
}

/** リワード広告を最後まで視聴した特典として、広告を非表示にする時間。 */
const val REWARDED_AD_FREE_DURATION_MILLIS = 24 * 60 * 60 * 1000L

/** InterstitialAdManagerのAD_EXPIRATION_MILLISと同じ理由で、期限前に読み込み直す。 */
private const val AD_EXPIRATION_MILLIS = 50 * 60 * 1000L

/**
 * 「動画広告を見ると24時間広告が非表示になる」リワード広告。ユーザーが設定画面で
 * 特典内容を確認したうえで自分から選んだときだけ表示する(勝手に出すことはない)。
 */
object RewardedAdManager {

    private var loadedAd: RewardedAd? = null
    private var loadedAt: Long = 0L
    private var isLoading = false

    private val _isAdLoaded = MutableStateFlow(false)
    val isAdLoaded: StateFlow<Boolean> = _isAdLoaded.asStateFlow()

    val isConfigured: Boolean get() = REWARDED_AD_UNIT_ID.isNotEmpty()

    /** 購入済み・特典期間中でなければ、リワード広告で広告を非表示にできる。 */
    fun isOfferAvailable(context: Context): Boolean = isConfigured && !AdFreeAccess.isAdFree(context)

    private fun setLoadedAd(ad: RewardedAd?) {
        loadedAd = ad
        _isAdLoaded.value = ad != null
    }

    fun preload(context: Context) {
        if (loadedAd != null && System.currentTimeMillis() - loadedAt >= AD_EXPIRATION_MILLIS) setLoadedAd(null)
        if (!ConsentManager.isAdsReady.value || !isOfferAvailable(context)) return
        if (isLoading || loadedAd != null) return
        isLoading = true
        RewardedAd.load(
            context.applicationContext,
            REWARDED_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    isLoading = false
                    loadedAt = System.currentTimeMillis()
                    setLoadedAd(ad)
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    isLoading = false
                    setLoadedAd(null)
                }
            },
        )
    }

    /**
     * 読み込み済みのリワード広告を表示する。最後まで視聴されたら特典(広告の非表示)を
     * 付与し、onRewardedを呼ぶ。読み込みが済んでいなければfalseを返す。
     */
    fun show(activity: Activity, onRewarded: () -> Unit): Boolean {
        val ad = loadedAd ?: run {
            preload(activity)
            return false
        }
        setLoadedAd(null)
        FullScreenAdGate.isShowing = true
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdClicked() = AppOpenAdManager.notifyLeavingByOwnAction()

            override fun onAdDismissedFullScreenContent() {
                FullScreenAdGate.isShowing = false
                preload(activity)
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                FullScreenAdGate.isShowing = false
                preload(activity)
            }
        }
        ad.show(activity) {
            AdFreeAccess.grantRewardedAdFree(activity, REWARDED_AD_FREE_DURATION_MILLIS)
            onRewarded()
        }
        return true
    }
}
