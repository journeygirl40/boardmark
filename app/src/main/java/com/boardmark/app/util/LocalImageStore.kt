package com.boardmark.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlin.math.max

/**
 * 端末のギャラリーから選んだ写真を、アプリ専用ストレージにコピーして永続化する。
 * ピッカーが返すcontent:// Uriへの読み取り権限は一時的なため、コピーせず保存するとアプリ再起動後に読めなくなる。
 */
object LocalImageStore {

    // 一覧のサムネイル表示に必要な解像度はこれで十分。カメラ写真(4000px超)や
    // 端末フル解像度のWebキャプチャを原寸のまま保存すると、一覧スクロール中に
    // Coilが毎回大きなJPEGをデコードすることになりフリング中のジャンクの原因になる。
    private const val MAX_DIMENSION = 1080

    fun saveToInternalStorage(context: Context, sourceUri: Uri): String? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                BitmapFactory.decodeStream(input, null, bounds)
            } ?: return null

            val sampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight)
            val bitmap = context.contentResolver.openInputStream(sourceUri)?.use { input ->
                BitmapFactory.decodeStream(input, null, BitmapFactory.Options().apply { inSampleSize = sampleSize })
            } ?: return null

            saveBitmapToInternalStorage(context, bitmap)
        } catch (e: IOException) {
            null
        }
    }

    fun saveBitmapToInternalStorage(context: Context, bitmap: Bitmap): String? {
        return try {
            val scaled = downscaleIfNeeded(bitmap)
            val dir = File(context.filesDir, "thumbnails").apply { mkdirs() }
            val file = File(dir, "${UUID.randomUUID()}.jpg")
            file.outputStream().use { output -> scaled.compress(Bitmap.CompressFormat.JPEG, 90, output) }
            file.toURI().toString()
        } catch (e: IOException) {
            null
        }
    }

    /**
     * 完全バックアップの復元時に使う。バックアップ内の画像は保存時点で既にJPEGへ
     * 圧縮・縮小済みのため、再デコード・再エンコードはせずバイト列をそのまま書き出す。
     */
    fun restoreBytesToInternalStorage(context: Context, bytes: ByteArray): String? {
        return try {
            val dir = File(context.filesDir, "thumbnails").apply { mkdirs() }
            val file = File(dir, "${UUID.randomUUID()}.jpg")
            file.outputStream().use { output -> output.write(bytes) }
            file.toURI().toString()
        } catch (e: IOException) {
            null
        }
    }

    private fun downscaleIfNeeded(bitmap: Bitmap): Bitmap {
        val longerSide = max(bitmap.width, bitmap.height)
        if (longerSide <= MAX_DIMENSION) return bitmap
        val scale = MAX_DIMENSION.toFloat() / longerSide
        val width = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val height = (bitmap.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }

    private fun calculateInSampleSize(width: Int, height: Int): Int {
        var sampleSize = 1
        val longerSide = max(width, height)
        while (longerSide / (sampleSize * 2) >= MAX_DIMENSION) {
            sampleSize *= 2
        }
        return sampleSize
    }
}
