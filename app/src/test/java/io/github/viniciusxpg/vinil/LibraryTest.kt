package io.github.viniciusxpg.vinil

import io.github.viniciusxpg.vinil.data.Library
import io.github.viniciusxpg.vinil.data.Song
import io.github.viniciusxpg.vinil.data.folderName
import io.github.viniciusxpg.vinil.data.normalizeFolder
import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryTest {

    private fun song(
        id: Long,
        title: String,
        artist: String = "Artista",
        album: String = "Álbum",
        albumId: Long = 1,
        track: Int = 0,
        folder: String = "Music/",
    ) = Song(id, title, artist, album, albumId, durationMs = 1000, track = track, folder = folder)

    @Test
    fun albumsAreSortedByTrackNumber() {
        val library = Library.from(
            listOf(
                song(1, "C", track = 3),
                song(2, "A", track = 1),
                song(3, "B", track = 2),
            ),
        )
        assertEquals(listOf("A", "B", "C"), library.album(1)!!.songs.map { it.title })
    }

    @Test
    fun albumArtistIsTheMostCommonOne() {
        val library = Library.from(
            listOf(
                song(1, "a", artist = "Fulano"),
                song(2, "b", artist = "Fulano"),
                song(3, "c", artist = "Convidado"),
            ),
        )
        assertEquals("Fulano", library.albums.single().artist)
    }

    @Test
    fun artistsCountDistinctAlbums() {
        val library = Library.from(
            listOf(
                song(1, "a", artist = "Fulano", albumId = 1),
                song(2, "b", artist = "Fulano", albumId = 2),
                song(3, "c", artist = "Fulano", albumId = 2),
                song(4, "d", artist = "Beltrano", albumId = 3),
            ),
        )
        assertEquals(listOf("Beltrano", "Fulano"), library.artists.map { it.name })
        assertEquals(2, library.artist("Fulano")!!.albumCount)
        assertEquals(3, library.artist("Fulano")!!.songs.size)
    }

    @Test
    fun foldersIgnoreTrailingSlash() {
        val library = Library.from(
            listOf(
                song(1, "a", folder = "Music/Rock/"),
                song(2, "b", folder = "Music/Rock"),
                song(3, "c", folder = "Download/"),
            ),
        )
        assertEquals(listOf("Download", "Rock"), library.folders.map { it.name })
        assertEquals(2, library.folder("Music/Rock")!!.songs.size)
    }

    @Test
    fun folderNames() {
        assertEquals("Rock", folderName("Music/Rock/"))
        assertEquals("Music", folderName("/storage/emulated/0/Music"))
        assertEquals("/", folderName("/"))
        assertEquals("/", folderName(""))
        assertEquals("/", normalizeFolder("/"))
    }
}
