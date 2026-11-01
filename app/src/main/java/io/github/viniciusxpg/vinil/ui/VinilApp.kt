package io.github.viniciusxpg.vinil.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.viniciusxpg.vinil.R
import io.github.viniciusxpg.vinil.data.Library
import io.github.viniciusxpg.vinil.data.Playlist
import io.github.viniciusxpg.vinil.data.Song
import io.github.viniciusxpg.vinil.playback.MediaIds
import io.github.viniciusxpg.vinil.ui.components.AddToPlaylistDialog
import io.github.viniciusxpg.vinil.ui.components.DeletePlaylistDialog
import io.github.viniciusxpg.vinil.ui.components.MiniPlayer
import io.github.viniciusxpg.vinil.ui.components.PlaylistNameDialog
import io.github.viniciusxpg.vinil.ui.components.SongActions
import io.github.viniciusxpg.vinil.ui.components.quantityString
import io.github.viniciusxpg.vinil.ui.screens.CollectionScreen
import io.github.viniciusxpg.vinil.ui.screens.CollectionUi
import io.github.viniciusxpg.vinil.ui.screens.EqualizerScreen
import io.github.viniciusxpg.vinil.ui.screens.LibraryScreen
import io.github.viniciusxpg.vinil.ui.screens.NowPlayingScreen
import io.github.viniciusxpg.vinil.ui.screens.PermissionScreen
import io.github.viniciusxpg.vinil.ui.screens.QueueScreen
import io.github.viniciusxpg.vinil.ui.screens.SearchScreen
import io.github.viniciusxpg.vinil.ui.screens.SettingsScreen
import io.github.viniciusxpg.vinil.ui.theme.ThemeMode
import io.github.viniciusxpg.vinil.ui.theme.ThemeSettings
import io.github.viniciusxpg.vinil.util.PermissionUtils

private const val ROUTE_SEARCH = "search"
private const val ROUTE_LIBRARY = "library"

