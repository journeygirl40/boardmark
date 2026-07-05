package com.picsearch.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
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
import com.picsearch.app.domain.model.FolderWithPreview
import com.picsearch.app.util.domainOf
import kotlin.math.abs

private val FolderAccentPalette = listOf(
    Color(0xFFE05252), Color(0xFFE08A3C), Color(0xFFD1B22E),
    Color(0xFF4FA36B), Color(0xFF3C8FE0), Color(0xFF6C6CE0),
    Color(0xFFB25CD1), Color(0xFFD1608F),
)

private fun accentColorForFolder(folderId: Long): Color =
    FolderAccentPalette[abs(folderId.hashCode()) % FolderAccentPalette.size]

@Composable
fun FolderTile(
    data: FolderWithPreview,
    isSelected: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val accentColor = accentColorForFolder(data.folder.id)

    // 選択された瞬間だけ軽くバウンドさせ、タップの手応えを演出する。
    val scale = remember { Animatable(1f) }
    LaunchedEffect(isSelected) {
        if (isSelected) {
            scale.animateTo(1.06f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
            scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }
    }

    Column(modifier = modifier.scale(scale.value)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.4f)
                .clip(RoundedCornerShape(12.dp))
                .background(accentColor.copy(alpha = 0.25f)),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                repeat(2) { row ->
                    Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        repeat(2) { col ->
                            val index = row * 2 + col
                            val bookmark = data.previewBookmarks.getOrNull(index)
                            Box(modifier = Modifier.weight(1f).fillMaxHeight().padding(1.dp)) {
                                if (bookmark != null) {
                                    val imageUrl = bookmark.ogImageUrl ?: bookmark.faviconUrl
                                    if (imageUrl != null) {
                                        AsyncImage(
                                            model = imageUrl,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize(),
                                        )
                                    } else {
                                        DomainPlaceholder(
                                            domain = domainOf(bookmark.url),
                                            modifier = Modifier.fillMaxSize(),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(
                    text = "${data.itemCount}",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                )
            }

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f)),
                )
                val checkScale = remember { Animatable(0.4f) }
                LaunchedEffect(Unit) {
                    checkScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                }
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(24.dp)
                        .scale(checkScale.value)
                        .background(Color.White, CircleShape),
                )
            }
        }

        Row(modifier = Modifier.padding(top = 6.dp)) {
            Box(
                modifier = Modifier
                    .padding(top = 4.dp, end = 6.dp)
                    .size(8.dp)
                    .background(accentColor, CircleShape),
            )
            Text(
                text = data.folder.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
