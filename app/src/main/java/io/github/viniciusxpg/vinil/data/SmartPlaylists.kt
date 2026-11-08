package io.github.viniciusxpg.vinil.data

/** Playlists que se montam sozinhas a partir da biblioteca, dos favoritos e do histórico. */
enum class SmartPlaylistKind(val key: String) {
    FAVORITES("favorites"),
    MOST_PLAYED("most_played"),
    RECENTLY_PLAYED("recently_played"),
    RECENTLY_ADDED("recently_added"),
    ;

    companion object {
        fun fromKey(key: String): SmartPlaylistKind? = entries.firstOrNull { it.key == key }
    }
}

data class SmartPlaylist(val kind: SmartPlaylistKind, val songs: List<Song>)

object SmartPlaylists {
    /** Tamanho máximo das listas de mais tocadas e recentes. */
    const val LIMIT = 50

    /** Favoritas na ordem em que foram curtidas (a mais recente primeiro). */
    fun favorites(library: Library, favoriteIds: List<Long>): List<Song> =
        favoriteIds.mapNotNull(library::song)

    fun mostPlayed(library: Library, stats: Map<Long, PlayStat>): List<Song> =
        stats.entries
            .sortedWith(compareByDescending<Map.Entry<Long, PlayStat>> { it.value.count }.thenByDescending { it.value.lastPlayedMs })
            .mapNotNull { library.song(it.key) }
            .take(LIMIT)

    fun recentlyPlayed(library: Library, stats: Map<Long, PlayStat>): List<Song> =
        stats.entries
            .sortedByDescending { it.value.lastPlayedMs }
            .mapNotNull { library.song(it.key) }
            .take(LIMIT)

    fun recentlyAdded(library: Library): List<Song> =
        library.songs.filter { it.dateAdded > 0 }.sortedByDescending { it.dateAdded }.take(LIMIT)

    fun all(library: Library, favoriteIds: List<Long>, stats: Map<Long, PlayStat>): List<SmartPlaylist> = listOf(
        SmartPlaylist(SmartPlaylistKind.FAVORITES, favorites(library, favoriteIds)),
        SmartPlaylist(SmartPlaylistKind.MOST_PLAYED, mostPlayed(library, stats)),
        SmartPlaylist(SmartPlaylistKind.RECENTLY_PLAYED, recentlyPlayed(library, stats)),
        SmartPlaylist(SmartPlaylistKind.RECENTLY_ADDED, recentlyAdded(library)),
    )

    fun of(kind: SmartPlaylistKind, library: Library, favoriteIds: List<Long>, stats: Map<Long, PlayStat>): List<Song> =
        when (kind) {
            SmartPlaylistKind.FAVORITES -> favorites(library, favoriteIds)
            SmartPlaylistKind.MOST_PLAYED -> mostPlayed(library, stats)
            SmartPlaylistKind.RECENTLY_PLAYED -> recentlyPlayed(library, stats)
            SmartPlaylistKind.RECENTLY_ADDED -> recentlyAdded(library)
        }

    /** Curtir/descurtir: a música curtida vai para o topo das Favoritas. */
    fun toggleFavorite(favoriteIds: List<Long>, songId: Long): List<Long> =
        if (songId in favoriteIds) favoriteIds - songId else listOf(songId) + favoriteIds
}
