package com.videoeditor.audio

import com.videoeditor.core.model.Clip

/**
 * Audio helpers. Non-destructive — only mutates Clip copies.
 */
object AudioControls {

    fun setVolume(clip: Clip, volume: Float): Clip =
        clip.withVolume(volume.coerceIn(0f, 2f))

    fun setMuted(clip: Clip, muted: Boolean): Clip =
        clip.copy(isMuted = muted, volume = if (muted) 0f else clip.volume.coerceAtLeast(0.01f))

    fun setFadeIn(clip: Clip, fadeUs: Long): Clip =
        clip.copy(fadeInUs = fadeUs.coerceIn(0L, clip.durationUs / 2))

    fun setFadeOut(clip: Clip, fadeUs: Long): Clip =
        clip.copy(fadeOutUs = fadeUs.coerceIn(0L, clip.durationUs / 2))

    /** Effective volume at a given time within the clip (0..durationUs). */
    fun volumeAt(clip: Clip, timeInClipUs: Long): Float {
        if (clip.isMuted) return 0f
        var v = clip.volume
        if (clip.fadeInUs > 0 && timeInClipUs < clip.fadeInUs) {
            v *= (timeInClipUs.toFloat() / clip.fadeInUs)
        }
        val fadeOutStart = clip.durationUs - clip.fadeOutUs
        if (clip.fadeOutUs > 0 && timeInClipUs > fadeOutStart) {
            val remaining = (clip.durationUs - timeInClipUs).toFloat()
            v *= (remaining / clip.fadeOutUs).coerceIn(0f, 1f)
        }
        return v.coerceIn(0f, 2f)
    }
}
