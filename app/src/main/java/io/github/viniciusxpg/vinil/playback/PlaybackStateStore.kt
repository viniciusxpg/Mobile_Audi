package io.github.viniciusxpg.vinil.playback

import android.content.Context
import androidx.core.content.edit

/**
 * Guarda a fila, a música atual, a posição e os modos (aleatório/repetir) para
 * retomar de onde parou ao reabrir o app ou ao apertar "play" no fone com o app fechado.
 *
 * A música atual é guardada pelo ID (e não pela posição na fila) para continuar
 * certa mesmo que músicas tenham sido apagadas do aparelho nesse meio-tempo.
 */
class PlaybackStateStore(context: Context) {

    data class Saved(
        val songIds: List<Long>,
        val currentSongId: Long,
        val positionMs: Long,
        val shuffle: Boolean,
        val repeatMode: Int,
    )

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveQueue(songIds: List<Long>) {
        prefs.edit { putString(KEY_QUEUE, encodeIds(songIds)) }
    }

    fun savePosition(currentSongId: Long, positionMs: Long) {
        prefs.edit {
            putLong(KEY_CURRENT_SONG, currentSongId)
            putLong(KEY_POSITION, positionMs.coerceAtLeast(0))
        }
    }

    fun saveModes(shuffle: Boolean, repeatMode: Int) {
        prefs.edit {
            putBoolean(KEY_SHUFFLE, shuffle)
            putInt(KEY_REPEAT, repeatMode)
        }
    }

    fun load(): Saved? {
        val ids = decodeIds(prefs.getString(KEY_QUEUE, null))
        if (ids.isEmpty()) return null
        return Saved(
            songIds = ids,
            currentSongId = prefs.getLong(KEY_CURRENT_SONG, ids.first()),
            positionMs = prefs.getLong(KEY_POSITION, 0),
            shuffle = prefs.getBoolean(KEY_SHUFFLE, false),
            repeatMode = prefs.getInt(KEY_REPEAT, 0),
        )
    }

    fun clear() {
        prefs.edit { clear() }
    }

    companion object {
        private const val PREFS_NAME = "playback_state"
        private const val KEY_QUEUE = "queue"
        private const val KEY_CURRENT_SONG = "current_song"
        private const val KEY_POSITION = "position"
        private const val KEY_SHUFFLE = "shuffle"
        private const val KEY_REPEAT = "repeat"

        fun encodeIds(ids: List<Long>): String = ids.joinToString(",")

        fun decodeIds(encoded: String?): List<Long> =
            encoded.orEmpty().split(',').mapNotNull { it.trim().toLongOrNull() }
    }
}
