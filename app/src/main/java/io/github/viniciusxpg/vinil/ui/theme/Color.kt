package io.github.viniciusxpg.vinil.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/*
 * Paleta do Vinil, derivada da logo: laranja queimado (#F4511E), preto do disco
 * (#161616) e creme do selo (#FFF3E0). Os tons seguem os papéis do Material 3,
 * com contraste adequado para texto em cada combinação (on* sobre a cor base).
 */

val VinilLightColors = lightColorScheme(
    primary = Color(0xFFB3380B),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDBD0),
    onPrimaryContainer = Color(0xFF3A0B00),
    inversePrimary = Color(0xFFFFB59D),

    secondary = Color(0xFF77574C),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFDBD0),
    onSecondaryContainer = Color(0xFF2C160D),

    tertiary = Color(0xFF6B5E2F),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF4E2A7),
    onTertiaryContainer = Color(0xFF231B00),

    // Fundo creme, como o selo do disco.
    background = Color(0xFFFFF8F4),
    onBackground = Color(0xFF231917),
    surface = Color(0xFFFFF8F4),
    onSurface = Color(0xFF231917),
    surfaceVariant = Color(0xFFF5DED6),
    onSurfaceVariant = Color(0xFF53433E),
    surfaceTint = Color(0xFFB3380B),
    surfaceBright = Color(0xFFFFF8F4),
    surfaceDim = Color(0xFFE8D6D1),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFF1EC),
    surfaceContainer = Color(0xFFFCEAE4),
    surfaceContainerHigh = Color(0xFFF6E4DE),
    surfaceContainerHighest = Color(0xFFF1DED8),
    inverseSurface = Color(0xFF392E2B),
    inverseOnSurface = Color(0xFFFFEDE8),

    outline = Color(0xFF85736D),
    outlineVariant = Color(0xFFD8C2BA),
)

val VinilDarkColors = darkColorScheme(
    primary = Color(0xFFFFB59D),
    onPrimary = Color(0xFF5D1900),
    primaryContainer = Color(0xFF8A2C05),
    onPrimaryContainer = Color(0xFFFFDBD0),
    inversePrimary = Color(0xFFB3380B),

    secondary = Color(0xFFE7BDB1),
    onSecondary = Color(0xFF442A21),
    secondaryContainer = Color(0xFF5D4036),
    onSecondaryContainer = Color(0xFFFFDBD0),

    tertiary = Color(0xFFD8C68D),
    onTertiary = Color(0xFF3A3005),
    tertiaryContainer = Color(0xFF52461A),
    onTertiaryContainer = Color(0xFFF4E2A7),

    // Fundo "preto vinil", levemente quente.
    background = Color(0xFF161211),
    onBackground = Color(0xFFEDE0DB),
    surface = Color(0xFF161211),
    onSurface = Color(0xFFEDE0DB),
    surfaceVariant = Color(0xFF53433E),
    onSurfaceVariant = Color(0xFFD8C2BA),
    surfaceTint = Color(0xFFFFB59D),
    surfaceBright = Color(0xFF3E3634),
    surfaceDim = Color(0xFF161211),
    surfaceContainerLowest = Color(0xFF100C0B),
    surfaceContainerLow = Color(0xFF1F1A18),
    surfaceContainer = Color(0xFF231D1C),
    surfaceContainerHigh = Color(0xFF2E2826),
    surfaceContainerHighest = Color(0xFF393230),
    inverseSurface = Color(0xFFEDE0DB),
    inverseOnSurface = Color(0xFF392E2B),

    outline = Color(0xFFA08C86),
    outlineVariant = Color(0xFF53433E),
)
