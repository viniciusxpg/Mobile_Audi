package io.github.viniciusxpg.vinil

import io.github.viniciusxpg.vinil.data.TagValues
import io.github.viniciusxpg.vinil.data.TagWriter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

class TagWriterTest {

    /** WAV mínimo (meio segundo de silêncio), gerado aqui para o teste não depender de arquivos. */
    private fun silentWav(): File {
        val sampleRate = 8_000
        val samples = sampleRate / 2
        val dataSize = samples * 2
        val buffer = ByteBuffer.allocate(44 + dataSize).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put("RIFF".toByteArray()).putInt(36 + dataSize).put("WAVE".toByteArray())
        buffer.put("fmt ".toByteArray()).putInt(16).putShort(1).putShort(1)
            .putInt(sampleRate).putInt(sampleRate * 2).putShort(2).putShort(16)
        buffer.put("data".toByteArray()).putInt(dataSize)
        val file = File.createTempFile("vinil-teste", ".wav")
        file.writeBytes(buffer.array())
        file.deleteOnExit()
        return file
    }

    @Test
    fun writesAndReadsBackAccentedValues() {
        val file = silentWav()
        TagWriter.write(file, TagValues("Águas de Março", "Elis & Tom", "Elis & Tom", 1))
        assertEquals(TagValues("Águas de Março", "Elis & Tom", "Elis & Tom", 1), TagWriter.read(file))
    }

    @Test
    fun blankFieldsAreRemoved() {
        val file = silentWav()
        TagWriter.write(file, TagValues("Título", "Artista", "Álbum", 3))
        TagWriter.write(file, TagValues("Novo título", "", "", null))
        assertEquals(TagValues("Novo título", "", "", null), TagWriter.read(file))
    }

    @Test
    fun supportedExtensions() {
        assertTrue(TagWriter.isSupported("MP3"))
        assertTrue(TagWriter.isSupported("flac"))
        assertEquals(false, TagWriter.isSupported("mid"))
    }
}
