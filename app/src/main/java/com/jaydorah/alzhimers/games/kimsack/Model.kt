package com.jaydorah.alzhimers.games.kimsack

import androidx.annotation.StringRes
import com.jaydorah.alzhimers.R
import com.jaydorah.alzhimers.ui.components.SPRITE_NAMES
import kotlin.random.Random

typealias ItemType = String

val ITEM_POOL: List<ItemType> = SPRITE_NAMES

enum class KimsSackDifficulty(
    @StringRes val labelRes: Int,
    val itemCount: Int,
    val seconds: Int,
    val decoys: Int,
) {
    Easy(R.string.ks_diff_easy, 6, 20, 4),
    Normal(R.string.ks_diff_normal, 12, 35, 6),
    Hard(R.string.ks_diff_hard, 15, 50, 5),
}

enum class KimsSackPhase { Options, Memorize, Collect, Recall, Result }

const val TABLE_COLS = 3

fun tableRows(itemCount: Int): Int =
    if (itemCount <= 0) 0 else (itemCount + TABLE_COLS - 1) / TABLE_COLS

fun splitSides(pool: List<ItemType>): Pair<List<ItemType>, List<ItemType>> {
    val mid = (pool.size + 1) / 2
    return pool.take(mid) to pool.drop(mid)
}

data class KimsSackSettings(
    val difficulty: KimsSackDifficulty = KimsSackDifficulty.Normal,
    /** When true, each item must land in the exact slot it occupied while memorizing. */
    val preciseLocation: Boolean = false,
)

data class KimsSackState(
    val phase: KimsSackPhase = KimsSackPhase.Options,
    val settings: KimsSackSettings = KimsSackSettings(),
    val trayItems: List<ItemType> = emptyList(),
    val pool: List<ItemType> = emptyList(),
    val placed: Map<Int, ItemType> = emptyMap(),
    val elapsedMs: Long = 0L,
    val correctCount: Int = 0,
    val wrongCount: Int = 0,
) {
    val memorizeDurationMs: Long get() = settings.difficulty.seconds * 1000L

    val memorizeFraction: Float
        get() = if (memorizeDurationMs <= 0L) 0f
        else (1f - elapsedMs.toFloat() / memorizeDurationMs).coerceIn(0f, 1f)

    val secondsLeft: Int
        get() = ((memorizeDurationMs - elapsedMs + 999) / 1000).toInt().coerceAtLeast(0)

    val score: Int get() = correctCount

    val leftSide: List<ItemType> get() = splitSides(remainingPool).first
    val rightSide: List<ItemType> get() = splitSides(remainingPool).second

    /** Items still available to drag from the sides (not yet placed). */
    val remainingPool: List<ItemType>
        get() {
            val used = placed.values.groupingBy { it }.eachCount()
            val left = used.toMutableMap()
            return pool.filter { item ->
                val n = left.getOrDefault(item, 0)
                if (n > 0) {
                    left[item] = n - 1
                    false
                } else {
                    true
                }
            }
        }
}

/** Originals for the table (unique types, count limited by available ItemTypes). */
fun pickTrayItems(count: Int, rng: Random = Random.Default): List<ItemType> =
    ITEM_POOL.shuffled(rng).take(count.coerceIn(1, ITEM_POOL.size))

/**
 * Side-rail pool: originals + decoys (other types not on the tray), shuffled.
 * Decoy count is clamped so the pool never exceeds the full ItemType set.
 */
fun buildPool(
    tray: List<ItemType>,
    decoys: Int,
    rng: Random = Random.Default,
): List<ItemType> {
    val extras = (ITEM_POOL - tray.toSet()).shuffled(rng)
        .take(decoys.coerceIn(0, ITEM_POOL.size - tray.size))
    return (tray + extras).shuffled(rng)
}

/**
 * How many placements are correct.
 * - Multiset mode: item was on the tray (order/slot ignored).
 * - Precise mode: item sits in the exact original slot.
 */
fun scoreRecall(
    tray: List<ItemType>,
    placed: Map<Int, ItemType>,
    preciseLocation: Boolean = false,
): Int {
    if (preciseLocation) {
        return tray.indices.count { slot -> placed[slot] == tray[slot] }
    }
    val remaining = tray.groupingBy { it }.eachCount().toMutableMap()
    var correct = 0
    placed.values.forEach { item ->
        val n = remaining.getOrDefault(item, 0)
        if (n > 0) {
            remaining[item] = n - 1
            correct++
        }
    }
    return correct
}

fun wrongPlaced(
    tray: List<ItemType>,
    placed: Map<Int, ItemType>,
    preciseLocation: Boolean = false,
): Int = placed.size - scoreRecall(tray, placed, preciseLocation)
