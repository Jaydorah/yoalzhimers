package com.jaydorah.alzhimers.games.lightsout

import androidx.annotation.StringRes
import com.jaydorah.alzhimers.R
import java.util.Locale
import kotlin.random.Random

enum class LightsOutDifficulty(
    @StringRes val labelRes: Int,
    val gridSize: Int,
    val scrambleTaps: Int,
    @StringRes val descriptionRes: Int,
) {
    Easy(
        labelRes = R.string.lo_diff_easy,
        gridSize = 3,
        scrambleTaps = 4,
        descriptionRes = R.string.lo_desc_easy,
    ),
    Normal(
        labelRes = R.string.lo_diff_normal,
        gridSize = 5,
        scrambleTaps = 10,
        descriptionRes = R.string.lo_desc_normal,
    ),
    Hard(
        labelRes = R.string.lo_diff_hard,
        gridSize = 7,
        scrambleTaps = 20,
        descriptionRes = R.string.lo_desc_hard,
    ),
}

data class GameSettings(
    val difficulty: LightsOutDifficulty = LightsOutDifficulty.Easy,
)

enum class LightsOutPhase { Options, Playing, Paused, Result }

data class LightsOutState(
    val phase: LightsOutPhase = LightsOutPhase.Options,
    val settings: GameSettings = GameSettings(),
    val board: List<Boolean> = emptyList(),
    val moves: Int = 0,
    val elapsedMs: Long = 0L,
) {
    val gridSize: Int get() = settings.difficulty.gridSize

    val allOut: Boolean get() = board.isNotEmpty() && board.none { it }

    val litCount: Int get() = board.count { it }

    val timeLabel: String
        get() {
            val totalSec = elapsedMs / 1000
            val m = totalSec / 60
            val s = totalSec % 60
            return "%d:%02d".format(Locale.ROOT, m, s)
        }
}

fun isSolved(board: List<Boolean>): Boolean = board.isNotEmpty() && board.none { it }

/** Toggles the cell at (row, col) plus its orthogonal neighbors. */
fun toggleAt(board: List<Boolean>, size: Int, row: Int, col: Int): List<Boolean> {
    val next = board.toMutableList()
    fun toggle(r: Int, c: Int) {
        if (r in 0 until size && c in 0 until size) {
            val i = r * size + c
            next[i] = !next[i]
        }
    }
    toggle(row, col)
    toggle(row - 1, col)
    toggle(row + 1, col)
    toggle(row, col - 1)
    toggle(row, col + 1)
    return next
}

/**
 * Builds a board by applying random taps to an all-out board,
 * so every generated puzzle is guaranteed solvable.
 */
fun generateBoard(settings: GameSettings, rng: Random = Random.Default): List<Boolean> {
    val d = settings.difficulty
    val size = d.gridSize
    var board = List(size * size) { false }
    val taps = (0 until size * size).shuffled(rng).take(d.scrambleTaps)
    for (cell in taps) {
        board = toggleAt(board, size, cell / size, cell % size)
    }
    if (isSolved(board)) {
        board = toggleAt(board, size, 0, 0)
    }
    return board
}
