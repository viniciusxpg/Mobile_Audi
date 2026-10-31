package io.github.viniciusxpg.vinil.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.viniciusxpg.vinil.R
import io.github.viniciusxpg.vinil.data.Album
import io.github.viniciusxpg.vinil.data.Artist
import io.github.viniciusxpg.vinil.data.Folder
import io.github.viniciusxpg.vinil.data.Playlist
import io.github.viniciusxpg.vinil.data.Song
import io.github.viniciusxpg.vinil.data.uri
import io.github.viniciusxpg.vinil.playback.MediaIds
import io.github.viniciusxpg.vinil.ui.PlayerUiState
import io.github.viniciusxpg.vinil.ui.components.AlbumArt
import io.github.viniciusxpg.vinil.ui.components.SongActions
import io.github.viniciusxpg.vinil.ui.components.SongRow
import io.github.viniciusxpg.vinil.ui.components.quantityString
import kotlinx.coroutines.launch

private enum class LibraryTab(@StringRes val title: Int) {
    SONGS(R.string.tab_songs),
    PLAYLISTS(R.string.library_playlists),
    ALBUMS(R.string.library_albums),
    ARTISTS(R.string.library_artists),
    FOLDERS(R.string.library_folders),
}

/**
 * Tela principal com as abas Músicas, Playlists, Álbuns, Artistas e Pastas (troca
 * deslizando para os lados ou tocando na aba).
 *
 * @param onOpenCollection abre um álbum, artista ou pasta pelo seu mediaId (ver [MediaIds]).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    state: PlayerUiState,
    playlists: List<Playlist>,
    songActions: SongActions,
    onPlaySongs: (List<Song>, Int) -> Unit,
    onOpenCollection: (String) -> Unit,
    onCreatePlaylist: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onRefresh: () -> Unit,
    miniPlayer: @Composable () -> Unit,
) {
    val tabs = LibraryTab.entries
    val pagerState = rememberPagerState { tabs.size }
    val scope = rememberCoroutineScope()
    val library = state.library

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(stringResource(R.string.app_name)) },
                    actions = {
                        IconButton(onClick = onOpenSearch) {
                            Icon(
                                Icons.Filled.Search,
                                contentDescription = stringResource(R.string.action_search),
                            )
                        }
                        IconButton(onClick = onOpenEqualizer) {
                            Icon(
                                Icons.Filled.Equalizer,
                                contentDescription = stringResource(R.string.equalizer),
                            )
                        }
                        IconButton(onClick = onRefresh) {
                            Icon(
                                Icons.Filled.Refresh,
                                contentDescription = stringResource(R.string.action_refresh),
                            )
                        }
                    },
                )
                ScrollableTabRow(selectedTabIndex = pagerState.currentPage, edgePadding = 8.dp) {
                    tabs.forEachIndexed { index, tab ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                            text = {
                                Text(stringResource(tab.title), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            },
                        )
                    }
                }
            }
        },
        bottomBar = miniPlayer,
    ) { padding ->
        when {
            state.isLoading && library.songs.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            library.songs.isEmpty() -> EmptyLibrary(Modifier.padding(padding))

            else -> HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = padding.calculateTopPadding()),
            ) { page ->
                val bottom = padding.calculateBottomPadding()
                val currentId = state.current?.mediaId
                when (tabs[page]) {
                    LibraryTab.SONGS -> LazyColumn(contentPadding = PaddingValues(bottom = bottom)) {
                        itemsIndexed(library.songs, key = { _, song -> song.id }) { index, song ->
                            SongRow(
                                song = song,
                                isCurrent = currentId == MediaIds.song(song.id),
                                onClick = { onPlaySongs(library.songs, index) },
                                actions = songActions,
                            )
                        }
                    }

                    LibraryTab.PLAYLISTS -> LazyColumn(contentPadding = PaddingValues(bottom = bottom)) {
                        item(key = "new") {
                            IconRow(
                                icon = Icons.Filled.Add,
                                title = stringResource(R.string.playlist_new),
                                subtitle = null,
                                onClick = onCreatePlaylist,
                            )
                        }
                        items(playlists, key = { it.id }) { playlist ->
                            IconRow(
                                icon = Icons.Filled.LibraryMusic,
                                title = playlist.name,
                                subtitle = quantityString(R.plurals.song_count, playlist.songIds.size),
                                onClick = { onOpenCollection(MediaIds.playlist(playlist.id)) },
                            )
                        }
                    }

                    LibraryTab.ALBUMS -> AlbumGrid(
                        albums = library.albums,
                        bottomPadding = bottom,
                        onClick = { onOpenCollection(MediaIds.album(it.id)) },
                    )

                    LibraryTab.ARTISTS -> LazyColumn(contentPadding = PaddingValues(bottom = bottom)) {
                        items(library.artists, key = { it.name }) { artist ->
                            ArtistRow(artist, onClick = { onOpenCollection(MediaIds.artist(artist.name)) })
                        }
                    }

                    LibraryTab.FOLDERS -> LazyColumn(contentPadding = PaddingValues(bottom = bottom)) {
                        items(library.folders, key = { it.path }) { folder ->
                            FolderRow(folder, onClick = { onOpenCollection(MediaIds.folder(folder.path)) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AlbumGrid(albums: List<Album>, bottomPadding: Dp, onClick: (Album) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 150.dp),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 12.dp + bottomPadding),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(albums, key = { it.id }) { album ->
            Column(
                Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onClick(album) }
                    .padding(4.dp),
            ) {
                AlbumArt(
                    contentUri = album.songs.first().uri,
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                    cornerRadius = 12.dp,
                    sizePx = 384,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = album.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = album.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ArtistRow(artist: Artist, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = {
            AlbumArt(
                contentUri = artist.songs.first().uri,
                modifier = Modifier.size(48.dp),
                cornerRadius = 24.dp,
                sizePx = 144,
            )
        },
        headlineContent = { Text(artist.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Text(
                quantityString(R.plurals.album_count, artist.albumCount) + " • " +
                    quantityString(R.plurals.song_count, artist.songs.size),
                maxLines = 1,
            )
        },
    )
}

@Composable
private fun FolderRow(folder: Folder, onClick: () -> Unit) {
    IconRow(
        icon = Icons.Filled.Folder,
        title = folder.name,
        subtitle = folder.path + " • " + quantityString(R.plurals.song_count, folder.songs.size),
        onClick = onClick,
    )
}

/** Linha com ícone num quadrado colorido (pastas, playlists, "Nova playlist"). */
@Composable
private fun IconRow(icon: ImageVector, title: String, subtitle: String?, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        },
        headlineContent = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = subtitle?.let { text ->
            { Text(text, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        },
    )
}

@Composable
private fun EmptyLibrary(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Filled.MusicOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(64.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.empty_library_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.empty_library_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
