package io.github.viniciusxpg.vinil.playback

/**
 * Identificadores dos itens de mídia compartilhados entre o app, o serviço
 * de reprodução e o Android Auto.
 *
 * Árvore de navegação:
 * ```
 * root
 * ├── songs              -> song:{id}, ...
 * ├── albums             -> album:{id}, ...
 * │   └── album:{id}     -> song:{id}, ...
 * ├── playlists          -> smart:{tipo}, ..., playlist:{id}, ...
 * │   └── playlist:{id}  -> song:{id}, ...
 * ├── artists            -> artist:{nome}, ...
 * │   └── artist:{nome}  -> song:{id}, ...
 * └── folders            -> folder:{caminho}, ...
 *     └── folder:{path}  -> song:{id}, ...
 * ```
 */
object MediaIds {
    const val ROOT = "root"
    const val SONGS = "songs"
    const val ALBUMS = "albums"
    const val ARTISTS = "artists"
    const val FOLDERS = "folders"
    const val PLAYLISTS = "playlists"

    private const val SONG_PREFIX = "song:"
    private const val ALBUM_PREFIX = "album:"
    private const val ARTIST_PREFIX = "artist:"
    private const val FOLDER_PREFIX = "folder:"
    private const val PLAYLIST_PREFIX = "playlist:"
    private const val SMART_PREFIX = "smart:"

    fun song(id: Long): String = "$SONG_PREFIX$id"

    fun album(id: Long): String = "$ALBUM_PREFIX$id"

    fun artist(name: String): String = "$ARTIST_PREFIX$name"

    fun folder(path: String): String = "$FOLDER_PREFIX$path"

    fun playlist(id: Long): String = "$PLAYLIST_PREFIX$id"

    /** Playlist automática (favoritas, mais tocadas...), pela chave de SmartPlaylistKind. */
    fun smart(key: String): String = "$SMART_PREFIX$key"

    fun parseSongId(mediaId: String): Long? = suffix(mediaId, SONG_PREFIX)?.toLongOrNull()

    fun parseAlbumId(mediaId: String): Long? = suffix(mediaId, ALBUM_PREFIX)?.toLongOrNull()

    fun parseArtist(mediaId: String): String? = suffix(mediaId, ARTIST_PREFIX)

    fun parseFolder(mediaId: String): String? = suffix(mediaId, FOLDER_PREFIX)

    fun parsePlaylistId(mediaId: String): Long? = suffix(mediaId, PLAYLIST_PREFIX)?.toLongOrNull()

    fun parseSmart(mediaId: String): String? = suffix(mediaId, SMART_PREFIX)

    private fun suffix(mediaId: String, prefix: String): String? =
        if (mediaId.startsWith(prefix)) mediaId.substring(prefix.length) else null
}

/** Retorna a página [page] (base 0) de tamanho [pageSize], como pede o MediaLibraryService. */
fun <T> List<T>.page(page: Int, pageSize: Int): List<T> {
    if (page < 0 || pageSize <= 0 || pageSize == Int.MAX_VALUE) return this
    val from = page.toLong() * pageSize
    if (from >= size) return emptyList()
    val to = minOf(size.toLong(), from + pageSize)
    return subList(from.toInt(), to.toInt())
}
