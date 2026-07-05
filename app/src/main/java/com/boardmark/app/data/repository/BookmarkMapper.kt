package com.boardmark.app.data.repository

import com.boardmark.app.data.local.BookmarkEntity
import com.boardmark.app.data.local.FolderEntity
import com.boardmark.app.domain.model.Bookmark
import com.boardmark.app.domain.model.Folder

fun BookmarkEntity.toDomain(): Bookmark = Bookmark(
    id = id,
    url = url,
    originalUrl = originalUrl,
    title = title,
    ogImageUrl = ogImageUrl,
    faviconUrl = faviconUrl,
    fetchStatus = fetchStatus,
    addedAt = addedAt,
    folderId = folderId,
    description = description,
    siteName = siteName,
)

fun FolderEntity.toDomain(): Folder = Folder(id = id, name = name, createdAt = createdAt)
