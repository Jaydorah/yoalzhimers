package com.jaydorah.alzhimers.games.colormatch

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.jaydorah.alzhimers.core.AppPreferences
import com.jaydorah.alzhimers.core.ScoreKeys
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ColorMatchViewModel(application: Application) : AndroidViewModel(application) {

    private val appPrefs = AppPreferences.get(application)

    private val _state = MutableStateFlow(ColorMatchState())
    val state: StateFlow<ColorMatchState> = _state.asStateFlow()

    fun toggleHidePreview() = updateSettings { it.copy(hidePreview = !it.hidePreview) }

    fun setRed(value: Int) = setChannel(value) { c, v -> c.copy(r = v) }

    fun setGreen(value: Int) = setChannel(value) { c, v -> c.copy(g = v) }

    fun setBlue(value: Int) = setChannel(value) { c, v -> c.copy(b = v) }

    fun startGame() {
        _state.update {
            it.copy(
                phase = ColorMatchPhase.Matching,
                target = Rgb.random(),
                guess = Rgb(128, 128, 128),
                score = null,
            )
        }
    }

    fun check() {
        val s = _state.value
        if (s.phase != ColorMatchPhase.Matching) return
        val score = matchScore(s.target, s.guess)
        _state.update {
            it.copy(
                phase = ColorMatchPhase.Result,
                score = score,
                roundsPlayed = it.roundsPlayed + 1,
                bestScore = maxOf(it.bestScore, score),
                totalScore = it.totalScore + score,
            )
        }
        appPrefs.recordPlayedAndScore(ScoreKeys.ColorMatch, score)
    }

    fun playAgain() {
        _state.update {
            it.copy(
                phase = ColorMatchPhase.Matching,
                target = Rgb.random(),
                guess = Rgb(128, 128, 128),
                score = null,
            )
        }
    }

    fun backToOptions() {
        _state.update {
            it.copy(phase = ColorMatchPhase.Options, score = null)
        }
    }

    fun quit() {
        _state.value = ColorMatchState(settings = _state.value.settings)
    }

    private fun updateSettings(block: (ColorMatchSettings) -> ColorMatchSettings) {
        _state.update { it.copy(settings = block(it.settings)) }
    }

    private fun setChannel(value: Int, map: (Rgb, Int) -> Rgb) {
        val clamped = value.coerceIn(0, 255)
        _state.update { it.copy(guess = map(it.guess, clamped)) }
    }
}
