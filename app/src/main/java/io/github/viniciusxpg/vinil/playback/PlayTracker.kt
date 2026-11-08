package io.github.viniciusxpg.vinil.playback

import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import io.github.viniciusxpg.vinil.data.PlayStatsCodec
import io.github.viniciusxpg.vinil.data.PlayStatsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Conta uma reprodução quando a faixa atual toca por tempo suficiente
 * ([PlayStatsCodec.playThresholdMs]); alimenta "Mais tocadas" e "Tocadas recentemente".
 */
class PlayTracker(
    private val player: Player,
    private val scope: CoroutineScope,
    private val stats: PlayStatsRepository,
) {
    private var currentSongId: Long? = null
    private var counted = false
    private var loopJob: Job? = null

    private val listener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            currentSongId = mediaItem?.mediaId?.let(MediaIds::parseSongId)
            counted = false
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            loopJob?.cancel()
            if (isPlaying) {
                loopJob = scope.launch {
                    while (isActive) {
                        check()
                        delay(1_000)
                    }
                }
            }
        }
    }

    fun start() {
        currentSongId = player.currentMediaItem?.mediaId?.let(MediaIds::parseSongId)
        player.addListener(listener)
    }

    fun stop() {
        loopJob?.cancel()
        player.removeListener(listener)
    }

    private fun check() {
        val songId = currentSongId ?: return
        if (counted) return
        val duration = player.duration.takeIf { it != C.TIME_UNSET } ?: 0L
        if (player.currentPosition >= PlayStatsCodec.playThresholdMs(duration)) {
            counted = true
            stats.recordPlay(songId)
        }
    }
}
