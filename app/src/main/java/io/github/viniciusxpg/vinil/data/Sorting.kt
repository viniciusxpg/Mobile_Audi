package io.github.viniciusxpg.vinil.data

import java.text.Collator
import java.util.Locale

enum class SongSort { TITLE, ARTIST, ALBUM, DATE_ADDED, DURATION }

enum class AlbumSort { TITLE, ARTIST, SONG_COUNT }

enum class ArtistSort { NAME, SONG_COUNT }

/** Ordem escolhida para cada aba da biblioteca. */
data class LibrarySort(
    val songs: SongSort = SongSort.TITLE,
    val songsDescending: Boolean = false,
    val albums: AlbumSort = AlbumSort.TITLE,
    val albumsDescending: Boolean = false,
    val artists: ArtistSort = ArtistSort.NAME,
    val artistsDescending: Boolean = false,
)

/** Ordem alfabética em português: "Água" fica junto de "agora", não depois do "Z". */
private val collator: Collator = Collator.getInstance(Locale.forLanguageTag("pt-BR")).apply {
    strength = Collator.SECONDARY
}

private val byText = Comparator<String> { a, b -> collator.compare(a, b) }

private fun <T> List<T>.ordered(comparator: Comparator<T>, descending: Boolean): List<T> =
    sortedWith(if (descending) comparator.reversed() else comparator)

fun sortSongs(songs: List<Song>, by: SongSort, descending: Boolean): List<Song> {
    val title = compareBy(byText, Song::title)
    val comparator: Comparator<Song> = when (by) {
        SongSort.TITLE -> title
        SongSort.ARTIST -> compareBy(byText, Song::artist).then(compareBy(byText, Song::album)).thenBy { it.track }
        SongSort.ALBUM -> compareBy(byText, Song::album).thenBy { it.track }.then(title)
        SongSort.DATE_ADDED -> compareBy<Song> { it.dateAdded }.then(title)
        SongSort.DURATION -> compareBy<Song> { it.durationMs }.then(title)
    }
    return songs.ordered(comparator, descending)
}

fun sortAlbums(albums: List<Album>, by: AlbumSort, descending: Boolean): List<Album> {
    val title = compareBy(byText, Album::title)
    val comparator: Comparator<Album> = when (by) {
        AlbumSort.TITLE -> title
        AlbumSort.ARTIST -> compareBy(byText, Album::artist).then(title)
        AlbumSort.SONG_COUNT -> compareBy<Album> { it.songs.size }.then(title)
    }
    return albums.ordered(comparator, descending)
}

fun sortArtists(artists: List<Artist>, by: ArtistSort, descending: Boolean): List<Artist> {
    val name = compareBy(byText, Artist::name)
    val comparator: Comparator<Artist> = when (by) {
        ArtistSort.NAME -> name
        ArtistSort.SONG_COUNT -> compareBy<Artist> { it.songs.size }.then(name)
    }
    return artists.ordered(comparator, descending)
}
