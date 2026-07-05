package com.picsearch.app.data.remote

import com.picsearch.app.util.RedirectUrlUnwrapper
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup

private const val MOBILE_USER_AGENT =
    "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36"
private const val MAX_CANDIDATE_IMAGES = 24

class OgpFetcher @Inject constructor(private val client: OkHttpClient) {

    suspend fun fetch(url: String): OgpResult = withContext(Dispatchers.IO) {
        try {
            val targetUrl = RedirectUrlUnwrapper.unwrap(url)
            val request = Request.Builder()
                .url(targetUrl)
                .header("User-Agent", MOBILE_USER_AGENT)
                .header("Accept", "text/html,application/xhtml+xml,*/*;q=0.8")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext OgpResult.Failure
                val finalUrl = response.request.url.toString()
                val body = response.body ?: return@withContext OgpResult.Failure

                val doc = Jsoup.parse(body.byteStream(), null, finalUrl)

                val image = doc.select("meta[property=og:image]").attr("abs:content")
                    .ifBlank { doc.select("meta[name=twitter:image]").attr("abs:content") }
                val title = doc.select("meta[property=og:title]").attr("content")
                    .ifBlank { doc.title() }
                val favicon = doc.select("link[rel~=(?i)^icon$]").attr("abs:href")
                    .ifBlank { doc.select("link[rel~=(?i)shortcut icon]").attr("abs:href") }
                val description = doc.select("meta[property=og:description]").attr("content")
                    .ifBlank { doc.select("meta[name=description]").attr("content") }
                val siteName = doc.select("meta[property=og:site_name]").attr("content")

                OgpResult.Success(
                    finalUrl = finalUrl,
                    title = title.ifBlank { finalUrl },
                    imageUrl = image.ifBlank { null },
                    faviconUrl = favicon.ifBlank { null },
                    description = description.ifBlank { null },
                    siteName = siteName.ifBlank { null },
                )
            }
        } catch (e: IOException) {
            OgpResult.Failure
        }
    }

    /**
     * 自動取得に失敗した際、ユーザーが手動で選べるよう画像候補を集める。
     * YouTubeなどJavaScriptで描画するSPA型ページは本文に<img>タグをほぼ持たないため、
     * <img>だけでなくog:image/twitter:image、<link rel="image_src">、JSON-LD構造化データの
     * thumbnailUrl/imageも候補に含め、静的HTMLの範囲でヒット率を上げる。
     */
    suspend fun fetchCandidateImages(url: String): List<String> = withContext(Dispatchers.IO) {
        try {
            val targetUrl = RedirectUrlUnwrapper.unwrap(url)
            val request = Request.Builder()
                .url(targetUrl)
                .header("User-Agent", MOBILE_USER_AGENT)
                .header("Accept", "text/html,application/xhtml+xml,*/*;q=0.8")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val finalUrl = response.request.url.toString()
                val body = response.body ?: return@withContext emptyList()
                val doc = Jsoup.parse(body.byteStream(), null, finalUrl)

                val candidates = LinkedHashSet<String>()

                doc.select("meta[property=og:image], meta[property=og:image:secure_url], meta[name=twitter:image]")
                    .forEach { el -> el.attr("abs:content").ifBlank { null }?.let(candidates::add) }

                doc.select("link[rel=image_src]")
                    .forEach { el -> el.attr("abs:href").ifBlank { null }?.let(candidates::add) }

                doc.select("video[poster]")
                    .forEach { el -> el.attr("abs:poster").ifBlank { null }?.let(candidates::add) }

                doc.select("script[type=application/ld+json]").forEach { script ->
                    JSON_LD_THUMBNAIL_REGEX.findAll(script.data())
                        .forEach { match -> candidates.add(match.groupValues[1]) }
                }

                doc.select("img[src]")
                    .forEach { el -> el.attr("abs:src").ifBlank { null }?.let(candidates::add) }

                candidates.take(MAX_CANDIDATE_IMAGES).toList()
            }
        } catch (e: IOException) {
            emptyList()
        }
    }

    private companion object {
        val JSON_LD_THUMBNAIL_REGEX = Regex(""""(?:thumbnailUrl|image)"\s*:\s*\[?\s*"([^"]+)"""")
    }
}
