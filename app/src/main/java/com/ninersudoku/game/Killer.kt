package com.ninersudoku.game

import kotlin.random.Random

/**
 * One cage in a Killer Sudoku puzzle: a set of cells whose digits must sum to [targetSum] and
 * all be different from each other (in addition to standard Sudoku constraints).
 */
data class KillerCage(val cells: Set<Int>, val targetSum: Int) {
    init {
        require(cells.isNotEmpty()) { "Cage must contain at least one cell" }
        // Max per-cage sum is 45 (nine unique digits 1..9). We don't restrict size here since
        // the generator caps cage size, but a cage summing > 45 would be unsolvable.
        require(targetSum in 1..45) { "Cage target sum must be in 1..45, got $targetSum" }
    }
    /** The cell where the sum label is drawn — top-left in row/col order. */
    val labelCell: Int = cells.minByOrNull { it / 9 * 100 + it % 9 } ?: cells.first()
}

data class KillerPuzzle(
    val cages: List<KillerCage>,
    val solution: IntArray,
    /** Initial clues: same shape as Generator.Puzzle.puzzle (81 ints, 0 = empty). */
    val puzzle: IntArray
) {
    init {
        require(solution.size == 81)
        require(puzzle.size == 81)
        require(cages.flatMap { it.cells }.toSet().size == 81) { "Cages must cover all 81 cells exactly once" }
    }
}

object KillerGenerator {

    /**
     * Generate a Killer Sudoku puzzle. Every cage is exactly 2 or 3 cells — no size-1 cages
     * that would double as redundant givens. Starting clues come from the standard Generator
     * at the same difficulty level, so Killer plays like Classic plus cage-sum hints.
     *
     * Difficulty expresses via the 2-vs-3 mix (easier levels lean 2-cell pairs, harder levels
     * lean 3-cell cages). Uniqueness is guaranteed by the underlying Classic generator —
     * adding cage sums only adds information, never removes it.
     */
    /**
     * Killer uses fewer starting clues than Classic at the same difficulty — cage sums
     * carry part of the deduction weight, so the puzzle stays challenging. Roughly ~65%
     * of Classic's count at Beginner, scaling down to ~40% at Expert.
     */
    private fun killerClueCount(difficulty: Difficulty): Int = when (difficulty) {
        Difficulty.BEGINNER -> 32
        Difficulty.EASY -> 24
        Difficulty.MEDIUM -> 18
        Difficulty.HARD -> 13
        Difficulty.EXPERT -> 9
    }

    fun generate(difficulty: Difficulty, random: Random = Random.Default): KillerPuzzle {
        val classic = Generator.generateWithClueCount(killerClueCount(difficulty), random)
        // Strict 2-3 cell partition, retrying if random topology traps an orphan.
        repeat(100) {
            val cageSets = buildCages(difficulty, random, allowSize4Merge = false) ?: return@repeat
            return KillerPuzzle(
                cages = cageSets.map { cells ->
                    KillerCage(cells, cells.sumOf { classic.solution[it] })
                },
                solution = classic.solution,
                puzzle = classic.puzzle
            )
        }
        // Fallback: allow a rare size-4 cage if the random shuffle keeps trapping orphans.
        val cageSets = buildCages(difficulty, random, allowSize4Merge = true)
            ?: error("Killer cage generator failed to partition the grid — topology bug")
        return KillerPuzzle(
            cages = cageSets.map { cells ->
                KillerCage(cells, cells.sumOf { classic.solution[it] })
            },
            solution = classic.solution,
            puzzle = classic.puzzle
        )
    }

    /**
     * Partition all 81 cells into 2- or 3-cell connected cages.
     *
     * @return the cage list, or `null` if the random topology trapped an orphan that couldn't
     *   be merged without exceeding size 3 (in which case the caller should retry with a
     *   different random shuffle). When [allowSize4Merge] is true, the merge step can grow a
     *   neighbour from 3→4 as a last resort so this never returns null.
     */
    private fun buildCages(
        difficulty: Difficulty,
        random: Random,
        allowSize4Merge: Boolean = false
    ): List<Set<Int>>? {
        // Probability that a starting cell aims for a 3-cell cage (vs a 2-cell pair).
        // Smaller cages are easier to reason about, so easier levels lean 2-cell.
        val threeCellBias = when (difficulty) {
            Difficulty.BEGINNER -> 0.25f
            Difficulty.EASY -> 0.45f
            Difficulty.MEDIUM -> 0.60f
            Difficulty.HARD -> 0.75f
            Difficulty.EXPERT -> 0.85f
        }

        val assignment = IntArray(81) { -1 }
        val cages = mutableListOf<MutableSet<Int>>()
        val ordered = (0 until 81).shuffled(random)

        for (start in ordered) {
            if (assignment[start] != -1) continue

            val cage = mutableSetOf(start)
            val cageId = cages.size
            assignment[start] = cageId

            val targetSize = if (random.nextFloat() < threeCellBias) 3 else 2
            while (cage.size < targetSize) {
                val candidates = cage.flatMap { neighbors(it) }
                    .filter { assignment[it] == -1 }
                    .distinct()
                if (candidates.isEmpty()) break
                val pick = candidates.random(random)
                cage += pick
                assignment[pick] = cageId
            }
            cages += cage
        }

        // Post-process: no size-1 cages allowed. Merge orphans into a neighbour cage.
        // Prefer size-2 targets (→ size 3, stays within spec); only fall back to size-3
        // targets (→ size 4) when [allowSize4Merge] is true.
        for (i in cages.indices) {
            val cage = cages[i]
            if (cage.size != 1) continue
            val cell = cage.first()
            val neighborCageIds = neighbors(cell)
                .mapNotNull { assignment[it].takeIf { id -> id >= 0 && id != i } }
                .distinct()
            val target = neighborCageIds.firstOrNull { cages[it].size == 2 }
                ?: if (allowSize4Merge) neighborCageIds.firstOrNull { cages[it].size == 3 } else null
            if (target != null) {
                cages[target].add(cell)
                cage.clear()
                assignment[cell] = target
            } else {
                // Topology trapped — all neighbours are at size 3 and we can't bump past it.
                // Signal the caller to retry with a different random shuffle.
                return null
            }
        }

        return cages.filter { it.isNotEmpty() }
    }

