package com.videoeditor.app.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import com.videoeditor.core.model.Effect
import com.videoeditor.core.model.Project
import com.videoeditor.core.model.Transition
import com.videoeditor.core.model.TransitionType
import com.videoeditor.effects.EffectCatalog
import com.videoeditor.export.ExportConfig
import com.videoeditor.export.ExportResolution
import com.videoeditor.export.ExportState
import com.videoeditor.export.VideoExporter
import com.videoeditor.media.MediaImportHelper
import com.videoeditor.media.PlayerController
import com.videoeditor.timeline.TimelineState
import com.videoeditor.timeline.ui.TimelineView
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

private enum class BottomPanel { None, Effects, Audio, Export }

@OptIn(UnstableApi::class)
@Composable
fun EditorScreen(
    project: Project,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val timelineState = remember(project.id) { TimelineState(project) }
    var timelineVersion by remember { mutableStateOf(0) }

    val playerController = remember { PlayerController(context) }
    val isPlaying by playerController.isPlaying.collectAsState()
    val positionUs by playerController.positionUs.collectAsState()

    var selectedClipId by remember { mutableStateOf<String?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var bottomPanel by remember { mutableStateOf(BottomPanel.None) }

    val exporter = remember { VideoExporter(context) }
    var exportState by remember { mutableStateOf<ExportState>(ExportState.Idle) }
    var exportProgress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(isPlaying) {
        while (isActive && isPlaying) {
            playerController.updatePosition()
            delay(50)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            playerController.release()
            exporter.cancel()
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: Exception) {
        }
        scope.launch {
            val clip = MediaImportHelper.createClipFromUri(context, uri)
            if (clip != null) {
                timelineState.addClipToNewVideoTrack(clip)
                timelineVersion++
                if (clip.mediaType == com.videoeditor.core.model.MediaType.VIDEO ||
                    clip.mediaType == com.videoeditor.core.model.MediaType.AUDIO
                ) {
                    playerController.setMedia(clip.mediaUri)
                }
                statusMessage = "Imported: ${clip.displayName}"
            } else {
                statusMessage = "Could not import media"
            }
        }
    }

    fun findSelected(): Pair<String, com.videoeditor.core.model.Clip>? {
        val id = selectedClipId ?: return null
        for (t in timelineState.project.tracks) {
            val c = t.clips.find { it.id == id }
            if (c != null) return t.id to c
        }
        return null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(Color(0xFF1A1A1A))
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = onBack) {
                Text("Back", color = Color(0xFF4FC3F7))
            }
            Text(text = timelineState.project.name, color = Color.White, fontSize = 16.sp)
            Row {
                TextButton(
                    onClick = {
                        if (timelineState.canUndo) {
                            timelineState.undo()
                            timelineVersion++
                        }
                    },
                    enabled = timelineState.canUndo
                ) {
                    Text("Undo", color = if (timelineState.canUndo) Color(0xFFB0B0B0) else Color(0xFF444444))
                }
                TextButton(
                    onClick = {
                        if (timelineState.canRedo) {
                            timelineState.redo()
                            timelineVersion++
                        }
                    },
                    enabled = timelineState.canRedo
                ) {
                    Text("Redo", color = if (timelineState.canRedo) Color(0xFFB0B0B0) else Color(0xFF444444))
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = playerController.player
                        useController = false
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(Color(0xFF1A1A1A))
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SmallBtn(if (isPlaying) "Pause" else "Play", Color(0xFF4FC3F7), Color.Black) {
                playerController.togglePlayPause()
            }
            SmallBtn("Import", Color(0xFF2A2A2A), Color.White) {
                importLauncher.launch(arrayOf("video/*", "audio/*", "image/*"))
            }
            if (selectedClipId != null) {
                SmallBtn("Delete", Color(0xFF5C1A1A), Color.White) {
                    val sel = findSelected()
                    if (sel != null) {
                        timelineState.removeClip(sel.first, sel.second.id)
                        selectedClipId = null
                        timelineVersion++
                    }
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            SmallBtn("FX", if (bottomPanel == BottomPanel.Effects) Color(0xFF4FC3F7) else Color(0xFF2A2A2A),
                if (bottomPanel == BottomPanel.Effects) Color.Black else Color.White) {
                bottomPanel = if (bottomPanel == BottomPanel.Effects) BottomPanel.None else BottomPanel.Effects
            }
            SmallBtn("Audio", if (bottomPanel == BottomPanel.Audio) Color(0xFF4FC3F7) else Color(0xFF2A2A2A),
                if (bottomPanel == BottomPanel.Audio) Color.Black else Color.White) {
                bottomPanel = if (bottomPanel == BottomPanel.Audio) BottomPanel.None else BottomPanel.Audio
            }
            SmallBtn("Export", if (bottomPanel == BottomPanel.Export) Color(0xFF4FC3F7) else Color(0xFF2A2A2A),
                if (bottomPanel == BottomPanel.Export) Color.Black else Color.White) {
                bottomPanel = if (bottomPanel == BottomPanel.Export) BottomPanel.None else BottomPanel.Export
            }
        }

        statusMessage?.let {
            Text(text = it, color = Color(0xFF80CBC4), fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp))
        }

        when (bottomPanel) {
            BottomPanel.Effects -> EffectsPanel(
                selected = findSelected(),
                onApply = { trackId, clipId, effect ->
                    timelineState.applyEffect(trackId, clipId, effect)
                    timelineVersion++
                    statusMessage = "Applied ${effect.type}"
                },
                onTransition = { trackId, clipId, t ->
                    timelineState.setTransitionIn(trackId, clipId, t)
                    timelineVersion++
                    statusMessage = "Transition: ${t.type}"
                }
            )
            BottomPanel.Audio -> AudioPanel(
                selected = findSelected(),
                onVolume = { trackId, clipId, v ->
                    timelineState.setClipVolume(trackId, clipId, v)
                    timelineVersion++
                },
                onMute = { trackId, clipId, m ->
                    timelineState.setClipMuted(trackId, clipId, m)
                    timelineVersion++
                },
                onSpeed = { trackId, clipId, s ->
                    timelineState.setSpeed(trackId, clipId, s)
                    timelineVersion++
                }
            )
            BottomPanel.Export -> ExportPanel(
                exportState = exportState,
                progress = exportProgress,
                onStart = { res ->
                    scope.launch {
                        val outDir = File(context.cacheDir, "exports").apply { mkdirs() }
                        val out = File(outDir, "export_${System.currentTimeMillis()}.mp4")
                        val config = ExportConfig(resolution = res)
                        exportState = ExportState.Progress(0f)
                        exporter.export(timelineState.project, config, out).collect { state ->
                            exportState = state
                            if (state is ExportState.Progress) exportProgress = state.percent
                            when (state) {
                                is ExportState.Completed -> statusMessage = "Exported: ${state.outputPath}"
                                is ExportState.Failed -> statusMessage = "Export failed: ${state.message}"
                                is ExportState.Cancelled -> statusMessage = "Export cancelled"
                                else -> {}
                            }
                        }
                    }
                },
                onCancel = {
                    exporter.cancel()
                    exportState = ExportState.Cancelled
                }
            )
            BottomPanel.None -> {}
        }

        val tracks = remember(timelineVersion) { timelineState.project.tracks }
        TimelineView(
            tracks = tracks,
            playheadUs = positionUs,
            selectedClipId = selectedClipId,
            onClipSelected = { selectedClipId = it },
            onClipMoved = { trackId, clipId, newStart ->
                timelineState.moveClip(trackId, clipId, newStart)
                timelineVersion++
            },
            modifier = Modifier.fillMaxWidth().weight(1f)
        )
    }
}

@Composable
private fun SmallBtn(label: String, bg: Color, fg: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = bg),
        modifier = Modifier.height(36.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 0.dp)
    ) {
        Text(label, color = fg, fontSize = 12.sp)
    }
}

