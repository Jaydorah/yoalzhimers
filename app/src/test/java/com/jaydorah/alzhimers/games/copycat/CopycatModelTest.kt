package com.jaydorah.alzhimers.games.copycat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class CopycatModelTest {

    @Test
    fun classicStartsWithOne() {
        val settings = CopycatSettings(mode = CopycatMode.Classic)
        assertEquals(1, settings.startingLength)
    }

    @Test
    fun fixedUsesConfiguredLength() {
        val settings = CopycatSettings(
            mode = CopycatMode.Fixed,
            fixedLength = 6,
        )
        assertEquals(6, settings.startingLength)
    }

    @Test
    fun hardIsFasterThanNormal() {
        val normal = CopycatSettings(difficulty = CopycatDifficulty.Normal)
        val hard = CopycatSettings(difficulty = CopycatDifficulty.Hard)
        assertTrue(hard.flashOnMs < normal.flashOnMs)
        assertTrue(hard.flashGapMs <= normal.flashGapMs)
    }

    @Test
    fun classicGrowsSequenceByOne() {
        val state = CopycatState(
            phase = CopycatPhase.Input,
            settings = CopycatSettings(mode = CopycatMode.Classic),
            sequence = listOf(0, 2, 1),
            roundsCleared = 2,
        )
        assertEquals(4, state.nextSequenceLength())
    }

    @Test
    fun classicExtendsPreviousSequence() {
        val settings = CopycatSettings(mode = CopycatMode.Classic, boardSize = BoardSize.Four)
        val previous = listOf(2, 1, 3, 0, 2)
        val next = nextSequence(settings, previous, Random(5))
        assertEquals(previous.size + 1, next.size)
        assertEquals(previous, next.take(previous.size))
    }

    @Test
    fun fixedRollsEntirelyNewSequence() {
        val settings = CopycatSettings(
            mode = CopycatMode.Fixed,
            fixedLength = 4,
            boardSize = BoardSize.Six,
        )
        val previous = listOf(0, 1, 2, 3)
        val next = nextSequence(settings, previous, Random(7))
        assertEquals(4, next.size)
        // Extremely unlikely to be identical; Fixed must not just extend.
        assertTrue(next != previous + next.last())
    }

    @Test
    fun fixedKeepsSequenceLength() {
        val state = CopycatState(
            phase = CopycatPhase.Input,
            settings = CopycatSettings(mode = CopycatMode.Fixed, fixedLength = 5),
            sequence = listOf(0, 2, 1, 3, 0),
            roundsCleared = 4,
        )
        assertEquals(5, state.nextSequenceLength())
    }

    @Test
    fun defaultSettingsAreClassicFourPadsNormal() {
        val settings = CopycatSettings()
        assertEquals(CopycatMode.Classic, settings.mode)
        assertEquals(BoardSize.Four, settings.boardSize)
        assertEquals(CopycatDifficulty.Normal, settings.difficulty)
    }

    @Test
    fun longestClearedIsZeroWhenNothingCleared() {
        val state = CopycatState(
            phase = CopycatPhase.GameOver,
            settings = CopycatSettings(mode = CopycatMode.Classic),
            sequence = listOf(3),
            roundsCleared = 0,
        )
        assertEquals(0, state.longestCleared)
    }

    @Test
    fun classicLongestClearedExcludesFailedPattern() {
        // Failed on the 4-pad pattern after clearing 1, 2 and 3.
        val state = CopycatState(
            phase = CopycatPhase.GameOver,
            settings = CopycatSettings(mode = CopycatMode.Classic),
            sequence = listOf(0, 2, 1, 3),
            roundsCleared = 3,
        )
        assertEquals(3, state.longestCleared)
    }

    @Test
    fun fixedLongestClearedIsPatternLength() {
        val state = CopycatState(
            phase = CopycatPhase.GameOver,
            settings = CopycatSettings(mode = CopycatMode.Fixed, fixedLength = 5),
            sequence = listOf(0, 2, 1, 3, 0),
            roundsCleared = 2,
        )
        assertEquals(5, state.longestCleared)
    }
}
