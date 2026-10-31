package io.github.viniciusxpg.vinil.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.viniciusxpg.vinil.R
import io.github.viniciusxpg.vinil.data.Song
import io.github.viniciusxpg.vinil.data.uri
import io.github.viniciusxpg.vinil.playback.MediaIds
import io.github.viniciusxpg.vinil.ui.components.AlbumArt
import io.github.viniciusxpg.vinil.ui.components.SongRow

/** Dados de um álbum, artista ou pasta aberto. */
data class CollectionUi(
    val title: String,
    val subtitle: String,
    val songs: List<Song>,
    /** Falso em álbuns: as linhas mostram o número da faixa em vez da capa. */
    val showArtwork: Boolean,
)

/** Tela de um álbum, artista ou pasta: cabeçalho com capa e botões, e a lista de músicas. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionScreen(
    collection: CollectionUi,
    currentMediaId: String?,
    onBack: () -> Unit,
    onPlay: (Int) -> Unit,
    onShuffle: () -> Unit,
    miniPlayer: @Composable () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(collection.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
            )
        },
        bottomBar = miniPlayer,
    ) { padding ->
        LazyColumn(contentPadding = padding) {
            item(key = "header") {
                Header(collection, onPlay = { onPlay(0) }, onShuffle = onShuffle)
            }
            itemsIndexed(collection.songs, key = { _, song -> song.id }) { index, song ->
                SongRow(
                    song = song,
                    isCurrent = currentMediaId == MediaIds.song(song.id),
                    onClick = { onPlay(index) },
                    showArtwork = collection.showArtwork,
                    position = index,
                )
            }
        }
    }
}

@Composable
private fun Header(collection: CollectionUi, onPlay: () -> Unit, onShuffle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AlbumArt(
            contentUri = collection.songs.firstOrNull()?.uri,
            modifier = Modifier.size(120.dp),
            cornerRadius = 12.dp,
            sizePx = 384,
        )
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = collection.title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = collection.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onPlay, contentPadding = ButtonDefaults.ButtonWithIconContentPadding) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.action_play_all))
                }
                OutlinedButton(onClick = onShuffle, contentPadding = PaddingValues(horizontal = 14.dp)) {
                    Icon(Icons.Filled.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.action_shuffle_all))
                }
            }
        }
    }
}
