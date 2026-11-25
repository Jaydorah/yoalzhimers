package com.jaydorah.alzhimers.games.percentages

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

class PercentagesViewModel(application: Application) : AndroidViewModel(application) {

    private val appPrefs = AppPreferences.get(application)

    private val _state = MutableStateFlow(PercentagesState())
    val state: StateFlow<PercentagesState> = _state.asStateFlow()

    private var ticker: Job? = null
    private var feedback: Job? = null
    private var scoreRecorded = false

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

    fun setCustomSeconds(seconds: Int) {
        _state.update {
            it.copy(settings = it.settings.copy(customSeconds = seconds.coerceIn(MIN_SECONDS, MAX_SECONDS)))
        }
    }

    fun setMinNumber(value: Int) {
        _state.update { it.copy(settings = it.settings.withMinNumber(value)) }
    }

    fun setMaxNumber(value: Int) {
        _state.update { it.copy(settings = it.settings.withMaxNumber(value)) }
    }

    fun toggleRoundNumbers() {
        _state.update {
            it.copy(settings = it.settings.copy(roundNumbers = !it.settings.roundNumbers))
        }
    }

    fun startGame() {
        feedback?.cancel()
        ticker?.cancel()
        scoreRecorded = false
        _state.update {
            it.copy(
                phase = GamePhase.Question,
                questions = generateQuestions(it.settings),
                index = 0,
                remainingMs = it.timeLimitMs,
                input = "",
                wasCorrect = false,
                timedOut = false,
                correctCount = 0,
                streak = 0,
                bestStreak = 0,
            )
        }
        startTicker()
    }

    fun onInputDigit(digit: Char) {
        _state.update { s ->
            if (s.phase != GamePhase.Question || s.input.length >= 7) {
                s
            } else {
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
            if (s.phase != GamePhase.Question) s else s.copy(input = s.input.dropLast(1))
        }
    }

    fun submit() {
        val s = _state.value
        if (s.phase != GamePhase.Question || s.input.isEmpty()) return
        val given = s.input.toIntOrNull() ?: return
        val correct = given == s.current?.answer
        ticker?.cancel()
        _state.update { s ->
            val streak = if (correct) s.streak + 1 else 0
            s.copy(
                phase = GamePhase.Feedback,
                wasCorrect = correct,
                timedOut = false,
                correctCount = s.correctCount + if (correct) 1 else 0,
                streak = streak,
                bestStreak = maxOf(s.bestStreak, streak),
            )
        }
        scheduleNext()
    }

    fun quit() {
        feedback?.cancel()
        ticker?.cancel()
        if (_state.value.index > 0) recordRun()
        _state.value = PercentagesState(settings = _state.value.settings)
    }

    fun playAgain() = startGame()

    fun backToOptions() {
        feedback?.cancel()
        ticker?.cancel()
        recordRun()
        _state.update {
            it.copy(
                phase = GamePhase.SelectDifficulty,
                questions = emptyList(),
                index = 0,
                remainingMs = 0L,
                input = "",
                wasCorrect = false,
                timedOut = false,
                correctCount = 0,
                streak = 0,
                bestStreak = 0,
            )
        }
    }

    private fun startTicker() {
        ticker?.cancel()
        if (!_state.value.settings.timed) return
        ticker = viewModelScope.launch {
            while (true) {
                delay(TICK_MS)
                val s = _state.value
                if (inBackground || s.phase != GamePhase.Question) continue
                val remaining = s.remainingMs - TICK_MS
                if (remaining <= 0L) timeUp() else _state.update { it.copy(remainingMs = remaining) }
            }
        }
    }

    private fun timeUp() {
        if (_state.value.phase != GamePhase.Question) return
        ticker?.cancel()
        _state.update {
            it.copy(phase = GamePhase.Feedback, wasCorrect = false, timedOut = true, streak = 0)
        }
        scheduleNext()
    }

    private fun scheduleNext() {
        feedback?.cancel()
        feedback = viewModelScope.launch {
            delay(FEEDBACK_MS)
            advance()
        }
    }

    private fun advance() {
        if (_state.value.phase != GamePhase.Feedback) return
        if (_state.value.index + 1 >= _state.value.total) {
            _state.update { it.copy(phase = GamePhase.Result, remainingMs = 0L) }
            recordRun()
            return
        }
        _state.update {
            it.copy(
                phase = GamePhase.Question,
                index = it.index + 1,
                remainingMs = it.timeLimitMs,
                input = "",
            )
        }
        startTicker()
    }

    private fun recordRun() {
        if (scoreRecorded) return
        scoreRecorded = true
        appPrefs.recordPlayedAndScore(ScoreKeys.Percentages, _state.value.correctCount)
    }

    companion object {
        private const val TICK_MS = 50L
        private const val FEEDBACK_MS = 1200L
    }
}
