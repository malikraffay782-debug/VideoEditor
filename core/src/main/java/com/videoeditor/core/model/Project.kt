package com.videoeditor.core.model

import java.util.UUID

data class Project(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val tracks: List<Track> = emptyList(),
    val durationUs: Long = 0L,
    val frameRate: Int = 30,
    val width: Int = 1280,
    val height: Int = 720
) {
    fun withTracks(newTracks: List<Track>): Project =
        copy(tracks = newTracks, updatedAt = System.currentTimeMillis())

    fun recalculateDuration(): Project {
        val maxEnd = tracks
            .flatMap { it.clips }
            .maxOfOrNull { it.timelineEndUs } ?: 0L
        return copy(durationUs = maxEnd, updatedAt = System.currentTimeMillis())
    }
}
