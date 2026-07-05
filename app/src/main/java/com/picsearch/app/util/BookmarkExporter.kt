package com.picsearch.app.util

import android.content.Context
import androidx.core.content.FileProvider
import android.net.Uri
import com.picsearch.app.domain.model.Bookmark
import com.picsearch.app.domain.model.Folder
import java.io.File

/**
 * ブックマーク/フォルダをJSONへ書き出す。手書きシリアライズなのは、この程度のフラットな
 * データ構造に外部JSONライブラリを導入するほどではないため(既存のutil関数群も外部依存なし)。
 */
object BookmarkExporter {

    fun toJson(bookmarks: List<Bookmark>, folders: List<Folder>): String {
        val foldersJson = folders.joinToString(",") { folder ->
            """{"id":${folder.id},"name":${jsonString(folder.name)},"createdAt":${jsonString(folder.createdAt.toString())}}"""
        }
        val bookmarksJson = bookmarks.joinToString(",") { bookmark ->
            "{" +
                """"id":${bookmark.id},""" +
                """"url":${jsonString(bookmark.url)},""" +
                """"originalUrl":${jsonString(bookmark.originalUrl)},""" +
                """"title":${jsonStringOrNull(bookmark.title)},""" +
                """"description":${jsonStringOrNull(bookmark.description)},""" +
                """"siteName":${jsonStringOrNull(bookmark.siteName)},""" +
                """"ogImageUrl":${jsonStringOrNull(bookmark.ogImageUrl)},""" +
                """"faviconUrl":${jsonStringOrNull(bookmark.faviconUrl)},""" +
                """"fetchStatus":${jsonString(bookmark.fetchStatus.name)},""" +
                """"addedAt":${jsonString(bookmark.addedAt.toString())},""" +
                """"folderId":${bookmark.folderId ?: "null"}""" +
                "}"
        }
        return """{"folders":[$foldersJson],"bookmarks":[$bookmarksJson]}"""
    }

    /** JSONをキャッシュディレクトリへ書き出し、共有可能なcontent:// Uriを返す。 */
    fun saveToCacheAndGetUri(context: Context, json: String): Uri {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "picsearch_export_${System.currentTimeMillis()}.json")
        file.writeText(json)
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    private fun jsonStringOrNull(value: String?): String = if (value == null) "null" else jsonString(value)

    private fun jsonString(value: String): String = buildString {
        append('"')
        for (c in value) {
            when (c) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> if (c.code < 0x20) append("\\u%04x".format(c.code)) else append(c)
            }
        }
        append('"')
    }
}
