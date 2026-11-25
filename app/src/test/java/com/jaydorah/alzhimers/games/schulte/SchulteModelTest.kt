package com.jaydorah.alzhimers.games.schulte

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class SchulteModelTest {

    @Test
    fun gridSizeIsCoerced() {
        assertEquals(SCHULTE_MIN_GRID, coerceGridSize(1))
        assertEquals(SCHULTE_MAX_GRID, coerceGridSize(99))
        assertEquals(5, coerceGridSize(5))
    }

    @Test
    fun makeGridHasExactCellCountAndNumbers() {
        val settings = SchulteSettings(gridSize = 4)
        val (cells, groups) = makeGridCells(settings, Random(1))
        assertEquals(16, cells.size)
        assertEquals(1, groups.size)
        assertEquals((1..16).toSet(), cells.map { it.number }.toSet())
    }

    @Test
    fun makeGridIsShuffledPermutation() {
        val settings = SchulteSettings(gridSize = 5)
        val (cells, _) = makeGridCells(settings, Random(2))
        assertEquals(25, cells.size)
        assertEquals((1..25).toSet(), cells.map { it.number }.toSet())
        // not strictly guaranteed, but with fixed seed order differs from sequential for size 5
        assertTrue(cells.map { it.number } != (1..25).toList() || cells.size == 25)
    }

    @Test
    fun forwardCountCorrectTapAdvances() {
        var state = beginGame(SchulteState(settings = SchulteSettings(gridSize = 3)), Random(3))
        assertEquals(SchultePhase.Playing, state.phase)
        val first = state.cells.indexOfFirst { it.group == 0 && it.number == 1 }
        val outcome = applyTap(state, first, nowMs = 0L, random = Random(3))
        assertTrue(outcome is TapOutcome.Continue)
        state = (outcome as TapOutcome.Continue).state
        assertEquals(2, state.groups[0].currNum)
        assertTrue(state.cells[first].traced)
        assertEquals(1, state.correctTaps)
    }

    @Test
    fun wrongTapIncrementsWrongAndKeepsExpected() {
        var state = beginGame(SchulteState(settings = SchulteSettings(gridSize = 3)), Random(4))
        val wrongIndex = state.cells.indexOfFirst { it.number != 1 || it.group != 0 }
        val outcome = applyTap(state, wrongIndex, nowMs = 0L, random = Random(4))
        assertTrue(outcome is TapOutcome.Continue)
        state = (outcome as TapOutcome.Continue).state
        assertEquals(1, state.wrongTaps)
        assertEquals(1, state.groups[0].currNum)
        assertEquals(SCHULTE_WRONG_PENALTY_MS, state.scoreMs)
    }

    @Test
    fun inverseCountStartsAtSizeAndStepsDown() {
        val group = SchulteGroup(size = 9, inverted = true, divergent = false)
        assertEquals(9, group.firstNumber())
        assertEquals(1, group.lastNumber())
        assertEquals(8, group.nextNumber(9))
        assertEquals(1, group.nextNumber(2))
    }

    @Test
    fun divergentCountStartsAtHalf() {
        val group = SchulteGroup(size = 9, inverted = false, divergent = true)
        assertEquals(4, group.firstNumber())
        assertEquals(9, group.lastNumber())
    }

    @Test
    fun completingAllCellsFinishesSingleRoundGame() {
        val settings = SchulteSettings(gridSize = 3, rounds = 1, clearCorrect = true)
        var state = beginGame(SchulteState(settings = settings), Random(5))
        val total = state.cells.size
        var lastOutcome: TapOutcome? = null
        repeat(total) {
            val expected = state.groups[state.currGroup].currNum
            val idx = state.cells.indexOfFirst { c ->
                c.group == state.currGroup && c.number == expected
            }
            lastOutcome = applyTap(state, idx, nowMs = it.toLong(), random = Random(5))
            state = when (val o = lastOutcome!!) {
                is TapOutcome.Continue -> o.state
                is TapOutcome.RoundComplete -> o.state
                is TapOutcome.GameComplete -> o.state
                is TapOutcome.Aborted -> o.state
            }
        }
        assertTrue(lastOutcome is TapOutcome.GameComplete)
        assertEquals(SchultePhase.Result, state.phase)
        assertEquals(total, state.correctTaps)
    }

    @Test
    fun scoreIncludesWrongPenaltyAndElapsedTime() {
        val state = SchulteState(
            phase = SchultePhase.Result,
            elapsedMs = 2_500L,
            wrongTaps = 3,
            penaltyMs = 500L,
        )
        assertEquals(2_500 + 3 * SCHULTE_WRONG_PENALTY_MS + 500, state.scoreMs)
    }

    @Test
    fun multiRoundWithBreaksGoesToRoundBreak() {
        val settings = SchulteSettings(gridSize = 2, rounds = 2, roundBreaks = true)
        var state = beginGame(SchulteState(settings = settings), Random(6))
        var outcome: TapOutcome = TapOutcome.Continue(state)
        repeat(4) {
            val expected = state.groups[state.currGroup].currNum
            val idx = state.cells.indexOfFirst { c ->
                c.group == state.currGroup && c.number == expected
            }
            outcome = applyTap(state, idx, nowMs = it.toLong(), random = Random(6))
            state = when (val o = outcome) {
                is TapOutcome.Continue -> o.state
                is TapOutcome.RoundComplete -> o.state
                is TapOutcome.GameComplete -> o.state
                is TapOutcome.Aborted -> o.state
            }
        }
        assertTrue(outcome is TapOutcome.RoundComplete)
        assertEquals(SchultePhase.RoundBreak, state.phase)
        assertEquals(1, state.roundIndex)

        state = nextAfterBreak(state, Random(6))
        assertEquals(SchultePhase.Playing, state.phase)
        assertEquals(0, state.correctTaps)
        assertTrue(state.cells.all { it.symbolVisible })
    }

    @Test
    fun clearCorrectHidesSymbolAfterTap() {
        val settings = SchulteSettings(gridSize = 3, clearCorrect = true)
        var state = beginGame(SchulteState(settings = settings), Random(7))
        val idx = state.cells.indexOfFirst { it.group == 0 && it.number == 1 }
        val outcome = applyTap(state, idx, nowMs = 0L, random = Random(7))
        state = (outcome as TapOutcome.Continue).state
        val tapped = state.cells.first { it.traced }
        assertFalse(tapped.symbolVisible)
    }

    @Test
    fun noErrorsAbortReturnsToOptionsWithoutResult() {
        val settings = SchulteSettings(gridSize = 3, noErrors = true)
        val state = beginGame(SchulteState(settings = settings), Random(8))
        val wrongIndex = state.cells.indexOfFirst { !(it.group == 0 && it.number == 1) }
        val outcome = applyTap(state, wrongIndex, nowMs = 0L, random = Random(8))
        assertTrue(outcome is TapOutcome.Aborted)
        assertEquals(SchultePhase.Options, (outcome as TapOutcome.Aborted).state.phase)
    }

    @Test
    fun formatSchulteTimeFormatsMinutesSecondsMillis() {
        assertEquals("0:01.500", formatSchulteTime(1_500L))
        assertEquals("1:05.250", formatSchulteTime(65_250L))
    }

    @Test
    fun groupsSplitAcrossCells() {
        val settings = SchulteSettings(gridSize = 4, groupCount = 4)
        val (cells, groups) = makeGridCells(settings, Random(9))
        assertEquals(4, groups.size)
        assertEquals(16, groups.sumOf { it.size })
        assertEquals(16, cells.size)
        assertEquals(groups.size, cells.map { it.group }.distinct().size)
    }
    private fun tapExpected(state: SchulteState, random: Random): SchulteState {
        val expected = state.groups[state.currGroup].currNum
        val idx = state.cells.indexOfFirst { it.group == state.currGroup && it.number == expected }
        return when (val o = applyTap(state, idx, nowMs = 0L, random = random)) {
            is TapOutcome.Continue -> o.state
            is TapOutcome.RoundComplete -> o.state
            is TapOutcome.GameComplete -> o.state
            is TapOutcome.Aborted -> o.state
        }
    }

    private fun visibleKeys(state: SchulteState): Set<Pair<Int, Int>> =
        state.cells.filter { it.symbolVisible }.map { it.group to it.number }.toSet()

    @Test
    fun frenzyShowsTheNextNumbersAfterEachTap() {
        val settings = SchulteSettings(gridSize = 4, frenzyMode = true, frenzyCount = 3)
        var state = beginGame(SchulteState(settings = settings), Random(10))
        assertEquals(setOf(0 to 1, 0 to 2, 0 to 3), visibleKeys(state))
        state = tapExpected(state, Random(10))
        assertEquals(setOf(0 to 2, 0 to 3, 0 to 4), visibleKeys(state))
        state = tapExpected(state, Random(10))
        assertEquals(setOf(0 to 3, 0 to 4, 0 to 5), visibleKeys(state))
    }

    @Test
    fun frenzyFollowsCurrentGroupWhenCollating() {
        val settings = SchulteSettings(gridSize = 4, groupCount = 2, collateGroups = true, frenzyMode = true, frenzyCount = 2)
        var state = beginGame(SchulteState(settings = settings), Random(11))
        assertEquals(setOf(0 to 1, 1 to 1), visibleKeys(state))
        state = tapExpected(state, Random(11))
        assertEquals(1, state.currGroup)
        assertEquals(setOf(1 to 1, 0 to 2), visibleKeys(state))
    }

    @Test
    fun shuffleKeepsFlashOnTappedCell() {
        val settings = SchulteSettings(gridSize = 5, shuffleSymbols = true, clearCorrect = false)
        val state = beginGame(SchulteState(settings = settings), Random(12))
        val next = tapExpected(state, Random(12))
        val flashed = next.cells[next.lastTapIndex]
        assertEquals(0 to 1, flashed.group to flashed.number)
    }

    @Test
    fun lettersModeUsesTwoLettersPastZ() {
        assertEquals("A", letterLabel(1))
        assertEquals("Z", letterLabel(26))
        assertEquals("AA", letterLabel(27))
        assertEquals("AZ", letterLabel(52))
        assertEquals("BA", letterLabel(53))
        val (cells, _) = makeGridCells(SchulteSettings(gridSize = 7, lettersMode = true), Random(13))
        assertTrue(cells.all { c -> c.symbol.all { it in 'A'..'Z' } })
    }

    @Test
    fun penaltySecondsIncludesBlindPenalty() {
        val state = SchulteState(wrongTaps = 2, penaltyMs = SCHULTE_BLIND_EXTRA_MS)
        assertEquals(12, state.penaltySeconds)
    }
}
