package com.ninersudoku.game

enum class GameMode(val displayName: String, val description: String) {
    CLASSIC("Classic", "$MISTAKE_LIMIT mistakes, normal hint count"),
    NO_MISTAKES("Strict", "1 mistake ends the game"),
    COACH("Coach", "No mistake limit, unlimited hints"),
    SPEED("Speed", "Race a count-down clock"),
    KILLER("Killer", "Cages with target sums");
}

/** Initial time budget (in seconds) for Speed mode, per difficulty. Each correct cell adds [SPEED_BONUS_SECONDS]. */
fun Difficulty.speedBudgetSeconds(): Int = when (this) {
    Difficulty.BEGINNER -> 5 * 60
    Difficulty.EASY -> 7 * 60
    Difficulty.MEDIUM -> 10 * 60
    Difficulty.HARD -> 15 * 60
    Difficulty.EXPERT -> 20 * 60
}

const val SPEED_BONUS_SECONDS: Int = 4
