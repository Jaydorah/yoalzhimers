package com.jaydorah.alzhimers.games.mentalmath

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class MentalMathModelTest {

    @Test
    fun roundComputesCorrectAnswer() {
        val round = Round(
            startNumber = 45,
            operations = listOf(
                Operation(OpType.Multiply, 3),
                Operation(OpType.Add, 7),
                Operation(OpType.Subtract, 10),
            ),
        )
        assertEquals(45 * 3 + 7 - 10, round.correctAnswer)
        assertEquals(4, round.totalSteps)
    }

    @Test
    fun difficultiesExposeExpectedShape() {
        assertEquals(3, Difficulty.Easy.operations)
        assertEquals(4, Difficulty.Easy.stepSeconds)
        assertEquals(5, Difficulty.Medium.operations)
        assertEquals(3, Difficulty.Medium.stepSeconds)
        assertEquals(8, Difficulty.Hard.operations)
        assertEquals(2, Difficulty.Hard.stepSeconds)
        assertEquals(0, Difficulty.Custom.operations)
    }

    @Test
    fun customSettingsOverrideDifficulty() {
        val settings = GameSettings(
            difficulty = Difficulty.Custom,
            customOperations = 7,
            customStepSeconds = 8,
        )
        assertEquals(7, settings.effectiveOperations)
        assertEquals(8, settings.effectiveStepSeconds)
    }

    @Test
    fun presetSettingsIgnoreCustomValues() {
        val settings = GameSettings(
            difficulty = Difficulty.Easy,
            customOperations = 7,
            customStepSeconds = 8,
        )
        assertEquals(3, settings.effectiveOperations)
        assertEquals(4, settings.effectiveStepSeconds)
    }

    @Test
    fun emptyEnabledOpsFallsBackToAdd() {
        val settings = GameSettings(enabledOps = emptySet())
        assertEquals(setOf(OpType.Add), settings.activeOps)
    }

    @Test
    fun defaultSettingsAllowAllOperationsAndPause() {
        val settings = GameSettings()
        assertTrue(settings.allowPause)
        assertEquals(OpType.entries.toSet(), settings.enabledOps)
    }

    @Test
    fun multiplyOnlyChainsStayWithinKeypadInput() {
        // Before the cap, 10 multiplications from 200 overflowed Int.
        val settings = GameSettings(
            difficulty = Difficulty.Custom,
            customOperations = 10,
            enabledOps = setOf(OpType.Multiply),
        )
        val rng = Random(7)
        repeat(500) {
            val answer = generateRound(settings, rng).correctAnswer
            assertTrue("answer $answer", answer in 0..9_999_999)
        }
    }

    @Test
    fun generatedRoundsNeverGoNegative() {
        val rng = Random(3)
        listOf(false, true).forEach { roundNumbers ->
            val settings = GameSettings(difficulty = Difficulty.Hard, roundNumbers = roundNumbers)
            repeat(500) {
                val round = generateRound(settings, rng)
                assertEquals(8, round.operations.size)
                var value = round.startNumber
                round.operations.forEach { op ->
                    value = op.apply(value)
                    assertTrue(value >= 0)
                }
            }
        }
    }
}
