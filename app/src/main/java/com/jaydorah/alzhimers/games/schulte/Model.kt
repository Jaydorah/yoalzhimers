package com.jaydorah.alzhimers.games.schulte

import java.util.Locale
import kotlin.random.Random

const val SCHULTE_MIN_GRID = 2
const val SCHULTE_MAX_GRID = 9
const val SCHULTE_WRONG_PENALTY_MS = 1_000
const val SCHULTE_BLIND_EXTRA_MS = 10_000L
const val SCHULTE_FLASH_MS = 400L

enum class SchultePhase { Options, Playing, RoundBreak, Result }

enum class SpinSpeed(val durationMs: Long, val reverse: Boolean) {
    Off(0L, false),
    Low(4_000L, false),
    Medium(3_000L, false),
    High(2_000L, false),
    ReverseLow(4_000L, true),
    ReverseMedium(3_000L, true),
    ReverseHigh(2_000L, true),
    ;
}

data class SchulteSettings(
    val gridSize: Int = 5,
    val rounds: Int = 1,
    val roundBreaks: Boolean = true,
    val showRounds: Boolean = true,
    val groupCount: Int = 1,
    val colorGroups: Boolean = false,
    val inverseCount: Boolean = false,
    val divergentCount: Boolean = false,
    val variousCounts: Boolean = false,
    val collateGroups: Boolean = false,
    val originalColors: Boolean = false,
    val numberOffset: Int = 0,
    val clearCorrect: Boolean = true,
    val showTrace: Boolean = true,
    val showClickResult: Boolean = true,
    val showCenterDot: Boolean = false,
    val turnSymbols: Boolean = false,
    val spinSymbols: Boolean = false,
    val spinTable: Boolean = false,
    val spinTableSpeed: SpinSpeed = SpinSpeed.Low,
    val noErrors: Boolean = false,
    val startOnClick: Boolean = false,
    val shuffleSymbols: Boolean = false,
    val frenzyMode: Boolean = false,
    val frenzyCount: Int = 3,
    val hideReact: Boolean = false,
    val blindMode: Boolean = false,
    val mathMode: Boolean = false,
    val lettersMode: Boolean = false,
)

data class SchulteCell(
    val number: Int,
    val symbol: String,
    val group: Int,
    val traced: Boolean = false,
    val symbolVisible: Boolean = true,
    val rotationDegrees: Float = 0f,
)

data class SchulteGroup(
    val size: Int,
    val inverted: Boolean,
    val divergent: Boolean,
    var currNum: Int = 1,
)

fun SchulteGroup.firstNumber(): Int = when {
    inverted && !divergent -> size
    divergent && !inverted -> size / 2
    else -> 1
}

fun SchulteGroup.lastNumber(): Int = when {
    !inverted -> size
    divergent -> size / 2 + 1
    else -> 1
}

fun SchulteGroup.nextNumber(currNum: Int = this.currNum): Int = when {
    !divergent && !inverted -> currNum + 1
    !divergent -> currNum - 1
    else -> {
        val h = size / 2
        if (inverted) {
            if (currNum <= h) size - currNum + 1 else 2 + (size - currNum)
        } else {
            val evenSize = 2 * h
            when {
                currNum == evenSize -> evenSize + 1
                currNum <= h -> evenSize - currNum + 1
                else -> evenSize - currNum
            }
        }
    }
}

fun coerceGridSize(value: Int): Int = value.coerceIn(SCHULTE_MIN_GRID, SCHULTE_MAX_GRID)

fun formatSchulteTime(ms: Long): String {
    val total = ms.coerceAtLeast(0L)
    val minutes = total / 60_000
    val seconds = (total / 1000) % 60
    val millis = total % 1000
    return "%d:%02d.%03d".format(Locale.ROOT, minutes, seconds, millis)
}

/** 1 -> "A", 26 -> "Z", 27 -> "AA", like spreadsheet columns, so grids above 26 cells stay letters. */
fun letterLabel(number: Int): String {
    var n = number
    val sb = StringBuilder()
    while (n > 0) {
        val rem = (n - 1) % 26
        sb.append('A' + rem)
        n = (n - 1) / 26
    }
    return sb.reverse().toString()
}

