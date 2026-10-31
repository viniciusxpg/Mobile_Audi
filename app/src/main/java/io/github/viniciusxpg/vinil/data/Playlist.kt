package io.github.viniciusxpg.vinil.data

import org.json.JSONArray
import org.json.JSONObject

/** Playlist criada pelo usuário. Guarda só os IDs das músicas, na ordem escolhida. */
data class Playlist(
    val id: Long,
    val name: String,
    val songIds: List<Long>,
) {
    /** Músicas da playlist que ainda existem no aparelho, na ordem da playlist. */
    fun songsIn(library: Library): List<Song> = songIds.mapNotNull(library::song)
}

/** Conversão das playlists de/para o JSON salvo em disco. */
object PlaylistCodec {
    private const val VERSION = 1

    fun encode(playlists: List<Playlist>): String {
        val array = JSONArray()
        playlists.forEach { playlist ->
            array.put(
                JSONObject()
                    .put("id", playlist.id)
                    .put("name", playlist.name)
                    .put("songs", JSONArray(playlist.songIds)),
            )
        }
        return JSONObject()
            .put("version", VERSION)
            .put("playlists", array)
            .toString()
    }

    /** Lê o JSON; entradas inválidas são ignoradas em vez de derrubar o app. */
    fun decode(json: String?): List<Playlist> {
        if (json.isNullOrBlank()) return emptyList()
        val array = runCatching { JSONObject(json).optJSONArray("playlists") }.getOrNull()
            ?: return emptyList()
        return (0 until array.length()).mapNotNull { index ->
            val obj = array.optJSONObject(index) ?: return@mapNotNull null
            val id = obj.optLong("id", -1)
            val name = obj.optString("name", "")
            if (id < 0 || name.isBlank()) return@mapNotNull null
            val songs = obj.optJSONArray("songs")
            val songIds = if (songs == null) {
                emptyList()
            } else {
                (0 until songs.length()).map { songs.optLong(it, -1) }.filter { it >= 0 }
            }
            Playlist(id, name, songIds)
        }
    }
}
