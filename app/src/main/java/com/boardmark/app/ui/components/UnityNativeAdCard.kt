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
import com.ironsource.mediationsdk.ads.nativead.LevelPlayMediaView
import com.ironsource.mediationsdk.ads.nativead.LevelPlayNativeAd
import com.ironsource.mediationsdk.ads.nativead.NativeAdLayout

// AdMob版(NativeAdCard.kt)と全く同じ理由・同じ見た目にするため、レイアウトの組み立て方も
// そちらを踏襲する。dpToPx/AspectRatioFrameLayoutはNativeAdCard.ktのものをそのまま使う。

private class UnityNativeAdViewHolder(
    val root: NativeAdLayout,
    val mediaFrame: AspectRatioFrameLayout,
    val badgeText: TextView,
    val headlineView: TextView,
    val bodyView: TextView,
    val ctaView: TextView,
)

private fun buildUnityNativeAdView(context: Context, adBadgeLabel: String): UnityNativeAdViewHolder {
    val cornerRadiusPx = context.dpToPx(12f).toFloat()
    val mediaView = LevelPlayMediaView(context)
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

    val nativeAdLayout = NativeAdLayout(context).apply {
        addView(column, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        setMediaView(mediaView)
        setTitleView(headlineView)
        setBodyView(bodyView)
        setCallToActionView(ctaView)
    }

    return UnityNativeAdViewHolder(nativeAdLayout, mediaFrame, badgeText, headlineView, bodyView, ctaView)
}

@Composable
fun UnityNativeAdCard(nativeAd: LevelPlayNativeAd, modifier: Modifier = Modifier) {
    val adBadgeLabel = stringResource(R.string.native_ad_badge)
    val titleColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val bodyColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    val badgeBg = Color.Black.copy(alpha = 0.55f).toArgb()
    val badgeFg = Color.White.toArgb()
    val ctaBg = badgeBg
    val ctaFg = badgeFg
    val placeholderBg = MaterialTheme.colorScheme.surfaceVariant.toArgb()

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val holder = buildUnityNativeAdView(ctx, adBadgeLabel)
            holder.root.tag = holder
            holder.root
        },
        update = { view ->
            val holder = view.tag as UnityNativeAdViewHolder

            holder.headlineView.setTextColor(titleColor)
            holder.bodyView.setTextColor(bodyColor)
            holder.badgeText.setTextColor(badgeFg)
            (holder.badgeText.background as? GradientDrawable)?.setColor(badgeBg)
            holder.ctaView.setTextColor(ctaFg)
            (holder.ctaView.background as? GradientDrawable)?.setColor(ctaBg)
            (holder.mediaFrame.background as? GradientDrawable)?.setColor(placeholderBg)

            holder.headlineView.text = nativeAd.title
            val bodyText = nativeAd.body ?: nativeAd.advertiser
            holder.bodyView.text = bodyText
            holder.bodyView.visibility = if (bodyText.isNullOrBlank()) View.GONE else View.VISIBLE
            val ctaText = nativeAd.callToAction
            holder.ctaView.text = ctaText
            holder.ctaView.visibility = if (ctaText.isNullOrBlank()) View.GONE else View.VISIBLE

            holder.root.registerNativeAdViews(nativeAd)
        },
    )
}
