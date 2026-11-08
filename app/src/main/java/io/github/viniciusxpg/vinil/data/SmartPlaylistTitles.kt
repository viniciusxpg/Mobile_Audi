package io.github.viniciusxpg.vinil.data

import androidx.annotation.StringRes
import io.github.viniciusxpg.vinil.R

/** Nome exibido de cada playlist automática. */
@get:StringRes
val SmartPlaylistKind.titleRes: Int
    get() = when (this) {
        SmartPlaylistKind.FAVORITES -> R.string.smart_favorites
        SmartPlaylistKind.MOST_PLAYED -> R.string.smart_most_played
        SmartPlaylistKind.RECENTLY_PLAYED -> R.string.smart_recently_played
        SmartPlaylistKind.RECENTLY_ADDED -> R.string.smart_recently_added
    }
