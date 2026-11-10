package com.videoeditor.core

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import java.io.File
import java.io.RandomAccessFile

/**
 * Detects device capabilities and recommends a PerformanceMode.
 * Prioritizes low-end devices (2GB RAM, older CPUs).
 */
object PerformanceDetector {

    data class DeviceCapabilities(
        val totalRamMb: Long,
        val availableRamMb: Long,
        val cpuCores: Int,
        val isLowRamDevice: Boolean,
        val recommendedMode: PerformanceMode,
        val maxPreviewHeight: Int,
        val allow3d: Boolean,
        val allowAdvancedEffects: Boolean
    )

    fun detect(context: Context): DeviceCapabilities {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)

        val totalRamMb = memInfo.totalMem / (1024 * 1024)
        val availableRamMb = memInfo.availMem / (1024 * 1024)
        val isLowRamDevice = activityManager.isLowRamDevice || totalRamMb <= 2048
        val cpuCores = Runtime.getRuntime().availableProcessors()

        val recommendedMode = when {
            isLowRamDevice || totalRamMb <= 2048 || cpuCores <= 4 -> PerformanceMode.PERFORMANCE
            totalRamMb <= 4096 -> PerformanceMode.BALANCED
            else -> PerformanceMode.HIGH_QUALITY
        }

        val maxPreviewHeight = when (recommendedMode) {
            PerformanceMode.PERFORMANCE -> 480
            PerformanceMode.BALANCED -> 720
            PerformanceMode.HIGH_QUALITY -> 1080
        }

        val allow3d = recommendedMode != PerformanceMode.PERFORMANCE
        val allowAdvancedEffects = recommendedMode == PerformanceMode.HIGH_QUALITY

        return DeviceCapabilities(
            totalRamMb = totalRamMb,
            availableRamMb = availableRamMb,
            cpuCores = cpuCores,
            isLowRamDevice = isLowRamDevice,
            recommendedMode = recommendedMode,
            maxPreviewHeight = maxPreviewHeight,
            allow3d = allow3d,
            allowAdvancedEffects = allowAdvancedEffects
        )
    }

    /**
     * Simple CPU frequency probe (best-effort, not available on all devices).
     */
    fun getMaxCpuFreqMhz(): Int {
        return try {
            val file = File("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq")
            if (file.exists()) {
                RandomAccessFile(file, "r").use {
                    (it.readLine().toLong() / 1000).toInt()
                }
            } else {
                -1
            }
        } catch (_: Exception) {
            -1
        }
    }
}
