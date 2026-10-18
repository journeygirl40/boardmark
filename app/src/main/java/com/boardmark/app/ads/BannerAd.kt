package com.boardmark.app.ads

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import com.boardmark.app.BuildConfig
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

// デバッグビルドはGoogle公式のテスト用ID、リリースビルドは本番IDを使う。
private val BANNER_AD_UNIT_ID = if (BuildConfig.DEBUG) {
    "ca-app-pub-3940256099942544/6300978111"
} else {
    "ca-app-pub-3334691626809528/2245831448"
}

private const val CONTENT_FADE_IN_MS = 200L

/**
 * 広告View(AdView)を、読み込み完了までViewそのもののalphaプロパティで
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
    val isAdFree by AdFreeAccess.isAdFreeFlow.collectAsState()
    // 同意取得とSDK初期化が済むまでは広告をリクエストしない(枠も確保しない)。
    val isAdsReady by ConsentManager.isAdsReady.collectAsState()
    if (isAdFree || !isAdsReady) return

    // 一度も読み込めなかった場合、広告欄自体の空間を完全に畳む。他ネットワーク(Unity Ads等)の
    // 在庫はAdMobメディエーション内で競合済みのため、ここで別途フォールバックはしない。
    var isHidden by remember { mutableStateOf(false) }
    if (isHidden) return

    val containerBackgroundColor = MaterialTheme.colorScheme.background.toArgb()

    // 固定のAdSize.BANNER(320x50dp)は、幅の広いタブレットでは画面に対して小さすぎて
    // 間延びして見える。実際に確保できた幅に合わせて高さも最適化される
    // アダプティブバナーを使うことで、端末サイズに関わらず自然な比率になる。
    BoxWithConstraints(modifier = modifier.fillMaxWidth().navigationBarsPadding()) {
        val adWidthDp = maxWidth.value.toInt()

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
                val adView = AdView(viewContext).apply {
                    alpha = 0f
                    setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(viewContext, adWidthDp))
                    this.adUnitId = adUnitId
                    adListener = object : AdListener() {
                        override fun onAdLoaded() {
                            // 自動更新で2回目以降が読み込まれた場合はフェードし直さない。
                            if (alpha == 0f) fadeIn(this@apply)
                        }

                        override fun onAdClicked() = AppOpenAdManager.notifyLeavingByOwnAction()

                        override fun onAdFailedToLoad(error: LoadAdError) {
                            // まだ一度も表示できていない場合だけ、黒い枠を残さず広告欄を畳む
                            // (表示済みなら、自動更新の失敗時も前の広告がそのまま残る)。
                            if (alpha == 0f) {
                                container.removeAllViews()
                                destroy()
                                isHidden = true
                            }
                        }
                    }
                }
                container.addView(
                    adView,
                    ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT),
                )
                adView.loadAd(AdRequest.Builder().build())
                container
            },
            update = { container -> container.setBackgroundColor(containerBackgroundColor) },
            onRelease = { container -> (container.getChildAt(0) as? AdView)?.destroy() },
        )
    }
}
