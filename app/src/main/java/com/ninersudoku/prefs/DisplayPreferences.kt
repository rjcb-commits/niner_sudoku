package com.ninersudoku.prefs

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * Display + accessibility toggles. All persisted in shared prefs.
 */
object DisplayPreferences {
    private const val PREFS_NAME = "sudoku_prefs"
    private const val KEY_THEME_MODE = "theme_mode"
    private const val KEY_PEER_HIGHLIGHT = "peer_highlight"
    private const val KEY_SAME_NUMBER_HIGHLIGHT = "same_number_highlight"
    private const val KEY_LARGE_TEXT = "large_text"
    private const val KEY_CENTERED_NOTES = "centered_notes"
    private const val KEY_COLOR_BLIND = "color_blind"
    private const val KEY_AUTO_RULE_OUT = "auto_rule_out"

    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _peerHighlight = MutableStateFlow(true)
    val peerHighlight: StateFlow<Boolean> = _peerHighlight.asStateFlow()

    private val _sameNumberHighlight = MutableStateFlow(true)
    val sameNumberHighlight: StateFlow<Boolean> = _sameNumberHighlight.asStateFlow()

    private val _largeText = MutableStateFlow(false)
    val largeText: StateFlow<Boolean> = _largeText.asStateFlow()

    private val _centeredNotes = MutableStateFlow(false)
    val centeredNotes: StateFlow<Boolean> = _centeredNotes.asStateFlow()

    private val _colorBlind = MutableStateFlow(false)
    val colorBlind: StateFlow<Boolean> = _colorBlind.asStateFlow()

    /**
     * When true, the number pad dims any digit that already conflicts with the selected
     * cell's row/column/box — visual training wheels. Default OFF so the pad doesn't do
     * the deduction work for the player. Coach-style assists belong on Coach mode, not
     * sneaked into Classic / Strict / Killer.
     */
    private val _autoRuleOut = MutableStateFlow(false)
    val autoRuleOut: StateFlow<Boolean> = _autoRuleOut.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val tm = prefs.getString(KEY_THEME_MODE, null)
        if (tm != null) {
            try { _themeMode.value = ThemeMode.valueOf(tm) } catch (_: Exception) {}
        }
        _peerHighlight.value = prefs.getBoolean(KEY_PEER_HIGHLIGHT, true)
        _sameNumberHighlight.value = prefs.getBoolean(KEY_SAME_NUMBER_HIGHLIGHT, true)
        _largeText.value = prefs.getBoolean(KEY_LARGE_TEXT, false)
        _centeredNotes.value = prefs.getBoolean(KEY_CENTERED_NOTES, false)
        _colorBlind.value = prefs.getBoolean(KEY_COLOR_BLIND, false)
        _autoRuleOut.value = prefs.getBoolean(KEY_AUTO_RULE_OUT, false)
    }

    fun setThemeMode(context: Context, mode: ThemeMode) {
        _themeMode.value = mode
        edit(context) { putString(KEY_THEME_MODE, mode.name) }
    }

    fun setPeerHighlight(context: Context, on: Boolean) {
        _peerHighlight.value = on
        edit(context) { putBoolean(KEY_PEER_HIGHLIGHT, on) }
    }

    fun setSameNumberHighlight(context: Context, on: Boolean) {
        _sameNumberHighlight.value = on
        edit(context) { putBoolean(KEY_SAME_NUMBER_HIGHLIGHT, on) }
    }

    fun setLargeText(context: Context, on: Boolean) {
        _largeText.value = on
        edit(context) { putBoolean(KEY_LARGE_TEXT, on) }
    }

    fun setCenteredNotes(context: Context, on: Boolean) {
        _centeredNotes.value = on
        edit(context) { putBoolean(KEY_CENTERED_NOTES, on) }
    }

    fun setColorBlind(context: Context, on: Boolean) {
        _colorBlind.value = on
        edit(context) { putBoolean(KEY_COLOR_BLIND, on) }
    }

    fun setAutoRuleOut(context: Context, on: Boolean) {
        _autoRuleOut.value = on
        edit(context) { putBoolean(KEY_AUTO_RULE_OUT, on) }
    }

    private inline fun edit(context: Context, block: android.content.SharedPreferences.Editor.() -> Unit) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().apply(block).apply()
    }
}
