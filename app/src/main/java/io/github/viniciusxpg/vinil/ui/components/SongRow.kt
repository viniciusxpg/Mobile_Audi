package io.github.viniciusxpg.vinil.ui.components

import androidx.annotation.PluralsRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.viniciusxpg.vinil.R
import io.github.viniciusxpg.vinil.data.Song
import io.github.viniciusxpg.vinil.data.uri
import io.github.viniciusxpg.vinil.util.formatDuration

/** Ações do menu ⋮ de uma música. [onRemoveFromPlaylist] só existe dentro de uma playlist. */
class SongActions(
    val onPlayNext: (Song) -> Unit,
    val onAddToPlaylist: (List<Song>) -> Unit,
    val onRemoveFromPlaylist: ((Song) -> Unit)? = null,
)

/**
 * Linha de música usada em todas as listas.
 *
 * @param showArtwork capa à esquerda; quando falso (dentro de um álbum) mostra o número da faixa.
 * @param actions ações do menu ⋮; sem ações, o menu não aparece.
 */
@Composable
fun SongRow(
    song: Song,
    isCurrent: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showArtwork: Boolean = true,
    position: Int = 0,
    actions: SongActions? = null,
) {
    val highlight = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    ListItem(
        modifier = modifier.clickable(onClick = onClick),
        leadingContent = {
            if (showArtwork) {
                AlbumArt(
                    contentUri = song.uri,
                    modifier = Modifier.size(48.dp),
                    cornerRadius = 6.dp,
                    sizePx = 144,
                )
            } else {
                Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                    val track = song.track % 1000
                    Text(
                        text = (if (track > 0) track else position + 1).toString(),
                        style = MaterialTheme.typography.titleSmall,
                        color = if (isCurrent) highlight else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        headlineContent = {
            Text(
                text = song.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = if (isCurrent) FontWeight.Bold else null,
            )
        },
        supportingContent = {
            Text(
                text = if (showArtwork) "${song.artist} • ${song.album}" else song.artist,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(formatDuration(song.durationMs))
                if (actions != null) SongMenu(song, actions)
            }
        },
        colors = ListItemDefaults.colors(headlineColor = highlight),
    )
}

@Composable
private fun SongMenu(song: Song, actions: SongActions) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.action_more))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_play_next)) },
                leadingIcon = { Icon(Icons.Filled.SkipNext, contentDescription = null) },
                onClick = {
                    expanded = false
                    actions.onPlayNext(song)
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_add_to_playlist)) },
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null) },
                onClick = {
                    expanded = false
                    actions.onAddToPlaylist(listOf(song))
                },
            )
            actions.onRemoveFromPlaylist?.let { remove ->
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_remove_from_playlist)) },
                    leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                    onClick = {
                        expanded = false
                        remove(song)
                    },
                )
            }
        }
    }
}

/** Texto no plural correto, ex.: "1 música" / "12 músicas". */
@Composable
fun quantityString(@PluralsRes id: Int, count: Int): String {
    return LocalResources.current.getQuantityString(id, count, count)
}
