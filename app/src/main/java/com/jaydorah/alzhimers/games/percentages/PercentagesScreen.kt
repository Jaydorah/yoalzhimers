package com.jaydorah.alzhimers.games.percentages

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.jaydorah.alzhimers.ui.components.YOptionRow
import com.jaydorah.alzhimers.ui.components.YSeparator
import com.jaydorah.alzhimers.ui.theme.TimerGreen
import com.jaydorah.alzhimers.ui.theme.TimerRed
import com.jaydorah.alzhimers.ui.theme.TimerYellow

@Composable
fun PercentagesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PercentagesViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()

    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.onAppBackgrounded() }
    LifecycleEventEffect(Lifecycle.Event.ON_START) { viewModel.onAppForegrounded() }

    BackHandler {
        if (viewModel.state.value.phase != GamePhase.SelectDifficulty) viewModel.quit()
        onBack()
    }

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        when (state.phase) {
            GamePhase.SelectDifficulty -> OptionsScreen(
                state = state,
                onSetDifficulty = viewModel::setDifficulty,
                onSetCustomSeconds = viewModel::setCustomSeconds,
                onSetMinNumber = viewModel::setMinNumber,
                onSetMaxNumber = viewModel::setMaxNumber,
                onToggleRoundNumbers = viewModel::toggleRoundNumbers,
                onStart = viewModel::startGame,
                onBack = onBack,
            )

            GamePhase.Question -> QuestionContent(
                state = state,
                onDigit = viewModel::onInputDigit,
                onBackspace = viewModel::onBackspace,
                onSubmit = viewModel::submit,
                onQuit = { viewModel.quit(); onBack() },
            )

            GamePhase.Feedback -> FeedbackContent(
                state = state,
                onQuit = { viewModel.quit(); onBack() },
            )

            GamePhase.Result -> ResultContent(
                state = state,
                onPlayAgain = viewModel::playAgain,
                onChangeOptions = viewModel::backToOptions,
                onHome = { viewModel.quit(); onBack() },
            )
        }
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
    state: PercentagesState,
    onSetDifficulty: (Difficulty) -> Unit,
    onSetCustomSeconds: (Int) -> Unit,
    onSetMinNumber: (Int) -> Unit,
    onSetMaxNumber: (Int) -> Unit,
    onToggleRoundNumbers: () -> Unit,
    onStart: () -> Unit,
    onBack: () -> Unit,
) {
    val settings = state.settings
    var step by rememberSaveable { mutableIntStateOf(1) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopBar(stringResource(R.string.game_percentages), onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                stringResource(R.string.pct_game_options),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Text(
                stringResource(R.string.pct_difficulty),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Difficulty.entries.forEach { d ->
                val selected = settings.difficulty == d
                YCard(onClick = { onSetDifficulty(d) }, modifier = Modifier.fillMaxWidth()) {
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
                            Text(
                                text = when (d) {
                                    Difficulty.Custom -> stringResource(R.string.pct_desc_custom)
                                    Difficulty.Easy -> stringResource(R.string.pct_desc_easy)
                                    else -> pluralStringResource(
                                        R.plurals.pct_desc_seconds_count,
                                        d.secondsPerQuestion,
                                        d.secondsPerQuestion,
                                    )
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (selected) {
                            YBadge(
                                text = stringResource(R.string.pct_selected),
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                    }
                }
            }

            if (settings.difficulty == Difficulty.Custom) {
                YCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            stringResource(R.string.pct_seconds_per_question, settings.customSeconds),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Slider(
                            value = settings.customSeconds.toFloat(),
                            onValueChange = { onSetCustomSeconds(it.roundToInt()) },
                            valueRange = MIN_SECONDS.toFloat()..MAX_SECONDS.toFloat(),
                            steps = MAX_SECONDS - MIN_SECONDS - 1,
                        )
                    }
                }
            }

            Text(
                stringResource(R.string.pct_number_range),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            YCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(vertical = 12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            stringResource(R.string.pct_step),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 4.dp),
                        )
                        listOf(1, 10, 100, 1000).forEach { value ->
                            YButton(
                                text = value.toString(),
                                onClick = { step = value },
                                variant = if (step == value) YButtonVariant.Default else YButtonVariant.Outline,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    YSeparator()
                    NumberStepper(
                        title = stringResource(R.string.pct_min_number),
                        value = settings.minNumber,
                        step = step,
                        lowerBound = MIN_NUMBER,
                        upperBound = settings.maxNumber,
                        onChange = onSetMinNumber,
                    )
                    YSeparator()
                    NumberStepper(
                        title = stringResource(R.string.pct_max_number),
                        value = settings.maxNumber,
                        step = step,
                        lowerBound = settings.minNumber,
                        upperBound = MAX_NUMBER,
                        onChange = onSetMaxNumber,
                    )
                }
            }

            YCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(4.dp)) {
                    YOptionRow(
                        title = stringResource(R.string.pct_round_numbers),
                        subtitle = stringResource(R.string.pct_round_numbers_sub),
                        checked = settings.roundNumbers,
                        onToggle = onToggleRoundNumbers,
                    )
                }
            }

            Spacer(Modifier.height(4.dp))
        }

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
private fun NumberStepper(
    title: String,
    value: Int,
    step: Int,
    lowerBound: Int,
    upperBound: Int,
    onChange: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        StepKey(
            label = "−",
            enabled = value > lowerBound,
            onClick = { onChange(maxOf(lowerBound, value - step)) },
        )
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(88.dp),
        )
        StepKey(
            label = "+",
            enabled = value < upperBound,
            onClick = { onChange(minOf(upperBound, value + step)) },
        )
    }
}

