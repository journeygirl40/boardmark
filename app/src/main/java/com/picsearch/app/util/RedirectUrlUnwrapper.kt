package com.picsearch.app.util

import java.net.URLDecoder

/**
 * 検索エンジンの中継(リダイレクト)URLから、クエリパラメータに埋め込まれた本来の遷移先URLを取り出す。
 * Bingの動画関連ページ(bing.com/videos/.../relatedvideo?...&churl=<実URL>)のように、
 * ページ自体がJavaScript描画でOGPタグを持たず、真の遷移先URLがパラメータに隠れているケースに対応する。
 * 汎用的なJS描画ページの解決策ではなく、既知の中継URL形式のみを対象とした限定的な対応。
 */
object RedirectUrlUnwrapper {

    private val WRAPPER_HOST_PARAMS = mapOf(
        "bing.com" to "churl",
    )

    fun unwrap(url: String): String {
        val host = extractHost(url) ?: return url
        val paramName = WRAPPER_HOST_PARAMS.entries.firstOrNull { host.endsWith(it.key) }?.value
            ?: return url
        val rawValue = extractQueryParam(url, paramName) ?: return url
        return runCatching { URLDecoder.decode(rawValue, "UTF-8") }.getOrDefault(rawValue)
    }

    private fun extractHost(url: String): String? {
        val withoutScheme = url.substringAfter("://", missingDelimiterValue = "")
        if (withoutScheme.isEmpty()) return null
        return withoutScheme.substringBefore("/").substringBefore("?").substringBefore(":")
    }

    private fun extractQueryParam(url: String, name: String): String? {
        val query = url.substringAfter("?", missingDelimiterValue = "")
        if (query.isEmpty()) return null
        return query.split("&")
            .mapNotNull { pair ->
                val idx = pair.indexOf('=')
                if (idx < 0) null else pair.substring(0, idx) to pair.substring(idx + 1)
            }
            .firstOrNull { it.first == name }
            ?.second
    }
}
