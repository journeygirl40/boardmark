package com.boardmark.app.ui.components

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import com.boardmark.app.R
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView

// ネイティブ広告の各アセット(見出し・本文・CTAなど)は、AdMob SDKがクリック計測のために
// 実View(classic Android View)として登録する必要がある。Composeのテキストは個別の
// Viewとして参照できないため、このカードだけは純粋なAndroid Viewで組み立て、
// AndroidView 1つとしてグリッドに埋め込む(見た目だけBookmarkCard/FolderTileに揃える)。

private fun Context.dpToPx(dp: Float): Int =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics).toInt()

// 横幅に応じてサムネイル比率(CardThumbnailAspectRatio)を維持するためだけの入れ物。
// ConstraintLayoutへの依存を増やさずに済ませるため、onMeasureで高さを算出する。
private class AspectRatioFrameLayout(context: Context, private val ratio: Float) : FrameLayout(context) {
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = (width / ratio).toInt()
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY))
    }
}

private class NativeAdViewHolder(
    val root: NativeAdView,
    val mediaFrame: AspectRatioFrameLayout,
    val badgeText: TextView,
    val headlineView: TextView,
    val bodyView: TextView,
    val ctaView: TextView,
)

private fun buildNativeAdView(context: Context, adBadgeLabel: String): NativeAdViewHolder {
    val cornerRadiusPx = context.dpToPx(12f).toFloat()
    val mediaView = MediaView(context)
    val mediaFrame = AspectRatioFrameLayout(context, CardThumbnailAspectRatio).apply {
        outlineProvider = ViewOutlineProvider.BACKGROUND
        clipToOutline = true
        background = GradientDrawable().apply { cornerRadius = cornerRadiusPx }
        addView(mediaView, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
    }

    val badgeText = TextView(context).apply {
        text = adBadgeLabel
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
        setPadding(context.dpToPx(6f), context.dpToPx(2f), context.dpToPx(6f), context.dpToPx(2f))
        background = GradientDrawable().apply { cornerRadius = context.dpToPx(6f).toFloat() }
    }
    mediaFrame.addView(
        badgeText,
        FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.TOP or Gravity.START,
        ).apply {
            leftMargin = context.dpToPx(6f)
            topMargin = context.dpToPx(6f)
        },
    )

    val headlineView = TextView(context).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        setTypeface(typeface, Typeface.BOLD)
        maxLines = 2
        ellipsize = TextUtils.TruncateAt.END
    }
    val bodyView = TextView(context).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
    }
    // 「Ad」バッジと同じ見た目(色)にした上で、大きさ(文字サイズ・余白・角丸)も
    // 揃えることで、広告に付随する表示だと一貫して伝わるようにする。
    val ctaView = TextView(context).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
        setPadding(context.dpToPx(6f), context.dpToPx(2f), context.dpToPx(6f), context.dpToPx(2f))
        background = GradientDrawable().apply { cornerRadius = context.dpToPx(6f).toFloat() }
    }
    val ctaRow = FrameLayout(context).apply {
        addView(
            ctaView,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.START or Gravity.CENTER_VERTICAL,
            ),
        )
    }

    val column = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        addView(mediaFrame, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        addView(
            headlineView,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = context.dpToPx(6f)
            },
        )
        addView(bodyView, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        addView(
            ctaRow,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, context.dpToPx(26f)).apply {
                topMargin = context.dpToPx(4f)
            },
        )
    }

    val nativeAdView = NativeAdView(context).apply {
        addView(column, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        this.mediaView = mediaView
        this.headlineView = headlineView
        this.bodyView = bodyView
        this.callToActionView = ctaView
    }

    return NativeAdViewHolder(nativeAdView, mediaFrame, badgeText, headlineView, bodyView, ctaView)
}

@Composable
fun NativeAdCard(nativeAd: NativeAd, modifier: Modifier = Modifier) {
    val adBadgeLabel = stringResource(R.string.native_ad_badge)
    val titleColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val bodyColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    // ユーザーが付けるラベルタグ(secondaryContainerの彩度の高いピル)と見分けが
    // つくよう、広告バッジ・CTAボタンは同じ「黒スクリム+白文字」で統一する。
    // 見た目を揃えることで、どちらも広告に付随する要素だと一目で分かるようにする。
    val badgeBg = Color.Black.copy(alpha = 0.55f).toArgb()
    val badgeFg = Color.White.toArgb()
    val ctaBg = badgeBg
    val ctaFg = badgeFg
    val placeholderBg = MaterialTheme.colorScheme.surfaceVariant.toArgb()

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val holder = buildNativeAdView(ctx, adBadgeLabel)
            holder.root.tag = holder
            holder.root
        },
        update = { view ->
            val holder = view.tag as NativeAdViewHolder

            holder.headlineView.setTextColor(titleColor)
            holder.bodyView.setTextColor(bodyColor)
            holder.badgeText.setTextColor(badgeFg)
            (holder.badgeText.background as? GradientDrawable)?.setColor(badgeBg)
            holder.ctaView.setTextColor(ctaFg)
            (holder.ctaView.background as? GradientDrawable)?.setColor(ctaBg)
            (holder.mediaFrame.background as? GradientDrawable)?.setColor(placeholderBg)

            holder.headlineView.text = nativeAd.headline
            val bodyText = nativeAd.body ?: nativeAd.advertiser
            holder.bodyView.text = bodyText
            holder.bodyView.visibility = if (bodyText.isNullOrBlank()) View.GONE else View.VISIBLE
            val ctaText = nativeAd.callToAction
            holder.ctaView.text = ctaText
            holder.ctaView.visibility = if (ctaText.isNullOrBlank()) View.GONE else View.VISIBLE

            holder.root.setNativeAd(nativeAd)
        },
    )
}
