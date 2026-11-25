package com.jaydorah.alzhimers.games.shortterm

import androidx.annotation.StringRes
import com.jaydorah.alzhimers.R
import com.jaydorah.alzhimers.ui.components.SPRITE_NAMES
import kotlin.random.Random

enum class ShortTermStage {
    Idle,
    History,
    Loading,
    Memorize,
    Distract,
    Recall,
    Results,
    Failed,
}

enum class VocabType { FAMOUS, RANDOM, NUMBERS, LETTERS, POKEMON, SPRITES }

fun VocabType.isImageVocab(): Boolean = this == VocabType.POKEMON || this == VocabType.SPRITES

fun imageChoices(vocab: VocabType): List<String> = when (vocab) {
    VocabType.POKEMON -> POKEMON_LIST
    VocabType.SPRITES -> SPRITE_NAMES
    else -> emptyList()
}

enum class DistractionMode { TASKS, TIME }

enum class Difficulty(val tasks: Int) {
    EASY(10),
    MEDIUM(20),
    HARD(30),
    SUPER(40),
    ULTRA(50),
    IMPOSSIBLE(100),
}

enum class RunMode { TASKS, TIME, ZEN }

data class ShortTermSettings(
    val distractionsEnabled: Boolean = true,
    val itemCount: Int = 5,
    val vocabType: VocabType = VocabType.FAMOUS,
    val difficulty: Difficulty = Difficulty.EASY,
    val memorizeDuration: Float = 5.0f,
    val timePerTask: Float = 5.0f,
    val distractionMode: DistractionMode = DistractionMode.TASKS,
    val zenDuration: Int = 30,
) {
    // The difficulty picker is only shown for task-based runs; a stale IMPOSSIBLE choice must not
    // turn TIME/ZEN runs into stack mode.
    val isImpossible: Boolean get() = difficulty == Difficulty.IMPOSSIBLE && runMode == RunMode.TASKS

    val runMode: RunMode
        get() = when {
            !distractionsEnabled -> RunMode.ZEN
            distractionMode == DistractionMode.TASKS -> RunMode.TASKS
            else -> RunMode.TIME
        }

    val effectiveItemCount: Int
        get() = if (isImpossible) STACK_ITEM_COUNT else itemCount.coerceIn(MIN_ITEMS, MAX_ITEMS)

    val effectiveMemorizeDuration: Float get() = memorizeDuration.coerceIn(MIN_MEMORIZE, MAX_MEMORIZE)
    val effectiveTimePerTask: Float get() = timePerTask.coerceIn(MIN_TIME_PER_TASK, MAX_TIME_PER_TASK)
    val effectiveZenDuration: Int get() = zenDuration.coerceIn(MIN_ZEN, MAX_ZEN)

    companion object {
        const val STACK_ITEM_COUNT = 10
        const val MIN_ITEMS = 3
        const val MAX_ITEMS = 20
        const val MIN_MEMORIZE = 0.1f
        const val MAX_MEMORIZE = 5.0f
        const val MIN_TIME_PER_TASK = 1.0f
        const val MAX_TIME_PER_TASK = 5.0f
        const val MIN_ZEN = 1
        const val MAX_ZEN = 120
        const val TIME_MODE_QUEUE = 200
    }
}

data class Phrase(
    val words: List<String>,
    val ids: List<Int> = emptyList(),
) {
    val combined: String get() = words.joinToString("")
    val count: Int get() = words.size
}

fun generatePhrase(count: Int, vocabType: VocabType, rng: Random = Random): Phrase {
    val n = count.coerceAtLeast(0)
    val words = mutableListOf<String>()
    val ids = mutableListOf<Int>()
    when (vocabType) {
        VocabType.RANDOM -> {
            for (i in 0 until n) {
                val bank = when (i % 3) {
                    0 -> ADJECTIVES
                    1 -> NOUNS
                    else -> VERBS
                }
                var pick = bank[rng.nextInt(bank.size)]
                var guard = 0
                while (pick in words && guard < 500) {
                    pick = bank[rng.nextInt(bank.size)]
                    guard++
                }
                words += pick
            }
        }
        VocabType.FAMOUS -> {
            for (i in 0 until n) {
                var pick = FAMOUS_LIST[rng.nextInt(FAMOUS_LIST.size)]
                var guard = 0
                while (pick in words && guard < 500) {
                    pick = FAMOUS_LIST[rng.nextInt(FAMOUS_LIST.size)]
                    guard++
                }
                words += pick
            }
        }
        VocabType.NUMBERS -> {
            repeat(n) { words += NUMBERS_LIST[rng.nextInt(NUMBERS_LIST.size)] }
        }
        VocabType.LETTERS -> {
            repeat(n) { words += LETTERS_LIST[rng.nextInt(LETTERS_LIST.size)] }
        }
        VocabType.POKEMON, VocabType.SPRITES -> {
            val choices = imageChoices(vocabType)
            for (i in 0 until n) {
                val idx = rng.nextInt(choices.size)
                var pick = choices[idx]
                var guard = 0
                while (pick in words && guard < 500) {
                    val next = rng.nextInt(choices.size)
                    pick = choices[next]
                    guard++
                }
                words += pick
                ids += (choices.indexOf(pick) + 1)
            }
        }
    }
    return Phrase(words = words, ids = ids)
}

