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

/** まだ枠に割り当てていない読み込み済み広告を、これだけ先回りして手元に置いておく。 */
private const val POOL_BUFFER = 2

/** 同時に並行して出すリクエストの上限。 */
private const val MAX_CONCURRENT_REQUESTS = 3

/**
 * 同時に枠へ割り当てておく広告の上限。これを超えたら、今見ている位置から最も遠い枠の
 * 広告を解放する(長い一覧を延々スクロールしてもメモリを使い続けないため)。
 */
private const val MAX_ASSIGNED_ADS = 16

/**
 * 読み込み済みのネイティブ広告は、およそ1時間以内に表示しないと無効になる。
 * 少し余裕を持たせたこの時間を過ぎた未割り当ての広告は捨てて読み込み直す。
 */
private const val AD_EXPIRATION_MILLIS = 50 * 60 * 1000L

/** 在庫切れ(1枚も返ってこなかった)の直後は、この時間リクエストを控える。 */
private const val NO_FILL_BACKOFF_MILLIS = 60 * 1000L

/**
 * 一覧を開いてから、ユーザーが最初に触れるまでの間に限り、画面内の枠にも広告を入れてよい
 * (ただし最大でこの時間まで)。まだ操作前なので、レイアウトのずれが誤タップにつながらない。
 * 最もよく見られる先頭の枠を、広告の読み込みが少し遅れても逃さないため。
 */
private const val INITIAL_PLACEMENT_WINDOW_MILLIS = 5 * 1000L

/** これだけ長くアプリを離れていたら、次に開いたとき全枠の広告を新しいものに入れ替える。 */
private const val SESSION_RESET_MILLIS = 30 * 60 * 1000L

/**
 * ブックマーク一覧に一定間隔で挟むネイティブ広告を管理する。
 *
 * 一覧の長さ分をまとめて読み込むのではなく、ユーザーがスクロールして次の広告枠に
 * 近づいたときに、その枠の分だけ先回りして読み込む(見られない広告を読み込まない
 * ことで表示率を高く保つ)。広告は画面外にある枠にだけ割り当て、今見えている画面の中に
 * 後から広告が割り込んでレイアウトがずれる(=誤タップを誘う)ことがないようにする。
 * 一度割り当てた枠の広告は、遠く離れてメモリ解放する場合を除いて差し替えない。
 *
 * ネイティブ広告のインスタンスは複数の枠で同時に使い回してはいけない(ポリシー)ため、
 * 枠ごとに別々の広告を割り当てる。
 */
object NativeAdManager {

    private class PooledAd(val ad: NativeAd, val loadedAt: Long)

    private val pool = ArrayDeque<PooledAd>()

    private val _assignedAds = MutableStateFlow<Map<Int, NativeAd>>(emptyMap())

    /** 枠番号(0始まり) → その枠に表示する広告。 */
    val assignedAds: StateFlow<Map<Int, NativeAd>> = _assignedAds.asStateFlow()

    private var inFlight = 0
    private var lastNoFillAt = 0L
    private var backgroundedAt = 0L

    // 直近にスクロール位置から申告された「割り当てたい枠」の状態。広告が届いた時点で
    // すぐ割り当てられるよう覚えておく。
    private var wantedSlots: List<Int> = emptyList()
    private var visibleSlots: Set<Int> = emptySet()
    private var allowVisibleUntil = 0L
    private var listShownAt = 0L
    private var focusSlot = 0

    // 画面に映っている枠には新たに広告を入れない(後から割り込んでレイアウトがずれるのを防ぐ)。
    // ただし一覧を開いた直後(allowVisibleUntilまで)は、まだ操作前なので入れてよい。
    private fun isPlaceable(slot: Int): Boolean =
        slot !in visibleSlots || System.currentTimeMillis() < allowVisibleUntil

    /** 次の広告枠に備え、未割り当ての広告をPOOL_BUFFER枚まで先に読み込んでおく。 */
    fun preload(context: Context) {
        topUp(context, pendingSlots = 0)
    }

    /** 一覧に最初に触れたときに呼ぶ。以後、画面内の枠には新たに広告を入れない。 */
    fun closeInitialPlacementWindow() {
        allowVisibleUntil = 0L
    }

    fun onAppBackgrounded() {
        backgroundedAt = System.currentTimeMillis()
    }

    /**
     * 長時間離れていた後にアプリを開いたら、全枠の広告を入れ替える。画面が描画される前
     * (プロセスがフォアグラウンドに来た時点)に行うため、見ている最中に広告が消えることはない。
     */
    fun onAppForegrounded(context: Context) {
        if (backgroundedAt != 0L && System.currentTimeMillis() - backgroundedAt >= SESSION_RESET_MILLIS) {
            reset()
        }
        preload(context)
    }

