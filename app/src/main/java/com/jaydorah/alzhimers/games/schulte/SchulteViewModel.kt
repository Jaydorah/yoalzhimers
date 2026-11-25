package com.jaydorah.alzhimers.games.schulte

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

class SchulteViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val appPrefs = AppPreferences.get(application)

    private var ticker: Job? = null
    private var flashJob: Job? = null

    private val _state = MutableStateFlow(SchulteState(settings = loadSettings()))
    val state: StateFlow<SchulteState> = _state.asStateFlow()

    fun updateSettings(transform: (SchulteSettings) -> SchulteSettings) {
        val next = transform(_state.value.settings)
        saveSettings(next)
        _state.update { it.copy(settings = next) }
    }

    fun startGame() {
        stopTicker()
        stopFlash()
        _state.update { beginGame(it) }
        startTicker()
    }

    fun onCellTap(index: Int) {
        val s = _state.value
        if (s.phase != SchultePhase.Playing) return

        if (s.settings.startOnClick && !s.hasStartedTimer) {
            _state.update { it.copy(hasStartedTimer = true) }
        }

        val now = android.os.SystemClock.elapsedRealtime()
        when (val outcome = applyTap(s, index, now)) {
            is TapOutcome.Aborted -> {
                stopTicker()
                _state.update { outcome.state }
            }
            is TapOutcome.GameComplete -> {
                stopTicker()
                stopFlash()
                val score = outcome.state.scoreMs
                val previousBest = if (appPrefs.hasBest(ScoreKeys.Schulte)) appPrefs.bestScore(ScoreKeys.Schulte) else null
                val isNewBest = previousBest == null || score < previousBest
                appPrefs.recordPlayedAndScore(ScoreKeys.Schulte, score)
                _state.update {
                    outcome.state.copy(
                        bestMs = if (isNewBest) score else previousBest!!,
                        isNewBest = isNewBest,
                    )
                }
            }
            is TapOutcome.RoundComplete -> {
                _state.update { outcome.state }
                if (outcome.state.phase == SchultePhase.Playing) {
                    // no-break mode: new table already active
                    if (!tickerActive()) startTicker()
                } else {
                    stopTicker()
                }
                scheduleFlashClear(outcome.state.flashUntilMs)
            }
            is TapOutcome.Continue -> {
                _state.update { outcome.state }
                if (!tickerActive() && outcome.state.phase == SchultePhase.Playing) {
                    startTicker()
                }
                scheduleFlashClear(outcome.state.flashUntilMs)
            }
        }
    }

    fun nextRound() {
        if (_state.value.phase != SchultePhase.RoundBreak) return
        _state.update { nextAfterBreak(it) }
        startTicker()
    }

    fun backToOptions() {
        stopTicker()
        stopFlash()
        _state.update {
            it.copy(
                phase = SchultePhase.Options,
                cells = emptyList(),
                groups = emptyList(),
                currGroup = 0,
                roundIndex = 0,
                elapsedMs = 0L,
                penaltyMs = 0L,
                wrongTaps = 0,
                correctTaps = 0,
                hasStartedTimer = false,
                lastTapIndex = -1,
                lastTapCorrect = false,
                flashUntilMs = 0L,
            )
        }
    }

    fun playAgain() = startGame()

    /** Stops the clock while the app is in the background. */
    fun onAppBackground() {
        stopTicker()
    }

    fun onAppForeground() {
        if (_state.value.phase == SchultePhase.Playing && !tickerActive()) startTicker()
    }

    private fun tickerActive(): Boolean = ticker?.isActive == true

    private fun startTicker() {
        stopTicker()
        ticker = viewModelScope.launch {
            while (true) {
                delay(TICK_MS)
                val s = _state.value
                if (s.phase != SchultePhase.Playing) return@launch
                if (s.settings.startOnClick && !s.hasStartedTimer) continue
                _state.update { it.copy(elapsedMs = it.elapsedMs + TICK_MS) }
            }
        }
    }

    private fun stopTicker() {
        ticker?.cancel()
        ticker = null
    }

    private fun scheduleFlashClear(flashUntilMs: Long) {
        stopFlash()
        if (flashUntilMs <= 0L) return
        flashJob = viewModelScope.launch {
            delay(SCHULTE_FLASH_MS)
            _state.update {
                if (it.flashUntilMs != 0L) {
                    it.copy(flashUntilMs = 0L, lastTapIndex = -1, lastTapCorrect = false)
                } else {
                    it
                }
            }
        }
    }

    private fun stopFlash() {
        flashJob?.cancel()
        flashJob = null
    }

    override fun onCleared() {
        stopTicker()
        stopFlash()
        super.onCleared()
    }

    private fun loadSettings(): SchulteSettings {
        val defaults = SchulteSettings()
        return SchulteSettings(
            gridSize = coerceGridSize(prefs.getInt(KEY_GRID, defaults.gridSize)),
            rounds = prefs.getInt(KEY_ROUNDS, defaults.rounds).coerceIn(1, 100),
            roundBreaks = prefs.getBoolean(KEY_ROUND_BREAKS, defaults.roundBreaks),
            showRounds = prefs.getBoolean(KEY_SHOW_ROUNDS, defaults.showRounds),
            groupCount = prefs.getInt(KEY_GROUPS, defaults.groupCount).coerceIn(1, 5),
            colorGroups = prefs.getBoolean(KEY_COLOR_GROUPS, defaults.colorGroups),
            inverseCount = prefs.getBoolean(KEY_INVERSE, defaults.inverseCount),
            divergentCount = prefs.getBoolean(KEY_DIVERGENT, defaults.divergentCount),
            variousCounts = prefs.getBoolean(KEY_VARIOUS, defaults.variousCounts),
            collateGroups = prefs.getBoolean(KEY_COLLATE, defaults.collateGroups),
            originalColors = prefs.getBoolean(KEY_ORIGINAL_COLORS, defaults.originalColors),
            numberOffset = prefs.getInt(KEY_OFFSET, defaults.numberOffset),
            clearCorrect = prefs.getBoolean(KEY_CLEAR, defaults.clearCorrect),
            showTrace = prefs.getBoolean(KEY_TRACE, defaults.showTrace),
            showClickResult = prefs.getBoolean(KEY_CLICK_RESULT, defaults.showClickResult),
            showCenterDot = prefs.getBoolean(KEY_CENTER_DOT, defaults.showCenterDot),
            turnSymbols = prefs.getBoolean(KEY_TURN, defaults.turnSymbols),
            spinSymbols = prefs.getBoolean(KEY_SPIN_SYMBOLS, defaults.spinSymbols),
            spinTable = prefs.getBoolean(KEY_SPIN_TABLE, defaults.spinTable),
            spinTableSpeed = runCatching {
                SpinSpeed.valueOf(prefs.getString(KEY_SPIN_SPEED, defaults.spinTableSpeed.name) ?: defaults.spinTableSpeed.name)
            }.getOrDefault(defaults.spinTableSpeed),
            noErrors = prefs.getBoolean(KEY_NO_ERRORS, defaults.noErrors),
            startOnClick = prefs.getBoolean(KEY_START_ON_CLICK, defaults.startOnClick),
            shuffleSymbols = prefs.getBoolean(KEY_SHUFFLE, defaults.shuffleSymbols),
            frenzyMode = prefs.getBoolean(KEY_FRENZY, defaults.frenzyMode),
            frenzyCount = prefs.getInt(KEY_FRENZY_COUNT, defaults.frenzyCount).coerceIn(1, 6),
            hideReact = prefs.getBoolean(KEY_HIDE_REACT, defaults.hideReact),
            blindMode = prefs.getBoolean(KEY_BLIND, defaults.blindMode),
            mathMode = prefs.getBoolean(KEY_MATH, defaults.mathMode),
            lettersMode = prefs.getBoolean(KEY_LETTERS, defaults.lettersMode),
        )
    }

    private fun saveSettings(s: SchulteSettings) {
        prefs.edit()
            .putInt(KEY_GRID, s.gridSize)
            .putInt(KEY_ROUNDS, s.rounds)
            .putBoolean(KEY_ROUND_BREAKS, s.roundBreaks)
            .putBoolean(KEY_SHOW_ROUNDS, s.showRounds)
            .putInt(KEY_GROUPS, s.groupCount)
            .putBoolean(KEY_COLOR_GROUPS, s.colorGroups)
            .putBoolean(KEY_INVERSE, s.inverseCount)
            .putBoolean(KEY_DIVERGENT, s.divergentCount)
            .putBoolean(KEY_VARIOUS, s.variousCounts)
            .putBoolean(KEY_COLLATE, s.collateGroups)
            .putBoolean(KEY_ORIGINAL_COLORS, s.originalColors)
            .putInt(KEY_OFFSET, s.numberOffset)
            .putBoolean(KEY_CLEAR, s.clearCorrect)
            .putBoolean(KEY_TRACE, s.showTrace)
            .putBoolean(KEY_CLICK_RESULT, s.showClickResult)
            .putBoolean(KEY_CENTER_DOT, s.showCenterDot)
            .putBoolean(KEY_TURN, s.turnSymbols)
            .putBoolean(KEY_SPIN_SYMBOLS, s.spinSymbols)
            .putBoolean(KEY_SPIN_TABLE, s.spinTable)
            .putString(KEY_SPIN_SPEED, s.spinTableSpeed.name)
            .putBoolean(KEY_NO_ERRORS, s.noErrors)
            .putBoolean(KEY_START_ON_CLICK, s.startOnClick)
            .putBoolean(KEY_SHUFFLE, s.shuffleSymbols)
            .putBoolean(KEY_FRENZY, s.frenzyMode)
            .putInt(KEY_FRENZY_COUNT, s.frenzyCount)
            .putBoolean(KEY_HIDE_REACT, s.hideReact)
            .putBoolean(KEY_BLIND, s.blindMode)
            .putBoolean(KEY_MATH, s.mathMode)
            .putBoolean(KEY_LETTERS, s.lettersMode)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "schulte_prefs"
        private const val TICK_MS = 50L

        private const val KEY_GRID = "grid_size"
        private const val KEY_ROUNDS = "rounds"
        private const val KEY_ROUND_BREAKS = "round_breaks"
        private const val KEY_SHOW_ROUNDS = "show_rounds"
        private const val KEY_GROUPS = "groups"
        private const val KEY_COLOR_GROUPS = "color_groups"
        private const val KEY_INVERSE = "inverse"
        private const val KEY_DIVERGENT = "divergent"
        private const val KEY_VARIOUS = "various"
        private const val KEY_COLLATE = "collate"
        private const val KEY_ORIGINAL_COLORS = "original_colors"
        private const val KEY_OFFSET = "offset"
        private const val KEY_CLEAR = "clear_correct"
        private const val KEY_TRACE = "show_trace"
        private const val KEY_CLICK_RESULT = "show_click_result"
        private const val KEY_CENTER_DOT = "center_dot"
        private const val KEY_TURN = "turn_symbols"
        private const val KEY_SPIN_SYMBOLS = "spin_symbols"
        private const val KEY_SPIN_TABLE = "spin_table"
        private const val KEY_SPIN_SPEED = "spin_table_speed"
        private const val KEY_NO_ERRORS = "no_errors"
        private const val KEY_START_ON_CLICK = "start_on_click"
        private const val KEY_SHUFFLE = "shuffle_symbols"
        private const val KEY_FRENZY = "frenzy_mode"
        private const val KEY_FRENZY_COUNT = "frenzy_count"
        private const val KEY_HIDE_REACT = "hide_react"
        private const val KEY_BLIND = "blind_mode"
        private const val KEY_MATH = "math_mode"
        private const val KEY_LETTERS = "letters_mode"
    }
}
