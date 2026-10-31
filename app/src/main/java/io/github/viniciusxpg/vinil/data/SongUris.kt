package io.github.viniciusxpg.vinil.data

import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore

/** URI de conteúdo do arquivo de áudio (usada para tocar e para ler a capa). */
val Song.uri: Uri
    get() = songContentUri(id)

fun songContentUri(songId: Long): Uri =
    ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, songId)
