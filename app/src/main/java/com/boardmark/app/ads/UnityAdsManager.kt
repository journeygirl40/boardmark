package com.boardmark.app.ads

import android.content.Context
import android.util.Log
import com.boardmark.app.BuildConfig
import com.unity3d.ads.IUnityAdsInitializationListener
import com.unity3d.ads.UnityAds
import com.unity3d.ads.metadata.MetaData
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

private const val TAG = "UnityAdsManager"

/**
 * AdMob(Google Mobile Ads SDK)自体が機能しない場合でも広告を配信できるよう、
 * Mediationを介さず独立したフォールバック経路として組み込むUnity Ads SDKの初期化と
 * placement IDを集約する。
 */
object UnityAdsManager {

    /** Android専用に発行されたUnity Games ID。 */
    private const val GAME_ID = "6162537"

    const val BANNER_PLACEMENT_ID = "Banner_Android"
    const val INTERSTITIAL_PLACEMENT_ID = "Interstitial_Android"

    private var isInitialized = false
    private var isInitializing = false

    // GDPR同意フロー解決後にinitialize()が呼ばれる非同期な流れのため、Google広告の
    // 読み込み失敗判定の方が先に来ることがある。その時点でまだ初期化中/未着手であっても、
    // 完了を待ってから判定し直せるよう、完了(成功/失敗どちらも)を通知するリスナーを保持する。
    private val readyListeners = mutableListOf<(Boolean) -> Unit>()

    /**
     * gdprConsent: ConsentManagerが判定した「広告リクエスト可否」(canRequestAds())をそのまま渡す。
     * Unity Ads SDKへの同意伝達はIAB TCF文字列の自前パースまでは行わない簡易方式(既存合意事項)。
     * GDPRメタデータはinitialize()より前に確定させておく必要があるため、ここで先に設定する。
     */
    fun initialize(context: Context, gdprConsent: Boolean) {
        if (isInitialized || isInitializing) return
        isInitializing = true
        updateGdprConsent(context, gdprConsent)

        UnityAds.initialize(
            context.applicationContext,
            GAME_ID,
            BuildConfig.DEBUG,
            object : IUnityAdsInitializationListener {
                override fun onInitializationComplete() {
                    isInitializing = false
                    isInitialized = true
                    notifyReadyListeners(true)
                }

                override fun onInitializationFailed(
                    error: UnityAds.UnityAdsInitializationError,
                    message: String,
                ) {
                    isInitializing = false
                    Log.w(TAG, "Unity Ads initialization failed: [$error] $message")
                    notifyReadyListeners(false)
                }
            },
        )
    }

    private fun notifyReadyListeners(success: Boolean) {
        val listeners = readyListeners.toList()
        readyListeners.clear()
        listeners.forEach { it(success) }
    }

    fun isReady(): Boolean = isInitialized

    /**
     * 初期化が完了する(または失敗する)まで最大timeoutMillis待ってから、その時点で
     * 広告リクエスト可能かどうかを返す。GDPR同意フローがまだ解決しておらず
     * initialize()自体が呼ばれていない状態で呼び出しても、後から呼ばれた時点の結果を
     * 拾えるようリスナー登録だけしておく(タイムアウトすればfalseを返す)。
     */
    suspend fun awaitReady(timeoutMillis: Long): Boolean {
        if (isInitialized) return true
        return withTimeoutOrNull(timeoutMillis) {
            suspendCancellableCoroutine { continuation ->
                val listener: (Boolean) -> Unit = { success ->
                    if (continuation.isActive) continuation.resumeWith(Result.success(success))
                }
                readyListeners += listener
                continuation.invokeOnCancellation { readyListeners -= listener }
            }
        } ?: false
    }

    /**
     * 設定画面の「プライバシー設定」で同意をやり直した場合など、初期化後に同意状態が
     * 変わったときに呼び直す。既にcommit済みのメタデータは以降のロードリクエストから反映される。
     */
    fun updateGdprConsent(context: Context, gdprConsent: Boolean) {
        MetaData(context).apply {
            set("gdpr.consent", gdprConsent)
            commit()
        }
    }
}
