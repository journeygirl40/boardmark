package com.boardmark.app.util

import java.util.regex.Pattern

object UrlExtractor {

    // android.util.Patterns.WEB_URL はAndroidランタイム依存でローカル単体テストでは
    // 初期化されないため、ローカルJVMテストでも動く独自の正規表現を用いる。
    private val WEB_URL: Pattern = Pattern.compile(
        "https?://[\\w\\-.]+(?::\\d+)?(?:/[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]*)?"
    )

    fun extract(text: String?): String? {
        if (text.isNullOrBlank()) return null
        val matcher = WEB_URL.matcher(text)
        return if (matcher.find()) matcher.group() else null
    }
}
