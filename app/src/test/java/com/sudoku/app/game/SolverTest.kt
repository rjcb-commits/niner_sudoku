package com.sudoku.app.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class SolverTest {
    @Test
    fun solverFinishesKnownPuzzle() {
        val puzzle = intArrayOf(
            5,3,0, 0,7,0, 0,0,0,
            6,0,0, 1,9,5, 0,0,0,
            0,9,8, 0,0,0, 0,6,0,

            8,0,0, 0,6,0, 0,0,3,
            4,0,0, 8,0,3, 0,0,1,
            7,0,0, 0,2,0, 0,0,6,

            0,6,0, 0,0,0, 2,8,0,
            0,0,0, 4,1,9, 0,0,5,
            0,0,0, 0,8,0, 0,7,9
        )
        val grid = puzzle.copyOf()
        assertTrue(Solver.solve(grid))
        for (i in 0 until 81) assertTrue("cell $i empty", grid[i] in 1..9)
    }

    @Test
    fun generatorProducesUniqueSolvablePuzzle() {
        for (diff in Difficulty.values()) {
            val (puzzle, solution) = Generator.generate(diff, Random(42))
            val clueCount = puzzle.count { it != 0 }
            assertTrue("$diff clue count $clueCount", clueCount in 17..81)
            assertEquals("$diff not unique", 1, Solver.countSolutions(puzzle, limit = 2))
            val solved = puzzle.copyOf().also { Solver.solve(it) }
            for (i in 0 until 81) {
                assertEquals("$diff cell $i mismatch", solution[i], solved[i])
            }
        }
    }
}
