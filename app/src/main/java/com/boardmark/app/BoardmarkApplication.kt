package com.boardmark.app

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.hilt.work.HiltWorkerFactory
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.Configuration
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import com.boardmark.app.ads.InterstitialAdManager
import com.boardmark.app.ads.NativeAdManager
import com.boardmark.app.ads.UnityNativeAdManager
import com.boardmark.app.ui.MainActivity
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import okhttp3.OkHttpClient

@HiltAndroidApp
class BoardmarkApplication :
    Application(),
    Configuration.Provider,
    SingletonImageLoader.Factory,
    Application.ActivityLifecycleCallbacks,
    DefaultLifecycleObserver {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var okHttpClient: OkHttpClient

    // onStart(ActivityがまだonResume前でウィンドウのフォーカスを持たない可能性がある)の
    // 時点で即座に全画面広告を出すと、Unity Adsのフルスクリーン表示がホストActivityの
    // ウィンドウ遷移と競合し、黒画面のまま操作不能になることがある。そのため実際の
    // maybeShow呼び出しは、Activityが完全にonResumeするタイミングまで遅らせる。
    private var pendingAppOpenShow = false

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components { add(OkHttpNetworkFetcherFactory(callFactory = { okHttpClient })) }
            // 読み込み完了時にフェードインさせ、一覧スクロール中に画像が
            // 唐突にポップインして見えるのを抑える。
            .crossfade(true)
            .build()

    override fun onCreate() {
        super<Application>.onCreate()
        registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        // 広告SDK(MobileAds/Unity Ads)の初期化はGDPR同意フォームの表示にActivityが
        // 必要なため、ここではなくConsentManager経由でMainActivity.onCreateから行う。
    }

    // アプリプロセスがフォアグラウンドに来るたび(コールドスタート・他アプリからの復帰いずれも)に呼ばれる。
    // 実際に表示できるかどうか(確率・クールダウン)の判定はInterstitialAdManager側が行う。
    // maybeShow自体はクールダウンや確率判定で早期returnすることが多く、その場合は
    // 広告の期限切れチェックまで到達しないため、ここで毎回明示的にpreloadも呼び、
    // 表示されないまま古い広告が期限切れになって以後読み込まれなくなることを防ぐ。
    // ネイティブ広告も、この「アプリを開き直す」という自然な区切りでだけ更新する
    // (NativeAdManager側に別途クールダウンがあり、短時間の連続起動では読み込み直さない)。
    // ネイティブ広告はbanner/interstitialと違い1画面に1枠しか出せない制約が無く、
    // 一覧内の複数枠に同時に混ぜて表示できるため、AdMob(NativeAdManager)とUnity
    // (UnityNativeAdManager)を「フォールバック」ではなく独立した2つの広告在庫として
    // 常に両方読み込む(表示件数・収益を最大化する狙い)。
    // 全画面広告の実際の表示は、まだウィンドウがフォーカスを持たないこの時点では行わず、
    // 次にActivityがonResumeするタイミングまで遅らせる(onActivityResumed参照)。
    override fun onStart(owner: LifecycleOwner) {
        InterstitialAdManager.preload(this)
        pendingAppOpenShow = true
        NativeAdManager.preload(this)
        UnityNativeAdManager.preload(this)
    }

    override fun onActivityStarted(activity: Activity) = Unit

    override fun onActivityStopped(activity: Activity) = Unit

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

    // アプリ起動直後にUnity Ads等の全画面広告を出そうとすると、ホストActivityが
    // まだウィンドウフォーカスを得ていないタイミングと競合し、黒画面のまま操作不能になる
    // 不具合が発生していたため、Activityが確実にresumeし終えたこのタイミングまで待つ。
    // また、共有インテント受信用のShareReceiverActivity(透明・処理後すぐ自身をfinishする)
    // など、UIを持たない/一瞬で終了するActivity上に全画面広告を出すと、広告のウィンドウが
    // ホストを失って黒画面のまま残ってしまうため、必ずMainActivity上でのみ表示する。
    override fun onActivityResumed(activity: Activity) {
        if (pendingAppOpenShow && activity is MainActivity) {
            pendingAppOpenShow = false
            InterstitialAdManager.maybeShow(activity, InterstitialAdManager.Trigger.APP_OPEN)
        }
    }

    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit
}
