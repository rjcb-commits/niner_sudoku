package com.ninersudoku.ui.theme

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ThemeManager {
    private const val PREFS_NAME = "sudoku_prefs"
    private const val KEY_THEME = "app_theme"

    private val _currentTheme = MutableStateFlow(AppThemeVariant.ELECTRIC)
    val currentTheme: StateFlow<AppThemeVariant> = _currentTheme.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_THEME, null)
        if (saved != null) {
            try { _currentTheme.value = AppThemeVariant.valueOf(saved) }
            catch (_: Exception) { /* unknown value → keep default */ }
        }
    }

    fun setTheme(context: Context, variant: AppThemeVariant) {
        _currentTheme.value = variant
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_THEME, variant.name)
            .apply()
    }
}
