package com.ninersudoku.game

import kotlin.random.Random

/**
 * Bitmask-based Sudoku solver. Operates on an IntArray of size 81 (row-major),
 * with 0 representing empty cells and 1..9 the digits.
 */
object Solver {

    private const val ALL = 0x3FE // bits 1..9 set

    /**
     * Solve in place. Returns true if a solution was found.
     *
     * The optional [random] is used to deterministically randomize digit
     * order during the backtrack — critical for the daily puzzle to be
     * identical across devices given the same seed. Without it, the solver
     * falls back to `Random.Default` and any other code in the process that
     * has used `Random.Default.nextInt` poisons the daily's determinism.
     */
    fun solve(grid: IntArray, randomized: Boolean = false, random: Random = Random.Default): Boolean {
        val rows = IntArray(9)
        val cols = IntArray(9)
        val boxes = IntArray(9)
        if (!seedMasks(grid, rows, cols, boxes)) return false
        return backtrack(grid, rows, cols, boxes, randomized, countOnly = false, limit = 1, random = random) == 1
    }

    /** Count solutions up to [limit]. Useful for uniqueness checks. */
    fun countSolutions(grid: IntArray, limit: Int = 2): Int {
        val copy = grid.copyOf()
        val rows = IntArray(9)
        val cols = IntArray(9)
        val boxes = IntArray(9)
        if (!seedMasks(copy, rows, cols, boxes)) return 0
        return backtrack(copy, rows, cols, boxes, randomized = false, countOnly = true, limit = limit, random = Random.Default)
    }

    private fun seedMasks(grid: IntArray, rows: IntArray, cols: IntArray, boxes: IntArray): Boolean {
        for (i in 0 until 81) {
            val v = grid[i]
            if (v == 0) continue
            val bit = 1 shl v
            val r = i / 9; val c = i % 9; val b = (r / 3) * 3 + c / 3
            if ((rows[r] or cols[c] or boxes[b]) and bit != 0) return false
            rows[r] = rows[r] or bit
            cols[c] = cols[c] or bit
            boxes[b] = boxes[b] or bit
        }
        return true
    }

    private fun backtrack(
        grid: IntArray,
        rows: IntArray,
        cols: IntArray,
        boxes: IntArray,
        randomized: Boolean,
        countOnly: Boolean,
        limit: Int,
        random: Random
    ): Int {
        // MRV: pick the empty cell with the fewest candidates.
        var bestIdx = -1
        var bestMask = 0
        var bestCount = 10
        for (i in 0 until 81) {
            if (grid[i] != 0) continue
            val r = i / 9; val c = i % 9; val b = (r / 3) * 3 + c / 3
            val used = rows[r] or cols[c] or boxes[b]
            val avail = ALL and used.inv()
            val cnt = Integer.bitCount(avail)
            if (cnt == 0) return 0
            if (cnt < bestCount) {
                bestCount = cnt
                bestIdx = i
                bestMask = avail
                if (cnt == 1) break
            }
        }
        if (bestIdx == -1) return 1 // solved

        val r = bestIdx / 9; val c = bestIdx % 9; val b = (r / 3) * 3 + c / 3
        val digits = digitsOf(bestMask, randomized, random)
        var found = 0
        for (d in digits) {
            val bit = 1 shl d
            grid[bestIdx] = d
            rows[r] = rows[r] or bit
            cols[c] = cols[c] or bit
            boxes[b] = boxes[b] or bit

            val n = backtrack(grid, rows, cols, boxes, randomized, countOnly, limit - found, random)
            found += n

            if (!countOnly && found >= 1) return 1 // leave grid in solved state

            grid[bestIdx] = 0
            rows[r] = rows[r] and bit.inv()
            cols[c] = cols[c] and bit.inv()
            boxes[b] = boxes[b] and bit.inv()

            if (countOnly && found >= limit) return found
        }
        return found
    }

    private fun digitsOf(mask: Int, randomized: Boolean, random: Random): IntArray {
        val out = IntArray(Integer.bitCount(mask))
        var idx = 0
        for (d in 1..9) if ((mask shr d) and 1 == 1) out[idx++] = d
        if (randomized) {
            // Use the passed-in seeded Random (was Random.Default before),
            // so daily-puzzle determinism actually holds across devices.
            for (i in out.size - 1 downTo 1) {
                val j = random.nextInt(i + 1)
                val t = out[i]; out[i] = out[j]; out[j] = t
            }
        }
        return out
    }
}
