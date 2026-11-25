package com.jaydorah.alzhimers.games.copycat

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import com.jaydorah.alzhimers.R
import kotlin.random.Random

enum class CopycatDifficulty(@StringRes val labelRes: Int) {
    Normal(R.string.cc_diff_normal),
    Hard(R.string.cc_diff_hard),
}

enum class BoardSize(@PluralsRes val labelRes: Int, val pads: Int) {
    Four(R.plurals.cc_pads_count, 4),
    Six(R.plurals.cc_pads_count, 6),
}

enum class CopycatMode(@StringRes val labelRes: Int) {
    Classic(R.string.cc_mode_classic),
    Fixed(R.string.cc_mode_fixed),
}

data class CopycatSettings(
    val difficulty: CopycatDifficulty = CopycatDifficulty.Normal,
    val boardSize: BoardSize = BoardSize.Four,
    val mode: CopycatMode = CopycatMode.Classic,
    val fixedLength: Int = 5,
) {
    val flashOnMs: Long
        get() = if (difficulty == CopycatDifficulty.Hard) 300L else 500L

    val flashGapMs: Long
        get() = if (difficulty == CopycatDifficulty.Hard) 100L else 150L

    val startingLength: Int
        get() = if (mode == CopycatMode.Classic) 1 else fixedLength.coerceIn(3, 15)
}

enum class CopycatPhase { Options, Showing, Input, GameOver }

data class CopycatState(
    val phase: CopycatPhase = CopycatPhase.Options,
    val settings: CopycatSettings = CopycatSettings(),
    val sequence: List<Int> = emptyList(),
    val inputIndex: Int = 0,
    val activePad: Int? = null,
    val roundsCleared: Int = 0,
) {
    /**
     * Longest pattern the player fully repeated. At game over [sequence] is the pattern
     * that was failed, so in Classic the last cleared one was one pad shorter.
     */
    val longestCleared: Int
        get() = when {
            roundsCleared == 0 -> 0
            settings.mode == CopycatMode.Classic -> (sequence.size - 1).coerceAtLeast(0)
            else -> sequence.size
        }

    fun nextSequenceLength(): Int = when (settings.mode) {
        CopycatMode.Classic -> sequence.size + 1
        CopycatMode.Fixed -> settings.startingLength
    }
}

fun randomSequence(
    pads: Int,
    length: Int,
    previous: List<Int> = emptyList(),
    rng: Random = Random.Default,
): List<Int> {
    if (length <= 0) return emptyList()
    while (true) {
        val next = List(length) { rng.nextInt(pads) }
        if (next != previous) return next
    }
}

/**
 * Classic: keep the previous sequence and append one new pad (grows forever).
 * Fixed: throw the old one away and roll a brand-new sequence each round.
 */
fun nextSequence(
    settings: CopycatSettings,
    previous: List<Int>,
    rng: Random = Random.Default,
): List<Int> = when (settings.mode) {
    CopycatMode.Classic -> {
        if (previous.isEmpty()) {
            randomSequence(settings.boardSize.pads, settings.startingLength, rng = rng)
        } else {
            previous + rng.nextInt(settings.boardSize.pads)
        }
    }

    CopycatMode.Fixed -> randomSequence(
        pads = settings.boardSize.pads,
        length = settings.startingLength,
        previous = previous,
        rng = rng,
    )
}
