package com.boardmark.app.data.repository

import com.boardmark.app.data.local.BookmarkEntity
import com.boardmark.app.data.local.BookmarkWithLabels
import com.boardmark.app.data.local.FolderEntity
import com.boardmark.app.data.local.LabelEntity
import com.boardmark.app.domain.model.Bookmark
import com.boardmark.app.domain.model.Folder
import com.boardmark.app.domain.model.Label

fun BookmarkWithLabels.toDomain(): Bookmark = bookmark.toDomain(labels = labels.map { it.toDomain() })

fun BookmarkEntity.toDomain(labels: List<Label> = emptyList()): Bookmark = Bookmark(
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
    manualOrder = manualOrder,
    viewCount = viewCount,
    duplicateIgnored = duplicateIgnored,
    labels = labels,
)

fun LabelEntity.toDomain(): Label = Label(id = id, name = name)

fun FolderEntity.toDomain(): Folder = Folder(
    id = id,
    name = name,
    createdAt = createdAt,
    defaultBrowserPackage = defaultBrowserPackage,
)