@Composable
fun VinilApp(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    dynamicColor: Boolean,
    onDynamicColorChange: (Boolean) -> Unit,
    viewModel: PlayerViewModel = viewModel(),
) {
    val context = LocalContext.current
    // Textos lidos assim acompanham mudanças de configuração (ex.: idioma).
    val resources = LocalResources.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val equalizerState by viewModel.equalizerState.collectAsStateWithLifecycle()
    val queue by viewModel.queue.collectAsStateWithLifecycle()

    var hasPermission by remember { mutableStateOf(PermissionUtils.hasAudioPermission(context)) }
    var showNowPlaying by rememberSaveable { mutableStateOf(false) }
    // O equalizador abre por cima de qualquer tela e, ao voltar, retorna a ela.
    var showEqualizer by rememberSaveable { mutableStateOf(false) }
    // A fila abre a partir de "Tocando agora" e volta para ela.
    var showQueue by rememberSaveable { mutableStateOf(false) }
    // Configurações abrem a partir da biblioteca; o equalizador aberto por elas volta para elas.
    var showSettings by rememberSaveable { mutableStateOf(false) }
    val libraryFilter by viewModel.libraryFilter.collectAsStateWithLifecycle()
    val allFolders by viewModel.allFolders.collectAsStateWithLifecycle()

    // Pilha de telas abertas sobre a biblioteca: "search" ou um mediaId
    // de coleção ("album:42", "playlist:3"...). Vazia = biblioteca.
    var backStack by rememberSaveable { mutableStateOf(listOf<String>()) }
    // Mantém a aba, a rolagem e o texto da busca de cada tela ao navegar.
    val stateHolder = rememberSaveableStateHolder()
    fun push(route: String) {
        backStack = backStack + route
    }
    fun pop() {
        val top = backStack.lastOrNull() ?: return
        backStack = backStack.dropLast(1)
        stateHolder.removeState(top)
    }

    // Diálogos de playlist.
    var songsToAdd by remember { mutableStateOf<List<Song>?>(null) }
    var newPlaylistSongs by remember { mutableStateOf<List<Song>?>(null) }
    var playlistToRename by remember { mutableStateOf<Playlist?>(null) }
    var playlistToDelete by remember { mutableStateOf<Playlist?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        hasPermission = PermissionUtils.hasAudioPermission(context)
    }

    // O usuário pode conceder a permissão pelas configurações e voltar ao app.
    LifecycleResumeEffect(Unit) {
        hasPermission = PermissionUtils.hasAudioPermission(context)
        onPauseOrDispose { }
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) viewModel.loadLibrary()
    }

    if (!hasPermission) {
        PermissionScreen(
            onRequestPermission = { permissionLauncher.launch(PermissionUtils.runtimePermissions()) },
            onOpenSettings = {
                context.startActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null),
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            },
        )
        return
    }

    val route = backStack.lastOrNull()
    val collection = route?.takeIf { it != ROUTE_SEARCH }?.let { collectionFor(it, state.library, playlists) }

    // Se o item aberto sumiu (playlist excluída, música apagada...), volta uma tela.
    LaunchedEffect(route, collection, state.isLoading) {
        if (route != null && route != ROUTE_SEARCH && collection == null && !state.isLoading) pop()
    }

    val songActions = SongActions(
        onPlayNext = { song ->
            viewModel.playNext(song)
            context.toast(resources.getString(R.string.play_next_done))
        },
        onAddToPlaylist = { songs -> songsToAdd = songs },
    )

    val miniPlayer: @Composable () -> Unit = {
        MiniPlayer(
            state = state,
            onClick = { showNowPlaying = true },
            onPlayPause = viewModel::playPause,
            onNext = viewModel::next,
        )
    }
    // Na biblioteca o mini player fica acima do menu inferior, que já cuida da barra do sistema.
    val miniPlayerAboveMenu: @Composable () -> Unit = {
        MiniPlayer(
            state = state,
            onClick = { showNowPlaying = true },
            onPlayPause = viewModel::playPause,
            onNext = viewModel::next,
            applyNavigationBarPadding = false,
        )
    }

    when {
        showEqualizer -> {
            BackHandler { showEqualizer = false }
            EqualizerScreen(
                state = equalizerState,
                onBack = { showEqualizer = false },
                onEnabledChange = viewModel::setEqualizerEnabled,
                onPresetSelected = viewModel::useEqualizerPreset,
                onBandChange = viewModel::setEqualizerBand,
                onBassBoostChange = viewModel::setBassBoost,
                onReset = viewModel::resetEqualizer,
            )
        }

        showQueue -> {
            BackHandler { showQueue = false }
            QueueScreen(
                queue = queue,
                onBack = { showQueue = false },
                onPlay = viewModel::playQueueItem,
                onRemove = viewModel::removeFromQueue,
                onMove = viewModel::moveInQueue,
                onClearUpNext = viewModel::clearUpNext,
            )
        }

        showSettings -> {
            BackHandler { showSettings = false }
            // Recarrega a lista de pastas ao abrir e sempre que a biblioteca muda.
            LaunchedEffect(state.library) { viewModel.loadAllFolders() }
            SettingsScreen(
                themeMode = themeMode,
                onThemeModeChange = onThemeModeChange,
                dynamicColorSupported = ThemeSettings.isDynamicColorSupported,
                dynamicColor = dynamicColor,
                onDynamicColorChange = onDynamicColorChange,
                onOpenEqualizer = { showEqualizer = true },
                filter = libraryFilter,
                folders = allFolders,
                onHideMessagingAndRadioChange = viewModel::setHideMessagingAndRadio,
                onFolderHiddenChange = viewModel::setFolderHidden,
                onRefreshLibrary = { viewModel.loadLibrary(forceRefresh = true) },
                onBack = { showSettings = false },
            )
        }

        showNowPlaying && state.current != null -> {
            BackHandler { showNowPlaying = false }
            NowPlayingScreen(
                state = state,
                onClose = { showNowPlaying = false },
                onPlayPause = viewModel::playPause,
                onNext = viewModel::next,
                onPrevious = viewModel::previous,
                onSeek = viewModel::seekTo,
                onToggleShuffle = viewModel::toggleShuffle,
                onCycleRepeat = viewModel::cycleRepeatMode,
                onOpenEqualizer = { showEqualizer = true },
                onOpenQueue = { showQueue = true },
            )
        }

        route == ROUTE_SEARCH -> {
            BackHandler { pop() }
            stateHolder.SaveableStateProvider(ROUTE_SEARCH) {
                SearchScreen(
                    library = state.library,
                    playlists = playlists,
                    currentMediaId = state.current?.mediaId,
                    songActions = songActions,
                    onBack = { pop() },
                    onPlaySongs = viewModel::playSongs,
                    onOpenCollection = { push(it) },
                    miniPlayer = miniPlayer,
                )
            }
        }

        route != null && collection != null -> {
            BackHandler { pop() }
            val playlist = collection.playlistId?.let { id -> playlists.firstOrNull { it.id == id } }
            stateHolder.SaveableStateProvider(route) {
                CollectionScreen(
                    collection = collection,
                    currentMediaId = state.current?.mediaId,
                    onBack = { pop() },
                    onPlay = { index -> viewModel.playSongs(collection.songs, index) },
                    onShuffle = { viewModel.shuffleSongs(collection.songs) },
                    songActions = if (playlist != null) {
                        SongActions(
                            onPlayNext = songActions.onPlayNext,
                            onAddToPlaylist = songActions.onAddToPlaylist,
                            onRemoveFromPlaylist = { song -> viewModel.removeFromPlaylist(playlist.id, song.id) },
                        )
                    } else {
                        songActions
                    },
                    miniPlayer = miniPlayer,
                    emptyMessage = if (playlist != null) stringResource(R.string.playlist_empty) else null,
                    topBarActions = {
                        if (playlist != null) {
                            IconButton(onClick = { playlistToRename = playlist }) {
                                Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.playlist_rename))
                            }
                            IconButton(onClick = { playlistToDelete = playlist }) {
                                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.playlist_delete))
                            }
                        } else {
                            IconButton(onClick = { songsToAdd = collection.songs }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.PlaylistAdd,
                                    contentDescription = stringResource(R.string.action_add_to_playlist),
                                )
                            }
                        }
                    },
                )
            }
        }

        else -> stateHolder.SaveableStateProvider(ROUTE_LIBRARY) {
            LibraryScreen(
                state = state,
                playlists = playlists,
                songActions = songActions,
                onPlaySongs = viewModel::playSongs,
                onOpenCollection = { push(it) },
                onCreatePlaylist = { newPlaylistSongs = emptyList() },
                onOpenSearch = { push(ROUTE_SEARCH) },
                onOpenSettings = { showSettings = true },
                miniPlayer = miniPlayerAboveMenu,
            )
        }
    }

    // ---- Diálogos ----------------------------------------------------------------------

    songsToAdd?.let { songs ->
        AddToPlaylistDialog(
            playlists = playlists,
            onDismiss = { songsToAdd = null },
            onPick = { playlist ->
                songsToAdd = null
                viewModel.addToPlaylist(playlist.id, songs) { added ->
                    context.toast(
                        if (added == 0) {
                            resources.getString(R.string.playlist_already_contains, playlist.name)
                        } else {
                            resources.getQuantityString(
                                R.plurals.songs_added_to_playlist,
                                added,
                                added,
                                playlist.name,
                            )
                        },
                    )
                }
            },
            onCreateNew = {
                songsToAdd = null
                newPlaylistSongs = songs
            },
        )
    }

    newPlaylistSongs?.let { songs ->
        PlaylistNameDialog(
            title = stringResource(R.string.playlist_new),
            confirmLabel = stringResource(R.string.playlist_create),
            onDismiss = { newPlaylistSongs = null },
            onConfirm = { name ->
                newPlaylistSongs = null
                viewModel.createPlaylist(name, songs) { playlist ->
                    context.toast(resources.getString(R.string.playlist_created, playlist.name))
                }
            },
        )
    }

    playlistToRename?.let { playlist ->
        PlaylistNameDialog(
            title = stringResource(R.string.playlist_rename),
            confirmLabel = stringResource(R.string.playlist_save),
            initialName = playlist.name,
            onDismiss = { playlistToRename = null },
            onConfirm = { name ->
                playlistToRename = null
                viewModel.renamePlaylist(playlist.id, name)
            },
        )
    }

    playlistToDelete?.let { playlist ->
        DeletePlaylistDialog(
            playlist = playlist,
            onDismiss = { playlistToDelete = null },
            onConfirm = {
                playlistToDelete = null
                viewModel.deletePlaylist(playlist.id)
            },
        )
    }
}

