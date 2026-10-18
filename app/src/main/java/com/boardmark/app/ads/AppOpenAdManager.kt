package com.boardmark.app.ads

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.core.content.edit
import com.boardmark.app.BuildConfig
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import java.lang.ref.WeakReference

// デバッグビルドはGoogle公式のテスト用ID、リリースビルドは本番IDを使う。
private val APP_OPEN_AD_UNIT_ID = if (BuildConfig.DEBUG) {
    "ca-app-pub-3940256099942544/9257395921"
} else {
    "ca-app-pub-3334691626809528/1272331945"
}

private const val PREFS_NAME = "boardmark_prefs"
private const val KEY_COLD_START_COUNT = "app_open_cold_start_count"
private const val KEY_LAST_SHOWN_AT = "app_open_last_shown_at"

/**
 * アプリ起動広告は、Google側の仕様でロードからおよそ4時間で期限切れになる。
 * 少し余裕を持たせたこの時間を過ぎたら古い広告を捨てて必ず読み込み直す。
 */
private const val AD_EXPIRATION_MILLIS = 3 * 60 * 60 * 1000L

/**
 * 新規ユーザーには、まずアプリそのものを体験してもらうため起動広告を出さない
 * (Google推奨)。この回数のコールドスタートまでは表示しない。
 */
private const val NEW_USER_GRACE_COLD_STARTS = 2

/** コールドスタート時、起動画面(スプラッシュ)を出したまま広告の読み込みを待つ上限。 */
private const val COLD_START_MAX_WAIT_MILLIS = 3 * 1000L

/** アプリ起動広告どうしの最低間隔。 */
private const val MIN_INTERVAL_MILLIS = 30 * 60 * 1000L

/**
 * アプリ自身の操作(リンクを開く・ファイル選択・広告クリック等)の直後、この時間内に
 * バックグラウンドへ移った場合は「アプリ自身が外へ送り出した」とみなす。
 */
private const val OWN_LEAVE_WINDOW_MILLIS = 10 * 1000L

/**
 * アプリ自身が外へ送り出した場合、戻ってきても原則アプリ起動広告は出さない。ただし
 * これだけ長く離れていた場合は、改めてアプリを開き直したものとして扱う。
 */
private const val OWN_LEAVE_MIN_BACKGROUND_MILLIS = 30 * 60 * 1000L

/** ホームボタン等でユーザー自身が離れた場合も、この時間未満のすぐの復帰では出さない。 */
private const val MIN_BACKGROUND_MILLIS = 3 * 60 * 1000L

/** InterstitialAdManagerのSHOW_WATCHDOG_MILLISと同じ理由の保護。 */
private const val SHOW_WATCHDOG_MILLIS = 10 * 1000L

/** InterstitialAdManagerのSHOW_POST_START_WATCHDOG_MILLISと同じ理由の保護。 */
private const val SHOW_POST_START_WATCHDOG_MILLIS = 45 * 1000L

/**
 * AdMobのアプリ起動広告(App Open Ad)。Googleのベストプラクティスに沿って、
 * 次の2つの場面でだけ表示する。
 * - コールドスタート: 起動画面(スプラッシュ)を最大COLD_START_MAX_WAIT_MILLIS出したまま
 *   読み込みを待ち、間に合えば起動画面から直接広告に切り替える。間に合わなければ
 *   そのまま通常画面に進み、読み込んだ広告は次の復帰時に回す。
 * - ウォームスタート(バックグラウンドからの復帰): 読み込み済みの広告があるときだけ、
 *   画面が表示された瞬間に出す。後から読み込めても、操作中の画面に割り込ませはしない。
 *
 * このアプリはリンクを開いてすぐ戻ってくる使い方が多く、そのたびに起動広告を出すと
 * 意図しない表示・誤タップとしてAdMobに無効なトラフィック扱いされるため、
 * 「改めてアプリを開いた」と言える復帰に限定する(isEligibleWarmStart参照)。
 */
object AppOpenAdManager {

    private enum class Pending { NONE, COLD, WARM }

    private var loadedAd: AppOpenAd? = null
    private var loadedAt: Long = 0L
    private var isLoading = false

    private var pending = Pending.NONE
    private var coldStartDeadline = 0L
    private var resumedMainActivity: WeakReference<Activity>? = null

    private var lastOwnLeaveAt = 0L
    private var backgroundedAt = 0L
    private var leftByOwnAction = false

    private val mainHandler = Handler(Looper.getMainLooper())

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun isLoadedAdStale(): Boolean =
        loadedAd != null && System.currentTimeMillis() - loadedAt >= AD_EXPIRATION_MILLIS

