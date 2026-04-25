package com.ninersudoku.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ninersudoku.achievements.Achievement
import com.ninersudoku.achievements.AchievementManager
import com.ninersudoku.daily.DailyManager
import com.ninersudoku.game.Board
import com.ninersudoku.game.Cell
import com.ninersudoku.game.Difficulty
import com.ninersudoku.game.GameMode
import com.ninersudoku.game.Generator
import com.ninersudoku.game.HintEngine
import com.ninersudoku.game.HintExplanation
import com.ninersudoku.game.KillerCage
import com.ninersudoku.game.KillerGenerator
import com.ninersudoku.game.MISTAKE_LIMIT
import com.ninersudoku.game.SPEED_BONUS_SECONDS
import com.ninersudoku.game.speedBudgetSeconds
import com.ninersudoku.persistence.SaveManager
import com.ninersudoku.persistence.SavedGame
import com.ninersudoku.sound.SoundManager
import com.ninersudoku.stats.FinishOutcome
import com.ninersudoku.stats.StatsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class Screen { DIFFICULTY, GAME, STATS, ABOUT }

enum class LossReason { MISTAKE_LIMIT, GAVE_UP, SPEED_TIMEOUT }

private data class UndoEntry(
    val board: Board,
    val mistakes: Int,
    val hintsUsed: Int
)

data class CompletedFlash(
    val rows: Set<Int> = emptySet(),
    val cols: Set<Int> = emptySet(),
    val boxes: Set<Int> = emptySet(),
    val tick: Long = 0L
)

