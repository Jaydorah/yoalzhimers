package com.jaydorah.alzhimers.games.wordhunt

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
import org.json.JSONObject
import kotlin.random.Random

class WordHuntViewModel(application: Application) : AndroidViewModel(application) {

    private val appPrefs = AppPreferences.get(application)

    private val _state = MutableStateFlow(WordHuntState())
    val state: StateFlow<WordHuntState> = _state.asStateFlow()

    private var ticker: Job? = null
    private var hintClearJob: Job? = null

    private val words: List<String> by lazy { loadWords(application) }

    fun setDifficulty(difficulty: Difficulty) {
        _state.update { it.copy(settings = it.settings.copy(difficulty = difficulty)) }
    }

    fun toggleAllowPause() {
        _state.update { it.copy(settings = it.settings.copy(allowPause = !it.settings.allowPause)) }
    }

    fun startGame() {
        hintClearJob?.cancel()
        val settings = _state.value.settings
        val round = generateRound(words, settings, Random.Default)
        _state.update {
            it.copy(
                phase = WordHuntPhase.Playing,
                round = round,
                foundWords = emptySet(),
                selection = emptyList(),
                elapsedMs = 0L,
                lastMissAtMs = -10_000L,
                missCells = emptyList(),
                hintCell = null,
                hintsUsed = 0,
            )
        }
        startTicker()
    }

    fun pause() {
        if (_state.value.phase != WordHuntPhase.Playing || !_state.value.settings.allowPause) return
        ticker?.cancel()
        _state.update { it.copy(phase = WordHuntPhase.Paused, selection = emptyList()) }
    }

    fun resume() {
        if (_state.value.phase != WordHuntPhase.Paused) return
        _state.update { it.copy(phase = WordHuntPhase.Playing) }
        startTicker()
    }

    fun quit() {
        ticker?.cancel()
        hintClearJob?.cancel()
        _state.value = WordHuntState(settings = _state.value.settings)
    }

    fun playAgain() = startGame()

    /** Stops the clock while the app is in the background. */
    fun onAppBackground() {
        ticker?.cancel()
    }

    fun onAppForeground() {
        if (_state.value.phase == WordHuntPhase.Playing && ticker?.isActive != true) startTicker()
    }

    fun giveUp() {
        val s = _state.value
        val round = s.round ?: return
        if (s.phase != WordHuntPhase.Playing && s.phase != WordHuntPhase.Paused) return
        ticker?.cancel()
        hintClearJob?.cancel()
        _state.update {
            it.copy(
                phase = WordHuntPhase.Revealed,
                foundWords = round.placements.map { p -> p.word }.toSet(),
                selection = emptyList(),
                hintCell = null,
            )
        }
        appPrefs.recordPlayedAndScore(ScoreKeys.WordHunt)
    }

    fun useHint() {
        val s = _state.value
        if (s.phase != WordHuntPhase.Playing) return
        val round = s.round ?: return
        val unfound = round.placements.filter { it.word !in s.foundWords }
        if (unfound.isEmpty()) return
        val placement = unfound.random()
        val lastIndex = placement.cells.lastIndex
        val indices = listOf(0, lastIndex / 2, lastIndex).distinct()
        val cell = placement.cells[indices.random()]
        hintClearJob?.cancel()
        _state.update { it.copy(hintCell = cell, hintsUsed = it.hintsUsed + 1) }
        hintClearJob = viewModelScope.launch {
            delay(HINT_DURATION_MS)
            _state.update { it.copy(hintCell = null) }
        }
    }

    fun backToOptions() {
        ticker?.cancel()
        hintClearJob?.cancel()
        _state.update {
            it.copy(
                phase = WordHuntPhase.SelectDifficulty,
                round = null,
                foundWords = emptySet(),
                selection = emptyList(),
                elapsedMs = 0L,
                hintCell = null,
            )
        }
    }

    fun onSelectStart(cell: Cell) {
        if (_state.value.phase != WordHuntPhase.Playing) return
        _state.update { it.copy(selection = listOf(cell)) }
    }

    fun onSelectMove(cell: Cell) {
        val s = _state.value
        if (s.phase != WordHuntPhase.Playing || s.selection.isEmpty()) return
        val start = s.selection.first()
        if (cell == start) {
            _state.update { it.copy(selection = listOf(start)) }
            return
        }
        val path = cellsAlong(start, cell) ?: return
        val size = s.round?.size ?: return
        if (path.any { it.row !in 0 until size || it.col !in 0 until size }) return
        _state.update { it.copy(selection = path) }
    }

    fun onSelectEnd() {
        val s = _state.value
        if (s.phase != WordHuntPhase.Playing || s.round == null) return
        val cells = s.selection
        _state.update { it.copy(selection = emptyList()) }
        if (cells.size < 2) return
        val match = matchSelection(s.round.grid, cells, s.round.words, s.foundWords)
        if (match != null) {
            val found = s.foundWords + match
            val hintWord = s.hintCell?.let { cell ->
                s.round.placements.firstOrNull { p -> cell in p.cells }?.word
            }
            val clearHint = hintWord == match
            if (found.size == s.round.placements.size) {
                ticker?.cancel()
                _state.update {
                    it.copy(
                        phase = WordHuntPhase.Result,
                        foundWords = found,
                        hintCell = null,
                    )
                }
                appPrefs.recordPlayedAndScore(ScoreKeys.WordHunt, _state.value.elapsedMs.toInt())
            } else {
                _state.update {
                    it.copy(
                        foundWords = found,
                        hintCell = if (clearHint) null else it.hintCell,
                    )
                }
            }
        } else {
            _state.update { it.copy(lastMissAtMs = it.elapsedMs, missCells = cells) }
        }
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker = viewModelScope.launch {
            while (true) {
                delay(TICK_MS)
                val s = _state.value
                if (s.phase != WordHuntPhase.Playing) continue
                _state.update { it.copy(elapsedMs = it.elapsedMs + TICK_MS) }
            }
        }
    }

    override fun onCleared() {
        ticker?.cancel()
        hintClearJob?.cancel()
        super.onCleared()
    }

    private fun loadWords(app: Application): List<String> {
        val json = app.assets.open("english_1k.json").bufferedReader().use { it.readText() }
        val arr = JSONObject(json).getJSONArray("words")
        return buildList(arr.length()) {
            for (i in 0 until arr.length()) {
                add(arr.getString(i).lowercase())
            }
        }
    }

    companion object {
        private const val TICK_MS = 50L
        private const val HINT_DURATION_MS = 3_000L
    }
}
