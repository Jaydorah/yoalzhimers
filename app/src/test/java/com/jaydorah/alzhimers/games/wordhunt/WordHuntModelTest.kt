package com.jaydorah.alzhimers.games.wordhunt

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class WordHuntModelTest {

    private val sampleWords = listOf(
        "cat", "dog", "house", "river", "mountain", "cloud",
        "star", "green", "happy", "walk", "jump", "blue",
        "tree", "book", "sun", "moon", "light", "water",
    )

    @Test
    fun difficultiesExposeExpectedShape() {
        assertEquals(8, Difficulty.Easy.gridSize)
        assertEquals(5, Difficulty.Easy.wordCount)
        assertEquals(10, Difficulty.Medium.gridSize)
        assertEquals(7, Difficulty.Medium.wordCount)
        assertEquals(12, Difficulty.Hard.gridSize)
        assertEquals(9, Difficulty.Hard.wordCount)
        assertEquals(listOf(Direction.Right, Direction.Down), Difficulty.Easy.directions)
    }

    @Test
    fun generateRoundProducesSizedGrid() {
        val round = generateRound(sampleWords, GameSettings(Difficulty.Easy), Random(1))
        assertEquals(8, round.size)
        assertEquals(8, round.grid.size)
        round.grid.forEach { row ->
            assertEquals(8, row.size)
            assertTrue(row.all { it in 'a'..'z' })
        }
    }

    @Test
    fun generateRoundPlacesUpToWordCount() {
        val round = generateRound(sampleWords, GameSettings(Difficulty.Easy), Random(2))
        assertTrue(round.placements.isNotEmpty())
        assertTrue(round.placements.size <= Difficulty.Easy.wordCount)
        assertEquals(round.placements.size, round.words.size)
    }

    @Test
    fun everyPlacementMatchesGridLetters() {
        val round = generateRound(sampleWords, GameSettings(Difficulty.Hard), Random(3))
        for (placement in round.placements) {
            val letters = lettersAt(round.grid, placement.cells)
            assertTrue(
                "expected ${placement.word} at ${placement.cells} got $letters",
                letters == placement.word || letters.reversed() == placement.word,
            )
        }
    }

    @Test
    fun placementWordsRespectDifficultyLength() {
        val round = generateRound(sampleWords, GameSettings(Difficulty.Medium), Random(4))
        for (word in round.words) {
            assertTrue(word.length in Difficulty.Medium.minWordLen..Difficulty.Medium.maxWordLen)
        }
    }

    @Test
    fun filtersWordsOutsideLengthRange() {
        val words = listOf("a", "to", "supercalifragilistic")
        val round = generateRound(words, GameSettings(Difficulty.Easy), Random(5))
        assertTrue(round.placements.isEmpty())
    }

    @Test
    fun emptyWordListYieldsEmptyPlacements() {
        val round = generateRound(emptyList(), GameSettings(Difficulty.Easy), Random(6))
        assertTrue(round.placements.isEmpty())
        assertEquals(8, round.grid.size)
    }

    @Test
    fun cellsAlongHorizontalVerticalDiagonal() {
        assertEquals(listOf(Cell(0, 0), Cell(0, 1), Cell(0, 2)), cellsAlong(Cell(0, 0), Cell(0, 2)))
        assertEquals(listOf(Cell(0, 0), Cell(1, 0), Cell(2, 0)), cellsAlong(Cell(0, 0), Cell(2, 0)))
        assertEquals(listOf(Cell(0, 0), Cell(1, 1), Cell(2, 2)), cellsAlong(Cell(0, 0), Cell(2, 2)))
        assertEquals(listOf(Cell(2, 2), Cell(1, 1), Cell(0, 0)), cellsAlong(Cell(2, 2), Cell(0, 0)))
    }

    @Test
    fun cellsAlongRejectsNonStraightPath() {
        assertNull(cellsAlong(Cell(0, 0), Cell(1, 2)))
        assertNull(cellsAlong(Cell(0, 0), Cell(2, 1)))
    }

    @Test
    fun cellsAlongSameCellReturnsSingleCell() {
        assertEquals(listOf(Cell(3, 3)), cellsAlong(Cell(3, 3), Cell(3, 3)))
    }

    @Test
    fun matchSelectionFindsForwardWord() {
        val grid = listOf(
            listOf('c', 'a', 't'),
            listOf('x', 'y', 'z'),
        )
        val match = matchSelection(
            grid,
            listOf(Cell(0, 0), Cell(0, 1), Cell(0, 2)),
            listOf("cat", "dog"),
            found = emptySet(),
        )
        assertEquals("cat", match)
    }

    @Test
    fun matchSelectionFindsReversedWord() {
        val grid = listOf(
            listOf('t', 'a', 'c'),
            listOf('x', 'y', 'z'),
        )
        val match = matchSelection(
            grid,
            listOf(Cell(0, 2), Cell(0, 1), Cell(0, 0)),
            listOf("cat"),
            found = emptySet(),
        )
        assertEquals("cat", match)
    }

    @Test
    fun matchSelectionIgnoresAlreadyFoundWords() {
        val grid = listOf(
            listOf('c', 'a', 't'),
            listOf('x', 'y', 'z'),
        )
        val match = matchSelection(
            grid,
            listOf(Cell(0, 0), Cell(0, 1), Cell(0, 2)),
            listOf("cat"),
            found = setOf("cat"),
        )
        assertNull(match)
    }

    @Test
    fun matchSelectionRejectsNonWordAndShortSelection() {
        val grid = listOf(
            listOf('c', 'a', 't'),
            listOf('x', 'y', 'z'),
        )
        assertNull(
            matchSelection(
                grid,
                listOf(Cell(0, 0), Cell(0, 1)),
                listOf("cat"),
                found = emptySet(),
            ),
        )
        assertNull(
            matchSelection(
                grid,
                listOf(Cell(0, 0)),
                listOf("cat"),
                found = emptySet(),
            ),
        )
    }

    @Test
    fun defaultSettingsAllowPauseOnEasy() {
        val settings = GameSettings()
        assertTrue(settings.allowPause)
        assertEquals(Difficulty.Easy, settings.difficulty)
    }

    @Test
    fun stateTimeLabelFormatsMinutesAndSeconds() {
        val state = WordHuntState(elapsedMs = 65_000L)
        assertEquals("1:05", state.timeLabel)
        val zero = WordHuntState(elapsedMs = 0L)
        assertEquals("0:00", zero.timeLabel)
    }

    @Test
    fun allFoundWhenEveryWordFound() {
        val round = generateRound(sampleWords, GameSettings(Difficulty.Easy), Random(7))
        assertNotNull(round.placements.firstOrNull())
        val partial = WordHuntState(round = round, foundWords = round.words.dropLast(1).toSet())
        assertTrue(!partial.allFound)
        val done = WordHuntState(round = round, foundWords = round.words.toSet())
        assertTrue(done.allFound)
    }
}