@Composable
private fun StepKey(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.size(44.dp),
    ) {
        Box(
            modifier = Modifier.fillMaxSize().border(
                1.dp,
                MaterialTheme.colorScheme.outline,
                RoundedCornerShape(10.dp),
            ),
            contentAlignment = Alignment.Center,
        ) {
            Text(label, fontSize = 20.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun QuestionContent(
    state: PercentagesState,
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    onSubmit: () -> Unit,
    onQuit: () -> Unit,
) {
    val question = state.current ?: return
    val fraction by animateFloatAsState(
        targetValue = state.timeFraction,
        animationSpec = tween(durationMillis = 80),
        label = "timer",
    )
    val barColor by animateColorAsState(
        targetValue = timerColor(state.timeFraction),
        animationSpec = tween(durationMillis = 200),
        label = "barColor",
    )

    Column(modifier = Modifier.fillMaxSize()) {
        TopBar(
            title = stringResource(R.string.game_percentages),
            onBack = onQuit,
            actions = {
                Text(
                    stringResource(R.string.pct_question, state.index + 1, state.total),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 8.dp),
                )
            },
        )

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (state.settings.timed) {
                TimerBar(
                    fraction = fraction,
                    color = barColor,
                    secondsLeft = state.secondsLeft,
                )
                Spacer(Modifier.height(14.dp))
            } else {
                Spacer(Modifier.height(6.dp))
            }

            Text(
                text = stringResource(R.string.pct_prompt, question.percent, question.number),
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = state.input.ifEmpty { "0" },
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (state.input.isEmpty()) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
            }

            Spacer(Modifier.weight(1f))

            Keypad(onDigit = onDigit, onBackspace = onBackspace)

            Spacer(Modifier.height(12.dp))

            YButton(
                text = stringResource(R.string.btn_submit),
                onClick = onSubmit,
                enabled = state.input.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun TimerBar(
    fraction: Float,
    color: Color,
    secondsLeft: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(10.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(999.dp)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .background(color),
            )
        }
        Text(
            text = stringResource(R.string.pct_seconds, secondsLeft),
            style = MaterialTheme.typography.labelMedium,
            color = color,
        )
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
        modifier = modifier.aspectRatio(1.8f),
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
private fun FeedbackContent(
    state: PercentagesState,
    onQuit: () -> Unit,
) {
    val question = state.current ?: return

    Column(modifier = Modifier.fillMaxSize()) {
        TopBar(
            title = stringResource(R.string.top_your_answer),
            onBack = onQuit,
            actions = {
                Text(
                    stringResource(R.string.pct_question, state.index + 1, state.total),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 8.dp),
                )
            },
        )

        Column(
            modifier = Modifier.fillMaxSize().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(24.dp))
            YCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = when {
                            state.timedOut -> stringResource(R.string.pct_time_up)
                            state.wasCorrect -> stringResource(R.string.pct_correct)
                            else -> stringResource(R.string.pct_not_quite)
                        },
                        style = MaterialTheme.typography.headlineMedium,
                        color = if (state.wasCorrect) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                    )
                    Text(
                        text = stringResource(R.string.pct_prompt, question.percent, question.number),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = question.answer.toString(),
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (!state.wasCorrect && state.input.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.pct_you_answered, state.input),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultContent(
    state: PercentagesState,
    onPlayAgain: () -> Unit,
    onChangeOptions: () -> Unit,
    onHome: () -> Unit,
) {
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
                        text = stringResource(R.string.pct_score_out_of, state.correctCount, state.total),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = state.correctCount.toString(),
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    if (state.bestStreak > 1) {
                        Text(
                            text = stringResource(R.string.pct_result_streak, state.bestStreak),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            YButton(
                text = stringResource(R.string.pct_play_again),
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
                    onClick = onChangeOptions,
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

private fun timerColor(fraction: Float): Color = when {
    fraction > 0.5f -> {
        val t = (1f - fraction) * 2f
        lerpColor(TimerGreen, TimerYellow, t)
    }
    else -> {
        val t = (0.5f - fraction) * 2f
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
