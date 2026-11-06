package io.github.viniciusxpg.vinil.playback

import android.os.SystemClock
import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Estado do timer: horário (relógio desde o boot) em que pausa, ou "no fim da música atual". */
data class SleepTimerState(
    val endsAtElapsedMs: Long? = null,
    val endOfTrack: Boolean = false,
) {
    val isActive: Boolean get() = endsAtElapsedMs != null || endOfTrack

    /** Tempo restante, ou null se o timer não for por minutos. */
    fun remainingMs(nowElapsedMs: Long = SystemClock.elapsedRealtime()): Long? =
        endsAtElapsedMs?.let { (it - nowElapsedMs).coerceAtLeast(0) }
}

/**
 * Timer de desligamento. Vive no processo do app e pausa o player do serviço, então
 * funciona com a tela apagada e o app fechado (enquanto o serviço estiver tocando).
 */
@OptIn(UnstableApi::class)
object SleepTimer {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var job: Job? = null
    private var player: ExoPlayer? = null

    private val _state = MutableStateFlow(SleepTimerState())
    val state: StateFlow<SleepTimerState> = _state.asStateFlow()

    private val listener = object : Player.Listener {
        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            // "Fim da música atual": o player pausou sozinho no fim da faixa; timer cumprido.
            if (reason == Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM && _state.value.endOfTrack) {
                cancel()
            }
        }
    }

    /** Chamado pelo serviço quando o player é criado. */
    fun attach(exoPlayer: ExoPlayer) {
        player = exoPlayer
        exoPlayer.addListener(listener)
        exoPlayer.pauseAtEndOfMediaItems = _state.value.endOfTrack
    }

    fun detach(exoPlayer: ExoPlayer) {
        exoPlayer.removeListener(listener)
        if (player === exoPlayer) player = null
    }

    fun startMinutes(minutes: Int) {
        cancel()
        val durationMs = minutes * 60_000L
        _state.value = SleepTimerState(endsAtElapsedMs = SystemClock.elapsedRealtime() + durationMs)
        job = scope.launch {
            delay(durationMs)
            player?.pause()
            _state.value = SleepTimerState()
            job = null
        }
    }

    fun startEndOfTrack() {
        cancel()
        player?.pauseAtEndOfMediaItems = true
        _state.value = SleepTimerState(endOfTrack = true)
    }

    fun cancel() {
        job?.cancel()
        job = null
        player?.pauseAtEndOfMediaItems = false
        _state.value = SleepTimerState()
    }
}
