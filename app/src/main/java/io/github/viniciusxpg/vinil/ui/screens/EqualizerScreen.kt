package io.github.viniciusxpg.vinil.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.viniciusxpg.vinil.R
import io.github.viniciusxpg.vinil.playback.EqualizerState
import io.github.viniciusxpg.vinil.util.formatDecibels
import io.github.viniciusxpg.vinil.util.formatFrequency
import io.github.viniciusxpg.vinil.util.translatePresetName
import kotlin.math.roundToInt

/** Equalizador: liga/desliga, ajustes prontos, uma barra por faixa e reforço de graves. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerScreen(
    state: EqualizerState,
    onBack: () -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    onPresetSelected: (Int) -> Unit,
    onBandChange: (band: Int, levelMb: Int) -> Unit,
    onBassBoostChange: (Int) -> Unit,
    onReset: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.equalizer)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                actions = {
                    if (state.available) {
                        Switch(
                            checked = state.enabled,
                            onCheckedChange = onEnabledChange,
                            modifier = Modifier.padding(end = 12.dp),
                        )
                    }
                },
            )
        },
    ) { padding ->
        if (!state.available) {
            Box(Modifier.fillMaxSize().padding(padding).padding(32.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.equalizer_unavailable),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            return@Scaffold
        }

        val enabled = state.enabled
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp),
        ) {
            if (!enabled) {
                Text(
                    text = stringResource(R.string.equalizer_disabled_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            if (state.presets.isNotEmpty()) {
                SectionTitle(stringResource(R.string.equalizer_presets))
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = state.currentPreset == EqualizerState.CUSTOM_PRESET,
                        onClick = {},
                        enabled = enabled,
                        label = { Text(stringResource(R.string.equalizer_custom)) },
                    )
                    state.presets.forEachIndexed { index, name ->
                        FilterChip(
                            selected = state.currentPreset == index,
                            onClick = { onPresetSelected(index) },
                            enabled = enabled,
                            label = { Text(translatePresetName(name)) },
                        )
                    }
                }
            }

            SectionTitle(stringResource(R.string.equalizer_bands))
            val range = state.minLevelMb.toFloat()..state.maxLevelMb.toFloat()
            // Passos de 1 dB (100 mB) entre o mínimo e o máximo.
            val steps = ((state.maxLevelMb - state.minLevelMb) / 100 - 1).coerceAtLeast(0)
            state.bands.forEachIndexed { index, band ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                ) {
                    Text(
                        text = formatFrequency(band.centerHz),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.width(72.dp),
                    )
                    Slider(
                        value = band.levelMb.toFloat().coerceIn(range),
                        onValueChange = { onBandChange(index, (it / 100).roundToInt() * 100) },
                        valueRange = range,
                        steps = steps,
                        enabled = enabled,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = formatDecibels(band.levelMb),
                        style = MaterialTheme.typography.labelMedium,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(56.dp),
                    )
                }
            }

            if (state.bassBoostSupported) {
                HorizontalDivider(Modifier.padding(vertical = 12.dp))
                SectionTitle(stringResource(R.string.equalizer_bass_boost))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                ) {
                    Slider(
                        value = state.bassBoostStrength.toFloat(),
                        onValueChange = { onBassBoostChange(it.roundToInt()) },
                        valueRange = 0f..1000f,
                        enabled = enabled,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "${state.bassBoostStrength / 10}%",
                        style = MaterialTheme.typography.labelMedium,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(56.dp),
                    )
                }
            }

            TextButton(
                onClick = onReset,
                enabled = enabled,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            ) {
                Text(stringResource(R.string.equalizer_reset))
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
    )
}
