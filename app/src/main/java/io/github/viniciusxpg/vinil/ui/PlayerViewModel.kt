package io.github.viniciusxpg.vinil.ui

import android.app.Application
import android.content.ComponentName
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import io.github.viniciusxpg.vinil.data.Library
import io.github.viniciusxpg.vinil.data.MusicRepository
import io.github.viniciusxpg.vinil.data.Song
import io.github.viniciusxpg.vinil.data.songContentUri
import io.github.viniciusxpg.vinil.playback.MediaIds
import io.github.viniciusxpg.vinil.playback.PlaybackService
import io.github.viniciusxpg.vinil.playback.toMediaItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Faixa que está tocando (ou pausada) no serviço. */
data class NowPlaying(
    val mediaId: String,
    val title: String,
    val artist: String,
    /** URI do arquivo, usada para carregar a capa. */
    val contentUri: Uri?,
)

data class PlayerUiState(
    val library: Library = Library.EMPTY,
    val isLoading: Boolean = true,
    val current: NowPlaying? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val shuffle: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
) {
    val progress: Float
        get() = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
}

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MusicRepository.getInstance(application)

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private var progressJob: Job? = null

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            syncFromPlayer(player)
        }
    }

    init {
        connectToService()
    }

    private fun connectToService() {
        val app = getApplication<Application>()
        val token = SessionToken(app, ComponentName(app, PlaybackService::class.java))
        val future = MediaController.Builder(app, token).buildAsync()
        controllerFuture = future
        future.addListener(
            {
                val connected = runCatching { future.get() }.getOrNull() ?: return@addListener
                controller = connected
                connected.addListener(playerListener)
                syncFromPlayer(connected)
            },
            ContextCompat.getMainExecutor(app),
        )
    }

    fun loadLibrary(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val library = repository.getLibrary(forceRefresh)
            _uiState.update { it.copy(library = library, isLoading = false) }
        }
    }

    /** Toca [songs] (uma aba, álbum, artista ou pasta) começando pela faixa [index]. */
    fun playSongs(songs: List<Song>, index: Int) {
        val player = controller ?: return
        if (index !in songs.indices) return
        player.setMediaItems(songs.map { it.toMediaItem() }, index, 0L)
        player.prepare()
        player.play()
    }

    /** Liga o modo aleatório e toca [songs] começando por uma faixa sorteada. */
    fun shuffleSongs(songs: List<Song>) {
        val player = controller ?: return
        if (songs.isEmpty()) return
        player.shuffleModeEnabled = true
        playSongs(songs, songs.indices.random())
    }

    fun playPause() {
        val player = controller ?: return
        if (player.isPlaying) {
            player.pause()
        } else {
            when (player.playbackState) {
                Player.STATE_IDLE -> player.prepare()
                Player.STATE_ENDED -> player.seekToDefaultPosition(0)
                else -> Unit
            }
            player.play()
        }
    }

    fun next() {
        controller?.seekToNext()
    }

    fun previous() {
        controller?.seekToPrevious()
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
        _uiState.update { it.copy(positionMs = positionMs) }
    }

    fun toggleShuffle() {
        val player = controller ?: return
        player.shuffleModeEnabled = !player.shuffleModeEnabled
    }

    /** Alterna entre: sem repetição → repetir tudo → repetir uma. */
    fun cycleRepeatMode() {
        val player = controller ?: return
        player.repeatMode = when (player.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    private fun syncFromPlayer(player: Player) {
        val item = player.currentMediaItem
        val current = item?.let {
            // Os metadados do player incluem as tags lidas do arquivo; os do item são o fallback.
            val metadata = player.mediaMetadata
            NowPlaying(
                mediaId = it.mediaId,
                title = (metadata.title ?: it.mediaMetadata.title)?.toString().orEmpty(),
                artist = (metadata.artist ?: it.mediaMetadata.artist)?.toString().orEmpty(),
                contentUri = MediaIds.parseSongId(it.mediaId)?.let(::songContentUri),
            )
        }
        _uiState.update {
            it.copy(
                current = current,
                isPlaying = player.isPlaying,
                positionMs = player.currentPosition,
                durationMs = player.duration.takeIf { d -> d != C.TIME_UNSET } ?: 0L,
                shuffle = player.shuffleModeEnabled,
                repeatMode = player.repeatMode,
            )
        }
        updateProgressTicker(player.isPlaying)
    }

    /** Enquanto toca, atualiza a posição da barra de progresso duas vezes por segundo. */
    private fun updateProgressTicker(isPlaying: Boolean) {
        if (!isPlaying) {
            progressJob?.cancel()
            progressJob = null
            return
        }
        if (progressJob?.isActive == true) return
        progressJob = viewModelScope.launch {
            while (isActive) {
                controller?.let { player ->
                    _uiState.update { it.copy(positionMs = player.currentPosition) }
                }
                delay(500)
            }
        }
    }

    override fun onCleared() {
        progressJob?.cancel()
        controller?.removeListener(playerListener)
        controllerFuture?.let(MediaController::releaseFuture)
        controller = null
        super.onCleared()
    }
}
