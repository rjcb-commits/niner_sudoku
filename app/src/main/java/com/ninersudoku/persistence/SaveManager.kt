package com.ninersudoku.persistence

import android.content.Context
import com.ninersudoku.game.Cell
import com.ninersudoku.game.Difficulty
import com.ninersudoku.game.GameMode
import com.ninersudoku.game.KillerCage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

data class SavedGame(
    val difficulty: Difficulty,
    val solution: IntArray,
    val cells: List<Cell>,
    val mistakes: Int,
    val hintsUsed: Int,
    val elapsedSeconds: Int,
    val notesMode: Boolean,
    val selected: Int?,
    val isDaily: Boolean = false,
    val mode: GameMode = GameMode.CLASSIC,
    val cages: List<KillerCage> = emptyList(),
    val timeBudgetSeconds: Int = 0
) {
    init { require(solution.size == 81 && cells.size == 81) }

    fun toJson(): String {
        val solArr = JSONArray()
        for (v in solution) solArr.put(v)
        val cellsArr = JSONArray()
        for (cell in cells) {
            val c = JSONObject()
            c.put("v", cell.value)
            c.put("g", cell.isGiven)
            c.put("h", cell.isHint)
            val nArr = JSONArray()
            for (n in cell.notes.sorted()) nArr.put(n)
            c.put("n", nArr)
            cellsArr.put(c)
        }
        val cagesArr = JSONArray()
        for (cage in cages) {
            val co = JSONObject()
            val cellsList = JSONArray()
            for (idx in cage.cells.sorted()) cellsList.put(idx)
            co.put("c", cellsList)
            co.put("s", cage.targetSum)
            cagesArr.put(co)
        }
        return JSONObject().apply {
            put("difficulty", difficulty.name)
            put("solution", solArr)
            put("cells", cellsArr)
            put("mistakes", mistakes)
            put("hintsUsed", hintsUsed)
            put("elapsedSeconds", elapsedSeconds)
            put("notesMode", notesMode)
            put("selected", selected ?: -1)
            put("isDaily", isDaily)
            put("mode", mode.name)
            put("cages", cagesArr)
            put("timeBudgetSeconds", timeBudgetSeconds)
        }.toString()
    }

    fun summary(): SavedGameSummary {
        val remaining = cells.count { it.value == 0 }
        return SavedGameSummary(difficulty, elapsedSeconds, remaining, isDaily)
    }

    companion object {
        fun fromJson(raw: String): SavedGame? {
            return try {
                val obj = JSONObject(raw)
                val difficulty = Difficulty.valueOf(obj.getString("difficulty"))
                val solArr = obj.getJSONArray("solution")
                if (solArr.length() != 81) return null
                val solution = IntArray(81) { solArr.getInt(it) }
                val cellsArr = obj.getJSONArray("cells")
                if (cellsArr.length() != 81) return null
                val cells = List(81) { i ->
                    val c = cellsArr.getJSONObject(i)
                    val notesArr = c.getJSONArray("n")
                    val notes = (0 until notesArr.length()).map { notesArr.getInt(it) }.toSet()
                    Cell(
                        value = c.getInt("v"),
                        isGiven = c.getBoolean("g"),
                        isHint = c.getBoolean("h"),
                        notes = notes
                    )
                }
                val mode = try {
                    GameMode.valueOf(obj.optString("mode", "CLASSIC"))
                } catch (_: Exception) { GameMode.CLASSIC }
                val cages = if (obj.has("cages")) {
                    val arr = obj.getJSONArray("cages")
                    (0 until arr.length()).map { i ->
                        val co = arr.getJSONObject(i)
                        val cellsArr2 = co.getJSONArray("c")
                        val cellsSet = (0 until cellsArr2.length()).map { cellsArr2.getInt(it) }.toSet()
                        KillerCage(cellsSet, co.getInt("s"))
                    }
                } else emptyList()
                SavedGame(
                    difficulty = difficulty,
                    solution = solution,
                    cells = cells,
                    mistakes = obj.getInt("mistakes"),
                    hintsUsed = obj.getInt("hintsUsed"),
                    elapsedSeconds = obj.getInt("elapsedSeconds"),
                    notesMode = obj.optBoolean("notesMode", false),
                    selected = obj.getInt("selected").takeIf { it >= 0 },
                    isDaily = obj.optBoolean("isDaily", false),
                    mode = mode,
                    cages = cages,
                    timeBudgetSeconds = obj.optInt("timeBudgetSeconds", 0)
                )
            } catch (_: Exception) {
                null
            }
        }
    }
}

data class SavedGameSummary(
    val difficulty: Difficulty,
    val elapsedSeconds: Int,
    val cellsRemaining: Int,
    val isDaily: Boolean = false
)

object SaveManager {
    private const val PREFS_NAME = "sudoku_prefs"
    private const val KEY_SAVED = "saved_game"

    private val _summary = MutableStateFlow<SavedGameSummary?>(null)
    val summary: StateFlow<SavedGameSummary?> = _summary.asStateFlow()

    fun init(context: Context) {
        _summary.value = load(context)?.summary()
    }

    fun save(context: Context, game: SavedGame) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SAVED, game.toJson())
            .apply()
        _summary.value = game.summary()
    }

    fun load(context: Context): SavedGame? {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_SAVED, null) ?: return null
        return SavedGame.fromJson(raw)
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_SAVED)
            .apply()
        _summary.value = null
    }
}
