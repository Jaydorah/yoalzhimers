package com.jaydorah.alzhimers.games.numbermemory

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class NumberMemoryModelTest {

    @Test
    fun defaultSettingsStartAtThreeDigits() {
        val settings = GameSettings()
        assertEquals(3, settings.effectiveStartingDigits)
    }

    @Test
    fun startingDigitsAreClampedToValidRange() {
        assertEquals(1, GameSettings(startingDigits = 0).effectiveStartingDigits)
        assertEquals(8, GameSettings(startingDigits = 99).effectiveStartingDigits)
        assertEquals(5, GameSettings(startingDigits = 5).effectiveStartingDigits)
    }

    @Test
    fun showDurationGrowsWithDigitCount() {
        assertTrue(showDurationFor(5) > showDurationFor(3))
        assertEquals(2_500L + 400L * 4, showDurationFor(4))
    }

    @Test
    fun randomDigitsHasRequestedLengthAndOnlyDigits() {
        val digits = randomDigits(6, Random(1))
        assertEquals(6, digits.length)
        assertTrue(digits.all { it in '0'..'9' })
    }

    @Test
    fun roundExposesDigitCount() {
        assertEquals(4, Round("1234").digits)
    }

    @Test
    fun defaultStateStartsOnOptionsScreen() {
        val state = NumberMemoryState()
        assertEquals(NumberMemoryPhase.Options, state.phase)
        assertEquals(0, state.bestDigits)
        assertEquals("", state.input)
    }

    @Test
    fun showFractionDecreasesAsTimePasses() {
        val full = NumberMemoryState(levelDigits = 3, showElapsedMs = 0L)
        val half = NumberMemoryState(levelDigits = 3, showElapsedMs = full.showDurationMs / 2)
        val empty = NumberMemoryState(
            levelDigits = 3,
            showElapsedMs = full.showDurationMs,
        )
        assertEquals(1f, full.showFraction, 0.01f)
        assertEquals(0.5f, half.showFraction, 0.05f)
        assertEquals(0f, empty.showFraction, 0.01f)
    }

    @Test
    fun secondsLeftCountsDownFromDuration() {
        val start = NumberMemoryState(levelDigits = 3, showElapsedMs = 0L)
        val end = NumberMemoryState(
            levelDigits = 3,
            showElapsedMs = start.showDurationMs - 1,
        )
        assertEquals((start.showDurationMs + 999) / 1000, start.secondsLeft.toLong())
        assertEquals(1, end.secondsLeft)
    }

    @Test
    fun nextLevelStopsAtLongestShowableNumber() {
        assertEquals(4, nextLevelDigits(3))
        assertEquals(MAX_ROUND_DIGITS, nextLevelDigits(MAX_ROUND_DIGITS - 1))
        assertEquals(MAX_ROUND_DIGITS, nextLevelDigits(MAX_ROUND_DIGITS))
        assertEquals(MAX_ROUND_DIGITS, randomDigits(nextLevelDigits(MAX_ROUND_DIGITS)).length)
    }
}
