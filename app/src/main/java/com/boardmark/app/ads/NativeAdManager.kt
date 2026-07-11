package com.boardmark.app.ads

import android.content.Context
import com.boardmark.app.BuildConfig
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// デバッグビルドはGoogle公式のテスト用ID、リリースビルドは本番IDを使う。
private val NATIVE_AD_UNIT_ID = if (BuildConfig.DEBUG) {
    "ca-app-pub-3940256099942544/2247696110"
} else {
    "ca-app-pub-3334691626809528/2253744347"
}

/** 一覧に何件かおきに複数枚を挟み込むため、まとめて保持しておく広告枠の数。 */
const val NATIVE_AD_POOL_SIZE = 3

/**
 * 前回の読み込みから最低これだけ間隔を空ける。ネイティブ広告に自動更新の仕組みは
 * なく最小表示時間の縛りも公式にはないが、画面を開いたまま裏で頻繁に差し替えたり
 * オフスクリーンで更新したりするのは「無効なトラフィック」としてポリシー違反に
 * なりうるため、更新は常に「アプリを開き直す」という自然な区切りでのみ行い、
 * かつ短時間の連続起動では読み込み直さないようにする。
 */
private const val RELOAD_COOLDOWN_MILLIS = 5 * 60 * 1000L

/**
 * ブックマーク一覧のグリッドに自然に混ぜ込むネイティブ広告を、繰り返し掲載できる
 * よう複数枚まとめて読み込み・保持する。ネイティブ広告のインスタンスは同時に
 * 複数箇所へ使い回してはいけない(ポリシー違反)ため、掲載枠の数だけ別々に読み込み、
 * 差し替え時は古い広告をdestroy()してから新しい広告に置き換える。
 */
object NativeAdManager {

    private val _nativeAds = MutableStateFlow<List<NativeAd>>(emptyList())
    val nativeAds: StateFlow<List<NativeAd>> = _nativeAds.asStateFlow()

    private var isLoading = false
    private var lastLoadedAt = 0L

    fun preload(context: Context) {
        if (AdFreeAccess.isAdFree(context) || isLoading) return
        val now = System.currentTimeMillis()
        if (_nativeAds.value.isNotEmpty() && now - lastLoadedAt < RELOAD_COOLDOWN_MILLIS) return
        isLoading = true
        lastLoadedAt = now

        // 新しいセットの読み込みを開始する時点で、前回表示していた分は破棄する
        // (表示に使われなくなった広告はNativeAd.destroy()で解放するのがSDKの決まり)。
        val staleAds = _nativeAds.value
        _nativeAds.value = emptyList()
        staleAds.forEach { it.destroy() }

        val loadedSoFar = mutableListOf<NativeAd>()
        val adLoader = AdLoader.Builder(context.applicationContext, NATIVE_AD_UNIT_ID)
            .forNativeAd { ad ->
                loadedSoFar.add(ad)
                _nativeAds.value = loadedSoFar.toList()
                if (loadedSoFar.size >= NATIVE_AD_POOL_SIZE) isLoading = false
            }
            .withAdListener(
                object : AdListener() {
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        // 一部だけ埋まらなくても、読み込めた分だけで表示を続ける。
                        isLoading = false
                    }
                },
            )
            .withNativeAdOptions(NativeAdOptions.Builder().build())
            .build()
        adLoader.loadAds(AdRequest.Builder().build(), NATIVE_AD_POOL_SIZE)
    }
}
