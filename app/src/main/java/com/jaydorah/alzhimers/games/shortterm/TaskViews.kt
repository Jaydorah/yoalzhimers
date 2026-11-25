package com.jaydorah.alzhimers.games.shortterm

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jaydorah.alzhimers.R
import kotlin.math.roundToInt

// Shared palette for neo-brutalist task UI (accents only; neutrals from theme)
private val AccentBlue = Color(0xFF2563EB)
private val Green500 = Color(0xFF22C55E)
private val Green100 = Color(0xFFDCFCE7)
private val Red500 = Color(0xFFEF4444)
private val Red100 = Color(0xFFFEE2E2)
private val Purple600 = Color(0xFF9333EA)
private val Indigo600 = Color(0xFF4F46E5)

@Composable
private fun taskText(): Color = MaterialTheme.colorScheme.onSurface

@Composable
private fun taskMutedText(): Color = MaterialTheme.colorScheme.onSurfaceVariant

@Composable
private fun taskBorder(): Color = MaterialTheme.colorScheme.outline

@Composable
private fun taskSurface(): Color = MaterialTheme.colorScheme.surface

@Composable
private fun taskSurfaceVariant(): Color = MaterialTheme.colorScheme.surfaceVariant

/** Keeps a sub-tree left-to-right in RTL locales (equations, digit sequences, keypads, left/right buttons). */
@Composable
internal fun ForceLtr(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr, content = content)
}

