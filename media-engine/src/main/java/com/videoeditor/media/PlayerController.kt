package com.videoeditor.media

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import com.videoeditor.core.PerformanceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Lightweight ExoPlayer wrapper tuned by PerformanceManager.
 * Low buffer on Performance mode to reduce RAM pressure.
 */
class PlayerController(context: Context) {

    private val appContext = context.applicationContext

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _positionUs = MutableStateFlow(0L)
    val positionUs: StateFlow<Long> = _positionUs.asStateFlow()

    private val _durationUs = MutableStateFlow(0L)
    val durationUs: StateFlow<Long> = _durationUs.asStateFlow()

    private val loadControl: DefaultLoadControl = buildLoadControl()

    val player: ExoPlayer = ExoPlayer.Builder(appContext)
        .setHandleAudioBecomingNoisy(true)
        .setLoadControl(loadControl)
        .build()
        .also { exo ->
            exo.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _isPlaying.value = isPlaying
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) {
                        _durationUs.value = exo.duration.coerceAtLeast(0L) * 1000
                    }
                }
            })
        }

    private fun buildLoadControl(): DefaultLoadControl {
        val bufferMs = PerformanceManager.playerBufferMs()
        return DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                bufferMs,           // minBuffer
                bufferMs * 3,       // maxBuffer
                bufferMs / 2,       // bufferForPlayback
                bufferMs            // bufferForPlaybackAfterRebuffer
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()
    }

    fun setMedia(uri: String) {
        val item = MediaItem.fromUri(uri)
        player.setMediaItem(item)
        player.prepare()
        _positionUs.value = 0L
    }

    fun setMediaItems(uris: List<String>) {
        if (uris.isEmpty()) {
            player.clearMediaItems()
            return
        }
        val items = uris.map { MediaItem.fromUri(it) }
        player.setMediaItems(items)
        player.prepare()
        _positionUs.value = 0L
    }

    fun play() {
        player.play()
    }

    fun pause() {
        player.pause()
    }

    fun togglePlayPause() {
        if (player.isPlaying) pause() else play()
    }

    fun seekToUs(positionUs: Long) {
        player.seekTo((positionUs / 1000).coerceAtLeast(0L))
        _positionUs.value = positionUs
    }

    fun release() {
        player.release()
    }

    fun updatePosition() {
        _positionUs.value = player.currentPosition * 1000
    }
}
