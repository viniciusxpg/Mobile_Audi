package io.github.viniciusxpg.vinil.data

/** Uma faixa de áudio encontrada no MediaStore do aparelho. A URI do arquivo fica em [uri]. */
data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
    /** Número da faixa no álbum (o MediaStore usa disco * 1000 + faixa). 0 se desconhecido. */
    val track: Int = 0,
    /** Pasta onde o arquivo está, ex.: "Music/Rock" ou "/storage/emulated/0/Music/Rock". */
    val folder: String = "",
)

/** Álbum montado a partir das faixas que compartilham o mesmo ALBUM_ID, em ordem de faixa. */
data class Album(
    val id: Long,
    val title: String,
    val artist: String,
    val songs: List<Song>,
)

data class Artist(
    val name: String,
    val albumCount: Int,
    val songs: List<Song>,
)

data class Folder(
    val path: String,
    val name: String,
    val songs: List<Song>,
)
