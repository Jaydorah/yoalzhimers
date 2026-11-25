package com.jaydorah.alzhimers.games.chimp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ChimpModelTest {

    @Test
    fun startingNumberIsCoercedToAllowedRange() {
        assertEquals(CHIMP_MIN_START, coerceStartingNumber(1))
        assertEquals(CHIMP_MAX_START, coerceStartingNumber(99))
        assertEquals(7, coerceStartingNumber(7))
    }

    @Test
    fun placeChimpNumbersFillsExactCount() {
        val grid = placeChimpNumbers(5, Random(1))
        assertEquals(CHIMP_GRID_CELLS, grid.size)
        assertEquals(5, grid.count { it != 0 })
        assertEquals((1..5).toSet(), grid.filter { it != 0 }.toSet())
    }

    @Test
    fun firstTapHidesRemainingNumbers() {
        val state = startChimpRound(
            ChimpState(startingNumber = 4, currentCount = 4),
            Random(2),
        )
        val cell = state.grid.indexOf(1)
        val next = state.copy(nextExpected = 2, numbersVisible = false)

        assertTrue(state.showNumberInCell(1))
        assertFalse(next.showNumberInCell(2))
        assertTrue(next.isCellFound(1))
        assertFalse(next.isCellFound(2))
        assertEquals(1, cell.let { state.grid[it] })
    }

    @Test
    fun completingRoundIncrementsCountAndClears() {
        var state = startChimpRound(
            ChimpState(startingNumber = 4, currentCount = 4),
            Random(3),
        )
        // tap all numbers in order
        for (n in 1..4) {
            val idx = state.grid.indexOf(n)
            val prev = state
            // simulate ViewModel tap path for correct cells
            if (prev.grid[idx] == prev.nextExpected) {
                val next = prev.nextExpected + 1
                state = if (next > prev.currentCount) {
                    prev.copy(
                        phase = ChimpPhase.RoundResult,
                        roundsCleared = prev.roundsCleared + 1,
                        currentCount = (prev.currentCount + 1).coerceAtMost(CHIMP_MAX_NUMBERS),
                        numbersVisible = true,
                        nextExpected = 1,
                    )
                } else {
                    prev.copy(nextExpected = next, numbersVisible = false)
                }
            }
        }
        assertEquals(ChimpPhase.RoundResult, state.phase)
        assertEquals(1, state.roundsCleared)
        assertEquals(5, state.currentCount)
    }

    @Test
    fun defaultStateStartsOnOptionsWithMinStartingNumber() {
        val state = ChimpState()
        assertEquals(ChimpPhase.Options, state.phase)
        assertEquals(CHIMP_MIN_START, state.startingNumber)
        assertEquals(0, state.score)
    }

    @Test
    fun easyNeverHasViewWindow() {
        assertNull(viewWindowDurationMs(ChimpDifficulty.Easy, 8))
        assertNull(viewWindowDurationMs(ChimpDifficulty.Easy, 12))
    }

    @Test
    fun normalHasSevenSecondWindowFromEightNumbersOnly() {
        assertNull(viewWindowDurationMs(ChimpDifficulty.Normal, 7))
        assertEquals(CHIMP_VIEW_WINDOW_MS, viewWindowDurationMs(ChimpDifficulty.Normal, 8))
        assertEquals(CHIMP_VIEW_WINDOW_MS, viewWindowDurationMs(ChimpDifficulty.Normal, 12))
    }

    @Test
    fun hardWindowDropsPerRoundAndStopsAtFiveSeconds() {
        assertNull(viewWindowDurationMs(ChimpDifficulty.Hard, 7))
        assertEquals(CHIMP_VIEW_WINDOW_MS, viewWindowDurationMs(ChimpDifficulty.Hard, 8))

        var window = CHIMP_VIEW_WINDOW_MS
        window = nextHardViewWindowMs(window)
        assertEquals(6_000L, window)
        window = nextHardViewWindowMs(window)
        assertEquals(CHIMP_VIEW_WINDOW_MIN_MS, window)
        window = nextHardViewWindowMs(window)
        assertEquals(CHIMP_VIEW_WINDOW_MIN_MS, window)
    }

    @Test
    fun startChimpRoundSetsViewWindowForTimedDifficulties() {
        val hard = startChimpRound(
            ChimpState(
                difficulty = ChimpDifficulty.Hard,
                startingNumber = 8,
                currentCount = 8,
            ),
            Random(4),
        )
        assertEquals(CHIMP_VIEW_WINDOW_MS, hard.viewWindowRemainingMs)
        assertTrue(hard.numbersVisible)

        val easy = startChimpRound(
            ChimpState(startingNumber = 8, currentCount = 8),
            Random(4),
        )
        assertNull(easy.viewWindowRemainingMs)
    }
}
