package com.boardmark.app.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.boardmark.app.BuildConfig
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

private const val TAG = "ConsentManager"

/**
 * Google UMPによるGDPR同意管理。対象地域(EEA/UK/スイス)かどうかの判定はUMPが
 * 自動で行うため、ここでは国判定は行わない。同意フォームの表示にはActivityが必要な
 * ため、Applicationではなく最初のActivity(MainActivity)から呼び出す。
 */
object ConsentManager {

    // プロセス内で一度だけ実行する(端末回転などによるActivity再生成のたびに
    // 通信・フォーム表示をやり直さないためのガード)。
    private var initStarted = false

    fun initializeAdsIfNeeded(activity: Activity) {
        if (AdFreeAccess.isAdFree(activity) || initStarted) return
        initStarted = true

        val consentInformation = UserMessagingPlatform.getConsentInformation(activity)
        val paramsBuilder = ConsentRequestParameters.Builder()
        if (BuildConfig.DEBUG) {
            // 実機の所在地に関わらずEEA相当としてフォーム表示を確認できるようにする
            // (本番ビルドでは付与しない)。
            val debugSettings = ConsentDebugSettings.Builder(activity)
                .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                .build()
            paramsBuilder.setConsentDebugSettings(debugSettings)
        }

        consentInformation.requestConsentInfoUpdate(
            activity,
            paramsBuilder.build(),
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    if (formError != null) {
                        Log.w(TAG, "Consent form error: ${formError.message}")
                    }
                    onConsentResolved(activity, consentInformation.canRequestAds())
                }
            },
            { requestConsentError ->
                Log.w(TAG, "requestConsentInfoUpdate failed: ${requestConsentError.message}")
                onConsentResolved(activity, consentInformation.canRequestAds())
            },
        )
    }

    private fun onConsentResolved(activity: Activity, canRequestAds: Boolean) {
        if (!canRequestAds) return
        val appContext = activity.applicationContext
        MobileAds.initialize(appContext) {
            InterstitialAdManager.preload(appContext)
            NativeAdManager.preload(appContext)
        }
        // 同意フローの結果request可能と判定された時点のみ初期化する(簡易方式。
        // TCF文字列の自前パースによる厳密な承諾/拒否の弁別は行わない)。
        UnityAdsManager.initialize(appContext, gdprConsent = true)
        // ネイティブ広告フォールバック用のUnity LevelPlay SDK。旧Unity Ads SDKとは
        // 別モジュール・別クレデンシャルのため独立して初期化する。
        UnityLevelPlayManager.initialize(appContext, gdprConsent = true)
    }

    fun isPrivacyOptionsRequired(context: Context): Boolean =
        UserMessagingPlatform.getConsentInformation(context).privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    /** 設定画面から同意状態を確認・変更し直すための導線(Googleのポリシー上必須)。 */
    fun showPrivacyOptionsForm(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { dismissError ->
            if (dismissError != null) {
                Log.w(TAG, "showPrivacyOptionsForm error: ${dismissError.message}")
            }
            val canRequestAds = UserMessagingPlatform.getConsentInformation(activity).canRequestAds()
            UnityAdsManager.updateGdprConsent(activity.applicationContext, canRequestAds)
            UnityLevelPlayManager.updateGdprConsent(canRequestAds)
        }
    }
}
