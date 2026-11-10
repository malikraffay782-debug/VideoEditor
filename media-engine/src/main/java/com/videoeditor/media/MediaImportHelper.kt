package com.videoeditor.media

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import com.videoeditor.core.model.Clip
import com.videoeditor.core.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Helpers for importing media without modifying originals.
 * Duration and metadata are read only.
 */
object MediaImportHelper {

    suspend fun createClipFromUri(
        context: Context,
        uri: Uri,
        preferredType: MediaType? = null
    ): Clip? = withContext(Dispatchers.IO) {
        val type = preferredType ?: detectType(context, uri) ?: return@withContext null
        val name = queryDisplayName(context, uri) ?: "media"
        val durationUs = when (type) {
            MediaType.IMAGE -> 3_000_000L // default 3s for stills
            else -> readDurationUs(context, uri)
        }
        if (durationUs <= 0L && type != MediaType.IMAGE) return@withContext null

        Clip(
            mediaUri = uri.toString(),
            mediaType = type,
            displayName = name,
            sourceStartUs = 0L,
            sourceEndUs = durationUs,
            durationUs = durationUs,
            timelineStartUs = 0L
        )
    }

    private fun detectType(context: Context, uri: Uri): MediaType? {
        val mime = context.contentResolver.getType(uri) ?: return null
        return when {
            mime.startsWith("video/") -> MediaType.VIDEO
            mime.startsWith("audio/") -> MediaType.AUDIO
            mime.startsWith("image/") -> MediaType.IMAGE
            else -> null
        }
    }

    private fun queryDisplayName(context: Context, uri: Uri): String? {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && cursor.moveToFirst()) {
                return cursor.getString(idx)
            }
        }
        return uri.lastPathSegment
    }

    private fun readDurationUs(context: Context, uri: Uri): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
            durationMs * 1000
        } catch (_: Exception) {
            0L
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }
    }
}
