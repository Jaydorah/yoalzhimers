package com.jaydorah.alzhimers.games.copycat

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

class CopycatViewModel(application: Application) : AndroidViewModel(application) {

    private val appPrefs = AppPreferences.get(application)

    private val _state = MutableStateFlow(CopycatState())
    val state: StateFlow<CopycatState> = _state.asStateFlow()

    private var playback: Job? = null

    fun setBoardSize(size: BoardSize) = updateSettings { it.copy(boardSize = size) }

    fun setMode(mode: CopycatMode) = updateSettings { it.copy(mode = mode) }

    fun setFixedLength(length: Int) = updateSettings {
        it.copy(fixedLength = length.coerceIn(3, 15))
    }

    fun setDifficulty(difficulty: CopycatDifficulty) = updateSettings { it.copy(difficulty = difficulty) }

    fun startGame() {
        playback?.cancel()
        val settings = _state.value.settings
        val sequence = randomSequence(settings.boardSize.pads, settings.startingLength)
        _state.update {
            it.copy(
                phase = CopycatPhase.Showing,
                sequence = sequence,
                inputIndex = 0,
                activePad = null,
                roundsCleared = 0,
            )
        }
        playSequence()
    }

    fun onPadTap(pad: Int) {
        val s = _state.value
        if (s.phase != CopycatPhase.Input) return

        val expected = s.sequence[s.inputIndex]
        if (pad != expected) {
            _state.update { it.copy(phase = CopycatPhase.GameOver, activePad = null) }
            appPrefs.recordPlayedAndScore(ScoreKeys.Copycat, s.roundsCleared)
            return
        }

        // Brief feedback flash on the correct pad.
        _state.update { it.copy(activePad = pad) }
        viewModelScope.launch {
            delay(s.settings.flashOnMs / 2)
            _state.update { current ->
                if (current.activePad == pad) current.copy(activePad = null) else current
            }
        }

        val nextIndex = s.inputIndex + 1
        if (nextIndex >= s.sequence.size) {
            val cleared = s.roundsCleared + 1
            // Leave Input right away so taps during the beat can't be scored against
            // the old pattern (or end a game that the beat would then resume).
            _state.update {
                it.copy(phase = CopycatPhase.Showing, roundsCleared = cleared, inputIndex = 0)
            }
            // Small beat before showing the next pattern. Tracked as playback so
            // backToOptions()/quit() cancel it instead of it restarting the game.
            playback?.cancel()
            playback = viewModelScope.launch {
                delay(400)
                val settings = _state.value.settings
                val next = nextSequence(settings, previous = s.sequence)
                _state.update {
                    it.copy(
                        phase = CopycatPhase.Showing,
                        sequence = next,
                        inputIndex = 0,
                        activePad = null,
                    )
                }
                playSequence()
            }
        } else {
            _state.update { it.copy(inputIndex = nextIndex) }
        }
    }

    fun playAgain() = startGame()

    fun backToOptions() {
        playback?.cancel()
        _state.update {
            it.copy(
                phase = CopycatPhase.Options,
                sequence = emptyList(),
                inputIndex = 0,
                activePad = null,
                roundsCleared = 0,
            )
        }
    }

    fun quit() {
        playback?.cancel()
        _state.value = CopycatState()
    }

    private fun updateSettings(block: (CopycatSettings) -> CopycatSettings) {
        _state.update { it.copy(settings = block(it.settings)) }
    }

    private fun playSequence() {
        playback?.cancel()
        playback = viewModelScope.launch {
            val s = _state.value
            val settings = s.settings
            delay(500)
            s.sequence.forEachIndexed { _, pad ->
                _state.update { it.copy(activePad = pad) }
                delay(settings.flashOnMs)
                _state.update { it.copy(activePad = null) }
                delay(settings.flashGapMs)
            }
            // Only move to input if this playback is still the live one.
            if (_state.value.phase == CopycatPhase.Showing) {
                _state.update { it.copy(phase = CopycatPhase.Input, inputIndex = 0, activePad = null) }
            }
        }
    }

    override fun onCleared() {
        playback?.cancel()
        super.onCleared()
    }
}
