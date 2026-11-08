package io.github.viniciusxpg.vinil.data

import android.content.Context
import android.util.AtomicFile
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

/** Histórico de reprodução (quantas vezes e quando cada música tocou), salvo em play_stats.json. */
class PlayStatsRepository private constructor(context: Context) {

    private val file = AtomicFile(File(context.filesDir, FILE_NAME))
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()

    private val _stats = MutableStateFlow(read())
    val stats: StateFlow<Map<Long, PlayStat>> = _stats.asStateFlow()

    fun recordPlay(songId: Long, nowMs: Long = System.currentTimeMillis()) {
        scope.launch {
            mutex.withLock {
                val updated = PlayStatsCodec.record(_stats.value, songId, nowMs)
                _stats.value = updated
                write(updated)
            }
        }
    }

    private fun read(): Map<Long, PlayStat> = try {
        if (file.baseFile.exists()) PlayStatsCodec.decode(file.readFully().decodeToString()) else emptyMap()
    } catch (e: Exception) {
        Log.w(TAG, "Não foi possível ler o histórico", e)
        emptyMap()
    }

    private fun write(stats: Map<Long, PlayStat>) {
        val stream = try {
            file.startWrite()
        } catch (e: Exception) {
            Log.w(TAG, "Não foi possível salvar o histórico", e)
            return
        }
        try {
            stream.write(PlayStatsCodec.encode(stats).encodeToByteArray())
            file.finishWrite(stream)
        } catch (e: Exception) {
            file.failWrite(stream)
            Log.w(TAG, "Não foi possível salvar o histórico", e)
        }
    }

    companion object {
        private const val TAG = "PlayStatsRepository"
        private const val FILE_NAME = "play_stats.json"

        @Volatile
        private var instance: PlayStatsRepository? = null

        fun getInstance(context: Context): PlayStatsRepository =
            instance ?: synchronized(this) {
                instance ?: PlayStatsRepository(context.applicationContext).also { instance = it }
            }
    }
}
