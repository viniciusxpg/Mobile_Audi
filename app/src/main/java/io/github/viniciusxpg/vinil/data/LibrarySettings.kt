package io.github.viniciusxpg.vinil.data

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Guarda quais pastas ficam fora da biblioteca (ver [LibraryFilter]). */
class LibrarySettings private constructor(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _filter = MutableStateFlow(
        LibraryFilter(
            hideMessagingAndRadio = prefs.getBoolean(KEY_HIDE_DEFAULT, true),
            hiddenFolders = prefs.getStringSet(KEY_HIDDEN_FOLDERS, emptySet()).orEmpty().toSet(),
        ),
    )
    val filter: StateFlow<LibraryFilter> = _filter.asStateFlow()

    fun setHideMessagingAndRadio(hide: Boolean) {
        prefs.edit { putBoolean(KEY_HIDE_DEFAULT, hide) }
        _filter.value = _filter.value.copy(hideMessagingAndRadio = hide)
    }

    fun setFolderHidden(path: String, hidden: Boolean) {
        val folder = normalizeFolder(path)
        val current = _filter.value.hiddenFolders
        val updated = if (hidden) current + folder else current - folder
        // Sempre um conjunto novo: o SharedPreferences não deve receber o mesmo objeto alterado.
        prefs.edit { putStringSet(KEY_HIDDEN_FOLDERS, HashSet(updated)) }
        _filter.value = _filter.value.copy(hiddenFolders = updated)
    }

    companion object {
        private const val PREFS_NAME = "library_settings"
        private const val KEY_HIDE_DEFAULT = "hide_messaging_and_radio"
        private const val KEY_HIDDEN_FOLDERS = "hidden_folders"

        @Volatile
        private var instance: LibrarySettings? = null

        fun getInstance(context: Context): LibrarySettings =
            instance ?: synchronized(this) {
                instance ?: LibrarySettings(context.applicationContext).also { instance = it }
            }
    }
}
