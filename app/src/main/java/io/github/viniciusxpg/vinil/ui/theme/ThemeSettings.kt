package io.github.viniciusxpg.vinil.ui.theme

import android.content.Context
import android.os.Build
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

    private val _dynamicColor = MutableStateFlow(prefs.getBoolean(KEY_DYNAMIC_COLOR, false))

    /** Usar as cores do papel de parede (Android 12+) no lugar das cores da logo. */
    val dynamicColor: StateFlow<Boolean> = _dynamicColor.asStateFlow()

    fun setMode(mode: ThemeMode) {
        prefs.edit { putString(KEY_MODE, mode.name) }
        _mode.value = mode
    }

    fun setDynamicColor(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_DYNAMIC_COLOR, enabled) }
        _dynamicColor.value = enabled
    }

    companion object {
        private const val PREFS_NAME = "settings"
        private const val KEY_MODE = "theme_mode"
        private const val KEY_DYNAMIC_COLOR = "dynamic_color"

        /** As cores do papel de parede só existem a partir do Android 12. */
        val isDynamicColorSupported: Boolean
            get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

        @Volatile
        private var instance: ThemeSettings? = null

        fun getInstance(context: Context): ThemeSettings =
            instance ?: synchronized(this) {
                instance ?: ThemeSettings(context.applicationContext).also { instance = it }
            }
    }
}
