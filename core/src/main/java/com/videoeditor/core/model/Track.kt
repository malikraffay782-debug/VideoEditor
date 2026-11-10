package com.videoeditor.core.model

import java.util.UUID

enum class TrackType {
    VIDEO,
    AUDIO,
    TEXT,
    IMAGE,
    OVERLAY
}

data class Track(
    val id: String = UUID.randomUUID().toString(),
    val type: TrackType,
    val name: String,
    val clips: List<Clip> = emptyList(),
    val isLocked: Boolean = false,
    val isMuted: Boolean = false,
    val isVisible: Boolean = true
) {
    fun addClip(clip: Clip): Track = copy(clips = clips + clip)

    fun removeClip(clipId: String): Track = copy(clips = clips.filter { it.id != clipId })

    fun updateClip(clipId: String, transform: (Clip) -> Clip): Track =
        copy(clips = clips.map { if (it.id == clipId) transform(it) else it })

    fun replaceClips(newClips: List<Clip>): Track = copy(clips = newClips)
}
