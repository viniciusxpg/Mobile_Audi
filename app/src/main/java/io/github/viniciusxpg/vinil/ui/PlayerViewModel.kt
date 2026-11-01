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
import io.github.viniciusxpg.vinil.data.Playlist
import io.github.viniciusxpg.vinil.data.PlaylistRepository
import io.github.viniciusxpg.vinil.data.Song
import io.github.viniciusxpg.vinil.data.songContentUri
import io.github.viniciusxpg.vinil.playback.EqualizerController
import io.github.viniciusxpg.vinil.playback.EqualizerState
import io.github.viniciusxpg.vinil.playback.MediaIds
import io.github.viniciusxpg.vinil.playback.PlaybackService
import io.github.viniciusxpg.vinil.playback.toMediaItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
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

/** Uma música na fila; [index] é a posição dela na fila do player. */
data class QueueEntry(
    val index: Int,
    val mediaId: String,
    val title: String,
    val artist: String,
    val contentUri: Uri?,
)

/**
 * Fila de reprodução: a música atual e as próximas na ordem em que vão tocar
 * (com o aleatório ligado, é a ordem sorteada).
 */
data class QueueState(
    val current: QueueEntry? = null,
    val upNext: List<QueueEntry> = emptyList(),
    val shuffle: Boolean = false,
)

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MusicRepository.getInstance(application)
    private val playlistRepository = PlaylistRepository.getInstance(application)

    /** Playlists do usuário, em ordem alfabética. */
    val playlists: StateFlow<List<Playlist>> = playlistRepository.playlists

    private val equalizer = EqualizerController.getInstance(application)

    /** Estado do equalizador (aplicado pelo serviço de reprodução). */
    val equalizerState: StateFlow<EqualizerState> = equalizer.state

    fun setEqualizerEnabled(enabled: Boolean) = equalizer.setEnabled(enabled)

    fun setEqualizerBand(band: Int, levelMb: Int) = equalizer.setBandLevel(band, levelMb)

    fun useEqualizerPreset(index: Int) = equalizer.usePreset(index)

    fun setBassBoost(strength: Int) = equalizer.setBassBoost(strength)

    fun resetEqualizer() = equalizer.reset()

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private val _queue = MutableStateFlow(QueueState())
    val queue: StateFlow<QueueState> = _queue.asStateFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private var progressJob: Job? = null

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            syncFromPlayer(player)
            if (events.containsAny(
                    Player.EVENT_TIMELINE_CHANGED,
                    Player.EVENT_MEDIA_ITEM_TRANSITION,
                    Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED,
                    Player.EVENT_MEDIA_METADATA_CHANGED,
                )
            ) {
                _queue.value = buildQueue(player)
            }
        }
    }

    init {
        connectToService()
        viewModelScope.launch { playlistRepository.load() }
        // Arquivos de áudio novos ou apagados: recarrega a biblioteca sem mostrar "carregando".
        viewModelScope.launch {
            repository.changes.drop(1).collect { refreshLibrarySilently() }
        }
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
                _queue.value = buildQueue(connected)
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

    private suspend fun refreshLibrarySilently() {
        if (_uiState.value.isLoading) return
        val library = repository.getLibrary()
        _uiState.update { it.copy(library = library) }
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

    /** Coloca [song] logo depois da música atual na fila. */
    fun playNext(song: Song) {
        val player = controller ?: return
        val item = song.toMediaItem()
        if (player.mediaItemCount == 0) {
            player.setMediaItem(item)
            player.prepare()
            player.play()
        } else {
            player.addMediaItem(player.currentMediaItemIndex + 1, item)
        }
    }

    fun createPlaylist(name: String, songs: List<Song>, onCreated: (Playlist) -> Unit = {}) {
        viewModelScope.launch {
            onCreated(playlistRepository.create(name, songs.map { it.id }))
        }
    }

    fun renamePlaylist(playlistId: Long, name: String) {
        viewModelScope.launch { playlistRepository.rename(playlistId, name) }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch { playlistRepository.delete(playlistId) }
    }

    /** Adiciona as músicas à playlist; [onDone] recebe quantas eram novas. */
    fun addToPlaylist(playlistId: Long, songs: List<Song>, onDone: (Int) -> Unit = {}) {
        viewModelScope.launch {
            onDone(playlistRepository.addSongs(playlistId, songs.map { it.id }))
        }
    }

    fun removeFromPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch { playlistRepository.removeSong(playlistId, songId) }
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

    // ---- Fila ------------------------------------------------------------------------

    fun playQueueItem(index: Int) {
        val player = controller ?: return
        if (index !in 0 until player.mediaItemCount) return
        player.seekToDefaultPosition(index)
        player.play()
    }

    fun removeFromQueue(index: Int) {
        val player = controller ?: return
        if (index in 0 until player.mediaItemCount) player.removeMediaItem(index)
    }

    /** Move uma música da posição [from] para [to] da fila (aleatório desligado). */
    fun moveInQueue(from: Int, to: Int) {
        val player = controller ?: return
        val count = player.mediaItemCount
        if (from !in 0 until count || to !in 0 until count || from == to) return
        player.moveMediaItem(from, to)
    }

    /** Remove todas as músicas que viriam depois da atual. */
    fun clearUpNext() {
        val player = controller ?: return
        val current = player.currentMediaItemIndex
        if (!player.shuffleModeEnabled) {
            if (current + 1 < player.mediaItemCount) player.removeMediaItems(current + 1, player.mediaItemCount)
        } else {
            _queue.value.upNext.map { it.index }.sortedDescending().forEach(player::removeMediaItem)
        }
    }

    private fun buildQueue(player: Player): QueueState {
        val count = player.mediaItemCount
        val currentIndex = player.currentMediaItemIndex
        if (count == 0 || currentIndex !in 0 until count) return QueueState()
        val timeline = player.currentTimeline
        val shuffle = player.shuffleModeEnabled

        fun entry(index: Int): QueueEntry {
            val item = player.getMediaItemAt(index)
            return QueueEntry(
                index = index,
                mediaId = item.mediaId,
                title = item.mediaMetadata.title?.toString().orEmpty(),
                artist = item.mediaMetadata.artist?.toString().orEmpty(),
                contentUri = MediaIds.parseSongId(item.mediaId)?.let(::songContentUri),
            )
        }

        val upNext = buildList {
            if (timeline.isEmpty) return@buildList
            var next = timeline.getNextWindowIndex(currentIndex, Player.REPEAT_MODE_OFF, shuffle)
            while (next != C.INDEX_UNSET && size < count) {
                add(entry(next))
                next = timeline.getNextWindowIndex(next, Player.REPEAT_MODE_OFF, shuffle)
            }
        }
        return QueueState(current = entry(currentIndex), upNext = upNext, shuffle = shuffle)
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
