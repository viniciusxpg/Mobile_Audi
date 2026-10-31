package io.github.viniciusxpg.vinil.data

import java.text.Normalizer

data class SearchResults(
    val songs: List<Song>,
    val albums: List<Album>,
    val artists: List<Artist>,
) {
    val isEmpty: Boolean
        get() = songs.isEmpty() && albums.isEmpty() && artists.isEmpty()

    companion object {
        val EMPTY = SearchResults(emptyList(), emptyList(), emptyList())
    }
}

private val DIACRITICS = Regex("\\p{Mn}+")
private val WHITESPACE = Regex("\\s+")

/** Minúsculas e sem acentos: "Açúcar" -> "acucar". */
fun normalizeForSearch(text: String): String =
    Normalizer.normalize(text, Normalizer.Form.NFD)
        .replace(DIACRITICS, "")
        .lowercase()

private fun searchTerms(query: String): List<String> =
    normalizeForSearch(query).split(WHITESPACE).filter { it.isNotEmpty() }

/** Verdadeiro se todas as palavras da busca aparecem em [text], em qualquer ordem. */
fun matchesQuery(text: String, query: String): Boolean {
    val terms = searchTerms(query)
    if (terms.isEmpty()) return false
    val normalized = normalizeForSearch(text)
    return terms.all { normalized.contains(it) }
}

/**
 * Músicas cujo título, artista ou álbum combinam com a busca. Músicas em que o
 * próprio título combina vêm primeiro.
 */
fun matchingSongs(songs: List<Song>, query: String): List<Song> {
    val terms = searchTerms(query)
    if (terms.isEmpty()) return emptyList()
    fun matches(text: String): Boolean {
        val normalized = normalizeForSearch(text)
        return terms.all { normalized.contains(it) }
    }
    return songs
        .filter { matches("${it.title} ${it.artist} ${it.album}") }
        .sortedByDescending { matches(it.title) }
}

fun searchLibrary(library: Library, query: String): SearchResults {
    if (searchTerms(query).isEmpty()) return SearchResults.EMPTY
    return SearchResults(
        songs = matchingSongs(library.songs, query),
        albums = library.albums.filter { matchesQuery("${it.title} ${it.artist}", query) },
        artists = library.artists.filter { matchesQuery(it.name, query) },
    )
}
