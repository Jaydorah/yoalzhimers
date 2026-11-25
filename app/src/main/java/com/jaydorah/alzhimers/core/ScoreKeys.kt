package com.jaydorah.alzhimers.core

object ScoreKeys {
    const val MentalMath = "mental_math"
    const val Copycat = "copycat"
    const val WordHunt = "word_hunt"
    const val NumberMemory = "number_memory"
    const val Chimp = "chimp"
    const val LightsOut = "lights_out"
    const val ColorMatch = "color_match"
    const val KimsSack = "kims_sack"
    const val ShortTerm = "short_term"
    const val Schulte = "schulte"
    const val Percentages = "percentages"

    /** true = higher is better; false = lower is better (e.g. time, moves). */
    val higherIsBetter: Map<String, Boolean> = mapOf(
        MentalMath to true,
        Copycat to true,
        WordHunt to false,
        NumberMemory to true,
        Chimp to true,
        LightsOut to false,
        ColorMatch to true,
        KimsSack to true,
        ShortTerm to true,
        Schulte to false,
        Percentages to true,
    )
}
