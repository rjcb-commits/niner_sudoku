package com.ninersudoku.sound

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Lightweight in-app SFX using the system [ToneGenerator]. No bundled audio assets — every sound
 * is a short DTMF / system tone played at quiet volume so it doesn't dominate the room.
 *
 * For "moments" (win, completion, loss) we sequence multiple tones into a chime instead of
 * playing a single beep. Toggleable via [setEnabled] (persisted in shared prefs).
 */
object SoundManager {
    private const val PREFS_NAME = "sudoku_prefs"
    private const val KEY_ENABLED = "sound_enabled"
    private const val VOLUME = 40 // 0–100

    private val _enabled = MutableStateFlow(false)
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    private var generator: ToneGenerator? = null
    // Owned scope so we can cancel it on release. Dispatchers.Default is fine
    // since these tone calls don't touch the UI thread.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _enabled.value = prefs.getBoolean(KEY_ENABLED, false)
        ensureGenerator()
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        _enabled.value = enabled
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ENABLED, enabled)
            .apply()
    }

    fun playTap() = play(ToneGenerator.TONE_PROP_BEEP, 28)
    fun playEnter() = play(ToneGenerator.TONE_PROP_PROMPT, 60)
    fun playMistake() = play(ToneGenerator.TONE_PROP_NACK, 220)

    /** Row/col/box clear: short two-note "ding". */
    fun playComplete() = sequence(
        ToneGenerator.TONE_DTMF_5 to 70,
        ToneGenerator.TONE_DTMF_8 to 110,
        gapMs = 70
    )

    /** Win: ascending three-note chime that feels celebratory. */
    fun playWin() = sequence(
        ToneGenerator.TONE_DTMF_3 to 90,
        ToneGenerator.TONE_DTMF_6 to 90,
        ToneGenerator.TONE_DTMF_9 to 200,
        gapMs = 90
    )

    /** Loss: descending two-note dirge, less harsh than the busy buzzer. */
    fun playLoss() = sequence(
        ToneGenerator.TONE_DTMF_5 to 120,
        ToneGenerator.TONE_DTMF_1 to 280,
        gapMs = 120
    )

    private fun play(tone: Int, durationMs: Int) {
        if (!_enabled.value) return
        try {
            ensureGenerator()
            generator?.startTone(tone, durationMs)
        } catch (_: Exception) {
            // ToneGenerator can throw if audio focus is denied; ignore.
        }
    }

    private fun sequence(vararg tones: Pair<Int, Int>, gapMs: Long) {
        if (!_enabled.value) return
        scope.launch {
            tones.forEachIndexed { idx, (tone, dur) ->
                play(tone, dur)
                if (idx < tones.size - 1) delay(gapMs)
            }
        }
    }

    private fun ensureGenerator() {
        if (generator == null) {
            generator = try {
                ToneGenerator(AudioManager.STREAM_MUSIC, VOLUME)
            } catch (_: Exception) {
                null
            }
        }
    }

    /**
     * Release the underlying [ToneGenerator] HAL handle and cancel the
     * sequencing scope. Called from `MainActivity.onDestroy` to avoid
     * accumulating audio handles on low-end devices.
     */
    fun release() {
        try { generator?.release() } catch (_: Exception) { /* ignore */ }
        generator = null
        scope.cancel()
    }
}
