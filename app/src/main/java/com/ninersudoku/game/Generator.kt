package com.ninersudoku.game

import kotlin.random.Random

data class Puzzle(val puzzle: IntArray, val solution: IntArray) {
    init {
        require(puzzle.size == 81 && solution.size == 81)
    }
}

object Generator {

    fun generate(difficulty: Difficulty, random: Random = Random.Default): Puzzle =
        generateWithClueCount(difficulty.clueCount, random)

    /**
     * Generate a puzzle with [targetClues] starting values (the rest are empty).
     * Uniqueness is preserved — cells are removed one at a time and rolled back if the
     * removal would create multiple solutions.
     */
    fun generateWithClueCount(targetClues: Int, random: Random = Random.Default): Puzzle {
        val solution = generateSolved(random)
        val puzzle = solution.copyOf()
        val cellOrder = (0 until 81).shuffled(random).toMutableList()
        var clues = 81

        for (idx in cellOrder) {
            if (clues <= targetClues) break
            val saved = puzzle[idx]
            if (saved == 0) continue
            puzzle[idx] = 0
            val solutions = Solver.countSolutions(puzzle, limit = 2)
            if (solutions != 1) {
                puzzle[idx] = saved
            } else {
                clues--
            }
        }
        return Puzzle(puzzle, solution)
    }

    private fun generateSolved(random: Random): IntArray {
        val grid = IntArray(81)
        // Fill the three independent diagonal 3x3 boxes first — no constraints between them.
        for (b in 0..2) fillDiagonalBox(grid, b * 3, b * 3, random)
        check(Solver.solve(grid, randomized = true, random = random)) { "Failed to fill grid" }
        return grid
    }

    private fun fillDiagonalBox(grid: IntArray, startRow: Int, startCol: Int, random: Random) {
        val digits = (1..9).shuffled(random)
        var k = 0
        for (r in 0..2) for (c in 0..2) {
            grid[(startRow + r) * 9 + (startCol + c)] = digits[k++]
        }
    }
}
