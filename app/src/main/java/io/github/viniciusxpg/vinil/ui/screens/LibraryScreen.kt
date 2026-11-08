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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.viniciusxpg.vinil.R
import io.github.viniciusxpg.vinil.data.Album
import io.github.viniciusxpg.vinil.data.Artist
import io.github.viniciusxpg.vinil.data.AlbumSort
import io.github.viniciusxpg.vinil.data.ArtistSort
import io.github.viniciusxpg.vinil.data.Folder
import io.github.viniciusxpg.vinil.data.LibrarySort
import io.github.viniciusxpg.vinil.data.SmartPlaylist
import io.github.viniciusxpg.vinil.data.SmartPlaylistKind
import io.github.viniciusxpg.vinil.data.SongSort
import io.github.viniciusxpg.vinil.data.titleRes
import io.github.viniciusxpg.vinil.data.sortAlbums
import io.github.viniciusxpg.vinil.data.sortArtists
import io.github.viniciusxpg.vinil.data.sortSongs
import io.github.viniciusxpg.vinil.data.Playlist
import io.github.viniciusxpg.vinil.data.Song
import io.github.viniciusxpg.vinil.data.uri
import io.github.viniciusxpg.vinil.playback.MediaIds
import io.github.viniciusxpg.vinil.ui.PlayerUiState
import io.github.viniciusxpg.vinil.ui.components.AlbumArt
import io.github.viniciusxpg.vinil.ui.components.SongActions
import io.github.viniciusxpg.vinil.ui.components.SongRow
import io.github.viniciusxpg.vinil.ui.components.quantityString

private enum class LibraryTab(@StringRes val title: Int, val icon: ImageVector) {
    SONGS(R.string.tab_songs, Icons.Filled.MusicNote),
    PLAYLISTS(R.string.library_playlists, Icons.AutoMirrored.Filled.QueueMusic),
    ALBUMS(R.string.library_albums, Icons.Filled.Album),
    ARTISTS(R.string.library_artists, Icons.Filled.Person),
    FOLDERS(R.string.library_folders, Icons.Filled.Folder),
}

