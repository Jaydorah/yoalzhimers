package com.jaydorah.alzhimers.games.numbermemory

import kotlin.random.Random

enum class NumberMemoryPhase { Options, Showing, Answer, Result }

data class GameSettings(
    val startingDigits: Int = DEFAULT_STARTING_DIGITS,
) {
    val effectiveStartingDigits: Int
        get() = startingDigits.coerceIn(MIN_DIGITS, MAX_DIGITS)

    companion object {
        const val DEFAULT_STARTING_DIGITS = 3
        const val MIN_DIGITS = 1
        const val MAX_DIGITS = 8
    }
}

data class Round(val number: String) {
    val digits: Int get() = number.length
}

data class NumberMemoryState(
    val phase: NumberMemoryPhase = NumberMemoryPhase.Options,
    val settings: GameSettings = GameSettings(),
    val round: Round? = null,
    val levelDigits: Int = GameSettings.DEFAULT_STARTING_DIGITS,
    val showElapsedMs: Long = 0L,
    val input: String = "",
    val wasCorrect: Boolean = false,
    val bestDigits: Int = 0,
) {
    val showDurationMs: Long get() = showDurationFor(levelDigits)

    val showFraction: Float
        get() = if (showDurationMs <= 0L) 0f
        else (1f - showElapsedMs.toFloat() / showDurationMs).coerceIn(0f, 1f)

    val secondsLeft: Int
        get() = ((showDurationMs - showElapsedMs + 999) / 1000).toInt().coerceAtLeast(0)
}

/** Longest number a round can show; matches the answer input limit. */
const val MAX_ROUND_DIGITS = 12

fun nextLevelDigits(current: Int): Int = (current + 1).coerceAtMost(MAX_ROUND_DIGITS)

fun showDurationFor(digits: Int): Long = 2_500L + 400L * digits.coerceIn(1, MAX_ROUND_DIGITS)

fun randomDigits(digits: Int, rng: Random = Random): String = buildString {
    repeat(digits.coerceIn(1, MAX_ROUND_DIGITS)) { append(rng.nextInt(0, 10)) }
}
