package com.boardmark.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import androidx.core.content.FileProvider
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * フォルダ内のサムネイルを1枚のコラージュ画像に合成し、共有可能なUriとして書き出す。
 * BoardmarkApplicationで設定済みのCoilシングルトンImageLoaderを再利用する(独自クライアントは作らない)。
 */
object FolderCollageRenderer {

    private const val TILE_SIZE_PX = 512
    private const val GRID_COLUMNS = 3

    suspend fun renderAndSaveCollage(context: Context, imageUrls: List<String>): Uri? =
        withContext(Dispatchers.IO) {
            val bitmaps = imageUrls.mapNotNull { url -> loadBitmap(context, url) }
            if (bitmaps.isEmpty()) return@withContext null

            val columns = GRID_COLUMNS.coerceAtMost(bitmaps.size)
            val rows = (bitmaps.size + columns - 1) / columns
            val collage = Bitmap.createBitmap(
                columns * TILE_SIZE_PX,
                rows * TILE_SIZE_PX,
                Bitmap.Config.ARGB_8888,
            )
            val canvas = Canvas(collage)
            canvas.drawColor(Color.WHITE)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

            bitmaps.forEachIndexed { index, bitmap ->
                val col = index % columns
                val row = index / columns
                val destRect = Rect(
                    col * TILE_SIZE_PX,
                    row * TILE_SIZE_PX,
                    (col + 1) * TILE_SIZE_PX,
                    (row + 1) * TILE_SIZE_PX,
                )
                canvas.drawBitmap(bitmap, centerCropRect(bitmap), destRect, paint)
            }

            val dir = File(context.cacheDir, "exports").apply { mkdirs() }
            val file = File(dir, "boardmark_collage_${System.currentTimeMillis()}.jpg")
            file.outputStream().use { out -> collage.compress(Bitmap.CompressFormat.JPEG, 90, out) }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        }

    private suspend fun loadBitmap(context: Context, url: String): Bitmap? {
        val request = ImageRequest.Builder(context)
            .data(url)
            .size(TILE_SIZE_PX, TILE_SIZE_PX)
            .allowHardware(false)
            .build()
        val result = context.imageLoader.execute(request)
        return (result as? SuccessResult)?.image?.toBitmap()
    }

    private fun centerCropRect(bitmap: Bitmap): Rect {
        val size = minOf(bitmap.width, bitmap.height)
        val left = (bitmap.width - size) / 2
        val top = (bitmap.height - size) / 2
        return Rect(left, top, left + size, top + size)
    }
}
