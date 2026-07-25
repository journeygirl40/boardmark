package com.boardmark.app.ads

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
    if (state == BannerState.HIDDEN) return

    // 固定のAdSize.BANNER(320x50dp)は、幅の広いタブレットでは画面に対して小さすぎて
    // 間延びして見える。実際に確保できた幅に合わせて高さも最適化される
    // アダプティブバナーを使うことで、端末サイズに関わらず自然な比率になる。
    BoxWithConstraints(modifier = modifier.fillMaxWidth().navigationBarsPadding()) {
        val adWidthDp = maxWidth.value.toInt()
        if (state == BannerState.UNITY) {
            AndroidView(
                modifier = Modifier.fillMaxWidth(),
                factory = { viewContext ->
                    BannerView(viewContext, UnityAdsManager.BANNER_PLACEMENT_ID, UnityBannerSize(320, 50)).apply {
                        listener = object : BannerView.IListener {
                            override fun onBannerLoaded(bannerAdView: BannerView) = Unit

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
                onRelease = { it.destroy() },
            )
        } else {
            AndroidView(
                modifier = Modifier.fillMaxWidth(),
                factory = { adViewContext ->
                    AdView(adViewContext).apply {
                        setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(adViewContext, adWidthDp))
                        this.adUnitId = adUnitId
                        adListener = object : AdListener() {
                            override fun onAdFailedToLoad(error: LoadAdError) {
                                // Unity Ads SDKがまだ初期化できていない場合は、確実に失敗する
                                // リクエストを投げるだけになるため、フォールバックせず折りたたむ。
                                state = if (UnityAdsManager.isReady()) BannerState.UNITY else BannerState.HIDDEN
                            }
                        }
                        loadAd(AdRequest.Builder().build())
                    }
                },
            )
        }
    }
}
