package com.jaydorah.alzhimers.games.mentalmath

import androidx.annotation.StringRes
import com.jaydorah.alzhimers.R
import kotlin.random.Random

enum class Difficulty(
    @StringRes val labelRes: Int,
    val operations: Int,
    val stepSeconds: Int,
) {
    Easy(R.string.mm_diff_easy, 3, 4),
    Medium(R.string.mm_diff_normal, 5, 3),
    Hard(R.string.mm_diff_hard, 8, 2),
    Custom(R.string.mm_diff_custom, 0, 0),
}

enum class OpType(@StringRes val labelRes: Int, val symbol: String) {
    Add(R.string.mm_op_add, "+"),
    Subtract(R.string.mm_op_subtract, "−"),
    Multiply(R.string.mm_op_multiply, "×"),
}

data class GameSettings(
    val difficulty: Difficulty = Difficulty.Easy,
    val customOperations: Int = 3,
    val customStepSeconds: Int = 5,
    val roundNumbers: Boolean = false,
    val allowPause: Boolean = true,
    val enabledOps: Set<OpType> = OpType.entries.toSet(),
) {
    val effectiveOperations: Int
        get() = if (difficulty == Difficulty.Custom) customOperations else difficulty.operations

    val effectiveStepSeconds: Int
        get() = if (difficulty == Difficulty.Custom) customStepSeconds else difficulty.stepSeconds

    val activeOps: Set<OpType>
        get() = enabledOps.ifEmpty { setOf(OpType.Add) }
}

data class Operation(val type: OpType, val operand: Int) {
    val symbol: String get() = type.symbol

    val display: String
        get() = "$symbol $operand"

    fun apply(value: Int): Int = when (type) {
        OpType.Add -> value + operand
        OpType.Subtract -> value - operand
        OpType.Multiply -> value * operand
    }
}

data class Round(
    val startNumber: Int,
    val operations: List<Operation>,
) {
    val correctAnswer: Int by lazy {
        operations.fold(startNumber) { acc, op -> op.apply(acc) }
    }

    val totalSteps: Int get() = operations.size + 1
}

enum class GamePhase { SelectDifficulty, Playing, Paused, Answer, Result }

data class MentalMathState(
    val phase: GamePhase = GamePhase.SelectDifficulty,
    val settings: GameSettings = GameSettings(),
    val round: Round? = null,
    val stepIndex: Int = 0,
    val stepElapsedMs: Long = 0L,
    val input: String = "",
    val wasCorrect: Boolean = false,
) {
    val stepDurationMs: Long get() = settings.effectiveStepSeconds * 1000L

    val stepFraction: Float
        get() = if (stepDurationMs <= 0L) 0f
        else (1f - stepElapsedMs.toFloat() / stepDurationMs).coerceIn(0f, 1f)

    val currentDisplay: String
        get() = round?.let { r ->
            if (stepIndex == 0) r.startNumber.toString()
            else r.operations[stepIndex - 1].display
        } ?: ""
}

/**
 * Multiplication is only offered while the running total is at most this,
 * so the answer always fits the 7-digit keypad input and never overflows.
 */
const val MAX_MULTIPLY_BASE = 99_999

fun generateRound(settings: GameSettings, rng: Random = Random.Default): Round {
    val start = if (settings.roundNumbers) {
        rng.nextInt(0, 41) * 5
    } else {
        rng.nextInt(0, 201)
    }
    val ops = mutableListOf<Operation>()
    var current = start
    repeat(settings.effectiveOperations) {
        val op = randomOperation(current, settings, rng)
        ops += op
        current = op.apply(current)
    }
    return Round(start, ops)
}

private fun randomOperation(current: Int, settings: GameSettings, rng: Random): Operation {
    val round = settings.roundNumbers
    val enabled = settings.activeOps

    val choices = enabled.filter { op ->
        when (op) {
            OpType.Subtract -> if (round) current >= 5 else current > 25
            OpType.Multiply -> current <= MAX_MULTIPLY_BASE
            else -> true
        }
    }.ifEmpty { listOf(OpType.Add) }

    return when (choices.random(rng)) {
        OpType.Add -> {
            if (round) Operation(OpType.Add, rng.nextInt(1, 9) * 5)
            else Operation(OpType.Add, rng.nextInt(1, 41))
        }

        OpType.Subtract -> {
            if (round) {
                val maxSteps = current / 5
                Operation(OpType.Subtract, rng.nextInt(1, minOf(maxSteps, 5) + 1) * 5)
            } else {
                val operand = rng.nextInt(1, minOf(current - 1, 25) + 1)
                Operation(OpType.Subtract, operand)
            }
        }

        OpType.Multiply -> {
            val maxMul = when (settings.difficulty) {
                Difficulty.Easy -> 6
                Difficulty.Medium -> 8
                Difficulty.Hard -> 12
                Difficulty.Custom -> 8
            }
            val mulCap = if (current > 80) minOf(maxMul, 6) else maxMul
            Operation(OpType.Multiply, rng.nextInt(2, mulCap + 1))
        }
    }
}
