package io.github.viniciusxpg.vinil.playback

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Transição suave: ajusta o volume do player para a música terminar diminuindo e a
 * seguinte começar aumentando (ver [transitionVolume]). Só age em trocas automáticas;
 * pular ou buscar uma posição volta ao volume cheio na hora.
 */
@OptIn(UnstableApi::class)
class TransitionFader(
    private val player: ExoPlayer,
    private val scope: CoroutineScope,
    private val fadeSeconds: StateFlow<Int>,
) {
    private var loopJob: Job? = null
    private var settingsJob: Job? = null
    private var startedAutomatically = false

    private val listener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            startedAutomatically = reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO ||
                reason == Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT
            applyVolume()
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int,
        ) {
            if (reason == Player.DISCONTINUITY_REASON_SEEK) startedAutomatically = false
            applyVolume()
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) = restartLoop()
    }

    fun start() {
        player.addListener(listener)
        settingsJob = scope.launch { fadeSeconds.collect { restartLoop() } }
    }

    fun stop() {
        loopJob?.cancel()
        settingsJob?.cancel()
        player.removeListener(listener)
        player.volume = 1f
    }

    private fun restartLoop() {
        loopJob?.cancel()
        applyVolume()
        if (!player.isPlaying || fadeSeconds.value <= 0) return
        loopJob = scope.launch {
            while (isActive) {
                applyVolume()
                delay(50)
            }
        }
    }

    private fun applyVolume() {
        val duration = player.duration.takeIf { it != C.TIME_UNSET } ?: 0L
        val nextComesAutomatically = player.hasNextMediaItem() || player.repeatMode != Player.REPEAT_MODE_OFF
        player.volume = transitionVolume(
            positionMs = player.currentPosition,
            durationMs = duration,
            fadeMs = fadeSeconds.value * 1_000L,
            fadeIn = startedAutomatically,
            fadeOut = nextComesAutomatically && !player.pauseAtEndOfMediaItems,
        )
    }
}
