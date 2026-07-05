package com.picsearch.app.data.remote

sealed interface OgpResult {
    data class Success(
        val finalUrl: String,
        val title: String,
        val imageUrl: String?,
        val faviconUrl: String?,
        val description: String?,
        val siteName: String?,
    ) : OgpResult

    data object Failure : OgpResult
}