fun buildGroups(settings: SchulteSettings): List<SchulteGroup> {
    val cellCount = settings.gridSize * settings.gridSize
    val groupCount = settings.groupCount.coerceIn(1, cellCount)
    val base = cellCount / groupCount
    val remainder = cellCount % groupCount
    return List(groupCount) { g ->
        val size = base + if (g < remainder) 1 else 0
        val inverted: Boolean
        val divergent: Boolean
        if (settings.variousCounts) {
            when (g % 4) {
                0 -> {
                    inverted = false
                    divergent = false
                }
                1 -> {
                    inverted = true
                    divergent = false
                }
                2 -> {
                    inverted = false
                    divergent = true
                }
                else -> {
                    inverted = true
                    divergent = true
                }
            }
        } else {
            inverted = settings.inverseCount
            divergent = settings.divergentCount
        }
        SchulteGroup(size = size, inverted = inverted, divergent = divergent).also {
            it.currNum = it.firstNumber()
        }
    }
}

fun mathSymbols(count: Int, random: Random): List<String> {
    val pairs = mutableListOf<Pair<Double, String>>()
    pairs.add(0.0 to "0")
    val integerMax = count / 2
    for (i in 1..integerMax) {
        pairs.add(i.toDouble() to i.toString())
        pairs.add((-i).toDouble() to "-$i")
    }
    val fractionMax = maxOf(9, 2 * 3)
    for (i in 2..fractionMax) {
        pairs.add(1.0 / i to "1/$i")
        pairs.add(-1.0 / i to "-1/$i")
        if (i > 2) {
            pairs.add(1.0 - 1.0 / i to "${i - 1}/$i")
            pairs.add(-1.0 + 1.0 / i to "-${i - 1}/$i")
        }
    }
    for (i in 3..integerMax step 2) {
        pairs.add(i / 2.0 to "$i/2")
        pairs.add(-i / 2.0 to "-$i/2")
    }
    if (count >= 25) {
        pairs.add(2.71828 to "e")
        pairs.add(-2.71828 to "-e")
        pairs.add(3.14159 to "π")
        pairs.add(-3.14159 to "-π")
        pairs.add(6.28318 to "2π")
        pairs.add(-6.28318 to "-2π")
    }
    return pairs
        .shuffled(random)
        .sortedBy { it.first }
        .take(count)
        .map { it.second }
}

fun makeGridCells(settings: SchulteSettings, random: Random = Random.Default): Pair<List<SchulteCell>, List<SchulteGroup>> {
    val groups = buildGroups(settings)
    val cells = mutableListOf<SchulteCell>()
    groups.forEachIndexed { g, group ->
        for (n in 1..group.size) {
            cells += SchulteCell(
                number = n,
                symbol = (n + settings.numberOffset).toString(),
                group = g,
            )
        }
    }
    val math = if (settings.mathMode) mathSymbols(cells.size, random) else null
    val finalized = cells.mapIndexed { i, cell ->
        val symbol = when {
            math != null -> math[i]
            settings.lettersMode -> letterLabel(cell.number)
            else -> cell.symbol
        }
        cell.copy(
            symbol = symbol,
            rotationDegrees = randomRotation(settings, random),
            symbolVisible = true,
        )
    }.shuffled(random)

    val withFrenzy = if (settings.frenzyMode) {
        applyFrenzyVisibility(finalized, groups, settings)
    } else {
        finalized
    }
    return withFrenzy to groups
}

private fun randomRotation(settings: SchulteSettings, random: Random): Float = when {
    settings.turnSymbols -> listOf(0f, 90f, 180f, 270f)[random.nextInt(4)]
    settings.spinSymbols -> if (random.nextBoolean()) 0f else 180f
    else -> 0f
}

fun goalSequence(
    groups: List<SchulteGroup>,
    cellCount: Int,
    collateGroups: Boolean,
    startGroup: Int = 0,
): List<Pair<Int, Int>> {
    val goals = mutableListOf<Pair<Int, Int>>()
    if (groups.isEmpty() || cellCount <= 0) return goals
    val nums = groups.map { it.currNum }.toMutableList()
    var g = startGroup.coerceIn(0, groups.size - 1)
    goals += g to nums[g]
    repeat(cellCount - 1) {
        val group = groups[g]
        val isLast = group.lastNumber() == nums[g]
        nums[g] = group.nextNumber(nums[g])
        if (isLast || collateGroups) g = (g + 1) % groups.size
        goals += g to nums[g]
    }
    return goals
}

