package io.github.viniciusxpg.vinil.data

/** A biblioteca inteira, já agrupada para as abas do app e para o Android Auto. */
data class Library(
    val songs: List<Song>,
    val albums: List<Album>,
    val artists: List<Artist>,
    val folders: List<Folder>,
) {
    fun album(id: Long): Album? = albums.firstOrNull { it.id == id }

    fun artist(name: String): Artist? = artists.firstOrNull { it.name == name }

    fun folder(path: String): Folder? = folders.firstOrNull { it.path == path }

    fun song(id: Long): Song? = songs.firstOrNull { it.id == id }

    companion object {
        val EMPTY = Library(emptyList(), emptyList(), emptyList(), emptyList())

        fun from(songs: List<Song>): Library = Library(
            songs = songs,
            albums = groupAlbums(songs),
            artists = groupArtists(songs),
            folders = groupFolders(songs),
        )
    }
}

private val byTrack = compareBy<Song>({ it.track }, { it.title.lowercase() })

fun groupAlbums(songs: List<Song>): List<Album> =
    songs.groupBy { it.albumId }
        .map { (albumId, albumSongs) ->
            val sorted = albumSongs.sortedWith(byTrack)
            Album(
                id = albumId,
                title = sorted.first().album,
                artist = mostCommonArtist(sorted),
                songs = sorted,
            )
        }
        .sortedBy { it.title.lowercase() }

fun groupArtists(songs: List<Song>): List<Artist> =
    songs.groupBy { it.artist }
        .map { (name, artistSongs) ->
            Artist(
                name = name,
                albumCount = artistSongs.map { it.albumId }.distinct().size,
                songs = artistSongs.sortedWith(
                    compareBy<Song>({ it.album.lowercase() }, { it.track }, { it.title.lowercase() }),
                ),
            )
        }
        .sortedBy { it.name.lowercase() }

fun groupFolders(songs: List<Song>): List<Folder> =
    songs.groupBy { normalizeFolder(it.folder) }
        .map { (path, folderSongs) ->
            Folder(
                path = path,
                name = folderName(path),
                songs = folderSongs.sortedBy { it.title.lowercase() },
            )
        }
        .sortedWith(compareBy<Folder>({ it.name.lowercase() }, { it.path }))

/**
 * Pastas de apps de mensagem (áudios e mensagens de voz do WhatsApp, WhatsApp Business
 * e Telegram). O Android costuma marcar esses arquivos como "música", então eles são
 * filtrados aqui. Exemplos de caminho:
 * - Android 10+: `Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Voice Notes/202544/`
 * - Android 7–9: `/storage/emulated/0/WhatsApp/Media/WhatsApp Audio`
 */
fun isMessagingAppFolder(path: String): Boolean =
    path.lowercase().split('/').any { segment ->
        MESSAGING_APP_MARKERS.any { marker -> segment.contains(marker) }
    }

private val MESSAGING_APP_MARKERS = listOf("whatsapp", "telegram")

/** Remove a barra final: "Music/Rock/" -> "Music/Rock". A raiz continua sendo "/". */
fun normalizeFolder(path: String): String {
    val trimmed = path.trimEnd('/')
    return if (trimmed.isEmpty() && path.startsWith("/")) "/" else trimmed
}

/** Nome exibido da pasta: o último trecho do caminho. */
fun folderName(path: String): String {
    val normalized = normalizeFolder(path)
    if (normalized.isEmpty() || normalized == "/") return "/"
    return normalized.substringAfterLast('/')
}

/** Em coletâneas, o álbum leva o nome do artista que mais aparece. */
private fun mostCommonArtist(songs: List<Song>): String =
    songs.groupingBy { it.artist }.eachCount().maxByOrNull { it.value }?.key.orEmpty()