private fun Context.toast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

@Composable
private fun collectionFor(mediaId: String, library: Library, playlists: List<Playlist>): CollectionUi? {
    MediaIds.parseAlbumId(mediaId)?.let { id ->
        val album = library.album(id) ?: return null
        return CollectionUi(
            title = album.title,
            subtitle = album.artist + " • " + quantityString(R.plurals.song_count, album.songs.size),
            songs = album.songs,
            showArtwork = false,
        )
    }
    MediaIds.parseArtist(mediaId)?.let { name ->
        val artist = library.artist(name) ?: return null
        return CollectionUi(
            title = artist.name,
            subtitle = quantityString(R.plurals.album_count, artist.albumCount) + " • " +
                quantityString(R.plurals.song_count, artist.songs.size),
            songs = artist.songs,
            showArtwork = true,
        )
    }
    MediaIds.parseFolder(mediaId)?.let { path ->
        val folder = library.folder(path) ?: return null
        return CollectionUi(
            title = folder.name,
            subtitle = folder.path + " • " + quantityString(R.plurals.song_count, folder.songs.size),
            songs = folder.songs,
            showArtwork = true,
        )
    }
    MediaIds.parsePlaylistId(mediaId)?.let { id ->
        val playlist = playlists.firstOrNull { it.id == id } ?: return null
        val songs = playlist.songsIn(library)
        return CollectionUi(
            title = playlist.name,
            subtitle = quantityString(R.plurals.song_count, songs.size),
            songs = songs,
            showArtwork = true,
            playlistId = playlist.id,
        )
    }
    return null
}
