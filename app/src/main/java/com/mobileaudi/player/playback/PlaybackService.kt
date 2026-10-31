package com.mobileaudi.player.playback

import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
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
import com.mobileaudi.player.MainActivity
import com.mobileaudi.player.R
import com.mobileaudi.player.data.MusicRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.guava.future

/**
 * Serviço de reprodução em segundo plano.
 *
 * O Media3 cuida automaticamente da notificação com controles, da tela de
 * bloqueio, dos botões de fones Bluetooth, do foco de áudio e do Android Auto.
 * Aqui só configuramos o player e expomos a biblioteca de músicas.
 */
@OptIn(UnstableApi::class)
class PlaybackService : MediaLibraryService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var repository: MusicRepository
    private var session: MediaLibrarySession? = null

    override fun onCreate() {
        super.onCreate()
        repository = MusicRepository.getInstance(this)

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
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? =
        session

    /** Ao fechar o app pela lista de recentes, encerra o serviço se nada estiver tocando. */
    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = session?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        session?.run {
            player.release()
            release()
        }
        session = null
        serviceScope.cancel()
        super.onDestroy()
    }

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
         * sem a URI do arquivo; aqui ela é reconstruída a partir do mediaId.
         */
        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
        ): ListenableFuture<MutableList<MediaItem>> =
            Futures.immediateFuture(mediaItems.mapNotNull(::resolve).toMutableList())

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
                val resolved = mediaItems.mapNotNull(::resolve)
                if (resolved.size == 1) {
                    val songs = repository.getSongs()
                    val index = songs.indexOfFirst { MediaIds.song(it.id) == resolved[0].mediaId }
                    if (index >= 0 && songs.size > 1) {
                        return@future MediaSession.MediaItemsWithStartPosition(
                            songs.map { it.toMediaItem() },
                            index,
                            startPositionMs,
                        )
                    }
                }
                val safeIndex = if (resolved.size == mediaItems.size) startIndex else 0
                MediaSession.MediaItemsWithStartPosition(resolved, safeIndex, startPositionMs)
            }
    }

    private fun resolve(item: MediaItem): MediaItem? {
        if (item.localConfiguration != null) return item
        val songId = MediaIds.parseSongId(item.mediaId) ?: return null
        return item.buildUpon().setUri(songContentUri(songId)).build()
    }

    private fun rootItem(): MediaItem =
        folderItem(MediaIds.ROOT, getString(R.string.app_name), MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)

    private fun songsFolder(): MediaItem =
        folderItem(
            MediaIds.SONGS,
            getString(R.string.library_all_songs),
            MediaMetadata.MEDIA_TYPE_FOLDER_MIXED,
        )

    private fun albumsFolder(): MediaItem =
        folderItem(
            MediaIds.ALBUMS,
            getString(R.string.library_albums),
            MediaMetadata.MEDIA_TYPE_FOLDER_ALBUMS,
        )

    private suspend fun childrenOf(parentId: String): List<MediaItem>? = when (parentId) {
        MediaIds.ROOT -> listOf(songsFolder(), albumsFolder())
        MediaIds.SONGS -> repository.getSongs().map { it.toMediaItem() }
        MediaIds.ALBUMS -> repository.getAlbums().map { album ->
            album.toMediaItem(getString(R.string.album_song_count, album.songs.size))
        }
        else -> MediaIds.parseAlbumId(parentId)?.let { albumId ->
            repository.getAlbum(albumId)?.songs?.map { it.toMediaItem() }
        }
    }

    private suspend fun itemFor(mediaId: String): MediaItem? = when (mediaId) {
        MediaIds.ROOT -> rootItem()
        MediaIds.SONGS -> songsFolder()
        MediaIds.ALBUMS -> albumsFolder()
        else -> MediaIds.parseSongId(mediaId)?.let { repository.getSong(it)?.toMediaItem() }
            ?: MediaIds.parseAlbumId(mediaId)?.let { albumId ->
                repository.getAlbum(albumId)?.let { album ->
                    album.toMediaItem(getString(R.string.album_song_count, album.songs.size))
                }
            }
    }
}
