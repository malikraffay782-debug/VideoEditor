package com.videoeditor.core.model

import java.util.UUID

enum class EffectType {
    BRIGHTNESS,
    CONTRAST,
    SATURATION,
    TEMPERATURE,
    BLUR,
    VIGNETTE,
    SHARPEN,
    OPACITY
}

/**
 * Non-destructive effect applied to a clip.
 * Values are normalized where applicable (e.g. brightness -1..1).
 */
data class Effect(
    val id: String = UUID.randomUUID().toString(),
    val type: EffectType,
    /** Primary parameter, meaning depends on type */
    val intensity: Float = 0f,
    val enabled: Boolean = true
) {
    fun withIntensity(value: Float): Effect = copy(intensity = value)
}

enum class TransitionType {
    NONE,
    FADE,
    DISSOLVE,
    SLIDE_LEFT,
    SLIDE_RIGHT,
    WIPE
}

data class Transition(
    val id: String = UUID.randomUUID().toString(),
    val type: TransitionType = TransitionType.NONE,
    /** Duration in microseconds */
    val durationUs: Long = 500_000L
)
