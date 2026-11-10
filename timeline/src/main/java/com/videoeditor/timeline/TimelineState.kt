package com.videoeditor.timeline

import com.videoeditor.core.model.Clip
import com.videoeditor.core.model.Project
import com.videoeditor.core.model.Track
import com.videoeditor.core.model.TrackType

/**
 * Mutable timeline operations.
 * All edits are non-destructive (original media untouched).
 */
class TimelineState(initial: Project) {

    var project: Project = initial
        private set

    private val undoStack = ArrayDeque<Project>()
    private val redoStack = ArrayDeque<Project>()

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    private fun pushUndo() {
        undoStack.addLast(project)
        if (undoStack.size > 50) undoStack.removeFirst()
        redoStack.clear()
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        redoStack.addLast(project)
        project = undoStack.removeLast()
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        undoStack.addLast(project)
        project = redoStack.removeLast()
    }

    fun addClipToTrack(trackId: String, clip: Clip) {
        pushUndo()
        val tracks = project.tracks.map { track ->
            if (track.id == trackId) {
                val start = track.clips.maxOfOrNull { it.timelineEndUs } ?: 0L
                track.addClip(clip.withTimelineStart(start))
            } else track
        }
        project = project.withTracks(tracks).recalculateDuration()
    }

    fun addClipToNewVideoTrack(clip: Clip) {
        pushUndo()
        val track = Track(
            type = when (clip.mediaType) {
                com.videoeditor.core.model.MediaType.AUDIO -> TrackType.AUDIO
                com.videoeditor.core.model.MediaType.IMAGE -> TrackType.IMAGE
                else -> TrackType.VIDEO
            },
            name = when (clip.mediaType) {
                com.videoeditor.core.model.MediaType.AUDIO -> "Audio"
                com.videoeditor.core.model.MediaType.IMAGE -> "Image"
                else -> "Video"
            },
            clips = listOf(clip.withTimelineStart(0L))
        )
        project = project.withTracks(project.tracks + track).recalculateDuration()
    }

    fun removeClip(trackId: String, clipId: String) {
        pushUndo()
        val tracks = project.tracks.map { track ->
            if (track.id == trackId) track.removeClip(clipId) else track
        }.filter { it.clips.isNotEmpty() || it.type == TrackType.VIDEO }
        project = project.withTracks(tracks).recalculateDuration()
    }

    fun moveClip(trackId: String, clipId: String, newTimelineStartUs: Long) {
        pushUndo()
        val tracks = project.tracks.map { track ->
            if (track.id == trackId) {
                track.updateClip(clipId) { it.withTimelineStart(newTimelineStartUs.coerceAtLeast(0L)) }
            } else track
        }
        project = project.withTracks(tracks).recalculateDuration()
    }

    fun trimClip(
        trackId: String,
        clipId: String,
        newSourceStartUs: Long,
        newSourceEndUs: Long
    ) {
        pushUndo()
        val tracks = project.tracks.map { track ->
            if (track.id == trackId) {
                track.updateClip(clipId) { clip ->
                    val start = newSourceStartUs.coerceIn(0L, clip.sourceEndUs.takeIf { it > 0 } ?: clip.durationUs)
                    val end = newSourceEndUs.coerceAtLeast(start + 100_000L) // min 0.1s
                    val newDuration = end - start
                    clip.trimmed(start, end, newDuration)
                }
            } else track
        }
        project = project.withTracks(tracks).recalculateDuration()
    }

    fun splitClip(trackId: String, clipId: String, splitAtTimelineUs: Long) {
        val track = project.tracks.find { it.id == trackId } ?: return
        val clip = track.clips.find { it.id == clipId } ?: return
        if (splitAtTimelineUs <= clip.timelineStartUs || splitAtTimelineUs >= clip.timelineEndUs) return

        pushUndo()
        val relativeUs = ((splitAtTimelineUs - clip.timelineStartUs) * clip.speed).toLong()
        val leftDuration = relativeUs
        val rightDuration = clip.durationUs - relativeUs

        val left = clip.copy(
            id = java.util.UUID.randomUUID().toString(),
            sourceEndUs = clip.sourceStartUs + leftDuration,
            durationUs = leftDuration
        )
        val right = clip.copy(
            id = java.util.UUID.randomUUID().toString(),
            sourceStartUs = clip.sourceStartUs + leftDuration,
            durationUs = rightDuration,
            timelineStartUs = splitAtTimelineUs
        )

        val newClips = track.clips.flatMap { c ->
            if (c.id == clipId) listOf(left, right) else listOf(c)
        }
        val tracks = project.tracks.map {
            if (it.id == trackId) it.replaceClips(newClips) else it
        }
        project = project.withTracks(tracks).recalculateDuration()
    }

    fun ensureDefaultTracks() {
        if (project.tracks.isNotEmpty()) return
        pushUndo()
        val video = Track(type = TrackType.VIDEO, name = "Video 1")
        val audio = Track(type = TrackType.AUDIO, name = "Audio 1")
        project = project.withTracks(listOf(video, audio))
    }

    fun applyEffect(trackId: String, clipId: String, effect: com.videoeditor.core.model.Effect) {
        pushUndo()
        val tracks = project.tracks.map { track ->
            if (track.id == trackId) {
                track.updateClip(clipId) { it.withEffect(effect) }
            } else track
        }
        project = project.withTracks(tracks)
    }

    fun removeEffect(trackId: String, clipId: String, type: com.videoeditor.core.model.EffectType) {
        pushUndo()
        val tracks = project.tracks.map { track ->
            if (track.id == trackId) {
                track.updateClip(clipId) { it.removeEffect(type) }
            } else track
        }
        project = project.withTracks(tracks)
    }

    fun setClipVolume(trackId: String, clipId: String, volume: Float) {
        pushUndo()
        val tracks = project.tracks.map { track ->
            if (track.id == trackId) {
                track.updateClip(clipId) { it.withVolume(volume) }
            } else track
        }
        project = project.withTracks(tracks)
    }

    fun setClipMuted(trackId: String, clipId: String, muted: Boolean) {
        pushUndo()
        val tracks = project.tracks.map { track ->
            if (track.id == trackId) {
                track.updateClip(clipId) { it.copy(isMuted = muted) }
            } else track
        }
        project = project.withTracks(tracks)
    }

    fun setTransitionIn(trackId: String, clipId: String, transition: com.videoeditor.core.model.Transition) {
        pushUndo()
        val tracks = project.tracks.map { track ->
            if (track.id == trackId) {
                track.updateClip(clipId) { it.withTransitionIn(transition) }
            } else track
        }
        project = project.withTracks(tracks)
    }

    fun setSpeed(trackId: String, clipId: String, speed: Float) {
        pushUndo()
        val tracks = project.tracks.map { track ->
            if (track.id == trackId) {
                track.updateClip(clipId) { it.copy(speed = speed.coerceIn(0.25f, 4f)) }
            } else track
        }
        project = project.withTracks(tracks).recalculateDuration()
    }

}
