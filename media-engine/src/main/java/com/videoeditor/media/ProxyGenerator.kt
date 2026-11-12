package com.videoeditor.media

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Presentation
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import com.videoeditor.core.PerformanceManager
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume

/**
 * Generates low-resolution proxy videos for timeline/preview.
 * Runs only when PerformanceManager.useProxyForPreview() is true.
 * Never modifies original media.
 */
@UnstableApi
class ProxyGenerator(
    private val context: Context,
    private val tempFiles: TempFileManager
) {

    /**
     * Create or return existing proxy for [clipId] at the height for current performance mode.
     * Returns absolute path of proxy file, or null on failure.
     */
    suspend fun ensureProxy(
        clipId: String,
        sourceUri: String,
        mediaType: com.videoeditor.core.model.MediaType
    ): String? {
        if (mediaType == com.videoeditor.core.model.MediaType.AUDIO) return null
        if (mediaType == com.videoeditor.core.model.MediaType.IMAGE) return null // images stay as-is

        val height = PerformanceManager.maxPreviewHeight()
        val out = tempFiles.proxyFile(clipId, height)
        if (out.exists() && out.length() > 1024) {
            return out.absolutePath
        }

        return try {
            transcodeProxy(sourceUri, out, height)
            if (out.exists() && out.length() > 1024) out.absolutePath else null
        } catch (_: Exception) {
            out.delete()
            null
        }
    }

    private suspend fun transcodeProxy(
        sourceUri: String,
        output: File,
        height: Int
    ): Unit = suspendCancellableCoroutine { cont ->
        if (output.exists()) output.delete()

        val mediaItem = MediaItem.fromUri(Uri.parse(sourceUri))
        val edited = EditedMediaItem.Builder(mediaItem)
            .setEffects(
                Effects(
                    emptyList(),
                    listOf(Presentation.createForHeight(height))
                )
            )
            .build()

        val transformer = Transformer.Builder(context)
            .setVideoMimeType(MimeTypes.VIDEO_H264)
            .setAudioMimeType(MimeTypes.AUDIO_AAC)
            .build()

        transformer.addListener(object : Transformer.Listener {
            override fun onCompleted(
                composition: androidx.media3.transformer.Composition,
                exportResult: ExportResult
            ) {
                if (cont.isActive) cont.resume(Unit)
            }

            override fun onError(
                composition: androidx.media3.transformer.Composition,
                exportResult: ExportResult,
                exportException: ExportException
            ) {
                output.delete()
                if (cont.isActive) cont.resume(Unit) // soft fail — caller checks file
            }
        })

        cont.invokeOnCancellation {
            try {
                transformer.cancel()
            } catch (_: Exception) {
            }
            output.delete()
        }

        try {
            val sequence = androidx.media3.transformer.EditedMediaItemSequence(listOf(edited))
            val composition = androidx.media3.transformer.Composition.Builder(listOf(sequence)).build()
            transformer.start(composition, output.absolutePath)
        } catch (e: Exception) {
            output.delete()
            if (cont.isActive) cont.resume(Unit)
        }
    }
}
