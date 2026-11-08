package io.github.viniciusxpg.vinil.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.viniciusxpg.vinil.R
import io.github.viniciusxpg.vinil.data.TagValues

/** Formulário de edição das informações gravadas no arquivo da música. */
@Composable
fun EditTagsDialog(
    initial: TagValues,
    onDismiss: () -> Unit,
    onSave: (TagValues) -> Unit,
) {
    var title by rememberSaveable { mutableStateOf(initial.title) }
    var artist by rememberSaveable { mutableStateOf(initial.artist) }
    var album by rememberSaveable { mutableStateOf(initial.album) }
    var track by rememberSaveable { mutableStateOf(initial.track?.toString().orEmpty()) }

    val textOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.action_edit_tags)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.field_title)) },
                    singleLine = true,
                    keyboardOptions = textOptions,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = artist,
                    onValueChange = { artist = it },
                    label = { Text(stringResource(R.string.field_artist)) },
                    singleLine = true,
                    keyboardOptions = textOptions,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = album,
                    onValueChange = { album = it },
                    label = { Text(stringResource(R.string.field_album)) },
                    singleLine = true,
                    keyboardOptions = textOptions,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = track,
                    onValueChange = { value -> track = value.filter(Char::isDigit).take(4) },
                    label = { Text(stringResource(R.string.field_track)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = stringResource(R.string.edit_tags_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(TagValues(title, artist, album, track.toIntOrNull())) },
                enabled = title.isNotBlank(),
            ) { Text(stringResource(R.string.playlist_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}