fun applyFrenzyVisibility(
    cells: List<SchulteCell>,
    groups: List<SchulteGroup>,
    settings: SchulteSettings,
    currGroup: Int = 0,
): List<SchulteCell> {
    // groups already hold the current position of every group, so the upcoming goals start at index 0.
    val goals = goalSequence(groups, cells.size, settings.collateGroups, currGroup)
    val visible = goals.take(settings.frenzyCount).toSet()
    val showAllWhenHidden = settings.frenzyCount == 1 && settings.hideReact
    return cells.map { cell ->
        val key = cell.group to cell.number
        val show = key in visible && !showAllWhenHidden
        cell.copy(symbolVisible = show)
    }
}

fun applyBlindHide(cells: List<SchulteCell>): List<SchulteCell> =
    cells.map { it.copy(symbolVisible = false) }

fun applyBlindReveal(cells: List<SchulteCell>, index: Int): List<SchulteCell> =
    cells.mapIndexed { i, c -> if (i == index) c.copy(symbolVisible = true) else c }

data class SchulteState(
    val phase: SchultePhase = SchultePhase.Options,
    val settings: SchulteSettings = SchulteSettings(),
    val cells: List<SchulteCell> = emptyList(),
    val groups: List<SchulteGroup> = emptyList(),
    val currGroup: Int = 0,
    val roundIndex: Int = 0,
    val elapsedMs: Long = 0L,
    val penaltyMs: Long = 0L,
    val wrongTaps: Int = 0,
    val correctTaps: Int = 0,
    val hasStartedTimer: Boolean = false,
    val lastTapIndex: Int = -1,
    val lastTapCorrect: Boolean = false,
    val flashUntilMs: Long = 0L,
    /** Best score after this game was recorded (0 until a game finishes). */
    val bestMs: Int = 0,
    /** True when the finished game beat (or set) the stored best. */
    val isNewBest: Boolean = false,
) {
    val settingsScoreMs: Int
        get() = (elapsedMs + wrongTaps * SCHULTE_WRONG_PENALTY_MS + penaltyMs).toInt()

    val scoreMs: Int get() = settingsScoreMs

    /** Whole seconds added on top of the clock: 1 s per wrong tap plus blind-mode penalties. */
    val penaltySeconds: Int get() = ((wrongTaps.toLong() * SCHULTE_WRONG_PENALTY_MS + penaltyMs) / 1000).toInt()

    val timeLabel: String get() = formatSchulteTime(elapsedMs)

    val scoreLabel: String get() = formatSchulteTime(scoreMs.toLong())

    val expectedSymbol: String?
        get() {
            val group = groups.getOrNull(currGroup) ?: return null
            val cell = cells.firstOrNull { it.group == currGroup && it.number == group.currNum }
            return cell?.symbol
        }

    fun isCorrect(index: Int): Boolean {
        val cell = cells.getOrNull(index) ?: return false
        val group = groups.getOrNull(currGroup) ?: return false
        return cell.group == currGroup && cell.number == group.currNum
    }

    fun cellColorRole(index: Int): SchulteCellRole {
        val cell = cells.getOrNull(index) ?: return SchulteCellRole.Normal
        if (index == lastTapIndex && settings.showClickResult) {
            return if (lastTapCorrect) SchulteCellRole.Correct else SchulteCellRole.Wrong
        }
        if (cell.traced && settings.showTrace) return SchulteCellRole.Traced
        return SchulteCellRole.Normal
    }
}

enum class SchulteCellRole { Normal, Traced, Correct, Wrong }

fun startRound(state: SchulteState, random: Random = Random.Default): SchulteState {
    val (cells, groups) = makeGridCells(state.settings, random)
    return state.copy(
        phase = SchultePhase.Playing,
        cells = cells,
        groups = groups,
        currGroup = 0,
        lastTapIndex = -1,
        lastTapCorrect = false,
        flashUntilMs = 0L,
        correctTaps = 0,
    )
}

fun beginGame(state: SchulteState, random: Random = Random.Default): SchulteState =
    startRound(
        state.copy(
            roundIndex = 0,
            elapsedMs = 0L,
            penaltyMs = 0L,
            wrongTaps = 0,
            correctTaps = 0,
            hasStartedTimer = !state.settings.startOnClick,
        ),
        random,
    )

sealed interface TapOutcome {
    data class Continue(val state: SchulteState) : TapOutcome
    data class RoundComplete(val state: SchulteState) : TapOutcome
    data class GameComplete(val state: SchulteState) : TapOutcome
    data class Aborted(val state: SchulteState) : TapOutcome
}

