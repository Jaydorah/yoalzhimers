package com.jaydorah.alzhimers.games.schulte

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.annotation.StringRes
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jaydorah.alzhimers.R
import com.jaydorah.alzhimers.ui.components.YBadge
import com.jaydorah.alzhimers.ui.components.YButton
import com.jaydorah.alzhimers.ui.components.YButtonVariant
import com.jaydorah.alzhimers.ui.components.YCard

private fun spinSpeedLabelRes(speed: SpinSpeed): Int = when (speed) {
    SpinSpeed.Off -> R.string.sch_spin_off
    SpinSpeed.Low -> R.string.sch_spin_low
    SpinSpeed.Medium -> R.string.sch_spin_medium
    SpinSpeed.High -> R.string.sch_spin_high
    SpinSpeed.ReverseLow -> R.string.sch_spin_ccw_low
    SpinSpeed.ReverseMedium -> R.string.sch_spin_ccw_medium
    SpinSpeed.ReverseHigh -> R.string.sch_spin_ccw_high
}

private val GroupColorsDefault = listOf(
    Color(0xFF2563EB),
    Color(0xFF16A34A),
    Color(0xFFD97706),
    Color(0xFFDC2626),
    Color(0xFFDB2777),
)
private val GroupColorsOriginal = listOf(
    Color(0xFF16A34A),
    Color(0xFFDC2626),
    Color(0xFF2563EB),
    Color(0xFFDB2777),
    Color(0xFF92400E),
)

private val CorrectGreen = Color(0xFFB5FCAC)
private val WrongRed = Color(0xFFF4ADAD)