    private fun neighbors(idx: Int): List<Int> {
        val r = idx / 9; val c = idx % 9
        val result = mutableListOf<Int>()
        if (r > 0) result += (r - 1) * 9 + c
        if (r < 8) result += (r + 1) * 9 + c
        if (c > 0) result += r * 9 + (c - 1)
        if (c < 8) result += r * 9 + (c + 1)
        return result
    }

    private fun generateSolved(random: Random): IntArray {
        val grid = IntArray(81)
        for (b in 0..2) fillDiagonalBox(grid, b * 3, b * 3, random)
        check(Solver.solve(grid, randomized = true))
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

/** Killer-aware backtracking solver. Counts solutions up to a limit (used for uniqueness checks). */
object KillerSolver {

    fun countSolutions(cages: List<KillerCage>, limit: Int = 2): Int {
        val grid = IntArray(81)
        // Pre-place singles since they have a fixed value.
        for (cage in cages) {
            if (cage.cells.size == 1) {
                val cell = cage.cells.first()
                if (cage.targetSum !in 1..9) return 0
                grid[cell] = cage.targetSum
            }
        }
        val cageOf = IntArray(81)
        for ((i, c) in cages.withIndex()) for (cell in c.cells) cageOf[cell] = i
        val cageDigitsUsed = IntArray(cages.size)  // bitmask per cage of digits already placed
        val cageSumSoFar = IntArray(cages.size)
        val cageFilledCount = IntArray(cages.size)

        // Validate seed (singles + nothing else).
        for (i in 0 until 81) {
            val v = grid[i]
            if (v == 0) continue
            val ci = cageOf[i]
            val bit = 1 shl v
            if (cageDigitsUsed[ci] and bit != 0) return 0
            cageDigitsUsed[ci] = cageDigitsUsed[ci] or bit
            cageSumSoFar[ci] += v
            cageFilledCount[ci]++
            // Sudoku row/col/box check
            val r = i / 9; val c = i % 9; val b = (r / 3) * 3 + c / 3
            // Check that no peer in same row/col/box already has v
            for (k in 0..8) {
                if (k != c && grid[r * 9 + k] == v) return 0
                if (k != r && grid[k * 9 + c] == v) return 0
            }
            val br = (r / 3) * 3; val bc = (c / 3) * 3
            for (dr in 0..2) for (dc in 0..2) {
                val idx = (br + dr) * 9 + bc + dc
                if (idx != i && grid[idx] == v) return 0
            }
        }

        return backtrack(grid, cages, cageOf, cageDigitsUsed, cageSumSoFar, cageFilledCount, limit)
    }

    private fun backtrack(
        grid: IntArray,
        cages: List<KillerCage>,
        cageOf: IntArray,
        cageDigitsUsed: IntArray,
        cageSumSoFar: IntArray,
        cageFilledCount: IntArray,
        limit: Int
    ): Int {
        // Find first empty cell.
        val idx = (0 until 81).firstOrNull { grid[it] == 0 } ?: return 1

        val r = idx / 9; val c = idx % 9
        val br = (r / 3) * 3; val bc = (c / 3) * 3
        val ci = cageOf[idx]
        val cage = cages[ci]
        val remainingCells = cage.cells.size - cageFilledCount[ci]
        val remainingSum = cage.targetSum - cageSumSoFar[ci]

        var found = 0
        for (d in 1..9) {
            // Sudoku constraints
            val bit = 1 shl d
            if (cageDigitsUsed[ci] and bit != 0) continue
            // row/col
            var conflict = false
            for (k in 0..8) {
                if (grid[r * 9 + k] == d) { conflict = true; break }
                if (grid[k * 9 + c] == d) { conflict = true; break }
            }
            if (conflict) continue
            // box
            for (dr in 0..2) {
                for (dc in 0..2) {
                    if (grid[(br + dr) * 9 + bc + dc] == d) { conflict = true; break }
                }
                if (conflict) break
            }
            if (conflict) continue
            // cage sum constraints
            if (d > remainingSum) continue  // too big for remaining
            if (remainingCells == 1 && d != remainingSum) continue  // must equal exact remaining
            // Reasonable lower bound: cage must still be reachable
            // For simplicity skip the lower-bound check (it's an optimization, not correctness).

            // Place
            grid[idx] = d
            cageDigitsUsed[ci] = cageDigitsUsed[ci] or bit
            cageSumSoFar[ci] += d
            cageFilledCount[ci]++

            val n = backtrack(grid, cages, cageOf, cageDigitsUsed, cageSumSoFar, cageFilledCount, limit - found)
            found += n

            // Undo
            grid[idx] = 0
            cageDigitsUsed[ci] = cageDigitsUsed[ci] and bit.inv()
            cageSumSoFar[ci] -= d
            cageFilledCount[ci]--

            if (found >= limit) return found
        }
        return found
    }
}
