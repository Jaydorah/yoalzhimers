package com.jaydorah.alzhimers.games.mentalmath

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.material3.LocalTextStyle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jaydorah.alzhimers.R
import com.jaydorah.alzhimers.ui.components.YBadge
import com.jaydorah.alzhimers.ui.components.YButton
import com.jaydorah.alzhimers.ui.components.YButtonVariant
import com.jaydorah.alzhimers.ui.components.YCard
import com.jaydorah.alzhimers.ui.components.YSeparator
import com.jaydorah.alzhimers.ui.theme.TimerGreen
import com.jaydorah.alzhimers.ui.theme.TimerRed
import com.jaydorah.alzhimers.ui.theme.TimerYellow

@Composable
fun MentalMathScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MentalMathViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()

    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.onAppBackgrounded() }
    LifecycleEventEffect(Lifecycle.Event.ON_START) { viewModel.onAppForegrounded() }

    BackHandler {
        if (viewModel.state.value.phase != GamePhase.SelectDifficulty) {
            viewModel.quit()
        }
        onBack()
    }

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        when (state.phase) {
            GamePhase.SelectDifficulty -> OptionsScreen(
                state = state,
                onSetDifficulty = viewModel::setDifficulty,
                onSetCustomOperations = viewModel::setCustomOperations,
                onSetCustomStepSeconds = viewModel::setCustomStepSeconds,
                onToggleRoundNumbers = viewModel::toggleRoundNumbers,
                onToggleAllowPause = viewModel::toggleAllowPause,
                onToggleOp = viewModel::toggleOp,
                onStart = viewModel::startGame,
                onBack = onBack,
            )

            GamePhase.Playing, GamePhase.Paused -> RoundContent(
                state = state,
                onPause = viewModel::pause,
                onResume = viewModel::resume,
                onQuit = { viewModel.quit(); onBack() },
            )

            GamePhase.Answer -> AnswerContent(
                state = state,
                onDigit = viewModel::onInputDigit,
                onBackspace = viewModel::onBackspace,
                onSubmit = viewModel::submitAnswer,
                onQuit = { viewModel.quit(); onBack() },
            )

            GamePhase.Result -> ResultContent(
                state = state,
                onPlayAgain = viewModel::playAgain,
                onChangeDifficulty = viewModel::backToOptions,
                onHome = { viewModel.quit(); onBack() },
            )
        }
    }
}

@Composable
private fun PauseGlyph(tint: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(6.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(tint),
        )
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(6.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(tint),
        )
    }
}

@Composable
private fun TopBar(
    title: String,
    onBack: () -> Unit,
    actions: @Composable () -> Unit = {},
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.content_desc_back),
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = 4.dp),
        )
        Spacer(Modifier.weight(1f))
        actions()
    }
}

@Composable
private fun OptionsScreen(
    state: MentalMathState,
    onSetDifficulty: (Difficulty) -> Unit,
    onSetCustomOperations: (Int) -> Unit,
    onSetCustomStepSeconds: (Int) -> Unit,
    onToggleRoundNumbers: () -> Unit,
    onToggleAllowPause: () -> Unit,
    onToggleOp: (OpType) -> Unit,
    onStart: () -> Unit,
    onBack: () -> Unit,
) {
    val settings = state.settings

    Column(modifier = Modifier.fillMaxSize()) {
        TopBar(stringResource(R.string.game_mental_math), onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                stringResource(R.string.mm_game_options),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )

            // Difficulty first
            Text(
                stringResource(R.string.mm_difficulty),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Difficulty.entries.forEach { d ->
                val selected = settings.difficulty == d
                YCard(
                    onClick = { onSetDifficulty(d) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (selected) Modifier.border(
                                    2.dp,
                                    MaterialTheme.colorScheme.primary,
                                    RoundedCornerShape(12.dp),
                                ) else Modifier
                            )
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(d.labelRes),
                                style = MaterialTheme.typography.titleLarge,
                                color = if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface,
                            )
                            val desc = if (d == Difficulty.Custom) {
                                pluralStringResource(
                                    R.plurals.mm_desc_ops_count,
                                    settings.customOperations,
                                    settings.customOperations,
                                    settings.customStepSeconds,
                                )
                            } else {
                                pluralStringResource(
                                    R.plurals.mm_desc_ops_count,
                                    d.operations,
                                    d.operations,
                                    d.stepSeconds,
                                )
                            }
                            Text(
                                desc,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (selected) {
                            YBadge(
                                text = stringResource(R.string.mm_selected),
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                    }
                }
            }

            // Custom sliders
            if (settings.difficulty == Difficulty.Custom) {
                YCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            stringResource(R.string.mm_operations_count, settings.customOperations),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Slider(
                            value = settings.customOperations.toFloat(),
                            onValueChange = { onSetCustomOperations(it.roundToInt()) },
                            valueRange = 1f..10f,
                            steps = 8,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.mm_seconds_per_step, settings.customStepSeconds),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Slider(
                            value = settings.customStepSeconds.toFloat(),
                            onValueChange = { onSetCustomStepSeconds(it.roundToInt()) },
                            valueRange = 2f..15f,
                            steps = 12,
                        )
                    }
                }
            }

            // Operations
            Text(
                stringResource(R.string.mm_operations),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            YCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OpType.entries.forEach { op ->
                        val selected = op in settings.enabledOps
                        YButton(
                            text = stringResource(op.labelRes),
                            onClick = { onToggleOp(op) },
                            variant = if (selected) YButtonVariant.Default else YButtonVariant.Outline,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            // Round numbers + pause
            YCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(4.dp)) {
                    OptionRow(
                        title = stringResource(R.string.mm_round_numbers),
                        subtitle = stringResource(R.string.mm_round_numbers_sub),
                        checked = settings.roundNumbers,
                        onToggle = onToggleRoundNumbers,
                    )
                    YSeparator()
                    OptionRow(
                        title = stringResource(R.string.mm_allow_pause),
                        subtitle = stringResource(R.string.mm_allow_pause_sub),
                        checked = settings.allowPause,
                        onToggle = onToggleAllowPause,
                    )
                }
            }

            Spacer(Modifier.height(4.dp))
        }

        // Start button pinned to bottom
        YButton(
            text = stringResource(R.string.btn_start),
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .height(52.dp),
        )
    }
}

