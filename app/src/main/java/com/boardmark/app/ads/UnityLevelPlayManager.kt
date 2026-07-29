package com.boardmark.app.ads

import android.content.Context
import android.util.Log
import com.unity3d.mediation.LevelPlay
import com.unity3d.mediation.LevelPlayConfiguration
import com.unity3d.mediation.LevelPlayInitError
import com.unity3d.mediation.LevelPlayInitListener
import com.unity3d.mediation.LevelPlayInitRequest
import com.unity3d.mediation.LevelPlayPrivacySettings

private const val TAG = "UnityLevelPlayManager"

/**
 * ネイティブ広告専用に使うUnity LevelPlay SDK(com.unity3d.mediation.*、ironSourceの後継)の
 * 初期化を管理する。banner/interstitialのフォールバックに使っている旧来のUnity Ads SDK
 * (UnityAdsManager, com.unity3d.ads.*)とはアーティファクト・APIが完全に別物で初期化状態も
 * 共有されない(重複クラスも無く共存可能なことは確認済み)ため、独立したGame ID相当の
 * 「App Key」で別途初期化する。
 */
object UnityLevelPlayManager {

    /** Unity LevelPlay(platform.ironsrc.com)Dashboardで発行したこのアプリのApp Key。 */
    private const val APP_KEY = "27628c575"

    private var isInitialized = false
    private var isInitializing = false

    /**
     * gdprConsent: ConsentManagerが判定した「広告リクエスト可否」(canRequestAds())をそのまま渡す。
     * UnityAdsManager.initializeと同様、init()より前にGDPR同意を確定させておく。
     */
    fun initialize(context: Context, gdprConsent: Boolean) {
        if (isInitialized || isInitializing) return
        isInitializing = true
        LevelPlayPrivacySettings.setGDPRConsent(gdprConsent)

        val request = LevelPlayInitRequest.Builder(APP_KEY).build()
        LevelPlay.init(
            context.applicationContext,
            request,
            object : LevelPlayInitListener {
                override fun onInitSuccess(configuration: LevelPlayConfiguration) {
                    isInitializing = false
                    isInitialized = true
                    UnityNativeAdManager.preload(context.applicationContext)
                }

                override fun onInitFailed(error: LevelPlayInitError) {
                    isInitializing = false
                    Log.w(TAG, "LevelPlay init failed: [${error.errorCode}] ${error.errorMessage}")
                }
            },
        )
    }

    fun isReady(): Boolean = isInitialized

    /** 設定画面の「プライバシー設定」で同意をやり直した場合など、初期化後に同意状態が変わったときに呼び直す。 */
    fun updateGdprConsent(gdprConsent: Boolean) {
        LevelPlayPrivacySettings.setGDPRConsent(gdprConsent)
    }
}
