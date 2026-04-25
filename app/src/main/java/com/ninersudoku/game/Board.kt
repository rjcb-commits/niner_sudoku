package com.ninersudoku.game

data class Cell(
    val value: Int = 0,
    val isGiven: Boolean = false,
    val isHint: Boolean = false,
    val notes: Set<Int> = emptySet()
)

data class Board(val cells: List<Cell>) {
    init { require(cells.size == 81) }

    fun cell(row: Int, col: Int): Cell = cells[row * 9 + col]

    fun isFilled(): Boolean = cells.all { it.value != 0 }

    /** Returns the set of (row, col) coordinates that violate Sudoku rules in the current state. */
    fun conflicts(): Set<Int> {
        val bad = HashSet<Int>()
        // rows
        for (r in 0..8) {
            val seen = HashMap<Int, Int>() // value -> first index
            for (c in 0..8) {
                val v = cell(r, c).value
                if (v == 0) continue
                val idx = r * 9 + c
                val prev = seen.put(v, idx)
                if (prev != null) { bad += idx; bad += prev }
            }
        }
        // cols
        for (c in 0..8) {
            val seen = HashMap<Int, Int>()
            for (r in 0..8) {
                val v = cell(r, c).value
                if (v == 0) continue
                val idx = r * 9 + c
                val prev = seen.put(v, idx)
                if (prev != null) { bad += idx; bad += prev }
            }
        }
        // boxes
        for (b in 0..8) {
            val seen = HashMap<Int, Int>()
            val br = (b / 3) * 3; val bc = (b % 3) * 3
            for (dr in 0..2) for (dc in 0..2) {
                val r = br + dr; val c = bc + dc
                val v = cell(r, c).value
                if (v == 0) continue
                val idx = r * 9 + c
                val prev = seen.put(v, idx)
                if (prev != null) { bad += idx; bad += prev }
            }
        }
        return bad
    }

    companion object {
        fun fromPuzzle(puzzle: IntArray): Board {
            require(puzzle.size == 81)
            return Board(puzzle.map { v -> Cell(value = v, isGiven = v != 0) })
        }
    }
}
