package io.github.viniciusxpg.vinil.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import io.github.viniciusxpg.vinil.R
import io.github.viniciusxpg.vinil.ui.PlayerUiState
import io.github.viniciusxpg.vinil.ui.components.AlbumArt
import io.github.viniciusxpg.vinil.util.formatDuration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    state: PlayerUiState,
    onClose: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenQueue: () -> Unit,
) {
    val current = state.current ?: return
    // Posição enquanto o usuário arrasta a barra; só faz o seek ao soltar.
    var dragPositionMs by remember { mutableStateOf<Float?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.now_playing)) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            Icons.Filled.KeyboardArrowDown,
                            contentDescription = stringResource(R.string.action_close),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenQueue) {
                        Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = stringResource(R.string.queue))
                    }
                    IconButton(onClick = onOpenEqualizer) {
                        Icon(Icons.Filled.Equalizer, contentDescription = stringResource(R.string.equalizer))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly,
        ) {
            AlbumArt(
                contentUri = current.contentUri,
                modifier = Modifier
                    .widthIn(max = 400.dp)
                    .fillMaxWidth()
                    .aspectRatio(1f),
                cornerRadius = 16.dp,
                sizePx = 1024,
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = current.title,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = current.artist,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Column(Modifier.fillMaxWidth()) {
                val durationMs = state.durationMs.coerceAtLeast(0L)
                val maxValue = durationMs.coerceAtLeast(1L).toFloat()
                val sliderValue = (dragPositionMs ?: state.positionMs.toFloat()).coerceIn(0f, maxValue)
                Slider(
                    value = sliderValue,
                    onValueChange = { dragPositionMs = it },
                    onValueChangeFinished = {
                        dragPositionMs?.let { onSeek(it.toLong()) }
                        dragPositionMs = null
                    },
                    valueRange = 0f..maxValue,
                    enabled = durationMs > 0,
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(formatDuration(sliderValue.toLong()), style = MaterialTheme.typography.labelMedium)
                    Text(formatDuration(durationMs), style = MaterialTheme.typography.labelMedium)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val activeTint = MaterialTheme.colorScheme.primary
                val inactiveTint = MaterialTheme.colorScheme.onSurfaceVariant

                IconButton(onClick = onToggleShuffle) {
                    Icon(
                        Icons.Filled.Shuffle,
                        contentDescription = stringResource(R.string.action_shuffle),
                        tint = if (state.shuffle) activeTint else inactiveTint,
                    )
                }
                IconButton(onClick = onPrevious, modifier = Modifier.size(56.dp)) {
                    Icon(
                        Icons.Filled.SkipPrevious,
                        contentDescription = stringResource(R.string.action_previous),
                        modifier = Modifier.size(36.dp),
                    )
                }
                FilledIconButton(onClick = onPlayPause, modifier = Modifier.size(72.dp)) {
                    Icon(
                        imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = stringResource(
                            if (state.isPlaying) R.string.action_pause else R.string.action_play,
                        ),
                        modifier = Modifier.size(40.dp),
                    )
                }
                IconButton(onClick = onNext, modifier = Modifier.size(56.dp)) {
                    Icon(
                        Icons.Filled.SkipNext,
                        contentDescription = stringResource(R.string.action_next),
                        modifier = Modifier.size(36.dp),
                    )
                }
                IconButton(onClick = onCycleRepeat) {
                    Icon(
                        imageVector = if (state.repeatMode == Player.REPEAT_MODE_ONE) {
                            Icons.Filled.RepeatOne
                        } else {
                            Icons.Filled.Repeat
                        },
                        contentDescription = stringResource(R.string.action_repeat),
                        tint = if (state.repeatMode == Player.REPEAT_MODE_OFF) inactiveTint else activeTint,
                    )
                }
            }
        }
    }
}
