package io.github.viniciusxpg.vinil

import io.github.viniciusxpg.vinil.playback.EqualizerController
import io.github.viniciusxpg.vinil.util.formatDecibels
import io.github.viniciusxpg.vinil.util.formatFrequency
import io.github.viniciusxpg.vinil.util.translatePresetName
import org.junit.Assert.assertEquals
import org.junit.Test

class AudioFormatTest {
    @Test
    fun frequencies() {
        assertEquals("60 Hz", formatFrequency(60))
        assertEquals("910 Hz", formatFrequency(910))
        assertEquals("3,6 kHz", formatFrequency(3600))
        assertEquals("14 kHz", formatFrequency(14000))
        assertEquals("1 kHz", formatFrequency(1000))
    }

    @Test
    fun decibels() {
        assertEquals("+3 dB", formatDecibels(300))
        assertEquals("0 dB", formatDecibels(0))
        assertEquals("-15 dB", formatDecibels(-1500))
    }

    @Test
    fun presetNames() {
        assertEquals("Clássica", translatePresetName("Classical"))
        assertEquals("Rock", translatePresetName("Rock"))
    }

    @Test
    fun savedLevelsRoundTrip() {
        val levels = listOf(300, 0, -150, 1500, -1500)
        assertEquals(levels, EqualizerController.decodeLevels(EqualizerController.encodeLevels(levels)))
        assertEquals(emptyList<Int>(), EqualizerController.decodeLevels(null))
    }
}
