package io.github.viniciusxpg.vinil.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.viniciusxpg.vinil.R
import io.github.viniciusxpg.vinil.data.Library
import io.github.viniciusxpg.vinil.data.Playlist
import io.github.viniciusxpg.vinil.data.Song
import io.github.viniciusxpg.vinil.data.matchesQuery
import io.github.viniciusxpg.vinil.data.searchLibrary
import io.github.viniciusxpg.vinil.data.uri
import io.github.viniciusxpg.vinil.playback.MediaIds
import io.github.viniciusxpg.vinil.ui.components.AlbumArt
import io.github.viniciusxpg.vinil.ui.components.SongActions
import io.github.viniciusxpg.vinil.ui.components.SongRow
import io.github.viniciusxpg.vinil.ui.components.quantityString

/** Busca na biblioteca: artistas, álbuns, playlists e músicas, sem diferenciar acentos. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    library: Library,
    playlists: List<Playlist>,
    currentMediaId: String?,
    songActions: SongActions,
    onBack: () -> Unit,
    onPlaySongs: (List<Song>, Int) -> Unit,
    onOpenCollection: (String) -> Unit,
    miniPlayer: @Composable () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val results = remember(query, library) { searchLibrary(library, query) }
    val playlistResults = remember(query, playlists) { playlists.filter { matchesQuery(it.name, query) } }
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                title = {
                    TextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text(stringResource(R.string.search_hint)) },
                        singleLine = true,
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { query = "" }) {
                                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_clear))
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                    )
                },
            )
        },
        bottomBar = miniPlayer,
    ) { padding ->
        val nothingFound = results.isEmpty && playlistResults.isEmpty()
        when {
            query.isBlank() -> CenteredMessage(stringResource(R.string.search_prompt), Modifier.padding(padding))
            nothingFound -> CenteredMessage(
                stringResource(R.string.search_no_results, query.trim()),
                Modifier.padding(padding),
            )
            else -> LazyColumn(contentPadding = padding) {
                section(R.string.library_artists, results.artists.isNotEmpty())
                items(results.artists, key = { "artist-${it.name}" }) { artist ->
                    ListItem(
                        modifier = Modifier.clickable { onOpenCollection(MediaIds.artist(artist.name)) },
                        leadingContent = {
                            AlbumArt(
                                contentUri = artist.songs.first().uri,
                                modifier = Modifier.size(48.dp),
                                cornerRadius = 24.dp,
                                sizePx = 144,
                            )
                        },
                        headlineContent = { Text(artist.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        supportingContent = { Text(quantityString(R.plurals.song_count, artist.songs.size)) },
                    )
                }

                section(R.string.library_albums, results.albums.isNotEmpty())
                items(results.albums, key = { "album-${it.id}" }) { album ->
                    ListItem(
                        modifier = Modifier.clickable { onOpenCollection(MediaIds.album(album.id)) },
                        leadingContent = {
                            AlbumArt(
                                contentUri = album.songs.first().uri,
                                modifier = Modifier.size(48.dp),
                                cornerRadius = 6.dp,
                                sizePx = 144,
                            )
                        },
                        headlineContent = { Text(album.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        supportingContent = { Text(album.artist, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    )
                }

                section(R.string.library_playlists, playlistResults.isNotEmpty())
                items(playlistResults, key = { "playlist-${it.id}" }) { playlist ->
                    ListItem(
                        modifier = Modifier.clickable { onOpenCollection(MediaIds.playlist(playlist.id)) },
                        leadingContent = { Icon(Icons.Filled.LibraryMusic, contentDescription = null) },
                        headlineContent = { Text(playlist.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        supportingContent = { Text(quantityString(R.plurals.song_count, playlist.songIds.size)) },
                    )
                }

                section(R.string.tab_songs, results.songs.isNotEmpty())
                itemsIndexed(results.songs, key = { _, song -> "song-${song.id}" }) { index, song ->
                    SongRow(
                        song = song,
                        isCurrent = currentMediaId == MediaIds.song(song.id),
                        onClick = {
                            keyboard?.hide()
                            onPlaySongs(results.songs, index)
                        },
                        actions = songActions,
                    )
                }
            }
        }
    }
}

private fun LazyListScope.section(@StringRes title: Int, visible: Boolean) {
    if (!visible) return
    item(key = "header-$title") {
        Text(
            text = stringResource(title),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
        )
    }
}

@Composable
private fun CenteredMessage(text: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