fun applyTap(
    state: SchulteState,
    index: Int,
    nowMs: Long,
    random: Random = Random.Default,
): TapOutcome {
    if (state.phase != SchultePhase.Playing || index !in state.cells.indices) {
        return TapOutcome.Continue(state)
    }
    val s = state

    val correct = s.isCorrect(index)
    val flashUntil = if (s.settings.showClickResult) nowMs + SCHULTE_FLASH_MS else 0L

    if (!correct) {
        if (s.settings.noErrors) {
            return TapOutcome.Aborted(
                s.copy(
                    phase = SchultePhase.Options,
                    lastTapIndex = index,
                    lastTapCorrect = false,
                    flashUntilMs = flashUntil,
                    cells = emptyList(),
                    groups = emptyList(),
                ),
            )
        }
        var extraPenalty = 0L
        var cells = s.cells
        if (s.settings.blindMode && s.correctTaps >= 1 && !s.cells[index].traced) {
            cells = applyBlindReveal(cells, index)
            extraPenalty = SCHULTE_BLIND_EXTRA_MS
        }
        return TapOutcome.Continue(
            s.copy(
                cells = cells,
                wrongTaps = s.wrongTaps + 1,
                penaltyMs = s.penaltyMs + extraPenalty,
                lastTapIndex = index,
                lastTapCorrect = false,
                flashUntilMs = flashUntil,
                hasStartedTimer = true,
            ),
        )
    }

    var cells = s.cells.toMutableList()
    val settings = s.settings
    val cell = cells[index]
    cells[index] = cell.copy(traced = true)
    if (settings.clearCorrect || settings.frenzyMode) {
        cells[index] = cells[index].copy(symbolVisible = false)
    }

    var correctTaps = s.correctTaps + 1
    var groups = s.groups.map { g -> g.copy(currNum = g.currNum) }.toMutableList()
    val group = groups[s.currGroup]
    val isLastOfGroup = group.lastNumber() == group.currNum
    var nextGroup = s.currGroup
    if (isLastOfGroup || settings.collateGroups) {
        groups[s.currGroup] = group.copy(currNum = group.nextNumber())
        nextGroup = (s.currGroup + 1) % groups.size
    } else {
        groups[s.currGroup] = group.copy(currNum = group.nextNumber())
    }

    if (settings.frenzyMode) {
        cells = applyFrenzyVisibility(cells, groups, settings, nextGroup).toMutableList()
    } else if (settings.blindMode && correctTaps == 1) {
        cells = applyBlindHide(cells).toMutableList()
    }

    var tappedIndex = index
    if (settings.shuffleSymbols) {
        cells = cells.shuffled(random).toMutableList()
        // Keep the tap flash on the tapped cell, wherever the shuffle moved it.
        tappedIndex = cells.indexOfFirst { it.group == cell.group && it.number == cell.number }
    }

    val roundDone = correctTaps >= cells.size
    val base = s.copy(
        cells = cells,
        groups = groups,
        currGroup = nextGroup,
        correctTaps = correctTaps,
        lastTapIndex = tappedIndex,
        lastTapCorrect = true,
        flashUntilMs = flashUntil,
        hasStartedTimer = true,
    )

    if (!roundDone) {
        return TapOutcome.Continue(base)
    }

    val roundsTotal = settings.rounds.coerceAtLeast(1)
    val completedRounds = s.roundIndex + 1
    return if (completedRounds >= roundsTotal) {
        TapOutcome.GameComplete(
            base.copy(
                phase = SchultePhase.Result,
                roundIndex = completedRounds,
            ),
        )
    } else if (settings.roundBreaks) {
        TapOutcome.RoundComplete(
            base.copy(
                phase = SchultePhase.RoundBreak,
                roundIndex = completedRounds,
                cells = base.cells.map { it.copy(symbolVisible = false) },
                lastTapIndex = -1,
                flashUntilMs = 0L,
            ),
        )
    } else {
        TapOutcome.RoundComplete(
            startRound(
                base.copy(
                    roundIndex = completedRounds,
                    lastTapIndex = -1,
                    flashUntilMs = 0L,
                ),
                random,
            ).let { next ->
                // startRound keeps Playing; mark as continue-with-new-table
                next
            },
        )
    }
}

fun nextAfterBreak(state: SchulteState, random: Random = Random.Default): SchulteState =
    startRound(state, random)
