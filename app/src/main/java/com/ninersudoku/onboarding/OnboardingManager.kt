package com.ninersudoku.onboarding

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object OnboardingManager {
    private const val PREFS_NAME = "sudoku_prefs"
    private const val KEY_SEEN = "onboarding_seen_v1"

    private val _seen = MutableStateFlow(false)
    val seen: StateFlow<Boolean> = _seen.asStateFlow()

    fun init(context: Context) {
        _seen.value = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_SEEN, false)
    }

    fun markSeen(context: Context) {
        _seen.value = true
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_SEEN, true)
            .apply()
    }

    fun resetForReplay(context: Context) {
        _seen.value = false
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_SEEN)
            .apply()
    }
}
