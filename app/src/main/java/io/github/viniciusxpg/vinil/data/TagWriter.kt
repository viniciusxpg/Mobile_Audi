package io.github.viniciusxpg.vinil.data

import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import java.io.File
import java.util.logging.Level
import java.util.logging.Logger

/** Informações editáveis de uma música. Campos em branco são apagados do arquivo. */
data class TagValues(
    val title: String,
    val artist: String,
    val album: String,
    /** Número da faixa no álbum, ou null para apagar. */
    val track: Int?,
)

/**
 * Lê e grava as tags (ID3, Vorbis, MP4...) de um arquivo de áudio local usando a jaudiotagger.
 * Kotlin puro: o app chama isto numa cópia temporária do arquivo (ver TagEditor).
 */
object TagWriter {

    /** Extensões que a jaudiotagger sabe gravar. */
    val supportedExtensions = setOf("mp3", "flac", "ogg", "oga", "m4a", "m4b", "mp4", "wav", "aif", "aiff", "wma", "dsf", "opus")

    init {
        // A biblioteca escreve muito log por padrão.
        Logger.getLogger("org.jaudiotagger").level = Level.OFF
    }

    fun isSupported(extension: String): Boolean = extension.lowercase() in supportedExtensions

    fun write(file: File, values: TagValues) {
        val audioFile = AudioFileIO.read(file)
        val tag = audioFile.tagOrCreateAndSetDefault
        setOrDelete(tag, FieldKey.TITLE, values.title)
        setOrDelete(tag, FieldKey.ARTIST, values.artist)
        setOrDelete(tag, FieldKey.ALBUM, values.album)
        setOrDelete(tag, FieldKey.TRACK, values.track?.takeIf { it > 0 }?.toString().orEmpty())
        audioFile.commit()
    }

    fun read(file: File): TagValues {
        val tag = AudioFileIO.read(file).tag
        return TagValues(
            title = tag?.getFirst(FieldKey.TITLE).orEmpty(),
            artist = tag?.getFirst(FieldKey.ARTIST).orEmpty(),
            album = tag?.getFirst(FieldKey.ALBUM).orEmpty(),
            track = tag?.getFirst(FieldKey.TRACK)?.substringBefore('/')?.trim()?.toIntOrNull(),
        )
    }

    private fun setOrDelete(tag: org.jaudiotagger.tag.Tag, key: FieldKey, value: String) {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) tag.deleteField(key) else tag.setField(key, trimmed)
    }
}
