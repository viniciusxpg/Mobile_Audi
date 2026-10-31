package com.mobileaudi.player.data

import android.net.Uri

/** Uma faixa de áudio encontrada no MediaStore do aparelho. */
data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
    val uri: Uri,
)

/** Álbum montado a partir das faixas que compartilham o mesmo ALBUM_ID. */
data class Album(
    val id: Long,
    val title: String,
    val artist: String,
    val songs: List<Song>,
)
