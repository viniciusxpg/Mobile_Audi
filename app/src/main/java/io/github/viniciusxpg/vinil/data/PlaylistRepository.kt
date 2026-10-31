package io.github.viniciusxpg.vinil.data

import android.content.Context
import android.util.AtomicFile
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Playlists do usuário, salvas em `playlists.json` dentro do armazenamento do app.
 *
 * A escrita usa [AtomicFile], então um desligamento no meio da gravação não
 * corrompe o arquivo. É compartilhado entre a interface e o serviço (Android Auto).
 */
class PlaylistRepository private constructor(context: Context) {

    private val file = AtomicFile(File(context.filesDir, FILE_NAME))
    private val mutex = Mutex()
    private var loaded = false

    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())

    /** Playlists em ordem alfabética; atualiza a interface automaticamente. */
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    suspend fun load(): List<Playlist> = mutex.withLock {
        ensureLoaded()
        _playlists.value
    }

    suspend fun create(name: String, songIds: List<Long> = emptyList()): Playlist = update { list ->
        val playlist = Playlist(
            id = (list.maxOfOrNull { it.id } ?: 0L) + 1,
            name = name.trim(),
            songIds = songIds.distinct(),
        )
        (list + playlist) to playlist
    }

    suspend fun rename(playlistId: Long, name: String) = update { list ->
        list.map { if (it.id == playlistId) it.copy(name = name.trim()) else it } to Unit
    }

    suspend fun delete(playlistId: Long) = update { list ->
        list.filterNot { it.id == playlistId } to Unit
    }

    /** Adiciona ao fim da playlist as músicas que ainda não estão nela. Retorna quantas entraram. */
    suspend fun addSongs(playlistId: Long, songIds: List<Long>): Int = update { list ->
        var added = 0
        val newList = list.map { playlist ->
            if (playlist.id != playlistId) return@map playlist
            val existing = playlist.songIds.toSet()
            val newIds = songIds.distinct().filterNot { it in existing }
            added = newIds.size
            playlist.copy(songIds = playlist.songIds + newIds)
        }
        newList to added
    }

    suspend fun removeSong(playlistId: Long, songId: Long) = update { list ->
        list.map { playlist ->
            if (playlist.id == playlistId) playlist.copy(songIds = playlist.songIds - songId) else playlist
        } to Unit
    }

    private suspend fun <T> update(transform: (List<Playlist>) -> Pair<List<Playlist>, T>): T =
        mutex.withLock {
            ensureLoaded()
            val (newList, result) = transform(_playlists.value)
            val sorted = newList.sortedBy { it.name.lowercase() }
            _playlists.value = sorted
            withContext(Dispatchers.IO) { write(sorted) }
            result
        }

    private suspend fun ensureLoaded() {
        if (loaded) return
        _playlists.value = withContext(Dispatchers.IO) { read() }.sortedBy { it.name.lowercase() }
        loaded = true
    }

    private fun read(): List<Playlist> {
        if (!file.baseFile.exists()) return emptyList()
        return try {
            PlaylistCodec.decode(file.readFully().decodeToString())
        } catch (e: Exception) {
            Log.w(TAG, "Não foi possível ler as playlists", e)
            emptyList()
        }
    }

    private fun write(playlists: List<Playlist>) {
        val stream = try {
            file.startWrite()
        } catch (e: Exception) {
            Log.w(TAG, "Não foi possível salvar as playlists", e)
            return
        }
        try {
            stream.write(PlaylistCodec.encode(playlists).encodeToByteArray())
            file.finishWrite(stream)
        } catch (e: Exception) {
            file.failWrite(stream)
            Log.w(TAG, "Não foi possível salvar as playlists", e)
        }
    }

    companion object {
        private const val TAG = "PlaylistRepository"
        private const val FILE_NAME = "playlists.json"

        @Volatile
        private var instance: PlaylistRepository? = null

        fun getInstance(context: Context): PlaylistRepository =
            instance ?: synchronized(this) {
                instance ?: PlaylistRepository(context.applicationContext).also { instance = it }
            }
    }
}
