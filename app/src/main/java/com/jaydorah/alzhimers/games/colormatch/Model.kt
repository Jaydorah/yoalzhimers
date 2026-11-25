package com.jaydorah.alzhimers.games.colormatch

import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.random.Random

data class Rgb(val r: Int, val g: Int, val b: Int) {
    init {
        require(r in 0..255 && g in 0..255 && b in 0..255) {
            "RGB components must be in 0..255, got ($r, $g, $b)"
        }
    }

    companion object {
        fun random(rng: Random = Random.Default): Rgb = Rgb(
            rng.nextInt(256),
            rng.nextInt(256),
            rng.nextInt(256),
        )
    }
}

fun colorDistance(a: Rgb, b: Rgb): Double {
    val dr = (a.r - b.r).toDouble()
    val dg = (a.g - b.g).toDouble()
    val db = (a.b - b.b).toDouble()
    return sqrt(dr * dr + dg * dg + db * db)
}

fun matchScore(target: Rgb, guess: Rgb): Int {
    val maxDistance = sqrt(3.0 * 255.0 * 255.0)
    val score = 100.0 * (1.0 - colorDistance(target, guess) / maxDistance)
    return score.roundToInt().coerceIn(0, 100)
}

data class ColorMatchSettings(
    val hidePreview: Boolean = true,
)

enum class ColorMatchPhase { Options, Matching, Result }

data class ColorMatchState(
    val phase: ColorMatchPhase = ColorMatchPhase.Options,
    val settings: ColorMatchSettings = ColorMatchSettings(),
    val target: Rgb = Rgb(128, 128, 128),
    val guess: Rgb = Rgb(128, 128, 128),
    val score: Int? = null,
    val roundsPlayed: Int = 0,
    val bestScore: Int = 0,
    val totalScore: Int = 0,
) {
    val showGuess: Boolean get() = !settings.hidePreview || phase == ColorMatchPhase.Result

    val deltaR: Int get() = guess.r - target.r
    val deltaG: Int get() = guess.g - target.g
    val deltaB: Int get() = guess.b - target.b

    val averageScore: Int
        get() = if (roundsPlayed == 0) 0 else totalScore / roundsPlayed
}
