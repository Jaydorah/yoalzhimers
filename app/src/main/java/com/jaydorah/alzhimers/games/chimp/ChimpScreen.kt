package com.jaydorah.alzhimers.games.chimp

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.math.roundToInt
import com.jaydorah.alzhimers.R
import com.jaydorah.alzhimers.ui.components.YBadge
import com.jaydorah.alzhimers.ui.components.YButton
import com.jaydorah.alzhimers.ui.components.YButtonVariant
import com.jaydorah.alzhimers.ui.components.YCard
import com.jaydorah.alzhimers.ui.theme.TimerGreen
import com.jaydorah.alzhimers.ui.theme.TimerRed
import com.jaydorah.alzhimers.ui.theme.TimerYellow

private const val GRID_COLS = 5

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
fun ChimpScreen(
    onHome: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChimpViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()

    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.onAppBackgrounded() }
    LifecycleEventEffect(Lifecycle.Event.ON_START) { viewModel.onAppForegrounded() }

    BackHandler {
        when (state.phase) {
            ChimpPhase.Options -> onHome()
            else -> viewModel.backToOptions()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        when (state.phase) {
            ChimpPhase.Options -> OptionsContent(
                state = state,
                onSetStartingNumber = viewModel::setStartingNumber,
                onSetDifficulty = viewModel::setDifficulty,
                onStart = viewModel::startGame,
                onHome = onHome,
            )

            ChimpPhase.Playing -> PlayingContent(
                state = state,
                onCellTap = viewModel::onCellTap,
                onQuit = viewModel::backToOptions,
            )

            ChimpPhase.RoundResult -> RoundResultContent(
                state = state,
                onNext = viewModel::nextRound,
                onOptions = viewModel::backToOptions,
                onHome = {
                    viewModel.backToOptions()
                    onHome()
                },
            )

            ChimpPhase.GameOver -> GameOverContent(
                state = state,
                onPlayAgain = viewModel::playAgain,
                onOptions = viewModel::backToOptions,
                onHome = {
                    viewModel.backToOptions()
                    onHome()
                },
            )
        }
    }
}

