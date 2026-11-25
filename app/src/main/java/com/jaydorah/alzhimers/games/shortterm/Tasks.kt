package com.jaydorah.alzhimers.games.shortterm

import androidx.annotation.StringRes
import com.jaydorah.alzhimers.R
import kotlin.random.Random

/** Pure generation + validation for the 22 distraction tasks. */

enum class TaskKind {
    Reaction, Math, Stroop, LetterType, TapCount,
    Extremes, GridSearch, LetterCase, SolveX, Shapes,
    Arrow, Sequence, Swatch, RapidClick, ButtonDir,
    Parity, Drag, Compare, EmojiCategory, MissingDigit,
    CircleSize, OrderedTap,
}

fun taskKindAt(index: Int): TaskKind =
    TaskKind.entries[(index % TaskKind.entries.size).coerceIn(0, TaskKind.entries.size - 1)]

sealed interface TaskOutcome {
    data object Incomplete : TaskOutcome
    data object Complete : TaskOutcome
    data class Fail(
        @StringRes val reasonRes: Int,
        val formatArgs: Array<Any> = emptyArray(),
    ) : TaskOutcome
}

// --- Digit-append answer flow shared by Math / SolveX / Sequence ---

data class DigitAnswerState(val input: String = "") {
    fun press(digit: String, answer: Int): Pair<DigitAnswerState, TaskOutcome> {
        val next = input + digit
        val parsed = next.toIntOrNull()
        return when {
            parsed != null && parsed == answer -> DigitAnswerState(next) to TaskOutcome.Complete
            next.length >= answer.toString().length ->
                DigitAnswerState(next) to TaskOutcome.Fail(R.string.stm_fail_wrong_answer)
            else -> DigitAnswerState(next) to TaskOutcome.Incomplete
        }
    }
}

// --- Task generators ---

data class ReactionSpec(val targetGreen: Boolean, val goDelayMs: Int)

fun generateReaction(timerDuration: Float, rng: Random = Random): ReactionSpec {
    val (min, max) = when {
        timerDuration <= 1.1f -> 0.2 to 0.4
        timerDuration <= 1.6f -> 0.25 to 0.45
        else -> 0.7 to 1.3
    }
    val delay = (rng.nextDouble() * (max - min) + min) * 1000
    return ReactionSpec(targetGreen = rng.nextBoolean(), goDelayMs = delay.toInt())
}

fun validateReaction(go: Boolean, clicked: Boolean): TaskOutcome = when {
    !clicked -> TaskOutcome.Incomplete
    go -> TaskOutcome.Complete
    else -> TaskOutcome.Fail(R.string.stm_fail_too_soon)
}

data class MathSpec(val add: Boolean, val n1: Int, val n2: Int) {
    val answer: Int get() = if (add) n1 + n2 else n1 - n2
    val prompt: String get() = "$n1 ${if (add) "+" else "-"} $n2"
}

fun generateMath(rng: Random = Random): MathSpec =
    MathSpec(add = rng.nextBoolean(), n1 = rng.nextInt(15) + 10, n2 = rng.nextInt(9) + 1)

data class StroopSpec(val identifyFontColor: Boolean, val word: String, val wordHex: String, val inkName: String, val inkHex: String)

val STROOP_COLORS = listOf(
    Triple("Red", "#ef4444", 0xFFEF4444.toInt()),
    Triple("Blue", "#3b82f6", 0xFF3B82F6.toInt()),
    Triple("Green", "#22c55e", 0xFF22C55E.toInt()),
)

fun generateStroop(rng: Random = Random): StroopSpec {
    val identifyFont = rng.nextBoolean()
    val text = STROOP_COLORS[rng.nextInt(3)]
    // ink = first color in order that isn't the word (matches HTML find)
    val ink = STROOP_COLORS.first { it.first != text.first }
    return StroopSpec(
        identifyFontColor = identifyFont,
        word = text.first,
        wordHex = text.second,
        inkName = ink.first,
        inkHex = ink.second,
    )
}

fun validateStroop(spec: StroopSpec, chosen: String): TaskOutcome {
    val correct = if (!spec.identifyFontColor) chosen == spec.word else chosen == spec.inkName
    return if (correct) TaskOutcome.Complete
    else TaskOutcome.Fail(R.string.stm_fail_stroop)
}

