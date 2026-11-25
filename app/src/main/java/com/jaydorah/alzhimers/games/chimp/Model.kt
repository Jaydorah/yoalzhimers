package com.jaydorah.alzhimers.games.chimp

import androidx.annotation.StringRes
import com.jaydorah.alzhimers.R
import kotlin.random.Random

const val CHIMP_GRID_CELLS = 20
const val CHIMP_MIN_START = 4
const val CHIMP_MAX_START = 10
const val CHIMP_MAX_NUMBERS = 12
const val CHIMP_MAX_STRIKES = 3
const val CHIMP_TIMER_MIN_NUMBERS = 8
const val CHIMP_VIEW_WINDOW_MS = 7_000L
const val CHIMP_VIEW_WINDOW_MIN_MS = 5_000L

enum class ChimpPhase { Options, Playing, RoundResult, GameOver }

enum class ChimpDifficulty(
    @StringRes val labelRes: Int,
    @StringRes val descriptionRes: Int,
) {
    Easy(
        labelRes = R.string.chimp_diff_easy,
        descriptionRes = R.string.chimp_desc_easy,
    ),
    Normal(
        labelRes = R.string.chimp_diff_normal,
        descriptionRes = R.string.chimp_desc_normal,
    ),
    Hard(
        labelRes = R.string.chimp_diff_hard,
        descriptionRes = R.string.chimp_desc_hard,
    ),
}

data class ChimpState(
    val phase: ChimpPhase = ChimpPhase.Options,
    val difficulty: ChimpDifficulty = ChimpDifficulty.Easy,
    val startingNumber: Int = CHIMP_MIN_START,
    val currentCount: Int = CHIMP_MIN_START,
    val grid: List<Int> = emptyList(),
    val numbersVisible: Boolean = true,
    val nextExpected: Int = 1,
    val strikes: Int = 0,
    val roundsCleared: Int = 0,
    val hardViewWindowMs: Long = CHIMP_VIEW_WINDOW_MS,
    val viewWindowRemainingMs: Long? = null,
) {
    val score: Int get() = roundsCleared

    /** Remaining fraction of the current memorize window, 1 → 0. */
    val viewFraction: Float
        get() {
            val remaining = viewWindowRemainingMs ?: return 0f
            val total = viewWindowDurationMs(
                difficulty = difficulty,
                count = currentCount,
                hardViewWindowMs = hardViewWindowMs,
            ) ?: return 0f
            if (total <= 0L) return 0f
            return (remaining.toFloat() / total).coerceIn(0f, 1f)
        }

    fun isCellFound(number: Int): Boolean = number in 1 until nextExpected

    fun showNumberInCell(number: Int): Boolean =
        number != 0 && numbersVisible && !isCellFound(number)
}

fun coerceStartingNumber(value: Int): Int = value.coerceIn(CHIMP_MIN_START, CHIMP_MAX_START)

fun viewWindowDurationMs(
    difficulty: ChimpDifficulty,
    count: Int,
    hardViewWindowMs: Long = CHIMP_VIEW_WINDOW_MS,
): Long? = when {
    difficulty == ChimpDifficulty.Easy -> null
    count < CHIMP_TIMER_MIN_NUMBERS -> null
    difficulty == ChimpDifficulty.Normal -> CHIMP_VIEW_WINDOW_MS
    else -> hardViewWindowMs.coerceAtLeast(CHIMP_VIEW_WINDOW_MIN_MS)
}

fun nextHardViewWindowMs(currentMs: Long): Long =
    (currentMs - 1_000L).coerceAtLeast(CHIMP_VIEW_WINDOW_MIN_MS)

fun placeChimpNumbers(
    count: Int,
    random: Random = Random.Default,
    cells: Int = CHIMP_GRID_CELLS,
): List<Int> {
    val n = count.coerceIn(1, cells)
    val positions = (0 until cells).shuffled(random).take(n)
    val grid = MutableList(cells) { 0 }
    positions.forEachIndexed { i, pos -> grid[pos] = i + 1 }
    return grid
}

fun startChimpRound(state: ChimpState, random: Random = Random.Default): ChimpState =
    state.copy(
        phase = ChimpPhase.Playing,
        grid = placeChimpNumbers(state.currentCount, random),
        numbersVisible = true,
        nextExpected = 1,
        viewWindowRemainingMs = viewWindowDurationMs(
            difficulty = state.difficulty,
            count = state.currentCount,
            hardViewWindowMs = state.hardViewWindowMs,
        ),
    )
