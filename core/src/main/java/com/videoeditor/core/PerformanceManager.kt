package com.videoeditor.core

import android.content.Context

/**
 * Runtime performance mode controller.
 * Defaults to device-recommended mode (Performance on low-end).
 * User can override, but low-end devices stay constrained.
 */
object PerformanceManager {

    @Volatile
    private var currentMode: PerformanceMode = PerformanceMode.PERFORMANCE

    @Volatile
    private var capabilities: PerformanceDetector.DeviceCapabilities? = null

    fun init(context: Context) {
        val caps = PerformanceDetector.detect(context)
        capabilities = caps
        currentMode = caps.recommendedMode
    }

    fun getMode(): PerformanceMode = currentMode

    fun getCapabilities(): PerformanceDetector.DeviceCapabilities? = capabilities

    /**
     * User override. On very low-end devices, HIGH_QUALITY is clamped to BALANCED.
     */
    fun setMode(mode: PerformanceMode) {
        val caps = capabilities
        currentMode = when {
            caps == null -> mode
            caps.isLowRamDevice && mode == PerformanceMode.HIGH_QUALITY -> PerformanceMode.BALANCED
            else -> mode
        }
    }

    fun maxPreviewHeight(): Int = when (currentMode) {
        PerformanceMode.PERFORMANCE -> 480
        PerformanceMode.BALANCED -> 720
        PerformanceMode.HIGH_QUALITY -> 1080
    }

    fun allow3d(): Boolean = currentMode != PerformanceMode.PERFORMANCE

    fun allowAdvancedEffects(): Boolean = currentMode == PerformanceMode.HIGH_QUALITY

    /** Prefer proxy for timeline/preview when true */
    fun useProxyForPreview(): Boolean = currentMode != PerformanceMode.HIGH_QUALITY

    /** Max concurrent decode / effect operations */
    fun maxConcurrentJobs(): Int = when (currentMode) {
        PerformanceMode.PERFORMANCE -> 1
        PerformanceMode.BALANCED -> 2
        PerformanceMode.HIGH_QUALITY -> 3
    }

    fun playerBufferMs(): Int = when (currentMode) {
        PerformanceMode.PERFORMANCE -> 1500
        PerformanceMode.BALANCED -> 2500
        PerformanceMode.HIGH_QUALITY -> 4000
    }
}