@Composable
fun ObjectiveHeader(text: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            stringResource(R.string.stm_mission_objective),
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(999.dp))
                .padding(horizontal = 10.dp, vertical = 3.dp),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun TaskKeypad(onInput: (String) -> Unit, modifier: Modifier = Modifier) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("", "0", ""),
    )
    ForceLtr {
        Column(
            modifier = modifier.width(240.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            rows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { key ->
                        if (key.isEmpty()) {
                            Spacer(Modifier.size(64.dp).weight(1f))
                        } else {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(taskSurface())
                                    .border(2.dp, taskBorder(), RoundedCornerShape(12.dp))
                                    .clickable { onInput(key) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(key, fontSize = 18.sp, fontWeight = FontWeight.Black, color = taskText())
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskChoiceRow(
    labels: List<Pair<String, String>>,
    onPick: (String) -> Unit,
    modifier: Modifier = Modifier,
    container: Color = Color.Unspecified,
    contentColor: Color = Color.Unspecified,
) {
    val bg = if (container == Color.Unspecified) taskSurfaceVariant() else container
    val fg = if (contentColor == Color.Unspecified) taskText() else contentColor
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        labels.forEach { (value, display) ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(bg)
                    .border(2.dp, taskBorder(), RoundedCornerShape(10.dp))
                    .clickable { onPick(value) }
                    .padding(vertical = 12.dp, horizontal = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(display, fontWeight = FontWeight.Black, color = fg)
            }
        }
    }
}

// ------------------------------------------------------------------
// Task host: dispatches by kind, owns local interactive state.
// ------------------------------------------------------------------

@Composable
fun TaskHost(
    kind: Int,
    timerDuration: Float,
    onComplete: () -> Unit,
    onFail: (Int) -> Unit,
    onScheduleGo: (Int, () -> Unit) -> Unit,
    modifier: Modifier = Modifier,
) {
    val task = taskKindAt(kind)
    // Remount on kind+index via key outside
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        when (task) {
            TaskKind.Reaction -> ReactionTask(timerDuration, onComplete, onFail, onScheduleGo)
            TaskKind.Math -> MathTask(onComplete, onFail)
            TaskKind.Stroop -> StroopTask(onComplete, onFail)
            TaskKind.LetterType -> LetterTypeTask(onComplete, onFail)
            TaskKind.TapCount -> TapCountTask(onComplete)
            TaskKind.Extremes -> ExtremesTask(onComplete, onFail)
            TaskKind.GridSearch -> GridSearchTask(onComplete, onFail)
            TaskKind.LetterCase -> LetterCaseTask(onComplete, onFail)
            TaskKind.SolveX -> SolveXTask(onComplete, onFail)
            TaskKind.Shapes -> ShapesTask(onComplete, onFail)
            TaskKind.Arrow -> ArrowTask(onComplete, onFail)
            TaskKind.Sequence -> SequenceTask(onComplete, onFail)
            TaskKind.Swatch -> SwatchTask(onComplete, onFail)
            TaskKind.RapidClick -> RapidClickTask(onComplete)
            TaskKind.ButtonDir -> ButtonDirTask(onComplete, onFail)
            TaskKind.Parity -> ParityTask(onComplete, onFail)
            TaskKind.Drag -> DragTask(onComplete)
            TaskKind.Compare -> CompareTask(onComplete, onFail)
            TaskKind.EmojiCategory -> EmojiTask(onComplete, onFail)
            TaskKind.MissingDigit -> MissingDigitTask(onComplete, onFail)
            TaskKind.CircleSize -> CircleSizeTask(onComplete, onFail)
            TaskKind.OrderedTap -> OrderedTapTask(onComplete, onFail)
        }
    }
}

@Composable
private fun ReactionTask(
    timerDuration: Float,
    onComplete: () -> Unit,
    onFail: (Int) -> Unit,
    onScheduleGo: (Int, () -> Unit) -> Unit,
) {
    val spec = remember { generateReaction(timerDuration) }
    var go by remember { mutableStateOf(false) }

    LaunchedEffect(spec) {
        onScheduleGo(spec.goDelayMs) { go = true }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        ObjectiveHeader(
            text = if (spec.targetGreen) stringResource(R.string.stm_reaction_green)
            else stringResource(R.string.stm_reaction_blue),
        )
        Spacer(Modifier.height(16.dp))
        val bg = when {
            !go -> taskSurface()
            spec.targetGreen -> Green500
            else -> Color(0xFF3B82F6)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(128.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(bg)
                .border(2.dp, if (go) bg else taskBorder(), RoundedCornerShape(12.dp))
                .clickable {
                    if (go) onComplete()
                    else onFail(R.string.stm_fail_too_soon)
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                if (go) stringResource(R.string.stm_now) else stringResource(R.string.stm_steady),
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = if (go) Color.White else taskText(),
            )
        }
    }
}

@Composable
private fun MathTask(onComplete: () -> Unit, onFail: (Int) -> Unit) {
    val spec = remember { generateMath() }
    var input by remember { mutableStateOf("") }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        ObjectiveHeader(text = if (spec.add) stringResource(R.string.stm_find_sum) else stringResource(R.string.stm_find_difference))
        Spacer(Modifier.height(8.dp))
        ForceLtr { Text(spec.prompt, fontSize = 28.sp, fontWeight = FontWeight.Black, color = taskText()) }
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(taskSurface())
                .border(4.dp, taskBorder(), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                input.ifEmpty { "?" },
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = if (input.isEmpty()) taskMutedText() else taskText(),
            )
        }
        Spacer(Modifier.height(8.dp))
        TaskKeypad(onInput = { d ->
            val (next, outcome) = DigitAnswerState(input).press(d, spec.answer)
            input = next.input
            when (outcome) {
                is TaskOutcome.Complete -> onComplete()
                is TaskOutcome.Fail -> onFail(outcome.reasonRes)
                else -> Unit
            }
        })
    }
}

@Composable
private fun StroopTask(onComplete: () -> Unit, onFail: (Int) -> Unit) {
    val spec = remember { generateStroop() }
    // The word is drawn in the ink color (which differs from the word), as in the original Stroop task.
    val inkColor = Color(STROOP_COLORS.first { it.first == spec.inkName }.third)
    val wordLabel = stringResource(
        when (spec.word) {
            "Red" -> R.string.stm_color_red
            "Blue" -> R.string.stm_color_blue
            else -> R.string.stm_color_green
        },
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        ObjectiveHeader(
            text = if (spec.identifyFontColor) stringResource(R.string.stm_identify_font)
            else stringResource(R.string.stm_identify_word),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            wordLabel,
            fontSize = 40.sp,
            fontWeight = FontWeight.Black,
            color = inkColor,
            modifier = Modifier.padding(vertical = 8.dp),
        )
        Spacer(Modifier.height(8.dp))
        TaskChoiceRow(
            labels = listOf(
                "Red" to stringResource(R.string.stm_color_red),
                "Blue" to stringResource(R.string.stm_color_blue),
                "Green" to stringResource(R.string.stm_color_green),
            ),
            onPick = {
                when (val outcome = validateStroop(spec, it)) {
                    is TaskOutcome.Complete -> onComplete()
                    is TaskOutcome.Fail -> onFail(outcome.reasonRes)
                    else -> Unit
                }
            },
        )
    }
}

@Composable
private fun LetterTypeTask(onComplete: () -> Unit, onFail: (Int) -> Unit) {
    val spec = remember { generateLetterType() }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        ObjectiveHeader(
            text = if (spec.wantVowel) stringResource(R.string.stm_tap_any_vowel)
            else stringResource(R.string.stm_tap_any_consonant),
        )
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            spec.letters.forEach { char ->
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(taskSurface())
                        .border(2.dp, taskBorder(), RoundedCornerShape(6.dp))
                        .clickable {
                            when (val outcome = validateLetterType(spec, char)) {
                                is TaskOutcome.Complete -> onComplete()
                                is TaskOutcome.Fail -> onFail(outcome.reasonRes)
                                else -> Unit
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(char, fontSize = 20.sp, fontWeight = FontWeight.Black, color = taskText())
                }
            }
        }
    }
}

@Composable
private fun TapCountTask(onComplete: () -> Unit) {
    var spec by remember { mutableStateOf(generateTapCount()) }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        ObjectiveHeader(text = pluralStringResource(R.plurals.stm_tap_exactly_count, spec.needed, spec.needed))
        Spacer(Modifier.height(20.dp))
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(AccentBlue)
                .border(4.dp, taskBorder(), CircleShape)
                .clickable {
                    val (next, outcome) = tapCountStep(spec)
                    spec = next
                    if (outcome is TaskOutcome.Complete) onComplete()
                },
            contentAlignment = Alignment.Center,
        ) {
            Text("${spec.count}", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color.White)
        }
    }
}

