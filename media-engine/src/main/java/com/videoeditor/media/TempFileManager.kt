package com.videoeditor.media

import android.content.Context
import java.io.File

/**
 * Manages temporary files (proxies, export intermediates, thumbnails).
 * Never touches original imported media.
 */
class TempFileManager(context: Context) {

    private val root = File(context.cacheDir, "videoeditor_tmp").apply { mkdirs() }
    private val proxiesDir = File(root, "proxies").apply { mkdirs() }
    private val thumbsDir = File(root, "thumbs").apply { mkdirs() }
    private val exportsDir = File(root, "exports").apply { mkdirs() }

    fun proxyFile(clipId: String, height: Int): File =
        File(proxiesDir, "${clipId}_${height}p.mp4")

    fun thumbFile(clipId: String): File =
        File(thumbsDir, "${clipId}.jpg")

    fun exportFile(name: String = "export_${System.currentTimeMillis()}.mp4"): File =
        File(exportsDir, name)

    fun proxiesDir(): File = proxiesDir
    fun thumbsDir(): File = thumbsDir
    fun exportsDir(): File = exportsDir

    /**
     * Delete files older than [maxAgeMs] under temp root.
     * Safe to call on background thread.
     */
    fun cleanup(maxAgeMs: Long = 7L * 24 * 60 * 60 * 1000) {
        val cutoff = System.currentTimeMillis() - maxAgeMs
        root.walkBottomUp().forEach { file ->
            if (file.isFile && file.lastModified() < cutoff) {
                file.delete()
            }
        }
    }

    fun clearAllProxies() {
        proxiesDir.listFiles()?.forEach { it.delete() }
    }

    fun storageUsedBytes(): Long {
        var total = 0L
        root.walkTopDown().forEach { if (it.isFile) total += it.length() }
        return total
    }
}
