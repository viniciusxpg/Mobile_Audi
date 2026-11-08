package io.github.viniciusxpg.vinil.data

import android.Manifest
import android.app.RecoverableSecurityException
import android.content.Context
import android.content.IntentSender
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** O que falta para o app poder gravar no arquivo de áudio. */
sealed interface WriteAccess {
    /** Pode gravar já. */
    data object Granted : WriteAccess

    /** O Android precisa que o usuário confirme numa caixa do sistema (Android 10+). */
    data class NeedsApproval(val intentSender: IntentSender) : WriteAccess

    /** Android 8–9: falta a permissão de gravar no armazenamento. */
    data object NeedsStoragePermission : WriteAccess
}

class TagEditException(message: String) : Exception(message)

/**
 * Edição das informações (tags) de uma música direto no arquivo.
 *
 * O arquivo é copiado para o cache do app, editado lá ([TagWriter]) e só então gravado de
 * volta por cima do original: se algo falhar no meio, o original fica intacto. Depois o
 * Android reindexa o arquivo e a biblioteca se atualiza sozinha.
 */
object TagEditor {

    /** A biblioteca de tags usa APIs de arquivo do Android 8 (API 26) em diante. */
    val isAvailable: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

    const val LEGACY_WRITE_PERMISSION = Manifest.permission.WRITE_EXTERNAL_STORAGE

    fun writeAccess(context: Context, uri: Uri): WriteAccess = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ->
            WriteAccess.NeedsApproval(MediaStore.createWriteRequest(context.contentResolver, listOf(uri)).intentSender)

        // Só o Android 10 chega aqui (o 11+ caiu no caso acima).
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> android10Access(context, uri)

        ContextCompat.checkSelfPermission(context, LEGACY_WRITE_PERMISSION) == PackageManager.PERMISSION_GRANTED ->
            WriteAccess.Granted

        else -> WriteAccess.NeedsStoragePermission
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun android10Access(context: Context, uri: Uri): WriteAccess = try {
        context.contentResolver.openFileDescriptor(uri, "rw")?.close()
        WriteAccess.Granted
    } catch (e: RecoverableSecurityException) {
        WriteAccess.NeedsApproval(e.userAction.actionIntent.intentSender)
    }

    /** Grava [values] no arquivo de [uri]. Chame só depois de [writeAccess] liberar. */
    suspend fun write(context: Context, uri: Uri, values: TagValues): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver
            val (displayName, path) = fileInfo(context, uri)
            val extension = displayName.substringAfterLast('.', "")
            if (!TagWriter.isSupported(extension)) throw TagEditException("formato .$extension")

            val workDir = File(context.cacheDir, "tag_edit").apply { mkdirs() }
            val temp = File(workDir, "edit.$extension")
            try {
                resolver.openInputStream(uri)?.use { input -> temp.outputStream().use(input::copyTo) }
                    ?: throw TagEditException("não foi possível ler o arquivo")
                TagWriter.write(temp, values)
                // "wt": substitui o conteúdo inteiro (sem "t", sobraria lixo no fim se o arquivo encolher).
                resolver.openOutputStream(uri, "wt")?.use { output -> temp.inputStream().use { it.copyTo(output) } }
                    ?: throw TagEditException("não foi possível gravar o arquivo")
            } finally {
                workDir.listFiles()?.forEach(File::delete)
            }
            // Pede ao Android para reler o arquivo, atualizando título/artista na biblioteca.
            path?.let { MediaScannerConnection.scanFile(context, arrayOf(it), null, null) }
            Unit
        }
    }

    @Suppress("DEPRECATION") // DATA ainda é a forma de obter o caminho para o MediaScanner.
    private fun fileInfo(context: Context, uri: Uri): Pair<String, String?> {
        val projection = arrayOf(MediaStore.Audio.Media.DISPLAY_NAME, MediaStore.Audio.Media.DATA)
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getString(0).orEmpty() to cursor.getString(1)
            }
        }
        throw TagEditException("arquivo não encontrado")
    }

    /** Valores iniciais do formulário a partir dos dados da biblioteca. */
    fun initialValues(song: Song, unknownArtist: String, unknownAlbum: String) = TagValues(
        title = song.title,
        artist = song.artist.takeUnless { it == unknownArtist }.orEmpty(),
        album = song.album.takeUnless { it == unknownAlbum }.orEmpty(),
        track = (song.track % 1000).takeIf { it > 0 },
    )
}