@Composable
private fun EffectsPanel(
    selected: Pair<String, com.videoeditor.core.model.Clip>?,
    onApply: (String, String, Effect) -> Unit,
    onTransition: (String, String, Transition) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().background(Color(0xFF151515)).padding(8.dp)) {
        if (selected == null) {
            Text("Select a clip to apply effects", color = Color(0xFF888888), fontSize = 13.sp)
            return
        }
        val (trackId, clip) = selected
        Text("Effects — ${clip.displayName}", color = Color.White, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(EffectCatalog.all) { info ->
                val current = clip.effects.find { it.type == info.type }
                Button(
                    onClick = {
                        val intensity = if (info.bipolar) 0.3f else 0.5f
                        onApply(trackId, clip.id, Effect(type = info.type, intensity = intensity))
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (current != null) Color(0xFF4FC3F7) else Color(0xFF2A2A2A)
                    ),
                    modifier = Modifier.height(34.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp)
                ) {
                    Text(info.displayName, color = if (current != null) Color.Black else Color.White, fontSize = 11.sp)
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text("Transition In", color = Color(0xFFB0B0B0), fontSize = 12.sp)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val types = listOf(
                TransitionType.NONE to "None",
                TransitionType.FADE to "Fade",
                TransitionType.DISSOLVE to "Dissolve",
                TransitionType.SLIDE_LEFT to "Slide L",
                TransitionType.SLIDE_RIGHT to "Slide R"
            )
            items(types) { (type, label) ->
                val active = clip.transitionIn.type == type
                Button(
                    onClick = { onTransition(trackId, clip.id, Transition(type = type, durationUs = 500_000L)) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (active) Color(0xFF4FC3F7) else Color(0xFF2A2A2A)
                    ),
                    modifier = Modifier.height(32.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp)
                ) {
                    Text(label, color = if (active) Color.Black else Color.White, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun AudioPanel(
    selected: Pair<String, com.videoeditor.core.model.Clip>?,
    onVolume: (String, String, Float) -> Unit,
    onMute: (String, String, Boolean) -> Unit,
    onSpeed: (String, String, Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().background(Color(0xFF151515)).padding(8.dp)) {
        if (selected == null) {
            Text("Select a clip for audio controls", color = Color(0xFF888888), fontSize = 13.sp)
            return
        }
        val (trackId, clip) = selected
        Text("Audio — ${clip.displayName}", color = Color.White, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text("Volume: ${"%.0f".format(clip.volume * 100)}%", color = Color(0xFFB0B0B0), fontSize = 12.sp)
        Slider(
            value = clip.volume.coerceIn(0f, 2f),
            onValueChange = { onVolume(trackId, clip.id, it) },
            valueRange = 0f..2f,
            colors = SliderDefaults.colors(thumbColor = Color(0xFF4FC3F7), activeTrackColor = Color(0xFF4FC3F7))
        )
        Text("Speed: ${"%.2f".format(clip.speed)}x", color = Color(0xFFB0B0B0), fontSize = 12.sp)
        Slider(
            value = clip.speed.coerceIn(0.25f, 4f),
            onValueChange = { onSpeed(trackId, clip.id, it) },
            valueRange = 0.25f..4f,
            colors = SliderDefaults.colors(thumbColor = Color(0xFF80CBC4), activeTrackColor = Color(0xFF80CBC4))
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SmallBtn(if (clip.isMuted) "Unmute" else "Mute", Color(0xFF2A2A2A), Color.White) {
                onMute(trackId, clip.id, !clip.isMuted)
            }
        }
    }
}

@Composable
private fun ExportPanel(
    exportState: ExportState,
    progress: Float,
    onStart: (ExportResolution) -> Unit,
    onCancel: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().background(Color(0xFF151515)).padding(8.dp)) {
        Text("Export", color = Color.White, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(ExportResolution.P480, ExportResolution.P720, ExportResolution.P1080).forEach { res ->
                SmallBtn(res.label, Color(0xFF2A2A2A), Color.White) {
                    if (exportState !is ExportState.Progress) onStart(res)
                }
            }
            if (exportState is ExportState.Progress) {
                SmallBtn("Cancel", Color(0xFF5C1A1A), Color.White, onCancel)
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        when (exportState) {
            is ExportState.Progress -> {
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF4FC3F7),
                    trackColor = Color(0xFF333333)
                )
                Text("Exporting… ${(progress * 100).toInt()}%", color = Color(0xFFB0B0B0), fontSize = 12.sp)
            }
            is ExportState.Completed -> Text("Done: ${exportState.outputPath}", color = Color(0xFF80CBC4), fontSize = 11.sp)
            is ExportState.Failed -> Text("Failed: ${exportState.message}", color = Color(0xFFCF6679), fontSize = 12.sp)
            is ExportState.Cancelled -> Text("Cancelled", color = Color(0xFFB0B0B0), fontSize = 12.sp)
            else -> Text("Choose resolution and start export", color = Color(0xFF888888), fontSize = 12.sp)
        }
    }
}
