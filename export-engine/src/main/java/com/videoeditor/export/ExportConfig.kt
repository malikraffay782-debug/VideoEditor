package com.videoeditor.export

enum class ExportResolution(val height: Int, val label: String) {
    P480(480, "480p"),
    P720(720, "720p"),
    P1080(1080, "1080p"),
    P2160(2160, "4K")
}

data class ExportConfig(
    val resolution: ExportResolution = ExportResolution.P720,
    val frameRate: Int = 30,
    /** 0 = auto bitrate */
    val bitrateMbps: Float = 0f,
    val includeAudio: Boolean = true
) {
    fun outputWidth(aspectWidth: Int = 16, aspectHeight: Int = 9): Int {
        return (resolution.height * aspectWidth) / aspectHeight
    }
}
