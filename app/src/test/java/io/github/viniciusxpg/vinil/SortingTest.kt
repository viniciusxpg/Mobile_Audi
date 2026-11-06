package io.github.viniciusxpg.vinil

import io.github.viniciusxpg.vinil.data.Library
import io.github.viniciusxpg.vinil.data.Song
import io.github.viniciusxpg.vinil.data.SongSort
import io.github.viniciusxpg.vinil.data.sortArtists
import io.github.viniciusxpg.vinil.data.ArtistSort
import io.github.viniciusxpg.vinil.data.sortSongs
import io.github.viniciusxpg.vinil.playback.transitionVolume
import org.junit.Assert.assertEquals
import org.junit.Test

class SortingTest {
    private val songs = listOf(
        Song(1, "Zebra", "Beto", "B", albumId = 1, durationMs = 300, dateAdded = 10),
        Song(2, "Água", "Ana", "A", albumId = 2, durationMs = 100, dateAdded = 30),
        Song(3, "agora", "Beto", "B", albumId = 1, durationMs = 200, dateAdded = 20),
    )

    @Test
    fun titleIgnoresAccentsAndCase() {
        assertEquals(listOf(3L, 2L, 1L), sortSongs(songs, SongSort.TITLE, false).map { it.id })
    }

    @Test
    fun descendingAndOtherFields() {
        assertEquals(listOf(2L, 3L, 1L), sortSongs(songs, SongSort.DATE_ADDED, true).map { it.id })
        assertEquals(listOf(2L, 3L, 1L), sortSongs(songs, SongSort.DURATION, false).map { it.id })
        assertEquals("Ana", sortSongs(songs, SongSort.ARTIST, false).first().artist)
    }

    @Test
    fun artistsBySongCount() {
        val artists = Library.from(songs).artists
        assertEquals(listOf("Beto", "Ana"), sortArtists(artists, ArtistSort.SONG_COUNT, true).map { it.name })
    }

    @Test
    fun transitionFadesInAndOut() {
        assertEquals(1f, transitionVolume(5_000, 200_000, 0, fadeIn = true, fadeOut = true))
        assertEquals(0.5f, transitionVolume(1_000, 200_000, 2_000, fadeIn = true, fadeOut = true))
        assertEquals(1f, transitionVolume(1_000, 200_000, 2_000, fadeIn = false, fadeOut = true))
        assertEquals(0.25f, transitionVolume(199_500, 200_000, 2_000, fadeIn = true, fadeOut = true))
        assertEquals(1f, transitionVolume(199_500, 200_000, 2_000, fadeIn = true, fadeOut = false))
        assertEquals(1f, transitionVolume(100_000, 200_000, 2_000, fadeIn = true, fadeOut = true))
    }
}
