package com.jaydorah.alzhimers.games.chimp

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

class ChimpViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val appPrefs = AppPreferences.get(application)

    private var viewTimer: Job? = null

    // Timers hold while the app is in the background so memorize time isn't lost.
    private var inBackground = false

    fun onAppBackgrounded() {
        inBackground = true
    }

    fun onAppForegrounded() {
        inBackground = false
    }

    private val _state = MutableStateFlow(
        ChimpState(
            difficulty = coerceDifficulty(
                prefs.getInt(KEY_DIFFICULTY, ChimpDifficulty.Easy.ordinal),
            ),
            startingNumber = coerceStartingNumber(prefs.getInt(KEY_STARTING, CHIMP_MIN_START)),
        ).let { it.copy(currentCount = it.startingNumber) },
    )
    val state: StateFlow<ChimpState> = _state.asStateFlow()

    fun setStartingNumber(value: Int) {
        val n = coerceStartingNumber(value)
        prefs.edit().putInt(KEY_STARTING, n).apply()
        _state.update { it.copy(startingNumber = n, currentCount = n) }
    }

    fun setDifficulty(difficulty: ChimpDifficulty) {
        prefs.edit().putInt(KEY_DIFFICULTY, difficulty.ordinal).apply()
        _state.update { it.copy(difficulty = difficulty) }
    }

    fun startGame() {
        cancelViewTimer()
        _state.update {
            it.copy(
                currentCount = it.startingNumber,
                strikes = 0,
                roundsCleared = 0,
                hardViewWindowMs = CHIMP_VIEW_WINDOW_MS,
                viewWindowRemainingMs = null,
            )
        }
        _state.update(::startChimpRound)
        startViewTimerForCurrentRound()
    }

    fun onCellTap(index: Int) {
        val s = _state.value
        if (s.phase != ChimpPhase.Playing || index !in s.grid.indices) return

        if (s.grid[index] == s.nextExpected) {
            val next = s.nextExpected + 1
            if (next > s.currentCount) {
                cancelViewTimer()
                val cleared = s.roundsCleared + 1
                val nextCount = (s.currentCount + 1).coerceAtMost(CHIMP_MAX_NUMBERS)
                val timedRound = viewWindowDurationMs(
                    difficulty = s.difficulty,
                    count = s.currentCount,
                    hardViewWindowMs = s.hardViewWindowMs,
                ) != null
                val nextHardWindow = if (s.difficulty == ChimpDifficulty.Hard && timedRound) {
                    nextHardViewWindowMs(s.hardViewWindowMs)
                } else {
                    s.hardViewWindowMs
                }
                _state.update {
                    it.copy(
                        phase = ChimpPhase.RoundResult,
                        roundsCleared = cleared,
                        currentCount = nextCount,
                        numbersVisible = true,
                        nextExpected = 1,
                        hardViewWindowMs = nextHardWindow,
                        viewWindowRemainingMs = null,
                    )
                }
            } else {
                cancelViewTimer()
                _state.update {
                    it.copy(nextExpected = next, numbersVisible = false, viewWindowRemainingMs = null)
                }
            }
        } else {
            cancelViewTimer()
            val strikes = s.strikes + 1
            if (strikes >= CHIMP_MAX_STRIKES) {
                _state.update {
                    it.copy(
                        phase = ChimpPhase.GameOver,
                        strikes = strikes,
                        viewWindowRemainingMs = null,
                    )
                }
                appPrefs.recordPlayedAndScore(ScoreKeys.Chimp, s.roundsCleared)
            } else {
                _state.update { it.copy(strikes = strikes) }
                _state.update(::startChimpRound)
                startViewTimerForCurrentRound()
            }
        }
    }

    fun nextRound() {
        _state.update(::startChimpRound)
        startViewTimerForCurrentRound()
    }

    fun backToOptions() {
        cancelViewTimer()
        val s = _state.value
        // Leaving a run that is still alive (e.g. Back/Options on the round-clear screen)
        // ends it, so keep the rounds already cleared. GameOver has recorded already.
        if ((s.phase == ChimpPhase.Playing || s.phase == ChimpPhase.RoundResult) && s.roundsCleared > 0) {
            appPrefs.recordPlayedAndScore(ScoreKeys.Chimp, s.roundsCleared)
        }
        _state.update {
            it.copy(
                phase = ChimpPhase.Options,
                currentCount = it.startingNumber,
                grid = emptyList(),
                numbersVisible = true,
                nextExpected = 1,
                strikes = 0,
                roundsCleared = 0,
                hardViewWindowMs = CHIMP_VIEW_WINDOW_MS,
                viewWindowRemainingMs = null,
            )
        }
    }

    fun playAgain() = startGame()

    private fun startViewTimerForCurrentRound() {
        cancelViewTimer()
        val duration = _state.value.viewWindowRemainingMs ?: return
        if (duration <= 0L) {
            _state.update { it.copy(numbersVisible = false, viewWindowRemainingMs = null) }
            return
        }
        viewTimer = viewModelScope.launch {
            var remaining = duration
            while (remaining > 0L) {
                delay(TICK_MS)
                val s = _state.value
                if (s.phase != ChimpPhase.Playing || !s.numbersVisible) return@launch
                if (inBackground) continue
                remaining -= TICK_MS
                _state.update {
                    it.copy(viewWindowRemainingMs = remaining.coerceAtLeast(0L))
                }
            }
            _state.update { it.copy(numbersVisible = false, viewWindowRemainingMs = null) }
        }
    }

    private fun cancelViewTimer() {
        viewTimer?.cancel()
        viewTimer = null
    }

    override fun onCleared() {
        cancelViewTimer()
        super.onCleared()
    }

    companion object {
        private const val PREFS_NAME = "chimp_prefs"
        private const val KEY_STARTING = "starting_number"
        private const val KEY_DIFFICULTY = "difficulty"
        private const val TICK_MS = 50L

        private fun coerceDifficulty(ordinal: Int): ChimpDifficulty =
            ChimpDifficulty.entries.getOrElse(ordinal) { ChimpDifficulty.Easy }
    }
}
