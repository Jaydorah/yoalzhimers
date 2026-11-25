package com.jaydorah.alzhimers.games.numbermemory

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jaydorah.alzhimers.core.AppPreferences
import com.jaydorah.alzhimers.core.ScoreKeys
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NumberMemoryViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val appPrefs = AppPreferences.get(application)

    private val _state = MutableStateFlow(
        NumberMemoryState(
            settings = GameSettings(
                startingDigits = prefs.getInt(
                    KEY_STARTING_DIGITS,
                    GameSettings.DEFAULT_STARTING_DIGITS,
                ),
            ),
        ),
    )
    val state: StateFlow<NumberMemoryState> = _state.asStateFlow()

    private var ticker: Job? = null

    /** The ticker holds still while the app is in the background. */
    private var inBackground = false

    // A run is many rounds (each correct answer moves up a level); it counts as one game played.
    private var runCounted = false

    fun onAppBackgrounded() {
        inBackground = true
    }

    fun onAppForegrounded() {
        inBackground = false
    }

    fun setStartingDigits(digits: Int) {
        val value = digits.coerceIn(GameSettings.MIN_DIGITS, GameSettings.MAX_DIGITS)
        prefs.edit().putInt(KEY_STARTING_DIGITS, value).apply()
        _state.update { it.copy(settings = it.settings.copy(startingDigits = value)) }
    }

    fun startGame() {
        runCounted = false
        val digits = _state.value.settings.effectiveStartingDigits
        beginRound(digits)
    }

    fun nextLevel() {
        val s = _state.value
        if (s.phase != NumberMemoryPhase.Result || !s.wasCorrect) return
        beginRound(nextLevelDigits(s.levelDigits))
    }

    fun playAgain() = startGame()

    fun quit() {
        ticker?.cancel()
        _state.value = NumberMemoryState(settings = _state.value.settings)
    }

    fun backToOptions() {
        ticker?.cancel()
        _state.update {
            it.copy(
                phase = NumberMemoryPhase.Options,
                round = null,
                showElapsedMs = 0L,
                input = "",
                wasCorrect = false,
            )
        }
    }

    fun onInputDigit(digit: Char) {
        _state.update { s ->
            if (s.phase != NumberMemoryPhase.Answer || s.input.length >= MAX_INPUT) s
            else s.copy(input = s.input + digit)
        }
    }

    fun onBackspace() {
        _state.update { s ->
            if (s.phase != NumberMemoryPhase.Answer) s else s.copy(input = s.input.dropLast(1))
        }
    }

    fun submitAnswer() {
        val s = _state.value
        val round = s.round ?: return
        if (s.phase != NumberMemoryPhase.Answer || s.input.isEmpty()) return
        ticker?.cancel()
        val correct = s.input == round.number
        _state.update {
            it.copy(
                phase = NumberMemoryPhase.Result,
                wasCorrect = correct,
                bestDigits = if (correct) maxOf(it.bestDigits, it.levelDigits) else it.bestDigits,
            )
        }
        // Best score is the longest number recalled correctly, so a miss records no score.
        val score = if (correct) s.levelDigits else null
        if (!runCounted) {
            runCounted = true
            appPrefs.recordPlayedAndScore(ScoreKeys.NumberMemory, score)
        } else if (score != null) {
            appPrefs.recordScore(ScoreKeys.NumberMemory, score)
        }
    }

    private fun beginRound(digits: Int) {
        ticker?.cancel()
        _state.update {
            it.copy(
                phase = NumberMemoryPhase.Showing,
                round = Round(randomDigits(digits)),
                levelDigits = digits,
                showElapsedMs = 0L,
                input = "",
                wasCorrect = false,
            )
        }
        startTicker()
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker = viewModelScope.launch {
            while (true) {
                delay(TICK_MS)
                val s = _state.value
                if (inBackground || s.phase != NumberMemoryPhase.Showing) continue
                val elapsed = s.showElapsedMs + TICK_MS
                if (elapsed >= s.showDurationMs) {
                    ticker?.cancel()
                    _state.update {
                        it.copy(phase = NumberMemoryPhase.Answer, showElapsedMs = 0L, input = "")
                    }
                } else {
                    _state.update { it.copy(showElapsedMs = elapsed) }
                }
            }
        }
    }

    override fun onCleared() {
        ticker?.cancel()
        super.onCleared()
    }

    companion object {
        private const val TICK_MS = 50L
        private const val MAX_INPUT = MAX_ROUND_DIGITS
        private const val PREFS_NAME = "number_memory"
        private const val KEY_STARTING_DIGITS = "starting_digits"
    }
}