@Composable
private fun OptionRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                checkedBorderColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                uncheckedBorderColor = MaterialTheme.colorScheme.outline,
                disabledCheckedThumbColor = MaterialTheme.colorScheme.onPrimary,
                disabledCheckedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                disabledCheckedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                disabledUncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledUncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                disabledUncheckedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
            ),
        )
    }
}

@Composable
private fun RoundContent(
    state: MentalMathState,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onQuit: () -> Unit,
) {
    val round = state.round ?: return
    val fraction by animateFloatAsState(
        targetValue = state.stepFraction,
        animationSpec = tween(durationMillis = 80),
        label = "timer",
    )
    val barColor by animateColorAsState(
        targetValue = timerColor(state.stepFraction),
        animationSpec = tween(durationMillis = 200),
        label = "barColor",
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopBar(
                title = stringResource(R.string.game_mental_math),
                onBack = onQuit,
                actions = {
                    Text(
                        stringResource(R.string.mm_step, state.stepIndex + 1, round.totalSteps),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 4.dp),
                    )
                    if (state.settings.allowPause) {
                        val pauseDesc = stringResource(R.string.mm_pause)
                        IconButton(onClick = onPause) {
                            PauseGlyph(
                                tint = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier
                                    .size(22.dp)
                                    .semantics { contentDescription = pauseDesc },
                            )
                        }
                    }
                },
            )

            // Content area leaves room for the timer rail on the right.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 20.dp, end = 28.dp)
                    .then(
                        if (state.settings.allowPause && state.phase == GamePhase.Playing) {
                            Modifier.pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        awaitFirstDown(requireUnconsumed = false)
                                        onPause()
                                    }
                                }
                            }
                        } else Modifier
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        if (state.stepIndex == 0) stringResource(R.string.mm_starting_number)
                        else stringResource(R.string.mm_then),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(16.dp))
                    // "+ 5" must read operator-first in RTL locales too.
                    Text(
                        text = state.currentDisplay,
                        style = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr),
                        fontSize = 88.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        when {
                            state.stepIndex == 0 -> stringResource(R.string.mm_memorize)
                            state.stepIndex < round.totalSteps - 1 -> stringResource(R.string.mm_next_up)
                            else -> stringResource(R.string.mm_get_ready)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // Vertical timer rail, right edge; fill sits on the bottom and
        // shrinks downward as time runs out.
        TimerRail(
            fraction = fraction,
            color = barColor,
            secondsLeft = ((state.stepDurationMs - state.stepElapsedMs + 999) / 1000).toInt(),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 8.dp)
                .width(40.dp)
                .fillMaxHeight(0.62f),
        )

        if (state.phase == GamePhase.Paused) {
            PauseOverlay(onResume = onResume)
        }
    }
}

@Composable
private fun TimerRail(
    fraction: Float,
    color: Color,
    secondsLeft: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 11.dp),
        ) {
            // Track
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(999.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(999.dp)),
            )
            // Fill from bottom, height = remaining fraction → drains downward.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(fraction)
                    .align(Alignment.BottomCenter)
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(color.copy(alpha = 0.85f), color),
                        )
                    ),
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.mm_seconds, secondsLeft),
            style = MaterialTheme.typography.labelMedium,
            color = color,
        )
    }
}

