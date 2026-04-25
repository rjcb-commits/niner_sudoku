package com.ninersudoku.ui.celebration

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object CelebrationManager {
    private const val PREFS_NAME = "sudoku_prefs"
    private const val KEY_CELEBRATION = "celebration_style"

    private val _currentStyle = MutableStateFlow(CelebrationStyle.CONFETTI)
    val currentStyle: StateFlow<CelebrationStyle> = _currentStyle.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_CELEBRATION, null)
        if (saved != null) {
            try { _currentStyle.value = CelebrationStyle.valueOf(saved) }
            catch (_: Exception) { /* unknown value → keep default */ }
        }
    }

    fun setStyle(context: Context, style: CelebrationStyle) {
        _currentStyle.value = style
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_CELEBRATION, style.name)
            .apply()
    }
}
