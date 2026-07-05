package com.picsearch.app.data.repository

import com.picsearch.app.data.local.BookmarkEntity
import com.picsearch.app.data.local.FolderEntity
import com.picsearch.app.domain.model.Bookmark
import com.picsearch.app.domain.model.Folder

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
