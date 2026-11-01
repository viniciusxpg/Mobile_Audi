package io.github.viniciusxpg.vinil.ui.theme

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Escolha de tema do usuário. */
enum class ThemeMode {
    /** Segue o modo claro/escuro do sistema. */
    SYSTEM,
    LIGHT,
    DARK,
    ;

    fun isDark(systemIsDark: Boolean): Boolean = when (this) {
        SYSTEM -> systemIsDark
        LIGHT -> false
        DARK -> true
    }
}

/** Guarda a escolha de tema; é lida na abertura do app para não "piscar" a cor errada. */
class ThemeSettings private constructor(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _mode = MutableStateFlow(
        prefs.getString(KEY_MODE, null)
            ?.let { saved -> ThemeMode.entries.firstOrNull { it.name == saved } }
            ?: ThemeMode.SYSTEM,
    )
    val mode: StateFlow<ThemeMode> = _mode.asStateFlow()

    fun setMode(mode: ThemeMode) {
        prefs.edit { putString(KEY_MODE, mode.name) }
        _mode.value = mode
    }

    companion object {
        private const val PREFS_NAME = "settings"
        private const val KEY_MODE = "theme_mode"

        @Volatile
        private var instance: ThemeSettings? = null

        fun getInstance(context: Context): ThemeSettings =
            instance ?: synchronized(this) {
                instance ?: ThemeSettings(context.applicationContext).also { instance = it }
            }
    }
}
