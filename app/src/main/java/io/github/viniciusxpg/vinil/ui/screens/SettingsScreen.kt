package io.github.viniciusxpg.vinil.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.viniciusxpg.vinil.R
import io.github.viniciusxpg.vinil.data.Folder
import io.github.viniciusxpg.vinil.data.LibraryFilter
import io.github.viniciusxpg.vinil.data.isExcludedFolder
import io.github.viniciusxpg.vinil.data.normalizeFolder
import io.github.viniciusxpg.vinil.playback.PlaybackSettings
import io.github.viniciusxpg.vinil.ui.components.quantityString
import io.github.viniciusxpg.vinil.ui.theme.ThemeMode
import kotlin.math.roundToInt

/**
 * Configurações: aparência (tema e cores), som (equalizador) e biblioteca
 * (regra de apps de mensagem / rádio FM, pastas ocultas e atualizar).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    dynamicColorSupported: Boolean,
    dynamicColor: Boolean,
    onDynamicColorChange: (Boolean) -> Unit,
    onOpenEqualizer: () -> Unit,
    fadeSeconds: Int,
    onFadeSecondsChange: (Int) -> Unit,
    filter: LibraryFilter,
    folders: List<Folder>,
    onHideMessagingAndRadioChange: (Boolean) -> Unit,
    onFolderHiddenChange: (path: String, hidden: Boolean) -> Unit,
    onRefreshLibrary: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
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
    ) { padding ->
        LazyColumn(contentPadding = padding) {
            // ---- Aparência ----
            section(R.string.settings_appearance)
            listOf(
                Triple(ThemeMode.SYSTEM, R.string.theme_system, Icons.Filled.BrightnessAuto),
                Triple(ThemeMode.LIGHT, R.string.theme_light, Icons.Filled.LightMode),
                Triple(ThemeMode.DARK, R.string.theme_dark, Icons.Filled.DarkMode),
            ).forEach { (mode, label, icon) ->
                item(key = "theme-$mode") {
                    ListItem(
                        modifier = Modifier.selectable(
                            selected = mode == themeMode,
                            role = Role.RadioButton,
                            onClick = { onThemeModeChange(mode) },
                        ),
                        leadingContent = { Icon(icon, contentDescription = null) },
                        headlineContent = { Text(stringResource(label)) },
                        trailingContent = { RadioButton(selected = mode == themeMode, onClick = null) },
                    )
                }
            }
            if (dynamicColorSupported) {
                item(key = "dynamic-color") {
                    SwitchRow(
                        icon = Icons.Filled.Palette,
                        title = stringResource(R.string.settings_dynamic_color),
                        subtitle = stringResource(R.string.settings_dynamic_color_summary),
                        checked = dynamicColor,
                        onCheckedChange = onDynamicColorChange,
                    )
                }
            }

            // ---- Som ----
            section(R.string.settings_sound)
            item(key = "equalizer") {
                ListItem(
                    modifier = Modifier.clickable(onClick = onOpenEqualizer),
                    leadingContent = { Icon(Icons.Filled.Equalizer, contentDescription = null) },
                    headlineContent = { Text(stringResource(R.string.equalizer)) },
                    trailingContent = {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                    },
                )
            }

            item(key = "fade") {
                ListItem(
                    leadingContent = { Icon(Icons.Filled.GraphicEq, contentDescription = null) },
                    headlineContent = { Text(stringResource(R.string.settings_fade)) },
                    supportingContent = {
                        Column {
                            Text(
                                if (fadeSeconds == 0) {
                                    stringResource(R.string.settings_fade_off)
                                } else {
                                    quantityString(R.plurals.seconds, fadeSeconds)
                                },
                            )
                            Slider(
                                value = fadeSeconds.toFloat(),
                                onValueChange = { onFadeSecondsChange(it.roundToInt()) },
                                valueRange = 0f..PlaybackSettings.MAX_FADE_SECONDS.toFloat(),
                                steps = PlaybackSettings.MAX_FADE_SECONDS - 1,
                            )
                        }
                    },
                )
            }

            // ---- Biblioteca ----
            section(R.string.settings_library)
            item(key = "hide-default") {
                SwitchRow(
                    icon = Icons.Filled.VisibilityOff,
                    title = stringResource(R.string.settings_hide_messaging),
                    subtitle = stringResource(R.string.settings_hide_messaging_summary),
                    checked = filter.hideMessagingAndRadio,
                    onCheckedChange = onHideMessagingAndRadioChange,
                )
            }
            item(key = "refresh") {
                ListItem(
                    modifier = Modifier.clickable(onClick = onRefreshLibrary),
                    leadingContent = { Icon(Icons.Filled.Refresh, contentDescription = null) },
                    headlineContent = { Text(stringResource(R.string.action_refresh)) },
                    supportingContent = { Text(stringResource(R.string.settings_refresh_summary)) },
                )
            }

            section(R.string.settings_folders)
            item(key = "folders-hint") {
                Text(
                    text = stringResource(R.string.settings_folders_summary),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            items(folders, key = { "folder-${it.path}" }) { folder ->
                val hiddenByRule = filter.hideMessagingAndRadio && isExcludedFolder(folder.path)
                val visible = !hiddenByRule && normalizeFolder(folder.path) !in filter.hiddenFolders
                ListItem(
                    leadingContent = {
                        Icon(
                            if (visible) Icons.Filled.Folder else Icons.Filled.FolderOff,
                            contentDescription = null,
                            tint = if (visible) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    },
                    headlineContent = { Text(folder.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    supportingContent = {
                        Text(
                            text = if (hiddenByRule) {
                                stringResource(R.string.settings_folder_hidden_by_rule)
                            } else {
                                folder.path + " • " + quantityString(R.plurals.song_count, folder.songs.size)
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked = visible,
                            enabled = !hiddenByRule,
                            onCheckedChange = { show -> onFolderHiddenChange(folder.path, !show) },
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun SwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    ListItem(
        modifier = Modifier.clickable { onCheckedChange(!checked) },
        leadingContent = { Icon(icon, contentDescription = null) },
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        trailingContent = { Switch(checked = checked, onCheckedChange = onCheckedChange) },
    )
}

private fun LazyListScope.section(@StringRes title: Int) {
    item(key = "section-$title") {
        HorizontalDivider()
        Text(
            text = stringResource(title),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
        )
    }
}
