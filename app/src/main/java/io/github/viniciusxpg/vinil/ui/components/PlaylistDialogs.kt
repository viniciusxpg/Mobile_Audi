package io.github.viniciusxpg.vinil.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import io.github.viniciusxpg.vinil.R
import io.github.viniciusxpg.vinil.data.Playlist

/** Diálogo com um campo de nome, usado para criar e para renomear playlists. */
@Composable
fun PlaylistNameDialog(
    title: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    initialName: String = "",
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    val confirm = { if (name.isNotBlank()) onConfirm(name.trim()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.playlist_name)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { confirm() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
            )
        },
        confirmButton = {
            TextButton(onClick = confirm, enabled = name.isNotBlank()) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

/** Escolha da playlist de destino, com a opção de criar uma nova. */
@Composable
fun AddToPlaylistDialog(
    playlists: List<Playlist>,
    onDismiss: () -> Unit,
    onPick: (Playlist) -> Unit,
    onCreateNew: () -> Unit,
) {
    val itemColors = ListItemDefaults.colors(containerColor = Color.Transparent)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.action_add_to_playlist)) },
        text = {
            LazyColumn(Modifier.heightIn(max = 360.dp)) {
                item(key = "new") {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.playlist_new)) },
                        leadingContent = { Icon(Icons.Filled.Add, contentDescription = null) },
                        colors = itemColors,
                        modifier = Modifier.clickable(onClick = onCreateNew),
                    )
                }
                items(playlists, key = { it.id }) { playlist ->
                    ListItem(
                        headlineContent = { Text(playlist.name) },
                        supportingContent = {
                            Text(quantityString(R.plurals.song_count, playlist.songIds.size))
                        },
                        leadingContent = { Icon(Icons.Filled.LibraryMusic, contentDescription = null) },
                        colors = itemColors,
                        modifier = Modifier.clickable { onPick(playlist) },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
fun DeletePlaylistDialog(playlist: Playlist, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.playlist_delete)) },
        text = { Text(stringResource(R.string.playlist_delete_message, playlist.name)) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.action_delete)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}
