package com.jaydorah.alzhimers.games.shortterm

import android.app.Application
import android.content.Context
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import coil.imageLoader
import coil.request.ImageRequest
import com.jaydorah.alzhimers.R
import com.jaydorah.alzhimers.core.AppPreferences
import com.jaydorah.alzhimers.core.ScoreKeys
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class ShortTermViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val appPrefs = AppPreferences.get(application)
    private val app = application

    private val _state = MutableStateFlow(
        ShortTermState(
            settings = loadSettings(),
            history = loadHistory(),
        ),
    )
    val state: StateFlow<ShortTermState> = _state.asStateFlow()

    private var memorizeTicker: Job? = null
    private var distractTicker: Job? = null
    private var reactionJob: Job? = null
    private var loadJob: Job? = null
    private var lastCompletedIndex = -1

    // True while the screen is not visible (app backgrounded); tickers hold their values.
    private var paused = false

    // --- Settings (persisted) ---

    fun setDistractionsEnabled(enabled: Boolean) = updateSettings {
        it.copy(distractionsEnabled = enabled)
    }

    fun setItemCount(count: Int) = updateSettings {
        it.copy(itemCount = count.coerceIn(ShortTermSettings.MIN_ITEMS, ShortTermSettings.MAX_ITEMS))
    }

    fun setMemorizeDuration(sec: Float) = updateSettings {
        it.copy(memorizeDuration = sec.coerceIn(ShortTermSettings.MIN_MEMORIZE, ShortTermSettings.MAX_MEMORIZE))
    }

    fun setTimePerTask(sec: Float) = updateSettings {
        it.copy(timePerTask = sec.coerceIn(ShortTermSettings.MIN_TIME_PER_TASK, ShortTermSettings.MAX_TIME_PER_TASK))
    }

    fun setDistractionMode(mode: DistractionMode) = updateSettings { it.copy(distractionMode = mode) }

    fun setDifficulty(difficulty: Difficulty) = updateSettings { it.copy(difficulty = difficulty) }

    fun setZenDuration(sec: Int) = updateSettings {
        it.copy(zenDuration = sec.coerceIn(ShortTermSettings.MIN_ZEN, ShortTermSettings.MAX_ZEN))
    }

    fun setVocabType(vocab: VocabType) = updateSettings { it.copy(vocabType = vocab) }

    private fun updateSettings(transform: (ShortTermSettings) -> ShortTermSettings) {
        _state.update { it.copy(settings = transform(it.settings)) }
        saveSettings(_state.value.settings)
    }

    // --- Navigation between stages ---

    fun openHistory() {
        cancelRunJobs()
        _state.update { it.copy(stage = ShortTermStage.History) }
    }

    fun closeHistory() {
        _state.update { it.copy(stage = ShortTermStage.Idle) }
    }

    fun clearHistory() {
        prefs.edit().remove(KEY_HISTORY).apply()
        _state.update { it.copy(history = emptyList()) }
    }

    fun backToIdle() {
        cancelRunJobs()
        _state.update {
            it.copy(
                stage = ShortTermStage.Idle,
                phrase = null,
                taskQueue = emptyList(),
                currentTaskIndex = 0,
                memorizeElapsedMs = 0L,
                distractElapsedSec = 0f,
                taskTimerSec = it.settings.effectiveTimePerTask,
                finalScore = 0,
                failReasonRes = null,
                stackStep = 1,
            )
        }
    }

    fun quit() = backToIdle()

    // --- Run lifecycle ---

    fun startTest() {
        cancelRunJobs()
        lastCompletedIndex = -1
        val settings = _state.value.settings
        val queueCount = when (settings.runMode) {
            RunMode.TASKS -> settings.difficulty.tasks
            RunMode.TIME, RunMode.ZEN -> ShortTermSettings.TIME_MODE_QUEUE
        }
        val queue = buildTaskQueue(queueCount)
        val phrase = generatePhrase(settings.effectiveItemCount, settings.vocabType)
        _state.update {
            it.copy(
                stage = ShortTermStage.Loading,
                phrase = phrase,
                taskQueue = queue,
                currentTaskIndex = 0,
                memorizeElapsedMs = 0L,
                distractElapsedSec = 0f,
                taskTimerSec = settings.effectiveTimePerTask,
                finalScore = 0,
                failReasonRes = null,
                stackStep = 1,
            )
        }
        loadJob = viewModelScope.launch {
            if (settings.vocabType == VocabType.POKEMON) preloadSprites(phrase.ids)
            if (_state.value.stage != ShortTermStage.Loading) return@launch
            _state.update { it.copy(stage = ShortTermStage.Memorize) }
            startMemorizeTicker()
        }
    }

    private suspend fun preloadSprites(ids: List<Int>) {
        val loader = app.imageLoader
        ids.forEach { id ->
            val request = ImageRequest.Builder(app)
                .data(pokemonSpriteUrl(id))
                .build()
            loader.execute(request)
        }
    }

    fun skipMemorize() {
        if (_state.value.stage != ShortTermStage.Memorize) return
        goToDistract()
    }

    fun setPaused(value: Boolean) {
        paused = value
    }

    private fun goToDistract() {
        memorizeTicker?.cancel()
        reactionJob?.cancel()
        _state.update {
            it.copy(
                stage = ShortTermStage.Distract,
                memorizeElapsedMs = 0L,
                distractElapsedSec = 0f,
                taskTimerSec = it.settings.effectiveTimePerTask,
            )
        }
        lastCompletedIndex = -1
        startDistractTicker()
    }

    private fun startMemorizeTicker() {
        memorizeTicker?.cancel()
        memorizeTicker = viewModelScope.launch {
            while (true) {
                delay(MEMORIZE_TICK_MS)
                val s = _state.value
                if (paused || s.stage != ShortTermStage.Memorize) continue
                val elapsed = s.memorizeElapsedMs + MEMORIZE_TICK_MS
                if (elapsed >= s.settings.effectiveMemorizeDuration * 1000f) {
                    goToDistract()
                    break
                } else {
                    _state.update { it.copy(memorizeElapsedMs = elapsed) }
                }
            }
        }
    }

    private fun startDistractTicker() {
        distractTicker?.cancel()
        val settings = _state.value.settings
        when {
            settings.distractionsEnabled && settings.distractionMode == DistractionMode.TASKS -> {
                distractTicker = viewModelScope.launch {
                    while (true) {
                        delay(DISTRACT_TICK_MS)
                        val s = _state.value
                        if (paused || s.stage != ShortTermStage.Distract) continue
                        val next = s.taskTimerSec - DISTRACT_TICK_MS / 1000f
                        if (next <= 0.05f) {
                            fail(reasonRes = R.string.stm_fail_timeout)
                            break
                        }
                        _state.update {
                            it.copy(
                                taskTimerSec = next,
                                distractElapsedSec = it.distractElapsedSec + DISTRACT_TICK_MS / 1000f,
                            )
                        }
                    }
                }
            }
            settings.distractionsEnabled && settings.distractionMode == DistractionMode.TIME -> {
                // Per-task timer + overall elapsed for TIME HUD
                distractTicker = viewModelScope.launch {
                    while (true) {
                        delay(DISTRACT_TICK_MS)
                        val s = _state.value
                        if (paused || s.stage != ShortTermStage.Distract) continue
                        val elapsed = s.distractElapsedSec + DISTRACT_TICK_MS / 1000f
                        val next = s.taskTimerSec - DISTRACT_TICK_MS / 1000f
                        if (elapsed >= s.settings.effectiveZenDuration) {
                            enterRecall(elapsed)
                            break
                        }
                        if (next <= 0.05f) {
                            fail(reasonRes = R.string.stm_fail_timeout)
                            break
                        }
                        _state.update {
                            it.copy(taskTimerSec = next, distractElapsedSec = elapsed)
                        }
                    }
                }
            }
            else -> {
                // ZEN: no per-task timer; only elapsed for countdown
                distractTicker = viewModelScope.launch {
                    while (true) {
                        delay(ZEN_TICK_MS)
                        val s = _state.value
                        if (paused || s.stage != ShortTermStage.Distract) continue
                        val elapsed = s.distractElapsedSec + ZEN_TICK_MS / 1000f
                        if (elapsed >= s.settings.effectiveZenDuration) {
                            enterRecall(elapsed)
                            break
                        }
                        _state.update { it.copy(distractElapsedSec = elapsed) }
                    }
                }
            }
        }
    }

    fun onTaskComplete() {
        val s = _state.value
        if (s.stage != ShortTermStage.Distract) return
        if (lastCompletedIndex == s.currentTaskIndex) return
        lastCompletedIndex = s.currentTaskIndex
        distractTicker?.cancel()
        reactionJob?.cancel()

        val settings = s.settings
        val isTasksMode = settings.runMode == RunMode.TASKS
        val elapsed = s.distractElapsedSec

        if (isTasksMode) {
            if (s.currentTaskIndex + 1 >= s.taskQueue.size) {
                enterRecall(elapsed)
                return
            }
        } else {
            // TIME mode: continue until zenDuration elapses (checked on next ticks / here)
            if (elapsed >= settings.effectiveZenDuration) {
                enterRecall(elapsed)
                return
            }
        }

        val nextIdx = s.currentTaskIndex + 1
        if (settings.isImpossible && nextIdx % 10 == 0 && s.stackStep < 10) {
            _state.update {
                it.copy(
                    stackStep = it.stackStep + 1,
                    currentTaskIndex = nextIdx,
                    stage = ShortTermStage.Memorize,
                    memorizeElapsedMs = 0L,
                    taskTimerSec = settings.effectiveTimePerTask,
                )
            }
            startMemorizeTicker()
        } else {
            _state.update {
                it.copy(
                    currentTaskIndex = nextIdx,
                    taskTimerSec = settings.effectiveTimePerTask,
                )
            }
            if (settings.distractionsEnabled) startDistractTicker()
        }
    }

    fun fail(@StringRes reasonRes: Int = R.string.stm_fail_default) {
        if (_state.value.stage != ShortTermStage.Distract) return
        cancelRunJobs()
        _state.update {
            it.copy(
                stage = ShortTermStage.Failed,
                failReasonRes = reasonRes,
            )
        }
    }

    private fun enterRecall(elapsed: Float) {
        cancelRunJobs()
        // For TIME mode with distractions, keep ticking elapsed display is not needed in Recall.
        _state.update {
            it.copy(stage = ShortTermStage.Recall, distractElapsedSec = elapsed)
        }
    }

    fun submitRecall(score: Int) {
        val s = _state.value
        if (s.stage != ShortTermStage.Recall) return
        val settings = s.settings
        val modeLabel = when (settings.runMode) {
            RunMode.TASKS -> "TASKS"
            RunMode.TIME -> "TIME"
            RunMode.ZEN -> "ZEN"
        }
        val entry = HistoryEntry(
            id = newHistoryId(),
            vocabType = settings.vocabType.name,
            wordCount = s.phrase?.count ?: settings.effectiveItemCount,
            mode = modeLabel,
            configValue = historyConfigValue(settings.runMode, settings),
            score = score,
            timestamp = System.currentTimeMillis(),
        )
        val history = prependHistory(entry, s.history)
        saveHistory(history)
        _state.update {
            it.copy(
                stage = ShortTermStage.Results,
                finalScore = score,
                history = history,
            )
        }
        appPrefs.recordPlayedAndScore(ScoreKeys.ShortTerm, score)
    }

    fun scheduleReactionGo(delayMs: Int, onGo: () -> Unit) {
        reactionJob?.cancel()
        reactionJob = viewModelScope.launch {
            delay(delayMs.toLong())
            onGo()
        }
    }

    private fun cancelRunJobs() {
        loadJob?.cancel()
        memorizeTicker?.cancel()
        distractTicker?.cancel()
        reactionJob?.cancel()
    }

    override fun onCleared() {
        cancelRunJobs()
        super.onCleared()
    }

    // --- Persistence ---

    private fun loadSettings(): ShortTermSettings {
        val raw = prefs.getString(KEY_SETTINGS, null) ?: return ShortTermSettings()
        return runCatching {
            val o = JSONObject(raw)
            val vocab = runCatching { VocabType.valueOf(o.getString("vocab")) }.getOrDefault(VocabType.FAMOUS)
            ShortTermSettings(
                distractionsEnabled = o.optBoolean("distractions", true),
                itemCount = o.optInt("itemCount", 5),
                vocabType = vocab,
                difficulty = runCatching { Difficulty.valueOf(o.getString("difficulty")) }.getOrDefault(Difficulty.EASY),
                memorizeDuration = o.optDouble("memorize", 5.0).toFloat(),
                timePerTask = o.optDouble("timePerTask", 5.0).toFloat(),
                distractionMode = runCatching {
                    DistractionMode.valueOf(o.getString("distractionMode"))
                }.getOrDefault(DistractionMode.TASKS),
                zenDuration = o.optInt("zen", 30),
            )
        }.getOrElse { ShortTermSettings() }
    }

    private fun saveSettings(s: ShortTermSettings) {
        val o = JSONObject().apply {
            put("distractions", s.distractionsEnabled)
            put("itemCount", s.itemCount)
            put("vocab", s.vocabType.name)
            put("difficulty", s.difficulty.name)
            put("memorize", s.memorizeDuration.toDouble())
            put("timePerTask", s.timePerTask.toDouble())
            put("distractionMode", s.distractionMode.name)
            put("zen", s.zenDuration)
        }
        prefs.edit().putString(KEY_SETTINGS, o.toString()).apply()
    }

    private fun loadHistory(): List<HistoryEntry> {
        val raw = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                HistoryEntry(
                    id = o.optString("id", "$i"),
                    vocabType = o.optString("vocabType", "FAMOUS"),
                    wordCount = o.optInt("wordCount", 5),
                    mode = o.optString("mode", "TASKS"),
                    configValue = o.optInt("configValue", 0),
                    score = o.optInt("score", 0),
                    timestamp = o.optLong("timestamp", 0L),
                )
            }
        }.getOrElse { emptyList() }
    }

    private fun saveHistory(history: List<HistoryEntry>) {
        val arr = JSONArray()
        history.forEach { e ->
            arr.put(JSONObject().apply {
                put("id", e.id)
                put("vocabType", e.vocabType)
                put("wordCount", e.wordCount)
                put("mode", e.mode)
                put("configValue", e.configValue)
                put("score", e.score)
                put("timestamp", e.timestamp)
            })
        }
        prefs.edit().putString(KEY_HISTORY, arr.toString()).apply()
    }

    companion object {
        private const val MEMORIZE_TICK_MS = 100L
        private const val DISTRACT_TICK_MS = 50L
        private const val ZEN_TICK_MS = 100L
        private const val PREFS_NAME = "short_term"
        private const val KEY_SETTINGS = "settings"
        private const val KEY_HISTORY = "history"
    }
}
