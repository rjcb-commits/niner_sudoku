package com.ninersudoku.achievements

import android.content.Context
import com.ninersudoku.game.Difficulty
import com.ninersudoku.game.GameMode
import com.ninersudoku.stats.DifficultyStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.time.LocalDate

object AchievementManager {
    private const val PREFS_NAME = "sudoku_prefs"
    private const val KEY_UNLOCKED = "achievements_unlocked"
    private const val KEY_UNLOCK_DATES = "achievements_unlock_dates"

    private val _unlocked = MutableStateFlow<Set<String>>(emptySet())
    val unlocked: StateFlow<Set<String>> = _unlocked.asStateFlow()

    /** Maps achievement id → epoch day when it was unlocked. May be missing for legacy data. */
    private val _unlockDates = MutableStateFlow<Map<String, Long>>(emptyMap())
    val unlockDates: StateFlow<Map<String, Long>> = _unlockDates.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_UNLOCKED, null) ?: ""
        _unlocked.value = if (raw.isEmpty()) emptySet()
            else raw.split(",").filter { it.isNotBlank() }.toSet()
        val datesRaw = prefs.getString(KEY_UNLOCK_DATES, null)
        _unlockDates.value = parseDates(datesRaw)
    }

    private fun parseDates(raw: String?): Map<String, Long> {
        if (raw.isNullOrEmpty()) return emptyMap()
        return try {
            val obj = JSONObject(raw)
            val out = mutableMapOf<String, Long>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                out[k] = obj.getLong(k)
            }
            out
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun serializeDates(dates: Map<String, Long>): String {
        val obj = JSONObject()
        dates.forEach { (id, day) -> obj.put(id, day) }
        return obj.toString()
    }

    fun isUnlocked(a: Achievement): Boolean = a.id in _unlocked.value

    /**
     * Run after the latest game has been recorded in StatsManager. Returns the achievements
     * that became unlocked as a result of this game (so the UI can display a toast).
     */
    fun checkAndUnlock(
        context: Context,
        difficulty: Difficulty,
        mode: GameMode,
        won: Boolean,
        timeSeconds: Int,
        mistakes: Int,
        hints: Int,
        statsAfter: Map<Difficulty, DifficultyStats>
    ): List<Achievement> {
        val current = _unlocked.value
        val totalWon = statsAfter.values.sumOf { it.won }
        // Approximate Killer wins: count of difficulties that have a Killer best time.
        // (We don't track per-mode win count separately yet — that's a future enhancement.)
        val killerDifficultiesWon = statsAfter.values.count { it.bestTimePerMode.containsKey(GameMode.KILLER) }

        val justUnlocked = mutableListOf<Achievement>()
        for (a in Achievement.values()) {
            if (a.id in current) continue
            val met = when (a) {
                Achievement.FIRST_WIN -> won
                Achievement.BEGINNER_MASTER -> (statsAfter[Difficulty.BEGINNER]?.won ?: 0) >= 10
                Achievement.EASY_MASTER -> (statsAfter[Difficulty.EASY]?.won ?: 0) >= 10
                Achievement.MEDIUM_MASTER -> (statsAfter[Difficulty.MEDIUM]?.won ?: 0) >= 10
                Achievement.HARD_MASTER -> (statsAfter[Difficulty.HARD]?.won ?: 0) >= 5
                Achievement.EXPERT_MASTER -> (statsAfter[Difficulty.EXPERT]?.won ?: 0) >= 3
                Achievement.SPEEDRUN_EASY -> won && difficulty == Difficulty.EASY && timeSeconds in 1..239
                Achievement.SPEEDRUN_MEDIUM -> won && difficulty == Difficulty.MEDIUM && timeSeconds in 1..359
                Achievement.HINT_FREE -> won && hints == 0
                Achievement.PERFECTIONIST -> won && mistakes == 0
                Achievement.FLAWLESS -> won && mistakes == 0 && hints == 0
                Achievement.PERSISTENT -> totalWon >= 50
                Achievement.SPEED_DEMON -> won && mode == GameMode.SPEED
                Achievement.DEMOLISHER -> won && mode == GameMode.SPEED && timeSeconds in 1..119
                Achievement.CAGEY -> won && mode == GameMode.KILLER
                // Cage Master: needs 10 Killer wins. Without per-mode-per-difficulty win counts,
                // gate on "won at all 5 difficulties in Killer" as a proxy for now.
                Achievement.CAGE_MASTER -> killerDifficultiesWon >= 5
            }
            if (met) justUnlocked += a
        }

        if (justUnlocked.isNotEmpty()) {
            val updated = current + justUnlocked.map { it.id }
            _unlocked.value = updated
            val today = LocalDate.now().toEpochDay()
            val updatedDates = _unlockDates.value + justUnlocked.associate { it.id to today }
            _unlockDates.value = updatedDates
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_UNLOCKED, updated.joinToString(","))
                .putString(KEY_UNLOCK_DATES, serializeDates(updatedDates))
                .apply()
        }
        return justUnlocked
    }

    fun reset(context: Context) {
        _unlocked.value = emptySet()
        _unlockDates.value = emptyMap()
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_UNLOCKED)
            .remove(KEY_UNLOCK_DATES)
            .apply()
    }
}