data class GameState(
    val screen: Screen = Screen.DIFFICULTY,
    val difficulty: Difficulty? = null,
    val board: Board? = null,
    val solution: IntArray? = null,
    val selected: Int? = null,
    val highlightDigit: Int? = null,
    val notesMode: Boolean = false,
    val mistakes: Int = 0,
    val hintsUsed: Int = 0,
    val elapsedSeconds: Int = 0,
    val isComplete: Boolean = false,
    val isGameOver: Boolean = false,
    val celebrationDismissed: Boolean = false,
    val isLoading: Boolean = false,
    val canUndo: Boolean = false,
    val rejectionTick: Int = 0,
    val mistakeTick: Int = 0,
    val completedFlash: CompletedFlash? = null,
    val finishOutcome: FinishOutcome? = null,
    val pendingAchievements: List<Achievement> = emptyList(),
    val isDaily: Boolean = false,
    val mode: GameMode = GameMode.CLASSIC,
    val isPaused: Boolean = false,
    val pendingHint: HintExplanation? = null,
    val cages: List<KillerCage> = emptyList(),
    val timeBudgetSeconds: Int = 0,
    val lossReason: LossReason? = null
) {
    val mistakeLimit: Int get() = when (mode) {
        GameMode.NO_MISTAKES -> 1
        GameMode.COACH -> Int.MAX_VALUE
        else -> MISTAKE_LIMIT
    }
    val hintLimit: Int get() = when (mode) {
        GameMode.COACH -> Int.MAX_VALUE
        // Killer mode is brutal without clues — give beginners a small starter allowance.
        GameMode.KILLER -> when (difficulty) {
            Difficulty.BEGINNER -> 5
            Difficulty.EASY -> 3
            else -> 1
        }
        else -> difficulty?.maxHints ?: 0
    }
    val hintsRemaining: Int get() = (hintLimit - hintsUsed).coerceAtLeast(0)
    val isFinished: Boolean get() = isComplete || isGameOver
    val isKiller: Boolean get() = mode == GameMode.KILLER
    val isSpeed: Boolean get() = mode == GameMode.SPEED
    /** For Speed mode: seconds remaining on the count-down clock. */
    val secondsRemaining: Int get() = (timeBudgetSeconds - elapsedSeconds).coerceAtLeast(0)
    val canErase: Boolean get() {
        val b = board ?: return false
        val idx = selected ?: return false
        if (isFinished) return false
        val cell = b.cells[idx]
        return !cell.isGiven && (cell.value != 0 || cell.notes.isNotEmpty())
    }

    val activeDigit: Int? get() {
        val b = board ?: return null
        if (highlightDigit != null) return highlightDigit
        val idx = selected ?: return null
        val v = b.cells[idx].value
        return if (v in 1..9) v else null
    }

    /**
     * The digits that don't conflict with the selected empty cell's row/col/box.
     * `null` when nothing is selected or the selection is a given/filled cell.
     */
    val legalDigitsForSelection: Set<Int>? get() {
        val b = board ?: return null
        val idx = selected ?: return null
        if (b.cells[idx].value != 0 && b.cells[idx].isGiven) return null
        if (b.cells[idx].isGiven) return null
        val r = idx / 9; val c = idx % 9
        val br = (r / 3) * 3; val bc = (c / 3) * 3
        val used = BooleanArray(10)
        for (k in 0..8) {
            used[b.cells[r * 9 + k].value] = true
            used[b.cells[k * 9 + c].value] = true
        }
        for (dr in 0..2) for (dc in 0..2) used[b.cells[(br + dr) * 9 + bc + dc].value] = true
        return (1..9).filter { !used[it] }.toSet()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GameState) return false
        return screen == other.screen &&
            difficulty == other.difficulty &&
            board == other.board &&
            selected == other.selected &&
            highlightDigit == other.highlightDigit &&
            notesMode == other.notesMode &&
            mistakes == other.mistakes &&
            hintsUsed == other.hintsUsed &&
            elapsedSeconds == other.elapsedSeconds &&
            isComplete == other.isComplete &&
            isGameOver == other.isGameOver &&
            celebrationDismissed == other.celebrationDismissed &&
            isLoading == other.isLoading &&
            canUndo == other.canUndo &&
            rejectionTick == other.rejectionTick &&
            mistakeTick == other.mistakeTick &&
            completedFlash == other.completedFlash &&
            finishOutcome == other.finishOutcome &&
            pendingAchievements == other.pendingAchievements &&
            isDaily == other.isDaily &&
            mode == other.mode &&
            isPaused == other.isPaused &&
            pendingHint == other.pendingHint &&
            cages == other.cages &&
            timeBudgetSeconds == other.timeBudgetSeconds &&
            lossReason == other.lossReason
    }

    override fun hashCode(): Int {
        var result = screen.hashCode()
        result = 31 * result + (difficulty?.hashCode() ?: 0)
        result = 31 * result + (board?.hashCode() ?: 0)
        result = 31 * result + (selected ?: 0)
        result = 31 * result + (highlightDigit ?: 0)
        result = 31 * result + notesMode.hashCode()
        result = 31 * result + mistakes
        result = 31 * result + hintsUsed
        result = 31 * result + elapsedSeconds
        result = 31 * result + isComplete.hashCode()
        result = 31 * result + isGameOver.hashCode()
        result = 31 * result + celebrationDismissed.hashCode()
        result = 31 * result + isLoading.hashCode()
        result = 31 * result + canUndo.hashCode()
        result = 31 * result + rejectionTick
        result = 31 * result + mistakeTick
        result = 31 * result + (completedFlash?.hashCode() ?: 0)
        result = 31 * result + (finishOutcome?.hashCode() ?: 0)
        result = 31 * result + pendingAchievements.hashCode()
        result = 31 * result + isDaily.hashCode()
        result = 31 * result + mode.hashCode()
        result = 31 * result + isPaused.hashCode()
        result = 31 * result + (pendingHint?.hashCode() ?: 0)
        result = 31 * result + cages.hashCode()
        result = 31 * result + timeBudgetSeconds
        result = 31 * result + (lossReason?.hashCode() ?: 0)
        return result
    }
}

class GameViewModel(application: Application) : AndroidViewModel(application) {

    var state by mutableStateOf(GameState())
        private set

    private val undoStack = ArrayDeque<UndoEntry>()
    private var timerJob: kotlinx.coroutines.Job? = null

