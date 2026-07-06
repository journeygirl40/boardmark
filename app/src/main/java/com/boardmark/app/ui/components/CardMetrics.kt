package com.boardmark.app.ui.components

import androidx.compose.ui.unit.dp

/**
 * BookmarkCardとFolderTileで共通のサムネイル比率・下部メタ情報欄の高さ。
 * 両者でこの値がずれると、同じ行に並んだときにサムネの高さや縦位置が揃わなくなる。
 */
internal const val CardThumbnailAspectRatio = 1.4f
internal val CardMetaRowHeight = 26.dp
