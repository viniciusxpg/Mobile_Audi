package com.mobileaudi.player.playback

/**
 * Identificadores dos itens de mídia compartilhados entre o app, o serviço
 * de reprodução e o Android Auto.
 *
 * Árvore de navegação:
 * ```
 * root
 * ├── songs            -> song:{id}, song:{id}, ...
 * └── albums           -> album:{id}, ...
 *     └── album:{id}   -> song:{id}, ...
 * ```
 */
object MediaIds {
    const val ROOT = "root"
    const val SONGS = "songs"
    const val ALBUMS = "albums"

    private const val SONG_PREFIX = "song:"
    private const val ALBUM_PREFIX = "album:"

    fun song(id: Long): String = "$SONG_PREFIX$id"

    fun album(id: Long): String = "$ALBUM_PREFIX$id"

    fun parseSongId(mediaId: String): Long? = parse(mediaId, SONG_PREFIX)

    fun parseAlbumId(mediaId: String): Long? = parse(mediaId, ALBUM_PREFIX)

    private fun parse(mediaId: String, prefix: String): Long? =
        if (mediaId.startsWith(prefix)) mediaId.substring(prefix.length).toLongOrNull() else null
}

/** Retorna a página [page] (base 0) de tamanho [pageSize], como pede o MediaLibraryService. */
fun <T> List<T>.page(page: Int, pageSize: Int): List<T> {
    if (page < 0 || pageSize <= 0 || pageSize == Int.MAX_VALUE) return this
    val from = page.toLong() * pageSize
    if (from >= size) return emptyList()
    val to = minOf(size.toLong(), from + pageSize)
    return subList(from.toInt(), to.toInt())
}
