package io.github.viniciusxpg.vinil.data

import org.json.JSONObject

/** Quantas vezes uma música foi tocada e quando foi a última (ms desde 1970). */
data class PlayStat(val count: Int, val lastPlayedMs: Long)

/** Regras e formato do histórico de reprodução. Kotlin puro (testável na JVM). */
object PlayStatsCodec {

    /** Ouvir 30 s (ou metade da música, se ela for mais curta) conta como uma reprodução. */
    fun playThresholdMs(durationMs: Long): Long =
        if (durationMs > 0) minOf(30_000L, durationMs / 2) else 30_000L

    fun record(stats: Map<Long, PlayStat>, songId: Long, nowMs: Long): Map<Long, PlayStat> {
        val previous = stats[songId]
        return stats + (songId to PlayStat((previous?.count ?: 0) + 1, nowMs))
    }

    fun encode(stats: Map<Long, PlayStat>): String {
        val root = JSONObject()
        stats.forEach { (id, stat) ->
            root.put(id.toString(), JSONObject().put("c", stat.count).put("l", stat.lastPlayedMs))
        }
        return root.toString()
    }

    fun decode(json: String?): Map<Long, PlayStat> {
        if (json.isNullOrBlank()) return emptyMap()
        val root = runCatching { JSONObject(json) }.getOrNull() ?: return emptyMap()
        return root.keys().asSequence().mapNotNull { key ->
            val id = key.toLongOrNull() ?: return@mapNotNull null
            val obj = root.optJSONObject(key) ?: return@mapNotNull null
            id to PlayStat(obj.optInt("c", 0), obj.optLong("l", 0))
        }.filter { it.second.count > 0 }.toMap()
    }
}
