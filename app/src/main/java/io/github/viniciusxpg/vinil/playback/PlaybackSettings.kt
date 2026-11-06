package io.github.viniciusxpg.vinil.playback

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Preferências de reprodução: duração da transição suave entre músicas. */
class PlaybackSettings private constructor(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _fadeSeconds = MutableStateFlow(prefs.getInt(KEY_FADE_SECONDS, 0).coerceIn(0, MAX_FADE_SECONDS))

    /** Segundos de diminuir/aumentar o volume entre faixas; 0 = desligado. */
    val fadeSeconds: StateFlow<Int> = _fadeSeconds.asStateFlow()

    fun setFadeSeconds(seconds: Int) {
        val value = seconds.coerceIn(0, MAX_FADE_SECONDS)
        prefs.edit { putInt(KEY_FADE_SECONDS, value) }
        _fadeSeconds.value = value
    }

    companion object {
        const val MAX_FADE_SECONDS = 10
        private const val PREFS_NAME = "playback_settings"
        private const val KEY_FADE_SECONDS = "fade_seconds"

        @Volatile
        private var instance: PlaybackSettings? = null

        fun getInstance(context: Context): PlaybackSettings =
            instance ?: synchronized(this) {
                instance ?: PlaybackSettings(context.applicationContext).also { instance = it }
            }
    }
}
