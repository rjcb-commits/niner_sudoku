package com.ninersudoku.daily

import android.content.Context
import com.ninersudoku.game.Difficulty
import com.ninersudoku.game.Generator
import com.ninersudoku.game.Puzzle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import kotlin.random.Random

/**
 * Daily Sudoku — same puzzle for every user on a given day, seeded by the calendar date.
 * Tracks consecutive-day streak.
 */
object DailyManager {
    private const val PREFS_NAME = "sudoku_prefs"
    private const val KEY_LAST_SOLVED_EPOCH_DAY = "daily_last_solved"
    private const val KEY_CURRENT_STREAK = "daily_current_streak"
    private const val KEY_BEST_STREAK = "daily_best_streak"
    private const val KEY_SOLVED_DATES = "daily_solved_dates"
    /** Seed offset so the daily seed is harder to reverse-engineer than just "epoch day". */
    private const val SEED_OFFSET = 0x53D04A17L

    /** Difficulty for the daily puzzle. Kept fixed so all players get the same puzzle. */
    val difficulty: Difficulty = Difficulty.MEDIUM

    data class State(
        val lastSolvedEpochDay: Long? = null,
        val currentStreak: Int = 0,
        val bestStreak: Int = 0,
        val solvedDates: Set<Long> = emptySet()
    ) {
        fun solvedToday(today: Long): Boolean = lastSolvedEpochDay == today

        /** The streak number we should show in the UI: 0 if the streak has been broken. */
        fun displayStreak(today: Long): Int {
            val last = lastSolvedEpochDay ?: return 0
            val gap = today - last
            return if (gap <= 1) currentStreak else 0
        }
    }

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val last = prefs.getLong(KEY_LAST_SOLVED_EPOCH_DAY, -1L).takeIf { it >= 0 }
        val datesRaw = prefs.getString(KEY_SOLVED_DATES, "") ?: ""
        val solvedDates = if (datesRaw.isEmpty()) emptySet()
            else datesRaw.split(",").mapNotNull { it.trim().toLongOrNull() }.toSet()
        _state.value = State(
            lastSolvedEpochDay = last,
            currentStreak = prefs.getInt(KEY_CURRENT_STREAK, 0),
            bestStreak = prefs.getInt(KEY_BEST_STREAK, 0),
            solvedDates = solvedDates
        )
    }

    fun todayEpochDay(): Long = LocalDate.now().toEpochDay()

    /** Generates today's daily puzzle (deterministic by date). */
    fun generateToday(): Puzzle {
        val seed = todayEpochDay() xor SEED_OFFSET
        return Generator.generate(difficulty, Random(seed))
    }

    /**
     * Record that the user solved today's daily. Updates streaks. Idempotent within the same day.
     */
    fun recordDailyWin(context: Context): State {
        val today = todayEpochDay()
        val current = _state.value
        if (current.lastSolvedEpochDay == today) return current  // already counted

        val gap = current.lastSolvedEpochDay?.let { today - it }
        val newStreak = if (gap == 1L) current.currentStreak + 1 else 1
        val newDates = current.solvedDates + today
        val updated = current.copy(
            lastSolvedEpochDay = today,
            currentStreak = newStreak,
            bestStreak = maxOf(current.bestStreak, newStreak),
            solvedDates = newDates
        )
        _state.value = updated
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_LAST_SOLVED_EPOCH_DAY, today)
            .putInt(KEY_CURRENT_STREAK, newStreak)
            .putInt(KEY_BEST_STREAK, updated.bestStreak)
            .putString(KEY_SOLVED_DATES, newDates.joinToString(","))
            .apply()
        return updated
    }

    fun reset(context: Context) {
        _state.value = State()
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_LAST_SOLVED_EPOCH_DAY)
            .remove(KEY_CURRENT_STREAK)
            .remove(KEY_BEST_STREAK)
            .remove(KEY_SOLVED_DATES)
            .apply()
    }
}
