package com.jaydorah.alzhimers.games.lightsout

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
import kotlin.random.Random

class LightsOutViewModel(application: Application) : AndroidViewModel(application) {

    private val appPrefs = AppPreferences.get(application)

    private val _state = MutableStateFlow(LightsOutState())
    val state: StateFlow<LightsOutState> = _state.asStateFlow()

    private var ticker: Job? = null

    fun setDifficulty(difficulty: LightsOutDifficulty) {
        _state.update { it.copy(settings = it.settings.copy(difficulty = difficulty)) }
    }

    fun startGame() {
        val settings = _state.value.settings
        val board = generateBoard(settings, Random.Default)
        _state.update {
            it.copy(
                phase = LightsOutPhase.Playing,
                board = board,
                moves = 0,
                elapsedMs = 0L,
            )
        }
        startTicker()
    }

    fun onCellTap(row: Int, col: Int) {
        val s = _state.value
        if (s.phase != LightsOutPhase.Playing) return
        val size = s.gridSize
        if (row !in 0 until size || col !in 0 until size) return
        val board = toggleAt(s.board, size, row, col)
        val moves = s.moves + 1
        if (isSolved(board)) {
            ticker?.cancel()
            _state.update { it.copy(board = board, moves = moves, phase = LightsOutPhase.Result) }
            appPrefs.recordPlayedAndScore(ScoreKeys.LightsOut, moves)
        } else {
            _state.update { it.copy(board = board, moves = moves) }
        }
    }

    fun pause() {
        if (_state.value.phase != LightsOutPhase.Playing) return
        ticker?.cancel()
        _state.update { it.copy(phase = LightsOutPhase.Paused) }
    }

    fun resume() {
        if (_state.value.phase != LightsOutPhase.Paused) return
        _state.update { it.copy(phase = LightsOutPhase.Playing) }
        startTicker()
    }

    fun playAgain() = startGame()

    /** Stops the clock while the app is in the background. */
    fun onAppBackground() {
        ticker?.cancel()
    }

    fun onAppForeground() {
        if (_state.value.phase == LightsOutPhase.Playing && ticker?.isActive != true) startTicker()
    }

    fun backToOptions() {
        ticker?.cancel()
        _state.update {
            it.copy(
                phase = LightsOutPhase.Options,
                board = emptyList(),
                moves = 0,
                elapsedMs = 0L,
            )
        }
    }

    fun quit() {
        ticker?.cancel()
        _state.value = LightsOutState(settings = _state.value.settings)
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker = viewModelScope.launch {
            while (true) {
                delay(TICK_MS)
                if (_state.value.phase != LightsOutPhase.Playing) continue
                _state.update { it.copy(elapsedMs = it.elapsedMs + TICK_MS) }
            }
        }
    }

    override fun onCleared() {
        ticker?.cancel()
        super.onCleared()
    }

    companion object {
        private const val TICK_MS = 50L
    }
}
