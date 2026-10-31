package io.github.viniciusxpg.vinil

import io.github.viniciusxpg.vinil.data.Library
import io.github.viniciusxpg.vinil.data.Song
import io.github.viniciusxpg.vinil.data.matchesQuery
import io.github.viniciusxpg.vinil.data.matchingSongs
import io.github.viniciusxpg.vinil.data.normalizeForSearch
import io.github.viniciusxpg.vinil.data.searchLibrary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchTest {

    private val songs = listOf(
        Song(1, "Açúcar Mascavo", "Banda X", "Doce", albumId = 10, durationMs = 1000),
        Song(2, "Chuva", "Maria Açúcar", "Tempo", albumId = 20, durationMs = 1000),
        Song(3, "Outra", "Banda X", "Doce", albumId = 10, durationMs = 1000),
    )
    private val library = Library.from(songs)

    @Test
    fun ignoresAccentsAndCase() {
        assertEquals("acucar", normalizeForSearch("AÇÚCAR"))
        assertTrue(matchesQuery("Açúcar Mascavo", "acucar"))
        assertTrue(matchesQuery("Açúcar Mascavo", "MASCAVO açu"))
        assertFalse(matchesQuery("Açúcar Mascavo", "mel"))
        assertFalse(matchesQuery("Qualquer coisa", "   "))
    }

    @Test
    fun titleMatchesComeFirst() {
        val result = matchingSongs(songs, "acucar")
        // "Chuva" combina pelo artista; "Açúcar Mascavo" combina pelo título e vem antes.
        assertEquals(listOf(1L, 2L), result.map { it.id })
    }

    @Test
    fun searchFindsAlbumsAndArtists() {
        val results = searchLibrary(library, "banda")
        assertEquals(listOf("Banda X"), results.artists.map { it.name })
        assertEquals(listOf("Doce"), results.albums.map { it.title })
        assertEquals(setOf(1L, 3L), results.songs.map { it.id }.toSet())
    }

    @Test
    fun blankQueryReturnsNothing() {
        assertTrue(searchLibrary(library, "").isEmpty)
    }
}
