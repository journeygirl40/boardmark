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
import com.boardmark.app.ads.AdFreeAccess
import com.boardmark.app.ads.AppOpenAdManager
import com.boardmark.app.ads.InterstitialAdManager
import com.boardmark.app.ads.NativeAdManager
import com.boardmark.app.ads.RewardedAdManager
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
        AdFreeAccess.refresh(this)
        // 広告SDKの初期化はGDPR同意フォームの表示にActivityが必要なため、ここではなく
        // ConsentManager経由でMainActivity.onCreateから行う。
    }

    // アプリプロセスがフォアグラウンドに来るたび(コールドスタート・他アプリからの復帰いずれも)に呼ばれる。
    // アプリ起動広告を出してよい復帰かどうかの判定はAppOpenAdManager側が行う。
    // 各フォーマットのpreloadは、表示されないまま古い広告が期限切れになって以後
    // 読み込まれなくなることを防ぐため、この自然な区切りで毎回呼び直す。
    // ネイティブ広告は、長時間離れていた場合だけ、画面が描画される前のこの時点で
    // 全枠を入れ替える(すぐ戻った場合は、見ていた広告をそのまま残す)。
    // 全画面広告の実際の表示は、まだウィンドウがフォーカスを持たないこの時点では行わず、
    // MainActivityがonResumeするタイミングまで遅らせる(onActivityResumed参照)。
    override fun onStart(owner: LifecycleOwner) {
        AdFreeAccess.refresh(this)
        AppOpenAdManager.onAppForegrounded(this)
        InterstitialAdManager.preload(this)
        NativeAdManager.onAppForegrounded(this)
        RewardedAdManager.preload(this)
    }

    override fun onStop(owner: LifecycleOwner) {
        AppOpenAdManager.onAppBackgrounded()
        NativeAdManager.onAppBackgrounded()
    }

    override fun onActivityStarted(activity: Activity) = Unit

    override fun onActivityStopped(activity: Activity) = Unit

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

    // アプリ起動直後に全画面広告を出そうとすると、ホストActivityがまだウィンドウ
    // フォーカスを得ていないタイミングと競合し、黒画面のまま操作不能になる不具合が
    // 発生していたため、Activityが確実にresumeし終えたこのタイミングまで待つ。
    // また、共有インテント受信用のShareReceiverActivity(透明・処理後すぐ自身をfinishする)
    // など、UIを持たない/一瞬で終了するActivity上に全画面広告を出すと、広告のウィンドウが
    // ホストを失って黒画面のまま残ってしまうため、必ずMainActivity上でのみ表示する。
    override fun onActivityResumed(activity: Activity) {
        if (activity is MainActivity) AppOpenAdManager.onMainActivityResumed(activity)
    }

    override fun onActivityPaused(activity: Activity) {
        if (activity is MainActivity) AppOpenAdManager.onMainActivityPaused()
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit
}
