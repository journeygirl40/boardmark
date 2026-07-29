package com.boardmark.app.ads

import android.content.Context
import com.ironsource.mediationsdk.adunit.adapter.utility.AdInfo
import com.ironsource.mediationsdk.ads.nativead.LevelPlayNativeAd
import com.ironsource.mediationsdk.ads.nativead.LevelPlayNativeAdListener
import com.ironsource.mediationsdk.logger.IronSourceError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Unity LevelPlay Dashboardで作成した"Native_Android"というAd Unit(Native)のAd Unit ID。
 * LevelPlayNativeAd.BuilderにAd Unit IDを直接渡すAPIは無くwithPlacementName(String)のみ
 * のため、バナー(LevelPlayBannerAdView(context, "adUnitId"))と同様にAd Unit IDをそのまま渡す。
 */
const val UNITY_NATIVE_AD_UNIT_ID = "pz5wi4daiig0bo79"

/** 前回の読み込みから最低これだけ間隔を空ける(NativeAdManager.ktのRELOAD_COOLDOWN_MILLISと同じ理由)。 */
private const val UNITY_NATIVE_RELOAD_COOLDOWN_MILLIS = 5 * 60 * 1000L

/**
 * Unity LevelPlay SDK経由でネイティブ広告を読み込み・保持する。banner/interstitialと違い
 * ネイティブ広告は1画面に1枠という制約が無く一覧内の複数枠に混ぜて表示できるため、
 * AdMob(NativeAdManager)の「フォールバック」ではなく、常に並行して読み込む独立した
 * 広告在庫として扱う(表示可能な広告の総数・広告収益を最大化する狙い)。AdMob側と
 * 同じ理由(ネイティブ広告のインスタンスは同時に複数箇所へ使い回してはいけない)で、
 * 掲載枠の数だけ別々のLevelPlayNativeAdインスタンスを保持する。
 */
object UnityNativeAdManager {

    private val _nativeAds = MutableStateFlow<List<LevelPlayNativeAd>>(emptyList())
    val nativeAds: StateFlow<List<LevelPlayNativeAd>> = _nativeAds.asStateFlow()

    private var isLoading = false
    private var lastLoadedAt = 0L
    private var pendingResults = 0

    fun preload(context: Context) {
        if (AdFreeAccess.isAdFree(context) || isLoading || !UnityLevelPlayManager.isReady()) return
        val now = System.currentTimeMillis()
        if (_nativeAds.value.isNotEmpty() && now - lastLoadedAt < UNITY_NATIVE_RELOAD_COOLDOWN_MILLIS) return
        isLoading = true
        lastLoadedAt = now
        pendingResults = NATIVE_AD_POOL_SIZE

        val staleAds = _nativeAds.value
        _nativeAds.value = emptyList()
        staleAds.forEach { it.destroyAd() }

        val loadedSoFar = mutableListOf<LevelPlayNativeAd>()
        repeat(NATIVE_AD_POOL_SIZE) {
            val ad = LevelPlayNativeAd.Builder()
                .withPlacementName(UNITY_NATIVE_AD_UNIT_ID)
                .withListener(
                    object : LevelPlayNativeAdListener {
                        override fun onAdLoaded(nativeAd: LevelPlayNativeAd?, adInfo: AdInfo?) {
                            nativeAd ?: return
                            loadedSoFar.add(nativeAd)
                            _nativeAds.value = loadedSoFar.toList()
                            pendingResults--
                            if (pendingResults <= 0) isLoading = false
                        }

                        override fun onAdLoadFailed(nativeAd: LevelPlayNativeAd?, error: IronSourceError?) {
                            pendingResults--
                            if (pendingResults <= 0) isLoading = false
                        }

                        override fun onAdClicked(nativeAd: LevelPlayNativeAd?, adInfo: AdInfo?) = Unit
                        override fun onAdImpression(nativeAd: LevelPlayNativeAd?, adInfo: AdInfo?) = Unit
                    },
                )
                .build()
            ad.loadAd()
        }
    }
}
