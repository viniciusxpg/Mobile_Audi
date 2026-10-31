package io.github.viniciusxpg.vinil.util

import kotlin.math.roundToInt

/** Frequência para exibição: 60 -> "60 Hz", 3600 -> "3,6 kHz", 14000 -> "14 kHz". */
fun formatFrequency(hz: Int): String {
    if (hz < 1000) return "$hz Hz"
    val tenths = (hz / 100.0).roundToInt() // kHz com uma casa decimal
    val whole = tenths / 10
    val decimal = tenths % 10
    return if (decimal == 0) "$whole kHz" else "$whole,$decimal kHz"
}

/** Nível em milibéis para decibéis com sinal: 300 -> "+3 dB", 0 -> "0 dB", -1500 -> "-15 dB". */
fun formatDecibels(levelMb: Int): String {
    val db = (levelMb / 100.0).roundToInt()
    return when {
        db > 0 -> "+$db dB"
        else -> "$db dB"
    }
}

/** Traduz os nomes de presets que o Android fornece em inglês. */
fun translatePresetName(name: String): String = when (name.trim().lowercase()) {
    "classical" -> "Clássica"
    "flat" -> "Plano"
    "normal" -> "Normal"
    "heavy metal" -> "Heavy Metal"
    "hip hop" -> "Hip Hop"
    else -> name
}
