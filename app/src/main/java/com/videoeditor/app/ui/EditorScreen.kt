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
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.media3.ui.PlayerView
import com.videoeditor.core.model.Project
import com.videoeditor.media.MediaImportHelper
import com.videoeditor.media.PlayerController
import com.videoeditor.timeline.TimelineState
import com.videoeditor.timeline.ui.TimelineView
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun EditorScreen(
    project: Project,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val timelineState = remember(project.id) { TimelineState(project) }
    var timelineVersion by remember { mutableStateOf(0) } // force recomposition after edits

    val playerController = remember { PlayerController(context) }
    val isPlaying by playerController.isPlaying.collectAsState()
    val positionUs by playerController.positionUs.collectAsState()

    var selectedClipId by remember { mutableStateOf<String?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    // Keep playhead updated while playing
    LaunchedEffect(isPlaying) {
        while (isActive && isPlaying) {
            playerController.updatePosition()
            delay(50)
        }
    }

    DisposableEffect(Unit) {
        onDispose { playerController.release() }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        // Persist permission
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
                // Load first video into player for preview
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D))
    ) {
        // Top bar
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
            Text(
                text = timelineState.project.name,
                color = Color.White,
                fontSize = 16.sp
            )
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

        // Player area
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

        // Transport controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(Color(0xFF1A1A1A))
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = { playerController.togglePlayPause() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4FC3F7))
            ) {
                Text(if (isPlaying) "Pause" else "Play", color = Color.Black)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Button(
                onClick = {
                    importLauncher.launch(arrayOf("video/*", "audio/*", "image/*"))
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A2A))
            ) {
                Text("Import", color = Color.White)
            }
            Spacer(modifier = Modifier.width(16.dp))
            if (selectedClipId != null) {
                Button(
                    onClick = {
                        val track = timelineState.project.tracks.find { t ->
                            t.clips.any { it.id == selectedClipId }
                        }
                        if (track != null && selectedClipId != null) {
                            timelineState.removeClip(track.id, selectedClipId!!)
                            selectedClipId = null
                            timelineVersion++
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5C1A1A))
                ) {
                    Text("Delete", color = Color.White)
                }
            }
        }

        statusMessage?.let {
            Text(
                text = it,
                color = Color(0xFF80CBC4),
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }

        // Timeline
        // Force refresh via timelineVersion
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
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )
    }
}
