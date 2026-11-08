package io.github.viniciusxpg.vinil

import io.github.viniciusxpg.vinil.data.Library
import io.github.viniciusxpg.vinil.data.PlayStat
import io.github.viniciusxpg.vinil.data.PlayStatsCodec
import io.github.viniciusxpg.vinil.data.Song
import io.github.viniciusxpg.vinil.data.SmartPlaylists
import org.junit.Assert.assertEquals
import org.junit.Test

class SmartPlaylistsTest {
    private val library = Library.from(
        listOf(
            Song(1, "Um", "A", "X", albumId = 1, durationMs = 1, dateAdded = 100),
            Song(2, "Dois", "A", "X", albumId = 1, durationMs = 1, dateAdded = 300),
            Song(3, "Três", "B", "Y", albumId = 2, durationMs = 1, dateAdded = 200),
        ),
    )

    @Test
    fun favoritesKeepOrderAndSkipDeletedSongs() {
        var favs = SmartPlaylists.toggleFavorite(emptyList(), 1)
        favs = SmartPlaylists.toggleFavorite(favs, 3)
        favs = SmartPlaylists.toggleFavorite(favs, 99) // música que não existe mais
        assertEquals(listOf(3L, 1L), SmartPlaylists.favorites(library, favs).map { it.id })
        assertEquals(listOf(99L, 1L), SmartPlaylists.toggleFavorite(favs, 3)) // descurtir
    }

    @Test
    fun mostAndRecentlyPlayed() {
        var stats = emptyMap<Long, PlayStat>()
        stats = PlayStatsCodec.record(stats, 1, nowMs = 10)
        stats = PlayStatsCodec.record(stats, 2, nowMs = 20)
        stats = PlayStatsCodec.record(stats, 1, nowMs = 30)
        assertEquals(PlayStat(2, 30), stats[1])
        assertEquals(listOf(1L, 2L), SmartPlaylists.mostPlayed(library, stats).map { it.id })
        assertEquals(listOf(1L, 2L), SmartPlaylists.recentlyPlayed(library, stats).map { it.id })
        stats = PlayStatsCodec.record(stats, 2, nowMs = 40)
        assertEquals(listOf(2L, 1L), SmartPlaylists.recentlyPlayed(library, stats).map { it.id })
    }

    @Test
    fun recentlyAddedNewestFirst() {
        assertEquals(listOf(2L, 3L, 1L), SmartPlaylists.recentlyAdded(library).map { it.id })
    }

    @Test
    fun statsRoundTripAndThreshold() {
        val stats = mapOf(1L to PlayStat(3, 123), 42L to PlayStat(1, 456))
        assertEquals(stats, PlayStatsCodec.decode(PlayStatsCodec.encode(stats)))
        assertEquals(emptyMap<Long, PlayStat>(), PlayStatsCodec.decode("lixo"))
        assertEquals(30_000L, PlayStatsCodec.playThresholdMs(240_000))
        assertEquals(10_000L, PlayStatsCodec.playThresholdMs(20_000))
        assertEquals(30_000L, PlayStatsCodec.playThresholdMs(0))
    }
}