@Composable
private fun ExtremesTask(onComplete: () -> Unit, onFail: (Int) -> Unit) {
    val spec = remember { generateExtremes() }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        ObjectiveHeader(
            text = if (spec.wantLargest) stringResource(R.string.stm_select_largest)
            else stringResource(R.string.stm_select_smallest),
        )
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            spec.numbers.forEach { n ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(taskSurface())
                        .border(2.dp, taskBorder(), RoundedCornerShape(12.dp))
                        .clickable {
                            when (val outcome = validateExtremes(spec, n)) {
                                is TaskOutcome.Complete -> onComplete()
                                is TaskOutcome.Fail -> onFail(outcome.reasonRes)
                                else -> Unit
                            }
                        }
                        .padding(horizontal = 28.dp, vertical = 16.dp),
                ) {
                    Text("$n", fontSize = 28.sp, fontWeight = FontWeight.Black, color = taskText())
                }
            }
        }
    }
}

@Composable
private fun GridSearchTask(onComplete: () -> Unit, onFail: (Int) -> Unit) {
    val spec = remember { generateGridSearch() }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        ObjectiveHeader(text = stringResource(R.string.stm_find_and_tap, spec.target))
        Spacer(Modifier.height(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            spec.cells.chunked(4).forEach { rowCells ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rowCells.forEach { cell ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(taskSurfaceVariant())
                                .border(2.dp, taskBorder(), RoundedCornerShape(6.dp))
                                .clickable {
                                    when (val outcome = validateGridSearch(spec, cell)) {
                                        is TaskOutcome.Complete -> onComplete()
                                        is TaskOutcome.Fail -> onFail(outcome.reasonRes)
                                        else -> Unit
                                    }
                                }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(cell, fontWeight = FontWeight.Bold, color = taskText())
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LetterCaseTask(onComplete: () -> Unit, onFail: (Int) -> Unit) {
    val spec = remember { generateLetterCase() }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        ObjectiveHeader(
            text = if (spec.wantUpper) stringResource(R.string.stm_click_any_upper)
            else stringResource(R.string.stm_click_any_lower),
        )
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            spec.chars.forEach { char ->
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(taskSurface())
                        .border(2.dp, taskBorder(), RoundedCornerShape(6.dp))
                        .clickable {
                            when (val outcome = validateLetterCase(spec, char)) {
                                is TaskOutcome.Complete -> onComplete()
                                is TaskOutcome.Fail -> onFail(outcome.reasonRes)
                                else -> Unit
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(char, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = taskText())
                }
            }
        }
    }
}

@Composable
private fun SolveXTask(onComplete: () -> Unit, onFail: (Int) -> Unit) {
    val spec = remember { generateSolveX() }
    var input by remember { mutableStateOf("") }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        ObjectiveHeader(text = stringResource(R.string.stm_solve_for_x))
        Spacer(Modifier.height(8.dp))
        ForceLtr {
            Text("X × ${spec.factor} = ${spec.product}", fontSize = 22.sp, fontWeight = FontWeight.Black, color = taskText())
        }
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(taskSurface())
                .border(4.dp, taskBorder(), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(input.ifEmpty { "?" }, fontSize = 22.sp, fontWeight = FontWeight.Black,
                color = if (input.isEmpty()) taskMutedText() else taskText())
        }
        Spacer(Modifier.height(8.dp))
        TaskKeypad(onInput = { d ->
            val (next, outcome) = DigitAnswerState(input).press(d, spec.xVal)
            input = next.input
            when (outcome) {
                is TaskOutcome.Complete -> onComplete()
                is TaskOutcome.Fail -> onFail(outcome.reasonRes)
                else -> Unit
            }
        })
    }
}

@Composable
private fun ShapesTask(onComplete: () -> Unit, onFail: (Int) -> Unit) {
    val spec = remember { generateShapes() }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        ObjectiveHeader(
            text = stringResource(
                R.string.stm_locate_shape,
                stringResource(
                    when (spec.target) {
                        "Circle" -> R.string.stm_shape_circle
                        "Square" -> R.string.stm_shape_square
                        else -> R.string.stm_shape_triangle
                    },
                ),
            ),
        )
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            spec.shapes.forEach { (name, icon) ->
                Text(
                    icon,
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Black,
                    color = taskText(),
                    modifier = Modifier
                        .clickable {
                            when (val outcome = validateShapes(spec, name)) {
                                is TaskOutcome.Complete -> onComplete()
                                is TaskOutcome.Fail -> onFail(outcome.reasonRes)
                                else -> Unit
                            }
                        }
                        .padding(8.dp),
                )
            }
        }
    }
}

@Composable
private fun ArrowTask(onComplete: () -> Unit, onFail: (Int) -> Unit) {
    val spec = remember { generateArrow() }
    val arrow = if (spec.left) "←" else "→"
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        ObjectiveHeader(
            text = if (spec.sameMode) stringResource(R.string.stm_arrow_same)
            else stringResource(R.string.stm_arrow_opposite),
        )
        Spacer(Modifier.height(12.dp))
        Text(arrow, fontSize = 56.sp, fontWeight = FontWeight.Black, color = taskText())
        Spacer(Modifier.height(12.dp))
        val sides = listOf(
            false to stringResource(R.string.stm_left),
            true to stringResource(R.string.stm_right),
        )
        // LEFT/RIGHT are physical sides: keep the LEFT button on the left in RTL too.
        ForceLtr {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                sides.forEach { (isRight, text) ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(taskSurfaceVariant())
                            .border(2.dp, taskBorder(), RoundedCornerShape(6.dp))
                            .clickable {
                                when (val outcome = validateArrow(spec, choseLeft = !isRight)) {
                                    is TaskOutcome.Complete -> onComplete()
                                    is TaskOutcome.Fail -> onFail(outcome.reasonRes)
                                    else -> Unit
                                }
                            }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(text, fontWeight = FontWeight.Black, color = taskText())
                    }
                }
            }
        }
    }
}