    /**
     * 一覧のスクロール位置から、広告を割り当てたい枠を申告する。
     * wanted: 先読み範囲内にある枠の番号(近い順でなくてよい)
     * visible: 今まさに画面に映っている位置にある枠の番号(ここには新たに割り当てない)
     * focus: 今見ているあたりの枠番号(メモリ解放時に、ここから遠い枠を優先して解放する)
     * listShownAt: 一覧を表示し始めた時刻。新しく表示された一覧からの申告であれば、そこから
     *   INITIAL_PLACEMENT_WINDOW_MILLISの間(最初に触れるまで)は画面内の枠にも入れてよい
     */
    fun requestSlots(context: Context, wanted: List<Int>, visible: Set<Int>, focus: Int, listShownAt: Long) {
        if (listShownAt != this.listShownAt) {
            this.listShownAt = listShownAt
            allowVisibleUntil = listShownAt + INITIAL_PLACEMENT_WINDOW_MILLIS
        }
        wantedSlots = wanted
        visibleSlots = visible
        focusSlot = focus
        assignFromPool()
        val pendingSlots = wanted.count { it !in _assignedAds.value && isPlaceable(it) }
        topUp(context, pendingSlots)
    }

    private fun assignFromPool() {
        dropExpired()
        val assigned = _assignedAds.value
        val targets = wantedSlots.filter { it !in assigned && isPlaceable(it) }.sorted()
        if (targets.isEmpty() || pool.isEmpty()) return

        val updated = assigned.toMutableMap()
        for (slot in targets) {
            val pooled = pool.removeFirstOrNull() ?: break
            updated[slot] = pooled.ad
        }
        evictFarSlots(updated)
        _assignedAds.value = updated
    }

    private fun evictFarSlots(assigned: MutableMap<Int, NativeAd>) {
        while (assigned.size > MAX_ASSIGNED_ADS) {
            val farthest = assigned.keys
                .filter { it !in visibleSlots }
                .maxByOrNull { kotlin.math.abs(it - focusSlot) } ?: return
            assigned.remove(farthest)?.destroy()
        }
    }

    private fun dropExpired() {
        val now = System.currentTimeMillis()
        while (pool.isNotEmpty() && now - pool.first().loadedAt >= AD_EXPIRATION_MILLIS) {
            pool.removeFirst().ad.destroy()
        }
    }

    private fun topUp(context: Context, pendingSlots: Int) {
        if (!ConsentManager.isAdsReady.value || AdFreeAccess.isAdFree(context)) return
        dropExpired()
        val now = System.currentTimeMillis()
        if (now - lastNoFillAt < NO_FILL_BACKOFF_MILLIS) return
        val needed = pendingSlots + POOL_BUFFER - pool.size - inFlight
        if (needed <= 0) return
        repeat(needed.coerceAtMost(MAX_CONCURRENT_REQUESTS - inFlight)) { loadOne(context) }
    }

    // AdLoader.loadAds(複数枚)は返ってくる枚数とコールバック回数が一致しないことがあり
    // 処理中の件数を正しく数えられないため、1リクエスト1枚(必ずどちらか1回だけ
    // コールバックが来る)を必要な数だけ並行して出す。
    private fun loadOne(context: Context) {
        inFlight++
        val appContext = context.applicationContext

        fun onCallback(success: Boolean) {
            inFlight--
            if (!success) {
                lastNoFillAt = System.currentTimeMillis()
            } else {
                // 届いている間にさらにスクロールが進んでいれば、続けて不足分を読み込む。
                val pendingSlots = wantedSlots.count { it !in _assignedAds.value && isPlaceable(it) }
                topUp(appContext, pendingSlots)
            }
        }

        val adLoader = AdLoader.Builder(appContext, NATIVE_AD_UNIT_ID)
            .forNativeAd { ad ->
                if (AdFreeAccess.isAdFree(appContext)) {
                    ad.destroy()
                } else {
                    pool.addLast(PooledAd(ad, System.currentTimeMillis()))
                    assignFromPool()
                }
                onCallback(success = true)
            }
            .withAdListener(
                object : AdListener() {
                    override fun onAdClicked() = AppOpenAdManager.notifyLeavingByOwnAction()

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        onCallback(success = false)
                    }
                },
            )
            .withNativeAdOptions(
                NativeAdOptions.Builder()
                    // カード・横長行のどちらの枠にも収まりやすい横長の素材を優先する。
                    .setMediaAspectRatio(NativeAdOptions.NATIVE_MEDIA_ASPECT_RATIO_LANDSCAPE)
                    .build(),
            )
            .build()
        adLoader.loadAd(AdRequest.Builder().build())
    }

    private fun reset() {
        _assignedAds.value.values.forEach { it.destroy() }
        _assignedAds.value = emptyMap()
        while (pool.isNotEmpty()) pool.removeFirst().ad.destroy()
    }
}
