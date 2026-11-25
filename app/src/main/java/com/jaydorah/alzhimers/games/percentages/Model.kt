package com.jaydorah.alzhimers.games.percentages

import androidx.annotation.StringRes
import com.jaydorah.alzhimers.R
import kotlin.math.abs
import kotlin.random.Random

/** Questions per run. */
const val QUESTION_COUNT = 10

const val MIN_NUMBER = 1
const val MAX_NUMBER = 10_000
const val MIN_SECONDS = 5
const val MAX_SECONDS = 60

enum class Difficulty(
    @StringRes val labelRes: Int,
    val secondsPerQuestion: Int,
) {
    Easy(R.string.pct_diff_easy, 0),
    Normal(R.string.pct_diff_normal, 30),
    Hard(R.string.pct_diff_hard, 10),
    Custom(R.string.pct_diff_custom, 0),
}

data class GameSettings(
    val difficulty: Difficulty = Difficulty.Easy,
    val customSeconds: Int = 15,
    val minNumber: Int = MIN_NUMBER,
    val maxNumber: Int = MAX_NUMBER,
    val roundNumbers: Boolean = false,
) {
    val effectiveSeconds: Int
        get() = if (difficulty == Difficulty.Custom) {
            customSeconds.coerceIn(MIN_SECONDS, MAX_SECONDS)
        } else {
            difficulty.secondsPerQuestion
        }

    /** Easy has no timer at all. */
    val timed: Boolean get() = effectiveSeconds > 0
}

fun GameSettings.withMinNumber(value: Int): GameSettings {
    val min = value.coerceIn(MIN_NUMBER, MAX_NUMBER)
    return copy(minNumber = min, maxNumber = maxOf(maxNumber, min))
}

fun GameSettings.withMaxNumber(value: Int): GameSettings {
    val max = value.coerceIn(MIN_NUMBER, MAX_NUMBER)
    return copy(maxNumber = max, minNumber = minOf(minNumber, max))
}

data class Question(val percent: Int, val number: Int) {
    val answer: Int get() = number * percent / 100
}

enum class GamePhase { SelectDifficulty, Question, Feedback, Result }

data class PercentagesState(
    val phase: GamePhase = GamePhase.SelectDifficulty,
    val settings: GameSettings = GameSettings(),
    val questions: List<Question> = emptyList(),
    val index: Int = 0,
    val remainingMs: Long = 0L,
    val input: String = "",
    val wasCorrect: Boolean = false,
    val timedOut: Boolean = false,
    val correctCount: Int = 0,
    val streak: Int = 0,
    val bestStreak: Int = 0,
) {
    val timeLimitMs: Long get() = settings.effectiveSeconds * 1000L

    /** Remaining share of the question, 1f when full and 0f when it runs out. */
    val timeFraction: Float
        get() = if (timeLimitMs <= 0L) {
            0f
        } else {
            (remainingMs.toFloat() / timeLimitMs).coerceIn(0f, 1f)
        }

    val secondsLeft: Int get() = ((remainingMs + 999) / 1000).toInt()

    val current: Question? get() = questions.getOrNull(index)

    val total: Int get() = questions.size
}

/**
 * Builds a run of questions. The number is always a multiple of
 * 100 / gcd(percent, 100), so every answer is a whole number. With
 * "round numbers" on, the number is additionally a multiple of 5.
 */
fun generateQuestions(
    settings: GameSettings,
    count: Int = QUESTION_COUNT,
    rng: Random = Random.Default,
): List<Question> {
    val low = settings.minNumber.coerceAtLeast(MIN_NUMBER)
    val high = maxOf(settings.maxNumber, low)
    val grain = if (settings.roundNumbers) 5 else 1

    val questions = mutableListOf<Question>()
    val seen = mutableSetOf<Question>()

    repeat(count * 40) {
        if (questions.size >= count) return@repeat
        val percent = rng.nextInt(1, 101)
        val unit = lcm(100 / gcd(percent, 100), grain)
        val first = ceilDiv(low, unit)
        val last = high / unit
        if (last < first) return@repeat
        val number = unit * rng.nextInt(first, last + 1)
        if (number * percent < 100) return@repeat
        val question = Question(percent, number)
        if (seen.add(question)) questions += question
    }

    // Ranges too tight for varied percents: fall back to "n% out of n".
    var guard = count * 40
    while (questions.size < count && guard-- > 0) {
        val number = rng.nextInt(low, high + 1)
        val question = Question(100, number)
        if (seen.add(question)) questions += question
    }

    return questions
}

private fun ceilDiv(a: Int, b: Int): Int = (a + b - 1) / b

private fun gcd(a: Int, b: Int): Int {
    var x = abs(a)
    var y = abs(b)
    while (y != 0) {
        val t = y
        y = x % y
        x = t
    }
    return x
}

private fun lcm(a: Int, b: Int): Int = a / gcd(a, b) * b