    fun newGame(difficulty: Difficulty, mode: GameMode = GameMode.CLASSIC) {
        SaveManager.clear(getApplication())
        state = state.copy(isLoading = true, screen = Screen.GAME, difficulty = difficulty, isDaily = false, mode = mode)
        viewModelScope.launch {
            if (mode == GameMode.KILLER) {
                val killer = withContext(Dispatchers.Default) { KillerGenerator.generate(difficulty) }
                // Killer uses the same starting clues as Classic at this difficulty — every
                // cage is 2-3 cells (never size-1) so cage sums and givens are independent info.
                val board = Board(List(81) { idx ->
                    val v = killer.puzzle[idx]
                    if (v != 0) Cell(value = v, isGiven = true) else Cell()
                })
                undoStack.clear()
                state = GameState(
                    screen = Screen.GAME,
                    difficulty = difficulty,
                    board = board,
                    solution = killer.solution,
                    isLoading = false,
                    mode = mode,
                    cages = killer.cages
                )
            } else {
                val puzzle = withContext(Dispatchers.Default) { Generator.generate(difficulty) }
                val budget = if (mode == GameMode.SPEED) difficulty.speedBudgetSeconds() else 0
                undoStack.clear()
                state = GameState(
                    screen = Screen.GAME,
                    difficulty = difficulty,
                    board = Board.fromPuzzle(puzzle.puzzle),
                    solution = puzzle.solution,
                    isLoading = false,
                    mode = mode,
                    timeBudgetSeconds = budget
                )
            }
            startTimer()
            saveIfActive()
        }
    }

    fun startDailyGame() {
        SaveManager.clear(getApplication())
        state = state.copy(
            isLoading = true, screen = Screen.GAME,
            difficulty = DailyManager.difficulty, isDaily = true,
            mode = GameMode.CLASSIC
        )
        viewModelScope.launch {
            val puzzle = withContext(Dispatchers.Default) { DailyManager.generateToday() }
            undoStack.clear()
            state = GameState(
                screen = Screen.GAME,
                difficulty = DailyManager.difficulty,
                board = Board.fromPuzzle(puzzle.puzzle),
                solution = puzzle.solution,
                isLoading = false,
                isDaily = true,
                mode = GameMode.CLASSIC
            )
            startTimer()
            saveIfActive()
        }
    }

    /** Used by the "New game" button on the celebration overlay — keeps the same difficulty + mode. */
    fun restartCurrentDifficulty() {
        val difficulty = state.difficulty ?: return
        // Daily can only be played once per day; restart should never be called on daily.
        if (state.isDaily) {
            backToDifficulty()
            return
        }
        newGame(difficulty, state.mode)
    }


    /** Player gives up — reveal the solution and end the game as a loss. */
    fun giveUp() {
        val solution = state.solution ?: return
        val board = state.board ?: return
        if (state.isFinished) return
        // Fill in every remaining cell with the solution but mark them as hints (not user entries)
        // so the user can review.
        val newCells = board.cells.toMutableList()
        for (i in 0 until 81) {
            val c = newCells[i]
            if (c.value == 0 || c.value != solution[i]) {
                newCells[i] = c.copy(value = solution[i], isHint = true, notes = emptySet())
            }
        }
        val newBoard = board.copy(cells = newCells)
        var next = state.copy(
            board = newBoard,
            isComplete = false,
            isGameOver = true,
            celebrationDismissed = false,
            lossReason = LossReason.GAVE_UP
        )
        next = finalize(next)
        state = next
    }

    fun togglePause() {
        if (state.isFinished || state.isLoading) return
        if (state.isPaused) {
            state = state.copy(isPaused = false)
            startTimer()
        } else {
            timerJob?.cancel()
            state = state.copy(isPaused = true)
            saveIfActive()
        }
    }

