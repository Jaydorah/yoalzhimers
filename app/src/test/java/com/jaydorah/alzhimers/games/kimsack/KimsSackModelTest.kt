package com.jaydorah.alzhimers.games.kimsack

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class KimsSackModelTest {

    private val Keys = "keys"
    private val Coin = "coin"
    private val Pencil = "pencil"
    private val Paperclip = "paperclip"
    private val Fork = "fork"
    private val Angel = "angel"
    private val Nail = "nail"

    @Test
    fun difficultyCounts() {
        assertEquals(6, KimsSackDifficulty.Easy.itemCount)
        assertEquals(12, KimsSackDifficulty.Normal.itemCount)
        assertEquals(15, KimsSackDifficulty.Hard.itemCount)
        assertEquals(20, KimsSackDifficulty.Easy.seconds)
        assertEquals(35, KimsSackDifficulty.Normal.seconds)
        assertEquals(50, KimsSackDifficulty.Hard.seconds)
        assertEquals(4, KimsSackDifficulty.Easy.decoys)
        assertEquals(6, KimsSackDifficulty.Normal.decoys)
        assertEquals(5, KimsSackDifficulty.Hard.decoys)
    }

    @Test
    fun tableRowsForDifficulties() {
        assertEquals(2, tableRows(6))
        assertEquals(4, tableRows(12))
        assertEquals(5, tableRows(15))
        assertEquals(0, tableRows(0))
        assertEquals(1, tableRows(3))
        assertEquals(2, tableRows(4))
    }

    @Test
    fun splitSidesBalances() {
        val six = ITEM_POOL.take(6)
        val (l6, r6) = splitSides(six)
        assertEquals(3, l6.size)
        assertEquals(3, r6.size)
        assertEquals(6, (l6 + r6).size)
        assertEquals(six.toSet(), (l6 + r6).toSet())

        val twelve = ITEM_POOL.take(12)
        val (l12, r12) = splitSides(twelve)
        assertEquals(6, l12.size)
        assertEquals(6, r12.size)
        assertEquals(12, (l12 + r12).size)

        val fifteen = ITEM_POOL.take(15)
        val (l15, r15) = splitSides(fifteen)
        assertEquals(8, l15.size)
        assertEquals(7, r15.size)
        assertEquals(15, (l15 + r15).size)
    }

    @Test
    fun pickTrayItemsRespectsCountAndUniqueness() {
        val rng = Random(1)
        val items = pickTrayItems(12, rng)
        assertEquals(12, items.size)
        assertEquals(12, items.toSet().size)
        assertTrue(items.all { it in ITEM_POOL })
    }

    @Test
    fun pickTrayItemsClampsToPoolSize() {
        val items = pickTrayItems(999, Random(2))
        assertEquals(ITEM_POOL.size, items.size)
    }

    @Test
    fun buildPoolIncludesDecoysNotOnTray() {
        val rng = Random(3)
        val tray = pickTrayItems(6, rng)
        val pool = buildPool(tray, decoys = 4, rng = rng)
        assertEquals(10, pool.size)
        assertTrue(pool.containsAll(tray))
        assertEquals(tray.size, pool.count { it in tray })
        val decoys = pool.filter { it !in tray }
        assertEquals(4, decoys.size)
        assertTrue(decoys.none { it in tray })
    }

    @Test
    fun buildPoolClampsDecoysToAvailableTypes() {
        val tray = pickTrayItems(15, Random(4))
        val pool = buildPool(tray, decoys = 999, rng = Random(4))
        assertEquals(ITEM_POOL.size, pool.size)
        assertTrue(pool.containsAll(tray))
        assertEquals(ITEM_POOL.size, pool.toSet().size)
    }

    @Test
    fun buildPoolZeroDecoysIsTrayOnly() {
        val tray = pickTrayItems(6, Random(5))
        val pool = buildPool(tray, decoys = 0, rng = Random(5))
        assertEquals(tray.size, pool.size)
        assertEquals(tray.toSet(), pool.toSet())
    }

    @Test
    fun scoreRecallCountsOnlyMatchingPlacements() {
        val tray = listOf(Keys, Coin, Pencil, Fork)
        val placed = mapOf(
            0 to Keys,
            1 to Paperclip, // wrong (decoy)
            2 to Coin,
            5 to Fork,
            7 to Angel, // wrong (decoy)
        )
        assertEquals(3, scoreRecall(tray, placed))
        assertEquals(2, wrongPlaced(tray, placed))
    }

    @Test
    fun scoreRecallMultisetIgnoresOrder() {
        val tray = listOf(Keys, Coin, Pencil)
        val swapped = mapOf(0 to Pencil, 1 to Coin, 2 to Keys)
        assertEquals(3, scoreRecall(tray, swapped, preciseLocation = false))
        assertEquals(3, scoreRecall(tray, swapped))
    }

    @Test
    fun scoreRecallPreciseRequiresExactSlots() {
        val tray = listOf(Keys, Coin, Pencil)
        val swapped = mapOf(0 to Pencil, 1 to Coin, 2 to Keys)
        assertEquals(1, scoreRecall(tray, swapped, preciseLocation = true))
        assertEquals(2, wrongPlaced(tray, swapped, preciseLocation = true))

        val exact = mapOf(0 to Keys, 1 to Coin, 2 to Pencil)
        assertEquals(3, scoreRecall(tray, exact, preciseLocation = true))
        assertEquals(0, wrongPlaced(tray, exact, preciseLocation = true))
    }

    @Test
    fun scoreRecallPreciseFailsForRightItemWrongSlot() {
        val tray = listOf(Keys, Coin, Pencil, Fork)
        val placed = mapOf(0 to Keys, 1 to Paperclip, 3 to Pencil)
        assertEquals(1, scoreRecall(tray, placed, preciseLocation = true))
        assertEquals(2, wrongPlaced(tray, placed, preciseLocation = true))
        assertEquals(2, scoreRecall(tray, placed, preciseLocation = false))
        assertEquals(1, wrongPlaced(tray, placed, preciseLocation = false))
    }

    @Test
    fun scoreRecallHandlesDuplicateTrayItemsOnceEach() {
        val tray = listOf(Keys, Keys)
        assertEquals(2, scoreRecall(tray, mapOf(0 to Keys, 1 to Keys)))
        assertEquals(1, scoreRecall(tray, mapOf(0 to Keys, 1 to Coin)))
    }

    @Test
    fun emptyPlacementScoresZero() {
        assertEquals(0, scoreRecall(listOf(Keys), emptyMap()))
        assertEquals(0, wrongPlaced(listOf(Keys), emptyMap()))
        assertEquals(0, scoreRecall(listOf(Keys), emptyMap(), preciseLocation = true))
    }

    @Test
    fun defaultSettingsPreciseOff() {
        assertFalse(KimsSackSettings().preciseLocation)
        assertTrue(KimsSackSettings(preciseLocation = true).preciseLocation)
    }

    @Test
    fun memorizeFractionDrainsToOne() {
        val s = KimsSackState(
            phase = KimsSackPhase.Memorize,
            settings = KimsSackSettings(KimsSackDifficulty.Normal),
            elapsedMs = 0L,
        )
        assertEquals(1f, s.memorizeFraction, 1e-6f)
        assertEquals(KimsSackDifficulty.Normal.seconds, s.secondsLeft)
        val mid = s.copy(elapsedMs = s.memorizeDurationMs / 2)
        assertEquals(0.5f, mid.memorizeFraction, 1e-3f)
        val done = s.copy(elapsedMs = s.memorizeDurationMs)
        assertEquals(0f, done.memorizeFraction, 1e-6f)
        assertEquals(0, done.secondsLeft)
    }

    @Test
    fun remainingPoolDropsPlacedItems() {
        val tray = listOf(Keys, Coin, Fork, Angel)
        val pool = listOf(Keys, Coin, Fork, Angel, Nail)
        val s = KimsSackState(
            phase = KimsSackPhase.Recall,
            trayItems = tray,
            pool = pool,
            placed = mapOf(0 to Keys),
        )
        val remaining = s.remainingPool
        assertEquals(4, remaining.size)
        assertEquals(0, remaining.count { it == Keys })
        assertEquals(1, remaining.count { it == Coin })
        assertTrue(remaining.contains(Fork))
        assertTrue(remaining.contains(Angel))
        assertTrue(remaining.contains(Nail))
    }

    @Test
    fun defaultDifficultyIsNormalWithTwelveItems() {
        assertEquals(KimsSackDifficulty.Normal, KimsSackSettings().difficulty)
        assertEquals(12, KimsSackSettings().difficulty.itemCount)
        assertEquals(12, KimsSackDifficulty.Normal.itemCount)
    }

    @Test
    fun collectPhaseExists() {
        val phases = KimsSackPhase.entries
        assertTrue(phases.contains(KimsSackPhase.Collect))
        assertTrue(phases.contains(KimsSackPhase.Options))
        assertTrue(phases.contains(KimsSackPhase.Memorize))
        assertTrue(phases.contains(KimsSackPhase.Recall))
        assertTrue(phases.contains(KimsSackPhase.Result))
    }

    @Test
    fun allDifficultiesCanIncludeDecoys() {
        KimsSackDifficulty.entries.forEach { d ->
            val tray = pickTrayItems(d.itemCount, Random(6))
            val pool = buildPool(tray, d.decoys, Random(6))
            assertTrue(pool.size > tray.size)
            assertTrue(pool.any { it !in tray })
        }
    }
}