@Composable
private fun SequenceTask(onComplete: () -> Unit, onFail: (Int) -> Unit) {
    val spec = remember { generateSequence() }
    var input by remember { mutableStateOf("") }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        ObjectiveHeader(text = stringResource(R.string.stm_complete_sequence))
        Spacer(Modifier.height(8.dp))
        ForceLtr { Text(spec.prompt, fontSize = 22.sp, fontWeight = FontWeight.Black, color = taskText()) }
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(taskSurface())
                .border(4.dp, taskBorder(), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(input.ifEmpty { "?" }, fontSize = 22.sp, fontWeight = FontWeight.Black,
                color = if (input.isEmpty()) taskMutedText() else taskText())
        }
        Spacer(Modifier.height(8.dp))
        TaskKeypad(onInput = { d ->
            val (next, outcome) = DigitAnswerState(input).press(d, spec.answer)
            input = next.input
            when (outcome) {
                is TaskOutcome.Complete -> onComplete()
                is TaskOutcome.Fail -> onFail(outcome.reasonRes)
                else -> Unit
            }
        })
    }
}

@Composable
private fun SwatchTask(onComplete: () -> Unit, onFail: (Int) -> Unit) {
    val spec = remember { generateSwatch() }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        ObjectiveHeader(
            text = stringResource(
                R.string.stm_tap_swatch,
                stringResource(if (spec.target == "RED") R.string.stm_swatch_red else R.string.stm_swatch_blue),
            ),
        )
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            spec.order.forEach { name ->
                val color = if (name == "RED") Red500 else Color(0xFF3B82F6)
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(color)
                        .border(4.dp, taskBorder(), RoundedCornerShape(12.dp))
                        .clickable {
                            when (val outcome = validateSwatch(spec, name)) {
                                is TaskOutcome.Complete -> onComplete()
                                is TaskOutcome.Fail -> onFail(outcome.reasonRes)
                                else -> Unit
                            }
                        },
                )
            }
        }
    }
}

