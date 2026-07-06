package com.boardmark.app.domain.model

import androidx.compose.runtime.Immutable
import java.time.Instant

@Immutable
data class Folder(
    val id: Long,
    val name: String,
    val createdAt: Instant,
    val defaultBrowserPackage: String? = null,
)

/**
 * トップレベル表示用。フォルダタイルに表示する先頭4件のプレビューと件数は、
 * 検索クエリに関係なくフォルダの全内容から計算する(中身を見て判断できるようにするため)。
 * contentsはラベル絞り込みなど、プレビューに含まれない項目も含めた判定が必要な場面で使う。
 */
@Immutable
data class FolderWithPreview(
    val folder: Folder,
    val previewBookmarks: List<Bookmark>,
    val itemCount: Int,
    val contents: List<Bookmark>,
)
