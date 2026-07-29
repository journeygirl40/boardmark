package com.boardmark.app.ui.components

import androidx.compose.ui.unit.dp

/**
 * BookmarkCardとFolderTileで共通のサムネイル比率・下部メタ情報欄の高さ。
 * 両者でこの値がずれると、同じ行に並んだときにサムネの高さや縦位置が揃わなくなる。
 */
internal const val CardThumbnailAspectRatio = 1.4f
internal val CardMetaRowHeight = 26.dp

/**
 * グリッドの余白・列間隔。カード1枚あたりの実ピクセル幅を計算する際にも
 * LazyVerticalGridのcontentPadding/horizontalArrangementと同じ値を使う必要があるため、
 * ここに集約して両者のずれを防ぐ。
 */
internal val GridContentPadding = 12.dp
internal val GridHorizontalSpacing = 12.dp
internal val GridVerticalSpacing = 16.dp
