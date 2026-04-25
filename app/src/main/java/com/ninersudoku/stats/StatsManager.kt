package com.ninersudoku.stats

import android.content.Context
import com.ninersudoku.game.Difficulty
import com.ninersudoku.game.GameMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

data class DifficultyStats(
    val played: Int = 0,
    val won: Int = 0,
    val totalWinTimeSeconds: Long = 0,
    val totalMistakes: Long = 0,
    val totalHints: Long = 0,
    val recentWinTimes: List<Int> = emptyList(),
    val bestTimePerMode: Map<GameMode, Int> = emptyMap()
) {
    val winRate: Float get() = if (played == 0) 0f else won.toFloat() / played
    val averageWinTime: Int? get() = if (won == 0) null else (totalWinTimeSeconds / won).toInt()

    /** Backward-compatible "overall best" — minimum across all modes. */
    val bestTimeSeconds: Int? get() = bestTimePerMode.values.minOrNull()

    fun bestTime(mode: GameMode): Int? = bestTimePerMode[mode]

    fun serialize(): String {
        val obj = JSONObject()
        obj.put("p", played)
        obj.put("w", won)
        obj.put("t", totalWinTimeSeconds)
        obj.put("m", totalMistakes)
        obj.put("h", totalHints)
        obj.put("r", recentWinTimes.joinToString(","))
        val bests = JSONObject()
        bestTimePerMode.forEach { (mode, time) -> bests.put(mode.name, time) }
        obj.put("b", bests)
        return obj.toString()
    }

    companion object {
        const val RECENT_TIMES_CAP = 30

        fun parse(raw: String?): DifficultyStats {
            if (raw.isNullOrEmpty()) return DifficultyStats()
            // New JSON format
            if (raw.startsWith("{")) {
                return try {
                    val obj = JSONObject(raw)
                    val recentRaw = obj.optString("r", "")
                    val recent = if (recentRaw.isEmpty()) emptyList()
                        else recentRaw.split(",").mapNotNull { it.toIntOrNull() }
                    val bestObj = obj.optJSONObject("b") ?: JSONObject()
                    val bests = mutableMapOf<GameMode, Int>()
                    val keys = bestObj.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        try {
                            bests[GameMode.valueOf(key)] = bestObj.getInt(key)
                        } catch (_: Exception) { /* unknown mode */ }
                    }
                    DifficultyStats(
                        played = obj.getInt("p"),
                        won = obj.getInt("w"),
                        totalWinTimeSeconds = obj.getLong("t"),
                        totalMistakes = obj.getLong("m"),
                        totalHints = obj.getLong("h"),
                        recentWinTimes = recent,
                        bestTimePerMode = bests
                    )
                } catch (_: Exception) {
                    DifficultyStats()
                }
            }
            // Legacy delimited format: played/won/bestTime/total/mistakes/hints/recent
            val parts = raw.split("/")
            if (parts.size < 6) return DifficultyStats()
            return try {
                val recent = if (parts.size >= 7 && parts[6].isNotEmpty())
                    parts[6].split("|").mapNotNull { it.toIntOrNull() }
                else emptyList()
                val legacyBest = parts[2].toIntOrNull()?.takeIf { it >= 0 }
                DifficultyStats(
                    played = parts[0].toInt(),
                    won = parts[1].toInt(),
                    totalWinTimeSeconds = parts[3].toLong(),
                    totalMistakes = parts[4].toLong(),
                    totalHints = parts[5].toLong(),
                    recentWinTimes = recent,
                    bestTimePerMode = if (legacyBest != null) mapOf(GameMode.CLASSIC to legacyBest) else emptyMap()
                )
            } catch (_: Exception) {
                DifficultyStats()
            }
        }
    }
}

data class FinishOutcome(
    val won: Boolean,
    val timeSeconds: Int,
    val mistakes: Int,
    val hints: Int,
    val newBest: Boolean,
    val previousBest: Int?,
    val gamesWonThisDifficulty: Int
)

object StatsManager {
    private const val PREFS_NAME = "sudoku_prefs"
    private const val KEY_PREFIX = "stats_"

    private val _stats = MutableStateFlow(emptyMap<Difficulty, DifficultyStats>())
    val stats: StateFlow<Map<Difficulty, DifficultyStats>> = _stats.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _stats.value = Difficulty.values().associateWith { d ->
            DifficultyStats.parse(prefs.getString(KEY_PREFIX + d.name, null))
        }
    }

    fun get(difficulty: Difficulty): DifficultyStats =
        _stats.value[difficulty] ?: DifficultyStats()

    fun recordGame(
        context: Context,
        difficulty: Difficulty,
        mode: GameMode,
        won: Boolean,
        timeSeconds: Int,
        mistakes: Int,
        hints: Int
    ): FinishOutcome {
        val current = get(difficulty)
        val previousBestForMode = current.bestTime(mode)
        val newBest = won && (previousBestForMode == null || timeSeconds < previousBestForMode)
        val updatedRecent = if (won) {
            (current.recentWinTimes + timeSeconds).takeLast(DifficultyStats.RECENT_TIMES_CAP)
        } else current.recentWinTimes
        val updatedBests = if (won) {
            val newTime = if (previousBestForMode == null) timeSeconds
                else minOf(previousBestForMode, timeSeconds)
            current.bestTimePerMode + (mode to newTime)
        } else current.bestTimePerMode
        val updated = current.copy(
            played = current.played + 1,
            won = current.won + if (won) 1 else 0,
            totalWinTimeSeconds = current.totalWinTimeSeconds + if (won) timeSeconds.toLong() else 0,
            totalMistakes = current.totalMistakes + mistakes,
            totalHints = current.totalHints + hints,
            recentWinTimes = updatedRecent,
            bestTimePerMode = updatedBests
        )
        _stats.value = _stats.value + (difficulty to updated)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PREFIX + difficulty.name, updated.serialize())
            .apply()
        return FinishOutcome(
            won = won,
            timeSeconds = timeSeconds,
            mistakes = mistakes,
            hints = hints,
            newBest = newBest,
            previousBest = previousBestForMode,
            gamesWonThisDifficulty = updated.won
        )
    }

    fun reset(context: Context) {
        val editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
        Difficulty.values().forEach { d -> editor.remove(KEY_PREFIX + d.name) }
        editor.apply()
        _stats.value = Difficulty.values().associateWith { DifficultyStats() }
    }
}