@Composable
private fun RapidClickTask(onComplete: () -> Unit) {
    var spec by remember { mutableStateOf(generateRapidClick()) }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        ObjectiveHeader(
            text = if (spec.target == 2) stringResource(R.string.stm_double_click)
            else stringResource(R.string.stm_triple_click),
        )
        Spacer(Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Purple600)
                .border(4.dp, taskBorder(), RoundedCornerShape(12.dp))
                .clickable {
                    val (next, outcome) = rapidClickStep(spec)
                    spec = next
                    if (outcome is TaskOutcome.Complete) onComplete()
                }
                .padding(horizontal = 40.dp, vertical = 24.dp),
        ) {
            Text(
                stringResource(R.string.stm_tap_count, spec.taps, spec.target),
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
            )
        }
    }
}

@Composable
private fun ButtonDirTask(onComplete: () -> Unit, onFail: (Int) -> Unit) {
    val spec = remember { generateButtonDir() }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        ObjectiveHeader(
            text = if (spec.targetRight) stringResource(R.string.stm_press_right)
            else stringResource(R.string.stm_press_left),
        )
        Spacer(Modifier.height(16.dp))
        val sides = listOf(
            false to stringResource(R.string.stm_left),
            true to stringResource(R.string.stm_right),
        )
        // LEFT/RIGHT are physical sides: keep the LEFT button on the left in RTL too.
        ForceLtr {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                sides.forEach { (isRight, text) ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(taskSurfaceVariant())
                            .border(2.dp, taskBorder(), RoundedCornerShape(6.dp))
                            .clickable {
                                when (val outcome = validateButtonDir(spec, choseRight = isRight)) {
                                    is TaskOutcome.Complete -> onComplete()
                                    is TaskOutcome.Fail -> onFail(outcome.reasonRes)
                                    else -> Unit
                                }
                            }
                            .padding(vertical = 28.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(text, fontWeight = FontWeight.Black, color = taskText())
                    }
                }
            }
        }
    }
}