    fun preload(context: Context) {
        if (isLoadedAdStale()) loadedAd = null
        if (!ConsentManager.isAdsReady.value || AdFreeAccess.isAdFree(context)) return
        if (isLoading || loadedAd != null) return
        isLoading = true
        val appContext = context.applicationContext
        AppOpenAd.load(
            appContext,
            APP_OPEN_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    isLoading = false
                    loadedAd = ad
                    loadedAt = System.currentTimeMillis()
                    // コールドスタートで起動画面を出したまま待っている場合は、ここで表示する。
                    if (pending == Pending.COLD) tryShow(appContext)
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    isLoading = false
                    loadedAd = null
                }
            },
        )
    }

    /** アプリ自身が外部のActivity/ブラウザを開く直前(広告クリックを含む)に呼ぶ。 */
    fun notifyLeavingByOwnAction() {
        lastOwnLeaveAt = System.currentTimeMillis()
    }

    /** プロセスがバックグラウンドに移ったとき(ProcessLifecycleOwnerのonStop)に呼ぶ。 */
    fun onAppBackgrounded() {
        val now = System.currentTimeMillis()
        backgroundedAt = now
        leftByOwnAction = now - lastOwnLeaveAt < OWN_LEAVE_WINDOW_MILLIS
        pending = Pending.NONE
    }

    /** プロセスがフォアグラウンドに来たとき(ProcessLifecycleOwnerのonStart)に呼ぶ。 */
    fun onAppForegrounded(context: Context) {
        val isColdStart = backgroundedAt == 0L
        val coldStarts = if (isColdStart) {
            (prefs(context).getInt(KEY_COLD_START_COUNT, 0) + 1).also { count ->
                prefs(context).edit { putInt(KEY_COLD_START_COUNT, count) }
            }
        } else {
            prefs(context).getInt(KEY_COLD_START_COUNT, 0)
        }

        val eligible = !AdFreeAccess.isAdFree(context) &&
            coldStarts > NEW_USER_GRACE_COLD_STARTS &&
            System.currentTimeMillis() - prefs(context).getLong(KEY_LAST_SHOWN_AT, 0L) >= MIN_INTERVAL_MILLIS &&
            FullScreenAdGate.canShow(context) &&
            (isColdStart || isEligibleWarmStart())
        pending = when {
            !eligible -> Pending.NONE
            isColdStart -> Pending.COLD
            else -> Pending.WARM
        }
        if (pending == Pending.COLD) coldStartDeadline = System.currentTimeMillis() + COLD_START_MAX_WAIT_MILLIS
        preload(context)
    }

    private fun isEligibleWarmStart(): Boolean {
        val away = System.currentTimeMillis() - backgroundedAt
        return if (leftByOwnAction) away >= OWN_LEAVE_MIN_BACKGROUND_MILLIS else away >= MIN_BACKGROUND_MILLIS
    }

    /** MainActivityの起動画面(スプラッシュ)を、広告の読み込み待ちのため出し続けるべきか。 */
    fun shouldHoldSplash(): Boolean =
        pending == Pending.COLD && System.currentTimeMillis() < coldStartDeadline

    /** 同意フォームを表示する起動など、起動画面で広告を待つべきでない場合に呼ぶ。 */
    fun cancelColdStartWait() {
        if (pending == Pending.COLD) pending = Pending.NONE
    }

    fun onMainActivityResumed(activity: Activity) {
        resumedMainActivity = WeakReference(activity)
        tryShow(activity)
        // ウォームスタートは「画面が出た瞬間に読み込み済みなら出す」だけで、待たない。
        if (pending == Pending.WARM) pending = Pending.NONE
    }

    fun onMainActivityPaused() {
        resumedMainActivity = null
    }

    private fun tryShow(context: Context) {
        if (pending == Pending.NONE) return
        if (pending == Pending.COLD && System.currentTimeMillis() >= coldStartDeadline) {
            pending = Pending.NONE
            return
        }
        val activity = resumedMainActivity?.get() ?: return
        if (activity.isFinishing || activity.isDestroyed) return
        if (AdFreeAccess.isAdFree(context) || !FullScreenAdGate.canShow(context)) {
            pending = Pending.NONE
            return
        }
        if (isLoadedAdStale()) loadedAd = null
        val ad = loadedAd ?: return
        loadedAd = null
        pending = Pending.NONE
        show(activity, ad)
    }

    private fun show(activity: Activity, ad: AppOpenAd) {
        FullScreenAdGate.isShowing = true

        // resolved済みかどうかをウォッチドッグとコールバックの両方から参照するため、
        // 呼び出しごとに新しいフラグ/Runnableを作る(前回分の状態を引きずらないように)。
        var resolved = false
        fun resolve() {
            resolved = true
            FullScreenAdGate.isShowing = false
            preload(activity)
        }
        val watchdog = Runnable { if (!resolved) resolve() }
        val postStartWatchdog = Runnable { if (!resolved) resolve() }
        mainHandler.postDelayed(watchdog, SHOW_WATCHDOG_MILLIS)

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                mainHandler.removeCallbacks(watchdog)
                mainHandler.postDelayed(postStartWatchdog, SHOW_POST_START_WATCHDOG_MILLIS)
            }

            override fun onAdClicked() = notifyLeavingByOwnAction()

            override fun onAdDismissedFullScreenContent() {
                if (resolved) return
                mainHandler.removeCallbacks(watchdog)
                mainHandler.removeCallbacks(postStartWatchdog)
                prefs(activity).edit { putLong(KEY_LAST_SHOWN_AT, System.currentTimeMillis()) }
                FullScreenAdGate.markShown(activity)
                resolve()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                if (resolved) return
                mainHandler.removeCallbacks(watchdog)
                mainHandler.removeCallbacks(postStartWatchdog)
                resolve()
            }
        }
        ad.show(activity)
    }
}
