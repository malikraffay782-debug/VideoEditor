package com.videoeditor.core.model

import java.util.UUID

/**
 * Non-destructive clip representation.
 * Original media path is never modified.
 */
data class Clip(
    val id: String = UUID.randomUUID().toString(),
    val mediaUri: String,
    /** Low-res proxy for timeline/preview; null = use original */
    val proxyUri: String? = null,
    val mediaType: MediaType,
    val displayName: String,
    /** Source start time in microseconds (trim in point) */
    val sourceStartUs: Long = 0L,
    /** Source end time in microseconds (trim out point). 0 = full duration. */
    val sourceEndUs: Long = 0L,
    /** Duration of the clip on the timeline after trim, in microseconds */
    val durationUs: Long,
    /** Position on the timeline in microseconds */
    val timelineStartUs: Long = 0L,
    val speed: Float = 1.0f,
    val volume: Float = 1.0f,
    val fadeInUs: Long = 0L,
    val fadeOutUs: Long = 0L,
    val rotation: Int = 0,
    val isMuted: Boolean = false,
    val effects: List<Effect> = emptyList(),
    /** Transition applied at the start of this clip */
    val transitionIn: Transition = Transition(type = TransitionType.NONE)
) {
    val timelineEndUs: Long
        get() = timelineStartUs + (durationUs / speed.coerceAtLeast(0.1f)).toLong()

    fun withTimelineStart(startUs: Long): Clip = copy(timelineStartUs = startUs)

    fun trimmed(newSourceStartUs: Long, newSourceEndUs: Long, newDurationUs: Long): Clip =
        copy(
            sourceStartUs = newSourceStartUs,
            sourceEndUs = newSourceEndUs,
            durationUs = newDurationUs
        )

    fun withVolume(v: Float): Clip = copy(volume = v.coerceIn(0f, 2f))

    fun withEffect(effect: Effect): Clip {
        val others = effects.filter { it.type != effect.type }
        return copy(effects = others + effect)
    }

    fun removeEffect(type: EffectType): Clip =
        copy(effects = effects.filter { it.type != type })

    fun withTransitionIn(t: Transition): Clip = copy(transitionIn = t)

    fun withProxy(uri: String?): Clip = copy(proxyUri = uri)

    /** URI to use for preview/timeline (proxy preferred when available) */
    fun previewUri(preferProxy: Boolean = true): String =
        if (preferProxy && proxyUri != null) proxyUri else mediaUri
}
