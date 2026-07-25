package com.boardmark.app.ads

import android.content.Context
import android.util.Log
import com.boardmark.app.BuildConfig
import com.unity3d.ads.IUnityAdsInitializationListener
import com.unity3d.ads.UnityAds
import com.unity3d.ads.metadata.MetaData

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
                }

                override fun onInitializationFailed(
                    error: UnityAds.UnityAdsInitializationError,
                    message: String,
                ) {
                    isInitializing = false
                    Log.w(TAG, "Unity Ads initialization failed: [$error] $message")
                }
            },
        )
    }

    fun isReady(): Boolean = isInitialized

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
