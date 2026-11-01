package io.github.viniciusxpg.vinil

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.viniciusxpg.vinil.ui.VinilApp
import io.github.viniciusxpg.vinil.ui.theme.ThemeSettings
import io.github.viniciusxpg.vinil.ui.theme.VinilTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val themeSettings = ThemeSettings.getInstance(this)
        setContent {
            val themeMode by themeSettings.mode.collectAsStateWithLifecycle()
            val dynamicColor by themeSettings.dynamicColor.collectAsStateWithLifecycle()
            val darkTheme = themeMode.isDark(isSystemInDarkTheme())

            // Ícones da barra de status e de navegação acompanham o tema escolhido no app,
            // mesmo quando ele é diferente do tema do sistema.
            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(LIGHT_SCRIM, DARK_SCRIM) { darkTheme },
                )
                onDispose { }
            }

            VinilTheme(darkTheme = darkTheme, dynamicColor = dynamicColor) {
                VinilApp(
                    themeMode = themeMode,
                    onThemeModeChange = themeSettings::setMode,
                    dynamicColor = dynamicColor,
                    onDynamicColorChange = themeSettings::setDynamicColor,
                )
            }
        }
    }

    private companion object {
        // Mesmos véus que o enableEdgeToEdge usa por padrão na barra de navegação de 3 botões.
        val LIGHT_SCRIM = Color.argb(0xE6, 0xFF, 0xFF, 0xFF)
        val DARK_SCRIM = Color.argb(0x80, 0x1B, 0x1B, 0x1B)
    }
}
