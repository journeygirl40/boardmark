package com.picsearch.app.domain.model

import java.time.Instant

enum class FetchStatus { PENDING, SUCCESS, FAILED }

data class Bookmark(
    val id: Long,
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
