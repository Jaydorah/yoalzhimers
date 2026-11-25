package com.jaydorah.alzhimers.games.numbermemory

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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jaydorah.alzhimers.R
import com.jaydorah.alzhimers.ui.components.YButton
import com.jaydorah.alzhimers.ui.components.YButtonVariant
import com.jaydorah.alzhimers.ui.components.YCard
import com.jaydorah.alzhimers.ui.theme.TimerGreen
import com.jaydorah.alzhimers.ui.theme.TimerRed
import com.jaydorah.alzhimers.ui.theme.TimerYellow

@Composable
fun NumberMemoryScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NumberMemoryViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()

    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.onAppBackgrounded() }
    LifecycleEventEffect(Lifecycle.Event.ON_START) { viewModel.onAppForegrounded() }

    BackHandler {
        if (viewModel.state.value.phase != NumberMemoryPhase.Options) {
            viewModel.quit()
        }
        onBack()
    }

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        when (state.phase) {
            NumberMemoryPhase.Options -> OptionsScreen(
                state = state,
                onSetStartingDigits = viewModel::setStartingDigits,
                onStart = viewModel::startGame,
                onBack = onBack,
            )

            NumberMemoryPhase.Showing -> ShowingContent(
                state = state,
                onQuit = { viewModel.quit(); onBack() },
            )

            NumberMemoryPhase.Answer -> AnswerContent(
                state = state,
                onDigit = viewModel::onInputDigit,
                onBackspace = viewModel::onBackspace,
                onSubmit = viewModel::submitAnswer,
                onQuit = { viewModel.quit(); onBack() },
            )

            NumberMemoryPhase.Result -> ResultContent(
                state = state,
                onNext = viewModel::nextLevel,
                onRetry = viewModel::playAgain,
                onOptions = viewModel::backToOptions,
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
    state: NumberMemoryState,
    onSetStartingDigits: (Int) -> Unit,
    onStart: () -> Unit,
    onBack: () -> Unit,
) {
    val settings = state.settings

    Column(modifier = Modifier.fillMaxSize()) {
        TopBar(stringResource(R.string.game_number_memory), onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                stringResource(R.string.nm_game_options),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Text(
                stringResource(R.string.nm_how_to_play),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            YCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        pluralStringResource(
                            R.plurals.nm_starting_digits_count,
                            settings.effectiveStartingDigits,
                            settings.effectiveStartingDigits,
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        stringResource(R.string.nm_starting_sub),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    Slider(
                        value = settings.effectiveStartingDigits.toFloat(),
                        onValueChange = { onSetStartingDigits(it.roundToInt()) },
                        valueRange = GameSettings.MIN_DIGITS.toFloat()..GameSettings.MAX_DIGITS.toFloat(),
                        steps = GameSettings.MAX_DIGITS - GameSettings.MIN_DIGITS - 1,
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
private fun ShowingContent(
    state: NumberMemoryState,
    onQuit: () -> Unit,
) {
    val round = state.round ?: return
    val fraction by animateFloatAsState(
        targetValue = state.showFraction,
        animationSpec = tween(durationMillis = 80),
        label = "timer",
    )
    val barColor by animateColorAsState(
        targetValue = timerColor(state.showFraction),
        animationSpec = tween(durationMillis = 200),
        label = "barColor",
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopBar(
                title = stringResource(R.string.game_number_memory),
                onBack = onQuit,
                actions = {
                    Text(
                        pluralStringResource(R.plurals.nm_digits_count, round.digits, round.digits),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 4.dp),
                    )
                },
            )

            Box(
                modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        stringResource(R.string.nm_memorize),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = round.number,
                        fontSize = 72.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        stringResource(R.string.nm_next_type),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // Horizontal timer rail along the bottom.
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(999.dp)),
            ) {
                // Fill anchored left; width = remaining, so it drains right → left.
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction)
                        .align(Alignment.CenterStart)
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            Brush.horizontalGradient(listOf(barColor.copy(alpha = 0.85f), barColor)),
                        ),
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.nm_seconds, state.secondsLeft),
                style = MaterialTheme.typography.labelMedium,
                color = barColor,
            )
        }
    }
}

private fun timerColor(fraction: Float): Color = when {
    fraction > 0.5f -> lerpColor(TimerGreen, TimerYellow, (1f - fraction) * 2f)
    else -> lerpColor(TimerYellow, TimerRed, ((0.5f - fraction) * 2f).coerceIn(0f, 1f))
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
private fun AnswerContent(
    state: NumberMemoryState,
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
                stringResource(R.string.nm_question),
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
                    fontSize = 48.sp,
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
    state: NumberMemoryState,
    onNext: () -> Unit,
    onRetry: () -> Unit,
    onOptions: () -> Unit,
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
                        text = if (state.wasCorrect) stringResource(R.string.nm_correct)
                        else stringResource(R.string.nm_not_quite),
                        style = MaterialTheme.typography.headlineMedium,
                        color = if (state.wasCorrect) {
                            MaterialTheme.colorScheme.primary
                        } else MaterialTheme.colorScheme.error,
                    )
                    Text(
                        text = round.number,
                        fontSize = 56.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (!state.wasCorrect && state.input.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.nm_you_answered),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        // Digit-by-digit; wrong digits get struck through. Forced LTR so the
                        // digits keep reading left to right in RTL locales.
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                state.input.forEachIndexed { index, ch ->
                                    val expected = round.number.getOrNull(index)
                                    val wrong = expected == null || expected != ch
                                    Text(
                                        text = ch.toString(),
                                        fontSize = 34.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (wrong) {
                                            MaterialTheme.colorScheme.error
                                        } else MaterialTheme.colorScheme.onSurface,
                                        textDecoration = if (wrong) {
                                            TextDecoration.LineThrough
                                        } else null,
                                    )
                                }
                            }
                        }
                    }
                    Text(
                        text = pluralStringResource(
                            R.plurals.nm_level_digits_count,
                            state.levelDigits,
                            state.levelDigits,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            if (state.wasCorrect) {
                YButton(
                    text = stringResource(R.string.btn_continue),
                    onClick = onNext,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                )
            } else {
                YButton(
                    text = stringResource(R.string.btn_retry),
                    onClick = onRetry,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                YButton(
                    text = stringResource(R.string.btn_options),
                    onClick = onOptions,
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
