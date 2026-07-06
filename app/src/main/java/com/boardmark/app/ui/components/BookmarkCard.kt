package com.boardmark.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.boardmark.app.domain.model.Bookmark
import com.boardmark.app.domain.model.FetchStatus
import com.boardmark.app.util.domainOf

@Composable
fun BookmarkCard(
    bookmark: Bookmark,
    isSelected: Boolean,
    selectionMode: Boolean,
    isDragActive: Boolean = false,
    isDropTarget: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val domain = domainOf(bookmark.url)

    // 選択された瞬間だけ軽くバウンドさせ、タップの手応えを演出する。
    val cardScale = remember { Animatable(1f) }
    LaunchedEffect(isSelected) {
        if (isSelected) {
            cardScale.animateTo(1.05f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
            cardScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }
    }

    // ドラッグ中(自分がつままれている側)は少し拡大したまま浮かせ、
    // 「動かせる/動いている」ことを視覚的に伝え続ける。
    val liftScale by animateFloatAsState(
        targetValue = if (isDragActive) 1.06f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "liftScale",
    )

    Column(modifier = modifier.scale(cardScale.value * liftScale)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(CardThumbnailAspectRatio)
                .clip(RoundedCornerShape(12.dp))
                .then(
                    if (isDropTarget) {
                        Modifier.border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                    } else {
                        Modifier
                    },
                ),
        ) {
            // サムネの縦横比を固定しContentScale.Cropで揃えることで、画像の実際の
            // アスペクト比に関わらずカードの見た目の大きさを統一する。
            when (bookmark.fetchStatus) {
                FetchStatus.PENDING -> {
                    Box(modifier = Modifier.fillMaxWidth().aspectRatio(CardThumbnailAspectRatio).background(Color.LightGray)) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center).padding(24.dp))
                    }
                }
                FetchStatus.SUCCESS, FetchStatus.FAILED -> {
                    val imageUrl = bookmark.ogImageUrl ?: bookmark.faviconUrl
                    if (imageUrl != null) {
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = bookmark.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth().aspectRatio(CardThumbnailAspectRatio),
                        )
                    } else {
                        DomainPlaceholder(domain = domain, modifier = Modifier.fillMaxWidth().aspectRatio(CardThumbnailAspectRatio))
                    }
                }
            }

            if (selectionMode) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(if (isSelected) Color.Black.copy(alpha = 0.35f) else Color.Transparent),
                )
                val checkScale = remember { Animatable(1f) }
                LaunchedEffect(isSelected) {
                    if (isSelected) {
                        checkScale.snapTo(0.5f)
                        checkScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                    } else {
                        checkScale.snapTo(1f)
                    }
                }
                Icon(
                    imageVector = if (isSelected) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                    contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(24.dp)
                        .scale(checkScale.value)
                        .background(
                            if (isSelected) Color.White else Color.Black.copy(alpha = 0.35f),
                            CircleShape,
                        ),
                )
            }
        }

        // タイトルは常に2行分の高さを確保し、1行で収まる場合も余白は保持する
        // (カードごとに高さがぶれないようにするため)。
        Text(
            text = bookmark.title ?: domain,
            style = MaterialTheme.typography.titleMedium,
            minLines = 2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp),
        )
        Text(
            text = bookmark.siteName?.ifBlank { null } ?: domain,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        // ラベルの有無・個数に関わらず高さを固定し(横スクロールで収める)、
        // カードの縦サイズがラベル数によってばらつかないようにする。
        Box(modifier = Modifier.fillMaxWidth().height(CardMetaRowHeight).padding(top = 4.dp)) {
            if (bookmark.labels.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                ) {
                    bookmark.labels.forEach { label ->
                        Text(
                            text = label.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                }
            }
        }
    }
}
