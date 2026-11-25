package com.jaydorah.alzhimers.games.wordhunt

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import com.jaydorah.alzhimers.R
import java.util.Locale
import kotlin.math.abs
import kotlin.random.Random

enum class Direction(val dr: Int, val dc: Int) {
    Right(0, 1),
    Down(1, 0),
    DownRight(1, 1),
    UpRight(-1, 1),
}

enum class Difficulty(
    @StringRes val labelRes: Int,
    val gridSize: Int,
    val wordCount: Int,
    val minWordLen: Int,
    val maxWordLen: Int,
    val directions: List<Direction>,
    /** Plurals keyed on [wordCount]; format args are (gridSize, wordCount). */
    @PluralsRes val descriptionRes: Int,
) {
    Easy(
        labelRes = R.string.wh_diff_easy,
        gridSize = 8,
        wordCount = 5,
        minWordLen = 3,
        maxWordLen = 4,
        directions = listOf(Direction.Right, Direction.Down),
        descriptionRes = R.plurals.wh_desc_easy_n,
    ),
    Medium(
        labelRes = R.string.wh_diff_normal,
        gridSize = 10,
        wordCount = 7,
        minWordLen = 4,
        maxWordLen = 6,
        directions = listOf(
            Direction.Right,
            Direction.Down,
            Direction.DownRight,
            Direction.UpRight,
        ),
        descriptionRes = R.plurals.wh_desc_normal_n,
    ),
    Hard(
        labelRes = R.string.wh_diff_hard,
        gridSize = 12,
        wordCount = 9,
        minWordLen = 5,
        maxWordLen = 8,
        directions = listOf(
            Direction.Right,
            Direction.Down,
            Direction.DownRight,
            Direction.UpRight,
        ),
        descriptionRes = R.plurals.wh_desc_hard_n,
    ),
}

data class Cell(val row: Int, val col: Int)

data class Placement(val word: String, val cells: List<Cell>)

data class GameSettings(
    val difficulty: Difficulty = Difficulty.Easy,
    val allowPause: Boolean = true,
)

data class Round(
    val size: Int,
    val grid: List<List<Char>>,
    val placements: List<Placement>,
) {
    val words: List<String> get() = placements.map { it.word }

    fun cellsFor(word: String): List<Cell>? =
        placements.firstOrNull { it.word == word }?.cells
}

enum class WordHuntPhase { SelectDifficulty, Playing, Paused, Result, Revealed }

data class WordHuntState(
    val phase: WordHuntPhase = WordHuntPhase.SelectDifficulty,
    val settings: GameSettings = GameSettings(),
    val round: Round? = null,
    val foundWords: Set<String> = emptySet(),
    val selection: List<Cell> = emptyList(),
    val elapsedMs: Long = 0L,
    val lastMissAtMs: Long = -10_000L,
    /** Cells of the last selection that matched no word, flashed briefly as feedback. */
    val missCells: List<Cell> = emptyList(),
    val hintCell: Cell? = null,
    val hintsUsed: Int = 0,
) {
    val allFound: Boolean
        get() = round != null && foundWords.size == round.placements.size

    val timeLabel: String
        get() {
            val totalSec = elapsedMs / 1000
            val m = totalSec / 60
            val s = totalSec % 60
            return "%d:%02d".format(Locale.ROOT, m, s)
        }
}

fun generateRound(words: List<String>, settings: GameSettings, rng: Random = Random): Round {
    val d = settings.difficulty
    val size = d.gridSize
    val grid = MutableList(size) { MutableList(size) { ' ' } }
    val candidates = words
        .map { it.lowercase() }
        .filter { w ->
            w.length in d.minWordLen..d.maxWordLen && w.all { it in 'a'..'z' }
        }
        .shuffled(rng)
        .distinct()

    val placements = mutableListOf<Placement>()
    // Hard allows reverse direction (right-to-left / bottom-up).
    val maxReverse = if (d == Difficulty.Hard) 1 else 0

    for (word in candidates) {
        if (placements.size >= d.wordCount) break
        val placement = placeWord(grid, word, d.directions, maxReverse, rng) ?: continue
        placements += placement
    }

    for (r in 0 until size) {
        for (c in 0 until size) {
            if (grid[r][c] == ' ') {
                grid[r][c] = ('a'..'z').random(rng)
            }
        }
    }

    return Round(size = size, grid = grid.map { it.toList() }, placements = placements)
}

private fun placeWord(
    grid: MutableList<MutableList<Char>>,
    word: String,
    directions: List<Direction>,
    maxReverse: Int,
    rng: Random,
): Placement? {
    val size = grid.size
    repeat(ATTEMPTS_PER_WORD) {
        val dir = directions.random(rng)
        val reverse = rng.nextInt(0, maxReverse + 1) == 1
        val len = word.length
        val dr = if (reverse) -dir.dr else dir.dr
        val dc = if (reverse) -dir.dc else dir.dc

        val maxRow = size - 1
        val maxCol = size - 1
        val startOptions = mutableListOf<Cell>()
        for (r in 0..maxRow) {
            for (c in 0..maxCol) {
                val endR = r + dr * (len - 1)
                val endC = c + dc * (len - 1)
                if (endR !in 0 until size || endC !in 0 until size) continue
                val cells = pathCells(r, c, dr, dc, len)
                if (fits(grid, cells, word)) {
                    startOptions += Cell(r, c)
                }
            }
        }
        if (startOptions.isEmpty()) return@repeat
        val start = startOptions.random(rng)
        val cells = pathCells(start.row, start.col, dr, dc, len)
        cells.forEachIndexed { i, cell -> grid[cell.row][cell.col] = word[i] }
        return Placement(word, cells)
    }
    return null
}

private fun fits(grid: MutableList<MutableList<Char>>, cells: List<Cell>, word: String): Boolean {
    cells.forEachIndexed { i, cell ->
        val existing = grid[cell.row][cell.col]
        if (existing != ' ' && existing != word[i]) return false
    }
    return true
}

private fun pathCells(row: Int, col: Int, dr: Int, dc: Int, len: Int): List<Cell> =
    List(len) { i -> Cell(row + dr * i, col + dc * i) }

/** Straight line from start to end, or null if not aligned. */
fun cellsAlong(start: Cell, end: Cell): List<Cell>? {
    val dRow = end.row - start.row
    val dCol = end.col - start.col
    if (dRow == 0 && dCol == 0) return listOf(start)
    val aligned = dRow == 0 || dCol == 0 || abs(dRow) == abs(dCol)
    if (!aligned) return null
    val steps = maxOf(abs(dRow), abs(dCol))
    val stepR = if (dRow == 0) 0 else dRow / steps
    val stepC = if (dCol == 0) 0 else dCol / steps
    return List(steps + 1) { i -> Cell(start.row + stepR * i, start.col + stepC * i) }
}

fun lettersAt(grid: List<List<Char>>, cells: List<Cell>): String =
    buildString { cells.forEach { append(grid[it.row][it.col]) } }

/** Returns the found word if the selection matches one not yet found. */
fun matchSelection(
    grid: List<List<Char>>,
    cells: List<Cell>,
    words: List<String>,
    found: Set<String>,
): String? {
    if (cells.size < 2) return null
    val text = lettersAt(grid, cells)
    val reversed = text.reversed()
    return words.firstOrNull { w -> w !in found && (w == text || w == reversed) }
}

private const val ATTEMPTS_PER_WORD = 80
