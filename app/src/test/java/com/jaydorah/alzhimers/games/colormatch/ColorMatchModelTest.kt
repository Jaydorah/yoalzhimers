package com.jaydorah.alzhimers.games.colormatch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ColorMatchModelTest {

    @Test
    fun identicalColorsScore100() {
        val rgb = Rgb(42, 128, 200)
        assertEquals(100, matchScore(rgb, rgb))
    }

    @Test
    fun oppositeCornersScoreZero() {
        assertEquals(0, matchScore(Rgb(0, 0, 0), Rgb(255, 255, 255)))
    }

    @Test
    fun scoreIsBetweenZeroAndHundred() {
        val rng = Random(7)
        repeat(200) {
            val score = matchScore(Rgb.random(rng), Rgb.random(rng))
            assertTrue("score $score out of range", score in 0..100)
        }
    }

    @Test
    fun closerGuessScoresHigher() {
        val target = Rgb(100, 150, 200)
        val close = Rgb(110, 145, 195)
        val far = Rgb(200, 20, 90)
        assertTrue(matchScore(target, close) > matchScore(target, far))
    }

    @Test
    fun distanceIsEuclidean() {
        assertEquals(5.0, colorDistance(Rgb(0, 0, 0), Rgb(3, 4, 0)), 1e-9)
        assertEquals(
            0.0,
            colorDistance(Rgb(12, 34, 56), Rgb(12, 34, 56)),
            1e-9,
        )
    }

    @Test
    fun defaultsHidePreviewOn() {
        assertTrue(ColorMatchSettings().hidePreview)
    }

    @Test
    fun showGuessRevealsOnResultEvenWhenHidden() {
        val matching = ColorMatchState(
            phase = ColorMatchPhase.Matching,
            settings = ColorMatchSettings(hidePreview = true),
        )
        val result = matching.copy(phase = ColorMatchPhase.Result)
        assertFalse(matching.showGuess)
        assertTrue(result.showGuess)
    }

    @Test
    fun averageUsesTotalAcrossRounds() {
        val state = ColorMatchState(
            phase = ColorMatchPhase.Result,
            roundsPlayed = 2,
            totalScore = 150,
            bestScore = 90,
        )
        assertEquals(75, state.averageScore)
    }

    @Test
    fun randomStaysInChannelBounds() {
        val rng = Random(1)
        repeat(100) {
            val c = Rgb.random(rng)
            assertTrue(c.r in 0..255)
            assertTrue(c.g in 0..255)
            assertTrue(c.b in 0..255)
        }
    }
}