@Composable
private fun ParityTask(onComplete: () -> Unit, onFail: (Int) -> Unit) {
    val spec = remember { generateParity() }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        ObjectiveHeader(
            text = if (spec.wantEven) stringResource(R.string.stm_pick_even)
            else stringResource(R.string.stm_pick_odd),
        )
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            spec.numbers.forEach { n ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(taskSurface())
                        .border(2.dp, taskBorder(), RoundedCornerShape(6.dp))
                        .clickable {
                            when (val outcome = validateParity(spec, n)) {
                                is TaskOutcome.Complete -> onComplete()
                                is TaskOutcome.Fail -> onFail(outcome.reasonRes)
                                else -> Unit
                            }
                        }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("$n", fontSize = 18.sp, fontWeight = FontWeight.Black, color = taskText())
                }
            }
        }
    }
}

@Composable
private fun DragTask(onComplete: () -> Unit) {
    val spec = remember { generateDrag() }
    val label = when (spec.target) {
        DragCorner.TOP_LEFT -> stringResource(R.string.stm_corner_top_left)
        DragCorner.TOP_RIGHT -> stringResource(R.string.stm_corner_top_right)
        DragCorner.BOTTOM_LEFT -> stringResource(R.string.stm_corner_bottom_left)
        DragCorner.BOTTOM_RIGHT -> stringResource(R.string.stm_corner_bottom_right)
    }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var dragging by remember { mutableStateOf(false) }
    var areaSize by remember { mutableStateOf(IntSize.Zero) }
    val thresholdPx = with(LocalDensity.current) { DRAG_THRESHOLD_DP.dp.toPx() }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        ObjectiveHeader(text = stringResource(R.string.stm_drag_to, label))
        Spacer(Modifier.height(8.dp))
        // Corners are physical (top-left etc.) and drag deltas are absolute, so no mirroring here.
        ForceLtr {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(taskSurface())
                    .border(4.dp, taskBorder(), RoundedCornerShape(12.dp))
                    .onSizeChanged { areaSize = it },
            ) {
                // corner ghosts
                listOf(
                    Alignment.TopStart, Alignment.TopEnd, Alignment.BottomStart, Alignment.BottomEnd,
                ).forEach { align ->
                    Box(
                        modifier = Modifier
                            .align(align)
                            .padding(4.dp)
                            .size(32.dp)
                            .border(2.dp, taskBorder(), RoundedCornerShape(6.dp)),
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                        .size(40.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(AccentBlue)
                        .border(2.dp, taskBorder(), RoundedCornerShape(4.dp))
                        .pointerInput(spec) {
                            detectDragGestures(
                                onDragStart = { dragging = true },
                                onDragEnd = { dragging = false },
                                onDragCancel = { dragging = false },
                                onDrag = { change, drag ->
                                    change.consume()
                                    offsetX += drag.x
                                    offsetY += drag.y
                                    // Square starts centered in the drag area; check its center in area coords.
                                    val w = areaSize.width.toFloat()
                                    val h = areaSize.height.toFloat()
                                    val x = w / 2f + offsetX
                                    val y = h / 2f + offsetY
                                    if (w > 0f && h > 0f && validateDrag(spec, x, y, w, h, thresholdPx)) onComplete()
                                },
                            )
                        },
                )
            }
        }
    }
}

@Composable
private fun CompareTask(onComplete: () -> Unit, onFail: (Int) -> Unit) {
    val spec = remember { generateCompare() }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        ObjectiveHeader(
            text = if (spec.greater) stringResource(R.string.stm_compare_greater, spec.v1, spec.v2)
            else stringResource(R.string.stm_compare_lesser, spec.v1, spec.v2),
        )
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
            listOf(true to stringResource(R.string.stm_yes), false to stringResource(R.string.stm_no)).forEach { (isYes, text) ->
                val bg = if (isYes) Green100 else Red100
                val fg = if (isYes) Color(0xFF166534) else Color(0xFF991B1B)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(bg)
                        .border(2.dp, taskBorder(), RoundedCornerShape(12.dp))
                        .clickable {
                            when (val outcome = validateCompare(spec, isYes)) {
                                is TaskOutcome.Complete -> onComplete()
                                is TaskOutcome.Fail -> onFail(outcome.reasonRes)
                                else -> Unit
                            }
                        }
                        .padding(vertical = 20.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text, fontWeight = FontWeight.Black, color = fg)
                }
            }
        }
    }
}

