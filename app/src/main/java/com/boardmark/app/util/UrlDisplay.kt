package com.boardmark.app.util

import android.net.Uri

fun domainOf(url: String): String =
    Uri.parse(url).host?.removePrefix("www.") ?: url
