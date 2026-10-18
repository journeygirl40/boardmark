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
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.boardmark.app.R
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView

// ネイティブ広告の各アセット(見出し・本文・CTAなど)は、AdMob SDKがクリック計測のために
// 実View(classic Android View)として登録する必要がある。Composeのテキストは個別の
// Viewとして参照できないため、このカードだけは純粋なAndroid Viewで組み立て、
// AndroidView 1つとしてグリッドに埋め込む(見た目だけBookmarkCard/FolderTileに揃える)。

// 横幅いっぱいの1行で表示する場合の画像の比率(一般的な横長ネイティブ広告の1.91:1)。
private const val FullWidthMediaAspectRatio = 1.91f

// 広告カードの枠線の内側の余白(dp)。
private const val NativeAdCardInnerPaddingDp = 6f

// AdMobの規定で、MediaViewは120x120dp以上でなければならない(動画の再生要件であり、
// 満たさない枠は画像広告でも収益化の対象外になる)。
private val MinMediaSize = 120.dp

/**
 * カード幅cardWidthのとき、広告カード内のMediaView(余白を除いた幅x
 * CardThumbnailAspectRatioの高さ)がAdMobの最小サイズを満たすかどうか。
 */
internal fun nativeAdMediaFitsMinimumSize(cardWidth: Dp): Boolean {
    val mediaWidth = cardWidth - (NativeAdCardInnerPaddingDp * 2).dp
    val mediaHeight = mediaWidth / CardThumbnailAspectRatio
    return mediaWidth >= MinMediaSize && mediaHeight >= MinMediaSize
}

internal fun Context.dpToPx(dp: Float): Int =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics).toInt()

// 横幅に応じてサムネイル比率(CardThumbnailAspectRatio)を維持するためだけの入れ物。
// ConstraintLayoutへの依存を増やさずに済ませるため、onMeasureで高さを算出する。
internal class AspectRatioFrameLayout(context: Context, private val ratio: Float) : FrameLayout(context) {
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = (width / ratio).toInt()
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY))
    }
}

private class NativeAdViewHolder(
    val root: NativeAdView,
    val cardBackground: GradientDrawable,
    val mediaFrame: AspectRatioFrameLayout,
    val badgeText: TextView,
    val headlineView: TextView,
    val bodyView: TextView,
    val ctaView: TextView,
)

private fun buildNativeAdView(context: Context, adBadgeLabel: String, mediaAspectRatio: Float): NativeAdViewHolder {
    val cornerRadiusPx = context.dpToPx(12f).toFloat()
    val mediaView = MediaView(context)
    val mediaFrame = AspectRatioFrameLayout(context, mediaAspectRatio).apply {
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

    // ブックマークのカードと見た目を揃えすぎると、リンクを開くつもりのタップが広告の
    // クリックになってしまう(誤タップ=無効なクリック)。広告だけは枠線付きの別の地色に
    // 載せ、一覧の中で広告だと一目で区別できるようにする。
    val cardBackground = GradientDrawable().apply { cornerRadius = cornerRadiusPx }
    val column = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        background = cardBackground
        val innerPadding = context.dpToPx(NativeAdCardInnerPaddingDp)
        setPadding(innerPadding, innerPadding, innerPadding, innerPadding)
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

    return NativeAdViewHolder(nativeAdView, cardBackground, mediaFrame, badgeText, headlineView, bodyView, ctaView)
}

/**
 * fullWidth: 一覧の横幅いっぱいの1行として表示する場合はtrue。その場合は画像を一般的な
 * 横長広告の比率(1.91:1)にして、1行が画面を占めすぎないようにする。
 */
@Composable
fun NativeAdCard(nativeAd: NativeAd, fullWidth: Boolean, modifier: Modifier = Modifier) {
    // 比率はView生成時に決まるため、表示形式が切り替わったらViewごと作り直す。
    key(fullWidth) {
        val ratio = if (fullWidth) FullWidthMediaAspectRatio else CardThumbnailAspectRatio
        NativeAdCardContent(nativeAd, ratio, modifier)
    }
}

@Composable
private fun NativeAdCardContent(nativeAd: NativeAd, mediaAspectRatio: Float, modifier: Modifier) {
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
    val cardBg = MaterialTheme.colorScheme.surfaceContainerHigh.toArgb()
    val cardStroke = MaterialTheme.colorScheme.outlineVariant.toArgb()
    val cardStrokeWidth = with(LocalDensity.current) { 1.dp.roundToPx() }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val holder = buildNativeAdView(ctx, adBadgeLabel, mediaAspectRatio)
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
            holder.cardBackground.setColor(cardBg)
            holder.cardBackground.setStroke(cardStrokeWidth, cardStroke)

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
