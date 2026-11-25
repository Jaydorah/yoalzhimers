package com.jaydorah.alzhimers.games.mentalmath

import android.app.Application
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

class MentalMathViewModel(application: Application) : AndroidViewModel(application) {

    private val appPrefs = AppPreferences.get(application)

    private val _state = MutableStateFlow(MentalMathState())
    val state: StateFlow<MentalMathState> = _state.asStateFlow()

    private var ticker: Job? = null

    /** The ticker holds still while the app is in the background. */
    private var inBackground = false

    fun onAppBackgrounded() {
        inBackground = true
    }

    fun onAppForegrounded() {
        inBackground = false
    }

    fun setDifficulty(difficulty: Difficulty) {
        _state.update { it.copy(settings = it.settings.copy(difficulty = difficulty)) }
    }

    fun setCustomOperations(count: Int) {
        _state.update {
            it.copy(settings = it.settings.copy(customOperations = count.coerceIn(1, 10)))
        }
    }

    fun setCustomStepSeconds(seconds: Int) {
        _state.update {
            it.copy(settings = it.settings.copy(customStepSeconds = seconds.coerceIn(2, 15)))
        }
    }

    fun toggleRoundNumbers() {
        _state.update { it.copy(settings = it.settings.copy(roundNumbers = !it.settings.roundNumbers)) }
    }

    fun toggleAllowPause() {
        _state.update { it.copy(settings = it.settings.copy(allowPause = !it.settings.allowPause)) }
    }

    fun toggleOp(op: OpType) {
        _state.update { s ->
            val current = s.settings.enabledOps
            val next = if (op in current) current - op else current + op
            // Keep at least one operation on.
            val safe = next.ifEmpty { setOf(op) }
            s.copy(settings = s.settings.copy(enabledOps = safe))
        }
    }

    fun startGame() {
        val settings = _state.value.settings
        val round = generateRound(settings)
        _state.update {
            it.copy(
                phase = GamePhase.Playing,
                round = round,
                stepIndex = 0,
                stepElapsedMs = 0L,
                input = "",
                wasCorrect = false,
            )
        }
        startTicker()
    }

    fun pause() {
        val s = _state.value
        if (s.phase != GamePhase.Playing || !s.settings.allowPause) return
        ticker?.cancel()
        _state.update { it.copy(phase = GamePhase.Paused) }
    }

    fun resume() {
        if (_state.value.phase != GamePhase.Paused) return
        _state.update { it.copy(phase = GamePhase.Playing) }
        startTicker()
    }

    fun quit() {
        ticker?.cancel()
        _state.value = MentalMathState()
    }

    fun onInputDigit(digit: Char) {
        _state.update { s ->
            if (s.phase != GamePhase.Answer || s.input.length >= 7) s
            else {
                val next = when {
                    s.input == "0" && digit == '0' -> "0"
                    s.input == "0" -> digit.toString()
                    else -> s.input + digit
                }
                s.copy(input = next)
            }
        }
    }

    fun onBackspace() {
        _state.update { s ->
            if (s.phase != GamePhase.Answer) s else s.copy(input = s.input.dropLast(1))
        }
    }

    fun submitAnswer() {
        val s = _state.value
        val round = s.round ?: return
        if (s.phase != GamePhase.Answer || s.input.isEmpty()) return
        val answer = s.input.toIntOrNull() ?: return
        ticker?.cancel()
        _state.update {
            it.copy(
                phase = GamePhase.Result,
                wasCorrect = answer == round.correctAnswer,
            )
        }
        appPrefs.recordPlayedAndScore(ScoreKeys.MentalMath)
    }

    fun playAgain() = startGame()

    fun backToOptions() {
        ticker?.cancel()
        _state.update {
            it.copy(
                phase = GamePhase.SelectDifficulty,
                round = null,
                stepIndex = 0,
                stepElapsedMs = 0L,
                input = "",
                wasCorrect = false,
            )
        }
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker = viewModelScope.launch {
            while (true) {
                delay(TICK_MS)
                val s = _state.value
                if (inBackground || s.phase != GamePhase.Playing) continue
                val elapsed = s.stepElapsedMs + TICK_MS
                if (elapsed >= s.stepDurationMs) {
                    advanceStep()
                } else {
                    _state.update { it.copy(stepElapsedMs = elapsed) }
                }
            }
        }
    }

    private fun advanceStep() {
        _state.update { s ->
            val round = s.round ?: return@update s
            val next = s.stepIndex + 1
            if (next >= round.totalSteps) {
                s.copy(phase = GamePhase.Answer, stepIndex = next, stepElapsedMs = 0L, input = "")
            } else {
                s.copy(stepIndex = next, stepElapsedMs = 0L)
            }
        }
        if (_state.value.phase == GamePhase.Answer) ticker?.cancel()
    }

    companion object {
        private const val TICK_MS = 50L
    }
}