@Composable
fun SchulteScreen(
    onHome: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SchulteViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> viewModel.onAppBackground()
                Lifecycle.Event.ON_START -> viewModel.onAppForeground()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    BackHandler {
        when (state.phase) {
            SchultePhase.Options -> onHome()
            else -> viewModel.backToOptions()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        when (state.phase) {
            SchultePhase.Options -> OptionsContent(
                state = state,
                onUpdate = viewModel::updateSettings,
                onStart = viewModel::startGame,
                onBack = onHome,
            )

            SchultePhase.Playing, SchultePhase.RoundBreak -> PlayingContent(
                state = state,
                onCellTap = viewModel::onCellTap,
                onNextRound = viewModel::nextRound,
                onQuit = viewModel::backToOptions,
            )

            SchultePhase.Result -> ResultContent(
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
private fun OptionsContent(
    state: SchulteState,
    onUpdate: ((SchulteSettings) -> SchulteSettings) -> Unit,
    onStart: () -> Unit,
    onBack: () -> Unit,
) {
    val s = state.settings
    Column(modifier = Modifier.fillMaxSize()) {
        TopBar(stringResource(R.string.game_schulte), onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(R.string.sch_how_to_play),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            SettingsSection(stringResource(R.string.sch_board)) {
                ChoiceRow(
                    label = stringResource(R.string.sch_grid),
                    options = (SCHULTE_MIN_GRID..SCHULTE_MAX_GRID).map { "$it×$it" },
                    selectedIndex = s.gridSize - SCHULTE_MIN_GRID,
                    onSelect = { i -> onUpdate { it.copy(gridSize = coerceGridSize(SCHULTE_MIN_GRID + i)) } },
                )
                ChoiceRow(
                    label = stringResource(R.string.sch_rounds),
                    options = listOf("1", "2", "3", "5", "10"),
                    selectedIndex = listOf(1, 2, 3, 5, 10).indexOf(s.rounds).coerceAtLeast(0),
                    onSelect = { i -> onUpdate { it.copy(rounds = listOf(1, 2, 3, 5, 10)[i]) } },
                )
                ChoiceRow(
                    label = stringResource(R.string.sch_groups),
                    options = listOf("1", "1c", "2", "3", "4", "5"),
                    selectedIndex = when {
                        s.groupCount == 1 && s.colorGroups -> 1
                        s.groupCount <= 1 -> 0
                        else -> s.groupCount.coerceAtMost(5)
                    },
                    onSelect = { i ->
                        onUpdate {
                            when (i) {
                                0 -> it.copy(groupCount = 1, colorGroups = false)
                                1 -> it.copy(groupCount = 1, colorGroups = true)
                                else -> it.copy(groupCount = i, colorGroups = false)
                            }
                        }
                    },
                )
                ToggleRow(stringResource(R.string.sch_toggle_inverse), s.inverseCount) {
                    onUpdate { it.copy(inverseCount = !it.inverseCount) }
                }
                ToggleRow(stringResource(R.string.sch_toggle_divergent), s.divergentCount) {
                    onUpdate { it.copy(divergentCount = !it.divergentCount) }
                }
                ToggleRow(stringResource(R.string.sch_toggle_various), s.variousCounts) {
                    onUpdate { it.copy(variousCounts = !it.variousCounts) }
                }
                ToggleRow(stringResource(R.string.sch_toggle_collate), s.collateGroups) {
                    onUpdate { it.copy(collateGroups = !it.collateGroups) }
                }
                ToggleRow(
                    stringResource(R.string.sch_toggle_round_breaks),
                    s.roundBreaks && s.rounds > 1,
                    enabled = s.rounds > 1,
                ) {
                    onUpdate { it.copy(roundBreaks = !it.roundBreaks) }
                }
                ToggleRow(
                    stringResource(R.string.sch_toggle_show_round),
                    s.showRounds && s.rounds > 1,
                    enabled = s.rounds > 1,
                ) {
                    onUpdate { it.copy(showRounds = !it.showRounds) }
                }
                ToggleRow(stringResource(R.string.sch_toggle_colored), s.colorGroups) {
                    onUpdate { it.copy(colorGroups = !it.colorGroups, groupCount = if (!s.colorGroups) maxOf(s.groupCount, 1) else 1) }
                }
            }

            SettingsSection(stringResource(R.string.sch_play)) {
                ToggleRow(stringResource(R.string.sch_toggle_empty_correct), s.clearCorrect) {
                    onUpdate { it.copy(clearCorrect = !it.clearCorrect) }
                }
                ToggleRow(stringResource(R.string.sch_toggle_show_correct), s.showTrace) {
                    onUpdate { it.copy(showTrace = !it.showTrace) }
                }
                ToggleRow(stringResource(R.string.sch_toggle_tap_result), s.showClickResult) {
                    onUpdate { it.copy(showClickResult = !it.showClickResult) }
                }
                ToggleRow(stringResource(R.string.sch_toggle_no_errors), s.noErrors) {
                    onUpdate { it.copy(noErrors = !it.noErrors) }
                }
                ToggleRow(stringResource(R.string.sch_toggle_start_timer), s.startOnClick) {
                    onUpdate { it.copy(startOnClick = !it.startOnClick) }
                }
                ToggleRow(stringResource(R.string.sch_toggle_shuffle), s.shuffleSymbols) {
                    onUpdate { it.copy(shuffleSymbols = !it.shuffleSymbols) }
                }
                ToggleRow(stringResource(R.string.sch_toggle_center_dot), s.showCenterDot) {
                    onUpdate { it.copy(showCenterDot = !it.showCenterDot) }
                }
            }

            SettingsSection(stringResource(R.string.sch_visual)) {
                ToggleRow(stringResource(R.string.sch_toggle_turn), s.turnSymbols) {
                    onUpdate { it.copy(turnSymbols = !it.turnSymbols) }
                }
                ToggleRow(stringResource(R.string.sch_toggle_spin_numbers), s.spinSymbols) {
                    onUpdate { it.copy(spinSymbols = !it.spinSymbols) }
                }
                ToggleRow(stringResource(R.string.sch_toggle_spin_table), s.spinTable) {
                    onUpdate { it.copy(spinTable = !it.spinTable) }
                }
                if (s.spinTable) {
                    val speeds = SpinSpeed.entries.filter { it != SpinSpeed.Off }
                    ChoiceRow(
                        label = stringResource(R.string.sch_spin_speed),
                        options = speeds.map { stringResource(spinSpeedLabelRes(it)) },
                        selectedIndex = speeds.indexOf(s.spinTableSpeed),
                        onSelect = { i ->
                            val speed = speeds[i]
                            onUpdate { it.copy(spinTableSpeed = speed) }
                        },
                    )
                }
                ToggleRow(stringResource(R.string.sch_toggle_original_colors), s.originalColors) {
                    onUpdate { it.copy(originalColors = !it.originalColors) }
                }
            }

            SettingsSection(stringResource(R.string.sch_modes)) {
                ToggleRow(stringResource(R.string.sch_toggle_frenzy), s.frenzyMode) {
                    onUpdate { it.copy(frenzyMode = !it.frenzyMode, blindMode = if (!s.frenzyMode) false else it.blindMode) }
                }
                if (s.frenzyMode) {
                    ChoiceRow(
                        label = stringResource(R.string.sch_visible_count),
                        options = (1..6).map { it.toString() },
                        selectedIndex = s.frenzyCount - 1,
                        onSelect = { onUpdate { st -> st.copy(frenzyCount = it + 1) } },
                    )
                    ToggleRow(
                        stringResource(R.string.sch_toggle_hide_react),
                        s.hideReact && s.frenzyCount == 1,
                        enabled = s.frenzyCount == 1,
                    ) {
                        onUpdate { it.copy(hideReact = !it.hideReact) }
                    }
                }
                ToggleRow(stringResource(R.string.sch_toggle_blind), s.blindMode) {
                    onUpdate { it.copy(blindMode = !it.blindMode, frenzyMode = if (!s.blindMode) false else it.frenzyMode) }
                }
                ToggleRow(stringResource(R.string.sch_toggle_math), s.mathMode) {
                    onUpdate { it.copy(mathMode = !it.mathMode, lettersMode = if (!s.mathMode) false else it.lettersMode) }
                }
                ToggleRow(stringResource(R.string.sch_toggle_letters), s.lettersMode) {
                    onUpdate { it.copy(lettersMode = !it.lettersMode, mathMode = if (!s.lettersMode) false else it.mathMode) }
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
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    YCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            content()
        }
    }
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    enabled: Boolean = true,
    onToggle: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = interaction, indication = null, enabled = enabled) { onToggle() }
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(
                    if (checked && enabled) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surface,
                )
                .border(
                    1.5.dp,
                    if (checked && enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    RoundedCornerShape(6.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (checked && enabled) {
                Text(
                    "✓",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Spacer(Modifier.size(10.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
        )
    }
}

@Composable
private fun ChoiceRow(
    label: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    Column {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.size(6.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            options.forEachIndexed { i, option ->
                val selected = i == selectedIndex
                YBadge(
                    text = option,
                    containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                    contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onSelect(i) },
                )
            }
        }
    }
}

@Composable
private fun PlayingContent(
    state: SchulteState,
    onCellTap: (Int) -> Unit,
    onNextRound: () -> Unit,
    onQuit: () -> Unit,
) {
    val breakPhase = state.phase == SchultePhase.RoundBreak
    val spinning = state.settings.spinTable && state.phase == SchultePhase.Playing
    val speed = state.settings.spinTableSpeed
    val infinite = rememberInfiniteTransition(label = "spin")
    val spinProgress by infinite.animateFloat(
        initialValue = 0f,
        targetValue = if (speed.reverse) -360f else 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = speed.durationMs.toInt().coerceAtLeast(1), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "spinAngle",
    )

    Column(modifier = Modifier.fillMaxSize()) {
        TopBar(
            title = stringResource(R.string.game_schulte),
            onBack = onQuit,
            actions = {
                if (state.settings.showRounds && state.settings.rounds > 1 && !breakPhase) {
                    Text(
                        "${state.roundIndex + 1} / ${state.settings.rounds}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                }
                Text(
                    state.timeLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 4.dp),
                )
            },
        )

        if (breakPhase) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        stringResource(R.string.sch_round_done, state.roundIndex),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.sch_time_so_far, state.timeLabel),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(20.dp))
                    YButton(
                        text = stringResource(R.string.sch_next_round),
                        onClick = onNextRound,
                    )
                }
            }
            return
        }

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val rotation = if (spinning) spinProgress else 0f
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .padding(horizontal = 8.dp)
                    .rotate(rotation),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                SchulteGrid(
                    state = state,
                    onCellTap = onCellTap,
                )
            }

            if (state.settings.showCenterDot) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF22C55E)),
                )
            }
        }
    }
}

