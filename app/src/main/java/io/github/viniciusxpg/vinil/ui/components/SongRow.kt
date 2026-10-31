package io.github.viniciusxpg.vinil.ui.components

import androidx.annotation.PluralsRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.viniciusxpg.vinil.data.Song
import io.github.viniciusxpg.vinil.data.uri
import io.github.viniciusxpg.vinil.util.formatDuration

/**
 * Linha de música usada em todas as listas.
 *
 * @param showArtwork capa à esquerda; quando falso (dentro de um álbum) mostra o número da faixa.
 */
@Composable
fun SongRow(
    song: Song,
    isCurrent: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showArtwork: Boolean = true,
    position: Int = 0,
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
        trailingContent = { Text(formatDuration(song.durationMs)) },
        colors = ListItemDefaults.colors(headlineColor = highlight),
    )
}

/** Texto no plural correto, ex.: "1 música" / "12 músicas". */
@Composable
fun quantityString(@PluralsRes id: Int, count: Int): String {
    LocalConfiguration.current // recompõe se o idioma mudar
    return LocalContext.current.resources.getQuantityString(id, count, count)
}
