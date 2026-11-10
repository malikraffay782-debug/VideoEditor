package com.videoeditor.effects

import androidx.media3.common.Effect
import androidx.media3.effect.Brightness
import androidx.media3.effect.Contrast
import androidx.media3.effect.RgbMatrix
import androidx.media3.effect.ScaleAndRotateTransformation
import com.videoeditor.core.model.Effect as AppEffect
import com.videoeditor.core.model.EffectType

/**
 * Maps app Effect models to Media3 Effect instances for Transformer export.
 * Only effects that Media3 supports cleanly are applied; others are skipped safely.
 */
object Media3EffectMapper {

    fun toMedia3Effects(effects: List<AppEffect>): List<Effect> {
        val result = mutableListOf<Effect>()
        for (e in effects) {
            if (!e.enabled) continue
            when (e.type) {
                EffectType.BRIGHTNESS -> {
                    // Media3 Brightness expects -1..1
                    result.add(Brightness(e.intensity.coerceIn(-1f, 1f)))
                }
                EffectType.CONTRAST -> {
                    result.add(Contrast(e.intensity.coerceIn(-1f, 1f)))
                }
                EffectType.SATURATION -> {
                    // Approximate saturation via simple RGB matrix
                    val s = (1f + e.intensity).coerceIn(0f, 2f)
                    val inv = 1f - s
                    val r = 0.213f * inv
                    val g = 0.715f * inv
                    val b = 0.072f * inv
                    val matrix = floatArrayOf(
                        r + s, g, b, 0f,
                        r, g + s, b, 0f,
                        r, g, b + s, 0f,
                        0f, 0f, 0f, 1f
                    )
                    result.add(RgbMatrix { _ -> matrix })
                }
                EffectType.OPACITY -> {
                    // Opacity is handled via composition; skip matrix here
                }
                EffectType.TEMPERATURE,
                EffectType.BLUR,
                EffectType.VIGNETTE,
                EffectType.SHARPEN -> {
                    // Advanced effects require custom GL shaders; deferred to later phase
                    // Do not crash — simply skip unsupported ones in export
                }
            }
        }
        return result
    }
}
