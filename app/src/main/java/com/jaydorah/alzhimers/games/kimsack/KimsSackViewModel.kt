package com.jaydorah.alzhimers.games.kimsack

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
import kotlin.random.Random

class KimsSackViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val appPrefs = AppPreferences.get(application)

    private var timer: Job? = null

    // Timers hold while the app is in the background so memorize time isn't lost.
    private var inBackground = false

    fun onAppBackgrounded() {
        inBackground = true
    }

    fun onAppForegrounded() {
        inBackground = false
    }

    private val _state = MutableStateFlow(
        KimsSackState(
            settings = KimsSackSettings(
                difficulty = coerceDifficulty(
                    prefs.getInt(KEY_DIFFICULTY, KimsSackDifficulty.Normal.ordinal),
                ),
                preciseLocation = prefs.getBoolean(KEY_PRECISE, false),
            ),
        ),
    )
    val state: StateFlow<KimsSackState> = _state.asStateFlow()

    fun setDifficulty(difficulty: KimsSackDifficulty) {
        prefs.edit().putInt(KEY_DIFFICULTY, difficulty.ordinal).apply()
        _state.update { it.copy(settings = it.settings.copy(difficulty = difficulty)) }
    }

    fun setPreciseLocation(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_PRECISE, enabled).apply()
        _state.update { it.copy(settings = it.settings.copy(preciseLocation = enabled)) }
    }

    fun startGame() {
        cancelTimer()
        val d = _state.value.settings.difficulty
        val tray = pickTrayItems(d.itemCount, Random.Default)
        _state.update {
            it.copy(
                phase = KimsSackPhase.Memorize,
                trayItems = tray,
                pool = emptyList(),
                placed = emptyMap(),
                elapsedMs = 0L,
                correctCount = 0,
                wrongCount = 0,
            )
        }
        startMemorizeTimer()
    }

    fun hideNow() = finishMemorize()

    fun finishCollect() {
        val s = _state.value
        if (s.phase != KimsSackPhase.Collect) return
        _state.update { it.copy(phase = KimsSackPhase.Recall, placed = emptyMap()) }
    }

    fun placeItem(slot: Int, item: ItemType) {
        val s = _state.value
        if (s.phase != KimsSackPhase.Recall) return
        if (slot !in 0 until s.settings.difficulty.itemCount) return
        if (s.placed.containsKey(slot)) return
        if (item !in s.remainingPool) return
        _state.update { it.copy(placed = it.placed + (slot to item)) }
    }

    fun removeItem(slot: Int) {
        val s = _state.value
        if (s.phase != KimsSackPhase.Recall) return
        if (slot !in s.placed) return
        _state.update { it.copy(placed = it.placed - slot) }
    }

    fun submit() {
        val s = _state.value
        if (s.phase != KimsSackPhase.Recall) return
        cancelTimer()
        val precise = s.settings.preciseLocation
        val correct = scoreRecall(s.trayItems, s.placed, precise)
        val wrong = wrongPlaced(s.trayItems, s.placed, precise)
        _state.update {
            it.copy(
                phase = KimsSackPhase.Result,
                correctCount = correct,
                wrongCount = wrong,
            )
        }
        appPrefs.recordPlayedAndScore(ScoreKeys.KimsSack, correct)
    }

    fun playAgain() = startGame()

    fun backToOptions() {
        cancelTimer()
        _state.update {
            it.copy(
                phase = KimsSackPhase.Options,
                trayItems = emptyList(),
                pool = emptyList(),
                placed = emptyMap(),
                elapsedMs = 0L,
                correctCount = 0,
                wrongCount = 0,
            )
        }
    }

    fun quit() = backToOptions()

    private fun startMemorizeTimer() {
        cancelTimer()
        timer = viewModelScope.launch {
            var elapsed = 0L
            val duration = _state.value.memorizeDurationMs
            while (elapsed < duration) {
                delay(TICK_MS)
                if (_state.value.phase != KimsSackPhase.Memorize) return@launch
                if (inBackground) continue
                elapsed += TICK_MS
                _state.update { it.copy(elapsedMs = elapsed.coerceAtMost(duration)) }
            }
            finishMemorize()
        }
    }

    private fun finishMemorize() {
        cancelTimer()
        val s = _state.value
        if (s.phase != KimsSackPhase.Memorize) return
        _state.update {
            it.copy(
                phase = KimsSackPhase.Collect,
                elapsedMs = it.memorizeDurationMs,
                pool = buildPool(it.trayItems, it.settings.difficulty.decoys, Random.Default),
                placed = emptyMap(),
            )
        }
        timer = viewModelScope.launch {
            delay(COLLECT_MS)
            finishCollect()
        }
    }

    private fun cancelTimer() {
        timer?.cancel()
        timer = null
    }

    override fun onCleared() {
        cancelTimer()
        super.onCleared()
    }

    companion object {
        private const val PREFS_NAME = "kimsack_prefs"
        private const val KEY_DIFFICULTY = "difficulty"
        private const val KEY_PRECISE = "precise_location"
        private const val TICK_MS = 50L
        private const val COLLECT_MS = 1200L

        private fun coerceDifficulty(ordinal: Int): KimsSackDifficulty =
            KimsSackDifficulty.entries.getOrElse(ordinal) { KimsSackDifficulty.Normal }
    }
}
