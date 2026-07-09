package com.boardmark.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * DB読み込み完了前に表示するプレースホルダー。空状態(EmptyBookmarksState)と
 * 見た目を明確に分けることで、「一覧が空である」ことと「まだ読み込み中である」
 * ことを混同させない。スクロール不要な固定件数のシマーカードを並べるだけで、
 * 実データの件数を待たずに即座に表示できる。
 */
@Composable
fun BookmarkGridSkeleton(columns: Int, modifier: Modifier = Modifier) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        userScrollEnabled = false,
        modifier = modifier.fillMaxSize(),
    ) {
        items(columns * 4) {
            BookmarkCardSkeleton()
        }
    }
}

@Composable
private fun BookmarkCardSkeleton() {
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(CardThumbnailAspectRatio)
                .shimmer(RoundedCornerShape(12.dp)),
        )
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .fillMaxWidth(0.8f)
                .height(16.dp)
                .shimmer(RoundedCornerShape(4.dp)),
        )
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .fillMaxWidth(0.5f)
                .height(12.dp)
                .shimmer(RoundedCornerShape(4.dp)),
        )
    }
}
