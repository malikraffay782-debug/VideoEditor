package com.videoeditor.app

import android.app.Application
import com.videoeditor.core.PerformanceManager
import com.videoeditor.core.PerformanceMode
import com.videoeditor.media.TempFileManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class VideoEditorApp : Application() {

    lateinit var performanceMode: PerformanceMode
        private set

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    lateinit var tempFiles: TempFileManager
        private set

    override fun onCreate() {
        super.onCreate()
        PerformanceManager.init(this)
        performanceMode = PerformanceManager.getMode()
        tempFiles = TempFileManager(this)
        // Background cleanup of old temp/proxy files — never blocks UI
        appScope.launch {
            try {
                tempFiles.cleanup()
            } catch (_: Exception) {
            }
        }
    }
}