    fun dismissPendingHint(applyValue: Boolean) {
        val hint = state.pendingHint ?: return
        if (applyValue) {
            // Apply the hint as a real hint (counts toward hintsUsed, marked isHint).
            val board = state.board ?: return
            val solution = state.solution ?: return
            val cell = board.cells[hint.cellIdx]
            pushUndo()
            var newBoard = board.replace(
                hint.cellIdx,
                cell.copy(value = hint.value, isHint = true, notes = emptySet())
            )
            newBoard = autoCleanPeerNotes(newBoard, hint.cellIdx, hint.value)
            val complete = newBoard.isFilled() && (0 until 81).all { newBoard.cells[it].value == solution[it] }
            val flash = detectCompletions(newBoard, hint.cellIdx)
            var next = state.copy(
                board = newBoard,
                hintsUsed = state.hintsUsed + 1,
                selected = hint.cellIdx,
                isComplete = complete,
                completedFlash = flash,
                pendingHint = null
            )
            if (complete) next = finalize(next)
            state = next
            if (!state.isFinished) saveIfActive()
        } else {
            // "Try yourself" — just select the cell so the user knows where to look. No hint consumed.
            state = state.copy(selected = hint.cellIdx, pendingHint = null)
        }
    }

    fun resumeSavedGame() {
        val saved = SaveManager.load(getApplication()) ?: return
        undoStack.clear()
        state = GameState(
            screen = Screen.GAME,
            difficulty = saved.difficulty,
            board = Board(saved.cells),
            solution = saved.solution,
            selected = saved.selected,
            notesMode = saved.notesMode,
            mistakes = saved.mistakes,
            hintsUsed = saved.hintsUsed,
            elapsedSeconds = saved.elapsedSeconds,
            isLoading = false,
            isDaily = saved.isDaily,
            mode = saved.mode,
            cages = saved.cages,
            timeBudgetSeconds = saved.timeBudgetSeconds
        )
        startTimer()
    }

    fun goToStats() {
        state = state.copy(screen = Screen.STATS)
    }

    fun goToAbout() {
        state = state.copy(screen = Screen.ABOUT)
    }

    fun backToDifficulty() {
        timerJob?.cancel()
        if (state.isFinished) SaveManager.clear(getApplication())
        else saveIfActive()
        state = GameState()
        undoStack.clear()
    }

    fun discardSavedGame() {
        SaveManager.clear(getApplication())
    }

    fun dismissCelebration() {
        state = state.copy(celebrationDismissed = true)
    }

    fun acknowledgeAchievements() {
        state = state.copy(pendingAchievements = emptyList())
    }

    fun selectCell(row: Int, col: Int) {
        if (state.isFinished) return
        val idx = row * 9 + col
        state = state.copy(
            selected = if (state.selected == idx) null else idx,
            highlightDigit = null  // selecting a cell exits filter mode
        )
    }

    /** When no cell is selected, tapping a number on the pad highlights every cell of that digit. */
    fun toggleDigitHighlight(digit: Int) {
        if (state.isFinished) return
        val current = state.highlightDigit
        state = state.copy(highlightDigit = if (current == digit) null else digit)
    }

    fun toggleNotesMode() {
        if (state.isFinished) return
        state = state.copy(notesMode = !state.notesMode)
        saveIfActive()
    }

    fun erase() {
        val board = state.board ?: return
        val idx = state.selected ?: return
        val cell = board.cells[idx]
        if (cell.isGiven || (cell.value == 0 && cell.notes.isEmpty())) return
        if (state.isFinished) return
        pushUndo()
        val updated = cell.copy(value = 0, isHint = false, notes = emptySet())
        state = state.copy(board = board.replace(idx, updated))
        saveIfActive()
    }

