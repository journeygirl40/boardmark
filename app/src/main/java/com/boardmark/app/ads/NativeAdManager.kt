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

/**
 * AdLoader.loadAds(request, numAds)のnumAdsはAdMob側の仕様で最大5に制限されており、
 * それを超える数を渡しても実際には5件までしか返らない。一覧が長く5件を超える広告枠が
 * 必要な場合は、このバッチサイズ単位で複数回リクエストを重ねてpendingTargetまで蓄積する。
 */
private const val ADMOB_MAX_ADS_PER_REQUEST = 5

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
 * (Unity LevelPlay経由のネイティブ広告在庫は廃止し、AdMob単独で扱う。)
 *
 * 保持枚数に固定の上限は設けない。一覧が長ければ長いほど、画面側(BookmarkListScreen)
 * が挿入に必要な枚数を都度ensureCapacityで申告し、足りない分だけ追加で読み込む。
 */
object NativeAdManager {

    private val _nativeAds = MutableStateFlow<List<NativeAd>>(emptyList())
    val nativeAds: StateFlow<List<NativeAd>> = _nativeAds.asStateFlow()

    private var isLoading = false
    private var lastFullReloadAt = 0L

    // 現在読み込み中(または読み込み済み)であることを目指している総枚数。
    // ensureCapacityで一覧側から伸ばされ、実際に届くまでバッチを重ねて追いかける。
    private var pendingTarget = 0

    /**
     * アプリを開き直すたびに呼ばれる、全体リフレッシュの入口。クールダウン以内なら
     * 何もしない。クールダウンを過ぎていれば、前回分をdestroy()してから最初の
     * バッチ(AdMob1リクエストの上限である5件)を読み込み直す。一覧が5件を超える
     * 広告枠を必要とする場合は、後続でensureCapacityが追加分を読み込む。
     */
    fun preload(context: Context) {
        if (AdFreeAccess.isAdFree(context)) return
        val now = System.currentTimeMillis()
        if (_nativeAds.value.isNotEmpty() && now - lastFullReloadAt < RELOAD_COOLDOWN_MILLIS) return
        lastFullReloadAt = now

        // 新しいセットの読み込みを開始する時点で、前回表示していた分は破棄する
        // (表示に使われなくなった広告はNativeAd.destroy()で解放するのがSDKの決まり)。
        val staleAds = _nativeAds.value
        _nativeAds.value = emptyList()
        staleAds.forEach { it.destroy() }

        pendingTarget = ADMOB_MAX_ADS_PER_REQUEST
        if (!isLoading) requestNextBatch(context)
    }

    /**
     * 一覧の表示に必要な広告枚数を申告する。現在の保持数・目標数より多ければ、
     * 目標を引き上げて不足分の読み込みを(必要なら)開始する。既存の広告は破棄せず、
     * 追加のみ行う(preloadのような全体リセットはしない)。
     */
    fun ensureCapacity(context: Context, desiredCount: Int) {
        if (AdFreeAccess.isAdFree(context)) return
        if (desiredCount <= _nativeAds.value.size || desiredCount <= pendingTarget) return
        pendingTarget = desiredCount
        if (!isLoading) requestNextBatch(context)
    }

    private fun requestNextBatch(context: Context) {
        val remaining = pendingTarget - _nativeAds.value.size
        if (remaining <= 0) {
            isLoading = false
            return
        }
        isLoading = true
        val batchSize = remaining.coerceAtMost(ADMOB_MAX_ADS_PER_REQUEST)
        val loadedSoFar = _nativeAds.value.toMutableList()
        var callbacksReceived = 0
        var successCount = 0

        fun onCallback() {
            callbacksReceived++
            if (callbacksReceived >= batchSize) {
                // 1件も取れなかった場合は在庫切れとみなし、これ以上バッチを重ねて
                // リトライしない(次のpreloadまで待つ)。1件でも取れていれば、
                // まだ目標に届いていない限り次のバッチを続けて要求する。
                isLoading = false
                if (successCount > 0 && _nativeAds.value.size < pendingTarget) {
                    requestNextBatch(context)
                }
            }
        }

        val adLoader = AdLoader.Builder(context.applicationContext, NATIVE_AD_UNIT_ID)
            .forNativeAd { ad ->
                loadedSoFar.add(ad)
                _nativeAds.value = loadedSoFar.toList()
                successCount++
                onCallback()
            }
            .withAdListener(
                object : AdListener() {
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        // 一部だけ埋まらなくても、読み込めた分だけで表示を続ける。
                        onCallback()
                    }
                },
            )
            .withNativeAdOptions(NativeAdOptions.Builder().build())
            .build()
        adLoader.loadAds(AdRequest.Builder().build(), batchSize)
    }
}
