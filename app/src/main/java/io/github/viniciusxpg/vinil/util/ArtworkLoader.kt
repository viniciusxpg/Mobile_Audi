package io.github.viniciusxpg.vinil.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.LruCache
import android.util.Size

/**
 * Capas já carregadas, por URI + tamanho. Usa até 1/8 da memória do app e lembra também
 * quais arquivos não têm capa, para não reabrir o arquivo toda vez que a linha aparece.
 */
object ArtworkCache {
    private val maxKb = (Runtime.getRuntime().maxMemory() / 1024 / 8).toInt()

    private val bitmaps = object : LruCache<String, Bitmap>(maxKb) {
        override fun sizeOf(key: String, value: Bitmap): Int = (value.byteCount / 1024).coerceAtLeast(1)
    }
    private val missing = LruCache<String, Boolean>(2_000)

    private fun key(uri: Uri, sizePx: Int) = "$uri#$sizePx"

    /** Capa em cache, sem tocar no disco (pode ser chamada na thread principal). */
    fun peek(uri: Uri, sizePx: Int): Bitmap? = bitmaps.get(key(uri, sizePx))

    /** Verdadeiro se já sabemos que o arquivo não tem capa. */
    fun isMissing(uri: Uri, sizePx: Int): Boolean = missing.get(key(uri, sizePx)) == true

    /** Lê a capa usando o cache. Chame fora da thread principal. */
    fun load(context: Context, uri: Uri, sizePx: Int): Bitmap? {
        val key = key(uri, sizePx)
        bitmaps.get(key)?.let { return it }
        if (missing.get(key) == true) return null
        val bitmap = loadArtworkBitmap(context, uri, sizePx)
        if (bitmap != null) bitmaps.put(key, bitmap) else missing.put(key, true)
        return bitmap
    }

    /** Esquece tudo (ex.: arquivos alterados no aparelho). */
    fun clear() {
        bitmaps.evictAll()
        missing.evictAll()
    }
}

/**
 * Lê a capa embutida no arquivo de áudio, sem cache. Chame fora da thread principal.
 * Retorna null se o arquivo não tiver capa.
 */
fun loadArtworkBitmap(context: Context, uri: Uri, sizePx: Int): Bitmap? = runCatching {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        context.contentResolver.loadThumbnail(uri, Size(sizePx, sizePx), null)
    } else {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            retriever.embeddedPicture?.let { bytes ->
                // Reduz a imagem já na decodificação para não gastar memória à toa.
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                var sample = 1
                while (bounds.outWidth / (sample * 2) >= sizePx && bounds.outHeight / (sample * 2) >= sizePx) {
                    sample *= 2
                }
                val options = BitmapFactory.Options().apply { inSampleSize = sample }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            }
        } finally {
            retriever.release()
        }
    }
}.getOrNull()
