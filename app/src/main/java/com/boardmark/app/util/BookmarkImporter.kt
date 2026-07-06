package com.boardmark.app.util

import org.jsoup.Jsoup
import org.jsoup.nodes.Element

/** Netscapeブックマークファイル(Chrome/Firefox等がエクスポートするHTML)から読み取った1件分。 */
data class ImportedBookmark(
    val title: String,
    val url: String,
    val folderPath: List<String>,
)

/**
 * Netscape Bookmark File Format(<DL><DT><A>...のツリー構造)をパースする。
 * フォルダの入れ子(<DT><H3>の後に続く<DL>)は、実際のHTMLパーサーの挙動によって
 * <DT>の子になる場合と兄弟になる場合があるため両方をチェックする。
 */
object BookmarkImporter {

    fun parseNetscapeHtml(html: String): List<ImportedBookmark> {
        val doc = Jsoup.parse(html)
        val rootDl = doc.selectFirst("dl") ?: return emptyList()
        val results = mutableListOf<ImportedBookmark>()
        collect(rootDl, emptyList(), results)
        return results
    }

    private fun collect(dl: Element, path: List<String>, out: MutableList<ImportedBookmark>) {
        for (dt in dl.children().filter { it.tagName().equals("dt", ignoreCase = true) }) {
            val h3 = dt.selectFirst("h3")
            val a = dt.selectFirst("a")
            if (h3 != null) {
                val folderName = h3.text().trim()
                val nestedDl = dt.selectFirst("dl")
                    ?: dt.nextElementSibling()?.takeIf { it.tagName().equals("dl", ignoreCase = true) }
                if (nestedDl != null && folderName.isNotEmpty()) {
                    collect(nestedDl, path + folderName, out)
                }
            } else if (a != null) {
                val href = a.attr("href").trim()
                if (href.isNotEmpty()) {
                    out += ImportedBookmark(title = a.text().trim().ifBlank { href }, url = href, folderPath = path)
                }
            }
        }
    }
}
