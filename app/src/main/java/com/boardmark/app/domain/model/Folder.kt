package com.boardmark.app.domain.model

import java.time.Instant

data class Folder(
    val id: Long,
    val name: String,
    val createdAt: Instant,
)

/**
 * トップレベル表示用。フォルダタイルに表示する先頭4件のプレビューと件数は、
 * 検索クエリに関係なくフォルダの全内容から計算する(中身を見て判断できるようにするため)。
 */
data class FolderWithPreview(
    val folder: Folder,
    val previewBookmarks: List<Bookmark>,
    val itemCount: Int,
)
