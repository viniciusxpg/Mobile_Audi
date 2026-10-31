package com.mobileaudi.player

import com.mobileaudi.player.util.formatDuration
import org.junit.Assert.assertEquals
import org.junit.Test

class TimeFormatTest {
    @Test
    fun formatsMinutesAndSeconds() {
        assertEquals("0:00", formatDuration(0))
        assertEquals("0:09", formatDuration(9_999))
        assertEquals("3:25", formatDuration(205_000))
    }

    @Test
    fun formatsHours() {
        assertEquals("1:00:00", formatDuration(3_600_000))
        assertEquals("2:03:04", formatDuration(7_384_000))
    }

    @Test
    fun negativeBecomesZero() {
        assertEquals("0:00", formatDuration(-5_000))
    }
}
