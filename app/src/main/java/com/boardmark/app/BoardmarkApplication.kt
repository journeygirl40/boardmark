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
import com.boardmark.app.ads.AdFreeAccess
import com.boardmark.app.ads.InterstitialAdManager
import com.google.android.gms.ads.MobileAds
import dagger.hilt.android.HiltAndroidApp
import java.lang.ref.WeakReference
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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

    // アプリ全体が前面に来るたび(他アプリから復帰した場合も含む)に広告を出すため、
    // その時点で表示中のActivityを覚えておく(ProcessLifecycleOwnerはActivityを渡してくれないため)。
    private var currentActivity: WeakReference<Activity>? = null

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components { add(OkHttpNetworkFetcherFactory(callFactory = { okHttpClient })) }
            .build()

    override fun onCreate() {
        super<Application>.onCreate()
        registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        // 買い切りで広告非表示済みの端末では、広告SDKの初期化自体を行わない。
        if (AdFreeAccess.isAdFree(this)) return
        CoroutineScope(Dispatchers.IO).launch {
            // 完了リスナーはSDKがメインスレッドに戻して呼び出すため、
            // InterstitialAd.load(メインスレッド必須)はここから呼び出して問題ない。
            MobileAds.initialize(this@BoardmarkApplication) {
                InterstitialAdManager.preload(this@BoardmarkApplication)
            }
        }
    }

    // アプリプロセスがフォアグラウンドに来るたび(コールドスタート・他アプリからの復帰いずれも)に呼ばれる。
    // 実際に表示できるかどうか(確率・クールダウン)の判定はInterstitialAdManager側が行う。
    override fun onStart(owner: LifecycleOwner) {
        currentActivity?.get()?.let { InterstitialAdManager.maybeShow(it, InterstitialAdManager.Trigger.APP_OPEN) }
    }

    override fun onActivityStarted(activity: Activity) {
        currentActivity = WeakReference(activity)
    }

    override fun onActivityStopped(activity: Activity) {
        if (currentActivity?.get() === activity) currentActivity = null
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityResumed(activity: Activity) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit
}
