package io.github.viniciusxpg.vinil.playback

import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.LibraryParams
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionError
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import io.github.viniciusxpg.vinil.MainActivity
import io.github.viniciusxpg.vinil.R
import io.github.viniciusxpg.vinil.data.Album
import io.github.viniciusxpg.vinil.data.Artist
import io.github.viniciusxpg.vinil.data.Folder
import io.github.viniciusxpg.vinil.data.MusicRepository
import io.github.viniciusxpg.vinil.data.Song
import io.github.viniciusxpg.vinil.data.songContentUri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.guava.future
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Serviço de reprodução em segundo plano.
 *
 * O Media3 cuida automaticamente da notificação com controles, da tela de
 * bloqueio, dos botões de fones Bluetooth, do foco de áudio e do Android Auto.
 * Aqui configuramos o player, expomos a biblioteca de músicas e salvamos o
 * estado da reprodução para retomar de onde parou.
 */
@OptIn(UnstableApi::class)
class PlaybackService : MediaLibraryService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var repository: MusicRepository
    private lateinit var stateStore: PlaybackStateStore
    private var session: MediaLibrarySession? = null
    private var positionSaverJob: Job? = null

    /** Salva a fila, a música atual, a posição e os modos sempre que mudam. */
    private val stateSaver = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            if (events.contains(Player.EVENT_TIMELINE_CHANGED)) saveQueue(player)
            if (events.containsAny(
                    Player.EVENT_MEDIA_ITEM_TRANSITION,
                    Player.EVENT_IS_PLAYING_CHANGED,
                    Player.EVENT_POSITION_DISCONTINUITY,
                    Player.EVENT_TIMELINE_CHANGED,
                )
            ) {
                savePosition(player)
            }
            if (events.containsAny(
                    Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED,
                    Player.EVENT_REPEAT_MODE_CHANGED,
                )
            ) {
                stateStore.saveModes(player.shuffleModeEnabled, player.repeatMode)
            }
            if (events.contains(Player.EVENT_IS_PLAYING_CHANGED)) {
                updatePositionSaver(player)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        repository = MusicRepository.getInstance(this)
        stateStore = PlaybackStateStore(this)

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ true, // pausa em ligações e outros apps de áudio
            )
            .setHandleAudioBecomingNoisy(true) // pausa ao desconectar o fone
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()

        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        session = MediaLibrarySession.Builder(this, player, LibraryCallback())
            .setSessionActivity(openAppIntent)
            .build()

        restoreLastSession(player)
        player.addListener(stateSaver)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? =
        session

    /** Ao fechar o app pela lista de recentes, encerra o serviço se nada estiver tocando. */
    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = session?.player
        player?.let(::savePosition)
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        positionSaverJob?.cancel()
        session?.run {
            savePosition(player)
            player.removeListener(stateSaver)
            player.release()
            release()
        }
        session = null
        serviceScope.cancel()
        super.onDestroy()
    }

    // ---------------------------------------------------------------------------------------
    // Retomar de onde parou
    // ---------------------------------------------------------------------------------------

    /**
     * Carrega a última fila no player (pausada, na posição salva) para que o app
     * já abra mostrando a música onde o usuário parou.
     */
    private fun restoreLastSession(player: Player) {
        serviceScope.launch {
            val saved = stateStore.load() ?: return@launch
            val restored = restoredQueue(saved) ?: return@launch
            // Se algo já começou a tocar enquanto a biblioteca carregava, não sobrescreve.
            if (player.mediaItemCount > 0) return@launch
            player.shuffleModeEnabled = saved.shuffle
            player.repeatMode = saved.repeatMode
            player.setMediaItems(restored.mediaItems, restored.startIndex, restored.startPositionMs)
            player.prepare()
        }
    }

    private suspend fun restoredQueue(
        saved: PlaybackStateStore.Saved? = stateStore.load(),
    ): MediaSession.MediaItemsWithStartPosition? {
        saved ?: return null
        val library = repository.getLibrary()
        val songs = saved.songIds.mapNotNull(library::song)
        if (songs.isEmpty()) return null
        val index = songs.indexOfFirst { it.id == saved.currentSongId }
        return if (index >= 0) {
            MediaSession.MediaItemsWithStartPosition(
                songs.map { it.toMediaItem() },
                index,
                saved.positionMs,
            )
        } else {
            // A música em que parou foi apagada: começa a fila do início.
            MediaSession.MediaItemsWithStartPosition(songs.map { it.toMediaItem() }, 0, 0)
        }
    }

    private fun saveQueue(player: Player) {
        val ids = (0 until player.mediaItemCount).mapNotNull { index ->
            MediaIds.parseSongId(player.getMediaItemAt(index).mediaId)
        }
        if (ids.isEmpty()) stateStore.clear() else stateStore.saveQueue(ids)
    }

    private fun savePosition(player: Player) {
        val songId = player.currentMediaItem?.mediaId?.let(MediaIds::parseSongId) ?: return
        stateStore.savePosition(songId, player.currentPosition)
    }

    /** Enquanto toca, salva a posição a cada 5 segundos (caso o app seja encerrado à força). */
    private fun updatePositionSaver(player: Player) {
        positionSaverJob?.cancel()
        positionSaverJob = null
        if (!player.isPlaying) return
        positionSaverJob = serviceScope.launch {
            while (isActive) {
                delay(5_000)
                savePosition(player)
            }
        }
    }

    // ---------------------------------------------------------------------------------------
    // Biblioteca (app, Android Auto e Assistente)
    // ---------------------------------------------------------------------------------------

    private inner class LibraryCallback : MediaLibrarySession.Callback {

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<MediaItem>> =
            Futures.immediateFuture(LibraryResult.ofItem(rootItem(), params))

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> =
            serviceScope.future<LibraryResult<ImmutableList<MediaItem>>> {
                val children = childrenOf(parentId)
                if (children == null) {
                    LibraryResult.ofError(SessionError.ERROR_BAD_VALUE)
                } else {
                    LibraryResult.ofItemList(children.page(page, pageSize), params)
                }
            }

        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String,
        ): ListenableFuture<LibraryResult<MediaItem>> =
            serviceScope.future<LibraryResult<MediaItem>> {
                val item = itemFor(mediaId)
                if (item == null) {
                    LibraryResult.ofError(SessionError.ERROR_BAD_VALUE)
                } else {
                    LibraryResult.ofItem(item, null)
                }
            }

        /**
         * Itens que chegam de controladores (app, Android Auto, Assistente) podem vir
         * sem a URI do arquivo; aqui ela é reconstruída a partir do mediaId. Pedidos de
         * busca por voz chegam com `requestMetadata.searchQuery` e viram a lista de resultados.
         */
        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
        ): ListenableFuture<MutableList<MediaItem>> =
            serviceScope.future<MutableList<MediaItem>> { resolveAll(mediaItems).toMutableList() }

        /**
         * Quando uma única faixa é escolhida (ex.: no Android Auto), toca a biblioteca
         * inteira a partir dela, para que "próxima" e "anterior" funcionem.
         */
        override fun onSetMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
            startIndex: Int,
            startPositionMs: Long,
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> =
            serviceScope.future<MediaSession.MediaItemsWithStartPosition> {
                val single = mediaItems.singleOrNull()
                val singleSongId = single
                    ?.takeIf { it.requestMetadata.searchQuery == null }
                    ?.let { MediaIds.parseSongId(it.mediaId) }
                if (singleSongId != null) {
                    val songs = repository.getSongs()
                    val index = songs.indexOfFirst { it.id == singleSongId }
                    if (index >= 0 && songs.size > 1) {
                        return@future MediaSession.MediaItemsWithStartPosition(
                            songs.map { it.toMediaItem() },
                            index,
                            startPositionMs,
                        )
                    }
                }
                val resolved = resolveAll(mediaItems)
                val safeIndex = if (resolved.size == mediaItems.size) startIndex else 0
                MediaSession.MediaItemsWithStartPosition(resolved, safeIndex, startPositionMs)
            }

        /**
         * Chamado quando o usuário aperta "play" (fone Bluetooth, controles de mídia do
         * sistema, carro) com o app fechado: retoma a última fila na posição salva.
         */
        override fun onPlaybackResumption(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> =
            serviceScope.future<MediaSession.MediaItemsWithStartPosition> {
                val saved = stateStore.load()
                    ?: throw UnsupportedOperationException("Nada para retomar")
                val restored = restoredQueue(saved)
                    ?: throw UnsupportedOperationException("Músicas salvas não encontradas")
                mediaSession.player.shuffleModeEnabled = saved.shuffle
                mediaSession.player.repeatMode = saved.repeatMode
                restored
            }
    }

    private suspend fun resolveAll(items: List<MediaItem>): List<MediaItem> = items.flatMap { item ->
        val query = item.requestMetadata.searchQuery
        when {
            item.localConfiguration != null -> listOf(item)
            query != null -> search(query).map { it.toMediaItem() }
            else -> MediaIds.parseSongId(item.mediaId)
                ?.let { listOf(item.buildUpon().setUri(songContentUri(it)).build()) }
                .orEmpty()
        }
    }

    /**
     * Busca por título, artista ou álbum. Uma busca vazia ("tocar músicas no app")
     * toca a biblioteca inteira em ordem aleatória.
     */
    private suspend fun search(query: String): List<Song> {
        val songs = repository.getSongs()
        val terms = query.trim().lowercase()
        if (terms.isEmpty()) return songs.shuffled()
        return songs.filter { song ->
            song.title.lowercase().contains(terms) ||
                song.artist.lowercase().contains(terms) ||
                song.album.lowercase().contains(terms)
        }
    }

    private fun rootItem(): MediaItem =
        folderItem(MediaIds.ROOT, getString(R.string.app_name), MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)

    private fun rootFolders(): List<MediaItem> = listOf(
        folderItem(MediaIds.SONGS, getString(R.string.library_all_songs), MediaMetadata.MEDIA_TYPE_FOLDER_MIXED),
        folderItem(MediaIds.ALBUMS, getString(R.string.library_albums), MediaMetadata.MEDIA_TYPE_FOLDER_ALBUMS),
        folderItem(MediaIds.ARTISTS, getString(R.string.library_artists), MediaMetadata.MEDIA_TYPE_FOLDER_ARTISTS),
        folderItem(MediaIds.FOLDERS, getString(R.string.library_folders), MediaMetadata.MEDIA_TYPE_FOLDER_MIXED),
    )

    private fun songCount(count: Int): String =
        resources.getQuantityString(R.plurals.song_count, count, count)

    private fun artistSummary(artist: Artist): String =
        resources.getQuantityString(R.plurals.album_count, artist.albumCount, artist.albumCount) +
            " • " + songCount(artist.songs.size)

    private fun Album.toBrowsable() = toMediaItem(songCount(songs.size))

    private fun Artist.toBrowsable() = toMediaItem(artistSummary(this))

    private fun Folder.toBrowsable() = toMediaItem(songCount(songs.size))

    private suspend fun childrenOf(parentId: String): List<MediaItem>? {
        if (parentId == MediaIds.ROOT) return rootFolders()
        val library = repository.getLibrary()
        return when (parentId) {
            MediaIds.SONGS -> library.songs.map { it.toMediaItem() }
            MediaIds.ALBUMS -> library.albums.map { it.toBrowsable() }
            MediaIds.ARTISTS -> library.artists.map { it.toBrowsable() }
            MediaIds.FOLDERS -> library.folders.map { it.toBrowsable() }
            else -> {
                val songs = MediaIds.parseAlbumId(parentId)?.let { library.album(it)?.songs }
                    ?: MediaIds.parseArtist(parentId)?.let { library.artist(it)?.songs }
                    ?: MediaIds.parseFolder(parentId)?.let { library.folder(it)?.songs }
                songs?.map { it.toMediaItem() }
            }
        }
    }

    private suspend fun itemFor(mediaId: String): MediaItem? {
        if (mediaId == MediaIds.ROOT) return rootItem()
        rootFolders().firstOrNull { it.mediaId == mediaId }?.let { return it }
        val library = repository.getLibrary()
        return MediaIds.parseSongId(mediaId)?.let { library.song(it)?.toMediaItem() }
            ?: MediaIds.parseAlbumId(mediaId)?.let { library.album(it)?.toBrowsable() }
            ?: MediaIds.parseArtist(mediaId)?.let { library.artist(it)?.toBrowsable() }
            ?: MediaIds.parseFolder(mediaId)?.let { library.folder(it)?.toBrowsable() }
    }
}
