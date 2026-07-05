package com.boardmark.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.boardmark.app.domain.model.FetchStatus
import java.time.Instant

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val url: String,
    val originalUrl: String,
    val title: String?,
    val ogImageUrl: String?,
    val faviconUrl: String?,
    val fetchStatus: FetchStatus,
    val addedAt: Instant,
    val folderId: Long? = null,
    val description: String? = null,
    val siteName: String? = null,
)
