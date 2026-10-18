package com.boardmark.app.ads

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
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

/**
 * サムネイル更新の「重み」の母数。1件更新なら10回に1回、itemCount件分まとめて更新した
 * 場合はitemCount/この値の確率で表示する(itemCountがこの値以上なら実質確定表示になる)。
 */
private const val THUMBNAIL_UPDATE_FREQUENCY = 10

/** インポートは実際の取得件数によらず、サムネイル更新5件分に相当する重みとして扱う。 */
private const val IMPORT_EQUIVALENT_ITEM_COUNT = 5

/**
 * 読み込み済みのインタースティシャル広告は、Google側の仕様でロードからおよそ1時間で
 * 期限切れになりshow()しても表示されなくなる。表示条件(クールダウン・確率)を満たさない
 * まま1時間近く経つと期限切れの広告がloadedAdに残り続け、preload()の「既にあれば何もしない」
 * 判定に引っかかって以後ずっと新しい広告を読み込まなくなるため、少し余裕を持たせた
 * この時間を過ぎたら古い広告を捨てて必ず読み込み直す。
 */
private const val AD_EXPIRATION_MILLIS = 50 * 60 * 1000L

/**
 * InterstitialAd.show()呼び出し後、この時間内にFullScreenContentCallbackのいずれの
 * コールバック(表示成功/失敗/終了)も来ない場合の保護。アプリ復帰直後のウィンドウ遷移と
 * ごく稀に競合し黒画面のまま操作不能になることがあるため、強制的に状態を戻す。
 */
private const val SHOW_WATCHDOG_MILLIS = 10 * 1000L

/** 描画開始の通知後も、終了/失敗が一切来ない場合の保護(動画広告の再生分の猶予を持たせる)。 */
private const val SHOW_POST_START_WATCHDOG_MILLIS = 45 * 1000L

/**
 * インタースティシャル(全画面)広告を、ユーザーの作業がひと区切りついた自然な切れ目
 * (インポート・エクスポート・サムネイル更新の完了直後)でのみ表示する。
 * アプリ起動時はアプリ起動広告(AppOpenAdManager)の担当で、インタースティシャルは
 * 出さない(起動時のインタースティシャルはAdMobのポリシーで非推奨のため)。
 * 他のネットワーク(Unity Ads等)の在庫は、AdMobメディエーションの入札で自動的に競合する。
 */
object InterstitialAdManager {

    enum class Trigger { IMPORT, EXPORT, THUMBNAIL_UPDATE }

    private var loadedAd: InterstitialAd? = null
    private var loadedAt: Long = 0L
    private var isLoading = false

    private val mainHandler = Handler(Looper.getMainLooper())

    private fun isLoadedAdStale(): Boolean =
        loadedAd != null && System.currentTimeMillis() - loadedAt >= AD_EXPIRATION_MILLIS

    fun preload(context: Context) {
        // 期限切れの広告は「読み込み済み」扱いにせず、必ず新しいものに差し替える。
        if (isLoadedAdStale()) loadedAd = null
        if (!ConsentManager.isAdsReady.value || AdFreeAccess.isAdFree(context)) return
        if (isLoading || loadedAd != null) return
        isLoading = true
        InterstitialAd.load(
            context.applicationContext,
            INTERSTITIAL_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    isLoading = false
                    loadedAd = ad
                    loadedAt = System.currentTimeMillis()
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    isLoading = false
                    loadedAd = null
                }
            },
        )
    }

    /**
     * 全画面広告共通のクールダウンと、トリガーごとの確率判定を満たしていれば表示する。
     * itemCountはTHUMBNAIL_UPDATEでのみ意味を持ち、まとめて更新した件数を渡す
     * (例: 複数選択で10件更新した場合はitemCount=10。10件以上ならほぼ確定表示になる)。
     * EXPORTは確率判定なし、IMPORTは実際の件数によらず常にTHUMBNAIL_UPDATEの
     * 5件分相当の重みで判定する。
     * 操作の完了と同時に呼ぶこと(遅延させて呼ぶと、ユーザーが次の操作をしようとした
     * 瞬間に広告が割り込み、誤タップの原因になる)。読み込みが間に合っていない場合は
     * 何もしない(ユーザーの操作を待たせない)。
     */
    fun maybeShow(activity: Activity, trigger: Trigger, itemCount: Int = 1) {
        // Activityが破棄中/終了中の状態で全画面広告を出そうとすると、ウィンドウ遷移が
        // 競合し黒画面のまま操作不能になることがあるため、表示可能な状態でなければ諦める。
        if (activity.isFinishing || activity.isDestroyed) return
        if (AdFreeAccess.isAdFree(activity)) return
        if (!FullScreenAdGate.canShow(activity)) return
        when (trigger) {
            Trigger.THUMBNAIL_UPDATE -> if (Random.nextInt(THUMBNAIL_UPDATE_FREQUENCY) >= itemCount) return
            Trigger.IMPORT -> if (Random.nextInt(THUMBNAIL_UPDATE_FREQUENCY) >= IMPORT_EQUIVALENT_ITEM_COUNT) return
            Trigger.EXPORT -> Unit // 確率判定なし
        }

        if (isLoadedAdStale()) loadedAd = null
        val ad = loadedAd
        if (ad == null) {
            // 広告が無い(未読み込み・期限切れ)場合は今回は表示を諦めるが、次の機会には
            // 表示できるよう、ここで改めて読み込みをキックしておく。
            preload(activity)
            return
        }
        loadedAd = null
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
            // 描画が実際に始まったことが確認できたので、開始前ウォッチドッグは解除するが、
            // その後Dismissed/FailedToShowのどちらも来ないまま詰まるケースに備え、
            // より長い猶予の第二段階ウォッチドッグに切り替える。
            override fun onAdShowedFullScreenContent() {
                mainHandler.removeCallbacks(watchdog)
                mainHandler.postDelayed(postStartWatchdog, SHOW_POST_START_WATCHDOG_MILLIS)
            }

            override fun onAdClicked() = AppOpenAdManager.notifyLeavingByOwnAction()

            override fun onAdDismissedFullScreenContent() {
                if (resolved) return
                mainHandler.removeCallbacks(watchdog)
                mainHandler.removeCallbacks(postStartWatchdog)
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
