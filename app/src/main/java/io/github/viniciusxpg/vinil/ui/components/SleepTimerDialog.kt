package io.github.viniciusxpg.vinil.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import io.github.viniciusxpg.vinil.R
import io.github.viniciusxpg.vinil.playback.SleepTimerState
import io.github.viniciusxpg.vinil.util.formatDuration

private val TIMER_MINUTES = listOf(15, 30, 45, 60, 90)

/** Escolha do timer de desligamento: minutos, fim da música atual ou desligar o timer. */
@Composable
fun SleepTimerDialog(
    state: SleepTimerState,
    remainingMs: Long?,
    onPickMinutes: (Int) -> Unit,
    onPickEndOfTrack: () -> Unit,
    onCancelTimer: () -> Unit,
    onDismiss: () -> Unit,
) {
    val itemColors = ListItemDefaults.colors(containerColor = Color.Transparent)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.sleep_timer)) },
        text = {
            LazyColumn {
                if (state.isActive) {
                    item(key = "status") {
                        Text(
                            if (remainingMs != null) {
                                stringResource(R.string.sleep_timer_remaining, formatDuration(remainingMs))
                            } else {
                                stringResource(R.string.sleep_timer_end_of_track_active)
                            },
                        )
                    }
                }
                items(TIMER_MINUTES, key = { it }) { minutes ->
                    ListItem(
                        headlineContent = { Text(quantityString(R.plurals.minutes, minutes)) },
                        colors = itemColors,
                        modifier = Modifier.clickable { onPickMinutes(minutes) },
                    )
                }
                item(key = "end-of-track") {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.sleep_timer_end_of_track)) },
                        colors = itemColors,
                        modifier = Modifier.clickable(onClick = onPickEndOfTrack),
                    )
                }
            }
        },
        confirmButton = {
            if (state.isActive) {
                TextButton(onClick = onCancelTimer) { Text(stringResource(R.string.sleep_timer_cancel)) }
            } else {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
            }
        },
    )
}