data class LetterTypeSpec(val wantVowel: Boolean, val letters: List<String>)

fun generateLetterType(rng: Random = Random): LetterTypeSpec =
    LetterTypeSpec(
        wantVowel = rng.nextBoolean(),
        letters = listOf("A", "E", "B", "K", "T", "O").shuffled(rng),
    )

fun validateLetterType(spec: LetterTypeSpec, chosen: String): TaskOutcome {
    val isVowel = chosen.uppercase() in setOf("A", "E", "I", "O", "U")
    val correct = if (spec.wantVowel) isVowel else !isVowel
    return if (correct) TaskOutcome.Complete
    else TaskOutcome.Fail(
        if (spec.wantVowel) R.string.stm_fail_not_vowel else R.string.stm_fail_not_consonant,
    )
}

data class TapCountSpec(val needed: Int, val count: Int = 0)

fun generateTapCount(rng: Random = Random): TapCountSpec = TapCountSpec(needed = rng.nextInt(3) + 3)

fun tapCountStep(spec: TapCountSpec): Pair<TapCountSpec, TaskOutcome> {
    val next = spec.count + 1
    return if (next == spec.needed) spec.copy(count = next) to TaskOutcome.Complete
    else spec.copy(count = next) to TaskOutcome.Incomplete
}

data class ExtremesSpec(val numbers: List<Int>, val wantLargest: Boolean)

fun generateExtremes(rng: Random = Random): ExtremesSpec =
    ExtremesSpec(
        numbers = listOf(rng.nextInt(40), rng.nextInt(40) + 41).shuffled(rng),
        wantLargest = rng.nextBoolean(),
    )

fun validateExtremes(spec: ExtremesSpec, chosen: Int): TaskOutcome {
    val target = if (spec.wantLargest) spec.numbers.max() else spec.numbers.min()
    return if (chosen == target) TaskOutcome.Complete
    else TaskOutcome.Fail(R.string.stm_fail_extreme)
}

data class GridSearchSpec(val target: String, val cells: List<String>)

fun generateGridSearch(rng: Random = Random): GridSearchSpec {
    val target = if (rng.nextBoolean()) "X" else "7"
    val filler = if (target == "X") "K" else "1"
    val cells = MutableList(12) { filler }
    cells[rng.nextInt(12)] = target
    return GridSearchSpec(target, cells)
}

fun validateGridSearch(spec: GridSearchSpec, chosen: String): TaskOutcome =
    if (chosen == spec.target) TaskOutcome.Complete else TaskOutcome.Fail(R.string.stm_fail_target)

data class LetterCaseSpec(val wantUpper: Boolean, val chars: List<String>)

fun generateLetterCase(rng: Random = Random): LetterCaseSpec =
    LetterCaseSpec(
        wantUpper = rng.nextBoolean(),
        chars = listOf("Q", "r", "W", "p", "Z", "m").shuffled(rng),
    )

fun validateLetterCase(spec: LetterCaseSpec, chosen: String): TaskOutcome {
    val isUpper = chosen == chosen.uppercase()
    val correct = if (spec.wantUpper) isUpper else !isUpper
    return if (correct) TaskOutcome.Complete else TaskOutcome.Fail(R.string.stm_fail_letter_case)
}

data class SolveXSpec(val xVal: Int, val factor: Int) {
    val product: Int get() = xVal * factor
}

fun generateSolveX(rng: Random = Random): SolveXSpec =
    SolveXSpec(xVal = rng.nextInt(8) + 3, factor = rng.nextInt(4) + 2)

data class ShapeSpec(val shapes: List<Pair<String, String>>, val target: String)

fun generateShapes(rng: Random = Random): ShapeSpec {
    val shapes = listOf("Circle" to "●", "Square" to "■", "Triangle" to "▲").shuffled(rng)
    val target = shapes[rng.nextInt(3)].first
    return ShapeSpec(shapes, target)
}

fun validateShapes(spec: ShapeSpec, chosen: String): TaskOutcome =
    if (chosen == spec.target) TaskOutcome.Complete else TaskOutcome.Fail(R.string.stm_fail_shape)

data class ArrowSpec(val sameMode: Boolean, val left: Boolean)

