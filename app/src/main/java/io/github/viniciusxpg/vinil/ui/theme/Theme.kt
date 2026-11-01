package io.github.viniciusxpg.vinil.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/**
 * Tema do app com as cores da logo do Vinil. [darkTheme] vem da escolha do usuário
 * (Claro / Escuro / Automático), resolvida em [ThemeMode.isDark].
 */
@Composable
fun VinilTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) VinilDarkColors else VinilLightColors,
        content = content,
    )
}
