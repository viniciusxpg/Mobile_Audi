package com.mobileaudi.player

import com.mobileaudi.player.playback.MediaIds
import com.mobileaudi.player.playback.page
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MediaIdsTest {
    @Test
    fun songIdRoundTrip() {
        assertEquals(42L, MediaIds.parseSongId(MediaIds.song(42)))
        assertNull(MediaIds.parseSongId(MediaIds.album(42)))
        assertNull(MediaIds.parseSongId("song:abc"))
    }

    @Test
    fun albumIdRoundTrip() {
        assertEquals(7L, MediaIds.parseAlbumId(MediaIds.album(7)))
        assertNull(MediaIds.parseAlbumId(MediaIds.ROOT))
    }

    @Test
    fun pagination() {
        val items = (1..10).toList()
        assertEquals(listOf(1, 2, 3), items.page(0, 3))
        assertEquals(listOf(10), items.page(3, 3))
        assertEquals(emptyList<Int>(), items.page(4, 3))
        assertEquals(items, items.page(0, Int.MAX_VALUE))
    }
}
