package io.github.viniciusxpg.vinil

import io.github.viniciusxpg.vinil.data.Library
import io.github.viniciusxpg.vinil.data.Playlist
import io.github.viniciusxpg.vinil.data.PlaylistCodec
import io.github.viniciusxpg.vinil.data.Song
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaylistCodecTest {

    @Test
    fun roundTrip() {
        val playlists = listOf(
            Playlist(1, "Viagem", listOf(3L, 1L, 2L)),
            Playlist(2, "Vazia \"com aspas\"", emptyList()),
        )
        assertEquals(playlists, PlaylistCodec.decode(PlaylistCodec.encode(playlists)))
    }

    @Test
    fun invalidJsonBecomesEmpty() {
        assertEquals(emptyList<Playlist>(), PlaylistCodec.decode(null))
        assertEquals(emptyList<Playlist>(), PlaylistCodec.decode("isso não é json"))
        assertEquals(emptyList<Playlist>(), PlaylistCodec.decode("""{"playlists":[{"id":1,"name":""}]}"""))
    }

    @Test
    fun songsInSkipsDeletedSongsAndKeepsOrder() {
        val library = Library.from(
            listOf(
                Song(1, "Um", "A", "X", albumId = 1, durationMs = 1),
                Song(2, "Dois", "A", "X", albumId = 1, durationMs = 1),
            ),
        )
        val playlist = Playlist(1, "P", listOf(2L, 99L, 1L))
        assertEquals(listOf(2L, 1L), playlist.songsIn(library).map { it.id })
    }
}
