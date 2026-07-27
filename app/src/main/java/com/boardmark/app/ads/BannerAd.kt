package com.boardmark.app.ads

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
import kotlinx.coroutines.launch

// デバッグビルドはGoogle公式のテスト用ID、リリースビルドは本番IDを使う。
private val BANNER_AD_UNIT_ID = if (BuildConfig.DEBUG) {
    "ca-app-pub-3940256099942544/6300978111"
} else {
    "ca-app-pub-3334691626809528/2245831448"
}

// GDPR同意フロー解決後にUnity Ads SDKの初期化が走る非同期構成のため、Googleの
// 読み込み失敗時点ではUnity側がまだ初期化中/未着手であることが珍しくない。
// この時間だけ初期化完了を待ってからUnityへのフォールバック可否を判定する。
private const val UNITY_READY_TIMEOUT_MS = 8_000L

private const val CONTENT_FADE_IN_MS = 200L

/**
 * 広告View(AdView/Unity BannerView)を、読み込み完了までViewそのもののalphaプロパティで
 * 透明にしておき、完了したら滑らかにフェードインさせる。Composeのalpha modifierと違い、
 * View自身のalphaは初回描画から一貫して適用されるため、読み込み中に不透明な
 * プレースホルダ(SDKが内部的に使う既定の黒背景など)が一瞬見えてしまうことがない。
 */
private fun fadeIn(view: View) {
    view.alpha = 0f
    view.animate().alpha(1f).setDuration(CONTENT_FADE_IN_MS).start()
}

@Composable
fun BannerAd(modifier: Modifier = Modifier, adUnitId: String = BANNER_AD_UNIT_ID) {
    val context = LocalContext.current
    val isAdFree = remember { mutableStateOf(AdFreeAccess.isAdFree(context)) }.value
    if (isAdFree) return

    // AdMob・Unityともに読み込みに失敗した場合、広告欄自体の空間を完全に畳む。
    // 一度畳んだら再度広告Viewを作り直すことはない(片方向の遷移)ため、これによって
    // AndroidViewのfactoryが再実行されたり別のAndroidViewノードに差し替わったりはしない。
    var isHidden by remember { mutableStateOf(false) }
    if (isHidden) return

    val coroutineScope = rememberCoroutineScope()
    val containerBackgroundColor = MaterialTheme.colorScheme.background.toArgb()

    // 固定のAdSize.BANNER(320x50dp)は、幅の広いタブレットでは画面に対して小さすぎて
    // 間延びして見える。実際に確保できた幅に合わせて高さも最適化される
    // アダプティブバナーを使うことで、端末サイズに関わらず自然な比率になる。
    BoxWithConstraints(modifier = modifier.fillMaxWidth().navigationBarsPadding()) {
        val adWidthDp = maxWidth.value.toInt()

        // AdMob/Unityの切り替えをCompose側の状態分岐(if/elseで別々のAndroidViewを
        // 生成する)に頼ると、切り替わるたびにComposeが古いネイティブViewを破棄し
        // 新しいViewを一から生成し直すため、その最初の描画フレームでSDKの既定背景色が
        // 一瞬見えてしまう("黒い枠が一瞬出現して消える"不具合の原因)。そのため、
        // AndroidViewのnode自体はここで1度だけ生成し、以後はネットワークの選択を
        // 純粋なAndroid View操作(container.addView/removeAllViews)だけで行う。
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { viewContext ->
                val container = FrameLayout(viewContext).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                    )
                    setBackgroundColor(containerBackgroundColor)
                }

                fun collapse() {
                    container.removeAllViews()
                    isHidden = true
                }

                fun loadUnity() {
                    val banner = BannerView(
                        viewContext,
                        UnityAdsManager.BANNER_PLACEMENT_ID,
                        UnityBannerSize(320, 50),
                    ).apply {
                        alpha = 0f
                        listener = object : BannerView.IListener {
                            override fun onBannerLoaded(bannerAdView: BannerView) {
                                fadeIn(bannerAdView)
                            }

                            override fun onBannerFailedToLoad(bannerAdView: BannerView, errorInfo: BannerErrorInfo) {
                                // 読み込み失敗時は黒い枠だけを残さず、広告欄自体を折りたたむ。
                                collapse()
                            }

                            override fun onBannerClick(bannerAdView: BannerView) = Unit
                            override fun onBannerShown(bannerAdView: BannerView) = Unit
                            override fun onBannerLeftApplication(bannerAdView: BannerView) = Unit
                        }
                    }
                    container.removeAllViews()
                    container.addView(
                        banner,
                        ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT),
                    )
                    banner.load()
                }

                fun loadGoogle() {
                    val adView = AdView(viewContext).apply {
                        alpha = 0f
                        setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(viewContext, adWidthDp))
                        this.adUnitId = adUnitId
                        adListener = object : AdListener() {
                            override fun onAdLoaded() {
                                fadeIn(this@apply)
                            }

                            override fun onAdFailedToLoad(error: LoadAdError) {
                                // 即座にisReady()で判定すると、GDPR同意フロー解決待ちで
                                // Unity側の初期化がまだ終わっていないだけのケースまで
                                // 拾えずに折りたたんでしまうため、初期化完了を少し待つ。
                                coroutineScope.launch {
                                    if (UnityAdsManager.awaitReady(UNITY_READY_TIMEOUT_MS)) {
                                        loadUnity()
                                    } else {
                                        collapse()
                                    }
                                }
                            }
                        }
                    }
                    container.addView(
                        adView,
                        ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT),
                    )
                    adView.loadAd(AdRequest.Builder().build())
                }

                loadGoogle()
                container
            },
            update = { container -> container.setBackgroundColor(containerBackgroundColor) },
        )
    }
}
