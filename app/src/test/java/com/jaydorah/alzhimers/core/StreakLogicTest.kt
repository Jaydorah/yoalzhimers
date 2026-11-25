package com.jaydorah.alzhimers.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** Day-boundary rules of [nextStreak], which AppPreferences.markPlayedToday uses. */
class StreakLogicTest {

    @Test
    fun firstPlayStartsStreakAtOne() {
        val today = LocalDate.of(2026, 9, 22)
        assertEquals(1, nextStreak(last = null, current = 0, today = today))
    }

    @Test
    fun consecutiveDaysIncrement() {
        val today = LocalDate.of(2026, 9, 22)
        assertEquals(3, nextStreak(last = today.minusDays(1), current = 2, today = today))
    }

    @Test
    fun sameDayDoesNotIncrement() {
        val today = LocalDate.of(2026, 9, 22)
        assertEquals(5, nextStreak(last = today, current = 5, today = today))
    }

    @Test
    fun missedDayResetsToOne() {
        val today = LocalDate.of(2026, 9, 22)
        assertEquals(1, nextStreak(last = today.minusDays(3), current = 10, today = today))
    }

    @Test
    fun scoreKeysCoverAllGames() {
        val keys = listOf(
            ScoreKeys.MentalMath,
            ScoreKeys.Copycat,
            ScoreKeys.WordHunt,
            ScoreKeys.NumberMemory,
            ScoreKeys.Chimp,
            ScoreKeys.LightsOut,
            ScoreKeys.ColorMatch,
            ScoreKeys.KimsSack,
            ScoreKeys.ShortTerm,
        )
        keys.forEach {
            assertTrue(ScoreKeys.higherIsBetter.containsKey(it))
        }
    }

    @Test
    fun lowerIsBetterGames() {
        assertEquals(false, ScoreKeys.higherIsBetter[ScoreKeys.WordHunt])
        assertEquals(false, ScoreKeys.higherIsBetter[ScoreKeys.LightsOut])
        assertEquals(true, ScoreKeys.higherIsBetter[ScoreKeys.NumberMemory])
        assertEquals(true, ScoreKeys.higherIsBetter[ScoreKeys.ShortTerm])
    }
}