@Composable
private fun SchulteGrid(
    state: SchulteState,
    onCellTap: (Int) -> Unit,
) {
    val n = state.settings.gridSize
    val colors = if (state.settings.originalColors) GroupColorsOriginal else GroupColorsDefault

    Column(modifier = Modifier.fillMaxWidth()) {
        repeat(n) { row ->
            Row(modifier = Modifier.fillMaxWidth().weight(1f, fill = true)) {
                repeat(n) { col ->
                    val index = row * n + col
                    val cell = state.cells.getOrNull(index)
                    val role = state.cellColorRole(index)
                    val bg = when (role) {
                        SchulteCellRole.Correct -> CorrectGreen
                        SchulteCellRole.Wrong -> WrongRed
                        SchulteCellRole.Traced -> CorrectGreen
                        SchulteCellRole.Normal -> MaterialTheme.colorScheme.surface
                    }
                    val textColor = when {
                        role == SchulteCellRole.Wrong -> Color(0xFF7F1D1D)
                        state.settings.colorGroups && cell != null ->
                            colors.getOrElse(cell.group) { MaterialTheme.colorScheme.onSurface }
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .background(bg)
                            .border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(0.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                enabled = state.phase == SchultePhase.Playing,
                            ) { onCellTap(index) },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (cell != null && cell.symbolVisible) {
                            Text(
                                text = cell.symbol,
                                color = textColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = when (n) {
                                    in 2..4 -> 28.sp
                                    5 -> 22.sp
                                    6 -> 18.sp
                                    7 -> 16.sp
                                    else -> 14.sp
                                },
                                textAlign = TextAlign.Center,
                                // Keeps "-1/2" and "-3" from rendering as "1/2-" in RTL locales.
                                style = TextStyle(textDirection = TextDirection.Ltr),
                                modifier = Modifier.rotate(cell.rotationDegrees),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultContent(
    state: SchulteState,
    onPlayAgain: () -> Unit,
    onOptions: () -> Unit,
    onHome: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(24.dp))
        Text(
            stringResource(R.string.top_result),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(16.dp))

        YCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    state.scoreLabel,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    stringResource(R.string.sch_time, state.timeLabel),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    pluralStringResource(R.plurals.sch_penalty_n, state.wrongTaps, state.penaltySeconds, state.wrongTaps),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (state.bestMs > 0) {
                    YBadge(
                        text = if (state.isNewBest) stringResource(R.string.sch_new_best)
                        else stringResource(R.string.sch_best, formatSchulteTime(state.bestMs.toLong())),
                        containerColor = if (state.isNewBest) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                        contentColor = if (state.isNewBest) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondary,
                    )
                }
            }
        }

        Spacer(Modifier.weight(1f))

        YButton(
            text = stringResource(R.string.sch_play_again),
            onClick = onPlayAgain,
            modifier = Modifier.fillMaxWidth().height(52.dp),
        )
        Spacer(Modifier.height(10.dp))
        YButton(
            text = stringResource(R.string.btn_options),
            onClick = onOptions,
            variant = YButtonVariant.Outline,
            modifier = Modifier.fillMaxWidth().height(48.dp),
        )
        Spacer(Modifier.height(10.dp))
        YButton(
            text = stringResource(R.string.nav_home),
            onClick = onHome,
            variant = YButtonVariant.Ghost,
            modifier = Modifier.fillMaxWidth().height(48.dp),
        )
        Spacer(Modifier.height(20.dp))
    }
}