fun generateArrow(rng: Random = Random): ArrowSpec =
    ArrowSpec(sameMode = rng.nextBoolean(), left = rng.nextBoolean())

fun validateArrow(spec: ArrowSpec, choseLeft: Boolean): TaskOutcome {
    val correct = if (spec.sameMode) choseLeft == spec.left else choseLeft != spec.left
    return if (correct) TaskOutcome.Complete else TaskOutcome.Fail(R.string.stm_fail_direction)
}

data class SequenceSpec(val start: Int, val step: Int) {
    val answer: Int get() = start + step * 3
    val prompt: String get() = "$start, ${start + step}, ${start + step * 2}, ?"
}

fun generateSequence(rng: Random = Random): SequenceSpec =
    SequenceSpec(start = rng.nextInt(10), step = rng.nextInt(4) + 2)

data class SwatchSpec(val order: List<String>, val target: String)

fun generateSwatch(rng: Random = Random): SwatchSpec {
    val order = listOf("RED", "BLUE").shuffled(rng)
    return SwatchSpec(order, order[0])
}

fun validateSwatch(spec: SwatchSpec, chosen: String): TaskOutcome =
    if (chosen == spec.target) TaskOutcome.Complete else TaskOutcome.Fail(R.string.stm_fail_swatch)

data class RapidClickSpec(val target: Int, val taps: Int = 0)

fun generateRapidClick(rng: Random = Random): RapidClickSpec =
    RapidClickSpec(target = if (rng.nextBoolean()) 2 else 3)

fun rapidClickStep(spec: RapidClickSpec): Pair<RapidClickSpec, TaskOutcome> {
    val next = spec.taps + 1
    return if (next == spec.target) spec.copy(taps = next) to TaskOutcome.Complete
    else spec.copy(taps = next) to TaskOutcome.Incomplete
}

data class ButtonDirSpec(val targetRight: Boolean)

fun generateButtonDir(rng: Random = Random): ButtonDirSpec = ButtonDirSpec(rng.nextBoolean())

fun validateButtonDir(spec: ButtonDirSpec, choseRight: Boolean): TaskOutcome =
    if (choseRight == spec.targetRight) TaskOutcome.Complete
    else TaskOutcome.Fail(R.string.stm_fail_button)

data class ParitySpec(val wantEven: Boolean, val numbers: List<Int>)

fun generateParity(rng: Random = Random): ParitySpec {
    val wantEven = rng.nextBoolean()
    val numbers = (if (wantEven) listOf(1, 3, 5, 8) else listOf(2, 4, 6, 9)).shuffled(rng)
    return ParitySpec(wantEven, numbers)
}

fun validateParity(spec: ParitySpec, chosen: Int): TaskOutcome {
    val isEven = chosen % 2 == 0
    val correct = if (spec.wantEven) isEven else !isEven
    return if (correct) TaskOutcome.Complete
    else TaskOutcome.Fail(
        if (spec.wantEven) R.string.stm_fail_not_even else R.string.stm_fail_not_odd,
    )
}

