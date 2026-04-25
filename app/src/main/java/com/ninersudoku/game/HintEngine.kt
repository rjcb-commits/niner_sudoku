package com.ninersudoku.game

/** A hint plus a short, human-friendly reason explaining why it's the next move. */
data class HintExplanation(
    val cellIdx: Int,
    val value: Int,
    val technique: String,
    val reason: String
) {
    val row: Int get() = cellIdx / 9
    val col: Int get() = cellIdx % 9
}

object HintEngine {

    /**
     * Find the best hint to give in the current state, prioritizing techniques the player can learn from.
     * Returns `null` only if the board is already complete (nothing to suggest).
     *
     * When [cages] is non-empty (Killer mode), cage-aware techniques are preferred so the
     * explanation reflects the actual constraint that narrows the cell.
     */
    fun nextHint(
        board: Board,
        solution: IntArray,
        cages: List<KillerCage> = emptyList()
    ): HintExplanation? {
        if (cages.isNotEmpty()) {
            // Killer-first: if a cage constraint uniquely determines a cell, explain that.
            cageLastCell(board, cages)?.let { return it }
            cageNakedSingle(board, cages)?.let { return it }
        }
        // Try naked singles first: cells with exactly one possible candidate. Easiest to grasp.
        nakedSingle(board)?.let { return it }
        // Hidden singles in row/col/box: only cell in a unit that can hold a given digit.
        hiddenSingle(board)?.let { return it }
        // Fallback: most-constrained empty cell, plain "Try this cell."
        return fallback(board, solution)
    }

    private fun nakedSingle(board: Board): HintExplanation? {
        for (i in 0 until 81) {
            val cell = board.cells[i]
            if (cell.value != 0) continue
            val candidates = candidatesFor(board, i)
            if (candidates.size == 1) {
                val v = candidates.first()
                return HintExplanation(
                    cellIdx = i,
                    value = v,
                    technique = "Naked single",
                    reason = "Only $v can fit in this cell — every other digit is already in its row, column, or box."
                )
            }
        }
        return null
    }

    /** Killer: a cage with exactly one empty cell — the value is forced by the cage target. */
    private fun cageLastCell(board: Board, cages: List<KillerCage>): HintExplanation? {
        for (cage in cages) {
            if (cage.cells.size < 2) continue
            val empties = cage.cells.filter { board.cells[it].value == 0 }
            if (empties.size != 1) continue
            val placedSum = cage.cells.sumOf { board.cells[it].value }
            val missing = cage.targetSum - placedSum
            val idx = empties.first()
            if (missing !in 1..9) continue
            // Sanity: must not conflict with row/col/box or repeat a cage digit.
            if (missing !in candidatesFor(board, idx)) continue
            val usedInCage = cage.cells.mapNotNull {
                board.cells[it].value.takeIf { v -> v != 0 }
            }.toSet()
            if (missing in usedInCage) continue
            return HintExplanation(
                cellIdx = idx,
                value = missing,
                technique = "Cage sum",
                reason = "This cage needs ${cage.targetSum} total. With $placedSum already in, the last cell must be $missing."
            )
        }
        return null
    }