@Composable
private fun EmojiTask(onComplete: () -> Unit, onFail: (Int) -> Unit) {
    val spec = remember { generateEmoji() }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        ObjectiveHeader(
            text = if (spec.wantFruit) stringResource(R.string.stm_find_any_fruit)
            else stringResource(R.string.stm_find_any_animal),
        )
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            spec.choices.forEach { emoji ->
                Text(
                    emoji,
                    fontSize = 44.sp,
                    modifier = Modifier
                        .clickable {
                            when (val outcome = validateEmoji(spec, emoji)) {
                                is TaskOutcome.Complete -> onComplete()
                                is TaskOutcome.Fail -> onFail(outcome.reasonRes)
                                else -> Unit
                            }
                        }
                        .padding(4.dp),
                )
            }
        }
    }
}

@Composable
private fun MissingDigitTask(onComplete: () -> Unit, onFail: (Int) -> Unit) {
    val spec = remember { generateMissingDigit() }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        ObjectiveHeader(text = stringResource(R.string.stm_missing_digit))
        Spacer(Modifier.height(16.dp))
        ForceLtr { Text(spec.display, fontSize = 36.sp, fontWeight = FontWeight.Black, color = taskText()) }
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            spec.options.forEach { opt ->
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(taskSurfaceVariant())
                        .border(2.dp, taskBorder(), RoundedCornerShape(6.dp))
                        .clickable {
                            when (val outcome = validateMissingDigit(spec, opt)) {
                                is TaskOutcome.Complete -> onComplete()
                                is TaskOutcome.Fail -> onFail(outcome.reasonRes)
                                else -> Unit
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("$opt", fontWeight = FontWeight.Black, color = taskText())
                }
            }
        }
    }
}

@Composable
private fun CircleSizeTask(onComplete: () -> Unit, onFail: (Int) -> Unit) {
    val spec = remember { generateCircleSize() }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        ObjectiveHeader(
            text = when (spec.target) {
                CircleSizeTarget.BIGGEST -> stringResource(R.string.stm_tap_biggest)
                CircleSizeTarget.SMALLEST -> stringResource(R.string.stm_tap_smallest)
                CircleSizeTarget.MEDIUM -> stringResource(R.string.stm_tap_medium)
            },
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            spec.order.forEach { id ->
                val size = when (id) {
                    CircleId.S -> 24.dp
                    CircleId.M -> 40.dp
                    CircleId.L -> 64.dp
                }
                Box(
                    modifier = Modifier
                        .size(size)
                        .clip(CircleShape)
                        .background(Indigo600)
                        .border(2.dp, taskBorder(), CircleShape)
                        .clickable {
                            when (val outcome = validateCircleSize(spec, id)) {
                                is TaskOutcome.Complete -> onComplete()
                                is TaskOutcome.Fail -> onFail(outcome.reasonRes)
                                else -> Unit
                            }
                        },
                )
            }
        }
    }
}

@Composable
private fun OrderedTapTask(onComplete: () -> Unit, onFail: (Int) -> Unit) {
    var spec by remember { mutableStateOf(generateOrderedTap()) }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        ObjectiveHeader(
            // LTR isolate so "1 → 2 → 3" is not reordered inside an RTL sentence.
            text = stringResource(R.string.stm_tap_in_order, "\u2066" + spec.order.joinToString(" → ") + "\u2069"),
        )
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            spec.buttons.forEach { n ->
                val done = n in spec.completed
                val bg = if (done) Green100 else taskSurface()
                val border = if (done) Green500 else taskBorder()
                val fg = if (done) Color(0xFF166534) else taskText()
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(bg)
                        .border(4.dp, border, RoundedCornerShape(12.dp))
                        .clickable {
                            val (next, outcome) = orderedTapStep(spec, n)
                            spec = next
                            when (outcome) {
                                is TaskOutcome.Complete -> onComplete()
                                is TaskOutcome.Fail -> onFail(outcome.reasonRes)
                                else -> Unit
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("$n", fontSize = 22.sp, fontWeight = FontWeight.Black, color = fg)
                }
            }
        }
    }
}
