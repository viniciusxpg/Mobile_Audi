package io.github.viniciusxpg.vinil.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.viniciusxpg.vinil.R
import io.github.viniciusxpg.vinil.data.Library
import io.github.viniciusxpg.vinil.playback.MediaIds
import io.github.viniciusxpg.vinil.ui.components.MiniPlayer
import io.github.viniciusxpg.vinil.ui.components.quantityString
import io.github.viniciusxpg.vinil.ui.screens.CollectionScreen
import io.github.viniciusxpg.vinil.ui.screens.CollectionUi
import io.github.viniciusxpg.vinil.ui.screens.LibraryScreen
import io.github.viniciusxpg.vinil.ui.screens.NowPlayingScreen
import io.github.viniciusxpg.vinil.ui.screens.PermissionScreen
import io.github.viniciusxpg.vinil.util.PermissionUtils

@Composable
fun VinilApp(viewModel: PlayerViewModel = viewModel()) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var hasPermission by remember { mutableStateOf(PermissionUtils.hasAudioPermission(context)) }
    var showNowPlaying by rememberSaveable { mutableStateOf(false) }
    // Álbum, artista ou pasta aberto, identificado pelo mediaId (ex.: "album:42").
    var openCollectionId by rememberSaveable { mutableStateOf<String?>(null) }
    // Mantém a aba e a rolagem da biblioteca ao entrar e sair de um álbum/artista/pasta.
    val stateHolder = rememberSaveableStateHolder()

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

    val collection = openCollectionId?.let { collectionFor(it, state.library) }

    // Se o item aberto sumiu (ex.: após atualizar a biblioteca), volta para a lista.
    LaunchedEffect(openCollectionId, collection, state.isLoading) {
        if (openCollectionId != null && collection == null && !state.isLoading) {
            openCollectionId = null
        }
    }

    val miniPlayer: @Composable () -> Unit = {
        MiniPlayer(
            state = state,
            onClick = { showNowPlaying = true },
            onPlayPause = viewModel::playPause,
            onNext = viewModel::next,
        )
    }

    when {
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
            )
        }

        collection != null -> {
            BackHandler { openCollectionId = null }
            CollectionScreen(
                collection = collection,
                currentMediaId = state.current?.mediaId,
                onBack = { openCollectionId = null },
                onPlay = { index -> viewModel.playSongs(collection.songs, index) },
                onShuffle = { viewModel.shuffleSongs(collection.songs) },
                miniPlayer = miniPlayer,
            )
        }

        else -> stateHolder.SaveableStateProvider("library") {
            LibraryScreen(
                state = state,
                onPlaySongs = viewModel::playSongs,
                onOpenCollection = { openCollectionId = it },
                onRefresh = { viewModel.loadLibrary(forceRefresh = true) },
                miniPlayer = miniPlayer,
            )
        }
    }
}

@Composable
private fun collectionFor(mediaId: String, library: Library): CollectionUi? {
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
    return null
}
