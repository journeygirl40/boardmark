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
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

// デバッグビルドはGoogle公式のテスト用ID、リリースビルドは本番IDを使う。
private val BANNER_AD_UNIT_ID = if (BuildConfig.DEBUG) {
    "ca-app-pub-3940256099942544/6300978111"
} else {
    "ca-app-pub-3334691626809528/2245831448"
}

@Composable
fun BannerAd(modifier: Modifier = Modifier, adUnitId: String = BANNER_AD_UNIT_ID) {
    val context = LocalContext.current
    var isAdFree by remember { mutableStateOf(AdFreeAccess.isAdFree(context)) }
    if (isAdFree) return

    // 固定のAdSize.BANNER(320x50dp)は、幅の広いタブレットでは画面に対して小さすぎて
    // 間延びして見える。実際に確保できた幅に合わせて高さも最適化される
    // アダプティブバナーを使うことで、端末サイズに関わらず自然な比率になる。
    BoxWithConstraints(modifier = modifier.fillMaxWidth().navigationBarsPadding()) {
        val adWidthDp = maxWidth.value.toInt()
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { adViewContext ->
                AdView(adViewContext).apply {
                    setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(adViewContext, adWidthDp))
                    this.adUnitId = adUnitId
                    loadAd(AdRequest.Builder().build())
                }
            },
        )
    }
}