private fun timerColor(fraction: Float): Color = when {
    fraction > 0.5f -> {
        val t = (1f - fraction) * 2f // 0 at full -> 1 at half
        lerpColor(TimerGreen, TimerYellow, t)
    }
    else -> {
        val t = (0.5f - fraction) * 2f // 0 at half -> 0 at empty
        lerpColor(TimerYellow, TimerRed, t.coerceIn(0f, 1f))
    }
}

private fun lerpColor(a: Color, b: Color, t: Float): Color {
    val c = t.coerceIn(0f, 1f)
    return Color(
        red = a.red + (b.red - a.red) * c,
        green = a.green + (b.green - a.green) * c,
        blue = a.blue + (b.blue - a.blue) * c,
        alpha = a.alpha + (b.alpha - a.alpha) * c,
    )
}

@Composable
private fun PauseOverlay(onResume: () -> Unit) {
    // YouTube-shorts style: dimmed screen, big play icon in the middle, tap to resume.
    val pausedDesc = stringResource(R.string.mm_paused_tap)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitFirstDown(requireUnconsumed = false)
                        onResume()
                    }
                }
            }
            .semantics { contentDescription = pausedDesc },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "▶",
                fontSize = 44.sp,
                color = Color.White,
            )
        }
    }
}

@Composable
private fun AnswerContent(
    state: MentalMathState,
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    onSubmit: () -> Unit,
    onQuit: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopBar(stringResource(R.string.top_your_answer), onBack = onQuit)

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.mm_final_question),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                    .padding(vertical = 22.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = state.input.ifEmpty { "0" },
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (state.input.isEmpty()) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else MaterialTheme.colorScheme.onSurface,
                )
            }

            Spacer(Modifier.height(20.dp))

            Keypad(onDigit = onDigit, onBackspace = onBackspace)

            Spacer(Modifier.height(16.dp))

            YButton(
                text = stringResource(R.string.btn_submit),
                onClick = onSubmit,
                enabled = state.input.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            )
        }
    }
}

@Composable
private fun Keypad(onDigit: (Char) -> Unit, onBackspace: () -> Unit) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("0"),
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        rows.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                row.forEach { key ->
                    KeypadKey(
                        label = key,
                        onClick = { onDigit(key.first()) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (row.size == 3) {
                    KeypadKey(
                        label = "",
                        onClick = onBackspace,
                        modifier = Modifier.weight(1f),
                        isBackspace = true,
                    )
                } else {
                    Spacer(Modifier.weight(1f))
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun KeypadKey(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isBackspace: Boolean = false,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.aspectRatio(1.6f),
    ) {
        Box(
            modifier = Modifier.fillMaxSize().border(
                1.dp,
                MaterialTheme.colorScheme.outline,
                RoundedCornerShape(12.dp),
            ),
            contentAlignment = Alignment.Center,
        ) {
            if (isBackspace) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.content_desc_delete),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            } else {
                Text(label, fontSize = 24.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun ResultContent(
    state: MentalMathState,
    onPlayAgain: () -> Unit,
    onChangeDifficulty: () -> Unit,
    onHome: () -> Unit,
) {
    val round = state.round ?: return

    Column(modifier = Modifier.fillMaxSize()) {
        TopBar(stringResource(R.string.top_result), onBack = onHome)

        Column(
            modifier = Modifier.fillMaxSize().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(12.dp))
            YCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = if (state.wasCorrect) stringResource(R.string.mm_correct)
                        else stringResource(R.string.mm_not_quite),
                        style = MaterialTheme.typography.headlineMedium,
                        color = if (state.wasCorrect) {
                            MaterialTheme.colorScheme.primary
                        } else MaterialTheme.colorScheme.error,
                    )
                    Text(
                        text = round.correctAnswer.toString(),
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (!state.wasCorrect && state.input.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.mm_you_answered, state.input),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            YCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        stringResource(R.string.mm_the_chain),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    val chain = buildString {
                        append(round.startNumber.toString())
                        round.operations.forEach { op ->
                            append("  ${op.symbol} ${op.operand}")
                        }
                        append("  = ${round.correctAnswer}")
                    }
                    Text(
                        chain,
                        style = MaterialTheme.typography.titleMedium.copy(textDirection = TextDirection.Ltr),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            YButton(
                text = stringResource(R.string.mm_play_again),
                onClick = onPlayAgain,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                YButton(
                    text = stringResource(R.string.btn_options),
                    onClick = onChangeDifficulty,
                    variant = YButtonVariant.Outline,
                    modifier = Modifier.weight(1f).height(48.dp),
                )
                YButton(
                    text = stringResource(R.string.btn_back),
                    onClick = onHome,
                    variant = YButtonVariant.Ghost,
                    modifier = Modifier.weight(1f).height(48.dp),
                )
            }
        }
    }
}