/**
 * Tela principal: barra superior com busca e configurações; conteúdo da
 * seção escolhida; e, embaixo, o mini player acima do menu de navegação com ícones
 * (Músicas, Playlists, Álbuns, Artistas, Pastas).
 *
 * @param onOpenCollection abre um álbum, artista, pasta ou playlist pelo seu mediaId (ver [MediaIds]).
 * @param miniPlayer mini player desenhado acima do menu inferior (sem o recuo da barra do sistema,
 *   que fica por conta do menu).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    state: PlayerUiState,
    playlists: List<Playlist>,
    smartPlaylists: List<SmartPlaylist>,
    songActions: SongActions,
    onPlaySongs: (List<Song>, Int) -> Unit,
    onOpenCollection: (String) -> Unit,
    onCreatePlaylist: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    sort: LibrarySort,
    onSortChange: (LibrarySort) -> Unit,
    miniPlayer: @Composable () -> Unit,
) {
    var selectedTab by rememberSaveable { mutableStateOf(LibraryTab.SONGS) }
    // Cada seção guarda a própria rolagem ao trocar de seção no menu.
    val tabStates = rememberSaveableStateHolder()
    val library = state.library

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    if (selectedTab in SORTABLE_TABS) {
                        SortMenu(selectedTab, sort, onSortChange)
                    }
                    IconButton(onClick = onOpenSearch) {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = stringResource(R.string.action_search),
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.settings),
                        )
                    }
                },
            )
        },
        bottomBar = {
            Column {
                miniPlayer()
                NavigationBar {
                    LibraryTab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = tab == selectedTab,
                            onClick = { selectedTab = tab },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = {
                                Text(stringResource(tab.title), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            },
                        )
                    }
                }
            }
        },
    ) { padding ->
        when {
            state.isLoading && library.songs.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            library.songs.isEmpty() -> EmptyLibrary(Modifier.padding(padding))

            else -> tabStates.SaveableStateProvider(selectedTab.name) {
                val currentId = state.current?.mediaId
                val songs = remember(library.songs, sort.songs, sort.songsDescending) {
                    sortSongs(library.songs, sort.songs, sort.songsDescending)
                }
                val albums = remember(library.albums, sort.albums, sort.albumsDescending) {
                    sortAlbums(library.albums, sort.albums, sort.albumsDescending)
                }
                val artists = remember(library.artists, sort.artists, sort.artistsDescending) {
                    sortArtists(library.artists, sort.artists, sort.artistsDescending)
                }
                // fillMaxSize: a lista começa logo abaixo da barra superior, mesmo com poucos itens.
                val listModifier = Modifier.fillMaxSize()
                when (selectedTab) {
                    LibraryTab.SONGS -> LazyColumn(listModifier, contentPadding = padding) {
                        itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
                            SongRow(
                                song = song,
                                isCurrent = currentId == MediaIds.song(song.id),
                                onClick = { onPlaySongs(songs, index) },
                                actions = songActions,
                            )
                        }
                    }

                    LibraryTab.PLAYLISTS -> LazyColumn(listModifier, contentPadding = padding) {
                        item(key = "new") {
                            IconRow(
                                icon = Icons.Filled.Add,
                                title = stringResource(R.string.playlist_new),
                                subtitle = null,
                                onClick = onCreatePlaylist,
                            )
                        }
                        items(smartPlaylists, key = { "smart-${it.kind.key}" }) { smart ->
                            IconRow(
                                icon = smart.kind.icon,
                                title = stringResource(smart.kind.titleRes),
                                subtitle = quantityString(R.plurals.song_count, smart.songs.size),
                                onClick = { onOpenCollection(MediaIds.smart(smart.kind.key)) },
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
                        albums = albums,
                        padding = padding,
                        onClick = { onOpenCollection(MediaIds.album(it.id)) },
                    )

                    LibraryTab.ARTISTS -> LazyColumn(listModifier, contentPadding = padding) {
                        items(artists, key = { it.name }) { artist ->
                            ArtistRow(artist, onClick = { onOpenCollection(MediaIds.artist(artist.name)) })
                        }
                    }

                    LibraryTab.FOLDERS -> LazyColumn(listModifier, contentPadding = padding) {
                        items(library.folders, key = { it.path }) { folder ->
                            FolderRow(folder, onClick = { onOpenCollection(MediaIds.folder(folder.path)) })
                        }
                    }
                }
            }
        }
    }
}

private val SmartPlaylistKind.icon: ImageVector
    get() = when (this) {
        SmartPlaylistKind.FAVORITES -> Icons.Filled.Favorite
        SmartPlaylistKind.MOST_PLAYED -> Icons.AutoMirrored.Filled.TrendingUp
        SmartPlaylistKind.RECENTLY_PLAYED -> Icons.Filled.History
        SmartPlaylistKind.RECENTLY_ADDED -> Icons.Filled.NewReleases
    }

private val SORTABLE_TABS = setOf(LibraryTab.SONGS, LibraryTab.ALBUMS, LibraryTab.ARTISTS)

/** Botão de ordenar: critérios da aba atual e "ordem inversa". */
@Composable
private fun SortMenu(tab: LibraryTab, sort: LibrarySort, onSortChange: (LibrarySort) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = stringResource(R.string.sort))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            @Composable
            fun option(@StringRes label: Int, selected: Boolean, onClick: () -> Unit) {
                DropdownMenuItem(
                    text = { Text(stringResource(label)) },
                    leadingIcon = { RadioButton(selected = selected, onClick = null) },
                    onClick = {
                        expanded = false
                        onClick()
                    },
                )
            }
            when (tab) {
                LibraryTab.SONGS -> {
                    option(R.string.sort_title, sort.songs == SongSort.TITLE) { onSortChange(sort.copy(songs = SongSort.TITLE)) }
                    option(R.string.sort_artist, sort.songs == SongSort.ARTIST) { onSortChange(sort.copy(songs = SongSort.ARTIST)) }
                    option(R.string.sort_album, sort.songs == SongSort.ALBUM) { onSortChange(sort.copy(songs = SongSort.ALBUM)) }
                    option(R.string.sort_date_added, sort.songs == SongSort.DATE_ADDED) {
                        // Mais recentes primeiro costuma ser o que se quer ao escolher "data de adição".
                        onSortChange(sort.copy(songs = SongSort.DATE_ADDED, songsDescending = true))
                    }
                    option(R.string.sort_duration, sort.songs == SongSort.DURATION) { onSortChange(sort.copy(songs = SongSort.DURATION)) }
                }
                LibraryTab.ALBUMS -> {
                    option(R.string.sort_title, sort.albums == AlbumSort.TITLE) { onSortChange(sort.copy(albums = AlbumSort.TITLE)) }
                    option(R.string.sort_artist, sort.albums == AlbumSort.ARTIST) { onSortChange(sort.copy(albums = AlbumSort.ARTIST)) }
                    option(R.string.sort_song_count, sort.albums == AlbumSort.SONG_COUNT) {
                        onSortChange(sort.copy(albums = AlbumSort.SONG_COUNT))
                    }
                }
                LibraryTab.ARTISTS -> {
                    option(R.string.sort_name, sort.artists == ArtistSort.NAME) { onSortChange(sort.copy(artists = ArtistSort.NAME)) }
                    option(R.string.sort_song_count, sort.artists == ArtistSort.SONG_COUNT) {
                        onSortChange(sort.copy(artists = ArtistSort.SONG_COUNT))
                    }
                }
                else -> Unit
            }
            HorizontalDivider()
            val descending = when (tab) {
                LibraryTab.SONGS -> sort.songsDescending
                LibraryTab.ALBUMS -> sort.albumsDescending
                else -> sort.artistsDescending
            }
            DropdownMenuItem(
                text = { Text(stringResource(R.string.sort_descending)) },
                leadingIcon = { Checkbox(checked = descending, onCheckedChange = null) },
                onClick = {
                    expanded = false
                    onSortChange(
                        when (tab) {
                            LibraryTab.SONGS -> sort.copy(songsDescending = !descending)
                            LibraryTab.ALBUMS -> sort.copy(albumsDescending = !descending)
                            else -> sort.copy(artistsDescending = !descending)
                        },
                    )
                },
            )
        }
    }
}

@Composable
private fun AlbumGrid(albums: List<Album>, padding: PaddingValues, onClick: (Album) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 150.dp),
        contentPadding = PaddingValues(
            start = 12.dp,
            end = 12.dp,
            top = padding.calculateTopPadding() + 12.dp,
            bottom = padding.calculateBottomPadding() + 12.dp,
        ),
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
