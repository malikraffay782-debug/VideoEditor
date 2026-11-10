package com.videoeditor.timeline.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.videoeditor.core.model.Clip
import com.videoeditor.core.model.Track
import com.videoeditor.core.model.TrackType
import kotlin.math.roundToInt

private const val PX_PER_SECOND = 60f // zoom base: 60dp per second

@Composable
fun TimelineView(
    tracks: List<Track>,
    playheadUs: Long,
    selectedClipId: String?,
    onClipSelected: (String?) -> Unit,
    onClipMoved: (trackId: String, clipId: String, newStartUs: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val maxDurationUs = tracks
        .flatMap { it.clips }
        .maxOfOrNull { it.timelineEndUs }
        ?.coerceAtLeast(5_000_000L) ?: 10_000_000L

    val contentWidthDp = ((maxDurationUs / 1_000_000f) * PX_PER_SECOND).coerceAtLeast(300f)

    Column(modifier = modifier.background(Color(0xFF121212))) {
        // Ruler
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .horizontalScroll(scrollState)
                .background(Color(0xFF1A1A1A))
        ) {
            Row(modifier = Modifier.width(contentWidthDp.dp)) {
                val seconds = (maxDurationUs / 1_000_000L).toInt() + 1
                for (s in 0..seconds) {
                    Box(
                        modifier = Modifier
                            .width(PX_PER_SECOND.dp)
                            .height(24.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = "${s}s",
                            color = Color(0xFF888888),
                            fontSize = 10.sp,
                            modifier = Modifier.padding(start = 2.dp)
                        )
                    }
                }
            }
            // Playhead
            val playheadX = (playheadUs / 1_000_000f) * PX_PER_SECOND
            Box(
                modifier = Modifier
                    .offset { IntOffset(playheadX.dp.roundToPx(), 0) }
                    .width(2.dp)
                    .height(24.dp)
                    .background(Color(0xFFFF5252))
            )
        }

        // Tracks
        tracks.forEach { track ->
            TrackRow(
                track = track,
                contentWidthDp = contentWidthDp,
                selectedClipId = selectedClipId,
                onClipSelected = onClipSelected,
                onClipMoved = onClipMoved,
                scrollState = scrollState
            )
        }

        if (tracks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Import media to start editing", color = Color(0xFF888888), fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun TrackRow(
    track: Track,
    contentWidthDp: Float,
    selectedClipId: String?,
    onClipSelected: (String?) -> Unit,
    onClipMoved: (trackId: String, clipId: String, newStartUs: Long) -> Unit,
    scrollState: androidx.compose.foundation.ScrollState
) {
    val trackColor = when (track.type) {
        TrackType.VIDEO -> Color(0xFF1E3A5F)
        TrackType.AUDIO -> Color(0xFF1B4332)
        TrackType.IMAGE -> Color(0xFF3D2C5E)
        else -> Color(0xFF2A2A2A)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(Color(0xFF0D0D0D))
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Track label
        Box(
            modifier = Modifier
                .width(56.dp)
                .height(52.dp)
                .background(Color(0xFF1A1A1A)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = track.name.take(5),
                color = Color(0xFFB0B0B0),
                fontSize = 10.sp,
                maxLines = 1
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .height(52.dp)
                .horizontalScroll(scrollState)
        ) {
            Box(modifier = Modifier.width(contentWidthDp.dp).height(52.dp)) {
                track.clips.forEach { clip ->
                    ClipBlock(
                        clip = clip,
                        trackId = track.id,
                        isSelected = clip.id == selectedClipId,
                        trackColor = trackColor,
                        onSelected = { onClipSelected(clip.id) },
                        onMoved = { newStartUs -> onClipMoved(track.id, clip.id, newStartUs) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ClipBlock(
    clip: Clip,
    trackId: String,
    isSelected: Boolean,
    trackColor: Color,
    onSelected: () -> Unit,
    onMoved: (Long) -> Unit
) {
    val widthDp = ((clip.durationUs / 1_000_000f) / clip.speed.coerceAtLeast(0.1f) * PX_PER_SECOND)
        .coerceAtLeast(24f)
    val startDp = (clip.timelineStartUs / 1_000_000f) * PX_PER_SECOND

    var dragOffset by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .offset {
                val x = if (isDragging) (startDp + dragOffset).dp.roundToPx()
                else startDp.dp.roundToPx()
                IntOffset(x, 4)
            }
            .width(widthDp.dp)
            .height(44.dp)
            .background(trackColor, RoundedCornerShape(4.dp))
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = Color(0xFF4FC3F7),
                shape = RoundedCornerShape(4.dp)
            )
            .clickable { onSelected() }
            .pointerInput(clip.id) {
                detectDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = {
                        val newStartUs = ((startDp + dragOffset) / PX_PER_SECOND * 1_000_000f)
                            .roundToInt()
                            .toLong()
                            .coerceAtLeast(0L)
                        onMoved(newStartUs)
                        dragOffset = 0f
                        isDragging = false
                    },
                    onDragCancel = {
                        dragOffset = 0f
                        isDragging = false
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragOffset += dragAmount.x / density
                    }
                )
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = clip.displayName,
            color = Color.White,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 6.dp)
        )
    }
}
