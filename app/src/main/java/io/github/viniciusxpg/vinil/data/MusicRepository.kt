package io.github.viniciusxpg.vinil.data

import android.content.Context
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import io.github.viniciusxpg.vinil.R
import io.github.viniciusxpg.vinil.util.PermissionUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Lê as músicas do aparelho pelo MediaStore e mantém a biblioteca em cache na memória.
 *
 * É compartilhado entre a interface (ViewModel) e o serviço de reprodução
 * (que usa os mesmos dados para a navegação no Android Auto).
 */
class MusicRepository private constructor(private val context: Context) {

    private val mutex = Mutex()
    private var cache: Library? = null

    suspend fun getLibrary(forceRefresh: Boolean = false): Library = mutex.withLock {
        cache?.takeIf { !forceRefresh }?.let { return@withLock it }
        // Sem permissão não há o que ler, e não guardamos a biblioteca vazia no cache
        // para que a leitura aconteça assim que a permissão for concedida.
        if (!PermissionUtils.hasAudioPermission(context)) return@withLock Library.EMPTY
        val library = withContext(Dispatchers.IO) { Library.from(querySongs()) }
        cache = library
        library
    }

    suspend fun getSongs(): List<Song> = getLibrary().songs

    private fun querySongs(): List<Song> {
        val usesRelativePath = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        val collection = if (usesRelativePath) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }
        // Android 10+ expõe a pasta relativa; versões anteriores só têm o caminho completo.
        @Suppress("DEPRECATION")
        val folderColumn = if (usesRelativePath) {
            MediaStore.Audio.Media.RELATIVE_PATH
        } else {
            MediaStore.Audio.Media.DATA
        }
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.TRACK,
            folderColumn,
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        val unknownArtist = context.getString(R.string.unknown_artist)
        val unknownAlbum = context.getString(R.string.unknown_album)
        val songs = mutableListOf<Song>()

        try {
            context.contentResolver.query(collection, projection, selection, null, sortOrder)
                ?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                    val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                    val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                    val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                    val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                    val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                    val trackCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
                    val folderCol = cursor.getColumnIndexOrThrow(folderColumn)

                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idCol)
                        val rawFolder = cursor.getString(folderCol).orEmpty()
                        songs += Song(
                            id = id,
                            title = cursor.getString(titleCol).orIfBlank { "#$id" },
                            artist = cursor.getString(artistCol).cleanTag(unknownArtist),
                            album = cursor.getString(albumCol).cleanTag(unknownAlbum),
                            albumId = cursor.getLong(albumIdCol),
                            durationMs = cursor.getLong(durationCol),
                            track = cursor.getInt(trackCol),
                            folder = if (usesRelativePath) {
                                rawFolder
                            } else {
                                File(rawFolder).parent.orEmpty()
                            },
                        )
                    }
                }
        } catch (e: SecurityException) {
            Log.w(TAG, "Sem permissão para ler o MediaStore", e)
            return emptyList()
        }
        return songs
    }

    private inline fun String?.orIfBlank(fallback: () -> String): String =
        if (isNullOrBlank()) fallback() else this

    /** O MediaStore usa "<unknown>" para tags ausentes. */
    private fun String?.cleanTag(fallback: String): String =
        if (isNullOrBlank() || this == MediaStore.UNKNOWN_STRING) fallback else this

    companion object {
        private const val TAG = "MusicRepository"

        @Volatile
        private var instance: MusicRepository? = null

        fun getInstance(context: Context): MusicRepository =
            instance ?: synchronized(this) {
                instance ?: MusicRepository(context.applicationContext).also { instance = it }
            }
    }
}
