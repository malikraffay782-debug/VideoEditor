package com.videoeditor.export

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Presentation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import com.videoeditor.core.model.Clip
import com.videoeditor.core.model.MediaType
import com.videoeditor.core.model.Project
import com.videoeditor.effects.Media3EffectMapper
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

sealed class ExportState {
    data object Idle : ExportState()
    data class Progress(val percent: Float) : ExportState()
    data class Completed(val outputPath: String) : ExportState()
    data class Failed(val message: String) : ExportState()
    data object Cancelled : ExportState()
}

/**
 * Media3 Transformer-based exporter.
 * Non-destructive: reads original URIs, writes a new file.
 * Supports progress and cancellation.
 */
@UnstableApi
class VideoExporter(private val context: Context) {

    private var transformer: Transformer? = null
    private val cancelled = AtomicBoolean(false)
    private val mainHandler = Handler(Looper.getMainLooper())

    fun export(
        project: Project,
        config: ExportConfig,
        outputFile: File
    ): Flow<ExportState> = callbackFlow {
        cancelled.set(false)
        trySend(ExportState.Progress(0f))

        val videoClips = project.tracks
            .flatMap { it.clips }
            .filter { it.mediaType == MediaType.VIDEO || it.mediaType == MediaType.IMAGE }
            .sortedBy { it.timelineStartUs }

        if (videoClips.isEmpty()) {
            trySend(ExportState.Failed("No video/image clips to export"))
            close()
            return@callbackFlow
        }

        val editedItems = videoClips.map { clip -> buildEditedItem(clip, config) }

        val sequence = EditedMediaItemSequence(editedItems)
        val composition = Composition.Builder(listOf(sequence)).build()

        val transformerBuilder = Transformer.Builder(context)
            .setVideoMimeType(MimeTypes.VIDEO_H264)
            .setAudioMimeType(MimeTypes.AUDIO_AAC)

        val t = transformerBuilder.build()
        transformer = t

        t.addListener(object : Transformer.Listener {
            override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                if (cancelled.get()) {
                    trySend(ExportState.Cancelled)
                } else {
                    trySend(ExportState.Completed(outputFile.absolutePath))
                }
                close()
            }

            override fun onError(
                composition: Composition,
                exportResult: ExportResult,
                exportException: ExportException
            ) {
                if (cancelled.get()) {
                    trySend(ExportState.Cancelled)
                } else {
                    trySend(ExportState.Failed(exportException.message ?: "Export failed"))
                }
                close()
            }
        })

        // Progress polling (Transformer has limited progress API on older versions)
        val progressRunnable = object : Runnable {
            override fun run() {
                if (cancelled.get()) return
                val progressHolder = android.util.Pair<Int, Float>(0, 0f)
                // Best-effort: some Media3 versions expose getProgress
                try {
                    val method = t.javaClass.getMethod("getProgress", android.util.Pair::class.java)
                    val state = method.invoke(t, progressHolder) as? Int
                    if (state != null && progressHolder.second > 0f) {
                        trySend(ExportState.Progress(progressHolder.second.coerceIn(0f, 1f)))
                    }
                } catch (_: Exception) {
                    // ignore — progress optional
                }
                mainHandler.postDelayed(this, 300)
            }
        }
        mainHandler.post(progressRunnable)

        try {
            if (outputFile.exists()) outputFile.delete()
            t.start(composition, outputFile.absolutePath)
        } catch (e: Exception) {
            trySend(ExportState.Failed(e.message ?: "Failed to start export"))
            close()
        }

        awaitClose {
            mainHandler.removeCallbacksAndMessages(null)
            try {
                t.cancel()
            } catch (_: Exception) {
            }
            transformer = null
        }
    }

    fun cancel() {
        cancelled.set(true)
        try {
            transformer?.cancel()
        } catch (_: Exception) {
        }
    }

    private fun buildEditedItem(clip: Clip, config: ExportConfig): EditedMediaItem {
        val mediaItem = MediaItem.Builder()
            .setUri(Uri.parse(clip.mediaUri))
            .setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(clip.sourceStartUs / 1000)
                    .setEndPositionMs(
                        if (clip.sourceEndUs > 0) clip.sourceEndUs / 1000
                        else C_END
                    )
                    .build()
            )
            .build()

        val effects = Media3EffectMapper.toMedia3Effects(clip.effects).toMutableList()
        // Scale to target resolution
        effects.add(
            Presentation.createForHeight(config.resolution.height)
        )

        val builder = EditedMediaItem.Builder(mediaItem)
            .setEffects(Effects(emptyList(), effects))

        // Speed: Media3 1.4 applies via setSpeed on some versions; use removeAudio for mute.
        // Full speed control will use Effects / Composition in a follow-up if needed.
        if (clip.isMuted || clip.volume <= 0f) {
            builder.setRemoveAudio(true)
        }

        return builder.build()
    }

    companion object {
        private const val C_END = Long.MAX_VALUE / 2
    }
}