fun scoreVertical(inputs: List<String>, phrase: Phrase): Int {
    if (phrase.words.isEmpty()) return 0
    var correct = 0
    phrase.words.forEachIndexed { i, word ->
        val typed = inputs.getOrNull(i)?.trim()?.lowercase() ?: ""
        if (typed == word.lowercase()) correct++
    }
    return Math.round(correct.toFloat() / phrase.words.size * 100f)
}

fun scorePokemon(selections: List<Int?>, phrase: Phrase): Int {
    if (phrase.words.isEmpty()) return 0
    var correct = 0
    phrase.words.forEachIndexed { i, _ ->
        if (selections.getOrNull(i) != null && selections[i] == phrase.ids.getOrNull(i)) correct++
    }
    return Math.round(correct.toFloat() / phrase.words.size * 100f)
}

@StringRes
fun resultVerdict(score: Int): Int = when {
    score == 100 -> R.string.stm_verdict_legend
    score >= 60 -> R.string.stm_verdict_great
    else -> R.string.stm_verdict_overflow
}

fun buildTaskQueue(count: Int, rng: Random = Random): List<Int> =
    List(count.coerceAtLeast(0)) { rng.nextInt(TASK_KIND_COUNT) }

const val TASK_KIND_COUNT = 22

data class HistoryEntry(
    val id: String,
    val vocabType: String,
    val wordCount: Int,
    val mode: String,
    val configValue: Int,
    val score: Int,
    val timestamp: Long,
)

fun newHistoryId(rng: Random = Random): String =
    (1..9).joinToString("") { ALPHA[rng.nextInt(ALPHA.length)].toString() }

private const val ALPHA = "abcdefghijklmnopqrstuvwxyz0123456789"

fun prependHistory(entry: HistoryEntry, existing: List<HistoryEntry>, cap: Int = 50): List<HistoryEntry> =
    (listOf(entry) + existing).take(cap)

fun historyConfigValue(mode: RunMode, settings: ShortTermSettings): Int =
    when (mode) {
        RunMode.TASKS -> settings.difficulty.tasks
        RunMode.TIME, RunMode.ZEN -> settings.effectiveZenDuration
    }

fun pokemonSpriteUrl(id: Int): String =
    "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/$id.png"

data class ShortTermState(
    val stage: ShortTermStage = ShortTermStage.Idle,
    val settings: ShortTermSettings = ShortTermSettings(),
    val phrase: Phrase? = null,
    val taskQueue: List<Int> = emptyList(),
    val currentTaskIndex: Int = 0,
    val taskTimerSec: Float = 5.0f,
    val memorizeElapsedMs: Long = 0L,
    val distractElapsedSec: Float = 0f,
    val finalScore: Int = 0,
    @StringRes val failReasonRes: Int? = null,
    val stackStep: Int = 1,
    val history: List<HistoryEntry> = emptyList(),
) {
    val isImpossibleRun: Boolean get() = settings.isImpossible

    val memorizeFraction: Float
        get() {
            val total = settings.effectiveMemorizeDuration
            if (total <= 0f) return 0f
            val left = total - memorizeElapsedMs / 1000f
            return (left / total).coerceIn(0f, 1f)
        }

    val memorizeSecondsLeft: Float
        get() = (settings.effectiveMemorizeDuration - memorizeElapsedMs / 1000f).coerceAtLeast(0f)

    val taskTimerFraction: Float
        get() {
            val total = settings.effectiveTimePerTask
            if (total <= 0f) return 0f
            return (taskTimerSec / total).coerceIn(0f, 1f)
        }

    val timeLeftSec: Int
        get() = (settings.effectiveZenDuration - distractElapsedSec).toInt().coerceAtLeast(0)

    val currentTaskKind: Int
        get() = if (taskQueue.isEmpty()) 0 else taskQueue[currentTaskIndex % taskQueue.size]

    val hasMoreTasks: Boolean get() = currentTaskIndex + 1 < taskQueue.size

    val revealIndex: Int?
        get() = if (isImpossibleRun && stage == ShortTermStage.Memorize) stackStep - 1 else null
}