enum class DragCorner { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

data class DragSpec(val target: DragCorner)

fun generateDrag(rng: Random = Random): DragSpec =
    DragSpec(DragCorner.entries[rng.nextInt(4)])

/** Distance from a corner (in dp; callers convert to px) that counts as reaching it. */
const val DRAG_THRESHOLD_DP = 35f

fun validateDrag(
    spec: DragSpec,
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    threshold: Float = DRAG_THRESHOLD_DP,
): Boolean {
    val t = threshold
    return when (spec.target) {
        DragCorner.TOP_LEFT -> x < t && y < t
        DragCorner.TOP_RIGHT -> x > width - t && y < t
        DragCorner.BOTTOM_LEFT -> x < t && y > height - t
        DragCorner.BOTTOM_RIGHT -> x > width - t && y > height - t
    }
}

data class CompareSpec(val v1: Int, val v2: Int, val greater: Boolean) {
    val reality: Boolean get() = if (greater) v1 > v2 else v1 < v2
    val prompt: String
        get() = "Is $v1 ${if (greater) "greater than" else "less than"} $v2?"
}

fun generateCompare(rng: Random = Random): CompareSpec =
    CompareSpec(v1 = rng.nextInt(20), v2 = rng.nextInt(20), greater = rng.nextBoolean())

fun validateCompare(spec: CompareSpec, isYes: Boolean): TaskOutcome =
    if (isYes == spec.reality) TaskOutcome.Complete else TaskOutcome.Fail(R.string.stm_fail_compare)

data class EmojiSpec(val wantFruit: Boolean, val choices: List<String>)

fun generateEmoji(rng: Random = Random): EmojiSpec {
    val wantFruit = rng.nextBoolean()
    val choices = (if (wantFruit) listOf("🍎", "🐶", "🐱", "🦁") else listOf("🐶", "🍎", "🍇", "🍐")).shuffled(rng)
    return EmojiSpec(wantFruit, choices)
}

fun validateEmoji(spec: EmojiSpec, chosen: String): TaskOutcome {
    val isFruit = chosen in setOf("🍎", "🍇", "🍐")
    val correct = if (spec.wantFruit) isFruit else !isFruit
    return if (correct) TaskOutcome.Complete
    else TaskOutcome.Fail(
        if (spec.wantFruit) R.string.stm_fail_not_fruit else R.string.stm_fail_not_animal,
    )
}

data class MissingDigitSpec(val start: Int, val missingIdx: Int, val options: List<Int>) {
    val sequence: List<Int> get() = listOf(start, start + 1, start + 2, start + 3, start + 4)
    val answer: Int get() = sequence[missingIdx]
    val display: String
        get() = sequence.mapIndexed { i, n -> if (i == missingIdx) "?" else n.toString() }.joinToString(" ")
}

fun generateMissingDigit(rng: Random = Random): MissingDigitSpec {
    val start = rng.nextInt(5)
    val missingIdx = rng.nextInt(5)
    val seq = listOf(start, start + 1, start + 2, start + 3, start + 4)
    val correct = seq[missingIdx]
    val options = listOf(correct, correct + 5, correct - 2).shuffled(rng)
    return MissingDigitSpec(start, missingIdx, options)
}

fun validateMissingDigit(spec: MissingDigitSpec, chosen: Int): TaskOutcome =
    if (chosen == spec.answer) TaskOutcome.Complete else TaskOutcome.Fail(R.string.stm_fail_missing_digit)

enum class CircleSizeTarget { BIGGEST, SMALLEST, MEDIUM }
enum class CircleId { S, M, L }

data class CircleSizeSpec(val target: CircleSizeTarget, val order: List<CircleId>)

fun generateCircleSize(rng: Random = Random): CircleSizeSpec {
    val r = rng.nextFloat()
    val target = when {
        r < 0.33f -> CircleSizeTarget.BIGGEST
        r < 0.66f -> CircleSizeTarget.SMALLEST
        else -> CircleSizeTarget.MEDIUM
    }
    return CircleSizeSpec(target, listOf(CircleId.S, CircleId.M, CircleId.L).shuffled(rng))
}

fun validateCircleSize(spec: CircleSizeSpec, id: CircleId): TaskOutcome {
    val ok = when (spec.target) {
        CircleSizeTarget.BIGGEST -> id == CircleId.L
        CircleSizeTarget.SMALLEST -> id == CircleId.S
        CircleSizeTarget.MEDIUM -> id == CircleId.M
    }
    return if (ok) TaskOutcome.Complete else TaskOutcome.Fail(R.string.stm_fail_circle)
}

data class OrderedTapSpec(
    val order: List<Int>,
    val buttons: List<Int>,
    val step: Int = 0,
    val completed: List<Int> = emptyList(),
)

fun generateOrderedTap(rng: Random = Random): OrderedTapSpec =
    OrderedTapSpec(
        order = listOf(1, 2, 3).shuffled(rng),
        buttons = listOf(1, 2, 3).shuffled(rng),
    )

fun orderedTapStep(spec: OrderedTapSpec, n: Int): Pair<OrderedTapSpec, TaskOutcome> {
    return if (n == spec.order[spec.step]) {
        val completed = spec.completed + n
        if (spec.step == spec.order.size - 1) {
            spec.copy(step = spec.step + 1, completed = completed) to TaskOutcome.Complete
        } else {
            spec.copy(step = spec.step + 1, completed = completed) to TaskOutcome.Incomplete
        }
    } else {
        spec to TaskOutcome.Fail(R.string.stm_fail_order)
    }
}