@Composable
private fun OptionsContent(
    state: ChimpState,
    onSetStartingNumber: (Int) -> Unit,
    onSetDifficulty: (ChimpDifficulty) -> Unit,
    onStart: () -> Unit,
    onHome: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopBar(stringResource(R.string.game_chimp), onBack = onHome)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                stringResource(R.string.chimp_how_to_play),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text(
                stringResource(R.string.chimp_difficulty),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )

            ChimpDifficulty.entries.forEach { d ->
                val selected = state.difficulty == d
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
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(
                                stringResource(d.labelRes),
                                style = MaterialTheme.typography.titleMedium,
                                color = if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                stringResource(d.descriptionRes),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (selected) {
                            YBadge(text = stringResource(R.string.chimp_selected))
                        }
                    }
                }
            }

            Text(
                stringResource(R.string.chimp_starting_number),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )

            YCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        pluralStringResource(R.plurals.chimp_numbers_count, state.startingNumber, state.startingNumber),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Slider(
                        value = state.startingNumber.toFloat(),
                        onValueChange = { onSetStartingNumber(it.roundToInt()) },
                        valueRange = CHIMP_MIN_START.toFloat()..CHIMP_MAX_START.toFloat(),
                        steps = CHIMP_MAX_START - CHIMP_MIN_START - 1,
                    )
                    Text(
                        stringResource(R.string.chimp_starting_sub, CHIMP_MAX_NUMBERS),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
private fun PlayingContent(
    state: ChimpState,
    onCellTap: (Int) -> Unit,
    onQuit: () -> Unit,
) {
    val viewFraction by animateFloatAsState(
        targetValue = state.viewFraction,
        animationSpec = tween(durationMillis = 80),
        label = "viewTimer",
    )
    val viewColor by animateColorAsState(
        targetValue = chimpTimerColor(state.viewFraction),
        animationSpec = tween(durationMillis = 200),
        label = "viewColor",
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopBar(
                title = stringResource(R.string.game_chimp),
                onBack = onQuit,
                actions = {
                    if (state.roundsCleared > 0) {
                        YBadge(
                            text = pluralStringResource(
                                R.plurals.chimp_cleared_count,
                                state.roundsCleared,
                                state.roundsCleared,
                            ),
                        )
                    }
                },
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Hearts centered at the top — lost hearts empty right → left.
                StrikesHearts(strikes = state.strikes)

                // Memorize bar centered at the top while numbers are visible.
                val viewRemaining = state.viewWindowRemainingMs
                if (state.numbersVisible && viewRemaining != null) {
                    Spacer(Modifier.height(8.dp))
                    MemorizeBar(
                        fraction = viewFraction,
                        color = viewColor,
                        secondsLeft = ((viewRemaining + 999) / 1000).toInt(),
                        modifier = Modifier.fillMaxWidth(0.7f),
                    )
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    stringResource(R.string.chimp_find, state.nextExpected),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    pluralStringResource(R.plurals.chimp_numbers_count, state.currentCount, state.currentCount),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )

                Spacer(Modifier.weight(1f))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    state.grid.chunked(GRID_COLS).forEachIndexed { rowIndex, row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            row.forEachIndexed { colIndex, number ->
                                val index = rowIndex * GRID_COLS + colIndex
                                ChimpCellBox(
                                    label = if (state.showNumberInCell(number)) number.toString() else "",
                                    found = number != 0 && state.isCellFound(number),
                                    onTap = { onCellTap(index) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StrikesHearts(strikes: Int, modifier: Modifier = Modifier) {
    // Lose hearts from the right first: rightmost goes white on first strike.
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(CHIMP_MAX_STRIKES) { i ->
            val lost = i >= CHIMP_MAX_STRIKES - strikes
            Text(
                text = if (lost) "🤍" else "❤️",
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
private fun MemorizeBar(
    fraction: Float,
    color: Color,
    secondsLeft: Int,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(999.dp)),
        ) {
            // Fill anchored left; drains right → left as time runs out.
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction)
                    .align(Alignment.CenterStart)
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        Brush.horizontalGradient(listOf(color.copy(alpha = 0.85f), color)),
                    ),
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.chimp_memorize, secondsLeft),
            style = MaterialTheme.typography.labelMedium,
            color = color,
        )
    }
}

private fun chimpTimerColor(fraction: Float): Color = when {
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

@Composable
private fun ChimpCellBox(
    label: String,
    found: Boolean,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(shape)
            .background(
                if (found) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                else MaterialTheme.colorScheme.surface
            )
            .border(1.dp, MaterialTheme.colorScheme.outline, shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onTap,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun RoundResultContent(
    state: ChimpState,
    onNext: () -> Unit,
    onOptions: () -> Unit,
    onHome: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        Text(
            stringResource(R.string.chimp_round_clear),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(16.dp))
        YCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    "${state.roundsCleared}",
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    pluralStringResource(R.plurals.chimp_rounds_cleared_label, state.roundsCleared),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    pluralStringResource(R.plurals.chimp_next_numbers_count, state.currentCount, state.currentCount),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                StrikesHearts(strikes = state.strikes)
            }
        }
        Spacer(Modifier.weight(1f))
        YButton(
            text = stringResource(R.string.btn_continue),
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(52.dp),
        )
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

@Composable
private fun GameOverContent(
    state: ChimpState,
    onPlayAgain: () -> Unit,
    onOptions: () -> Unit,
    onHome: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopBar(stringResource(R.string.top_result), onBack = onHome)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))
            Text(
                stringResource(R.string.chimp_game_over),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(16.dp))
            YCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        "${state.score}",
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        pluralStringResource(R.plurals.chimp_rounds_cleared_label, state.score),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            YButton(
                text = stringResource(R.string.chimp_play_again),
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
