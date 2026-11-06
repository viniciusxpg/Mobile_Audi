package io.github.viniciusxpg.vinil.playback

/**
 * Volume (0..1) da transição suave entre músicas: sobe nos primeiros [fadeMs] da faixa
 * quando ela começou sozinha ([fadeIn]) e desce nos últimos [fadeMs] quando outra faixa
 * vai começar em seguida ([fadeOut]). Fora dessas janelas, volume cheio.
 */
fun transitionVolume(
    positionMs: Long,
    durationMs: Long,
    fadeMs: Long,
    fadeIn: Boolean,
    fadeOut: Boolean,
): Float {
    if (fadeMs <= 0) return 1f
    var volume = 1f
    if (fadeIn && positionMs < fadeMs) {
        volume = minOf(volume, positionMs.toFloat() / fadeMs)
    }
    if (fadeOut && durationMs > 0) {
        val remaining = durationMs - positionMs
        if (remaining < fadeMs) volume = minOf(volume, remaining.toFloat() / fadeMs)
    }
    return volume.coerceIn(0f, 1f)
}
