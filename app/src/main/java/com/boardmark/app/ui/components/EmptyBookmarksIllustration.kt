package com.boardmark.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.boardmark.app.R

/**
 * ブックマークが0件のときの表示。外部画像を使わず、Material3のテーマ色で
 * 円+アイコンを組み合わせて「イラストっぽさ」を出す(DomainPlaceholderの
 * 「色付き図形+中央要素」という手法を踏襲)。検索/ラベル絞り込みの結果が0件の
 * 場合は、初回オンボーディング文言だと不自然なため文言を出し分ける。
 */
@Composable
fun EmptyBookmarksState(
    isFiltered: Boolean,
    modifier: Modifier = Modifier,
    onLearnMoreClick: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (isFiltered) Icons.Filled.SearchOff else Icons.Outlined.Bookmarks,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Text(
            text = stringResource(
                if (isFiltered) R.string.empty_state_filtered_title else R.string.empty_state_title,
            ),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 20.dp),
        )
        Text(
            text = stringResource(
                if (isFiltered) R.string.empty_state_filtered_message else R.string.empty_state_message,
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp),
        )
        // 検索/絞り込みで0件のときは「登録方法」の案内は的外れなので出さない。
        if (!isFiltered && onLearnMoreClick != null) {
            TextButton(onClick = onLearnMoreClick, modifier = Modifier.padding(top = 4.dp)) {
                Text(stringResource(R.string.empty_state_learn_more))
            }
        }
    }
}
