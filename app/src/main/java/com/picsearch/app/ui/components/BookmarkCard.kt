package com.picsearch.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.picsearch.app.domain.model.Bookmark
import com.picsearch.app.domain.model.FetchStatus
import com.picsearch.app.util.domainOf

@Composable
fun BookmarkCard(
    bookmark: Bookmark,
    isSelected: Boolean,
    selectionMode: Boolean,
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

    Column(modifier = modifier.scale(cardScale.value)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp)),
        ) {
            // ロード中/画像なしのプレースホルダーは固定比率、実画像は縦横比を保って
            // 幅いっぱいに表示することでマソンリー(可変高さ)レイアウトを実現する。
            when (bookmark.fetchStatus) {
                FetchStatus.PENDING -> {
                    Box(modifier = Modifier.fillMaxWidth().aspectRatio(1.4f).background(Color.LightGray)) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center).padding(24.dp))
                    }
                }
                FetchStatus.SUCCESS, FetchStatus.FAILED -> {
                    val imageUrl = bookmark.ogImageUrl ?: bookmark.faviconUrl
                    if (imageUrl != null) {
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = bookmark.title,
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        DomainPlaceholder(domain = domain, modifier = Modifier.fillMaxWidth().aspectRatio(1.4f))
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

        Text(
            text = bookmark.title ?: domain,
            style = MaterialTheme.typography.titleMedium,
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
        if (!bookmark.description.isNullOrBlank()) {
            Text(
                text = bookmark.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}
