package io.github.viniciusxpg.vinil.playback

import android.content.Context
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.util.Log
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Uma faixa do equalizador: frequência central (Hz) e nível atual (milibéis; 100 mB = 1 dB). */
data class EqualizerBand(val centerHz: Int, val levelMb: Int)

data class EqualizerState(
    /** Falso se o aparelho não suporta equalizador ou o player ainda não foi criado. */
    val available: Boolean = false,
    val enabled: Boolean = false,
    val bands: List<EqualizerBand> = emptyList(),
    val minLevelMb: Int = -1500,
    val maxLevelMb: Int = 1500,
    /** Nomes dos ajustes prontos oferecidos pelo aparelho (Normal, Rock, Jazz...). */
    val presets: List<String> = emptyList(),
    /** Índice em [presets], ou -1 quando o usuário ajustou as faixas manualmente. */
    val currentPreset: Int = CUSTOM_PRESET,
    val bassBoostSupported: Boolean = false,
    /** Força do reforço de graves, de 0 a 1000. */
    val bassBoostStrength: Int = 0,
) {
    companion object {
        const val CUSTOM_PRESET = -1
    }
}

/**
 * Equalizador e reforço de graves aplicados à sessão de áudio do player do Vinil
 * (não afeta outros apps). O serviço chama [attach] ao criar o player e [release]
 * ao ser destruído; a interface lê [state] e chama os métodos de ajuste.
 *
 * Os ajustes ficam salvos e são reaplicados toda vez que o player é criado.
 */
class EqualizerController private constructor(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null

    private val _state = MutableStateFlow(EqualizerState())
    val state: StateFlow<EqualizerState> = _state.asStateFlow()

    fun attach(audioSessionId: Int) {
        release()
        val eq = try {
            Equalizer(0, audioSessionId)
        } catch (e: Exception) {
            // Alguns aparelhos/emuladores não oferecem o efeito.
            Log.w(TAG, "Equalizador indisponível", e)
            return
        }
        val bass = try {
            BassBoost(0, audioSessionId).takeIf { it.strengthSupported }
        } catch (e: Exception) {
            Log.w(TAG, "Reforço de graves indisponível", e)
            null
        }
        equalizer = eq
        bassBoost = bass

        restoreSavedSettings(eq, bass)
        publishState()
    }

    fun release() {
        equalizer?.release()
        bassBoost?.release()
        equalizer = null
        bassBoost = null
        _state.update { it.copy(available = false) }
    }

    fun setEnabled(enabled: Boolean) {
        runSafely {
            equalizer?.setEnabled(enabled)
            bassBoost?.setEnabled(enabled)
        }
        prefs.edit { putBoolean(KEY_ENABLED, enabled) }
        _state.update { it.copy(enabled = enabled) }
    }

    fun setBandLevel(band: Int, levelMb: Int) {
        val eq = equalizer ?: return
        val clamped = levelMb.coerceIn(_state.value.minLevelMb, _state.value.maxLevelMb)
        runSafely { eq.setBandLevel(band.toShort(), clamped.toShort()) }
        _state.update { state ->
            state.copy(
                currentPreset = EqualizerState.CUSTOM_PRESET,
                bands = state.bands.mapIndexed { index, b -> if (index == band) b.copy(levelMb = clamped) else b },
            )
        }
        saveBands()
    }

    fun usePreset(index: Int) {
        val eq = equalizer ?: return
        if (index !in 0 until eq.numberOfPresets) return
        runSafely { eq.usePreset(index.toShort()) }
        prefs.edit { putInt(KEY_PRESET, index) }
        publishState()
    }

    fun setBassBoost(strength: Int) {
        val bass = bassBoost ?: return
        val clamped = strength.coerceIn(0, 1000)
        runSafely { bass.setStrength(clamped.toShort()) }
        prefs.edit { putInt(KEY_BASS, clamped) }
        _state.update { it.copy(bassBoostStrength = clamped) }
    }

    /** Volta todas as faixas e o reforço de graves para zero. */
    fun reset() {
        val eq = equalizer ?: return
        runSafely {
            for (band in 0 until eq.numberOfBands) eq.setBandLevel(band.toShort(), 0)
        }
        prefs.edit { putInt(KEY_PRESET, EqualizerState.CUSTOM_PRESET) }
        saveBands(List(eq.numberOfBands.toInt()) { 0 })
        setBassBoost(0)
        publishState()
    }

    private fun restoreSavedSettings(eq: Equalizer, bass: BassBoost?) {
        runSafely {
            val preset = prefs.getInt(KEY_PRESET, EqualizerState.CUSTOM_PRESET)
            val savedBands = decodeLevels(prefs.getString(KEY_BANDS, null))
            if (preset in 0 until eq.numberOfPresets) {
                eq.usePreset(preset.toShort())
            } else if (savedBands.size == eq.numberOfBands.toInt()) {
                savedBands.forEachIndexed { band, level -> eq.setBandLevel(band.toShort(), level.toShort()) }
            }
            bass?.setStrength(prefs.getInt(KEY_BASS, 0).coerceIn(0, 1000).toShort())
            val enabled = prefs.getBoolean(KEY_ENABLED, false)
            eq.setEnabled(enabled)
            bass?.setEnabled(enabled)
        }
    }

    /** Lê do efeito os valores atuais e atualiza o estado exposto à interface. */
    private fun publishState() {
        val eq = equalizer ?: return
        runSafely {
            val range = eq.bandLevelRange
            val bandCount = eq.numberOfBands.toInt()
            _state.value = EqualizerState(
                available = true,
                enabled = prefs.getBoolean(KEY_ENABLED, false),
                bands = (0 until bandCount).map { band ->
                    EqualizerBand(
                        centerHz = eq.getCenterFreq(band.toShort()) / 1000,
                        levelMb = eq.getBandLevel(band.toShort()).toInt(),
                    )
                },
                minLevelMb = range[0].toInt(),
                maxLevelMb = range[1].toInt(),
                presets = (0 until eq.numberOfPresets).map { eq.getPresetName(it.toShort()) },
                currentPreset = prefs.getInt(KEY_PRESET, EqualizerState.CUSTOM_PRESET)
                    .takeIf { it in 0 until eq.numberOfPresets } ?: EqualizerState.CUSTOM_PRESET,
                bassBoostSupported = bassBoost != null,
                bassBoostStrength = prefs.getInt(KEY_BASS, 0),
            )
        }
    }

    private fun saveBands(levels: List<Int> = _state.value.bands.map { it.levelMb }) {
        prefs.edit {
            putString(KEY_BANDS, encodeLevels(levels))
            putInt(KEY_PRESET, EqualizerState.CUSTOM_PRESET)
        }
    }

    /** Os efeitos de áudio podem lançar exceção se o serviço de áudio do sistema falhar. */
    private inline fun runSafely(block: () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            Log.w(TAG, "Falha ao ajustar o equalizador", e)
        }
    }

    companion object {
        private const val TAG = "EqualizerController"
        private const val PREFS_NAME = "equalizer"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_PRESET = "preset"
        private const val KEY_BANDS = "bands"
        private const val KEY_BASS = "bass_boost"

        fun encodeLevels(levels: List<Int>): String = levels.joinToString(",")

        fun decodeLevels(encoded: String?): List<Int> =
            encoded.orEmpty().split(',').mapNotNull { it.trim().toIntOrNull() }

        @Volatile
        private var instance: EqualizerController? = null

        fun getInstance(context: Context): EqualizerController =
            instance ?: synchronized(this) {
                instance ?: EqualizerController(context.applicationContext).also { instance = it }
            }
    }
}
