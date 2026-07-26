package com.boardmark.app.ads

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.boardmark.app.BuildConfig
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.unity3d.services.banners.BannerErrorInfo
import com.unity3d.services.banners.BannerView
import com.unity3d.services.banners.UnityBannerSize

// デバッグビルドはGoogle公式のテスト用ID、リリースビルドは本番IDを使う。
private val BANNER_AD_UNIT_ID = if (BuildConfig.DEBUG) {
    "ca-app-pub-3940256099942544/6300978111"
} else {
    "ca-app-pub-3334691626809528/2245831448"
}

private enum class BannerState { GOOGLE, UNITY, HIDDEN }

// GDPR同意フロー解決後にUnity Ads SDKの初期化が走る非同期構成のため、Googleの
// 読み込み失敗時点ではUnity側がまだ初期化中/未着手であることが珍しくない。
// この時間だけ初期化完了を待ってからUnityへのフォールバック可否を判定する。
private const val UNITY_READY_TIMEOUT_MS = 8_000L

@Composable
fun BannerAd(modifier: Modifier = Modifier, adUnitId: String = BANNER_AD_UNIT_ID) {
    val context = LocalContext.current
    var isAdFree by remember { mutableStateOf(AdFreeAccess.isAdFree(context)) }
    if (isAdFree) return

    // AdMob側がbannerの読み込みに失敗した場合(no-fillだけでなくSDK自体が機能しない
    // 場合も含む)は、独立したフォールバックとしてUnity Adsのバナーに切り替える。
    // Unity側もSDK未初期化・読み込み失敗であればHIDDENにして、コンテンツの乗っていない
    // 「黒い枠だけ」の広告欄を残さないようにする。
    var state by remember { mutableStateOf(BannerState.GOOGLE) }
    var awaitingUnityReady by remember { mutableStateOf(false) }
    if (state == BannerState.HIDDEN) return

    // 高さ0/INVISIBLEにして読み込み中を隠す方式は、Unity Ads側が実サイズ・表示可能な
    // コンテナであることを読み込み完了(onBannerLoaded)の条件にしているらしく、
    // 「読み込み完了まで隠す→隠れていると読み込みが完了しない」というデッドロックで
    // Unityバナーが一切表示されなくなる不具合を引き起こした。そのため、サイズ・
    // visibilityは常に実サイズ・VISIBLEのままにし、読み込み中の見た目だけを
    // (1)テーマ背景色への合わせ込みと(2)Composeのalphaによるクロスフェードで隠す。
    // どちらもView自体のサイズ/visibilityには影響しないため、SDK側のビューアビリティ
    // 判定と衝突しない。
    var isContentLoaded by remember(state) { mutableStateOf(false) }
    val contentAlpha by animateFloatAsState(
        targetValue = if (isContentLoaded) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "bannerAdContentAlpha",
    )
    val containerBackgroundColor = MaterialTheme.colorScheme.background.toArgb()

    // Google失敗直後はUnity Ads SDKがまだ初期化中/未着手のことがあるため、即HIDDENに
    // 倒さずに一定時間だけ初期化完了を待ってからフォールバック可否を判定し直す。
    LaunchedEffect(awaitingUnityReady) {
        if (awaitingUnityReady) {
            val ready = UnityAdsManager.awaitReady(UNITY_READY_TIMEOUT_MS)
            state = if (ready) BannerState.UNITY else BannerState.HIDDEN
            awaitingUnityReady = false
        }
    }

    // 固定のAdSize.BANNER(320x50dp)は、幅の広いタブレットでは画面に対して小さすぎて
    // 間延びして見える。実際に確保できた幅に合わせて高さも最適化される
    // アダプティブバナーを使うことで、端末サイズに関わらず自然な比率になる。
    BoxWithConstraints(modifier = modifier.fillMaxWidth().navigationBarsPadding()) {
        val adWidthDp = maxWidth.value.toInt()
        val adModifier = Modifier.fillMaxWidth().alpha(contentAlpha)
        if (state == BannerState.UNITY) {
            AndroidView(
                modifier = adModifier,
                factory = { viewContext ->
                    BannerView(viewContext, UnityAdsManager.BANNER_PLACEMENT_ID, UnityBannerSize(320, 50)).apply {
                        listener = object : BannerView.IListener {
                            override fun onBannerLoaded(bannerAdView: BannerView) {
                                isContentLoaded = true
                            }

                            override fun onBannerFailedToLoad(bannerAdView: BannerView, errorInfo: BannerErrorInfo) {
                                // 読み込み失敗時は黒い枠だけを残さず、広告欄自体を折りたたむ。
                                state = BannerState.HIDDEN
                            }

                            override fun onBannerClick(bannerAdView: BannerView) = Unit
                            override fun onBannerShown(bannerAdView: BannerView) = Unit
                            override fun onBannerLeftApplication(bannerAdView: BannerView) = Unit
                        }
                        load()
                    }
                },
                update = { view -> view.setBackgroundColor(containerBackgroundColor) },
                onRelease = { it.destroy() },
            )
        } else {
            AndroidView(
                modifier = adModifier,
                factory = { adViewContext ->
                    AdView(adViewContext).apply {
                        setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(adViewContext, adWidthDp))
                        this.adUnitId = adUnitId
                        adListener = object : AdListener() {
                            override fun onAdLoaded() {
                                isContentLoaded = true
                            }

                            override fun onAdFailedToLoad(error: LoadAdError) {
                                // 即座にisReady()で判定すると、GDPR同意フロー解決待ちで
                                // Unity側の初期化がまだ終わっていないだけのケースまで
                                // 拾えずに折りたたんでしまうため、初期化完了を少し待つ。
                                awaitingUnityReady = true
                            }
                        }
                        loadAd(AdRequest.Builder().build())
                    }
                },
                update = { view -> view.setBackgroundColor(containerBackgroundColor) },
            )
        }
    }
}
