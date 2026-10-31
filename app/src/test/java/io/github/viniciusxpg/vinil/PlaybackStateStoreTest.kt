package io.github.viniciusxpg.vinil

import io.github.viniciusxpg.vinil.playback.PlaybackStateStore
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackStateStoreTest {
    @Test
    fun queueRoundTrip() {
        val ids = listOf(5L, 42L, 7L)
        assertEquals(ids, PlaybackStateStore.decodeIds(PlaybackStateStore.encodeIds(ids)))
    }

    @Test
    fun decodeIgnoresGarbage() {
        assertEquals(emptyList<Long>(), PlaybackStateStore.decodeIds(null))
        assertEquals(emptyList<Long>(), PlaybackStateStore.decodeIds(""))
        assertEquals(listOf(1L, 3L), PlaybackStateStore.decodeIds("1,x,3,"))
    }
}
