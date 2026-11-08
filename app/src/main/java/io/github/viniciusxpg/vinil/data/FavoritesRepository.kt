package io.github.viniciusxpg.vinil.data

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Músicas favoritas (ids na ordem em que foram curtidas, a mais recente primeiro). */
class FavoritesRepository private constructor(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _favorites = MutableStateFlow(
        prefs.getString(KEY_IDS, null).orEmpty().split(',').mapNotNull { it.trim().toLongOrNull() },
    )
    val favorites: StateFlow<List<Long>> = _favorites.asStateFlow()

    fun isFavorite(songId: Long): Boolean = songId in _favorites.value

    fun toggle(songId: Long) {
        val updated = SmartPlaylists.toggleFavorite(_favorites.value, songId)
        prefs.edit { putString(KEY_IDS, updated.joinToString(",")) }
        _favorites.value = updated
    }

    companion object {
        private const val PREFS_NAME = "favorites"
        private const val KEY_IDS = "ids"

        @Volatile
        private var instance: FavoritesRepository? = null

        fun getInstance(context: Context): FavoritesRepository =
            instance ?: synchronized(this) {
                instance ?: FavoritesRepository(context.applicationContext).also { instance = it }
            }
    }
}
