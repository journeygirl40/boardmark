package com.boardmark.app.ads

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.boardmark.app.BuildConfig
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.unity3d.ads.metadata.MetaData
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG = "ConsentManager"

/**
 * Google UMPによるGDPR同意管理と、同意後のGoogle Mobile Ads SDK初期化。対象地域
 * (EEA/UK/スイス)かどうかの判定はUMPが自動で行うため、ここでは国判定は行わない。
 * 同意フォームの表示にはActivityが必要なため、Applicationではなく最初のActivity
 * (MainActivity)から呼び出す。
 *
 * 広告の読み込みは、すべてisAdsReadyがtrueになってから行う(同意取得前に広告を
 * リクエストしないため)。Unity Adsなど他ネットワークはAdMobメディエーション経由で
 * 呼び出されるため、ここで個別に初期化する必要はない。
 */
object ConsentManager {

    // プロセス内で一度だけ実行する(端末回転などによるActivity再生成のたびに
    // 通信・フォーム表示をやり直さないためのガード)。
    private var consentFlowStarted = false
    private val mobileAdsInitStarted = AtomicBoolean(false)

    private val _isAdsReady = MutableStateFlow(false)
    val isAdsReady: StateFlow<Boolean> = _isAdsReady.asStateFlow()

    private val mainHandler = Handler(Looper.getMainLooper())

    fun gatherConsentAndInitialize(activity: Activity) {
        if (AdFreeAccess.isPurchased(activity) || consentFlowStarted) return
        consentFlowStarted = true

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
                if (consentInformation.consentStatus == ConsentInformation.ConsentStatus.REQUIRED) {
                    // 同意フォームを出す起動では、起動画面で広告を待たない。
                    AppOpenAdManager.cancelColdStartWait()
                }
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    if (formError != null) {
                        Log.w(TAG, "Consent form error: ${formError.message}")
                    }
                    if (consentInformation.canRequestAds()) initializeMobileAds(activity)
                }
            },
            { requestConsentError ->
                Log.w(TAG, "requestConsentInfoUpdate failed: ${requestConsentError.message}")
                if (consentInformation.canRequestAds()) initializeMobileAds(activity)
            },
        )

        // 前回までのセッションで同意が得られていれば、今回の同意情報の更新(通信)を
        // 待たずにすぐ初期化を始める(Google推奨。起動直後の広告読み込みを間に合わせるため)。
        if (consentInformation.canRequestAds()) initializeMobileAds(activity)
    }

    private fun initializeMobileAds(context: Context) {
        if (!mobileAdsInitStarted.compareAndSet(false, true)) return
        val appContext = context.applicationContext
        // Unity Adsはメディエーション経由で初期化されるため、同意状態はその前に渡しておく
        // (UMPで広告リクエスト可と判定された時点で同意済みとして扱う簡易方式)。
        updateUnityConsent(appContext, gdprConsent = true)
        // 初期化はメインスレッドを塞がないようバックグラウンドで行う(Google推奨)。
        CoroutineScope(Dispatchers.IO).launch {
            MobileAds.initialize(appContext) {
                mainHandler.post {
                    _isAdsReady.value = true
                    preloadAll(appContext)
                }
            }
        }
    }

    /** 各フォーマットの在庫を読み込む。SDK初期化前・広告非表示中は各マネージャ側で何もしない。 */
    fun preloadAll(context: Context) {
        AppOpenAdManager.preload(context)
        InterstitialAdManager.preload(context)
        NativeAdManager.preload(context)
        RewardedAdManager.preload(context)
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
            updateUnityConsent(activity.applicationContext, canRequestAds)
            if (canRequestAds) initializeMobileAds(activity)
        }
    }

    // Unity AdsはGoogleのUMPの同意状態を自動では読まないため、メタデータで明示的に渡す
    // (AdMobのUnityメディエーション導入ガイドの手順どおり)。
    private fun updateUnityConsent(context: Context, gdprConsent: Boolean) {
        MetaData(context).apply {
            set("gdpr.consent", gdprConsent)
            commit()
        }
    }
}
