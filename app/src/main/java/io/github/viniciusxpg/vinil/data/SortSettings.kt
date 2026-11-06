package io.github.viniciusxpg.vinil.data

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Guarda a ordem escolhida em cada aba (ver [LibrarySort]). */
class SortSettings private constructor(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _sort = MutableStateFlow(
        LibrarySort(
            songs = enumOr(prefs.getString(KEY_SONGS, null), SongSort.TITLE),
            songsDescending = prefs.getBoolean(KEY_SONGS_DESC, false),
            albums = enumOr(prefs.getString(KEY_ALBUMS, null), AlbumSort.TITLE),
            albumsDescending = prefs.getBoolean(KEY_ALBUMS_DESC, false),
            artists = enumOr(prefs.getString(KEY_ARTISTS, null), ArtistSort.NAME),
            artistsDescending = prefs.getBoolean(KEY_ARTISTS_DESC, false),
        ),
    )
    val sort: StateFlow<LibrarySort> = _sort.asStateFlow()

    fun update(sort: LibrarySort) {
        prefs.edit {
            putString(KEY_SONGS, sort.songs.name)
            putBoolean(KEY_SONGS_DESC, sort.songsDescending)
            putString(KEY_ALBUMS, sort.albums.name)
            putBoolean(KEY_ALBUMS_DESC, sort.albumsDescending)
            putString(KEY_ARTISTS, sort.artists.name)
            putBoolean(KEY_ARTISTS_DESC, sort.artistsDescending)
        }
        _sort.value = sort
    }

    companion object {
        private const val PREFS_NAME = "sort_settings"
        private const val KEY_SONGS = "songs"
        private const val KEY_SONGS_DESC = "songs_desc"
        private const val KEY_ALBUMS = "albums"
        private const val KEY_ALBUMS_DESC = "albums_desc"
        private const val KEY_ARTISTS = "artists"
        private const val KEY_ARTISTS_DESC = "artists_desc"

        private inline fun <reified T : Enum<T>> enumOr(name: String?, default: T): T =
            enumValues<T>().firstOrNull { it.name == name } ?: default

        @Volatile
        private var instance: SortSettings? = null

        fun getInstance(context: Context): SortSettings =
            instance ?: synchronized(this) {
                instance ?: SortSettings(context.applicationContext).also { instance = it }
            }
    }
}
