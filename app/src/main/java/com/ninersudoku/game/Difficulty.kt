package com.ninersudoku.game

enum class Difficulty(
    val displayName: String,
    val clueCount: Int,
    val maxHints: Int
) {
    BEGINNER("Beginner", 50, 5),
    EASY("Easy", 40, 3),
    MEDIUM("Medium", 32, 0),
    HARD("Hard", 28, 0),
    EXPERT("Expert", 24, 0)
}

const val MISTAKE_LIMIT = 3
