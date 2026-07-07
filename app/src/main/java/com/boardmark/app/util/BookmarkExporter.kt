package com.boardmark.app.util

import com.boardmark.app.domain.model.Bookmark
import com.boardmark.app.domain.model.Folder

object BookmarkExporter {

    /**
     * ブラウザ間で共有されているNetscape Bookmark File Format(Chrome/Firefox等がHTML
     * エクスポートに使う形式)へ書き出す。フォルダは1階層のみ(このアプリ自体がネストを
     * 持たないため)。
     */
    fun toNetscapeHtml(bookmarks: List<Bookmark>, folders: List<Folder>): String {
        val byFolder = bookmarks.groupBy { it.folderId }
        return buildString {
            append("<!DOCTYPE NETSCAPE-Bookmark-file-1>\n")
            append("<META HTTP-EQUIV=\"Content-Type\" CONTENT=\"text/html; charset=UTF-8\">\n")
            append("<TITLE>Bookmarks</TITLE>\n")
            append("<H1>Bookmarks</H1>\n")
            append("<DL><p>\n")
            byFolder[null].orEmpty().forEach { appendBookmarkLine(it, indent = 1) }
            folders.forEach { folder ->
                append("    <DT><H3 ADD_DATE=\"${folder.createdAt.epochSecond}\">${escapeHtml(folder.name)}</H3>\n")
                append("    <DL><p>\n")
                byFolder[folder.id].orEmpty().forEach { appendBookmarkLine(it, indent = 2) }
                append("    </DL><p>\n")
            }
            append("</DL><p>\n")
        }
    }

    private fun StringBuilder.appendBookmarkLine(bookmark: Bookmark, indent: Int) {
        val pad = "    ".repeat(indent)
        val title = escapeHtml(bookmark.title ?: bookmark.url)
        append("$pad<DT><A HREF=\"${escapeHtml(bookmark.url)}\" ADD_DATE=\"${bookmark.addedAt.epochSecond}\">$title</A>\n")
    }

    private fun escapeHtml(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
}
