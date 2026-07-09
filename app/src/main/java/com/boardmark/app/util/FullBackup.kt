package com.boardmark.app.util

import android.content.Context
import android.net.Uri
import com.boardmark.app.domain.model.Bookmark
import com.boardmark.app.domain.model.FetchStatus
import com.boardmark.app.domain.model.Folder
import com.boardmark.app.domain.model.Label
import com.boardmark.app.domain.repository.FullBackupSnapshot
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.net.URI
import java.time.Instant
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

private const val SCHEMA_VERSION = 1
private const val ENTRY_MANIFEST = "backup.json"
private const val THUMBNAIL_ENTRY_PREFIX = "thumbnails/"

/**
 * ラベル・並び順・ローカルサムネイル実体まで含めた完全バックアップをZIPファイルとして
 * 書き出し/読み込みする。ブラウザ互換のNetscape HTML書き出し(BookmarkExporter)は
 * URL・タイトル・フォルダ名しか保持できないため、機種変更時に今の状態を丸ごと
 * 復元したい場合はこちらを使う。
 */
object FullBackup {

    suspend fun write(snapshot: FullBackupSnapshot, output: OutputStream) {
        withContext(Dispatchers.IO) {
            ZipOutputStream(output).use { zip ->
                val writtenThumbnails = mutableSetOf<String>()
                val bookmarksJson = JSONArray()
                for (bookmark in snapshot.bookmarks) {
                    val thumbnailName = localThumbnailFileName(bookmark.ogImageUrl)
                    if (thumbnailName != null && writtenThumbnails.add(thumbnailName)) {
                        localThumbnailFile(bookmark.ogImageUrl!!)?.takeIf { it.exists() }?.let { file ->
                            zip.putNextEntry(ZipEntry(THUMBNAIL_ENTRY_PREFIX + thumbnailName))
                            file.inputStream().use { it.copyTo(zip) }
                            zip.closeEntry()
                        }
                    }
                    bookmarksJson.put(bookmark.toJson())
                }

                val manifest = JSONObject().apply {
                    put("version", SCHEMA_VERSION)
                    put("folders", JSONArray(snapshot.folders.map { it.toJson() }))
                    put("labels", JSONArray(snapshot.labels.map { it.toJson() }))
                    put("bookmarks", bookmarksJson)
                }

                zip.putNextEntry(ZipEntry(ENTRY_MANIFEST))
                zip.write(manifest.toString().toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
        }
    }

    suspend fun read(context: Context, input: InputStream): FullBackupSnapshot {
        return withContext(Dispatchers.IO) {
            val thumbnailBytes = mutableMapOf<String, ByteArray>()
            var manifest: JSONObject? = null

            ZipInputStream(input).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    when {
                        entry.name == ENTRY_MANIFEST -> manifest = JSONObject(zip.readBytes().toString(Charsets.UTF_8))
                        entry.name.startsWith(THUMBNAIL_ENTRY_PREFIX) ->
                            thumbnailBytes[entry.name.removePrefix(THUMBNAIL_ENTRY_PREFIX)] = zip.readBytes()
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }

            val root = requireNotNull(manifest) { "backup.json not found in archive" }
            val folders = root.getJSONArray("folders").mapObjects(::folderFromJson)
            val labels = root.getJSONArray("labels").mapObjects(::labelFromJson)
            val bookmarks = root.getJSONArray("bookmarks").mapObjects { bookmarkFromJson(context, it, thumbnailBytes) }

            FullBackupSnapshot(folders = folders, labels = labels, bookmarks = bookmarks)
        }
    }

    private fun <T> JSONArray.mapObjects(transform: (JSONObject) -> T): List<T> =
        (0 until length()).map { transform(getJSONObject(it)) }

    // ---- JSON mapping ----

    private fun Folder.toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("createdAt", createdAt.epochSecond)
        putNullable("defaultBrowserPackage", defaultBrowserPackage)
    }

    private fun folderFromJson(obj: JSONObject): Folder = Folder(
        id = obj.getLong("id"),
        name = obj.getString("name"),
        createdAt = Instant.ofEpochSecond(obj.getLong("createdAt")),
        defaultBrowserPackage = obj.getNullableString("defaultBrowserPackage"),
    )

    private fun Label.toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
    }

    private fun labelFromJson(obj: JSONObject): Label = Label(id = obj.getLong("id"), name = obj.getString("name"))

    private fun Bookmark.toJson(): JSONObject = JSONObject().apply {
        put("url", url)
        put("originalUrl", originalUrl)
        putNullable("title", title)
        putNullable("ogImageUrl", ogImageUrl)
        putNullable("faviconUrl", faviconUrl)
        put("fetchStatus", fetchStatus.name)
        put("addedAt", addedAt.epochSecond)
        putNullable("folderId", folderId)
        putNullable("description", description)
        putNullable("siteName", siteName)
        put("manualOrder", manualOrder)
        put("viewCount", viewCount)
        put("duplicateIgnored", duplicateIgnored)
        put("labels", JSONArray(labels.map { it.name }))
    }

    private fun bookmarkFromJson(context: Context, obj: JSONObject, thumbnails: Map<String, ByteArray>): Bookmark {
        val labelsArray = obj.optJSONArray("labels") ?: JSONArray()
        return Bookmark(
            id = 0,
            url = obj.getString("url"),
            originalUrl = obj.getString("originalUrl"),
            title = obj.getNullableString("title"),
            ogImageUrl = restoreThumbnailIfLocal(context, obj.getNullableString("ogImageUrl"), thumbnails),
            faviconUrl = obj.getNullableString("faviconUrl"),
            fetchStatus = FetchStatus.valueOf(obj.getString("fetchStatus")),
            addedAt = Instant.ofEpochSecond(obj.getLong("addedAt")),
            folderId = if (obj.isNull("folderId")) null else obj.getLong("folderId"),
            description = obj.getNullableString("description"),
            siteName = obj.getNullableString("siteName"),
            manualOrder = obj.getDouble("manualOrder"),
            viewCount = obj.getInt("viewCount"),
            duplicateIgnored = obj.getBoolean("duplicateIgnored"),
            labels = (0 until labelsArray.length()).map { Label(id = 0, name = labelsArray.getString(it)) },
        )
    }

    // ---- サムネイル(ローカルfile://参照)の埋め込み/復元 ----

    private fun localThumbnailFileName(ogImageUrl: String?): String? {
        if (ogImageUrl == null || !ogImageUrl.startsWith("file:")) return null
        return Uri.parse(ogImageUrl).lastPathSegment
    }

    private fun localThumbnailFile(ogImageUrl: String): File? = runCatching { File(URI(ogImageUrl)) }.getOrNull()

    private fun restoreThumbnailIfLocal(context: Context, ogImageUrl: String?, thumbnails: Map<String, ByteArray>): String? {
        if (ogImageUrl == null || !ogImageUrl.startsWith("file:")) return ogImageUrl
        val bytes = Uri.parse(ogImageUrl).lastPathSegment?.let { thumbnails[it] } ?: return null
        return LocalImageStore.restoreBytesToInternalStorage(context, bytes)
    }

    private fun JSONObject.putNullable(key: String, value: Any?) {
        put(key, value ?: JSONObject.NULL)
    }

    private fun JSONObject.getNullableString(key: String): String? =
        if (has(key) && !isNull(key)) getString(key) else null
}