    /**
     * Killer: naked single computed against *cage-aware* candidates. The cell may have
     * multiple Sudoku candidates but only one that also fits the cage constraints.
     */
    private fun cageNakedSingle(board: Board, cages: List<KillerCage>): HintExplanation? {
        val cageOf = IntArray(81) { -1 }
        for ((ci, cage) in cages.withIndex()) for (cell in cage.cells) cageOf[cell] = ci

        for (i in 0 until 81) {
            val cell = board.cells[i]
            if (cell.value != 0) continue
            val ci = cageOf[i]
            if (ci < 0) continue
            val cage = cages[ci]
            val stdCandidates = candidatesFor(board, i)
            if (stdCandidates.isEmpty()) continue

            val usedInCage = cage.cells.mapNotNull {
                board.cells[it].value.takeIf { v -> v != 0 }
            }.toSet()
            val placedSum = usedInCage.sum()
            val remainingSum = cage.targetSum - placedSum
            val remainingEmpties = cage.cells.count { board.cells[it].value == 0 }
            if (remainingEmpties < 1) continue
            val othersRemaining = remainingEmpties - 1

            val cageCandidates = stdCandidates.filter { d ->
                if (d in usedInCage) return@filter false
                val leftover = remainingSum - d
                if (othersRemaining == 0) leftover == 0
                else leftover in (othersRemaining * 1)..(othersRemaining * 9)
            }

            if (cageCandidates.size == 1) {
                val v = cageCandidates.first()
                return HintExplanation(
                    cellIdx = i,
                    value = v,
                    technique = "Cage constraint",
                    reason = "Only $v works here — the cage needs ${cage.targetSum} and the other digits already placed (or reachable) rule out everything else."
                )
            }
        }
        return null
    }

    private fun hiddenSingle(board: Board): HintExplanation? {
        // For each row, col, box and each digit 1-9, count empty cells where digit is a candidate.
        // If exactly one such cell, that's a hidden single.
        for (digit in 1..9) {
            for (r in 0..8) {
                val cells = (0..8).map { c -> r * 9 + c }
                hiddenIn(board, digit, cells, "row ${r + 1}")?.let { return it }
            }
            for (c in 0..8) {
                val cells = (0..8).map { r -> r * 9 + c }
                hiddenIn(board, digit, cells, "column ${c + 1}")?.let { return it }
            }
            for (b in 0..8) {
                val br = (b / 3) * 3; val bc = (b % 3) * 3
                val cells = (0..2).flatMap { dr -> (0..2).map { dc -> (br + dr) * 9 + bc + dc } }
                hiddenIn(board, digit, cells, "box ${b + 1}")?.let { return it }
            }
        }
        return null
    }

    private fun hiddenIn(
        board: Board,
        digit: Int,
        cells: List<Int>,
        unitName: String
    ): HintExplanation? {
        var foundIdx = -1
        for (i in cells) {
            val c = board.cells[i]
            if (c.value == digit) return null  // already placed
            if (c.value == 0 && digit in candidatesFor(board, i)) {
                if (foundIdx >= 0) return null
                foundIdx = i
            }
        }
        if (foundIdx == -1) return null
        return HintExplanation(
            cellIdx = foundIdx,
            value = digit,
            technique = "Hidden single",
            reason = "$digit can only go in this cell within $unitName — all other empty cells in $unitName are blocked."
        )
    }

    private fun fallback(board: Board, solution: IntArray): HintExplanation? {
        var bestIdx = -1
        var bestCount = 10
        for (i in 0 until 81) {
            if (board.cells[i].value != 0) continue
            val cnt = candidatesFor(board, i).size
            if (cnt in 1 until bestCount) {
                bestCount = cnt
                bestIdx = i
            }
        }
        if (bestIdx < 0) return null
        return HintExplanation(
            cellIdx = bestIdx,
            value = solution[bestIdx],
            technique = "Most constrained",
            reason = "This cell has only $bestCount candidates left — try working it out from the row, column, and box."
        )
    }

    /** Computes the set of digits 1..9 that don't already appear in the row/col/box of [idx]. */
    private fun candidatesFor(board: Board, idx: Int): Set<Int> {
        if (board.cells[idx].value != 0) return emptySet()
        val r = idx / 9; val c = idx % 9
        val br = (r / 3) * 3; val bc = (c / 3) * 3
        val used = BooleanArray(10)
        for (k in 0..8) {
            used[board.cells[r * 9 + k].value] = true
            used[board.cells[k * 9 + c].value] = true
        }
        for (dr in 0..2) for (dc in 0..2) used[board.cells[(br + dr) * 9 + bc + dc].value] = true
        return (1..9).filter { !used[it] }.toSet()
    }
}
