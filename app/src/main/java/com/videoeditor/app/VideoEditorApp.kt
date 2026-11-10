package com.videoeditor.app

import android.app.Application
import com.videoeditor.core.PerformanceDetector
import com.videoeditor.core.PerformanceMode

class VideoEditorApp : Application() {

    lateinit var performanceMode: PerformanceMode
        private set

    override fun onCreate() {
        super.onCreate()
        val capabilities = PerformanceDetector.detect(this)
        performanceMode = capabilities.recommendedMode
        // Low-end first: never force high quality on weak devices
    }
}