    fun enterValue(value: Int, asNote: Boolean = state.notesMode) {
        val board = state.board ?: return
        val solution = state.solution ?: return
        val idx = state.selected ?: return
        val cell = board.cells[idx]
        if (cell.isGiven || state.isFinished) return

        if (asNote && value != 0) {
            pushUndo()
            val newNotes = cell.notes.toMutableSet().apply {
                if (!add(value)) remove(value)
            }
            state = state.copy(board = board.replace(idx, cell.copy(value = 0, notes = newNotes)))
            saveIfActive()
            return
        }

        if (cell.value == value) return
        val isMistake = value != 0 && solution[idx] != value

        // Mistake path: don't write the wrong digit to the board. Just count it,
        // shake the board, pulse the counter, and check for game-over.
        if (isMistake) {
            pushUndo()  // so the user can undo the mistake counter bump
            val newMistakes = state.mistakes + 1
            val gameOver = newMistakes >= state.mistakeLimit
            var next = state.copy(
                mistakes = newMistakes,
                isGameOver = gameOver,
                mistakeTick = state.mistakeTick + 1,
                rejectionTick = state.rejectionTick + 1,
                lossReason = if (gameOver) LossReason.MISTAKE_LIMIT else state.lossReason
            )
            if (gameOver) next = finalize(next)
            state = next
            SoundManager.playMistake()
            if (gameOver) SoundManager.playLoss()
            if (!state.isFinished) saveIfActive()
            return
        }

        // Correct path: write the value, auto-clean peer notes, check completion.
        pushUndo()
        var newBoard = board.replace(
            idx,
            cell.copy(value = value, isHint = false, notes = emptySet())
        )
        if (value != 0) newBoard = autoCleanPeerNotes(newBoard, idx, value)

        val complete = newBoard.isFilled() && (0 until 81).all { newBoard.cells[it].value == solution[it] }
        val flash = if (value != 0) detectCompletions(newBoard, idx) else null

        // Speed bonus: every CORRECT entry extends the budget.
        val newBudget = if (state.isSpeed && value != 0) {
            state.timeBudgetSeconds + SPEED_BONUS_SECONDS
        } else state.timeBudgetSeconds

        var next = state.copy(
            board = newBoard,
            isComplete = complete,
            completedFlash = flash ?: state.completedFlash,
            timeBudgetSeconds = newBudget
        )
        if (complete) next = finalize(next)
        state = next
        when {
            complete -> SoundManager.playWin()
            flash != null -> SoundManager.playComplete()
            else -> SoundManager.playEnter()
        }
        if (!state.isFinished) saveIfActive()
    }

    fun useHint() {
        val board = state.board ?: return
        val solution = state.solution ?: return
        if (state.isFinished) return
        if (state.hintsRemaining <= 0) return
        if (state.pendingHint != null) return

        val explanation = HintEngine.nextHint(board, solution, state.cages) ?: return
        // Surface the explanation; ViewModel waits for dismissPendingHint(applyValue) to commit.
        state = state.copy(pendingHint = explanation, selected = explanation.cellIdx)
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        if (state.isFinished) return
        val entry = undoStack.removeLast()
        state = state.copy(
            board = entry.board,
            mistakes = entry.mistakes,
            hintsUsed = entry.hintsUsed,
            canUndo = undoStack.isNotEmpty()
        )
        saveIfActive()
    }

    fun signalRejection() {
        state = state.copy(rejectionTick = state.rejectionTick + 1)
    }

    fun pauseTimer() {
        timerJob?.cancel()
        saveIfActive()
    }

    fun resumeTimer() {
        if (state.screen == Screen.GAME && !state.isFinished && !state.isLoading) {
            startTimer()
        }
    }

