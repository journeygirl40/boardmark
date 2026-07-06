package com.boardmark.app.ads

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

// TODO: テスト用ID。申請/リリース前に本番ID(ca-app-pub-3334691626809528/2245831448)へ戻すこと。
private const val BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"

@Composable
fun BannerAd(modifier: Modifier = Modifier, adUnitId: String = BANNER_AD_UNIT_ID) {
    val context = LocalContext.current
    var isAdFree by remember { mutableStateOf(AdFreeAccess.isAdFree(context)) }
    if (isAdFree) return

    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { adViewContext ->
            AdView(adViewContext).apply {
                setAdSize(AdSize.BANNER)
                this.adUnitId = adUnitId
                loadAd(AdRequest.Builder().build())
            }
        },
    )
}
