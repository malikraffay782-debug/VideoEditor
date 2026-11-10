package com.videoeditor.core

/**
 * Adaptive performance modes for low-end first design.
 * Performance is the default on devices with limited resources.
 */
enum class PerformanceMode {
    /** 360p/480p proxy, minimal effects, 3D disabled or 2.5D only */
    PERFORMANCE,

    /** Up to 720p proxy when RAM/CPU allow */
    BALANCED,

    /** Higher preview resolution + advanced effects on capable hardware */
    HIGH_QUALITY
}