    private fun pushUndo() {
        val board = state.board ?: return
        undoStack.addLast(UndoEntry(board, state.mistakes, state.hintsUsed))
        if (undoStack.size > 200) undoStack.removeFirst()
        if (!state.canUndo) state = state.copy(canUndo = true)
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                if (state.isFinished || state.isPaused) continue
                val nextElapsed = state.elapsedSeconds + 1
                state = state.copy(elapsedSeconds = nextElapsed)
                // Speed mode: when the budget runs out, end the game as a loss.
                if (state.isSpeed && nextElapsed >= state.timeBudgetSeconds) {
                    val ended = finalize(state.copy(isGameOver = true, lossReason = LossReason.SPEED_TIMEOUT))
                    state = ended
                    SoundManager.playLoss()
                }
            }
        }
    }

    private fun finalize(snapshot: GameState): GameState {
        timerJob?.cancel()
        SaveManager.clear(getApplication())
        val difficulty = snapshot.difficulty ?: return snapshot
        val outcome = StatsManager.recordGame(
            context = getApplication(),
            difficulty = difficulty,
            mode = snapshot.mode,
            won = snapshot.isComplete,
            timeSeconds = snapshot.elapsedSeconds,
            mistakes = snapshot.mistakes,
            hints = snapshot.hintsUsed
        )
        if (snapshot.isComplete && snapshot.isDaily) {
            DailyManager.recordDailyWin(getApplication())
        }
        val newAchievements = AchievementManager.checkAndUnlock(
            context = getApplication(),
            difficulty = difficulty,
            mode = snapshot.mode,
            won = snapshot.isComplete,
            timeSeconds = snapshot.elapsedSeconds,
            mistakes = snapshot.mistakes,
            hints = snapshot.hintsUsed,
            statsAfter = StatsManager.stats.value
        )
        return snapshot.copy(finishOutcome = outcome, pendingAchievements = newAchievements)
    }

    private fun saveIfActive() {
        val board = state.board ?: return
        val solution = state.solution ?: return
        if (state.screen != Screen.GAME || state.isFinished || state.isLoading) return
        val difficulty = state.difficulty ?: return
        SaveManager.save(
            getApplication(),
            SavedGame(
                difficulty = difficulty,
                solution = solution,
                cells = board.cells,
                mistakes = state.mistakes,
                hintsUsed = state.hintsUsed,
                elapsedSeconds = state.elapsedSeconds,
                notesMode = state.notesMode,
                selected = state.selected,
                isDaily = state.isDaily,
                mode = state.mode,
                cages = state.cages,
                timeBudgetSeconds = state.timeBudgetSeconds
            )
        )
    }

    private fun Board.replace(index: Int, cell: Cell): Board =
        copy(cells = cells.toMutableList().also { it[index] = cell })

    private fun autoCleanPeerNotes(board: Board, idx: Int, value: Int): Board {
        val r = idx / 9; val c = idx % 9
        val br = (r / 3) * 3; val bc = (c / 3) * 3
        val newCells = board.cells.toMutableList()
        for (i in 0 until 81) {
            if (i == idx) continue
            val ir = i / 9; val ic = i % 9
            val sameRow = ir == r
            val sameCol = ic == c
            val sameBox = ir in br..(br + 2) && ic in bc..(bc + 2)
            if (!sameRow && !sameCol && !sameBox) continue
            val cell = newCells[i]
            if (value in cell.notes) {
                newCells[i] = cell.copy(notes = cell.notes - value)
            }
        }
        return board.copy(cells = newCells)
    }

    /** Returns row/col/box that became fully + correctly filled by placing at [idx]. */
    private fun detectCompletions(board: Board, idx: Int): CompletedFlash {
        val r = idx / 9; val c = idx % 9
        val box = (r / 3) * 3 + c / 3
        val rows = mutableSetOf<Int>()
        val cols = mutableSetOf<Int>()
        val boxes = mutableSetOf<Int>()
        if (isUnitComplete(board, rowCells(r))) rows += r
        if (isUnitComplete(board, colCells(c))) cols += c
        if (isUnitComplete(board, boxCells(box))) boxes += box
        return CompletedFlash(rows, cols, boxes, System.currentTimeMillis())
    }

    private fun isUnitComplete(board: Board, indices: List<Int>): Boolean {
        val seen = BooleanArray(10)
        for (i in indices) {
            val v = board.cells[i].value
            if (v == 0 || seen[v]) return false
            seen[v] = true
        }
        return true
    }

    private fun rowCells(r: Int): List<Int> = (0..8).map { r * 9 + it }
    private fun colCells(c: Int): List<Int> = (0..8).map { it * 9 + c }
    private fun boxCells(box: Int): List<Int> {
        val br = (box / 3) * 3; val bc = (box % 3) * 3
        return (0..2).flatMap { dr -> (0..2).map { dc -> (br + dr) * 9 + (bc + dc) } }
    }
}
