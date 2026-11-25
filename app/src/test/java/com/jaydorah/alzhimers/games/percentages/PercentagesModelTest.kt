package com.jaydorah.alzhimers.games.percentages

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class PercentagesModelTest {

    @Test
    fun difficultySeconds() {
        assertEquals(0, Difficulty.Easy.secondsPerQuestion)
        assertEquals(30, Difficulty.Normal.secondsPerQuestion)
        assertEquals(10, Difficulty.Hard.secondsPerQuestion)

        assertEquals(0, GameSettings(difficulty = Difficulty.Easy).effectiveSeconds)
        assertEquals(30, GameSettings(difficulty = Difficulty.Normal).effectiveSeconds)
        assertEquals(10, GameSettings(difficulty = Difficulty.Hard).effectiveSeconds)
        assertEquals(
            15,
            GameSettings(difficulty = Difficulty.Custom, customSeconds = 15).effectiveSeconds,
        )
    }

    @Test
    fun customSecondsAreClamped() {
        val low = GameSettings(difficulty = Difficulty.Custom, customSeconds = 1)
        val high = GameSettings(difficulty = Difficulty.Custom, customSeconds = 999)
        assertEquals(MIN_SECONDS, low.effectiveSeconds)
        assertEquals(MAX_SECONDS, high.effectiveSeconds)
    }

    @Test
    fun easyIsUntimed() {
        assertTrue(!GameSettings(difficulty = Difficulty.Easy).timed)
        assertTrue(GameSettings(difficulty = Difficulty.Normal).timed)
        assertTrue(GameSettings(difficulty = Difficulty.Hard).timed)
        assertTrue(GameSettings(difficulty = Difficulty.Custom, customSeconds = 5).timed)
    }

    @Test
    fun questionMath() {
        assertEquals(60, Question(30, 200).answer)
        assertEquals(5, Question(25, 20).answer)
        assertEquals(200, Question(100, 200).answer)
    }

    @Test
    fun stateTimerFractions() {
        val settings = GameSettings(difficulty = Difficulty.Hard)
        val fresh = PercentagesState(phase = GamePhase.Question, settings = settings, remainingMs = 10_000L)
        assertEquals(1f, fresh.timeFraction, 0.001f)
        assertEquals(10_000L, fresh.timeLimitMs)
        assertEquals(10, fresh.secondsLeft)

        val half = fresh.copy(remainingMs = 5_000L)
        assertEquals(0.5f, half.timeFraction, 0.001f)
        assertEquals(5, half.secondsLeft)

        val done = fresh.copy(remainingMs = 0L)
        assertEquals(0f, done.timeFraction, 0.001f)
        assertEquals(0, done.secondsLeft)

        val untimed = PercentagesState(
            phase = GamePhase.Question,
            settings = GameSettings(difficulty = Difficulty.Easy),
        )
        assertEquals(0f, untimed.timeFraction, 0.001f)
        assertEquals(0L, untimed.timeLimitMs)
    }

    @Test
    fun minMaxStaysOrderedAndBounded() {
        val base = GameSettings()

        val minTooHigh = base.withMinNumber(9_000)
        assertEquals(9_000, minTooHigh.minNumber)
        assertEquals(MAX_NUMBER, minTooHigh.maxNumber)

        val maxTooLow = base.withMaxNumber(10)
        assertEquals(MIN_NUMBER, maxTooLow.minNumber)
        assertEquals(10, maxTooLow.maxNumber)

        assertEquals(MAX_NUMBER, base.withMinNumber(999_999).minNumber)
        assertEquals(MIN_NUMBER, base.withMaxNumber(-5).maxNumber)
    }

    @Test
    fun generatedQuestionsAreWholeAndInRange() {
        val settings = GameSettings(minNumber = 10, maxNumber = 5_000)
        val questions = generateQuestions(settings, count = QUESTION_COUNT, rng = Random(7))

        assertEquals(QUESTION_COUNT, questions.size)
        assertEquals(questions.size, questions.toSet().size)
        questions.forEach { q ->
            assertTrue("percent in range: ${q.percent}", q.percent in 1..100)
            assertTrue("number in range: ${q.number}", q.number in settings.minNumber..settings.maxNumber)
            assertTrue("answer >= 1: ${q.answer}", q.answer >= 1)
            assertEquals(q.number * q.percent, q.answer * 100)
        }
    }

    @Test
    fun roundNumbersAreMultiplesOfFive() {
        val settings = GameSettings(
            minNumber = 1,
            maxNumber = 10_000,
            roundNumbers = true,
        )
        val questions = generateQuestions(settings, count = 40, rng = Random(11))
        questions.forEach { q ->
            assertEquals(0, q.number % 5)
        }
    }

    @Test
    fun tinyRangeStillFillsTheRun() {
        val questions = generateQuestions(
            GameSettings(minNumber = 2, maxNumber = 3),
            count = 5,
            rng = Random(3),
        )
        // Only a few whole-number answers exist in such a narrow range.
        assertTrue(questions.isNotEmpty())
        assertTrue(questions.size <= 5)
        questions.forEach { q ->
            assertTrue(q.number in 2..3)
            assertTrue(q.answer >= 1)
        }
    }
}
