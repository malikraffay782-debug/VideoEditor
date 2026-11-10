package com.videoeditor.effects

import com.videoeditor.core.model.Effect
import com.videoeditor.core.model.EffectType

/**
 * Catalog of supported effects and default intensities.
 * Actual GPU application is handled at export / preview time.
 */
object EffectCatalog {

    data class EffectInfo(
        val type: EffectType,
        val displayName: String,
        val min: Float,
        val max: Float,
        val default: Float,
        /** true = 0 means neutral */
        val bipolar: Boolean
    )

    val all = listOf(
        EffectInfo(EffectType.BRIGHTNESS, "Brightness", -1f, 1f, 0f, true),
        EffectInfo(EffectType.CONTRAST, "Contrast", -1f, 1f, 0f, true),
        EffectInfo(EffectType.SATURATION, "Saturation", -1f, 1f, 0f, true),
        EffectInfo(EffectType.TEMPERATURE, "Temperature", -1f, 1f, 0f, true),
        EffectInfo(EffectType.BLUR, "Blur", 0f, 1f, 0f, false),
        EffectInfo(EffectType.VIGNETTE, "Vignette", 0f, 1f, 0f, false),
        EffectInfo(EffectType.SHARPEN, "Sharpen", 0f, 1f, 0f, false),
        EffectInfo(EffectType.OPACITY, "Opacity", 0f, 1f, 1f, false)
    )

    fun info(type: EffectType): EffectInfo =
        all.first { it.type == type }

    fun defaultEffect(type: EffectType): Effect {
        val i = info(type)
        return Effect(type = type, intensity = i.default)
    }
}
