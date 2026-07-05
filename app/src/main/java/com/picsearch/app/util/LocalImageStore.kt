package com.picsearch.app.util

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import java.io.File
import java.io.IOException
import java.util.UUID

/**
 * 端末のギャラリーから選んだ写真を、アプリ専用ストレージにコピーして永続化する。
 * ピッカーが返すcontent:// Uriへの読み取り権限は一時的なため、コピーせず保存するとアプリ再起動後に読めなくなる。
 */
object LocalImageStore {

    fun saveToInternalStorage(context: Context, sourceUri: Uri): String? {
        return try {
            val dir = File(context.filesDir, "thumbnails").apply { mkdirs() }
            val file = File(dir, "${UUID.randomUUID()}.jpg")
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            } ?: return null
            file.toURI().toString()
        } catch (e: IOException) {
            null
        }
    }

    fun saveBitmapToInternalStorage(context: Context, bitmap: Bitmap): String? {
        return try {
            val dir = File(context.filesDir, "thumbnails").apply { mkdirs() }
            val file = File(dir, "${UUID.randomUUID()}.jpg")
            file.outputStream().use { output -> bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output) }
            file.toURI().toString()
        } catch (e: IOException) {
            null
        }
    }
}
