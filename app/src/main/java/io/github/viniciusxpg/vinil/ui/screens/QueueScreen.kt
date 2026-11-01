package io.github.viniciusxpg.vinil.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import io.github.viniciusxpg.vinil.R
import io.github.viniciusxpg.vinil.ui.QueueEntry
import io.github.viniciusxpg.vinil.ui.QueueState
import io.github.viniciusxpg.vinil.ui.components.AlbumArt
import io.github.viniciusxpg.vinil.ui.components.quantityString
import kotlinx.coroutines.isActive

/**
 * Fila de reprodução: música atual e as próximas. Tocar numa música pula para ela;
 * o X remove; com o aleatório desligado, a alça ≡ arrasta para reordenar (a lista rola
 * sozinha quando a música arrastada chega perto da borda).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueScreen(
    queue: QueueState,
    onBack: () -> Unit,
    onPlay: (index: Int) -> Unit,
    onRemove: (index: Int) -> Unit,
    onMove: (from: Int, to: Int) -> Unit,
    onClearUpNext: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.queue)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                actions = {
                    if (queue.upNext.isNotEmpty()) {
                        TextButton(onClick = onClearUpNext) { Text(stringResource(R.string.queue_clear)) }
                    }
                },
            )
        },
    ) { padding ->
        val current = queue.current
        if (current == null) {
            Box(Modifier.fillMaxSize().padding(padding).padding(32.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.queue_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            return@Scaffold
        }

        val canReorder = !queue.shuffle
        // Cópia local da lista para o arraste ficar fluido; volta a seguir a fila real
        // sempre que o player confirma uma mudança.
        var items by remember(queue.upNext) { mutableStateOf(queue.upNext) }
        var draggingKey by remember { mutableStateOf<Int?>(null) }
        var dragStartPosition by remember { mutableIntStateOf(-1) }
        var dragOffset by remember { mutableFloatStateOf(0f) }
        val defaultRowHeight = with(LocalDensity.current) { 72.dp.toPx() }
        var rowHeight by remember { mutableFloatStateOf(defaultRowHeight) }
        val autoScrollStep = with(LocalDensity.current) { 10.dp.toPx() }
        val listState = rememberLazyListState()

        /** Troca a música arrastada de lugar quando ela passa da metade da vizinha. */
        fun swapIfNeeded(key: Int) {
            val position = items.indexOfFirst { it.index == key }
            if (dragOffset > rowHeight / 2 && position in 0 until items.lastIndex) {
                items = items.swap(position, position + 1)
                dragOffset -= rowHeight
            } else if (dragOffset < -rowHeight / 2 && position > 0) {
                items = items.swap(position, position - 1)
                dragOffset += rowHeight
            }
        }

        // Rolagem automática enquanto a música arrastada estiver perto da borda da lista.
        LaunchedEffect(draggingKey) {
            val key = draggingKey ?: return@LaunchedEffect
            while (isActive && draggingKey == key) {
                val layout = listState.layoutInfo
                val dragged = layout.visibleItemsInfo.firstOrNull { it.key == key }
                val step = if (dragged == null) {
                    0f
                } else {
                    val top = dragged.offset + dragOffset
                    val bottom = top + dragged.size
                    when {
                        bottom > layout.viewportEndOffset - rowHeight -> autoScrollStep
                        top < layout.viewportStartOffset + rowHeight -> -autoScrollStep
                        else -> 0f
                    }
                }
                if (step != 0f) {
                    // A lista andou: compensa no deslocamento para a música continuar sob o dedo.
                    dragOffset += listState.scrollBy(step)
                    swapIfNeeded(key)
                }
                withFrameNanos { }
            }
        }

        fun finishDrag() {
            val key = draggingKey ?: return
            val target = items.indexOfFirst { it.index == key }
            if (target >= 0 && target != dragStartPosition && dragStartPosition >= 0) {
                // Sem aleatório, as próximas ocupam as posições seguintes à atual na fila.
                onMove(current.index + 1 + dragStartPosition, current.index + 1 + target)
            }
            draggingKey = null
            dragStartPosition = -1
            dragOffset = 0f
        }

        LazyColumn(state = listState, contentPadding = padding) {
            item(key = "now-header") { SectionHeader(stringResource(R.string.now_playing)) }
            item(key = "now") {
                QueueRow(entry = current, isCurrent = true, onClick = {}, onRemove = null, dragHandle = null)
            }

            item(key = "next-header") {
                SectionHeader(
                    stringResource(R.string.queue_up_next) +
                        if (items.isNotEmpty()) " • " + quantityString(R.plurals.song_count, items.size) else "",
                )
            }
            if (queue.shuffle && items.isNotEmpty()) {
                item(key = "shuffle-hint") {
                    Text(
                        text = stringResource(R.string.queue_shuffle_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
            }
            if (items.isEmpty()) {
                item(key = "next-empty") {
                    Text(
                        text = stringResource(R.string.queue_up_next_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                }
            }

            items(items, key = { it.index }) { entry ->
                val isDragging = draggingKey == entry.index
                val handle = if (canReorder) {
                    Modifier.pointerInput(entry.index) {
                        detectDragGestures(
                            onDragStart = {
                                draggingKey = entry.index
                                dragStartPosition = items.indexOfFirst { it.index == entry.index }
                                dragOffset = 0f
                            },
                            onDragEnd = { finishDrag() },
                            onDragCancel = { finishDrag() },
                            onDrag = { change, amount ->
                                change.consume()
                                dragOffset += amount.y
                                swapIfNeeded(entry.index)
                            },
                        )
                    }
                } else {
                    null
                }
                QueueRow(
                    entry = entry,
                    isCurrent = false,
                    onClick = { onPlay(entry.index) },
                    onRemove = { onRemove(entry.index) },
                    dragHandle = handle,
                    modifier = Modifier
                        .onSizeChanged { if (it.height > 0) rowHeight = it.height.toFloat() }
                        .zIndex(if (isDragging) 1f else 0f)
                        .graphicsLayer {
                            translationY = if (isDragging) dragOffset else 0f
                            shadowElevation = if (isDragging) 8.dp.toPx() else 0f
                        },
                )
            }
        }
    }
}

@Composable
private fun QueueRow(
    entry: QueueEntry,
    isCurrent: Boolean,
    onClick: () -> Unit,
    onRemove: (() -> Unit)?,
    modifier: Modifier = Modifier,
    dragHandle: Modifier? = null,
) {
    val titleColor = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    ListItem(
        modifier = modifier.clickable(enabled = !isCurrent, onClick = onClick),
        leadingContent = {
            AlbumArt(
                contentUri = entry.contentUri,
                modifier = Modifier.size(48.dp),
                cornerRadius = 6.dp,
                sizePx = 144,
            )
        },
        headlineContent = {
            Text(
                text = entry.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = if (isCurrent) FontWeight.Bold else null,
            )
        },
        supportingContent = { Text(entry.artist, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onRemove != null) {
                    IconButton(onClick = onRemove) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_remove_from_queue))
                    }
                }
                if (dragHandle != null) {
                    Icon(
                        Icons.Filled.DragHandle,
                        contentDescription = stringResource(R.string.action_reorder),
                        modifier = dragHandle.padding(8.dp),
                    )
                }
            }
        },
        colors = ListItemDefaults.colors(headlineColor = titleColor),
    )
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

private fun <T> List<T>.swap(a: Int, b: Int): List<T> =
    toMutableList().also {
        val tmp = it[a]
        it[a] = it[b]
        it[b] = tmp
    }
