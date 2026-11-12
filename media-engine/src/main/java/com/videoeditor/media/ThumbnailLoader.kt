package com.videoeditor.media

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileOutputStream

/**
 * Asynchronous thumbnail generation with disk cache.
 * Designed for low-end: small size, single-thread friendly, no full decode.
 */
class ThumbnailLoader(
    private val context: Context,
    private val tempFiles: TempFileManager
) {

    suspend fun getThumbnail(
        clipId: String,
        mediaUri: String,
        timeUs: Long = 0L,
        maxSizePx: Int = 160
    ): Bitmap? = withContext(Dispatchers.IO) {
        val cache = tempFiles.thumbFile(clipId)
        if (cache.exists() && cache.length() > 100) {
            return@withContext android.graphics.BitmapFactory.decodeFile(cache.absolutePath)
        }

        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, Uri.parse(mediaUri))
            val frame = retriever.getFrameAtTime(
                timeUs,
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC
            ) ?: return@withContext null

            val scaled = scaleDown(frame, maxSizePx)
            if (scaled != frame) frame.recycle()

            try {
                FileOutputStream(cache).use { out ->
                    scaled.compress(Bitmap.CompressFormat.JPEG, 75, out)
                }
            } catch (_: Exception) {
            }
            scaled
        } catch (_: Exception) {
            null
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }
    }

    private fun scaleDown(src: Bitmap, maxSize: Int): Bitmap {
        val w = src.width
        val h = src.height
        if (w <= maxSize && h <= maxSize) return src
        val scale = maxSize.toFloat() / maxOf(w, h)
        val nw = (w * scale).toInt().coerceAtLeast(1)
        val nh = (h * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(src, nw, nh, true)
    }
}
